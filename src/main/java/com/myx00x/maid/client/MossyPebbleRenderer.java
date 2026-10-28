package com.myx00x.maid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.myx00x.maid.mob.MossyPebbleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.RenderTypeHelper;
import net.minecraftforge.client.model.data.ModelData;

public class MossyPebbleRenderer extends EntityRenderer<MossyPebbleEntity> {
   private static final float PEBBLE_SCALE = 0.45F;
   private static final BlockState PEBBLE_STATE = Blocks.MOSSY_COBBLESTONE.defaultBlockState();

   public MossyPebbleRenderer(Context context) {
      super(context);
   }

   public void render(MossyPebbleEntity entity, float entityYaw, float partialTicks, PoseStack pose, MultiBufferSource buffer, int packedLight) {
      pose.pushPose();
      pose.scale(0.45F, 0.45F, 0.45F);
      pose.translate(-0.5, -0.5, -0.5);
      BlockPos pos = BlockPos.containing(entity.getX(), entity.getBoundingBox().maxY, entity.getZ());
      BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(PEBBLE_STATE);
      long seed = PEBBLE_STATE.getSeed(entity.blockPosition());

      for (RenderType type : model.getRenderTypes(PEBBLE_STATE, RandomSource.create(seed), ModelData.EMPTY)) {
         RenderType moving = RenderTypeHelper.getMovingBlockRenderType(type);
         Minecraft.getInstance()
            .getBlockRenderer()
            .getModelRenderer()
            .tesselateBlock(
               entity.level(),
               model,
               PEBBLE_STATE,
               pos,
               pose,
               buffer.getBuffer(moving),
               false,
               RandomSource.create(),
               seed,
               OverlayTexture.NO_OVERLAY,
               ModelData.EMPTY,
               moving
            );
      }

      pose.popPose();
      super.render(entity, entityYaw, partialTicks, pose, buffer, packedLight);
   }

   public ResourceLocation getTextureLocation(MossyPebbleEntity entity) {
      return InventoryMenu.BLOCK_ATLAS;
   }
}

