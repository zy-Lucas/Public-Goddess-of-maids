package com.myx00x.maid.network;

import com.myx00x.maid.client.OrbitCannonEffect;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public final class OrbitPacket {
   public static final int OFF = 0;
   public static final int IDLE = 1;
   public static final int FIRING = 2;
   private final int entityId;
   private final int state;
   private final int targetId;
   private final int idleMs;

   public OrbitPacket(int entityId, int state, int targetId, int idleMs) {
      this.entityId = entityId;
      this.state = state;
      this.targetId = targetId;
      this.idleMs = idleMs;
   }

   public static void encode(OrbitPacket msg, FriendlyByteBuf buf) {
      buf.writeVarInt(msg.entityId);
      buf.writeVarInt(msg.state);
      buf.writeVarInt(msg.targetId);
      buf.writeVarInt(msg.idleMs);
   }

   public static OrbitPacket decode(FriendlyByteBuf buf) {
      return new OrbitPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
   }

   public static void handle(OrbitPacket msg, Supplier<Context> ctx) {
      Context context = ctx.get();
      context.setPacketHandled(true);
      if (context.getDirection().getReceptionSide().isClient()) {
         OrbitCannonEffect.set(msg.entityId, msg.state, msg.targetId, msg.idleMs);
      }
   }
}

