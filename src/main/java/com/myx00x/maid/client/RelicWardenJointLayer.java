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

public class RelicWardenJointLayer extends RenderLayer<RelicWardenEntity, RelicWardenModel> {
   private static final RenderType JOINT_HEAD = RenderType.eyes(
      ResourceLocation.fromNamespaceAndPath("goddess_of_maids", "textures/entity/relic_warden_joint_head.png")
   );
   private static final RenderType JOINT_BODY = RenderType.eyes(
      ResourceLocation.fromNamespaceAndPath("goddess_of_maids", "textures/entity/relic_warden_joint_body.png")
   );
   private static final float BLUE_R = 0.25F;
   private static final float BLUE_G = 0.55F;
   private static final float BLUE_B = 1.0F;
   private static final float RED_G = 0.15F;
   private static final float RED_B = 0.12F;
   private static final int FULL_LIGHT = 15728640;

   public RelicWardenJointLayer(RenderLayerParent<RelicWardenEntity, RelicWardenModel> parent) {
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
         float head = entity.jointHeadGlow();
         if (head > 0.01F) {
            VertexConsumer vertex = buffer.getBuffer(JOINT_HEAD);
            ((RelicWardenModel)this.getParentModel())
               .renderToBuffer(
                  pose, vertex, 15728640, OverlayTexture.NO_OVERLAY, tint(head, 0.25F), tint(head, 0.55F), tint(head, 1.0F), Mth.clamp(head, 0.0F, 1.0F)
               );
         }

         float body = entity.jointBodyGlow();
         if (body > 0.01F) {
            float red = Mth.clamp(entity.jointBodyRed(), 0.0F, 1.0F);
            float r = Mth.lerp(red, 0.25F, 1.0F);
            float g = Mth.lerp(red, 0.55F, 0.15F);
            float b = Mth.lerp(red, 1.0F, 0.12F);
            VertexConsumer vertex = buffer.getBuffer(JOINT_BODY);
            ((RelicWardenModel)this.getParentModel())
               .renderToBuffer(pose, vertex, 15728640, OverlayTexture.NO_OVERLAY, tint(body, r), tint(body, g), tint(body, b), Mth.clamp(body, 0.0F, 1.0F));
         }
      }
   }

   private static float tint(float glow, float base) {
      float flat = Mth.clamp(glow * base, 0.0F, 1.0F);
      float over = Mth.clamp(glow - 1.0F, 0.0F, 1.0F);
      return Mth.lerp(over * 0.85F, flat, 1.0F);
   }
}

