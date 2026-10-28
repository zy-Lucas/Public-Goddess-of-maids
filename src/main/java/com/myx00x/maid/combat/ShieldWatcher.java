package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityTombstone;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.registry.ModItems;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

public final class ShieldWatcher {
   public static final long SNAPSHOT_INTERVAL_TICKS = 20L;
   private static final long BEAT_TTL_TICKS = 4L;
   private static final long DEATH_MARK_TTL_TICKS = 200L;
   private static final long ACTIVE_TTL_TICKS = 200L;
   private static final long FROZEN_TICKS = 40L;
   private static final long TELEPORT_GRACE_TICKS = 5L;
   private static final int MAX_RESTORE_FAILURES = 5;
   private static final long STORED_TTL_TICKS = 1728000L;
   private static final double NEAR_RADIUS = 8.0;
   private static final double SEARCH_RADIUS = 48.0;
   private static final double TARGET_RADIUS = 16.0;
   private static final double TOMBSTONE_RADIUS = 24.0;
   private static final Map<UUID, ShieldWatcher.Snapshot> SNAPSHOTS = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> DEATHS = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> LAST_LOG = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> LAST_RESCUE = new ConcurrentHashMap<>();
   private static long scanTickCounter = 0L;

   private ShieldWatcher() {
   }

   public static void record(EntityMaid maid) {
      CompoundTag nbt = MaidDataKeeper.snapshot(maid);
      if (nbt == null) {
         if (shouldLog(maid.getUUID(), maid.level().getGameTime())) {
            ShenlinMod.LOGGER.warn("[shenlin] maid {} data not writable, shield idle", maid.getUUID());
         }
      } else {
         ShieldWatcher.Snapshot fresh = new ShieldWatcher.Snapshot(maid.getUUID(), nbt, maid.getOwnerUUID(), maid.level().dimension(), maid);
         ShieldWatcher.Snapshot previous = SNAPSHOTS.get(maid.getUUID());
         if (previous != null && previous.stored) {
            fresh.stored = true;
            fresh.storedAt = previous.storedAt;
         }

         SNAPSHOTS.put(maid.getUUID(), fresh);
         MaidLedger.dirty();
      }
   }

   public static CompoundTag liveData(EntityMaid maid) {
      CompoundTag now = MaidDataKeeper.snapshot(maid);
      if (now != null) {
         return now;
      }

      ShieldWatcher.Snapshot snapshot = SNAPSHOTS.get(maid.getUUID());
      return snapshot == null ? null : snapshot.nbt().copy();
   }

   public static void beat(EntityMaid maid) {
      if (!maid.level().isClientSide && maid.level() instanceof ServerLevel level) {
         UUID var7 = maid.getUUID();
         ShieldWatcher.Snapshot snapshot = SNAPSHOTS.get(var7);
         if (snapshot != null) {
            if (snapshot.stored) {
               snapshot.stored = false;
               snapshot.acceptTeleport(maid);
               MaidRecovery.sanitize(maid);
               MaidLedger.dirty();
               if (shouldLog(var7, level.getGameTime())) {
                  ShenlinMod.LOGGER.info("[shenlin] maid {} back from the slab, guarding", var7);
               }
            }

            if (snapshot.awaitTeleportSince >= 0L && snapshot.dimension != maid.level().dimension()) {
               snapshot.acceptTeleport(maid);
            }

            snapshot.updatePose(maid);
            snapshot.updateTarget(maid);
            snapshot.anchored = ShieldTier.canHoldClear(maid);
            if (maid.getHealth() > 0.0F && !maid.isDeadOrDying()) {
               DEATHS.remove(var7);
               MaidRecovery.unlock(maid);
            } else {
               long now = level.getGameTime();
               Long last = LAST_RESCUE.get(var7);
               if (last == null || last != now) {
                  LAST_RESCUE.put(var7, now);
                  if (!ShieldTier.canHoldClear(maid) && MaidSlabber.pack(maid)) {
                     MaidRemoval.forceRemove(maid);
                  } else {
                     rescue(level, var7, snapshot, maid);
                  }
               }
            }
         }
      }
   }

