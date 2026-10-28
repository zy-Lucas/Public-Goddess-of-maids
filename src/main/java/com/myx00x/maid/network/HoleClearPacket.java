package com.myx00x.maid.network;

import com.myx00x.maid.client.BlackHoleEffect;
import com.myx00x.maid.client.HoleSounds;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public final class HoleClearPacket {
   private final double x;
   private final double y;
   private final double z;
   private final float radius;

   public HoleClearPacket(double x, double y, double z, float radius) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.radius = radius;
   }

   public static void encode(HoleClearPacket msg, FriendlyByteBuf buf) {
      buf.writeDouble(msg.x);
      buf.writeDouble(msg.y);
      buf.writeDouble(msg.z);
      buf.writeFloat(msg.radius);
   }

   public static HoleClearPacket decode(FriendlyByteBuf buf) {
      return new HoleClearPacket(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat());
   }

   public static void handle(HoleClearPacket msg, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.setPacketHandled(true);
      if (context.getDirection().getReceptionSide().isClient()) {
         BlackHoleEffect.clearNear(msg.x, msg.y, msg.z, msg.radius);
         HoleSounds.fadeNear(msg.x, msg.y, msg.z, msg.radius);
      }
   }
}

