package com.myx00x.maid.network;

import com.myx00x.maid.client.FlashEffect;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public final class FlashPacket {
   private final int ticks;
   private final boolean peaked;

   public FlashPacket(int ticks) {
      this(ticks, false);
   }

   public FlashPacket(int ticks, boolean peaked) {
      this.ticks = ticks;
      this.peaked = peaked;
   }

   public static void encode(FlashPacket msg, FriendlyByteBuf buf) {
      buf.writeVarInt(msg.ticks);
      buf.writeBoolean(msg.peaked);
   }

   public static FlashPacket decode(FriendlyByteBuf buf) {
      return new FlashPacket(buf.readVarInt(), buf.readBoolean());
   }

   public static void handle(FlashPacket msg, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.setPacketHandled(true);
      if (context.getDirection().getReceptionSide().isClient()) {
         if (msg.peaked) {
            FlashEffect.triggerSlam(msg.ticks);
         } else {
            FlashEffect.trigger(msg.ticks);
         }
      }
   }
}

