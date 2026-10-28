package com.myx00x.maid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.myx00x.maid.FxTrace;
import com.myx00x.maid.mob.WardenLaserBeamEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class WardenLaserBeamRenderer extends EntityRenderer<WardenLaserBeamEntity> {
   private static final ResourceLocation TEXTURE_COLD = ResourceLocation.fromNamespaceAndPath("goddess_of_maids", "textures/entity/beam/warden_beam_cold.png");
   private static final ResourceLocation TEXTURE_PULL = ResourceLocation.fromNamespaceAndPath("goddess_of_maids", "textures/entity/beam/warden_beam_pull.png");
   private static final float BEAM_RADIUS = 0.3F;
   private static final float STAR_RADIUS = 1.3F;
   private static final int STAR_TICKS = 40;
   private static final float END_RADIUS = 0.315F;
   private static final int BEAM_ROWS = 16;
   private static final float SEG_LEN = 0.3F;
   private static final int MAX_SEGS = 320;
   private static final float UV_PERIOD = 3.0F;
   private static final float UV_SPEED = 0.3F;
   private static final float WAVE_SEGS = 9.0F;
   private static final float WAVE_SPEED = 0.95F;
   private static final float BULGE_MID = 0.89F;
   private static final float BULGE_AMP = 0.31F;
   private boolean clearerView;

   public WardenLaserBeamRenderer(Context context) {
      super(context);
   }

   public ResourceLocation getTextureLocation(WardenLaserBeamEntity entity) {
      return entity.warm() ? TEXTURE_PULL : TEXTURE_COLD;
   }

   public void render(WardenLaserBeamEntity beam, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      this.clearerView = false;
      if (!beam.readyToRender()) {
         FxTrace.throttled(
            "beamRenderNotReady",
            "firstFrameBeforeTick",
            null,
            "id="
               + beam.getId()
               + " tick="
               + beam.tickCount
               + " pos="
               + FxTrace.f(beam.getX())
               + "/"
               + FxTrace.f(beam.getY())
               + "/"
               + FxTrace.f(beam.getZ())
         );
      } else {
         if (beam.blueStar() && !beam.starSpawned) {
            beam.starSpawned = true;
            float starRadius = 1.3F * Math.max(0.8F, beam.widthScale());
            StarEffect.spawn(beam.getX(), beam.getY(), beam.getZ(), starRadius, 40, true, beam.getId(), 2.2F, new float[]{0.36F, 0.66F, 1.0F});
            FxTrace.fx(
               "beamStar",
               "blue-emit-star",
               new Vec3(beam.getX(), beam.getY(), beam.getZ()),
               "id=" + beam.getId() + " radius=" + FxTrace.f(starRadius)
            );
         }

         if (beam.spray() && !beam.starSpawned) {
            beam.starSpawned = true;
            float starRadius = 1.3F * Math.max(0.8F, beam.widthScale());
            StarEffect.spawn(beam.getX(), beam.getY(), beam.getZ(), starRadius, 40, true, beam.getId());
            FxTrace.fx(
               "beamStar",
               "spray-beam-sustain",
               new Vec3(beam.getX(), beam.getY(), beam.getZ()),
               "id=" + beam.getId() + " radius=" + FxTrace.f(starRadius) + " width=" + FxTrace.f(beam.widthScale()) + " ticks=40"
            );
         }

         double cx = Mth.lerp(partialTicks, beam.prevCollidePosX, beam.collidePosX);
         double cy = Mth.lerp(partialTicks, beam.prevCollidePosY, beam.collidePosY);
         double cz = Mth.lerp(partialTicks, beam.prevCollidePosZ, beam.collidePosZ);
         double px = Mth.lerp(partialTicks, beam.xOld, beam.getX());
         double py = Mth.lerp(partialTicks, beam.yOld, beam.getY());
         double pz = Mth.lerp(partialTicks, beam.zOld, beam.getZ());
         float yaw = beam.prevYaw + (beam.renderYaw - beam.prevYaw) * partialTicks;
         float pitch = beam.prevPitch + (beam.renderPitch - beam.prevPitch) * partialTicks;
         float length = (float)Math.sqrt(sq(cx - px) + sq(cy - py) + sq(cz - pz));
         float fade = beam.fade();
         if (!(fade <= 0.0F)) {
            length *= fade;
            if (beam.tickCount <= 12 || length > 40.0F) {
               FxTrace.fx(
                  "beamRender",
                  length > 40.0F ? "abnormal-length" : "early-tick",
                  new Vec3(beam.getX(), beam.getY(), beam.getZ()),
                  "id="
                     + beam.getId()
                     + " tick="
                     + beam.tickCount
                     + " len="
                     + FxTrace.f(length)
                     + " end="
                     + FxTrace.f(cx)
                     + "/"
                     + FxTrace.f(cy)
                     + "/"
                     + FxTrace.f(cz)
                     + " yaw="
                     + FxTrace.f(yaw)
                     + " pitch="
                     + FxTrace.f(pitch)
                     + " width="
                     + FxTrace.f(beam.widthScale())
                     + " warm="
                     + beam.warm()
                     + " fixed="
                     + beam.fixedFrame()
                     + " appear="
                     + beam.appear
                     + " fade="
                     + FxTrace.f(fade)
               );
            }

            if (beam.fixedFrame() < 0 && beam.appear < 3) {
               length *= Math.min(1.0F, (beam.appear + partialTicks) / 3.0F);
            }

            int frame;
            if (beam.fixedFrame() >= 0) {
               frame = beam.fixedFrame();
            } else if (fade < 1.0F) {
               frame = Mth.clamp(1 + (int)(fade * 5.0F), 1, 5);
            } else if (beam.appear < 3) {
               frame = Mth.clamp(Mth.floor((beam.appear - 1 + partialTicks) * 2.0F), 0, 5);
            } else {
               frame = 6 + (beam.tickCount >> 1) % 10;
            }

            float clock = beam.tickCount + partialTicks;
            float scale = beam.widthScale();
            VertexConsumer vc = buffer.getBuffer(ModRenderTypes.GLOWING.apply(this.getTextureLocation(beam)));
            this.renderBeam(length, (float)Math.toDegrees(yaw), (float)Math.toDegrees(pitch), clock, scale, fade, beam.spray(), poseStack, vc, packedLight);
            poseStack.pushPose();
            poseStack.translate(cx - px, cy - py, cz - pz);
            this.renderEnd(frame, beam.blockSide, scale, poseStack, vc, packedLight, fade);
            poseStack.popPose();
         }
      }
   }

   private static double sq(double v) {
      return v * v;
   }

   private void renderBeam(
      float length, float yaw, float pitch, float clock, float scale, float fade, boolean spray, PoseStack poseStack, VertexConsumer vc, int packedLight
   ) {
      poseStack.pushPose();
      poseStack.mulPose(quat(90.0F, 0.0F, 0.0F));
      poseStack.mulPose(quat(0.0F, 0.0F, yaw - 90.0F));
      poseStack.mulPose(quat(-pitch, 0.0F, 0.0F));
      poseStack.pushPose();
      float camYaw = SafeCamera.yRot(Minecraft.getInstance());
      if (!this.clearerView) {
         poseStack.mulPose(new Quaternionf().rotationY((float)Math.toRadians(camYaw + 90.0F)));
      }

      this.drawBeam(length, clock, scale, spray, poseStack, vc, packedLight, fade);
      poseStack.popPose();
      if (!this.clearerView) {
         poseStack.pushPose();
         poseStack.mulPose(new Quaternionf().rotationY((float)Math.toRadians(-camYaw - 90.0F)));
         this.drawBeam(length, clock, scale, spray, poseStack, vc, packedLight, fade);
         poseStack.popPose();
      }

      poseStack.popPose();
   }

   private void drawBeam(float length, float clock, float scale, boolean spray, PoseStack poseStack, VertexConsumer vc, int packedLight, float fade) {
      float minU = 0.0F;
      float maxU = minU + 0.078125F;
      float half = 0.3F * scale;
      float offset = this.clearerView ? -1.0F : 0.0F;
      int segs = Math.max(1, Math.min(320, (int)Math.ceil(length / 0.3F)));
      float seg = length / segs;
      float perSeg = (float) (Math.PI * 2.0 / 9.0);
      float waveSpeed = spray ? 2.09F : 0.95F;
      float amp = spray ? 0.65099996F : 0.31F;
      float phase0 = -clock * waveSpeed;
      float uScroll = clock * (spray ? 0.6F : 0.3F);
      Pose pose = poseStack.last();
      Matrix4f m = pose.pose();
      Matrix3f n = pose.normal();
      float wA = half * bulgeAt(phase0, amp) * flareAt(0, segs, spray);

      for (int i = 0; i < segs; i++) {
         float phaseB = phase0 + (i + 1) * perSeg;
         float wB = half * bulgeAt(phaseB, amp) * flareAt(i + 1, segs, spray);
         int row = ((int)((i * seg + uScroll) / 3.0F * 16.0F) % 16 + 16) % 16;
         float v0 = 0.5F + 0.03125F * row;
         float v1 = v0 + 0.03125F;
         float y0 = i == 0 ? offset : seg * i;
         float y1 = seg * (i + 1);
         vertex(vc, m, n, -wA, y0, 0.0F, minU, v0, packedLight, fade);
         vertex(vc, m, n, -wB, y1, 0.0F, minU, v1, packedLight, fade);
         vertex(vc, m, n, wB, y1, 0.0F, maxU, v1, packedLight, fade);
         vertex(vc, m, n, wA, y0, 0.0F, maxU, v0, packedLight, fade);
         wA = wB;
      }
   }

   private static float bulge(float phase) {
      return 0.89F + 0.31F * Mth.sin(phase);
   }

   private static float bulgeAt(float phase, float amp) {
      return 0.89F + amp * Mth.sin(phase);
   }

   private static float flareAt(int segIndex, int segs, boolean spray) {
      if (spray && segs > 0) {
         float t = Math.min(1.0F, (float)segIndex / segs);
         return 0.78F + 0.62F * t;
      } else {
         return 1.0F;
      }
   }

   private void renderEnd(int frame, Direction side, float scale, PoseStack poseStack, VertexConsumer vc, int packedLight, float fade) {
      float radius = 0.315F * scale;
      poseStack.pushPose();
      poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
      this.renderFlatQuad(frame, radius, poseStack, vc, packedLight, fade);
      poseStack.popPose();
      if (side != null) {
         poseStack.pushPose();
         Quaternionf sideQuat = new Quaternionf(side.getRotation());
         sideQuat.mul(quat(90.0F, 0.0F, 0.0F));
         poseStack.mulPose(sideQuat);
         poseStack.translate(0.0F, 0.0F, -0.01F);
         this.renderFlatQuad(frame, radius, poseStack, vc, packedLight, fade);
         poseStack.popPose();
      }
   }

   private void renderFlatQuad(int frame, float radius, PoseStack poseStack, VertexConsumer vc, int packedLight, float fade) {
      float minU = 0.0F + 0.0625F * frame;
      float minV = 0.0F;
      float maxU = minU + 0.0625F;
      float maxV = minV + 0.5F;
      Pose pose = poseStack.last();
      Matrix4f m = pose.pose();
      Matrix3f n = pose.normal();
      vertex(vc, m, n, -radius, -radius, 0.0F, minU, minV, packedLight, fade);
      vertex(vc, m, n, -radius, radius, 0.0F, minU, maxV, packedLight, fade);
      vertex(vc, m, n, radius, radius, 0.0F, maxU, maxV, packedLight, fade);
      vertex(vc, m, n, radius, -radius, 0.0F, maxU, minV, packedLight, fade);
   }

   private static Quaternionf quat(float x, float y, float z) {
      return new Quaternionf().rotationXYZ((float)Math.toRadians(x), (float)Math.toRadians(y), (float)Math.toRadians(z));
   }

   private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, float u, float v, int packedLight, float alpha) {
      vc.vertex(m, x, y, z)
         .color(1.0F, 1.0F, 1.0F, Math.max(0.0F, Math.min(1.0F, alpha)))
         .uv(u, v)
         .overlayCoords(OverlayTexture.NO_OVERLAY)
         .uv2(packedLight)
         .normal(n, 0.0F, 1.0F, 0.0F)
         .endVertex();
   }
}

