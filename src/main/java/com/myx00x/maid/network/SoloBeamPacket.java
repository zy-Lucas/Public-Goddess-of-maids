package com.myx00x.maid.network;

import com.myx00x.maid.client.SoloBeamEffect;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public final class SoloBeamPacket {
   private final int maidId;
   private final int targetId;
   private final long startTick;
   private final int chargeTicks;
   private final int fireTicks;
   private final int fadeTicks;
   private final double x;
   private final double y;
   private final double z;
   private final double soundRange;
   private final double soundFar;

   public SoloBeamPacket(
      int maidId, int targetId, long startTick, int chargeTicks, int fireTicks, int fadeTicks, double x, double y, double z, double soundRange, double soundFar
   ) {
      this.maidId = maidId;
      this.targetId = targetId;
      this.startTick = startTick;
      this.chargeTicks = chargeTicks;
      this.fireTicks = fireTicks;
      this.fadeTicks = fadeTicks;
      this.x = x;
      this.y = y;
      this.z = z;
      this.soundRange = soundRange;
      this.soundFar = soundFar;
   }

   public static void encode(SoloBeamPacket msg, FriendlyByteBuf buf) {
      buf.writeVarInt(msg.maidId);
      buf.writeVarInt(msg.targetId);
      buf.writeVarLong(msg.startTick);
      buf.writeVarInt(msg.chargeTicks);
      buf.writeVarInt(msg.fireTicks);
      buf.writeVarInt(msg.fadeTicks);
      buf.writeDouble(msg.x);
      buf.writeDouble(msg.y);
      buf.writeDouble(msg.z);
      buf.writeDouble(msg.soundRange);
      buf.writeDouble(msg.soundFar);
   }

   public static SoloBeamPacket decode(FriendlyByteBuf buf) {
      return new SoloBeamPacket(
         buf.readVarInt(),
         buf.readVarInt(),
         buf.readVarLong(),
         buf.readVarInt(),
         buf.readVarInt(),
         buf.readVarInt(),
         buf.readDouble(),
         buf.readDouble(),
         buf.readDouble(),
         buf.readDouble(),
         buf.readDouble()
      );
   }

   public static void handle(SoloBeamPacket msg, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.setPacketHandled(true);
      if (context.getDirection().getReceptionSide().isClient()) {
         SoloBeamEffect.spawn(
            msg.maidId, msg.targetId, msg.startTick, msg.chargeTicks, msg.fireTicks, msg.fadeTicks, msg.x, msg.y, msg.z, msg.soundRange, msg.soundFar
         );
      }
   }
}

