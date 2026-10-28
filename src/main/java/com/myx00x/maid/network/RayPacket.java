package com.myx00x.maid.network;

import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.client.CannonRaySound;
import com.myx00x.maid.client.RayEffect;
import com.myx00x.maid.client.StarEffect;
import com.myx00x.maid.registry.ModSounds;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.network.NetworkEvent.Context;

public final class RayPacket {
   private final double fromX;
   private final double fromY;
   private final double fromZ;
   private final double toX;
   private final double toY;
   private final double toZ;
   private final int ticks;
   private final float width;
   private final boolean through;
   private final int leadMs;
   private final float starRadius;
   private final float cr;
   private final float cg;
   private final float cb;
   private final float soundRange;
   private final float coreWidth;
   private final float burst;
   private final int followId;
   private final int hangOn;
   private final boolean column;
   private static long lastLog;

   public RayPacket(
      double fromX,
      double fromY,
      double fromZ,
      double toX,
      double toY,
      double toZ,
      int ticks,
      float width,
      boolean through,
      int leadMs,
      float starRadius,
      float cr,
      float cg,
      float cb,
      float soundRange,
      float coreWidth,
      float burst,
      int followId,
      boolean column,
      int hangOn
   ) {
      this.fromX = fromX;
      this.fromY = fromY;
      this.fromZ = fromZ;
      this.toX = toX;
      this.toY = toY;
      this.toZ = toZ;
      this.ticks = ticks;
      this.width = width;
      this.through = through;
      this.leadMs = leadMs;
      this.starRadius = starRadius;
      this.cr = cr;
      this.cg = cg;
      this.cb = cb;
      this.soundRange = soundRange;
      this.coreWidth = coreWidth;
      this.burst = burst;
      this.followId = followId;
      this.column = column;
      this.hangOn = hangOn;
   }

   public static void encode(RayPacket msg, FriendlyByteBuf buf) {
      buf.writeDouble(msg.fromX);
      buf.writeDouble(msg.fromY);
      buf.writeDouble(msg.fromZ);
      buf.writeDouble(msg.toX);
      buf.writeDouble(msg.toY);
      buf.writeDouble(msg.toZ);
      buf.writeVarInt(msg.ticks);
      buf.writeFloat(msg.width);
      buf.writeBoolean(msg.through);
      buf.writeVarInt(msg.leadMs);
      buf.writeFloat(msg.starRadius);
      buf.writeFloat(msg.cr);
      buf.writeFloat(msg.cg);
      buf.writeFloat(msg.cb);
      buf.writeFloat(msg.soundRange);
      buf.writeFloat(msg.coreWidth);
      buf.writeFloat(msg.burst);
      buf.writeVarInt(msg.followId);
      buf.writeBoolean(msg.column);
      buf.writeVarInt(msg.hangOn);
   }

   public static RayPacket decode(FriendlyByteBuf buf) {
      return new RayPacket(
         buf.readDouble(),
         buf.readDouble(),
         buf.readDouble(),
         buf.readDouble(),
         buf.readDouble(),
         buf.readDouble(),
         buf.readVarInt(),
         buf.readFloat(),
         buf.readBoolean(),
         buf.readVarInt(),
         buf.readFloat(),
         buf.readFloat(),
         buf.readFloat(),
         buf.readFloat(),
         buf.readFloat(),
         buf.readFloat(),
         buf.readFloat(),
         buf.readVarInt(),
         buf.readBoolean(),
         buf.readVarInt()
      );
   }

   public static void handle(RayPacket msg, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.setPacketHandled(true);
      if (context.getDirection().getReceptionSide().isClient()) {
         long now = Util.getMillis();
         if (now - lastLog > 1000L) {
            lastLog = now;
            ShenlinMod.LOGGER
               .info(
                  "[shenlin] ray packet in ticks={} lead={} width={} through={} star={} core={} burst={}",
                  new Object[]{msg.ticks, msg.leadMs, msg.width, msg.through, msg.starRadius, msg.coreWidth, msg.burst}
               );
         }

         RayEffect.spawn(
            msg.fromX,
            msg.fromY,
            msg.fromZ,
            msg.toX,
            msg.toY,
            msg.toZ,
            msg.ticks,
            msg.width,
            msg.through,
            msg.leadMs,
            msg.cr,
            msg.cg,
            msg.cb,
            msg.coreWidth,
            msg.burst,
            msg.followId,
            msg.column,
            msg.hangOn
         );
         if (msg.soundRange > 0.0F) {
            CannonRaySound.play((SoundEvent)ModSounds.CANNON_RAY.get(), msg.fromX, msg.fromY, msg.fromZ, msg.soundRange);
         }

         if (msg.starRadius > 0.0F) {
            StarEffect.spawn(msg.fromX, msg.fromY, msg.fromZ, msg.starRadius);
         }
      }
   }
}

