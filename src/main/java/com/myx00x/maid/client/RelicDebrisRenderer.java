package com.myx00x.maid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.myx00x.maid.mob.RelicDebrisEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public class RelicDebrisRenderer extends EntityRenderer<RelicDebrisEntity> {
   private final BlockRenderDispatcher blockRenderer;

   public RelicDebrisRenderer(Context context) {
      super(context);
      this.blockRenderer = context.getBlockRenderDispatcher();
      this.shadowRadius = 0.0F;
   }

   public void render(RelicDebrisEntity entity, float entityYaw, float partialTicks, PoseStack pose, MultiBufferSource buffer, int packedLight) {
      BlockState state = entity.blockState();
      if (state.getRenderShape() == RenderShape.MODEL) {
         float scale = entity.debrisScale();
         float age = (float)entity.tickCount / RelicDebrisEntity.lifeTicks();
         float fade = age <= 0.8F ? 1.0F : Math.max(0.0F, (1.0F - age) / 0.2F);
         float grown = scale * fade;
         pose.pushPose();
         pose.translate(-0.5 * grown, 0.0, -0.5 * grown);
         pose.scale(grown, grown, grown);
         this.blockRenderer.renderSingleBlock(state, pose, buffer, packedLight, OverlayTexture.NO_OVERLAY);
         pose.popPose();
         super.render(entity, entityYaw, partialTicks, pose, buffer, packedLight);
      }
   }

   public ResourceLocation getTextureLocation(RelicDebrisEntity entity) {
      return TextureAtlas.LOCATION_BLOCKS;
   }
}

