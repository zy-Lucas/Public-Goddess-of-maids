package com.myx00x.maid.mixin;

import com.myx00x.maid.boss.ShellHold;
import com.myx00x.maid.client.GuardShield;
import it.unimi.dsi.fastutil.ints.IntConsumer;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientPacketListener.class)
public abstract class ClientRemovalHoldMixin {
   @Redirect(
      method = "m_182047_(Lnet/minecraft/network/protocol/game/ClientboundRemoveEntitiesPacket;)V",
      at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/ints/IntList;forEach(Lit/unimi/dsi/fastutil/ints/IntConsumer;)V")
   )
   private void shenlin.keepGuarded(IntList ids, IntConsumer action) {
      int size = ids.size();

      for (int i = 0; i < size; i++) {
         int id = ids.getInt(i);
         if (!GuardShield.holds(id) && !ShellHold.held(id)) {
            action.accept(id);
         }
      }
   }
}

