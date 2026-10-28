package com.myx00x.maid.mixin;

import com.myx00x.maid.client.Damnation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameRenderer.class, remap = false)
public class GameRendererDamnationMixin {
   @Inject(method = "m_109093_", at = @At("TAIL"), remap = false)
   private void shenlin.damnedScreen(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo ci) {
      Damnation.paint(Minecraft.getInstance());
   }
}

