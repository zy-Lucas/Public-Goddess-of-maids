package com.myx00x.maid.mixin;

import com.myx00x.maid.client.Damnation;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LocalPlayer.class, remap = false)
public class LocalPlayerDamnationMixin {
   @Inject(method = "m_8119_", at = @At("HEAD"), remap = false)
   private void shenlin.holdDamned(CallbackInfo ci) {
      LocalPlayer self = (LocalPlayer)this;
      if (Damnation.active() && self.input != null) {
         self.input.leftImpulse = 0.0F;
         self.input.forwardImpulse = 0.0F;
         self.input.jumping = false;
         self.input.shiftKeyDown = false;
         Damnation.hold(self);
      }
   }
}

