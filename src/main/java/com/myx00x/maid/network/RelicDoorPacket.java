package com.myx00x.maid.network;

import com.myx00x.maid.client.SpaceBreakEffect;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public final class RelicDoorPacket {
   public static final int OPEN = 0;
   public static final int CLOSE = 1;
   public static final int BREAK = 2;
   private final int action;
   private final double x;
   private final double y;
   private final double z;
   private final int ticks;
   private final double dx;
   private final double dy;
   private final double dz;

   public RelicDoorPacket(int action, double x, double y, double z, int ticks) {
      this(action, x, y, z, 0.0, 1.0, 0.0, ticks);
   }

   public RelicDoorPacket(int action, double x, double y, double z, double dx, double dy, double dz, int ticks) {
      this.action = action;
      this.x = x;
      this.y = y;
      this.z = z;
      this.dx = dx;
      this.dy = dy;
      this.dz = dz;
      this.ticks = ticks;
   }

   public static void encode(RelicDoorPacket msg, FriendlyByteBuf buf) {
      buf.writeVarInt(msg.action);
      buf.writeDouble(msg.x);
      buf.writeDouble(msg.y);
      buf.writeDouble(msg.z);
      buf.writeVarInt(msg.ticks);
      buf.writeDouble(msg.dx);
      buf.writeDouble(msg.dy);
      buf.writeDouble(msg.dz);
   }

   public static RelicDoorPacket decode(FriendlyByteBuf buf) {
      int action = buf.readVarInt();
      double x = buf.readDouble();
      double y = buf.readDouble();
      double z = buf.readDouble();
      int ticks = buf.readVarInt();
      double dx = buf.readDouble();
      double dy = buf.readDouble();
      double dz = buf.readDouble();
      return new RelicDoorPacket(action, x, y, z, dx, dy, dz, ticks);
   }

   public static void handle(RelicDoorPacket msg, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.setPacketHandled(true);
      if (context.getDirection().getReceptionSide().isClient()) {
         if (msg.action == 2) {
            SpaceBreakEffect.spawnUpright(msg.x, msg.y, msg.z, msg.dx, msg.dy, msg.dz, msg.ticks);
         } else if (msg.action == 0) {
            SpaceBreakEffect.doorOpen(msg.x, msg.y, msg.z, msg.ticks);
         } else {
            SpaceBreakEffect.doorClose();
         }
      }
   }
}

