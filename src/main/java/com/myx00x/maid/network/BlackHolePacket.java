package com.myx00x.maid.network;

import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.client.BlackHoleEffect;
import com.myx00x.maid.client.HoleSounds;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public final class BlackHolePacket {
   private final double x;
   private final double y;
   private final double z;
   private final float size;
   private final float warp;
   private final float reach;
   private final int ticks;
   private final boolean execution;
   private static long lastLog;

   public BlackHolePacket(double x, double y, double z, float size, float warp, float reach, int ticks, boolean execution) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.size = size;
      this.warp = warp;
      this.reach = reach;
      this.ticks = ticks;
      this.execution = execution;
   }

   public static void encode(BlackHolePacket msg, FriendlyByteBuf buf) {
      buf.writeDouble(msg.x);
      buf.writeDouble(msg.y);
      buf.writeDouble(msg.z);
      buf.writeFloat(msg.size);
      buf.writeFloat(msg.warp);
      buf.writeFloat(msg.reach);
      buf.writeVarInt(msg.ticks);
      buf.writeBoolean(msg.execution);
   }

   public static BlackHolePacket decode(FriendlyByteBuf buf) {
      return new BlackHolePacket(
         buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readBoolean()
      );
   }

   public static void handle(BlackHolePacket msg, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.setPacketHandled(true);
      if (context.getDirection().getReceptionSide().isClient()) {
         long now = Util.getMillis();
         if (now - lastLog > 1000L) {
            lastLog = now;
            ShenlinMod.LOGGER
               .info(
                  "[shenlin] hole packet in size={} warp={} reach={} ticks={} exec={}", new Object[]{msg.size, msg.warp, msg.reach, msg.ticks, msg.execution}
               );
         }

         BlackHoleEffect.spawn(msg.x, msg.y, msg.z, msg.size, msg.warp, msg.reach, msg.ticks, msg.execution);
         if (!msg.execution) {
            HoleSounds.begin(msg.x, msg.y, msg.z, msg.warp, Math.max(1, msg.ticks) * 50L);
         }
      }
   }
}

