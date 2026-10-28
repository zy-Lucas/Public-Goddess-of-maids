package com.myx00x.maid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.myx00x.maid.mob.WardenEnergyBoltEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class WardenEnergyBoltRenderer extends EntityRenderer<WardenEnergyBoltEntity> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("goddess_of_maids", "textures/entity/beam/warden_beam_cold.png");
   private static final float HALF = 0.05F;
   private static final float HALF_LEN = 0.15F;
   private static final float V0 = 0.65625F;
   private static final float V1 = 0.6875F;
   private static final Vector3f AXIS_Y = new Vector3f(0.0F, 1.0F, 0.0F);

   public WardenEnergyBoltRenderer(Context context) {
      super(context);
   }

   public ResourceLocation getTextureLocation(WardenEnergyBoltEntity entity) {
      return TEXTURE;
   }

   public void render(WardenEnergyBoltEntity bolt, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      Vec3 d = bolt.dir();
      if (!(d.lengthSqr() < 1.0E-6)) {
         Vector3f dir = new Vector3f((float)d.x, (float)d.y, (float)d.z).normalize();
         VertexConsumer vc = buffer.getBuffer(ModRenderTypes.GLOWING.apply(TEXTURE));
         poseStack.pushPose();
         poseStack.mulPose(new Quaternionf().rotationTo(AXIS_Y, dir));
         float camYaw = SafeCamera.yRot(Minecraft.getInstance());
         poseStack.mulPose(new Quaternionf().rotationY((float)Math.toRadians(camYaw + 90.0F)));
         plate(vc, poseStack);
         poseStack.mulPose(new Quaternionf().rotationY((float) (Math.PI / 2)));
         plate(vc, poseStack);
         poseStack.popPose();
      }
   }

   private static void plate(VertexConsumer vc, PoseStack poseStack) {
      Pose pose = poseStack.last();
      Matrix4f m = pose.pose();
      Matrix3f n = pose.normal();
      vertex(vc, m, n, -0.05F, -0.15F, 0.0F, 0.0F, 0.65625F);
      vertex(vc, m, n, -0.05F, 0.15F, 0.0F, 0.0F, 0.6875F);
      vertex(vc, m, n, 0.05F, 0.15F, 0.0F, 0.078125F, 0.6875F);
      vertex(vc, m, n, 0.05F, -0.15F, 0.0F, 0.078125F, 0.65625F);
   }

   private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, float u, float v) {
      vc.vertex(m, x, y, z)
         .color(1.0F, 1.0F, 1.0F, 1.0F)
         .uv(u, v)
         .overlayCoords(OverlayTexture.NO_OVERLAY)
         .uv2(15728880)
         .normal(n, 0.0F, 1.0F, 0.0F)
         .endVertex();
   }
}

