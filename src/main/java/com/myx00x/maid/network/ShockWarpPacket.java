package com.myx00x.maid.network;

import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.client.ShockWarpEffect;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public final class ShockWarpPacket {
   private final double x;
   private final double y;
   private final double z;
   private final float radius;
   private final float fastRadius;
   private final int fastMs;
   private final int slowMs;
   private final int kind;
   private final boolean flat;
   private final boolean hollow;
   private static long lastLog;

   public ShockWarpPacket(double x, double y, double z, float radius, float fastRadius, int fastMs, int slowMs, int kind, boolean flat, boolean hollow) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.radius = radius;
      this.fastRadius = fastRadius;
      this.fastMs = fastMs;
      this.slowMs = slowMs;
      this.kind = kind;
      this.flat = flat;
      this.hollow = hollow;
   }

   public static void encode(ShockWarpPacket msg, FriendlyByteBuf buf) {
      buf.writeDouble(msg.x);
      buf.writeDouble(msg.y);
      buf.writeDouble(msg.z);
      buf.writeFloat(msg.radius);
      buf.writeFloat(msg.fastRadius);
      buf.writeVarInt(msg.fastMs);
      buf.writeVarInt(msg.slowMs);
      buf.writeVarInt(msg.kind);
      buf.writeBoolean(msg.flat);
      buf.writeBoolean(msg.hollow);
   }

   public static ShockWarpPacket decode(FriendlyByteBuf buf) {
      return new ShockWarpPacket(
         buf.readDouble(),
         buf.readDouble(),
         buf.readDouble(),
         buf.readFloat(),
         buf.readFloat(),
         buf.readVarInt(),
         buf.readVarInt(),
         buf.readVarInt(),
         buf.readBoolean(),
         buf.readBoolean()
      );
   }

   public static void handle(ShockWarpPacket msg, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.setPacketHandled(true);
      if (context.getDirection().getReceptionSide().isClient()) {
         long now = Util.getMillis();
         if (now - lastLog > 1000L) {
            lastLog = now;
            ShenlinMod.LOGGER
               .info(
                  "[shenlin] shock warp packet in radius={} fast={} ms={}/{} kind={} flat={} hollow={}",
                  new Object[]{msg.radius, msg.fastRadius, msg.fastMs, msg.slowMs, msg.kind, msg.flat, msg.hollow}
               );
         }

         ShockWarpEffect.spawn(msg.x, msg.y, msg.z, msg.radius, msg.fastRadius, msg.fastMs, msg.slowMs, msg.kind, msg.flat, msg.hollow);
      }
   }
}

