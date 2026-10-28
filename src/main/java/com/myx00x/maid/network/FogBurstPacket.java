package com.myx00x.maid.network;

import com.myx00x.maid.Config;
import com.myx00x.maid.client.VolumeFogEffect;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public final class FogBurstPacket {
   private final double[] xs;
   private final double[] ys;
   private final double[] zs;
   private final float[] radii;
   private final float r;
   private final float g;
   private final float b;
   private final int ticks;
   private final int mode;
   private final float alphaScale;
   private final double dx;
   private final double dy;
   private final double dz;
   private final double speed;

   public FogBurstPacket(
      double[] xs,
      double[] ys,
      double[] zs,
      float[] radii,
      float r,
      float g,
      float b,
      int ticks,
      int mode,
      float alphaScale,
      double dx,
      double dy,
      double dz,
      double speed
   ) {
      this.xs = xs;
      this.ys = ys;
      this.zs = zs;
      this.radii = radii;
      this.r = r;
      this.g = g;
      this.b = b;
      this.ticks = ticks;
      this.mode = mode;
      this.alphaScale = alphaScale;
      this.dx = dx;
      this.dy = dy;
      this.dz = dz;
      this.speed = speed;
   }

   public static void encode(FogBurstPacket msg, FriendlyByteBuf buf) {
      int n = Math.min(msg.xs.length, Math.min(msg.ys.length, Math.min(msg.zs.length, msg.radii.length)));
      buf.writeVarInt(n);

      for (int i = 0; i < n; i++) {
         buf.writeDouble(msg.xs[i]);
         buf.writeDouble(msg.ys[i]);
         buf.writeDouble(msg.zs[i]);
         buf.writeFloat(msg.radii[i]);
      }

      buf.writeFloat(msg.r);
      buf.writeFloat(msg.g);
      buf.writeFloat(msg.b);
      buf.writeVarInt(msg.ticks);
      buf.writeVarInt(msg.mode);
      buf.writeFloat(msg.alphaScale);
      buf.writeDouble(msg.dx);
      buf.writeDouble(msg.dy);
      buf.writeDouble(msg.dz);
      buf.writeDouble(msg.speed);
   }

   public static FogBurstPacket decode(FriendlyByteBuf buf) {
      int n = buf.readVarInt();
      double[] xs = new double[n];
      double[] ys = new double[n];
      double[] zs = new double[n];
      float[] radii = new float[n];

      for (int i = 0; i < n; i++) {
         xs[i] = buf.readDouble();
         ys[i] = buf.readDouble();
         zs[i] = buf.readDouble();
         radii[i] = buf.readFloat();
      }

      float r = buf.readFloat();
      float g = buf.readFloat();
      float b = buf.readFloat();
      int ticks = buf.readVarInt();
      int mode = buf.readVarInt();
      float alphaScale = buf.readFloat();
      double dx = buf.readDouble();
      double dy = buf.readDouble();
      double dz = buf.readDouble();
      double speed = buf.readDouble();
      return new FogBurstPacket(xs, ys, zs, radii, r, g, b, ticks, mode, alphaScale, dx, dy, dz, speed);
   }

   public static void handle(FogBurstPacket msg, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.setPacketHandled(true);
      if (context.getDirection().getReceptionSide().isClient()) {
         if (msg.mode != 4 || Config.deathFogEnabled()) {
            int n = Math.min(msg.xs.length, Math.min(msg.ys.length, Math.min(msg.zs.length, msg.radii.length)));

            for (int i = 0; i < n; i++) {
               VolumeFogEffect.spawn(
                  msg.xs[i], msg.ys[i], msg.zs[i], msg.radii[i], msg.r, msg.g, msg.b, msg.ticks, msg.mode, msg.dx, msg.dy, msg.dz, msg.speed, msg.alphaScale
               );
            }
         }
      }
   }
}

