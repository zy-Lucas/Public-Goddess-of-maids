package com.myx00x.maid.network;

import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.combat.HateBoard;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public final class MarkPacket {
   private final int entityId;

   public MarkPacket(int entityId) {
      this.entityId = entityId;
   }

   public static void encode(MarkPacket msg, FriendlyByteBuf buf) {
      buf.writeVarInt(msg.entityId);
   }

   public static MarkPacket decode(FriendlyByteBuf buf) {
      return new MarkPacket(buf.readVarInt());
   }

   public static void handle(MarkPacket msg, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.setPacketHandled(true);
      ServerPlayer player = context.getSender();
      if (player != null && !player.level().isClientSide) {
         context.enqueueWork(() -> {
            try {
               HateBoard.accept(player, msg.entityId);
            } catch (Throwable t) {
               ShenlinMod.LOGGER.error("[shenlin] hate mark step failed", t);
            }
         });
      }
   }
}

