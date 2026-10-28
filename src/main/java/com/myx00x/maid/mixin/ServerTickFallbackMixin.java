package com.myx00x.maid.mixin;

import com.myx00x.maid.event.ServerTickGuard;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MinecraftServer.class, remap = false)
public abstract class ServerTickFallbackMixin {
   @Inject(method = "m_5705_(Ljava/util/function/BooleanSupplier;)V", at = @At("TAIL"), remap = false)
   private void shenlin.vanillaTickFallback(CallbackInfo ci) {
      ServerTickGuard.viaFallback((MinecraftServer)this);
   }
}

