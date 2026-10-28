package com.myx00x.maid.network;

import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.item.CoordinateDisplayItem;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public final class GhostPacket {
   private final boolean on;

   public GhostPacket(boolean on) {
      this.on = on;
   }

   public static void encode(GhostPacket msg, FriendlyByteBuf buf) {
      buf.writeBoolean(msg.on);
   }

   public static GhostPacket decode(FriendlyByteBuf buf) {
      return new GhostPacket(buf.readBoolean());
   }

   public static void handle(GhostPacket msg, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.setPacketHandled(true);
      ServerPlayer player = context.getSender();
      if (player != null && !player.level().isClientSide) {
         context.enqueueWork(() -> {
            try {
               CoordinateDisplayItem.setGhost(player, msg.on);
            } catch (Throwable t) {
               ShenlinMod.LOGGER.error("[shenlin] ghost toggle step failed", t);
            }
         });
      }
   }
}

