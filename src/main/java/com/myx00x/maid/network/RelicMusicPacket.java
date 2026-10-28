package com.myx00x.maid.network;

import com.myx00x.maid.client.RelicMusicPlayer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public final class RelicMusicPacket {
   private final boolean inside;
   private final double gapMin;
   private final double gapMax;
   private final double range;

   public RelicMusicPacket(boolean inside, double gapMin, double gapMax, double range) {
      this.inside = inside;
      this.gapMin = gapMin;
      this.gapMax = gapMax;
      this.range = range;
   }

   public static void encode(RelicMusicPacket msg, FriendlyByteBuf buf) {
      buf.writeBoolean(msg.inside);
      buf.writeDouble(msg.gapMin);
      buf.writeDouble(msg.gapMax);
      buf.writeDouble(msg.range);
   }

   public static RelicMusicPacket decode(FriendlyByteBuf buf) {
      return new RelicMusicPacket(buf.readBoolean(), buf.readDouble(), buf.readDouble(), buf.readDouble());
   }

   public static void handle(RelicMusicPacket msg, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.setPacketHandled(true);
      if (context.getDirection().getReceptionSide().isClient()) {
         RelicMusicPlayer.setInside(msg.inside, msg.gapMin, msg.gapMax, msg.range);
      }
   }
}

