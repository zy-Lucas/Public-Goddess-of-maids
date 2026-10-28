package com.myx00x.maid.client;

import com.myx00x.maid.ShenlinMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class CannonRaySound extends AbstractTickableSoundInstance {
   private static long lastSkipLog;
   private static final long SKIP_LOG_MS = 2000L;
   private final double posX;
   private final double posY;
   private final double posZ;

   private CannonRaySound(SoundEvent event, double posX, double posY, double posZ, float volume) {
      super(event, SoundSource.PLAYERS, RandomSource.create());
      this.posX = posX;
      this.posY = posY;
      this.posZ = posZ;
      this.attenuation = Attenuation.NONE;
      this.relative = false;
      this.volume = volume;
      this.pitch = 1.0F;
   }

   public static void play(SoundEvent event, double x, double y, double z, double range) {
      if (WorldGate.ready()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.getSoundManager() != null && range > 0.0) {
            double dx = mc.player.getX() - x;
            double dy = mc.player.getEyeY() - y;
            double dz = mc.player.getZ() - z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double level = 1.0 - dist / range;
            if (level <= 0.0) {
               long nowMs = System.currentTimeMillis();
               if (nowMs - lastSkipLog > 2000L) {
                  lastSkipLog = nowMs;
                  ShenlinMod.LOGGER.info("[shenlin] cannon ray sfx skipped, dist {} range {}", (int)dist, (int)range);
               }
            } else {
               mc.getSoundManager().play(new CannonRaySound(event, x, y, z, (float)level));
            }
         }
      }
   }

   public double getX() {
      return this.posX;
   }

   public double getY() {
      return this.posY;
   }

   public double getZ() {
      return this.posZ;
   }

   public void tick() {
   }
}

