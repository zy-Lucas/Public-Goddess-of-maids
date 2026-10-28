package com.myx00x.maid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.mob.WardenBeamEntity;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class WardenBeamRenderer extends EntityRenderer<WardenBeamEntity> {
   private static final float BEAM_WIDTH = 0.11F;
   private static final float COLUMN_SCALE = 2.6F;
   private static final float FADE_FROM = 0.45F;
   private static final int SEGMENTS = 12;
   private static final Vector3f AXIS_Y = new Vector3f(0.0F, 1.0F, 0.0F);
   private static final Vec3 SIDE_X = new Vec3(1.0, 0.0, 0.0);
   private static long lastLog;

   public WardenBeamRenderer(Context context) {
      super(context);
   }

   public ResourceLocation getTextureLocation(WardenBeamEntity entity) {
      return ResourceLocation.withDefaultNamespace("textures/misc/white.png");
   }

   public void render(WardenBeamEntity beam, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      Vec3 off = beam.offsetLerped(partialTicks);
      double length = off.length();
      if (!(length < 0.05)) {
         float alive = beam.alive();
         float fade = alive >= 0.45F ? 1.0F : alive / 0.45F;
         int rgb = beam.beamColor();
         float[] col = new float[]{(rgb >> 16 & 0xFF) / 255.0F, (rgb >> 8 & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F};
         float half = 0.11F * beam.beamWidth() * (beam.column() ? 2.6F : 1.0F);
         VertexConsumer vc = buffer.getBuffer(ModRenderTypes.GLOW_ALPHA);
         poseStack.pushPose();
         Vector3f dir = new Vector3f((float)off.x, (float)off.y, (float)off.z).normalize();
         poseStack.mulPose(new Quaternionf().rotationTo(AXIS_Y, dir));
         Vector3f cam = cameraInLocal(poseStack);
         float flat = (float)Math.hypot(cam.x, cam.z);
         float camAngle = flat > 0.001F ? (float)Math.atan2(cam.x, cam.z) : 0.0F;
         poseStack.mulPose(new Quaternionf().rotationY(camAngle));
         boolean cut = false;
         if (cam.y > 0.0F && cam.y < length) {
            length = cam.y - 1.2F;
            cut = true;
            if (length < 1.0) {
               poseStack.popPose();
               return;
            }
         }

         probe(beam, off, length, half, rgb, cut, new Matrix4f(poseStack.last().pose()));
         strip(vc, length, half, col, fade);
         poseStack.mulPose(new Quaternionf().rotationY((float) (Math.PI / 2)));
         strip(vc, length, half, col, fade);
         tube(vc, length, half * 0.92F, col, fade, camAngle);
         plate(vc, 0.0F, half * 1.8F, col, fade);
         plate(vc, (float)length, half * 1.8F, col, fade);
         poseStack.popPose();
      }
   }

   private static void probe(WardenBeamEntity beam, Vec3 off, double length, float half, int rgb, boolean cut, Matrix4f view) {
      long now = Util.getMillis();
      if (now - lastLog > 1000L) {
         lastLog = now;
         Minecraft mc = Minecraft.getInstance();
         Vec3 camWorld = SafeCamera.pos(mc);
         double axisDot = off.normalize().dot(camWorld.subtract(beam.position()).normalize());
         Matrix4f proj = mc.gameRenderer.getProjectionMatrix(((Integer)mc.options.fov().get()).doubleValue());
         Vector4f a = ndc(view, proj, 0.0F, 0.0F, 0.0F);
         Vector4f b = ndc(view, proj, 0.0F, (float)length, 0.0F);
         ShenlinMod.LOGGER
            .info(
               "[shenlin] beam render 长={} 粗={} 柱={} 活={} 轴视={} 截={} 色={} 屏={},{} → {},{}",
               new Object[]{
                  String.format("%.2f", length),
                  String.format("%.2f", half),
                  beam.column(),
                  String.format("%.2f", beam.alive()),
                  String.format("%.3f", Math.abs(axisDot)),
                  cut,
                  String.format("%06X", rgb & 16777215),
                  String.format("%.2f", a.x),
                  String.format("%.2f", a.y),
                  String.format("%.2f", b.x),
                  String.format("%.2f", b.y)
               }
            );
      }
   }

   private static Vector4f ndc(Matrix4f mv, Matrix4f proj, float x, float y, float z) {
      Vector4f p = new Vector4f(x, y, z, 1.0F);
      mv.transform(p);
      proj.transform(p);
      float w = Math.abs(p.w) < 1.0E-5F ? 1.0E-5F : p.w;
      return new Vector4f(p.x / w, p.y / w, p.z / w, 1.0F);
   }

   private static Vector3f cameraInLocal(PoseStack poseStack) {
      Vector3f cam = new Vector3f();
      new Matrix4f(poseStack.last().pose()).invert().transformPosition(cam);
      return cam;
   }

   private static void strip(VertexConsumer vc, double length, float half, float[] col, float fade) {
      for (int i = 0; i < 12; i++) {
         float t0 = i / 12.0F;
         float t1 = (i + 1) / 12.0F;
         float w0 = half * taper(t0);
         float w1 = half * taper(t1);
         EffectInk.layered(
            vc, new Vec3(0.0, length * t0, 0.0), new Vec3(0.0, length * t1, 0.0), SIDE_X, w0, w1, col, fade, t0 * 6.0F, t1 * 6.0F, EffectInk.EDGES, true
         );
      }
   }

   private static float taper(float t) {
      return (float)(0.62 + 0.38 * Math.sin(Math.PI * t));
   }

   private static void tube(VertexConsumer vc, double length, float radius, float[] col, float fade, float camAngle) {
      int AROUND = 8;
      float alpha = fade * 0.38F;

      for (int a = 0; a < 8; a++) {
         double a0 = (Math.PI * 2) * a / 8.0;
         double a1 = (Math.PI * 2) * (a + 1) / 8.0;
         float lit = 0.3F + 0.7F * Math.max(0.0F, (float)Math.cos((a0 + a1) * 0.5 - camAngle));
         float r = col[0] * lit;
         float g = col[1] * lit;
         float b = col[2] * lit;
         float x0 = (float)Math.cos(a0);
         float z0 = (float)Math.sin(a0);
         float x1 = (float)Math.cos(a1);
         float z1 = (float)Math.sin(a1);

         for (int k = 0; k < 12; k++) {
            float t0 = k / 12.0F;
            float t1 = (k + 1) / 12.0F;
            float r0 = radius * taper(t0);
            float r1 = radius * taper(t1);
            float y0 = (float)(length * t0);
            float y1 = (float)(length * t1);
            vc.vertex(x0 * r0, y0, z0 * r0).color(r, g, b, alpha).endVertex();
            vc.vertex(x1 * r0, y0, z1 * r0).color(r, g, b, alpha).endVertex();
            vc.vertex(x1 * r1, y1, z1 * r1).color(r, g, b, alpha).endVertex();
            vc.vertex(x0 * r0, y0, z0 * r0).color(r, g, b, alpha).endVertex();
            vc.vertex(x1 * r1, y1, z1 * r1).color(r, g, b, alpha).endVertex();
            vc.vertex(x0 * r1, y1, z0 * r1).color(r, g, b, alpha).endVertex();
         }
      }
   }

   private static void plate(VertexConsumer vc, float y, float half, float[] col, float fade) {
      float r = col[0] * 0.45F;
      float g = col[1] * 0.45F;
      float b = col[2] * 0.45F;
      float a = fade * 0.85F;
      vc.vertex(-half, y, -half).color(r, g, b, a).endVertex();
      vc.vertex(-half, y, half).color(r, g, b, a).endVertex();
      vc.vertex(half, y, half).color(r, g, b, a).endVertex();
      vc.vertex(half, y, half).color(r, g, b, a).endVertex();
      vc.vertex(half, y, -half).color(r, g, b, a).endVertex();
      vc.vertex(-half, y, -half).color(r, g, b, a).endVertex();
   }
}

