package com.myx00x.maid.client;

import com.myx00x.maid.ShenlinMod;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class SafeCamera {
   private static final double FAKE = -999.0;
   private static final double FAKE_EPS = 1.0;
   private static boolean warned;

   private SafeCamera() {
   }

   public static Vec3 pos(Minecraft mc) {
      Camera cam = camOrNull(mc);
      if (cam != null && cam.isInitialized()) {
         Vec3 p = cam.getPosition();
         if (!isFake(p)) {
            return p;
         }

         warnOnce();
      }

      return fallbackPos(mc);
   }

   public static Vec3 look(Minecraft mc) {
      Camera cam = camOrNull(mc);
      if (cam != null && cam.isInitialized()) {
         Vec3 p = cam.getPosition();
         if (!isFake(p)) {
            return new Vec3(cam.getLookVector());
         }
      }

      return mc.player != null ? mc.player.getViewVector(1.0F) : new Vec3(0.0, 0.0, 1.0);
   }

   public static float xRot(Minecraft mc) {
      Camera cam = camOrNull(mc);
      if (cam != null && cam.isInitialized() && !isFake(cam.getPosition())) {
         return cam.getXRot();
      } else {
         return mc.player == null ? 0.0F : mc.player.getXRot();
      }
   }

   public static float yRot(Minecraft mc) {
      Camera cam = camOrNull(mc);
      if (cam != null && cam.isInitialized() && !isFake(cam.getPosition())) {
         return cam.getYRot();
      } else {
         return mc.player == null ? 0.0F : mc.player.getYRot();
      }
   }

   public static boolean hijacked(Minecraft mc) {
      Camera cam = camOrNull(mc);
      return cam != null && isFake(cam.getPosition());
   }

   private static Camera camOrNull(Minecraft mc) {
      try {
         return mc.gameRenderer == null ? null : mc.gameRenderer.getMainCamera();
      } catch (Throwable t) {
         return null;
      }
   }

   private static boolean isFake(Vec3 p) {
      if (p == null) {
         return true;
      }

      double dx = p.x - -999.0;
      double dy = p.y - -999.0;
      double dz = p.z - -999.0;
      return dx * dx + dy * dy + dz * dz < 1.0;
   }

   private static Vec3 fallbackPos(Minecraft mc) {
      if (mc.player != null) {
         return mc.player.getEyePosition(1.0F);
      } else {
         return mc.gameRenderer != null ? Vec3.ZERO : Vec3.ZERO;
      }
   }

   private static void warnOnce() {
      if (!warned) {
         warned = true;
         ShenlinMod.LOGGER.warn("[shenlin] 主相机被顶替成 FakeCamera(-999,-999,-999)，已切换到自算相机。这通常是永爱之刃 renderAttack 触发过（usedAttack 不会复位）。");
      }
   }
}

