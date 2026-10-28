package com.myx00x.maid.network;

import com.myx00x.maid.client.RuinedPhantasmState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public final class RuinedPhantasmSpotPacket {
   private final int id;
   private final double x;
   private final double y;
   private final double z;
   private final float yRot;
   private final float xRot;
   private final int millis;
   private final boolean alive;

   public RuinedPhantasmSpotPacket(int id, double x, double y, double z, float yRot, float xRot, int millis, boolean alive) {
      this.id = id;
      this.x = x;
      this.y = y;
      this.z = z;
      this.yRot = yRot;
      this.xRot = xRot;
      this.millis = millis;
      this.alive = alive;
   }

   public static void encode(RuinedPhantasmSpotPacket msg, FriendlyByteBuf buf) {
      buf.writeVarInt(msg.id);
      buf.writeDouble(msg.x);
      buf.writeDouble(msg.y);
      buf.writeDouble(msg.z);
      buf.writeFloat(msg.yRot);
      buf.writeFloat(msg.xRot);
      buf.writeVarInt(msg.millis);
      buf.writeBoolean(msg.alive);
   }

   public static RuinedPhantasmSpotPacket decode(FriendlyByteBuf buf) {
      return new RuinedPhantasmSpotPacket(
         buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readBoolean()
      );
   }

   public static void handle(RuinedPhantasmSpotPacket msg, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.enqueueWork(
         () -> DistExecutor.unsafeRunWhenOn(
            Dist.CLIENT, () -> () -> RuinedPhantasmState.apply(msg.id, msg.x, msg.y, msg.z, msg.yRot, msg.xRot, msg.millis, msg.alive)
         )
      );
      context.setPacketHandled(true);
   }
}

