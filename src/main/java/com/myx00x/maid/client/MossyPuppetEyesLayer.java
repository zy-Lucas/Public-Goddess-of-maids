package com.myx00x.maid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.myx00x.maid.mob.MossyPuppetBase;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class MossyPuppetEyesLayer extends EyesLayer<MossyPuppetBase, MossyPuppetModel> {
   private static final RenderType EYES = RenderType.eyes(
      ResourceLocation.fromNamespaceAndPath("goddess_of_maids", "textures/entity/mossy_puppet_eyes.png")
   );

   public MossyPuppetEyesLayer(RenderLayerParent<MossyPuppetBase, MossyPuppetModel> parent) {
      super(parent);
   }

   public RenderType renderType() {
      return EYES;
   }

   public void render(
      PoseStack pose,
      MultiBufferSource buffer,
      int packedLight,
      MossyPuppetBase entity,
      float limbSwing,
      float limbSwingAmount,
      float partialTicks,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
   ) {
      if (!entity.isInvisible() && !entity.isDeadOrDying()) {
         float glow = 1.0F - entity.dormantLerp(partialTicks);
         if (!(glow <= 0.02F)) {
            VertexConsumer vertex = buffer.getBuffer(EYES);
            ((MossyPuppetModel)this.getParentModel()).renderToBuffer(pose, vertex, 15728640, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, glow);
         }
      }
   }
}

