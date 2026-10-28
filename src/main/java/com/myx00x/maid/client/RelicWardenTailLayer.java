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

public class RelicWardenTailLayer extends EyesLayer<RelicWardenEntity, RelicWardenModel> {
   private static final RenderType TAIL = RenderType.eyes(
      ResourceLocation.fromNamespaceAndPath("goddess_of_maids", "textures/entity/relic_warden_tail_glow.png")
   );
   private static final boolean DIAG = false;
   private static int LOGGED = -1;

   public RelicWardenTailLayer(RenderLayerParent<RelicWardenEntity, RelicWardenModel> parent) {
      super(parent);
   }

   public RenderType renderType() {
      return TAIL;
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
         if (entity.chargeActive()) {
            VertexConsumer vertex = buffer.getBuffer(TAIL);
            ((RelicWardenModel)this.getParentModel()).renderToBuffer(pose, vertex, 15728640, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
         }
      }
   }
}