   public static boolean onMaidDie(EntityMaid maid) {
      if (!ensureGuard(maid)) {
         return false;
      } else if (!ShieldTier.canHoldClear(maid) && !ShieldTier.canRevive(maid) && MaidSlabber.pack(maid)) {
         MaidRemoval.forceRemove(maid);
         return true;
      } else {
         return takeOver(maid);
      }
   }

   public static void markDeath(EntityMaid maid) {
      ShieldWatcher.Snapshot snapshot = SNAPSHOTS.get(maid.getUUID());
      if (snapshot != null) {
         snapshot.updateTarget(maid);
         DEATHS.put(maid.getUUID(), maid.level().getGameTime());
         if (shouldLog(maid.getUUID(), maid.level().getGameTime())) {
            ShenlinMod.LOGGER.info("[shenlin] maid {} died hp={} marked", maid.getUUID(), maid.getHealth());
         }
      }
   }

   public static boolean onRemovalAttempt(EntityMaid maid, RemovalReason reason, boolean viaSetRemoved) {
      if (!maid.level().isClientSide && maid.level() instanceof ServerLevel level) {
         UUID var6 = maid.getUUID();
         if (MaidRemoval.consumePermit(maid)) {
            return false;
         }

         if (!ensureGuard(maid)) {
            return false;
         }

         if (reason == RemovalReason.CHANGED_DIMENSION) {
            ShieldWatcher.Snapshot snapshot = SNAPSHOTS.get(var6);
            if (viaSetRemoved) {
               if (!inTeleportFlow()) {
                  return takeOver(maid);
               }

               if (snapshot != null) {
                  snapshot.awaitTeleportSince = level.getGameTime();
                  if (shouldLog(var6, level.getGameTime())) {
                     ShenlinMod.LOGGER.info("[shenlin] maid {} away by teleport, watching", var6);
                  }
               }

               return false;
            } else {
               return snapshot != null && snapshot.awaitTeleportSince >= 0L && maid.getRemovalReason() == RemovalReason.CHANGED_DIMENSION ? false : takeOver(maid);
            }
         } else if (reason == RemovalReason.UNLOADED_TO_CHUNK || reason == RemovalReason.UNLOADED_WITH_PLAYER) {
            MinecraftServer server = level.getServer();
            if (server == null || !server.isRunning()) {
               return false;
            }

            if (!ShieldTier.canHoldClear(maid) && !chunkKeptAlive(level, maid)) {
               return false;
            }

            MaidRecovery.unfreeze(maid);
            if (shouldLog(var6, level.getGameTime())) {
               ShenlinMod.LOGGER.info("[shenlin] maid {} faked an unload, revoked", var6);
            }

            return packedInstead(var6, maid) ? false : takeOver(maid);
         } else {
            return packedInstead(var6, maid) ? false : takeOver(maid);
         }
      } else {
         return false;
      }
   }

   private static boolean packedInstead(UUID maidId, EntityMaid maid) {
      if (!ShieldTier.canHoldClear(maid) && MaidSlabber.pack(maid)) {
         MaidLedger.noteClear(maidId, "cleared");
         return true;
      } else {
         return false;
      }
   }

   public static boolean onCallbackDrop(EntityMaid maid) {
      if (!(Boolean)Config.SHIELD_HOLD_CLEARS.get() || maid.isRemoved() || maid.level().isClientSide) {
         return false;
      }

      if (!ensureGuard(maid)) {
         return false;
      }

      MaidRecovery.unfreeze(maid);
      return true;
   }

   public static boolean onTickListEvict(Entity entity) {
      return Config.SHIELD_HOLD_CLEARS.get() && entity instanceof EntityMaid maid && !maid.isRemoved() && !maid.level().isClientSide ? ensureGuard(maid) : false;
   }

   private static EntityMaid findAcrossLevels(MinecraftServer server, UUID maidId) {
      for (ServerLevel level : server.getAllLevels()) {
         if (level.getEntity(maidId) instanceof EntityMaid maid && !maid.isRemoved()) {
            return maid;
         }
      }

      return null;
   }

   private static boolean inTeleportFlow() {
      for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
         String name = frame.getMethodName();
         if ("m_6089_".equals(name) || "removeAfterChangingDimensions".equals(name) || "m_5489_".equals(name) || "changeDimension".equals(name)) {
            return true;
         }
      }

