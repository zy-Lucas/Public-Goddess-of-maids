package com.myx00x.maid.mixin;

import com.myx00x.maid.client.BossMusicPlayer;
import com.myx00x.maid.client.RelicMusicPlayer;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.sounds.Music;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MusicManager.class, remap = false)
public class MusicManagerSilence {
   @Inject(method = "m_120184_(Lnet/minecraft/sounds/Music;)V", at = @At("HEAD"), remap = false, cancellable = true)
   private void shenlin.yieldToBoss(Music music, CallbackInfo ci) {
      if (BossMusicPlayer.bossNearby() || RelicMusicPlayer.silencing()) {
         ci.cancel();
      }
   }
}

