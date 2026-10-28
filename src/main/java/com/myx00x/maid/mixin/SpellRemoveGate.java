package com.myx00x.maid.mixin;

import com.myx00x.maid.ShenlinMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.yimeng261.maidspell.utils.AnchorCoreProtection", remap = false)
public class SpellRemoveGate {
   private static boolean seen;

   @Inject(method = "shouldBlockMaidRemove", at = @At("HEAD"), remap = false, cancellable = true, require = 0)
   private static void shenlin.allow(CallbackInfoReturnable<Boolean> cir) {
      if (!seen) {
         seen = true;
         ShenlinMod.LOGGER.info("[shenlin] hard removal gate taken over");
      }

      cir.setReturnValue(false);
   }

   @Inject(method = "shouldBlockSetRemoved", at = @At("HEAD"), remap = false, cancellable = true, require = 0)
   private static void shenlin.allowSetRemoved(CallbackInfoReturnable<Boolean> cir) {
      cir.setReturnValue(false);
   }
}

