package com.myx00x.maid.network;

import com.myx00x.maid.client.VolumeFogEffect;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public final class FogPacket {
   public static final int MODE_STILL = 0;
   public static final int MODE_SPREAD = 1;
   public static final int MODE_JET = 2;
   public static final int MODE_DISSOLVE = 3;
   public static final int MODE_BILLOW = 4;
   private final double x;
   private final double y;
   private final double z;
   private final float radius;
   private final float r;
   private final float g;
   private final float b;
   private final int ticks;
   private final int mode;
   private final double dx;
   private final double dy;
   private final double dz;
   private final double speed;
   private final float alphaScale;

   public FogPacket(double x, double y, double z, float radius, float r, float g, float b, int ticks) {
      this(x, y, z, radius, r, g, b, ticks, 0, 0.0, 0.0, 0.0, 0.0);
   }

   public FogPacket(double x, double y, double z, float radius, float r, float g, float b, int ticks, int mode, double dx, double dy, double dz, double speed) {
      this(x, y, z, radius, r, g, b, ticks, mode, dx, dy, dz, speed, 1.0F);
   }

   public FogPacket(
      double x,
      double y,
      double z,
      float radius,
      float r,
      float g,
      float b,
      int ticks,
      int mode,
      double dx,
      double dy,
      double dz,
      double speed,
      float alphaScale
   ) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.radius = radius;
      this.r = r;
      this.g = g;
      this.b = b;
      this.ticks = ticks;
      this.mode = mode;
      this.dx = dx;
      this.dy = dy;
      this.dz = dz;
      this.speed = speed;
      this.alphaScale = alphaScale;
   }

   public static void encode(FogPacket msg, FriendlyByteBuf buf) {
      buf.writeDouble(msg.x);
      buf.writeDouble(msg.y);
      buf.writeDouble(msg.z);
      buf.writeFloat(msg.radius);
      buf.writeFloat(msg.r);
      buf.writeFloat(msg.g);
      buf.writeFloat(msg.b);
      buf.writeVarInt(msg.ticks);
      buf.writeVarInt(msg.mode);
      buf.writeDouble(msg.dx);
      buf.writeDouble(msg.dy);
      buf.writeDouble(msg.dz);
      buf.writeDouble(msg.speed);
      buf.writeFloat(msg.alphaScale);
   }

   public static FogPacket decode(FriendlyByteBuf buf) {
      double x = buf.readDouble();
      double y = buf.readDouble();
      double z = buf.readDouble();
      float radius = buf.readFloat();
      float r = buf.readFloat();
      float g = buf.readFloat();
      float b = buf.readFloat();
      int ticks = buf.readVarInt();
      int mode = buf.readVarInt();
      double dx = buf.readDouble();
      double dy = buf.readDouble();
      double dz = buf.readDouble();
      double speed = buf.readDouble();
      float alphaScale = buf.readFloat();
      return new FogPacket(x, y, z, radius, r, g, b, ticks, mode, dx, dy, dz, speed, alphaScale);
   }

   public static void handle(FogPacket msg, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.setPacketHandled(true);
      if (context.getDirection().getReceptionSide().isClient()) {
         VolumeFogEffect.spawn(msg.x, msg.y, msg.z, msg.radius, msg.r, msg.g, msg.b, msg.ticks, msg.mode, msg.dx, msg.dy, msg.dz, msg.speed, msg.alphaScale);
      }
   }
}

