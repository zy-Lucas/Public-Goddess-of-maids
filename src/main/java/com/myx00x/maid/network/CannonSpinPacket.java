package com.myx00x.maid.network;

import com.myx00x.maid.client.CannonSpinEffect;
import com.myx00x.maid.client.SpinRaySound;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public final class CannonSpinPacket {
   private final int maidId;
   private final long startTick;
   private final int totalTicks;
   private final double x;
   private final double y;
   private final double z;
   private final float rayRange;
   private final float rayFar;

   public CannonSpinPacket(int maidId, long startTick, int totalTicks, double x, double y, double z, float rayRange, float rayFar) {
      this.maidId = maidId;
      this.startTick = startTick;
      this.totalTicks = totalTicks;
      this.x = x;
      this.y = y;
      this.z = z;
      this.rayRange = rayRange;
      this.rayFar = rayFar;
   }

   public static void encode(CannonSpinPacket msg, FriendlyByteBuf buf) {
      buf.writeVarInt(msg.maidId);
      buf.writeVarLong(msg.startTick);
      buf.writeVarInt(msg.totalTicks);
      buf.writeDouble(msg.x);
      buf.writeDouble(msg.y);
      buf.writeDouble(msg.z);
      buf.writeFloat(msg.rayRange);
      buf.writeFloat(msg.rayFar);
   }

   public static CannonSpinPacket decode(FriendlyByteBuf buf) {
      return new CannonSpinPacket(
         buf.readVarInt(), buf.readVarLong(), buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readFloat()
      );
   }

   public static void handle(CannonSpinPacket msg, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.setPacketHandled(true);
      if (context.getDirection().getReceptionSide().isClient()) {
         CannonSpinEffect.spawn(msg.maidId, msg.startTick, msg.totalTicks);
         if (msg.totalTicks > 0) {
            SpinRaySound.play(msg.maidId, msg.x, msg.y, msg.z, msg.rayRange, msg.rayFar);
         } else {
            SpinRaySound.stop(msg.maidId);
         }
      }
   }
}

