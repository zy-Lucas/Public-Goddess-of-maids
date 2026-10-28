package com.myx00x.maid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class VolumetricFogMesh {
   private static final int PUFFS = 160;
   private static final float PUFF_MIN = 0.11F;
   private static final float PUFF_MAX = 0.24F;
   private static final float PUFF_ALPHA = 0.03F;
   private static final int SEGMENTS = 10;

   private VolumetricFogMesh() {
   }

   public static void draw(
      PoseStack poseStack,
      Vec3 camPos,
      List<VolumetricFog.Ball> balls,
      float ambientR,
      float ambientG,
      float ambientB,
      float lightR,
      float lightG,
      float lightB,
      Vec3 sunDir
   ) {
      if (!balls.isEmpty()) {
         Minecraft mc = Minecraft.getInstance();
         Vec3 look = Vec3.directionFromRotation(SafeCamera.xRot(mc), SafeCamera.yRot(mc));
         Vec3 tsrg2 = look.cross(new Vec3(0.0, 1.0, 0.0));
         Vec3 up;
         if (tsrg2.lengthSqr() < 1.0E-6) {
            tsrg2 = new Vec3(1.0, 0.0, 0.0);
            up = new Vec3(0.0, 0.0, -1.0).scale(Math.signum(look.y));
         } else {
            tsrg2 = tsrg2.normalize();
            up = tsrg2.cross(look);
         }

         BufferSource buffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
         VertexConsumer vc = buffer.getBuffer(ModRenderTypes.GLOW_ALPHA);
         Matrix4f mat = poseStack.last().pose();

         for (VolumetricFog.Ball b : balls) {
            float fade = b.fadeNow();
            if (!(fade <= 0.002F)) {
               long seed = (long)(b.center().x * 31.0 + b.center().y * 977.0 + b.center().z * 131.0);

               for (int i = 0; i < 160; i++) {
                  float u = hash01(seed, i * 3 + 1);
                  float v = hash01(seed, i * 3 + 2);
                  float w = hash01(seed, i * 3 + 3);
                  double theta = u * Math.PI * 2.0;
                  double cosPhi = v * 2.0 - 1.0;
                  double sinPhi = Math.sqrt(Math.max(0.0, 1.0 - cosPhi * cosPhi));
                  double rr = Math.cbrt(w) * b.radius() * 0.9;
                  rr *= 0.62 + 0.72 * hash01(seed, i * 13 + 5);
                  double ox = Math.cos(theta) * sinPhi * rr;
                  double oy = cosPhi * rr * 0.8;
                  double oz = Math.sin(theta) * sinPhi * rr;
                  Vec3 at = b.center().add(ox, oy, oz);
                  float size = b.radius() * (0.11F + 0.13F * hash01(seed, i * 7 + 11));
                  float hNorm = Mth.clamp((float)(oy / b.radius() * 0.5 + 0.5), 0.0F, 1.0F);
                  float shade = 0.3F + 0.7F * hNorm;
                  shade *= 0.88F + 0.24F * hash01(seed, i * 17 + 3);
                  float cr = ambientR * (0.4F + 0.6F * shade) + lightR * shade;
                  float cg = ambientG * (0.4F + 0.6F * shade) + lightG * shade;
                  float cb = ambientB * (0.4F + 0.6F * shade) + lightB * shade;
                  float ca = 0.03F * fade * (0.8F + 0.4F * hash01(seed, i * 23 + 7));
                  disc(vc, mat, at, camPos, tsrg2, up, size, Mth.clamp(cr, 0.0F, 1.0F), Mth.clamp(cg, 0.0F, 1.0F), Mth.clamp(cb, 0.0F, 1.0F), ca);
               }
            }
         }

         buffer.endBatch(ModRenderTypes.GLOW_ALPHA);
      }
   }

   private static void disc(VertexConsumer vc, Matrix4f mat, Vec3 at, Vec3 camPos, Vec3 tsrg2, Vec3 up, float size, float r, float g, float b, float a) {
      Vec3 c = at.subtract(camPos);
      float cx = (float)c.x;
      float cy = (float)c.y;
      float cz = (float)c.z;

      for (int i = 0; i < 10; i++) {
         float a0 = (float) (Math.PI * 2) * i / 10.0F;
         float a1 = (float) (Math.PI * 2) * (i + 1) / 10.0F;
         float c0 = (float)Math.cos(a0);
         float s0 = (float)Math.sin(a0);
         float c1 = (float)Math.cos(a1);
         float s1 = (float)Math.sin(a1);
         float x0 = cx + (float)(tsrg2.x * c0 * size + up.x * s0 * size);
         float y0 = cy + (float)(tsrg2.y * c0 * size + up.y * s0 * size);
         float z0 = cz + (float)(tsrg2.z * c0 * size + up.z * s0 * size);
         float x1 = cx + (float)(tsrg2.x * c1 * size + up.x * s1 * size);
         float y1 = cy + (float)(tsrg2.y * c1 * size + up.y * s1 * size);
         float z1 = cz + (float)(tsrg2.z * c1 * size + up.z * s1 * size);
         vc.vertex(mat, cx, cy, cz).color(r, g, b, a).endVertex();
         vc.vertex(mat, x1, y1, z1).color(r, g, b, 0.0F).endVertex();
         vc.vertex(mat, x0, y0, z0).color(r, g, b, 0.0F).endVertex();
      }
   }

   private static float hash01(long seed, int k) {
      long x = seed * 374761393L + k * 668265263L;
      x = (x ^ x >> 13) * 1274126177L;
      x ^= x >> 16;
      return (float)((x & 2147483647L) / 2.147483647E9);
   }
}

