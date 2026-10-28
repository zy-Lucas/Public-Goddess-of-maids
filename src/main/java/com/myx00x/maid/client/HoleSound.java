package com.myx00x.maid.client;

import com.myx00x.maid.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

final class HoleSound {
   private static final float FLOOR = 1.0E-4F;
   private final HoleSound.Part start;
   private HoleSound.Part loop;
   private final double x;
   private final double y;
   private final double z;
   private final float warp;
   private final double extra;
   private final long lifeMs;
   private long seen = Long.MIN_VALUE;
   private long elapsed;
   private boolean swapped;
   private boolean fading;
   private boolean finished;
   private float applied = Float.NaN;

   HoleSound(double x, double y, double z, float warp, double extra, long lifeMs) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.warp = warp;
      this.extra = extra;
      this.lifeMs = lifeMs;
      this.start = new HoleSound.Part((SoundEvent)ModSounds.HOLE_START.get(), this, false);
   }

   void begin() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.getSoundManager() == null) {
         this.finished = true;
      } else {
         mc.getSoundManager().play(this.start);
      }
   }

   void update() {
      long now = EffectClock.now();
      if (this.seen == Long.MIN_VALUE) {
         this.seen = now;
      } else {
         long step = now - this.seen;
         this.seen = now;
         this.elapsed += step >= 0L && step <= 2000L ? step : 50L;
         if (!this.swapped && this.elapsed >= 8350L) {
            this.swapped = true;
            if (this.elapsed < this.lifeMs) {
               Minecraft mc = Minecraft.getInstance();
               if (mc.getSoundManager() != null) {
                  mc.getSoundManager().stop(this.start);
                  this.loop = new HoleSound.Part((SoundEvent)ModSounds.HOLE_LOOP.get(), this, true);
                  mc.getSoundManager().play(this.loop);
               }
            }
         }

         if (!this.fading && this.elapsed >= this.lifeMs) {
            this.beginFadeOut();
         }

         this.applyVolume();
         if (this.fading && this.elapsed >= this.fadeDone()) {
            this.stop();
         }
      }
   }

   private long fadeDone() {
      return Math.max(this.lifeMs, 8350L) + 600L;
   }

   private void applyVolume() {
      HoleSound.Part part = this.loop != null ? this.loop : this.start;
      if (part != null) {
         float level = this.levelFor();
         if (this.fading) {
            long from = Math.max(this.lifeMs, 8350L);
            long over = this.elapsed - from;
            level *= (float)Math.max(0.0, 1.0 - over / 600.0);
         }

         if (Float.isNaN(this.applied) || Math.abs(level - this.applied) > 1.0E-4F) {
            this.applied = level;
            part.setVolume(level);
         }
      }
   }

   private float levelFor() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return 0.0F;
      }

      double dx = mc.player.getX() - this.x;
      double dy = mc.player.getEyeY() - this.y;
      double dz = mc.player.getZ() - this.z;
      double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
      double inner = this.warp;
      double outer = this.warp + this.extra;
      double near;
      if (distance <= inner) {
         near = 1.0;
      } else if (outer > inner) {
         near = Math.max(0.0, 1.0 - (distance - inner) / (outer - inner));
      } else {
         near = 0.0;
      }

      return (float)near;
   }

   void beginFadeOut() {
      if (!this.fading) {
         this.fading = true;
         this.elapsed = Math.max(this.elapsed, Math.max(this.lifeMs, 8350L));
      }
   }

   boolean isFading() {
      return this.fading;
   }

   void stop() {
      if (!this.finished) {
         this.finished = true;
         Minecraft mc = Minecraft.getInstance();
         if (mc.getSoundManager() != null) {
            mc.getSoundManager().stop(this.start);
            if (this.loop != null) {
               mc.getSoundManager().stop(this.loop);
            }
         }
      }
   }

   boolean isFinished() {
      return this.finished;
   }

   double distanceSqr(double px, double py, double pz) {
      double dx = px - this.x;
      double dy = py - this.y;
      double dz = pz - this.z;
      return dx * dx + dy * dy + dz * dz;
   }

   private static final class Part extends AbstractTickableSoundInstance {
      private final HoleSound owner;

      private Part(SoundEvent event, HoleSound owner, boolean looping) {
         super(event, SoundSource.AMBIENT, RandomSource.create());
         this.owner = owner;
         this.attenuation = Attenuation.NONE;
         this.relative = false;
         this.looping = looping;
         this.volume = 1.0E-4F;
         this.pitch = 1.0F;
      }

      public double getX() {
         return this.owner.x;
      }

      public double getY() {
         return this.owner.y;
      }

      public double getZ() {
         return this.owner.z;
      }

      public void tick() {
      }

      private void setVolume(float level) {
         this.volume = Math.max(1.0E-4F, Math.max(0.0F, level)) * HoleSounds.master();
      }
   }
}

