package com.myx00x.maid.mixin;

import com.myx00x.maid.client.WardenHealthBar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossHealthOverlay.class)
public class BossBarOffsetMixin {
   @Inject(method = "m_280652_", at = @At("HEAD"), remap = false)
   private void shenlin.pushPose(GuiGraphics graphics, CallbackInfo ci) {
      graphics.pose().pushPose();
      if (WardenHealthBar.visibleNow()) {
         int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
         graphics.pose().translate(0.0F, WardenHealthBar.shiftBelowPx(width), 0.0F);
      }
   }

   @Inject(method = "m_280652_", at = @At("RETURN"), remap = false)
   private void shenlin.popPose(GuiGraphics graphics, CallbackInfo ci) {
      graphics.pose().popPose();
   }
}

