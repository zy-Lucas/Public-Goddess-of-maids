package com.myx00x.maid.network;

import com.myx00x.maid.client.OrbitCannonEffect;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public final class CannonFormPacket {
   private final int maidId;
   private final double ax;
   private final double ay;
   private final double az;
   private final double aimX;
   private final double aimZ;
   private final long startTick;
   private final int chargeTicks;
   private final int totalTicks;
   private final int targetId;
   private final float soundRange;
   private final float soundFar;

   public CannonFormPacket(
      int maidId,
      double ax,
      double ay,
      double az,
      double aimX,
      double aimZ,
      long startTick,
      int chargeTicks,
      int totalTicks,
      int targetId,
      float soundRange,
      float soundFar
   ) {
      this.maidId = maidId;
      this.ax = ax;
      this.ay = ay;
      this.az = az;
      this.aimX = aimX;
      this.aimZ = aimZ;
      this.startTick = startTick;
      this.chargeTicks = chargeTicks;
      this.totalTicks = totalTicks;
      this.targetId = targetId;
      this.soundRange = soundRange;
      this.soundFar = soundFar;
   }

   public static void encode(CannonFormPacket msg, FriendlyByteBuf buf) {
      buf.writeVarInt(msg.maidId);
      buf.writeDouble(msg.ax);
      buf.writeDouble(msg.ay);
      buf.writeDouble(msg.az);
      buf.writeDouble(msg.aimX);
      buf.writeDouble(msg.aimZ);
      buf.writeVarLong(msg.startTick);
      buf.writeVarInt(msg.chargeTicks);
      buf.writeVarInt(msg.totalTicks);
      buf.writeVarInt(msg.targetId);
      buf.writeFloat(msg.soundRange);
      buf.writeFloat(msg.soundFar);
   }

   public static CannonFormPacket decode(FriendlyByteBuf buf) {
      return new CannonFormPacket(
         buf.readVarInt(),
         buf.readDouble(),
         buf.readDouble(),
         buf.readDouble(),
         buf.readDouble(),
         buf.readDouble(),
         buf.readVarLong(),
         buf.readVarInt(),
         buf.readVarInt(),
         buf.readVarInt(),
         buf.readFloat(),
         buf.readFloat()
      );
   }

   public static void handle(CannonFormPacket msg, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.setPacketHandled(true);
      if (context.getDirection().getReceptionSide().isClient()) {
         OrbitCannonEffect.form(
            msg.maidId, msg.ax, msg.ay, msg.az, msg.aimX, msg.aimZ, msg.startTick, msg.chargeTicks, msg.totalTicks, msg.targetId, msg.soundRange, msg.soundFar
         );
      }
   }
}

