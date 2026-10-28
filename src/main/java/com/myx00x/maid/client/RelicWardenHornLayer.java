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

public class RelicWardenHornLayer extends EyesLayer<RelicWardenEntity, RelicWardenModel> {
   private static final RenderType HORN = RenderType.eyes(
      ResourceLocation.fromNamespaceAndPath("goddess_of_maids", "textures/entity/relic_warden_horn.png")
   );

   public RelicWardenHornLayer(RenderLayerParent<RelicWardenEntity, RelicWardenModel> parent) {
      super(parent);
   }

   public RenderType renderType() {
      return HORN;
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
         float glow = entity.hornGlow();
         if (!(glow <= 0.01F)) {
            VertexConsumer vertex = buffer.getBuffer(HORN);
            ((RelicWardenModel)this.getParentModel()).renderToBuffer(pose, vertex, 15728640, OverlayTexture.NO_OVERLAY, glow, glow, glow, glow);
         }
      }
   }
}

