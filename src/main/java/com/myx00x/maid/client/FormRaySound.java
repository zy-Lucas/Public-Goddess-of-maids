package com.myx00x.maid.client;

import com.myx00x.maid.combat.HoverCannon;
import com.myx00x.maid.registry.ModSounds;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

public final class FormRaySound extends AbstractTickableSoundInstance {
   private static final float FLOOR = 1.0E-4F;
   private static final long MAX_MS = 4000L;
   private static final Map<Integer, FormRaySound> LIVE = new ConcurrentHashMap<>();
   private final int maidId;
   private final int targetId;
   private final double noFalloff;
   private final double far;
   private double x;
   private double y;
   private double z;
   private final long bornMs;
   private float applied = Float.NaN;

   private FormRaySound(int maidId, int targetId, double x, double y, double z, double noFalloff, double far) {
      super((SoundEvent)ModSounds.FORM_RAY.get(), SoundSource.PLAYERS, RandomSource.create());
      this.maidId = maidId;
      this.targetId = targetId;
      this.x = x;
      this.y = y;
      this.z = z;
      this.noFalloff = noFalloff;
      this.far = far;
      this.bornMs = EffectClock.now();
      this.attenuation = Attenuation.NONE;
      this.relative = false;
      this.volume = 1.0E-4F;
      this.pitch = 1.0F;
   }

   public static void play(int maidId, int targetId, double x, double y, double z, double noFalloff, double far) {
      if (far > 0.0) {
         if (WorldGate.ready()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getSoundManager() != null && mc.level != null) {
               FormRaySound sound = new FormRaySound(maidId, targetId, x, y, z, noFalloff, far);
               FormRaySound was = LIVE.put(maidId, sound);
               if (was != null) {
                  was.stop();
               }

               mc.getSoundManager().play(sound);
            }
         }
      }
   }

   public static void stop(int maidId) {
      FormRaySound sound = LIVE.remove(maidId);
      if (sound != null) {
         sound.stop();
      }
   }

   public static void dropAll() {
      for (FormRaySound sound : LIVE.values()) {
         sound.stop();
      }

      LIVE.clear();
   }

   public static void sweep() {
      long now = EffectClock.now();
      LIVE.entrySet().removeIf(e -> now - e.getValue().bornMs > 4000L);
   }

   public void tick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         this.stop();
      } else {
         if (this.targetId > 0 && mc.level.getEntity(this.targetId) instanceof LivingEntity living && living.isAlive()) {
            this.x = living.getX();
            this.y = living.getY() + living.getBbHeight() + HoverCannon.formAbove();
            this.z = living.getZ();
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

