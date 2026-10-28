package com.myx00x.maid.network;

import com.myx00x.maid.client.GuardShield;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public final class GuardPassPacket {
   private final int id;

   public GuardPassPacket(int id) {
      this.id = id;
   }

   public static void encode(GuardPassPacket msg, FriendlyByteBuf buf) {
      buf.writeVarInt(msg.id);
   }

   public static GuardPassPacket decode(FriendlyByteBuf buf) {
      return new GuardPassPacket(buf.readVarInt());
   }

   public static void handle(GuardPassPacket msg, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.setPacketHandled(true);
      if (context.getDirection().getReceptionSide().isClient()) {
         GuardShield.release(msg.id);
      }
   }
}

