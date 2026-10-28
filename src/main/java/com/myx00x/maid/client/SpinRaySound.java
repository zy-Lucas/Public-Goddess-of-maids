package com.myx00x.maid.client;

import com.myx00x.maid.registry.ModSounds;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

public final class SpinRaySound extends AbstractTickableSoundInstance {
   private static final float FLOOR = 1.0E-4F;
   private static final long MAX_MS = 8000L;
   private static final Map<Integer, SpinRaySound> LIVE = new ConcurrentHashMap<>();
   private final int maidId;
   private final double noFalloff;
   private final double far;
   private double x;
   private double y;
   private double z;
   private final long bornMs;
   private final long holdMs;
   private final long fadeMs;
   private float applied = Float.NaN;

   private SpinRaySound(int maidId, double x, double y, double z, double noFalloff, double far, long holdMs, long fadeMs) {
      super((SoundEvent)ModSounds.SPIN_RAY.get(), SoundSource.PLAYERS, RandomSource.create());
      this.maidId = maidId;
      this.x = x;
      this.y = y;
      this.z = z;
      this.noFalloff = noFalloff;
      this.far = far;
      this.holdMs = holdMs;
      this.fadeMs = fadeMs;
      this.bornMs = EffectClock.now();
      this.attenuation = Attenuation.NONE;
      this.relative = false;
      this.volume = 1.0E-4F;
      this.pitch = 1.0F;
   }

   public static void play(int maidId, double x, double y, double z, double noFalloff, double far) {
      play(maidId, x, y, z, noFalloff, far, Long.MAX_VALUE, 0L);
   }

   public static void play(int maidId, double x, double y, double z, double noFalloff, double far, int holdTicks, int fadeTicks) {
      play(maidId, x, y, z, noFalloff, far, holdTicks <= 0 ? Long.MAX_VALUE : holdTicks * 50L, Math.max(0, fadeTicks) * 50L);
   }

   public static void play(int maidId, double x, double y, double z, double noFalloff, double far, long holdMs, long fadeMs) {
      if (far > 0.0) {
         if (WorldGate.ready()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getSoundManager() != null && mc.level != null) {
               Entity maid = mc.level.getEntity(maidId);
               if (maid != null) {
                  x = maid.getX();
                  y = maid.getY() + maid.getBbHeight() * 0.5;
                  z = maid.getZ();
               }

               SpinRaySound sound = new SpinRaySound(maidId, x, y, z, noFalloff, far, holdMs, fadeMs);
               SpinRaySound was = LIVE.put(maidId, sound);
               if (was != null) {
                  was.stop();
               }

               mc.getSoundManager().play(sound);
            }
         }
      }
   }

   public static void stop(int maidId) {
      SpinRaySound sound = LIVE.remove(maidId);
      if (sound != null) {
         sound.stop();
      }
   }

   public static void dropAll() {
      for (SpinRaySound sound : LIVE.values()) {
         sound.stop();
      }

      LIVE.clear();
   }

   public static void sweep() {
      long now = EffectClock.now();
      LIVE.entrySet().removeIf(e -> now - e.getValue().bornMs > 8000L);
   }

   public void tick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         this.stop();
      } else {
         Entity maid = mc.level.getEntity(this.maidId);
         if (maid != null) {
            this.x = maid.getX();
            this.y = maid.getY() + maid.getBbHeight() * 0.5;
            this.z = maid.getZ();
         }

         this.applyVolume();
      }
   }

   private void applyVolume() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         double dx = mc.player.getX() - this.x;
         double dy = mc.player.getEyeY() - this.y;
         double dz = mc.player.getZ() - this.z;
         double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
         double inner = Math.max(0.0, this.noFalloff);
         double outer = Math.max(this.far, inner);
         double level;
         if (distance <= inner) {
            level = 1.0;
         } else if (outer > inner) {
            level = Math.max(0.0, 1.0 - (distance - inner) / (outer - inner));
         } else {
            level = 0.0;
         }

         long over = EffectClock.now() - this.bornMs - this.holdMs;
         if (over >= 0L) {
            if (this.fadeMs <= 0L) {
               level = 0.0;
            } else {
               level *= Math.max(0.0, 1.0 - (double)over / this.fadeMs);
            }

            if (level <= 0.0) {
               this.stop();
               return;
            }
         }

         float want = Math.max(1.0E-4F, (float)level);
         if (Float.isNaN(this.applied) || Math.abs(want - this.applied) > 1.0E-4F) {
            this.applied = want;
            this.volume = want;
         }
      }
   }

   public double getX() {
      return this.x;
   }

   public double getY() {
      return this.y;
   }

   public double getZ() {
      return this.z;
   }
}

