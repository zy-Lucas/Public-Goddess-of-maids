package com.myx00x.maid.network;

import com.myx00x.maid.boss.ShenlinBossKeeper;
import com.myx00x.maid.client.Damnation;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public final class DamnationPacket {
   private final int ticks;
   private final String who;

   public DamnationPacket(int ticks, String who) {
      this.ticks = ticks;
      this.who = who == null ? "" : who;
   }

   public static void encode(DamnationPacket msg, FriendlyByteBuf buf) {
      buf.writeVarInt(msg.ticks);
      buf.writeUtf(msg.who, 64);
   }

   public static DamnationPacket decode(FriendlyByteBuf buf) {
      return new DamnationPacket(buf.readVarInt(), buf.readUtf(64));
   }

   public static void handle(DamnationPacket msg, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.setPacketHandled(true);
      if (!context.getDirection().getReceptionSide().isClient()) {
         ServerPlayer sender = context.getSender();
         if (msg.ticks <= 0 && sender != null) {
            ShenlinBossKeeper.release(sender);
         }
      } else {
         if (msg.ticks <= 0) {
            Damnation.clear();
         } else {
            Damnation.begin(msg.who);
         }
      }
   }
}

