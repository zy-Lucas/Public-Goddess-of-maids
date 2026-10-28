package com.myx00x.maid.network;

import com.myx00x.maid.boss.ShenlinBossKeeper;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public final class BossShellLostPacket {
   private final int id;

   public BossShellLostPacket(int id) {
      this.id = id;
   }

   public static void encode(BossShellLostPacket msg, FriendlyByteBuf buf) {
      buf.writeVarInt(msg.id);
   }

   public static BossShellLostPacket decode(FriendlyByteBuf buf) {
      return new BossShellLostPacket(buf.readVarInt());
   }

   public static void handle(BossShellLostPacket msg, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.setPacketHandled(true);
      if (!context.getDirection().getReceptionSide().isClient()) {
         ShenlinBossKeeper.shellLost(msg.id);
      }
   }
}

