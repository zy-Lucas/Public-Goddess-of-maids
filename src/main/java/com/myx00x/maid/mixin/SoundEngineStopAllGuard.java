package com.myx00x.maid.mixin;

import com.myx00x.maid.client.BossMusicPlayer;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SoundEngine.class, remap = false)
public class SoundEngineStopAllGuard {
   @Inject(method = "m_120311_", at = @At("HEAD"), cancellable = true)
   private void shenlin.keepTheme(CallbackInfo ci) {
      try {
         if (BossMusicPlayer.keepThroughStopAll()) {
            ci.cancel();
         }
      } catch (Throwable var3) {
      }
   }
}

