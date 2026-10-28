package com.myx00x.maid.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.registry.ModItems;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public class ItemHoldFaceMixin {
   @Unique
   private static final float HOLD_DIST = 0.62F;
   @Unique
   private static final float HOLD_DROP = 0.3F;
   @Unique
   private static long shenlin.lastLog;

   @Inject(method = "m_115143_", remap = false, at = @At("HEAD"))
   private void shenlin.faceCamera(
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
      if (shenlin.ours(stack)) {
         boolean firstPerson = ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
         boolean ground = ctx == ItemDisplayContext.GROUND;
         if (firstPerson || ground) {
            Matrix4f m = poseStack.last().pose();
            float beforeX = m.m30();
            float beforeY = m.m31();
            float beforeZ = m.m32();
            m.identity();
            if (firstPerson) {
               m.m31(-0.3F);
               m.m32(-0.62F);
            } else {
               m.m30(beforeX);
               m.m31(beforeY);
               m.m32(beforeZ);
               Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
               if (camera != null && camera.isInitialized()) {
                  poseStack.mulPose(camera.rotation());
               }
            }

            long now = System.currentTimeMillis();
            if (now - shenlin.lastLog > 1000L) {
               shenlin.lastLog = now;
               ShenlinMod.LOGGER
                  .info(
                     "[shenlin] face camera item={} ctx={} was=({},{},{}) {}",
                     new Object[]{stack.getItem(), ctx, beforeX, beforeY, beforeZ, firstPerson ? "now=(0,-0.3,-0.62)" : "kept place, faced camera"}
                  );
            }
         }
      }
   }

   @Unique
   private static boolean shenlin.ours(ItemStack stack) {
      return stack.isEmpty()
         ? false
         : stack.is((Item)ModItems.GUARDIAN_CORE.get())
            || stack.is((Item)ModItems.PROTECTION_CORE.get())
            || stack.is((Item)ModItems.BLACK_HOLE_SCROLL.get())
            || stack.is((Item)ModItems.GUARDIAN_SHIELD.get())
            || stack.is((Item)ModItems.DEVOTED_HEART.get())
            || stack.is((Item)ModItems.ENERGY_CORE.get())
            || stack.is((Item)ModItems.HOVER_CANNON.get())
            || stack.is((Item)ModItems.TELEPORT_WORMHOLE.get());
   }
}

