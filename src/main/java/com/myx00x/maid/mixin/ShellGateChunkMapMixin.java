package com.myx00x.maid.mixin;

import com.myx00x.maid.boss.ShellGate;
import com.myx00x.maid.boss.ShenlinBossEntity;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChunkMap.class, remap = false)
public class ShellGateChunkMapMixin {
   @Inject(method = "m_140331_(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), remap = false, cancellable = true)
   private void shenlin.holdShell(Entity entity, CallbackInfo ci) {
      if (entity instanceof ShenlinBossEntity shell && ShellGate.hold(shell, "chunkmap")) {
         ci.cancel();
      }
   }
}

