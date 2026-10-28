package com.myx00x.maid.mixin;

import com.myx00x.maid.combat.DeleteGate;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerGamePacketListenerImpl.class, remap = false)
public class PacketSendRemovalGateMixin {
   @Inject(method = "m_9829_(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), remap = false, cancellable = true)
   private void shenlin.holdRemoval(Packet<?> packet, CallbackInfo ci) {
      if (DeleteGate.holdsAny(packet)) {
         ci.cancel();
      }
   }
}

