package com.myx00x.maid.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT, bus = Bus.FORGE)
public final class ScreenShake {
   private static long start;
   private static long end;
   private static float power;
   private static float downBias;
   private static float bounce;
   private static final double BOUNCE_DECAY = 3.2;
   private static float rough;
   private static final RandomSource JITTER = RandomSource.create();
   private static long holdEnd;
   private static float holdPower;
   private static float holdDown;
   private static float holdRough;
   private static final long HOLD_TAIL_MS = 150L;
   private static float kickX;
   private static float kickZ;
   private static float kickPower;
   private static final double KICK_FREQ = 2.6;
   private static final double KICK_DECAY = 6.5;
   private static final double SEEN_MARGIN = 1.25;

   private ScreenShake() {
   }

   public static boolean seenById(int id) {
      Minecraft mc = Minecraft.getInstance();
      Player self = mc.player;
      Level level = mc.level;
      if (self != null && level != null) {
         Entity source = level.getEntity(id);
         return source != null && seen(self, source);
      } else {
         return false;
      }
   }

   private static boolean seen(Player self, Entity source) {
      if (source.level() != self.level()) {
         return false;
      }

      Vec3 eye = self.getEyePosition();
      Vec3 at = source.position().add(0.0, source.getBbHeight() * 0.5, 0.0);
      Vec3 to = at.subtract(eye);
      double d = to.length();
      if (d < 1.0E-4) {
         return true;
      }

      Vec3 dir = to.scale(1.0 / d);
      Vec3 look = self.getViewVector(1.0F);
      double fwd = dir.dot(look);
      if (fwd <= 0.0) {
         return false;
      }

      Vec3 tsrg2 = look.cross(new Vec3(0.0, 1.0, 0.0));
      Vec3 up;
      if (tsrg2.lengthSqr() < 1.0E-6) {
         tsrg2 = new Vec3(1.0, 0.0, 0.0);
         up = new Vec3(0.0, 0.0, -1.0).scale(Math.signum(look.y));
      } else {
         tsrg2 = tsrg2.normalize();
         up = tsrg2.cross(look);
      }

      Minecraft mc = Minecraft.getInstance();
      double tanV = Math.tan(Math.toRadians(((Integer)mc.options.fov().get()).intValue() * 0.5)) * 1.25;
      double aspect = (double)mc.getWindow().getWidth() / Math.max(1, mc.getWindow().getHeight());
      double tanH = tanV * aspect;
      if (Math.abs(dir.dot(tsrg2)) > tanH * fwd) {
         return false;
      } else {
         return Math.abs(dir.dot(up)) > tanV * fwd ? false : self.hasLineOfSight(source);
      }
   }

   public static void trigger(int ticks, float strength) {
      trigger(ticks, strength, 0.0F);
   }

   public static void trigger(int ticks, float strength, float downBias) {
      trigger(ticks, strength, downBias, 0.0F);
   }

   public static void trigger(int ticks, float strength, float downBias, float rough) {
      trigger(ticks, strength, downBias, rough, 0.0F);
   }

   public static void trigger(int ticks, float strength, float downBias, float rough, float bounce) {
      trigger(ticks, strength, downBias, rough, bounce, 0.0F, 0.0F, 0.0F);
   }

   public static void trigger(int ticks, float strength, float downBias, float rough, float bounce, float kx, float kz, float kp) {
      if (ticks > 0 && !(strength <= 0.0F)) {
         long now = EffectClock.now();
         if (now >= end) {
            start = now;
            power = strength;
            ScreenShake.downBias = downBias;
            ScreenShake.rough = rough;
            ScreenShake.bounce = bounce;
            kickX = kx;
            kickZ = kz;
            kickPower = kp;
         } else {
            power = Math.max(power, strength);
            if (Math.abs(downBias) > Math.abs(ScreenShake.downBias)) {
               ScreenShake.downBias = downBias;
            }

            ScreenShake.rough = Math.max(ScreenShake.rough, rough);
            ScreenShake.bounce = Math.max(ScreenShake.bounce, bounce);
            if (Math.abs(kp) > Math.abs(kickPower)) {
               kickX = kx;
               kickZ = kz;
               kickPower = kp;
            }
         }

         end = Math.max(end, now + ticks * 50L);
      }
   }

   public static void hold(int ticks, float strength, float downBias, float rough) {
      if (ticks > 0 && !(strength <= 0.0F)) {
         long now = EffectClock.now();
         holdPower = strength;
         holdDown = downBias;
         holdRough = rough;
         holdEnd = Math.max(holdEnd, now + ticks * 50L);
      }
   }

   @SubscribeEvent
   public static void onCameraAngles(ComputeCameraAngles event) {
      long now = EffectClock.now();
      double t = now * 0.06;
      float yaw = 0.0F;
      float pitch = 0.0F;
      float roll = 0.0F;
      long span = end - start;
      if (span > 0L && now < end) {
         double tau = (now - start) / 1000.0;
         float amp;
         float pull;
         if (bounce > 0.0F) {
            double damp = Math.exp(-3.2 * tau);
            amp = power * (float)damp;
            pull = downBias * (float)(damp * Math.cos(tau * bounce * Math.PI * 2.0));
         } else {
            float left = (float)(end - now) / (float)span;
            amp = power * left * left;
            pull = downBias * left * left;
         }

         float fine = 1.0F - rough;
         yaw += (wave(t, 0.0) * fine + jitter() * rough) * amp;
         if (kickPower != 0.0F) {
            double camYaw = Math.toRadians(event.getYaw());
            double rx = -Math.cos(camYaw);
            double rz = -Math.sin(camYaw);
            double len = Math.sqrt(kickX * kickX + kickZ * kickZ);
            double dot = len < 1.0E-6 ? 0.0 : (kickX * rx + kickZ * rz) / len;
            double kickDamp = Math.exp(-6.5 * tau);
            yaw += (float)(dot * kickPower * kickDamp * Math.cos(tau * 2.6 * Math.PI * 2.0));
         }

         pitch += (wave(t, 1.7) * fine * 0.8F + jitter() * rough * 0.8F) * amp + pull;
         roll += (wave(t, 3.3) * fine * 1.4F + jitter() * rough * 1.4F) * amp;
      }

      if (now < holdEnd) {
         long leftMs = holdEnd - now;
         float tail = leftMs >= 150L ? 1.0F : (float)leftMs / 150.0F;
         float amp = holdPower * tail;
         float fine = 1.0F - holdRough;
         yaw += (wave(t, 0.0) * fine + jitter() * holdRough) * amp;
         pitch += (wave(t, 1.7) * fine * 0.8F + jitter() * holdRough * 0.8F) * amp + holdDown * tail;
         roll += (wave(t, 3.3) * fine * 1.4F + jitter() * holdRough * 1.4F) * amp;
      }

      if (yaw != 0.0F || pitch != 0.0F || roll != 0.0F) {
         event.setYaw(event.getYaw() + yaw);
         event.setPitch(event.getPitch() + pitch);
         event.setRoll(event.getRoll() + roll);
      }
   }

   private static float jitter() {
      return JITTER.nextFloat() * 2.0F - 1.0F;
   }

   private static float wave(double t, double offset) {
      double v = Math.sin(t + offset) * 0.6 + Math.sin(t * 2.7 + offset * 1.9) * 0.3 + Math.sin(t * 5.3 + offset * 3.1) * 0.15;
      return (float)v;
   }
}

