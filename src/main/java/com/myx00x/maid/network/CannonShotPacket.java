package com.myx00x.maid.network;

import com.myx00x.maid.client.CannonShotEffect;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public final class CannonShotPacket {
   private final double fx;
   private final double fy;
   private final double fz;
   private final double tx;
   private final double ty;
   private final double tz;
   private final long startTick;

   public CannonShotPacket(double fx, double fy, double fz, double tx, double ty, double tz, long startTick) {
      this.fx = fx;
      this.fy = fy;
      this.fz = fz;
      this.tx = tx;
      this.ty = ty;
      this.tz = tz;
      this.startTick = startTick;
   }

   public static void encode(CannonShotPacket msg, FriendlyByteBuf buf) {
      buf.writeDouble(msg.fx);
      buf.writeDouble(msg.fy);
      buf.writeDouble(msg.fz);
      buf.writeDouble(msg.tx);
      buf.writeDouble(msg.ty);
      buf.writeDouble(msg.tz);
      buf.writeVarLong(msg.startTick);
   }

   public static CannonShotPacket decode(FriendlyByteBuf buf) {
      return new CannonShotPacket(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readVarLong());
   }

   public static void handle(CannonShotPacket msg, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.setPacketHandled(true);
      if (context.getDirection().getReceptionSide().isClient()) {
         CannonShotEffect.spawn(msg.fx, msg.fy, msg.fz, msg.tx, msg.ty, msg.tz, msg.startTick);
      }
   }
}

