package com.myx00x.maid.mixin;

import com.myx00x.maid.boss.ShellGate;
import com.myx00x.maid.boss.ShenlinBossEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.entity.EntityAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.level.entity.PersistentEntitySectionManager.Callback", remap = false)
public class ShellGateCallbackMixin {
   @Shadow(remap = false)
   @Final
   private EntityAccess entity;

   @Inject(method = "m_142472_(Lnet/minecraft/world/entity/Entity.RemovalReason;)V", at = @At("HEAD"), remap = false, cancellable = true)
   private void shenlin.holdShell(RemovalReason reason, CallbackInfo ci) {
      if (this.entity instanceof ShenlinBossEntity shell && ShellGate.hold(shell, "on-remove")) {
         ci.cancel();
      }
   }
}

