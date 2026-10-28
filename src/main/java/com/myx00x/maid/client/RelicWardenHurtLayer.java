package com.myx00x.maid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.myx00x.maid.mob.RelicWardenEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class RelicWardenHurtLayer extends RenderLayer<RelicWardenEntity, RelicWardenModel> {
   private static final RenderType HURT_BODY = RenderType.eyes(
      ResourceLocation.fromNamespaceAndPath("goddess_of_maids", "textures/entity/relic_warden_hurt_body.png")
   );
   private static final float BLUE_R = 0.25F;
   private static final float BLUE_G = 0.55F;
   private static final float BLUE_B = 1.0F;
   private static final int FULL_LIGHT = 15728640;

   public RelicWardenHurtLayer(RenderLayerParent<RelicWardenEntity, RelicWardenModel> parent) {
      super(parent);
   }

   public void render(
      PoseStack pose,
      MultiBufferSource buffer,
      int packedLight,
      RelicWardenEntity entity,
      float limbSwing,
      float limbSwingAmount,
      float partialTicks,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
   ) {
      if (!entity.isInvisible() && !entity.isDeadOrDying()) {
         float glow = Mth.clamp(entity.hurtGlow(partialTicks), 0.0F, 1.0F);
         if (!(glow <= 0.01F)) {
            VertexConsumer vertex = buffer.getBuffer(HURT_BODY);
            ((RelicWardenModel)this.getParentModel()).renderToBuffer(pose, vertex, 15728640, OverlayTexture.NO_OVERLAY, 0.25F * glow, 0.55F * glow, 1.0F * glow, glow);
         }
      }
   }
}

