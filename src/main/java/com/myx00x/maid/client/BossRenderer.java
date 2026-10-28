package com.myx00x.maid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.boss.ShenlinBossEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

public class BossRenderer extends EntityRenderer<ShenlinBossEntity> {
   private static final ResourceLocation SKIN = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
   private static Boolean wasVisible;

   public BossRenderer(Context context) {
      super(context);
   }

   public void render(ShenlinBossEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      boolean show = BossShellGuard.isVisible(entity.getId());
      if (wasVisible == null || wasVisible != show) {
         wasVisible = show;
         ShenlinMod.LOGGER.info("[shenlin] renderer {} id={}", show ? "on" : "off", entity.getId());
      }

      if (show) {
         AABB box = new AABB(-0.6, 0.0, -0.6, 0.6, 2.4, 0.6);

         try {
            VertexConsumer lines = buffer.getBuffer(RenderType.lines());
            LevelRenderer.renderLineBox(poseStack, lines, box, 1.0F, 1.0F, 1.0F, 1.0F);
         } catch (Throwable var10) {
         }

         BossShellGuard.markPainted(entity.getId());
      }
   }

   public ResourceLocation getTextureLocation(ShenlinBossEntity entity) {
      return SKIN;
   }
}

