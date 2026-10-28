package com.myx00x.maid.network;

import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.client.ShockwaveEffect;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public final class ShockwavePacket {
   private static final float LEGACY_FAST_SHARE = 0.5F;
   private static final int LEGACY_FAST_MS = 150;
   private static final int LEGACY_SLOW_MS = 500;
   private final double x;
   private final double y;
   private final double z;
   private final float radius;
   private final int ticks;
   private final float fastRadius;
   private final int fastMs;
   private final int slowMs;
   private final float ax;
   private final float ay;
   private final float az;
   private final int maidId;
   private final int pod;
   private final boolean wild;
   private final boolean warm;
   private final boolean violet;
   private final float flareScale;
   private static long lastLog;

   public ShockwavePacket(double x, double y, double z, float radius, int ticks, float ax, float ay, float az) {
      this(x, y, z, radius, ticks, radius * 0.5F, 150, 500, ax, ay, az, 0, -1, false, false);
   }

   public ShockwavePacket(double x, double y, double z, float radius, int ticks, float fastRadius, int fastMs, int slowMs, float ax, float ay, float az) {
      this(x, y, z, radius, ticks, fastRadius, fastMs, slowMs, ax, ay, az, 0, -1, false, false);
   }

   public ShockwavePacket(double x, double y, double z, float radius, int ticks, float ax, float ay, float az, int maidId, int pod) {
      this(x, y, z, radius, ticks, radius * 0.5F, 150, 500, ax, ay, az, maidId, pod, false, false);
   }

   public ShockwavePacket(double x, double y, double z, float radius, int ticks, float ax, float ay, float az, boolean wild) {
      this(x, y, z, radius, ticks, radius * 0.5F, 150, 500, ax, ay, az, 0, -1, wild, false);
   }

   public ShockwavePacket(double x, double y, double z, float radius, int ticks, float ax, float ay, float az, boolean wild, boolean warm) {
      this(x, y, z, radius, ticks, radius * 0.5F, 150, 500, ax, ay, az, 0, -1, wild, warm);
   }

   public ShockwavePacket(double x, double y, double z, float radius, int ticks, float ax, float ay, float az, boolean wild, boolean warm, float flareScale) {
      this(x, y, z, radius, ticks, radius * 0.5F, 150, 500, ax, ay, az, 0, -1, wild, warm, false, flareScale);
   }

   public ShockwavePacket(
      double x, double y, double z, float radius, int ticks, float fastRadius, int fastMs, int slowMs, float ax, float ay, float az, boolean warm
   ) {
      this(x, y, z, radius, ticks, fastRadius, fastMs, slowMs, ax, ay, az, 0, -1, false, warm);
   }

   public ShockwavePacket(
      double x,
      double y,
      double z,
      float radius,
      int ticks,
      float fastRadius,
      int fastMs,
      int slowMs,
      float ax,
      float ay,
      float az,
      int maidId,
      int pod,
      boolean wild,
      boolean warm,
      boolean violet
   ) {
      this(x, y, z, radius, ticks, fastRadius, fastMs, slowMs, ax, ay, az, maidId, pod, wild, warm, violet, true);
   }

   private ShockwavePacket(
      double x,
      double y,
      double z,
      float radius,
      int ticks,
      float fastRadius,
      int fastMs,
      int slowMs,
      float ax,
      float ay,
      float az,
      int maidId,
      int pod,
      boolean wild,
      boolean warm
   ) {
      this(x, y, z, radius, ticks, fastRadius, fastMs, slowMs, ax, ay, az, maidId, pod, wild, warm, false, true);
   }

   public ShockwavePacket(
      double x,
      double y,
      double z,
      float radius,
      int ticks,
      float fastRadius,
      int fastMs,
      int slowMs,
      float ax,
      float ay,
      float az,
      int maidId,
      int pod,
      boolean wild,
      boolean warm,
      boolean violet,
      float flareScale
   ) {
      this(x, y, z, radius, ticks, fastRadius, fastMs, slowMs, ax, ay, az, maidId, pod, wild, warm, violet, true, flareScale);
   }

   private ShockwavePacket(
      double x,
      double y,
      double z,
      float radius,
      int ticks,
      float fastRadius,
      int fastMs,
      int slowMs,
      float ax,
      float ay,
      float az,
      int maidId,
      int pod,
      boolean wild,
      boolean warm,
      boolean violet,
      boolean canonical
   ) {
      this(x, y, z, radius, ticks, fastRadius, fastMs, slowMs, ax, ay, az, maidId, pod, wild, warm, violet, canonical, 1.0F);
   }

   private ShockwavePacket(
      double x,
      double y,
      double z,
      float radius,
      int ticks,
      float fastRadius,
      int fastMs,
      int slowMs,
      float ax,
      float ay,
      float az,
      int maidId,
      int pod,
      boolean wild,
      boolean warm,
      boolean violet,
      boolean canonical,
      float flareScale
   ) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.radius = radius;
      this.ticks = ticks;
      this.fastRadius = fastRadius;
      this.fastMs = fastMs;
      this.slowMs = slowMs;
      this.ax = ax;
      this.ay = ay;
      this.az = az;
      this.maidId = maidId;
      this.pod = pod;
      this.wild = wild;
      this.warm = warm;
      this.violet = violet;
      this.flareScale = flareScale;
   }

   public static void encode(ShockwavePacket msg, FriendlyByteBuf buf) {
      buf.writeDouble(msg.x);
      buf.writeDouble(msg.y);
      buf.writeDouble(msg.z);
      buf.writeFloat(msg.radius);
      buf.writeVarInt(msg.ticks);
      buf.writeFloat(msg.fastRadius);
      buf.writeVarInt(msg.fastMs);
      buf.writeVarInt(msg.slowMs);
      buf.writeFloat(msg.ax);
      buf.writeFloat(msg.ay);
      buf.writeFloat(msg.az);
      buf.writeVarInt(msg.maidId);
      buf.writeVarInt(msg.pod);
      buf.writeBoolean(msg.wild);
      buf.writeBoolean(msg.warm);
      buf.writeBoolean(msg.violet);
      buf.writeFloat(msg.flareScale);
   }

   public static ShockwavePacket decode(FriendlyByteBuf buf) {
      return new ShockwavePacket(
         buf.readDouble(),
         buf.readDouble(),
         buf.readDouble(),
         buf.readFloat(),
         buf.readVarInt(),
         buf.readFloat(),
         buf.readVarInt(),
         buf.readVarInt(),
         buf.readFloat(),
         buf.readFloat(),
         buf.readFloat(),
         buf.readVarInt(),
         buf.readVarInt(),
         buf.readBoolean(),
         buf.readBoolean(),
         buf.readBoolean(),
         buf.readFloat()
      );
   }

   public static void handle(ShockwavePacket msg, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.setPacketHandled(true);
      if (context.getDirection().getReceptionSide().isClient()) {
         long now = Util.getMillis();
         if (now - lastLog > 1000L) {
            lastLog = now;
            ShenlinMod.LOGGER.info("[shenlin] shockwave packet in pod={}", msg.pod);
         }

         if (msg.pod >= 0) {
            ShockwaveEffect.spawnPod(msg.x, msg.y, msg.z, msg.radius, msg.ticks, msg.ax, msg.ay, msg.az, msg.maidId, msg.pod);
         } else {
            ShockwaveEffect.spawn(
               msg.x,
               msg.y,
               msg.z,
               msg.radius,
               msg.ticks,
               msg.fastRadius,
               msg.fastMs,
               msg.slowMs,
               msg.ax,
               msg.ay,
               msg.az,
               msg.wild,
               msg.warm,
               msg.violet,
               msg.flareScale
            );
         }
      }
   }
}

