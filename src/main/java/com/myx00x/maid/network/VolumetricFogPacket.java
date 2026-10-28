package com.myx00x.maid.network;

import com.myx00x.maid.client.VolumetricFog;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent.Context;

public final class VolumetricFogPacket {
   private final double x;
   private final double y;
   private final double z;
   private final float radius;
   private final float density;
   private final int ticks;

   public VolumetricFogPacket(double x, double y, double z, float radius, float density, int ticks) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.radius = radius;
      this.density = density;
      this.ticks = ticks;
   }

   public static void encode(VolumetricFogPacket msg, FriendlyByteBuf buf) {
      buf.writeDouble(msg.x);
      buf.writeDouble(msg.y);
      buf.writeDouble(msg.z);
      buf.writeFloat(msg.radius);
      buf.writeFloat(msg.density);
      buf.writeVarInt(msg.ticks);
   }

   public static VolumetricFogPacket decode(FriendlyByteBuf buf) {
      return new VolumetricFogPacket(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readFloat(), buf.readVarInt());
   }

   public static void handle(VolumetricFogPacket msg, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.enqueueWork(() -> VolumetricFog.spawn(new Vec3(msg.x, msg.y, msg.z), msg.radius, msg.density, msg.ticks * 50L));
      context.setPacketHandled(true);
   }
}

