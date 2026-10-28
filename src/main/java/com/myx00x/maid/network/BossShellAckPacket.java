package com.myx00x.maid.network;

import com.myx00x.maid.ShenlinMod;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public final class BossShellAckPacket {
   private final int id;
   private final boolean ok;
   private final boolean area;
   private final boolean visible;
   private final float width;
   private final float height;

   public BossShellAckPacket(int id, boolean ok, boolean area, boolean visible, float width, float height) {
      this.id = id;
      this.ok = ok;
      this.area = area;
      this.visible = visible;
      this.width = width;
      this.height = height;
   }

   public static void encode(BossShellAckPacket msg, FriendlyByteBuf buf) {
      buf.writeVarInt(msg.id);
      buf.writeBoolean(msg.ok);
      buf.writeBoolean(msg.area);
      buf.writeBoolean(msg.visible);
      buf.writeFloat(msg.width);
      buf.writeFloat(msg.height);
   }

   public static BossShellAckPacket decode(FriendlyByteBuf buf) {
      return new BossShellAckPacket(buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readFloat(), buf.readFloat());
   }

   public static void handle(BossShellAckPacket msg, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.setPacketHandled(true);
      if (!context.getDirection().getReceptionSide().isClient()) {
         ServerPlayer sender = context.getSender();
         ShenlinMod.LOGGER
            .info(
               "[shenlin] client ack from {}: id={} ok={} area={} visible={} box={}x{}",
               new Object[]{
                  sender == null ? "-" : sender.getName().getString(),
                  msg.id,
                  msg.ok,
                  msg.area,
                  msg.visible,
                  String.format("%.2f", msg.width),
                  String.format("%.2f", msg.height)
               }
            );
      }
   }
}

