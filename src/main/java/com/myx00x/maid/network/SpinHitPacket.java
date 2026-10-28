package com.myx00x.maid.network;

import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.client.CannonRaySound;
import com.myx00x.maid.registry.ModSounds;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.network.NetworkEvent.Context;

public final class SpinHitPacket {
   private final double x;
   private final double y;
   private final double z;
   private final float range;
   private static long lastLog;

   public SpinHitPacket(double x, double y, double z, float range) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.range = range;
   }

   public static void encode(SpinHitPacket msg, FriendlyByteBuf buf) {
      buf.writeDouble(msg.x);
      buf.writeDouble(msg.y);
      buf.writeDouble(msg.z);
      buf.writeFloat(msg.range);
   }

   public static SpinHitPacket decode(FriendlyByteBuf buf) {
      return new SpinHitPacket(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat());
   }

   public static void handle(SpinHitPacket msg, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.setPacketHandled(true);
      if (context.getDirection().getReceptionSide().isClient()) {
         long now = Util.getMillis();
         if (now - lastLog > 1000L) {
            lastLog = now;
            ShenlinMod.LOGGER.info("[shenlin] spin hit sound in at {} {} {} range {}", new Object[]{(float)msg.x, (float)msg.y, (float)msg.z, msg.range});
         }

         CannonRaySound.play((SoundEvent)ModSounds.SPIN_HIT.get(), msg.x, msg.y, msg.z, msg.range);
      }
   }
}

