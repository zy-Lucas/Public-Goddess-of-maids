package com.myx00x.maid.network;

import com.myx00x.maid.client.ScreenShake;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public final class ShakePacket {
   private final int ticks;
   private final float power;
   private final float downBias;
   private final float rough;
   private final boolean steady;
   private final float bounce;
   private final int sourceId;
   private final float kickX;
   private final float kickZ;
   private final float kickPower;

   public ShakePacket(int ticks, float power) {
      this(ticks, power, 0.0F);
   }

   public ShakePacket(int ticks, float power, float downBias) {
      this(ticks, power, downBias, 0.0F);
   }

   public ShakePacket(int ticks, float power, float downBias, float rough) {
      this(ticks, power, downBias, rough, false);
   }

   public ShakePacket(int ticks, float power, float downBias, float rough, boolean steady) {
      this(ticks, power, downBias, rough, steady, 0.0F);
   }

   public ShakePacket(int ticks, float power, float downBias, float rough, boolean steady, float bounce) {
      this(ticks, power, downBias, rough, steady, bounce, 0);
   }

   public ShakePacket(int ticks, float power, float downBias, float rough, boolean steady, float bounce, int sourceId) {
      this(ticks, power, downBias, rough, steady, bounce, sourceId, 0.0F, 0.0F, 0.0F);
   }

   public ShakePacket(
      int ticks, float power, float downBias, float rough, boolean steady, float bounce, int sourceId, float kickX, float kickZ, float kickPower
   ) {
      this.ticks = ticks;
      this.power = power;
      this.downBias = downBias;
      this.rough = rough;
      this.steady = steady;
      this.bounce = bounce;
      this.sourceId = sourceId;
      this.kickX = kickX;
      this.kickZ = kickZ;
      this.kickPower = kickPower;
   }

   public static void encode(ShakePacket msg, FriendlyByteBuf buf) {
      buf.writeVarInt(msg.ticks);
      buf.writeFloat(msg.power);
      buf.writeFloat(msg.downBias);
      buf.writeFloat(msg.rough);
      buf.writeBoolean(msg.steady);
      buf.writeFloat(msg.bounce);
      buf.writeVarInt(msg.sourceId);
      buf.writeFloat(msg.kickX);
      buf.writeFloat(msg.kickZ);
      buf.writeFloat(msg.kickPower);
   }

   public static ShakePacket decode(FriendlyByteBuf buf) {
      return new ShakePacket(
         buf.readVarInt(),
         buf.readFloat(),
         buf.readFloat(),
         buf.readFloat(),
         buf.readBoolean(),
         buf.readFloat(),
         buf.readVarInt(),
         buf.readFloat(),
         buf.readFloat(),
         buf.readFloat()
      );
   }

   public static void handle(ShakePacket msg, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.setPacketHandled(true);
      if (context.getDirection().getReceptionSide().isClient()) {
         if (msg.sourceId == 0 || ScreenShake.seenById(msg.sourceId)) {
            if (msg.steady) {
               ScreenShake.hold(msg.ticks, msg.power, msg.downBias, msg.rough);
            } else {
               ScreenShake.trigger(msg.ticks, msg.power, msg.downBias, msg.rough, msg.bounce, msg.kickX, msg.kickZ, msg.kickPower);
            }
         }
      }
   }
}

