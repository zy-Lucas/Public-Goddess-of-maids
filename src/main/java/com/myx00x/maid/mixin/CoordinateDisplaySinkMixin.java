package com.myx00x.maid.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.myx00x.maid.client.GhostHoldWatcher;
import com.myx00x.maid.registry.ModItems;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public class CoordinateDisplaySinkMixin {
   @Unique
   private static final float SINK_DEPTH = 0.1F;

   @Inject(method = "m_115143_", remap = false, at = @At("HEAD"))
   private void shenlin.sinkWhileHolding(
      ItemStack stack,
      ItemDisplayContext ctx,
      boolean leftHand,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int light,
      int overlay,
      BakedModel model,
      CallbackInfo ci
   ) {
      if (ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
         if (stack.is((Item)ModItems.COORDINATE_DISPLAY.get())) {
            float amount = GhostHoldWatcher.sinkAmount();
            if (!(amount <= 0.0F)) {
               poseStack.translate(0.0F, -0.1F * amount, 0.0F);
            }
         }
      }
   }
}

