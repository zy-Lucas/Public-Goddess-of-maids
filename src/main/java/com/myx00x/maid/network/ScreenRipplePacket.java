package com.myx00x.maid.network;

import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.client.ScreenRippleEffect;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public final class ScreenRipplePacket {
   private final int entityId;
   private final double x;
   private final double y;
   private final double z;
   private final int ticks;
   private static long lastLog;

   public ScreenRipplePacket(int entityId, double x, double y, double z, int ticks) {
      this.entityId = entityId;
      this.x = x;
      this.y = y;
      this.z = z;
      this.ticks = ticks;
   }

   public static void encode(ScreenRipplePacket msg, FriendlyByteBuf buf) {
      buf.writeVarInt(msg.entityId);
      buf.writeDouble(msg.x);
      buf.writeDouble(msg.y);
      buf.writeDouble(msg.z);
      buf.writeVarInt(msg.ticks);
   }

   public static ScreenRipplePacket decode(FriendlyByteBuf buf) {
      return new ScreenRipplePacket(buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readVarInt());
   }

   public static void handle(ScreenRipplePacket msg, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.setPacketHandled(true);
      if (context.getDirection().getReceptionSide().isClient()) {
         long now = Util.getMillis();
         if (now - lastLog > 1000L) {
            lastLog = now;
            ShenlinMod.LOGGER.info("[shenlin] screen ripple packet in who={} ticks={}", msg.entityId, msg.ticks);
         }

         ScreenRippleEffect.spawn(msg.entityId, msg.x, msg.y, msg.z, msg.ticks);
      }
   }
}

