package com.myx00x.maid.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

final class ViewGate {
   private static final double MARGIN = 1.25;

   private ViewGate() {
   }

   static boolean sees(Vec3 eye, Vec3 look, double x, double y, double z) {
      if (look.lengthSqr() < 1.0E-9) {
         return true;
      }

      Vec3 fwdAxis = look.normalize();
      Vec3 to = new Vec3(x - eye.x, y - eye.y, z - eye.z);
      double d = to.length();
      if (d < 1.0E-4) {
         return true;
      }

      Vec3 dir = to.scale(1.0 / d);
      double fwd = dir.dot(fwdAxis);
      if (fwd <= 0.0) {
         return false;
      }

      Vec3 tsrg2 = fwdAxis.cross(new Vec3(0.0, 1.0, 0.0));
      Vec3 up;
      if (tsrg2.lengthSqr() < 1.0E-6) {
         tsrg2 = new Vec3(1.0, 0.0, 0.0);
         up = new Vec3(0.0, 0.0, -1.0).scale(Math.signum(fwdAxis.y));
      } else {
         tsrg2 = tsrg2.normalize();
         up = tsrg2.cross(fwdAxis);
      }

      Minecraft mc = Minecraft.getInstance();
      double tanV = Math.tan(Math.toRadians(((Integer)mc.options.fov().get()).intValue() * 0.5)) * 1.25;
      double aspect = (double)mc.getWindow().getWidth() / Math.max(1, mc.getWindow().getHeight());
      double tanH = tanV * aspect;
      return Math.abs(dir.dot(tsrg2)) > tanH * fwd ? false : Math.abs(dir.dot(up)) <= tanV * fwd;
   }
}

