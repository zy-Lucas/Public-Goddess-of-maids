package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.network.ModPackets;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData.DataValue;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.level.entity.EntityLookup;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class GuardWatch {
   private static final int EVERY = 1;
   private static final int CAPACITY = 4096;
   private static final double WORLD_EDGE = 3.0E7;
   private static final Map<UUID, GuardWatch.Entry> WATCHED = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> VANISHABLE = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> RECENT = new ConcurrentHashMap<>();
   private static final long VANISH_FRESH = 3000L;
   private static final long VANISH_EXPIRE = 86400000L;
   private static boolean resolved = false;
   private static Field managerField;
   private static Field knownUuidsField;
   private static Field tickListField;
   private static Method tickAdd;
   private static Method managerAdd;
   private static Method managerHasUuid;
   private static Method managerAddNoEvent;
   private static Field visibleField;
   private static Method lookupAdd;
   private static Method lookupGet;
   private static Field sectionStorageField;
   private static Method sectionAt;
   private static Method sectionAdd;
   private static Constructor<?> callbackCtor;
   private static Method setCallback;
   private static Field lookupByUuid;
   private static Field lookupById;
   private static Method lookupGetById;
   private static Field sectionEntities;
   private static Field multiMapAll;
   private static Field multiMapByClass;
   private static Field tickActive;
   private static Field tickPassive;
   private static Field chunkEntityMap;
   private static Method chunkAddEntity;
   private static Method chunkEntityMapPut;
   private static Field trackedLastSection;
   private static final long REPAIR_GAP_MS = 1000L;
   private static final Map<UUID, Object[]> RETAINED = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> LAST_RESEND = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> LAST_PUSH_STAMP = new ConcurrentHashMap<>();
   private static final int LOST_ROUNDS = 200;
   private static final long ORPHAN_EVERY = 100L;
   private static final long ORPHAN_DEEP_EVERY = 6000L;
   private static long orphanLast;
   private static long orphanDeepLast;
   private static final long TROUBLE_WINDOW = 200L;
   private static final long SHIELD_PUSH_MS = 1000L;
   private static final int SHIELD_HOLD_MS = 3000;
   private static final Map<UUID, Integer> SHIELDED = new ConcurrentHashMap<>();
   private static long last;
   private static long lastShield;
   private static final Map<String, Long> LAST_LOG = new ConcurrentHashMap<>();
   private static Field removalReasonField;
   private static Field callbackFieldCache;

   private GuardWatch() {
   }

   public static void markVanishable(UUID id) {
      if (id != null) {
         long now = System.currentTimeMillis();
         VANISHABLE.put(id, now);
         RECENT.put(id, now);
      }
   }

   public static boolean isVanishable(UUID id) {
      return id != null && VANISHABLE.containsKey(id);
   }

   public static boolean isVanishableFresh(UUID id) {
      if (id == null) {
         return false;
      }

      Long at = RECENT.get(id);
      return at != null && System.currentTimeMillis() - at <= 3000L;
   }

   public static void clearVanishable(UUID id) {
      if (id != null) {
         VANISHABLE.remove(id);
         RECENT.remove(id);
      }
   }

   public static void keep(EntityMaid maid) {
      if (!maid.level().isClientSide) {
         UUID id = maid.getUUID();
         if (!isVanishableFresh(id)) {
            RECENT.remove(id);
            GuardWatch.Entry seen = WATCHED.get(id);
            if (seen == null || seen.strong != maid && seen.ref.get() != maid) {
               if (WATCHED.size() > 4096) {
                  trim();
               }

               GuardWatch.Entry entry = new GuardWatch.Entry(new WeakReference<>(maid), maid.level().getGameTime());
               if (inWorld(maid)) {
                  entry.lastX = maid.getX();
                  entry.lastY = maid.getY();
                  entry.lastZ = maid.getZ();
               }

               entry.tier = GuardTier.of(maid);
               entry.strong = maid;
               WATCHED.put(id, entry);
            } else {
               if (inWorld(maid)) {
                  seen.lastX = maid.getX();
                  seen.lastY = maid.getY();
                  seen.lastZ = maid.getZ();
               }

               seen.tier = Math.max(seen.tier, GuardTier.of(maid));
               seen.strong = maid;
            }
         }
      }
   }

   private static boolean inWorld(EntityMaid maid) {
      return Math.abs(maid.getX()) < 3.0E7 && Math.abs(maid.getZ()) < 3.0E7 && maid.getY() > -512.0 && maid.getY() < 1024.0;
   }

   public static void drop(UUID id) {
      WATCHED.remove(id);
      RETAINED.remove(id);
      LAST_RESEND.remove(id);
      LAST_PUSH_STAMP.remove(id);
      ChunkAnchor.release(id, ServerLifecycleHooks.getCurrentServer());
   }

   public static boolean onRoster(EntityMaid maid) {
      return maid != null && maid.level() instanceof ServerLevel level && registeredId(level, maid.getUUID());
   }

   public static EntityMaid held(UUID id) {
      GuardWatch.Entry seen = WATCHED.get(id);
      if (seen == null) {
         return null;
      } else {
         return seen.strong != null ? seen.strong : seen.ref.get();
      }
   }

   public static int tierOf(EntityMaid maid) {
      if (maid == null) {
         return 0;
      }

      int live = GuardTier.of(maid);
      GuardWatch.Entry seen = WATCHED.get(maid.getUUID());
      return seen == null ? live : Math.max(live, seen.tier);
   }

   public static void forgetAll() {
      ChunkAnchor.releaseAll(ServerLifecycleHooks.getCurrentServer());
      WATCHED.clear();
      RETAINED.clear();
      VANISHABLE.clear();
      RECENT.clear();
      LAST_RESEND.clear();
      LAST_PUSH_STAMP.clear();
      SHIELDED.clear();
      LAST_LOG.clear();
      orphanLast = 0L;
      orphanDeepLast = 0L;
   }

   public static Set<UUID> watched() {
      return WATCHED.keySet();
   }

   public static void tick(MinecraftServer server) {
      if (server != null) {
         ServerLevel level = server.overworld();
         if (level != null) {
            long now = level.getGameTime();
            if (now < last) {
               last = now;
            }

            if (now >= last + 1L) {
               last = now;
               long wallNow = System.currentTimeMillis();
               pushShield(wallNow, server);
               ChunkAnchor.sweep(server, now);
               sweepOrphans(server, now);
               if (!WATCHED.isEmpty() || !VANISHABLE.isEmpty()) {
                  VANISHABLE.values().removeIf(stamped -> wallNow - stamped > 86400000L);
                  adoptStale(wallNow, server);
                  Iterator<Map.Entry<UUID, GuardWatch.Entry>> it = WATCHED.entrySet().iterator();

                  while (it.hasNext()) {
                     Map.Entry<UUID, GuardWatch.Entry> pair = it.next();
                     GuardWatch.Entry entry = pair.getValue();
                     EntityMaid maid = entry.strong != null ? entry.strong : entry.ref.get();
                     if (maid == null) {
                        if (++entry.miss >= 200) {
                           it.remove();
                        }
                     } else {
                        entry.miss = 0;
                        if (maid.level() instanceof ServerLevel home && home.getServer() == server) {
                           try {
                              GuardKeeper.scrub(maid);
                              ChunkAnchor.ensure(maid);
                           } catch (Throwable t) {
                              ShenlinMod.LOGGER.warn("[shenlin] guard scrub failed: {}", t.toString());
                           }

                           try {
                              checkOne(entry);
                           } catch (Throwable t) {
                              ShenlinMod.LOGGER.warn("[shenlin] guard watch check failed: {}", t.toString());
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void sweepOrphans(MinecraftServer server, long now) {
      if (now < orphanLast) {
         orphanLast = now;
         orphanDeepLast = now;
      }

      if (now >= orphanLast + 100L) {
         boolean deep = now >= orphanDeepLast + 6000L;
         orphanLast = now;
         if (deep) {
            orphanDeepLast = now;
         }

         for (ServerLevel level : server.getAllLevels()) {
            try {
               if (deep) {
                  Vanisher.scanRoster(level, entity -> {
                     if (entity instanceof EntityMaid maid) {
                        rescueIfLost(level, maid);
                     }
                  });
               } else {
                  Vanisher.scanLive(level, entity -> {
                     if (entity instanceof EntityMaid maid) {
                        rescueIfLost(level, maid);
                     }
                  });
               }
            } catch (Throwable t) {
               ShenlinMod.LOGGER.warn("[shenlin] orphan sweep failed: {}", t.toString());
            }
         }
      }
   }

   private static void rescueIfLost(ServerLevel level, EntityMaid maid) {
      if (maid.level() == level && GuardKeeper.guards(maid)) {
         if (!GuardRemoval.handover(maid.getUUID(), level.getGameTime())) {
            boolean marked = maid.isRemoved();
            boolean ticked = inTicks(level, maid);
            boolean sectioned = inSection(level, maid);
            if (!marked && ticked && sectioned) {
               keep(maid);
            } else {
               if (note(maid, "orphan")) {
                  ShenlinMod.LOGGER
                     .warn(
                        "[shenlin] maid {} off the books: removed={} reason={} tick={} section={} blood={}",
                        new Object[]{maid.getUUID(), marked, maid.getRemovalReason(), ticked, sectioned, GuardVitals.alive(maid)}
                     );
               }

               GuardKeeper.clearRemovalMark(maid);
               GuardKeeper.scrub(maid);
               ChunkAnchor.ensure(maid);
               keep(maid);
               restore(maid, false);
               if (note(maid, "orphan-mended")) {
                  ShenlinMod.LOGGER.info("[shenlin] maid {} back on every book after sweep", maid.getUUID());
               }
            }
         }
      }
   }

   private static void adoptStale(long wallNow, MinecraftServer server) {
      if (!RECENT.isEmpty()) {
         for (Map.Entry<UUID, Long> mark : RECENT.entrySet()) {
            if (wallNow - mark.getValue() > 3000L) {
               UUID id = mark.getKey();
               RECENT.remove(id);

               for (ServerLevel level : server.getAllLevels()) {
                  if (level.getEntity(id) instanceof EntityMaid maid && GuardKeeper.guards(maid)) {
                     keep(maid);
                     if (note(maid, "adopt")) {
                        ShenlinMod.LOGGER.info("[shenlin] maid {} still in world after collect, back under watch", id);
                     }
                     break;
                  }
               }
            }
         }
      }
   }

   private static void pushShield(long wallNow, MinecraftServer server) {
      if (wallNow - lastShield >= 1000L) {
         lastShield = wallNow;
         Map<ServerLevel, List<Integer>> byLevel = new IdentityHashMap<>();

         for (UUID key : WATCHED.keySet()) {
            EntityMaid maid = held(key);
            if (maid != null && maid.level() instanceof ServerLevel level && level.getServer() == server && GuardVitals.alive(maid)) {
               byLevel.computeIfAbsent(level, k -> new ArrayList<>()).add(maid.getId());
               SHIELDED.put(key, maid.getId());
            }
         }

         for (Map.Entry<ServerLevel, List<Integer>> pair : byLevel.entrySet()) {
            List<Integer> ids = pair.getValue();
            int[] raw = new int[ids.size()];

            for (int i = 0; i < raw.length; i++) {
               raw[i] = ids.get(i);
            }

            ModPackets.sendGuardShield(pair.getKey(), raw, 3000);
         }

         SHIELDED.entrySet().removeIf(e -> {
            if (WATCHED.containsKey(e.getKey())) {
               return false;
            }

            ModPackets.sendGuardPass(e.getValue());
            return true;
         });
      }
   }

   private static void checkOne(GuardWatch.Entry entry) {
      EntityMaid maid = entry.strong != null ? entry.strong : entry.ref.get();
      if (maid != null && maid.level() != null && !maid.level().isClientSide) {
         if (!isVanishableFresh(maid.getUUID())) {
            RemovalReason reason = maid.getRemovalReason();
            if (reason == null || !GuardKeeper.isChurnReason(reason) || !GuardKeeper.beyondSight(maid)) {
               if (!GuardVitals.alive(maid)) {
                  if (note(maid, "skip-blood")) {
                     ShenlinMod.LOGGER.info("[shenlin] maid {} left alone, blood reads gone", maid.getUUID());
                  }
               } else if (GuardRemoval.hasPermit(maid)) {
                  if (note(maid, "skip-permit")) {
                     ShenlinMod.LOGGER.info("[shenlin] maid {} left alone, a permit of ours is still open", maid.getUUID());
                  }
               } else {
                  if (GuardVitals.fixCeiling(maid) && note(maid, "ceiling")) {
                     ShenlinMod.LOGGER.info("[shenlin] maid {} ceiling restored, max={}", maid.getUUID(), maid.getMaxHealth());
                  }

                  tidySkin(maid);
                  if (!isVanishableFresh(maid.getUUID())) {
                     boolean moved = pullBackPosition(entry, maid);
                     boolean marked = maid.isRemoved();
                     boolean unlisted = !registered(maid) || !visible(maid) || !queryable(maid);
                     boolean unseen = false;
                     boolean unticking = false;
                     ServerLevel home = maid.level() instanceof ServerLevel sl ? sl : null;
                     if (home != null) {
                        Object live = trackedEntity(chunkMapOf(home), maid);
                        if (live != null) {
                           RETAINED.put(maid.getUUID(), new Object[]{home, live});
                        }

                        unseen = live == null || callbackDetached(maid);
                        unticking = !inTicks(home, maid);
                     }

                     boolean bothered = home != null && GuardKeeper.troubledRecently(maid.getUUID(), home.getGameTime(), 200L);
                     if (bothered || marked || unlisted || moved || unseen || unticking) {
                        if (marked) {
                           GuardKeeper.clearRemovalMark(maid);
                        }

                        if (unlisted || unseen || unticking) {
                           restore(maid, marked);
                           if (note(maid, "pull-in")) {
                              ShenlinMod.LOGGER
                                 .info(
                                    "[shenlin] maid {} pulled back in, marked={} unlisted={} moved={} unseen={} ticking={}",
                                    new Object[]{maid.getUUID(), marked, unlisted, moved, unseen, !unticking}
                                 );
                           }
                        } else if (bothered && home != null) {
                           UUID id = maid.getUUID();
                           long stamp = GuardKeeper.troubleStamp(id);
                           Long pushed = LAST_PUSH_STAMP.get(id);
                           if ((pushed == null || pushed != stamp) && repairTracking(home, maid)) {
                              LAST_PUSH_STAMP.put(id, stamp);
                              if (note(maid, "track-refresh")) {
                                 ShenlinMod.LOGGER.info("[shenlin] maid {} server books all clean, tracking refreshed", maid.getUUID());
                              }
                           }
                        } else if (note(maid, "pull-in")) {
                           ShenlinMod.LOGGER
                              .info(
                                 "[shenlin] maid {} pulled back in, marked={} unlisted={} moved={} unseen={} ticking={}",
                                 new Object[]{maid.getUUID(), marked, unlisted, moved, unseen, !unticking}
                              );
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void tidySkin(EntityMaid maid) {
      float before = GuardVitals.skinOf(maid);
      boolean dyingClock = maid.deathTime != 0;
      boolean lying = maid.getPose() == Pose.DYING;
      boolean zero = before <= 0.0F;
      boolean over = GuardVitals.overCeiling(maid);
      boolean swapped = maid.getClass() != EntityMaid.class;
      if (swapped) {
         boolean restored = SkinFix.repair(maid, EntityMaid.class);
         if (note(maid, "skin-swapped")) {
            ShenlinMod.LOGGER
               .info(
                  "[shenlin] maid {} class swapped, now={} restored={} health={} synced={}",
                  new Object[]{maid.getUUID(), maid.getClass().getName(), restored, maid.getHealth(), Executioner.syncedHealth(maid)}
               );
         }
      }

      if (zero || over || dyingClock || lying) {
         if (zero) {
            GuardKeeper.onZeroSkin(maid);
         } else if (over) {
            GuardKeeper.noteTrouble(maid);
            int tier = tierOf(maid);
            float target = GuardTier.locksAtZero(tier) ? 1.0F : GuardVitals.ceilingOf(maid);
            setSkinOver(maid, target);
         }

         if (dyingClock) {
            maid.deathTime = 0;
            maid.hurtTime = 0;
         }

         if (lying) {
            maid.setPose(Pose.STANDING);
         }

         if (note(maid, "skin-tidy")) {
            ShenlinMod.LOGGER
               .info(
                  "[shenlin] maid {} skin tidy, health was {} over={} dyingClock={} lying={} swapped={}",
                  new Object[]{maid.getUUID(), before, over, dyingClock, lying, swapped}
               );
         }
      }
   }

   private static void setSkinOver(EntityMaid maid, float value) {
      try {
         Executioner.putSkin(maid, value);
         maid.deathTime = 0;
         maid.hurtTime = 0;
         maid.fallDistance = 0.0F;
         if (note(maid, "skin-over-fixed")) {
            ShenlinMod.LOGGER
               .info("[shenlin] maid {} skin over ceiling, pulled to {} (was {})", new Object[]{maid.getUUID(), value, Executioner.syncedHealth(maid)});
         }
      } catch (Throwable t) {
         if (note(maid, "skin-over-failed")) {
            ShenlinMod.LOGGER.warn("[shenlin] maid {} skin over fix failed: {}", maid.getUUID(), t.getMessage());
         }
      }
   }

   private static boolean pullBackPosition(GuardWatch.Entry entry, EntityMaid maid) {
      boolean moved = false;
      if (!inWorld(maid)) {
         maid.absMoveTo(entry.lastX, entry.lastY, entry.lastZ);
         moved = true;
         if (note(maid, "pull-back")) {
            ShenlinMod.LOGGER.info("[shenlin] maid {} pulled back from {}, {}, {}", new Object[]{maid.getUUID(), entry.lastX, entry.lastY, entry.lastZ});
         }
      } else {
         entry.lastX = maid.getX();
         entry.lastY = maid.getY();
         entry.lastZ = maid.getZ();
      }

      if (maid.getPose() == Pose.DYING) {
         maid.setPose(Pose.STANDING);
         moved = true;
      }

      return moved;
   }

   public static boolean callbackDetached(EntityMaid maid) {
      EntityInLevelCallback callback = callbackOf(maid);
      return callback == null || callback == EntityInLevelCallback.NULL;
   }

   private static boolean registeredId(ServerLevel level, UUID id) {
      resolve();
      if (managerField != null && managerHasUuid != null) {
         try {
            Object manager = managerField.get(level);
            return manager != null && Boolean.TRUE.equals(managerHasUuid.invoke(manager, id));
         } catch (Throwable t) {
            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean registered(EntityMaid maid) {
      if (maid.level() instanceof ServerLevel level) {
         resolve();
         if (managerField != null && managerHasUuid != null) {
            try {
               Object manager = managerField.get(level);
               if (manager == null) {
                  return true;
               }

               Object found = managerHasUuid.invoke(manager, maid.getUUID());
               return Boolean.TRUE.equals(found);
            } catch (Throwable t) {
               return true;
            }
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   private static boolean visible(EntityMaid maid) {
      if (maid.level() instanceof ServerLevel level) {
         resolve();
         if (managerField != null && visibleField != null && lookupGet != null) {
            try {
               Object manager = managerField.get(level);
               Object lookup = manager == null ? null : visibleField.get(manager);
               return lookup == null ? true : lookupGet.invoke(lookup, maid.getUUID()) != null;
            } catch (Throwable t) {
               return true;
            }
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   private static boolean queryable(EntityMaid maid) {
      if (maid.level() instanceof ServerLevel level) {
         resolve();
         if (managerField != null && visibleField != null && lookupGetById != null) {
            try {
               Object manager = managerField.get(level);
               Object lookup = manager == null ? null : visibleField.get(manager);
               return lookup == null ? true : lookupGetById.invoke(lookup, maid.getId()) == maid;
            } catch (Throwable t) {
               return true;
            }
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   private static List<String> unhealed(ServerLevel level, EntityMaid maid) {
      List<String> bad = new ArrayList<>(8);
      if (!registered(maid)) {
         bad.add("uuid");
      }

      if (!visible(maid)) {
         bad.add("lookup");
      }

      if (!queryable(maid)) {
         bad.add("id");
      }

      if (!inSection(level, maid)) {
         bad.add("section");
      }

      if (!inTicks(level, maid)) {
         bad.add("tick");
      }

      if (callbackDetached(maid)) {
         bad.add("callback");
      }

      if (!tracked(level, maid)) {
         bad.add("track");
      }

      return bad;
   }

   private static boolean inSection(ServerLevel level, EntityMaid maid) {
      resolve();
      if (sectionStorageField != null && sectionAt != null && sectionEntities != null && multiMapAll != null) {
         try {
            Object manager = managerField != null ? managerField.get(level) : null;
            Object storage = manager == null ? null : sectionStorageField.get(manager);
            if (storage == null) {
               return true;
            }

            Object section = sectionAt.invoke(storage, SectionPos.of(maid.blockPosition()).asLong());
            Object multiMap = section == null ? null : sectionEntities.get(section);
            return (multiMap == null ? null : multiMapAll.get(multiMap)) instanceof List<?> all && holdsIdentity(all, maid);
         } catch (Throwable t) {
            return true;
         }
      } else {
         return true;
      }
   }

   private static boolean inTicks(ServerLevel level, EntityMaid maid) {
      resolve();
      if (tickListField != null && tickActive != null) {
         try {
            Object list = tickListField.get(level);
            if (list == null) {
               return true;
            } else {
               return tickActive.get(list) instanceof Int2ObjectMap<?> map && map.containsKey(maid.getId())
                  ? true
                  : (tickPassive != null ? tickPassive.get(list) : null) instanceof Int2ObjectMap<?> map && map.containsKey(maid.getId());
            }
         } catch (Throwable t) {
            return true;
         }
      } else {
         return true;
      }
   }

   private static void releaseUuid(ServerLevel level, UUID id) {
      resolve();
      if (managerField != null && knownUuidsField != null) {
         try {
            Object manager = managerField.get(level);
            if (manager == null) {
               return;
            }

            if (knownUuidsField.get(manager) instanceof Set<?> set) {
               set.remove(id);
            }
         } catch (Throwable var5) {
         }
      }
   }

   private static boolean noteId(UUID id, long now) {
      String key = id + "|reclaim";
      Long seen = LAST_LOG.get(key);
      if (seen != null && now - seen < 20L) {
         return false;
      }

      if (LAST_LOG.size() > 64) {
         LAST_LOG.entrySet().removeIf(e -> now - e.getValue() > 1200L);
      }

      LAST_LOG.put(key, now);
      return true;
   }

   public static void reclaim(UUID id, EntityMaid except) {
      if (id != null) {
         for (ServerLevel level : levels()) {
            boolean touched = false;
            boolean held = false;
            if (level.getEntity(id) instanceof EntityMaid other && other != except) {
               GuardRemoval.permit(other);

               try {
                  other.setRemoved(RemovalReason.DISCARDED);
                  touched = true;
                  held = true;
               } catch (Throwable var8) {
               }
            }

            if (registeredId(level, id) && level.getEntity(id) == null) {
               releaseUuid(level, id);
               touched = true;
            }

            if (touched) {
               GuardRemoval.clear(id);
               if (noteId(id, level.getGameTime())) {
                  ShenlinMod.LOGGER.info("[shenlin] maid {} back on the register after collect, detached={}", id, held);
               }
            }
         }
      }
   }

   private static List<ServerLevel> levels() {
      List<ServerLevel> out = new ArrayList<>();
      MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
      if (server != null) {
         for (ServerLevel level : server.getAllLevels()) {
            out.add(level);
         }
      }

      return out;
   }

   private static void clearRemovalReason(EntityMaid maid) {
      if (maid.isRemoved()) {
         RemovalReason reason = maid.getRemovalReason();
         if (reason == null || !GuardKeeper.isChurnReason(reason)) {
            if (removalReasonField == null) {
               removalReasonField = field(Entity.class, "f_146795_", "removalReason");
               if (removalReasonField != null) {
                  removalReasonField.setAccessible(true);
               }
            }

            if (removalReasonField != null) {
               try {
                  removalReasonField.set(maid, null);
               } catch (Throwable var3) {
               }
            }
         }
      }
   }

   private static void restore(EntityMaid maid, boolean wasMarked) {
      if (maid.level() instanceof ServerLevel level) {
         GuardKeeper.clearRemovalMark(maid);
         clearRemovalReason(maid);
         boolean var8 = maid.isRemoved();
         resolve();

         try {
            if (callbackDetached(maid) && !onRoster(maid)) {
               releaseUuid(level, maid.getUUID());
            }

            Object manager = managerField != null ? managerField.get(level) : null;
            boolean placed = false;
            if (!var8) {
               placed = manager != null && enroll(manager, maid);
               if (!placed && manager != null) {
                  releaseUuid(level, maid.getUUID());
                  placed = enroll(manager, maid);
               }

               if (!placed && manager != null) {
                  placed = writeFields(manager, maid);
               }
            }

            if (manager != null) {
               placed = forceTables(manager, maid) || placed;
            }

            backIntoTickList(level, maid);
            if (!placed && note(maid, "restore-miss")) {
               ShenlinMod.LOGGER.warn("[shenlin] maid {} pull back missed every entry", maid.getUUID());
            }

            if (placed) {
               if (wasMarked && note(maid, "restore-marked")) {
                  ShenlinMod.LOGGER.info("[shenlin] maid {} put back on the roster while marked", maid.getUUID());
               }

               if (!RosterRestore.tracked(level, maid)) {
                  if (GuardTier.of(maid) >= 3) {
                     RosterRestore.forceTracking(level, maid);
                  }

                  if (!RosterRestore.tracked(level, maid)) {
                     repairTracking(level, maid);
                  }
               }

               List<String> gaps = unhealed(level, maid);
               if (!gaps.isEmpty() && note(maid, "restore-gap")) {
                  ShenlinMod.LOGGER.warn("[shenlin] maid {} put back, still missing {}", maid.getUUID(), gaps);
               }

               if (!tracked(level, maid)) {
                  resendToClients(level, maid);
               }
            }
         } catch (Throwable t) {
            if (note(maid, "restore-failed")) {
               ShenlinMod.LOGGER.warn("[shenlin] maid {} pull back failed: {}", maid.getUUID(), t.getMessage());
            }
         }
      }
   }

   private static boolean enroll(Object manager, EntityMaid maid) throws ReflectiveOperationException {
      if (managerAddNoEvent != null) {
         managerAddNoEvent.invoke(manager, maid);
      }

      if (registered(maid) && visible(maid)) {
         return true;
      }

      if (managerAdd != null) {
         managerAdd.invoke(manager, maid);
      }

      return registered(maid) && visible(maid);
   }

   private static boolean writeFields(Object manager, EntityMaid maid) {
      if (visibleField != null && lookupAdd != null) {
         try {
            Object lookup = visibleField.get(manager);
            if (lookup == null) {
               return false;
            }

            lookupAdd.invoke(lookup, maid);
            if (knownUuidsField != null && knownUuidsField.get(manager) instanceof Set<?> uuids) {
               ((Set<UUID>)uuids).add(maid.getUUID());
            }

            intoSection(manager, maid);
            return true;
         } catch (Throwable t) {
            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean forceTables(Object manager, EntityMaid maid) {
      if (visibleField != null && lookupByUuid != null && lookupById != null) {
         try {
            Object lookup = visibleField.get(manager);
            if (lookup == null) {
               return false;
            }

            Object uuidIndex = lookupByUuid.get(lookup);
            if (uuidIndex != null) {
               ((Map)uuidIndex).put(maid.getUUID(), maid);
            }

            Object idIndex = lookupById.get(lookup);
            if (idIndex != null) {
               ((Int2ObjectMap)idIndex).put(maid.getId(), maid);
            }

            if (knownUuidsField != null && knownUuidsField.get(manager) instanceof Set<?> uuids) {
               ((Set<UUID>)uuids).add(maid.getUUID());
            }

            intoSectionRaw(manager, maid);
            return true;
         } catch (Throwable t) {
            return false;
         }
      } else {
         return false;
      }
   }

   private static void intoSectionRaw(Object manager, EntityMaid maid) {
      if (sectionStorageField != null && sectionAt != null && sectionEntities != null && multiMapAll != null) {
         try {
            Object storage = sectionStorageField.get(manager);
            if (storage == null) {
               return;
            }

            long key = SectionPos.of(maid.blockPosition()).asLong();
            Object section = sectionAt.invoke(storage, key);
            Object multiMap = section != null ? sectionEntities.get(section) : null;
            if (multiMap == null) {
               return;
            }

            if (section != null && callbackDetached(maid)) {
               attachCallback(manager, maid, key, section);
            }

            if (multiMapAll.get(multiMap) instanceof List<?> all && !holdsIdentity(all, maid)) {
               ((List<EntityMaid>)all).add(maid);
            }

            if (multiMapByClass != null && multiMapByClass.get(multiMap) instanceof Map<?, ?> map) {
               for (Map.Entry<Class<?>, List<Object>> entry : map.entrySet()) {
                  if (entry.getKey().isInstance(maid) && !holdsIdentity(entry.getValue(), maid)) {
                     entry.getValue().add(maid);
                  }
               }
            }
         } catch (Throwable var12) {
         }
      }
   }

   private static boolean holdsIdentity(List<?> list, Object target) {
      for (Object item : list) {
         if (item == target) {
            return true;
         }
      }

      return false;
   }

   private static void resendToClients(ServerLevel level, EntityMaid maid) {
      UUID id = maid.getUUID();
      long wall = System.currentTimeMillis();
      Long last = LAST_RESEND.get(id);
      if (last == null || wall - last >= 1000L) {
         if (LAST_RESEND.size() > 512) {
            LAST_RESEND.entrySet().removeIf(e -> wall - e.getValue() > 10000L);
         }

         LAST_RESEND.put(id, wall);

         try {
            Packet<?> spawn = NetworkHooks.getEntitySpawningPacket(maid);
            List<DataValue<?>> data = maid.getEntityData().getNonDefaultValues();

            for (ServerPlayer player : level.players()) {
               if (!(player.distanceToSqr(maid) >= 16384.0)) {
                  player.connection.send(spawn);
                  if (data != null && !data.isEmpty()) {
                     player.connection.send(new ClientboundSetEntityDataPacket(maid.getId(), data));
                  }
               }
            }
         } catch (Throwable var10) {
         }
      }
   }

   private static boolean repairTracking(ServerLevel level, EntityMaid maid) {
      Object chunkMap = chunkMapOf(level);
      if (chunkMap == null || chunkEntityMap == null) {
         return false;
      }

      if (trackedEntity(chunkMap, maid) != null) {
         return true;
      }

      Object[] kept = RETAINED.get(maid.getUUID());
      if (kept != null && kept.length == 2 && kept[0] == level && kept[1] != null && putTracked(chunkMap, maid, kept[1])) {
         if (note(maid, "track-back")) {
            ShenlinMod.LOGGER.info("[shenlin] maid {} tracking entry put back as it was", maid.getUUID());
         }

         return true;
      } else {
         if (chunkAddEntity == null) {
            return false;
         }

         UUID id = maid.getUUID();
         long wall = System.currentTimeMillis();
         Long last = LAST_RESEND.get(id);
         if (last != null && wall - last < 1000L) {
            return false;
         }

         if (LAST_RESEND.size() > 512) {
            LAST_RESEND.entrySet().removeIf(e -> wall - e.getValue() > 10000L);
         }

         LAST_RESEND.put(id, wall);

         try {
            chunkAddEntity.invoke(chunkMap, maid);
            Object fresh = trackedEntity(chunkMap, maid);
            if (fresh == null) {
               return false;
            }

            RETAINED.put(id, new Object[]{level, fresh});
            if (trackedLastSection != null) {
               trackedLastSection.set(fresh, null);
            }

            return true;
         } catch (Throwable t) {
            if (note(maid, "track-failed")) {
               ShenlinMod.LOGGER.warn("[shenlin] maid {} tracking repair failed: {}", maid.getUUID(), t.getMessage());
            }

            return false;
         }
      }
   }

   private static boolean putTracked(Object chunkMap, EntityMaid maid, Object tracked) {
      if (chunkEntityMapPut == null) {
         return false;
      }

      try {
         Object map = chunkEntityMap.get(chunkMap);
         if (map == null) {
            return false;
         }

         chunkEntityMapPut.invoke(map, maid.getId(), tracked);
         return true;
      } catch (Throwable t) {
         return false;
      }
   }

   private static Object chunkMapOf(ServerLevel level) {
      try {
         return level.getChunkSource().chunkMap;
      } catch (Throwable t) {
         return null;
      }
   }

   private static Object trackedEntity(Object chunkMap, EntityMaid maid) {
      if (chunkEntityMap == null) {
         return null;
      }

      try {
         return chunkEntityMap.get(chunkMap) instanceof Int2ObjectMap<?> entities ? entities.get(maid.getId()) : null;
      } catch (Throwable t) {
         return null;
      }
   }

   private static boolean tracked(ServerLevel level, EntityMaid maid) {
      if (chunkEntityMap == null) {
         return false;
      }

      try {
         Object map = chunkEntityMap.get(level.getChunkSource().chunkMap);
         return map != null && ((Int2ObjectMap)map).containsKey(maid.getId());
      } catch (Throwable t) {
         return false;
      }
   }

   private static void intoSection(Object manager, EntityMaid maid) {
      if (sectionStorageField != null && sectionAt != null && sectionAdd != null) {
         try {
            Object storage = sectionStorageField.get(manager);
            if (storage == null) {
               return;
            }

            long key = SectionPos.of(maid.blockPosition()).asLong();
            Object section = sectionAt.invoke(storage, key);
            if (section == null) {
               return;
            }

            sectionAdd.invoke(section, maid);
            if (callbackDetached(maid)) {
               attachCallback(manager, maid, key, section);
            }
         } catch (Throwable var6) {
         }
      }
   }

   private static void attachCallback(Object manager, EntityMaid maid, long key, Object section) {
      if (callbackCtor != null && setCallback != null) {
         try {
            setCallback.invoke(maid, callbackCtor.newInstance(manager, maid, key, section));
         } catch (Throwable var6) {
         }
      }
   }

   private static void backIntoTickList(ServerLevel level, EntityMaid maid) {
      resolve();
      if (tickListField != null) {
         try {
            Object list = tickListField.get(level);
            if (list == null) {
               return;
            }

            if (tickAdd != null) {
               tickAdd.invoke(list, maid);
            }

            if ((tickActive != null ? tickActive.get(list) : null) instanceof Int2ObjectMap<?> active && !active.containsKey(maid.getId())) {
               active.put(maid.getId(), maid);
            }
         } catch (Throwable var5) {
         }
      }
   }

   private static void resolve() {
      if (!resolved) {
         resolved = true;
         managerField = field(ServerLevel.class, "f_143244_", "entityManager");
         tickListField = field(ServerLevel.class, "f_143243_", "entityTickList");
         tickAdd = method(EntityTickList.class, "m_156908_", "add", Entity.class);
         managerAdd = method(PersistentEntitySectionManager.class, "m_157533_", "addEntity", EntityAccess.class);
         managerHasUuid = method(PersistentEntitySectionManager.class, "m_157550_", "isLoaded", UUID.class);
         knownUuidsField = field(PersistentEntitySectionManager.class, "f_157491_", "knownUuids");
         managerAddNoEvent = method(PersistentEntitySectionManager.class, "addNewEntityWithoutEvent", "addNewEntityWithoutEvent", EntityAccess.class);
         visibleField = field(PersistentEntitySectionManager.class, "f_157494_", "visibleEntityStorage");
         sectionStorageField = field(PersistentEntitySectionManager.class, "f_157495_", "sectionStorage");
         lookupAdd = method(EntityLookup.class, "m_156814_", "add", EntityAccess.class);
         lookupGet = method(EntityLookup.class, "m_156819_", "get", UUID.class);
         lookupGetById = method(EntityLookup.class, "m_156812_", "getEntity", int.class);
         sectionAt = method(EntitySectionStorage.class, "m_156893_", "getOrCreateSection", long.class);
         sectionAdd = method(EntitySection.class, "m_188346_", "add", EntityAccess.class);
         lookupByUuid = field(EntityLookup.class, "f_156808_", "byUuid");
         lookupById = field(EntityLookup.class, "f_156807_", "byId");
         sectionEntities = field(EntitySection.class, "f_156827_", "storage");
         multiMapAll = field(ClassInstanceMultiMap.class, "f_13529_", "all");
         multiMapByClass = field(ClassInstanceMultiMap.class, "f_13527_", "byClass");
         tickActive = field(EntityTickList.class, "f_156903_", "active");
         tickPassive = field(EntityTickList.class, "f_156904_", "passive");
         chunkEntityMap = field(ChunkMap.class, "f_140150_", "entityMap");
         chunkAddEntity = method(ChunkMap.class, "m_140199_", "addEntity", Entity.class);

         try {
            chunkEntityMapPut = Int2ObjectMap.class.getMethod("put", int.class, Object.class);
            chunkEntityMapPut.setAccessible(true);
         } catch (Throwable ignored) {
            chunkEntityMapPut = null;
         }

         try {
            trackedLastSection = field(Class.forName("net.minecraft.server.level.ChunkMap.TrackedEntity"), "f_140474_", "lastSectionPos");
         } catch (Throwable ignored) {
            trackedLastSection = null;
         }

         setCallback = method(Entity.class, "m_141960_", "setLevelCallback", EntityInLevelCallback.class);
         callbackCtor = ctor(
            "net.minecraft.world.level.entity.PersistentEntitySectionManager.Callback",
            PersistentEntitySectionManager.class,
            EntityAccess.class,
            long.class,
            EntitySection.class
         );
         if (managerField == null || managerHasUuid == null || tickListField == null || visibleField == null || lookupGet == null) {
            ShenlinMod.LOGGER
               .warn(
                  "[shenlin] guard watch could not resolve world roster, entity watch is degraded (manager={} hasUuid={} tickList={} visible={} lookupGet={})",
                  new Object[]{managerField != null, managerHasUuid != null, tickListField != null, visibleField != null, lookupGet != null}
               );
         }
      }
   }

   private static Constructor<?> ctor(String name, Class<?>... args) {
      try {
         Constructor<?> found = Class.forName(name).getDeclaredConstructor(args);
         found.setAccessible(true);
         return found;
      } catch (Throwable t) {
         return null;
      }
   }

   private static Field field(Class<?> owner, String... names) {
      for (String name : names) {
         try {
            Field found = owner.getDeclaredField(name);
            found.setAccessible(true);
            return found;
         } catch (Throwable var7) {
         }
      }

      return null;
   }

   private static Method method(Class<?> owner, String srg, String official, Class<?>... args) {
      for (String name : new String[]{srg, official}) {
         try {
            Method found = owner.getDeclaredMethod(name, args);
            found.setAccessible(true);
            return found;
         } catch (Throwable var9) {
         }
      }

      return null;
   }

   private static EntityInLevelCallback callbackOf(EntityMaid maid) {
      try {
         Field found = callbackField();
         return found == null ? null : (EntityInLevelCallback)found.get(maid);
      } catch (Throwable t) {
         return null;
      }
   }

   private static Field callbackField() {
      if (callbackFieldCache == null) {
         callbackFieldCache = field(Entity.class, "f_146801_", "levelCallback");
      }

      return callbackFieldCache;
   }

   private static void trim() {
      int over = WATCHED.size() - 4096;
      if (over > 0) {
         List<UUID> order = new ArrayList<>(WATCHED.keySet());
         order.sort(Comparator.comparingLong(idx -> {
            GuardWatch.Entry seen = WATCHED.get(idx);
            return seen == null ? Long.MAX_VALUE : seen.since;
         }));

         for (UUID id : order) {
            if (over-- <= 0) {
               break;
            }

            WATCHED.remove(id);
            ChunkAnchor.release(id, ServerLifecycleHooks.getCurrentServer());
         }
      }
   }

   private static boolean note(EntityMaid maid, String what) {
      long now = maid.level().getGameTime();
      String key = maid.getUUID() + "|" + what;
      Long seen = LAST_LOG.get(key);
      if (seen != null && now - seen < 20L) {
         return false;
      }

      if (LAST_LOG.size() > 64) {
         LAST_LOG.entrySet().removeIf(e -> now - e.getValue() > 1200L);
      }

      LAST_LOG.put(key, now);
      return true;
   }

   private static final class Entry {
      private final WeakReference<EntityMaid> ref;
      private EntityMaid strong;
      private final long since;
      private double lastX;
      private double lastY;
      private double lastZ;
      private int tier;
      private int miss;

      private Entry(WeakReference<EntityMaid> ref, long since) {
         this.ref = ref;
         this.since = since;
      }
   }
}

