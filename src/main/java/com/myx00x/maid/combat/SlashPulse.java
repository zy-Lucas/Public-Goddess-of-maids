package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.mob.RelicWardenEntity;
import com.myx00x.maid.network.ModPackets;
import com.myx00x.maid.registry.ModItems;
import com.myx00x.maid.registry.ModSounds;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

public final class SlashPulse {
   private static final long STALE_TICKS = 600L;
   private static final int RAY_TICKS = 7;
   private static final double RAY_RANGE = 64.0;
   private static final float RAY_WIDTH = 1.0F;
   private static final int SHAKE_TICKS = 5;
   private static final float SHAKE_POWER = 1.0F;
   private static final double SHAKE_RANGE = 64.0;
   private static final int LOCK_TICKS = 8;
   private static final Map<UUID, Long> LOCKED_UNTIL = new ConcurrentHashMap<>();
   private static final Map<UUID, Map<UUID, SlashPulse.Clock>> CLOCKS = new ConcurrentHashMap<>();

   private SlashPulse() {
   }

   public static void tick(EntityMaid maid) {
      try {
         tickOnce(maid);
      } catch (Throwable t) {
         ShenlinMod.LOGGER.warn("[shenlin] slash pulse tick failed: {}", t.toString());
      }
   }

   private static void tickOnce(EntityMaid maid) {
      if ((Boolean)Config.SLASH_PULSE_ENABLED.get() && !maid.level().isClientSide && maid.getBrain() != null) {
         long now = maid.level().getGameTime();
         hold(maid, now);
         LivingEntity current = currentTarget(maid);
         if (current == null) {
            current = HateBoard.declaredTarget(maid, (Double)Config.SLASH_PULSE_RANGE.get());
         }

         Map<UUID, SlashPulse.Clock> clocks = CLOCKS.get(maid.getUUID());
         if (current != null && usable(maid, current)) {
            if (clocks == null) {
               clocks = new ConcurrentHashMap<>();
               CLOCKS.put(maid.getUUID(), clocks);
            }

            SlashPulse.Clock clock = clocks.get(current.getUUID());
            if (clock == null) {
               clocks.put(current.getUUID(), new SlashPulse.Clock(now));
            } else {
               clock.lastTouch = now;
            }
         }

         if (clocks != null) {
            for (Entry<UUID, SlashPulse.Clock> entry : clocks.entrySet()) {
               SlashPulse.Clock clock = entry.getValue();
               if (now >= clock.nextSlashAt) {
                  LivingEntity target = find(maid, entry.getKey());
                  if (target != null && !ExecutionSequencer.isExecuted(target) && !RetreatManager.isAvoided(maid.getUUID(), target.getUUID(), now)) {
                     clock.lastTouch = now;
                     if (current == null || !current.getUUID().equals(target.getUUID())) {
                        TargetKeeper.reassert(maid, target);
                     }

                     if (!(maid.distanceTo(target) > (Double)Config.SLASH_PULSE_RANGE.get())) {
                        clock.nextSlashAt = now + ((Integer)Config.SLASH_PULSE_INTERVAL_TICKS.get()).intValue();
                        strike(maid, target);
                     }
                  } else {
                     clocks.remove(entry.getKey());
                  }
               }
            }

            clocks.entrySet().removeIf(e -> now - e.getValue().lastTouch > 600L);
            if (clocks.isEmpty()) {
               CLOCKS.remove(maid.getUUID());
            }
         }
      }
   }

