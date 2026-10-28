package com.myx00x.maid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.myx00x.maid.mob.MossyPuppetBase;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;

public class MossyPuppetRenderer extends MobRenderer<MossyPuppetBase, MossyPuppetModel> {
   private static final ResourceLocation TEXTURE_AWAKE = ResourceLocation.fromNamespaceAndPath("goddess_of_maids", "textures/entity/mossy_puppet.png");
   private static final ResourceLocation TEXTURE_DORMANT = ResourceLocation.fromNamespaceAndPath("goddess_of_maids", "textures/entity/mossy_puppet_dormant.png");

   public MossyPuppetRenderer(Context context) {
      super(context, new MossyPuppetModel(context.bakeLayer(MossyPuppetModel.LAYER)), 0.35F);
      this.addLayer(new MossyPuppetEyesLayer(this));
   }

   public ResourceLocation getTextureLocation(MossyPuppetBase entity) {
      return entity.isDormant() ? TEXTURE_DORMANT : TEXTURE_AWAKE;
   }

   protected void scale(MossyPuppetBase entity, PoseStack poseStack, float partialTicks) {
      poseStack.scale(0.5F, 0.5F, 0.5F);
   }

   protected void setupRotations(MossyPuppetBase entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks) {
      super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks);
      if (entity.walkAnimation.speed() >= 1.0E-5F) {
         float period = 13.0F;
         float phase = entity.walkAnimation.position(partialTicks) + 6.0F;
         float sway = (Math.abs(phase % period - 6.5F) - 3.25F) / 3.25F;
         poseStack.mulPose(Axis.ZP.rotationDegrees(6.5F * sway));
      }
   }
}

