package com.myx00x.maid.network;

import com.myx00x.maid.client.BossShellGuard;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public final class BossShellPacket {
   private final int id;
   private final double x;
   private final double y;
   private final double z;
   private final float yRot;
   private final float xRot;
   private final int millis;
   private final boolean guarded;

   public BossShellPacket(int id, double x, double y, double z, float yRot, float xRot, int millis, boolean guarded) {
      this.id = id;
      this.x = x;
      this.y = y;
      this.z = z;
      this.yRot = yRot;
      this.xRot = xRot;
      this.millis = millis;
      this.guarded = guarded;
   }

   public static void encode(BossShellPacket msg, FriendlyByteBuf buf) {
      buf.writeVarInt(msg.id);
      buf.writeDouble(msg.x);
      buf.writeDouble(msg.y);
      buf.writeDouble(msg.z);
      buf.writeFloat(msg.yRot);
      buf.writeFloat(msg.xRot);
      buf.writeVarInt(msg.millis);
      buf.writeBoolean(msg.guarded);
   }

   public static BossShellPacket decode(FriendlyByteBuf buf) {
      return new BossShellPacket(
         buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readBoolean()
      );
   }

   public static void handle(BossShellPacket msg, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.setPacketHandled(true);
      if (context.getDirection().getReceptionSide().isClient()) {
         BossShellGuard.apply(msg.id, msg.x, msg.y, msg.z, msg.yRot, msg.xRot, msg.millis, msg.guarded);
      }
   }
}

