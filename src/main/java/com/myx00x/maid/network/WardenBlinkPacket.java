package com.myx00x.maid.network;

import com.myx00x.maid.client.WardenFlashScreen;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent.Context;

public final class WardenBlinkPacket {
   private final double ax;
   private final double ay;
   private final double az;
   private final double bx;
   private final double by;
   private final double bz;
   private final int ticks;

   public WardenBlinkPacket(double ax, double ay, double az, double bx, double by, double bz, int ticks) {
      this.ax = ax;
      this.ay = ay;
      this.az = az;
      this.bx = bx;
      this.by = by;
      this.bz = bz;
      this.ticks = ticks;
   }

   public static void encode(WardenBlinkPacket msg, FriendlyByteBuf buf) {
      buf.writeDouble(msg.ax);
      buf.writeDouble(msg.ay);
      buf.writeDouble(msg.az);
      buf.writeDouble(msg.bx);
      buf.writeDouble(msg.by);
      buf.writeDouble(msg.bz);
      buf.writeVarInt(msg.ticks);
   }

   public static WardenBlinkPacket decode(FriendlyByteBuf buf) {
      return new WardenBlinkPacket(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readVarInt());
   }

   public static void handle(WardenBlinkPacket msg, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.setPacketHandled(true);
      if (context.getDirection().getReceptionSide().isClient()) {
         WardenFlashScreen.triggerAt(new Vec3(msg.ax, msg.ay, msg.az), new Vec3(msg.bx, msg.by, msg.bz), msg.ticks);
      }
   }
}

