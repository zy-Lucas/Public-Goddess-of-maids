package com.myx00x.maid.mixin;

import com.myx00x.maid.client.BossMusicPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SoundEngine.class, remap = false)
public class SoundEngineStopGuard {
   @Inject(method = "m_120274_(Lnet/minecraft/client/resources/sounds/SoundInstance;)V", at = @At("HEAD"), remap = false, cancellable = true)
   private void shenlin.keepBossTheme(SoundInstance sound, CallbackInfo ci) {
      if (BossMusicPlayer.isTheme(sound)) {
         ci.cancel();
      }
   }
}