   private static void strike(EntityMaid maid, LivingEntity target) {
      long now = maid.level().getGameTime();
      LOCKED_UNTIL.put(maid.getUUID(), now + 8L);
      float shown = target.getHealth();
      float raw = Executioner.syncedHealth(target);
      float before = shown <= 0.0F ? raw : (raw <= 0.0F ? shown : Math.max(shown, raw));
      if (!Float.isNaN(before)) {
         double ratio = (Double)Config.SLASH_PULSE_CUT_RATIO.get();
         float after = before > 0.0F ? Math.max(0.0F, before * (float)(1.0 - ratio)) : 0.0F;
         boolean landed;
         if (target instanceof RelicWardenEntity warden) {
            warden.ownCutPercent((float)ratio);
            landed = true;
         } else {
            landed = Executioner.setHealthSilently(target, after);
         }

         int hits = ExecutionCounter.recordTrueHit(maid, target, (Integer)Config.SLASH_PULSE_EXEC_COUNT.get());
         ExecutionCounter.noteSwing(maid, target);
         boolean issuedFull = ExecutionCounter.recordIssued(maid, target, before > 0.0F ? Math.max(0.0F, before - after) : 0.0F);
         boolean executing = hits >= (Integer)Config.TRUE_HITS_EXECUTION.get() || issuedFull;
         boolean hole = !executing && ItemsUtil.hasBaubleItemInMaid(maid, (Item)ModItems.BLACK_HOLE_SCROLL.get());
         beam(maid, target, hole);
         if (maid.level() instanceof ServerLevel server) {
            SoundBroadcast.play(server, maid.position(), hole ? (SoundEvent)ModSounds.SLASH_HOLE.get() : (SoundEvent)ModSounds.SLASH.get(), 11);
            ModPackets.sendShake(server, maid.position(), 64.0, 5, 1.0F);
         }

         ShenlinMod.LOGGER
            .info(
               "[shenlin] slash {} hp {} -> {} visible {} shown {} raw {} count {}{}",
               new Object[]{target.getType().getDescriptionId(), before, after > 0.0F ? after : "-inf", target.getHealth(), shown, raw, hits, landed ? "" : " blocked"}
            );
         if (executing) {
            Executioner.executeByOwner(maid, target, "slash");
         }
      }
   }

   private static void beam(EntityMaid maid, LivingEntity target, boolean hole) {
      if (maid.level() instanceof ServerLevel level) {
         Vec3 var6 = maid.getEyePosition();
         Vec3 to = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
         if (!(var6.distanceTo(to) < 0.1)) {
            ModPackets.sendRay(level, var6, 64.0, var6, to, 7, 1.0F, false, 100, SlashFx.starRadius(maid));
            level.sendParticles(ParticleTypes.CRIT, to.x, to.y, to.z, 12, 0.25, 0.25, 0.25, 0.1);
            if (hole) {
               leaveHole(maid, to);
            }
         }
      }
   }

   private static void leaveHole(EntityMaid maid, Vec3 at) {
      if (maid.level() instanceof ServerLevel level) {
         BlackHoleKeeper.spawnSmallHole(level, maid, at);
      }
   }

   private static void hold(EntityMaid maid, long now) {
      Long until = LOCKED_UNTIL.get(maid.getUUID());
      if (until != null) {
         if (now > until) {
            LOCKED_UNTIL.remove(maid.getUUID());
         } else {
            maid.getNavigation().stop();
            maid.setDeltaMovement(0.0, maid.getDeltaMovement().y, 0.0);
            maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            maid.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
         }
      }
   }

   private static LivingEntity currentTarget(EntityMaid maid) {
      LivingEntity target = (LivingEntity)maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
      return target != null ? target : maid.getTarget();
   }

   private static boolean usable(EntityMaid maid, LivingEntity target) {
      return target != maid && !(target instanceof Player) && !(target instanceof EntityMaid) ? !target.isRemoved() && target.isAlive() : false;
   }

   public static void clear(UUID maidId) {
      CLOCKS.remove(maidId);
      LOCKED_UNTIL.remove(maidId);
   }

   public static void cleanup(long now) {
      CLOCKS.entrySet().removeIf(e -> {
         e.getValue().entrySet().removeIf(inner -> now - inner.getValue().lastTouch > 600L);
         return e.getValue().isEmpty();
      });
   }

   private static LivingEntity find(EntityMaid maid, UUID targetId) {
      return maid.level() instanceof ServerLevel level && level.getEntity(targetId) instanceof LivingEntity living ? living : null;
   }

   public static void forgetAll() {
      LOCKED_UNTIL.clear();
      CLOCKS.clear();
   }

   private static final class Clock {
      private long nextSlashAt;
      private long lastTouch;

      private Clock(long now) {
         this.nextSlashAt = now;
         this.lastTouch = now;
      }
   }
}

