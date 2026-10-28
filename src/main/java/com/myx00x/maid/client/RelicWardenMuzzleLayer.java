package com.myx00x.maid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.myx00x.maid.mob.RelicWardenEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class RelicWardenMuzzleLayer extends EyesLayer<RelicWardenEntity, RelicWardenModel> {
   private static final RenderType MUZZLE = RenderType.eyes(
      ResourceLocation.fromNamespaceAndPath("goddess_of_maids", "textures/entity/relic_warden_muzzle.png")
   );

   public RelicWardenMuzzleLayer(RenderLayerParent<RelicWardenEntity, RelicWardenModel> parent) {
      super(parent);
   }

   public RenderType renderType() {
      return MUZZLE;
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
         float glow = entity.muzzleGlow();
         if (!(glow <= 0.01F)) {
            float white = entity.lampWhite();
            float cr = glow * (1.0F - 0.45F * white);
            float cg = glow * (1.0F - 0.2F * white);
            float cb = glow;
            VertexConsumer vertex = buffer.getBuffer(MUZZLE);
            ((RelicWardenModel)this.getParentModel()).renderToBuffer(pose, vertex, 15728640, OverlayTexture.NO_OVERLAY, cr, cg, cb, glow);
         }
      }
   }
}

