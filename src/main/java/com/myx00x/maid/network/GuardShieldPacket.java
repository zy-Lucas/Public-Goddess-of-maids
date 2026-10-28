package com.myx00x.maid.network;

import com.myx00x.maid.client.GuardShield;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public final class GuardShieldPacket {
   private final int[] ids;
   private final int millis;

   public GuardShieldPacket(int[] ids, int millis) {
      this.ids = ids;
      this.millis = millis;
   }

   public static void encode(GuardShieldPacket msg, FriendlyByteBuf buf) {
      buf.writeVarInt(msg.ids.length);

      for (int id : msg.ids) {
         buf.writeVarInt(id);
      }

      buf.writeVarInt(msg.millis);
   }

   public static GuardShieldPacket decode(FriendlyByteBuf buf) {
      int count = buf.readVarInt();
      int[] ids = new int[count];

      for (int i = 0; i < count; i++) {
         ids[i] = buf.readVarInt();
      }

      return new GuardShieldPacket(ids, buf.readVarInt());
   }

   public static void handle(GuardShieldPacket msg, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.setPacketHandled(true);
      if (context.getDirection().getReceptionSide().isClient()) {
         GuardShield.apply(msg.ids, msg.millis);
      }
   }
}

