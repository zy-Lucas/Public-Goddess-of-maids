package com.myx00x.maid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.myx00x.maid.boss.RuinedPhantasmEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;

public class RuinedPhantasmRenderer extends EntityRenderer<RuinedPhantasmEntity> {
   private static final ResourceLocation SKIN = ResourceLocation.withDefaultNamespace("textures/misc/white.png");

   public RuinedPhantasmRenderer(Context context) {
      super(context);
   }

   public void render(RuinedPhantasmEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
   }

   public boolean shouldRender(RuinedPhantasmEntity entity, Frustum frustum, double camX, double camY, double camZ) {
      return RuinedPhantasmState.aliveNow(entity.getId()) && super.shouldRender(entity, frustum, camX, camY, camZ);
   }

   public ResourceLocation getTextureLocation(RuinedPhantasmEntity entity) {
      return SKIN;
   }
}

