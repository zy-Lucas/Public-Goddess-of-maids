package com.myx00x.maid.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;

final class EffectInk {
   static final float[] EDGES = new float[]{1.0F, 0.68F, 0.35F, 0.12F, 0.0F};
   static final float[] STAR_EDGES = new float[]{1.0F, 0.62F, 0.34F, 0.14F, 0.0F};
   static final float[] ARC_EDGES = new float[]{1.0F, 0.0F};
   private static final float[] WHITE = new float[]{1.0F, 0.86F, 0.8F};
   private static final float EMBER = 0.13F;
   private static final double GAMMA_RAY = 1.5;
   static final double GAMMA_STAR = 3.0;
   private static final float BLOOD = 0.78F;

   private EffectInk() {
   }

   static int layered(
      VertexConsumer consumer,
      Vec3 a,
      Vec3 b,
      Vec3 side,
      float widthA,
      float widthB,
      float[] rgb,
      float alpha,
      float vA,
      float vB,
      float[] edges,
      boolean axisFade,
      float floor,
      double gamma
   ) {
      int verts = 0;

      for (int i = 0; i < edges.length - 1; i++) {
         for (int sign = 1; sign >= -1; sign -= 2) {
            float s0 = edges[i] * sign;
            float s1 = edges[i + 1] * sign;
            Vec3 a0 = a.add(side.scale(s0 * widthA));
            Vec3 a1 = a.add(side.scale(s1 * widthA));
            Vec3 b0 = b.add(side.scale(s0 * widthB));
            Vec3 b1 = b.add(side.scale(s1 * widthB));
            verts += ink(consumer, a0, rgb, alpha, vA, s0, axisFade, floor, gamma);
            verts += ink(consumer, a1, rgb, alpha, vA, s1, axisFade, floor, gamma);
            verts += ink(consumer, b1, rgb, alpha, vB, s1, axisFade, floor, gamma);
            verts += ink(consumer, a0, rgb, alpha, vA, s0, axisFade, floor, gamma);
            verts += ink(consumer, b1, rgb, alpha, vB, s1, axisFade, floor, gamma);
            verts += ink(consumer, b0, rgb, alpha, vB, s0, axisFade, floor, gamma);
         }
      }

      return verts;
   }

   static int layered(
      VertexConsumer consumer,
      Vec3 a,
      Vec3 b,
      Vec3 side,
      float widthA,
      float widthB,
      float[] rgb,
      float alpha,
      float vA,
      float vB,
      float[] edges,
      boolean axisFade
   ) {
      int verts = 0;

      for (int i = 0; i < edges.length - 1; i++) {
         for (int sign = 1; sign >= -1; sign -= 2) {
            float s0 = edges[i] * sign;
            float s1 = edges[i + 1] * sign;
            Vec3 a0 = a.add(side.scale(s0 * widthA));
            Vec3 a1 = a.add(side.scale(s1 * widthA));
            Vec3 b0 = b.add(side.scale(s0 * widthB));
            Vec3 b1 = b.add(side.scale(s1 * widthB));
            verts += ink(consumer, a0, rgb, alpha, vA, s0, axisFade);
            verts += ink(consumer, a1, rgb, alpha, vA, s1, axisFade);
            verts += ink(consumer, b1, rgb, alpha, vB, s1, axisFade);
            verts += ink(consumer, a0, rgb, alpha, vA, s0, axisFade);
            verts += ink(consumer, b1, rgb, alpha, vB, s1, axisFade);
            verts += ink(consumer, b0, rgb, alpha, vB, s0, axisFade);
         }
      }

      return verts;
   }

   private static int ink(VertexConsumer consumer, Vec3 p, float[] rgb, float alpha, float along, float s, boolean axisFade) {
      return ink(consumer, p, rgb, alpha, along, s, axisFade, 0.13F, 1.5);
   }

   private static int ink(VertexConsumer consumer, Vec3 p, float[] rgb, float alpha, float along, float s, boolean axisFade, float floor, double gamma) {
      float ax = 1.0F - Math.min(1.0F, Math.abs(s));
      float toBlood = (float)Math.pow(ax, gamma);
      float toWhite = (float)Math.pow(ax, 16.0);
      float r = rgb[0] * (floor + (0.78F - floor) * toBlood);
      float g = rgb[1] * (floor + (0.78F - floor) * toBlood);
      float b = rgb[2] * (floor + (0.78F - floor) * toBlood);
      r += (WHITE[0] - r) * toWhite;
      g += (WHITE[1] - g) * toWhite;
      b += (WHITE[2] - b) * toWhite;
      float a = axisFade ? (float)Math.pow(ax, 0.75) * alpha * (float)(0.84 + 0.16 * Math.sin(along * 5.0)) : alpha;
      consumer.vertex(p.x, p.y, p.z).color(r, g, b, Math.min(1.0F, a)).endVertex();
      return 1;
   }
}

