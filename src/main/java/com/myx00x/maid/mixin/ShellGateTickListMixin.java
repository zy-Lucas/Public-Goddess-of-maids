package com.myx00x.maid.mixin;

import com.myx00x.maid.boss.ShellGate;
import com.myx00x.maid.boss.ShenlinBossEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTickList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityTickList.class, remap = false)
public class ShellGateTickListMixin {
   @Inject(method = "m_156912_(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), remap = false, cancellable = true)
   private void shenlin.holdShell(Entity entity, CallbackInfo ci) {
      if (entity instanceof ShenlinBossEntity shell && ShellGate.hold(shell, "ticklist")) {
         ci.cancel();
      }
   }
}