      return false;
   }

   private static boolean chunkKeptAlive(ServerLevel level, EntityMaid maid) {
      try {
         return level.isPositionEntityTicking(maid.blockPosition());
      } catch (Throwable t) {
         return false;
      }
   }

   public static void rejectFakeCollect(EntityMaid maid, ItemStack stack) {
      if (!maid.level().isClientSide && maid.level() instanceof ServerLevel) {
         UUID maidId = maid.getUUID();
         MaidSlabber.voidSlab(stack);
         if (maid.getHealth() <= 0.0F || maid.isDeadOrDying()) {
            maid.setHealth(maid.getMaxHealth());
            maid.deathTime = 0;
            maid.hurtTime = 0;
         }

         MaidLedger.noteClear(maidId, "fake-collect");
         ShenlinMod.LOGGER.info("[shenlin] maid {} held, collect without owner", maidId);
      }
   }

   public static boolean takeOver(EntityMaid maid) {
      if (!maid.level().isClientSide && maid.level() instanceof ServerLevel level) {
         UUID var7 = maid.getUUID();
         if (!ensureGuard(maid)) {
            return false;
         }

         ShieldWatcher.Snapshot snapshot = SNAPSHOTS.get(var7);
         if (snapshot == null) {
            return false;
         }

         long now = level.getGameTime();
         Long last = LAST_RESCUE.get(var7);
         if (last != null && last == now) {
            if (maid.getHealth() <= 0.0F || maid.isDeadOrDying()) {
               maid.setHealth(maid.getMaxHealth());
               maid.deathTime = 0;
               maid.hurtTime = 0;
            }

            return true;
         } else {
            LAST_RESCUE.put(var7, now);
            MaidLedger.noteClear(var7, "removal");
            snapshot.updatePose(maid);
            snapshot.updateTarget(maid);
            rescue(level, var7, snapshot, maid);
            return true;
         }
      } else {
         return false;
      }
   }

   public static void release(UUID maidId) {
      SNAPSHOTS.remove(maidId);
      DEATHS.remove(maidId);
      LAST_LOG.remove(maidId);
      LAST_RESCUE.remove(maidId);
      RetreatManager.clear(maidId);
      TargetKeeper.clear(maidId);
      MaidLedger.forget(maidId);
   }

   public static void stash(EntityMaid maid) {
      ShieldWatcher.Snapshot snapshot = SNAPSHOTS.get(maid.getUUID());
      if (snapshot != null) {
         snapshot.stored = true;
         snapshot.storedAt = maid.level().getGameTime();
         DEATHS.remove(maid.getUUID());
         if (!(Boolean)Config.GUARD_REWRITE.get()) {
            MaidLedger.flushNow();
         }
      }
   }

   public static void unstash(UUID maidId) {
      ShieldWatcher.Snapshot snapshot = SNAPSHOTS.get(maidId);
      if (snapshot != null && snapshot.stored) {
         snapshot.stored = false;
         snapshot.storedAt = -1L;
         snapshot.pendingSlab = false;
         MaidLedger.dirty();
         if (!(Boolean)Config.GUARD_REWRITE.get()) {
            MaidLedger.flushNow();
         }
      }
   }

   static void markPendingDelivery(UUID maidId) {
      ShieldWatcher.Snapshot snapshot = SNAPSHOTS.get(maidId);
      if (snapshot != null) {
         snapshot.pendingSlab = true;
         MaidLedger.dirty();
         MaidLedger.flushNow();
      }
   }

   private static void holdPending(ShieldWatcher.Snapshot taken, long now) {
      taken.stored = true;
      taken.storedAt = now;
      taken.pendingSlab = true;
      SNAPSHOTS.put(taken.maidId, taken);
      MaidLedger.dirty();
      MaidLedger.flushNow();
   }

   public static boolean ensureGuard(EntityMaid maid) {
      UUID maidId = maid.getUUID();
      if (hasSnapshot(maidId)) {
         return true;
      } else if (!maid.level().isClientSide && wearsShield(maid)) {
         MaidRecovery.clearLingering(maid);
         record(maid);
         return hasSnapshot(maidId);
      } else {
         return false;
      }
   }

   private static boolean wearsShield(EntityMaid maid) {
      try {
         return maid.getMaidBauble().containsItem((Item)ModItems.GUARDIAN_SHIELD.get());
      } catch (Throwable t) {
         return false;
      }
   }

   public static boolean isProtected(EntityMaid maid) {
      return hasSnapshot(maid.getUUID());
   }

   public static boolean hasSnapshot(UUID maidId) {
      return SNAPSHOTS.containsKey(maidId);
   }

   public static void tickScan(MinecraftServer server) {
      if (server != null) {
         MaidLedger.tick(server);
         if (!SNAPSHOTS.isEmpty()) {
            scanTickCounter++;
            if (scanTickCounter % 200L == 0L) {
               ShenlinMod.LOGGER.info("[shenlin] watch snapshots={} deaths={}", SNAPSHOTS.size(), DEATHS.size());
            }

            long now = server.overworld() == null ? 0L : server.overworld().getGameTime();

            for (UUID maidId : new ArrayList<>(SNAPSHOTS.keySet())) {
               ShieldWatcher.Snapshot snapshot = SNAPSHOTS.get(maidId);
               if (snapshot != null) {
                  if (snapshot.stored) {
                     if (snapshot.pendingSlab && scanTickCounter % 10L == 0L && MaidSlabber.tryDeliver(server, snapshot)) {
                        snapshot.pendingSlab = false;
                        MaidLedger.flushNow();
                     }

                     if (!snapshot.pendingSlab && now - snapshot.storedAt > 1728000L) {
                        release(maidId);
                     }
                  } else if (snapshot.awaitTeleportSince >= 0L) {
                     EntityMaid landed = findAcrossLevels(server, maidId);
                     if (landed != null) {
                        snapshot.acceptTeleport(landed);
                     } else if (now - snapshot.awaitTeleportSince > 5L) {
                        snapshot.awaitTeleportSince = -1L;
                        ShieldWatcher.Snapshot taken = SNAPSHOTS.remove(maidId);
                        if (taken != null) {
                           ShenlinMod.LOGGER.warn("[shenlin] maid {} gone after a claimed teleport, restoring", maidId);
                           MaidLedger.noteClear(maidId, "fake_teleport");
                           MaidLedger.markMissing(maidId);
                           ServerLevel takenLevel = server.getLevel(taken.dimension());
                           if (!snapshot.anchored && takenLevel != null) {
                              MaidSlabber.Outcome outcome = MaidSlabber.packFromBackup(
                                 takenLevel, maidId, taken.ownerId(), taken.nbt(), taken.x, taken.y, taken.z
                              );
                              if (outcome == MaidSlabber.Outcome.DELIVERED) {
                                 release(maidId);
                                 continue;
                              }

                              if (outcome == MaidSlabber.Outcome.PENDING) {
                                 holdPending(taken, now);
                                 continue;
                              }
                           }

                           restore(server, maidId, taken);
                        }
                     }
                  } else if (now - snapshot.lastSeen > 4L) {
                     ServerLevel level = server.getLevel(snapshot.dimension());
                     if (level != null) {
                        EntityMaid alive = findMaid(level, maidId, snapshot.x, snapshot.y, snapshot.z);
                        if (alive != null) {
                           if (!(alive.getHealth() <= 0.0F) && !alive.isDeadOrDying()) {
                              DEATHS.remove(maidId);
                              if (now - snapshot.lastSeen > 40L) {
                                 MaidRecovery.unfreeze(alive);
                              }
                           } else {
                              snapshot.updatePose(alive);
                              snapshot.updateTarget(alive);
                              if (!ShieldTier.canHoldClear(alive) && MaidSlabber.pack(alive)) {
                                 MaidRemoval.forceRemove(alive);
                              } else {
                                 rescue(level, maidId, snapshot, alive);
                              }
                           }
                        } else {
                           Long diedAt = DEATHS.get(maidId);
                           if (diedAt != null && now - diedAt <= 200L) {
                              ShieldWatcher.Snapshot taken = SNAPSHOTS.remove(maidId);
                              if (taken != null) {
                                 restore(server, maidId, taken);
                              }
                           } else if (now - snapshot.lastSeen > 200L) {
                              EntityMaid elsewhere = findAcrossLevels(server, maidId);
                              if (elsewhere != null) {
                                 snapshot.acceptTeleport(elsewhere);
                              } else if (level.isLoaded(BlockPos.containing(snapshot.x, snapshot.y, snapshot.z))) {
                                 if (!(Boolean)Config.SHIELD_RESTORE_VANISH.get()) {
                                    if (shouldLog(maidId, now)) {
                                       ShenlinMod.LOGGER
                                          .info(
                                             "[shenlin] maid {} gone with no death mark, release ({} ticks since last seen)", maidId, now - snapshot.lastSeen
                                          );
                                    }

                                    release(maidId);
                                 } else {
                                    ShieldWatcher.Snapshot taken = SNAPSHOTS.remove(maidId);
                                    if (taken != null) {
                                       MaidLedger.noteClear(maidId, "vanished");
                                       MaidLedger.markMissing(maidId);
                                       if (!snapshot.anchored) {
                                          MaidSlabber.Outcome outcome = MaidSlabber.packFromBackup(
                                             level, maidId, taken.ownerId(), taken.nbt(), taken.x, taken.y, taken.z
                                          );
                                          if (outcome == MaidSlabber.Outcome.DELIVERED) {
                                             release(maidId);
                                             continue;
                                          }

                                          if (outcome == MaidSlabber.Outcome.PENDING) {
                                             holdPending(taken, now);
                                             continue;
                                          }
                                       }

                                       ShenlinMod.LOGGER
                                          .warn("[shenlin] maid {} gone without a trace, restoring ({} ticks since last seen)", maidId, now - snapshot.lastSeen);
                                       restore(server, maidId, taken);
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void rescue(ServerLevel level, UUID maidId, ShieldWatcher.Snapshot snapshot, EntityMaid maid) {
      LivingEntity target = maid.getTarget();
      if (target == null || !target.isAlive() || target.isRemoved()) {
         target = resolveTarget(level, snapshot);
      }

      if (!(maid.getHealth() <= 0.0F) && !maid.isDeadOrDying()) {
         MaidRecovery.unlock(maid);
      } else {
         MaidRecovery.sanitize(maid);
      }

      DEATHS.remove(maidId);
      snapshot.failures = 0;
      handBackTarget(maid, target);
      if (shouldLog(maidId, level.getGameTime())) {
         ShenlinMod.LOGGER
            .info("[shenlin] shield revived maid {} at {}, {} hp={}", new Object[]{maidId, (int)maid.getX(), (int)maid.getZ(), (int)maid.getHealth()});
      }
   }

   private static void handBackTarget(EntityMaid maid, LivingEntity target) {
      long now = maid.level().getGameTime();
      if (target != null && !RetreatManager.isAvoided(maid.getUUID(), target.getUUID(), now)) {
         if (maid.getTarget() != target || maid.getBrain() == null || !maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).filter(t -> t == target).isPresent()) {
            maid.setTarget(target);
            if (maid.getBrain() != null) {
               maid.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
            }
         }
      } else {
         if (maid.getBrain() != null) {
            maid.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
         }

         maid.setTarget(null);
         if (target != null) {
            TargetKeeper.clear(maid.getUUID());
         }
      }
   }

   private static EntityMaid findMaid(ServerLevel level, UUID maidId, double x, double y, double z) {
      if (findEntity(level, maidId, x, y, z, 8.0) instanceof EntityMaid maid) {
         return maid;
      } else {
         return findEntity(level, maidId, x, y, z, 48.0) instanceof EntityMaid maid ? maid : null;
      }
   }

   private static Entity findEntity(ServerLevel level, UUID id, double x, double y, double z, double radius) {
      AABB box = new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
      Iterator var11 = level.getEntities((Entity)null, box, e -> id.equals(e.getUUID()) && !e.isRemoved()).iterator();
      return var11.hasNext() ? (Entity)var11.next() : null;
   }

   private static void restore(MinecraftServer server, UUID maidId, ShieldWatcher.Snapshot snapshot) {
      ServerLevel level = server.getLevel(snapshot.dimension());
      if (level == null) {
         retryOrDrop(server, maidId, snapshot, "level missing");
      } else if (!(level.getEntity(maidId) instanceof EntityMaid alive && reclaim(level, maidId, alive, snapshot))) {
         if (((EntityType)InitEntities.MAID.get()).create(level) instanceof EntityMaid fresh) {
            try {
               fresh.load(snapshot.nbt().copy());
            } catch (Throwable t) {
               retryOrDrop(server, maidId, snapshot, "load failed: " + t.getMessage());
               return;
            }

            fresh.setUUID(maidId);
            fresh.moveTo(snapshot.x, snapshot.y, snapshot.z, snapshot.yRot, snapshot.xRot);
            MaidRecovery.sanitize(fresh);
            clearTombstones(level, snapshot.ownerId(), fresh);
            if (findEntity(level, maidId, snapshot.x, snapshot.y, snapshot.z, 48.0) != null) {
               retryOrDrop(server, maidId, snapshot, "old copy still there");
            } else {
               boolean placed;
               try {
                  placed = level.addFreshEntity(fresh);
               } catch (Throwable t) {
                  placed = false;
                  ShenlinMod.LOGGER.warn("[shenlin] maid {} addFreshEntity threw: {}", maidId, t.toString());
               }

               if (!placed || !RosterRestore.tracked(level, fresh)) {
                  boolean registerSaidOk = placed;
                  if (RosterRestore.force(level, fresh, true)) {
                     placed = true;
                     ShenlinMod.LOGGER
                        .info("[shenlin] shield put maid {} back by table write (register={})", maidId, registerSaidOk ? "ok-but-untracked" : "refused");
                  }
               }

               if (placed && !RosterRestore.tracked(level, fresh)) {
                  RosterRestore.forceTracking(level, fresh);
               }

               if (!placed) {
                  retryOrDrop(server, maidId, snapshot, "add and table write both failed");
               } else {
                  snapshot.updatePose(fresh);
                  snapshot.updateTarget(fresh);
                  snapshot.failures = 0;
                  DEATHS.remove(maidId);
                  LivingEntity target = resolveTarget(level, snapshot);
                  if (target != null) {
                     RetreatManager.recordDeath(maidId, target, level.getGameTime());
                  }

                  handBackTarget(fresh, target);
                  SNAPSHOTS.put(maidId, snapshot);
                  MaidLedger.dirty();
                  MaidLedger.markLive(maidId);
                  if (shouldLog(maidId, level.getGameTime())) {
                     ShenlinMod.LOGGER
                        .info(
                           "[shenlin] shield restored maid {} at {}, {}{}",
                           new Object[]{maidId, (int)fresh.getX(), (int)fresh.getZ(), handedTarget(fresh)}
                        );
                  }
               }
            }
         } else {
            retryOrDrop(server, maidId, snapshot, "maid create failed");
         }
      }
   }

   private static boolean reclaim(ServerLevel level, UUID maidId, EntityMaid alive, ShieldWatcher.Snapshot snapshot) {
      GuardKeeper.clearRemovalMark(alive);
      GuardKeeper.scrub(alive);
      GuardWatch.keep(alive);
      snapshot.updatePose(alive);
      snapshot.updateTarget(alive);
      snapshot.failures = 0;
      DEATHS.remove(maidId);
      LivingEntity target = resolveTarget(level, snapshot);
      if (target != null) {
         RetreatManager.recordDeath(maidId, target, level.getGameTime());
      }

      handBackTarget(alive, target);
      SNAPSHOTS.put(maidId, snapshot);
      MaidLedger.dirty();
      MaidLedger.markLive(maidId);
      if (shouldLog(maidId, level.getGameTime())) {
         ShenlinMod.LOGGER
            .info(
               "[shenlin] shield kept the maid already standing {} at {}, {}{}",
               new Object[]{maidId, (int)alive.getX(), (int)alive.getZ(), handedTarget(alive)}
            );
      }

      return true;
   }

   private static String handedTarget(EntityMaid maid) {
      if (maid.getBrain() == null) {
         return "";
      }

      LivingEntity handed = (LivingEntity)maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
      return handed == null ? "" : " target " + handed.getType();
   }

   private static void retryOrDrop(MinecraftServer server, UUID maidId, ShieldWatcher.Snapshot snapshot, String why) {
      snapshot.failures++;
      SNAPSHOTS.put(maidId, snapshot);
      if (snapshot.failures <= 5 || snapshot.failures % 100 == 0) {
         ShenlinMod.LOGGER.warn("[shenlin] maid {} still out after {} tries: {}, holding", new Object[]{maidId, snapshot.failures, why});
      }
   }

   private static LivingEntity resolveTarget(ServerLevel level, ShieldWatcher.Snapshot snapshot) {
      UUID targetId = snapshot.targetId;
      if (targetId == null) {
         return null;
      }

      Entity found = findEntity(level, targetId, snapshot.targetX, snapshot.targetY, snapshot.targetZ, 16.0);
      if (found == null) {
         found = findEntity(level, targetId, snapshot.x, snapshot.y, snapshot.z, 48.0);
      }

      return found instanceof LivingEntity living && living.isAlive() && !living.isRemoved() ? living : null;
   }

   public static boolean shouldLog(UUID maidId, long gameTime) {
      Long last = LAST_LOG.get(maidId);
      if (last != null && gameTime - last < 100L) {
         return false;
      }

      LAST_LOG.put(maidId, gameTime);
      return true;
   }

   private static void clearTombstones(ServerLevel level, UUID ownerId, EntityMaid fresh) {
      AABB box = fresh.getBoundingBox().inflate(24.0);
      UUID maidId = fresh.getUUID();

      for (Entity entity : level.getEntities((Entity)null, box, e -> e instanceof EntityMaid)) {
         if (entity instanceof EntityMaid leftover && leftover != fresh && maidId.equals(leftover.getUUID())) {
            MaidRemoval.forceRemove(leftover);
         }
      }

      if (ownerId != null) {
         for (EntityTombstone tombstone : level.getEntities((EntityTypeTest)InitEntities.TOMBSTONE.get(), box, t -> ownerId.equals(t.getOwnerId()))) {
            tombstone.discard();
         }
      }
   }

   static ListTag dumpAll() {
      ListTag list = new ListTag();

      for (Entry<UUID, ShieldWatcher.Snapshot> entry : SNAPSHOTS.entrySet()) {
         ShieldWatcher.Snapshot snapshot = entry.getValue();
         CompoundTag tag = new CompoundTag();
         tag.putUUID("uuid", entry.getKey());
         if (snapshot.ownerId != null) {
            tag.putUUID("owner", snapshot.ownerId);
         }

         tag.putString("dim", snapshot.dimension.location().toString());
         tag.putDouble("x", snapshot.x);
         tag.putDouble("y", snapshot.y);
         tag.putDouble("z", snapshot.z);
         tag.putFloat("yRot", snapshot.yRot);
         tag.putFloat("xRot", snapshot.xRot);
         tag.putLong("lastSeen", snapshot.lastSeen);
         tag.putBoolean("stored", snapshot.stored);
         tag.putLong("storedAt", snapshot.storedAt);
         tag.putBoolean("anchored", snapshot.anchored);
         tag.putBoolean("pendingSlab", snapshot.pendingSlab);
         if (snapshot.targetId != null) {
            tag.putUUID("target", snapshot.targetId);
            tag.putDouble("tx", snapshot.targetX);
            tag.putDouble("ty", snapshot.targetY);
            tag.putDouble("tz", snapshot.targetZ);
         }

         tag.put("maid", snapshot.nbt.copy());
         list.add(tag);
      }

      return list;
   }

   static ShieldWatcher.Snapshot snapshotFromTag(CompoundTag tag) {
      if (tag.hasUUID("uuid") && tag.contains("maid", 10) && !tag.getString("dim").isEmpty()) {
         CompoundTag nbt = tag.getCompound("maid");
         if (nbt.isEmpty()) {
            return null;
         }

         ResourceKey<Level> dimension;
         try {
            ResourceLocation key = ResourceLocation.tryParse(tag.getString("dim"));
            if (key == null) {
               return null;
            }

            dimension = ResourceKey.create(Registries.DIMENSION, key);
         } catch (Throwable t) {
            return null;
         }

         ShieldWatcher.Snapshot snapshot = new ShieldWatcher.Snapshot(
            tag.getUUID("uuid"), nbt, tag.hasUUID("owner") ? tag.getUUID("owner") : null, dimension
         );
         snapshot.x = tag.getDouble("x");
         snapshot.y = tag.getDouble("y");
         snapshot.z = tag.getDouble("z");
         snapshot.yRot = tag.getFloat("yRot");
         snapshot.xRot = tag.getFloat("xRot");
         snapshot.lastSeen = tag.getLong("lastSeen");
         snapshot.stored = tag.getBoolean("stored");
         snapshot.storedAt = tag.getLong("storedAt");
         snapshot.anchored = tag.getBoolean("anchored");
         snapshot.pendingSlab = tag.getBoolean("pendingSlab");
         if (tag.hasUUID("target")) {
            snapshot.targetId = tag.getUUID("target");
            snapshot.targetX = tag.getDouble("tx");
            snapshot.targetY = tag.getDouble("ty");
            snapshot.targetZ = tag.getDouble("tz");
         }

         return snapshot;
      } else {
         return null;
      }
   }

   static void adopt(ShieldWatcher.Snapshot snapshot) {
      SNAPSHOTS.putIfAbsent(snapshot.maidId, snapshot);
   }

   static void markPendingRestore(UUID maidId, long gameTime) {
      DEATHS.put(maidId, gameTime);
   }

   public static void forgetAll() {
      SNAPSHOTS.clear();
      DEATHS.clear();
      LAST_LOG.clear();
      LAST_RESCUE.clear();
   }

   public static final class Snapshot {
      private final UUID maidId;
      private final CompoundTag nbt;
      private final UUID ownerId;
      private volatile ResourceKey<Level> dimension;
      private volatile long awaitTeleportSince = -1L;
      private volatile double x;
      private volatile double y;
      private volatile double z;
      private volatile float yRot;
      private volatile float xRot;
      private volatile UUID targetId;
      private volatile double targetX;
      private volatile double targetY;
      private volatile double targetZ;
      private volatile long lastSeen;
      private volatile boolean stored;
      private volatile long storedAt;
      private volatile boolean anchored;
      private volatile int failures;
      private volatile boolean pendingSlab;

      private Snapshot(UUID maidId, CompoundTag nbt, UUID ownerId, ResourceKey<Level> dimension, EntityMaid maid) {
         this.maidId = maidId;
         this.nbt = nbt;
         this.ownerId = ownerId;
         this.dimension = dimension;
         this.updatePose(maid);
         this.updateTarget(maid);
         this.anchored = ShieldTier.canHoldClear(maid);
      }

      private Snapshot(UUID maidId, CompoundTag nbt, UUID ownerId, ResourceKey<Level> dimension) {
         this.maidId = maidId;
         this.nbt = nbt;
         this.ownerId = ownerId;
         this.dimension = dimension;
      }

      private void updatePose(EntityMaid maid) {
         this.x = maid.getX();
         this.y = maid.getY();
         this.z = maid.getZ();
         this.yRot = maid.getYRot();
         this.xRot = maid.getXRot();
         this.lastSeen = maid.level().getGameTime();
      }

      private void updateTarget(EntityMaid maid) {
         LivingEntity target = maid.getTarget();
         if (target == null && maid.getBrain() != null) {
            target = (LivingEntity)maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
         }

         if (target == null) {
            target = maid.getLastHurtMob();
         }

         if (target != null) {
            this.targetId = target.getUUID();
            this.targetX = target.getX();
            this.targetY = target.getY();
            this.targetZ = target.getZ();
         }
      }

      private void acceptTeleport(EntityMaid maid) {
         this.dimension = maid.level().dimension();
         this.awaitTeleportSince = -1L;
         this.updatePose(maid);
         this.updateTarget(maid);
      }

      public CompoundTag nbt() {
         return this.nbt;
      }

      public UUID maidId() {
         return this.maidId;
      }

      public UUID ownerId() {
         return this.ownerId;
      }

      public ResourceKey<Level> dimension() {
         return this.dimension;
      }

      public boolean stored() {
         return this.stored;
      }
   }
}

