package com.myx00x.maid.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.yimeng261.maidspell.utils.MaidHardRemovalProtection", remap = false)
public class SpellHardRemovalGate {
   @Inject(method = "shouldBlockSetRemoved", at = @At("HEAD"), remap = false, cancellable = true, require = 0)
   private static void shenlin.allowSetRemoved(Entity entity, RemovalReason reason, CallbackInfoReturnable<Boolean> cir) {
      cir.setReturnValue(false);
   }

   @Inject(method = "shouldBlockUntrustedHardRemoval", at = @At("HEAD"), remap = false, cancellable = true, require = 0)
   private static void shenlin.allowUntrusted(Entity entity, RemovalReason reason, CallbackInfoReturnable<Boolean> cir) {
      cir.setReturnValue(false);
   }

   @Inject(method = "isProtectedMaid", at = @At("HEAD"), remap = false, cancellable = true, require = 0)
   private static void shenlin.notProtected(EntityMaid maid, CallbackInfoReturnable<Boolean> cir) {
      cir.setReturnValue(false);
   }

   @Inject(method = "handleMaidLeaveLevel", at = @At("HEAD"), remap = false, cancellable = true, require = 0)
   private static void shenlin.allowLeaveLevel(EntityMaid maid, CallbackInfoReturnable<Boolean> cir) {
      cir.setReturnValue(false);
   }

   @Inject(method = "rememberProtected", at = @At("HEAD"), remap = false, cancellable = true, require = 0)
   private static void shenlin.noSnapshot(EntityMaid maid, CallbackInfo ci) {
      ci.cancel();
   }
}

