package com.myx00x.maid.mixin;

import com.myx00x.maid.boss.ShellGate;
import com.myx00x.maid.boss.ShenlinBossEntity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityLookup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityLookup.class, remap = false)
public class ShellGateLookupMixin {
   @Inject(method = "m_156822_(Lnet/minecraft/world/level/entity/EntityAccess;)V", at = @At("HEAD"), remap = false, cancellable = true)
   private void shenlin.holdShell(EntityAccess entityAccess, CallbackInfo ci) {
      if (entityAccess instanceof ShenlinBossEntity shell && ShellGate.hold(shell, "lookup")) {
         ci.cancel();
      }
   }
}

