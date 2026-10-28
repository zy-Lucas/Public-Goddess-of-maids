package com.myx00x.maid.world;

import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.network.ModPackets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "goddess_of_maids", bus = Bus.FORGE)
public final class RelicDoor {
   private static final long LIFETIME_MS = 60000L;
   private static final double STEP_IN = 2.2;
   private static final double STEP_UP = 2.5;
   private static final double AHEAD = 4.5;
   private static final double MIN_AHEAD = 3.2;
   private static final long OPEN_GRACE_MS = 400L;
   private static final int TICKET_RADIUS = 2;
   private static final Map<UUID, RelicDoor.Pending> PENDING = new HashMap<>();

   private RelicDoor() {
   }

   public static void open(ServerPlayer player, BlockPos site, ItemStack stack) {
      release(PENDING.remove(player.getUUID()));
      Vec3 look = player.getLookAngle();
      Vec3 flat = new Vec3(look.x, 0.0, look.z);
      if (flat.lengthSqr() < 1.0E-6) {
         flat = new Vec3(0.0, 0.0, 1.0);
      }

      flat = flat.normalize();
      Vec3 door = place(player, flat);
      long now = System.currentTimeMillis();
      ChunkPos ticket = new ChunkPos(site.offset(28, 8, 0));
      player.serverLevel().getChunkSource().addRegionTicket(TicketType.PLAYER, ticket, 2, ticket);
      PENDING.put(player.getUUID(), new RelicDoor.Pending(player.getUUID(), player.serverLevel(), site, door, now + 60000L, now, stack, ticket));
      player.level().playSound(null, door.x, door.y, door.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.6F, 1.0F);
      ModPackets.sendRelicDoor(player, 0, door.x, door.y, door.z, 1200);
      ShenlinMod.LOGGER.info("[shenlin] relic door opened at {} for site {}", door, site);
   }

   private static Vec3 place(ServerPlayer player, Vec3 flat) {
      Vec3 side = new Vec3(-flat.z, 0.0, flat.x);

      for (Vec3 dir : new Vec3[]{flat, side, side.scale(-1.0), flat.scale(-1.0)}) {
         for (double d = 4.5; d >= 3.2; d -= 0.5) {
            Vec3 at = player.position().add(dir.scale(d));
            if (free(player, at)) {
               return new Vec3(at.x, at.y + 1.0, at.z);
            }
         }
      }

      return player.position().add(flat.scale(4.5)).add(0.0, 1.0, 0.0);
   }

   private static boolean free(ServerPlayer player, Vec3 at) {
      AABB box = new AABB(at.x - 0.3, at.y + 1.0, at.z - 0.3, at.x + 0.3, at.y + 3.0, at.z + 0.3);
      return player.level().noCollision(box);
   }

   @SubscribeEvent
   public static void onTick(PlayerTickEvent event) {
      if (event.phase == Phase.END && !PENDING.isEmpty()) {
         if (event.player instanceof ServerPlayer player) {
            RelicDoor.Pending pending = PENDING.get(player.getUUID());
            if (pending != null) {
               if (!player.isRemoved() && !player.hasDisconnected()) {
                  if (player.serverLevel() != pending.level()) {
                     close(player, "left dimension");
                  } else {
                     long now = System.currentTimeMillis();
                     if (now > pending.expiry()) {
                        close(player, "expired");
                     } else if (now - pending.openedAt() >= 400L) {
                        double dx = player.getX() - pending.door().x;
                        double dz = player.getZ() - pending.door().z;
                        if (!(dx * dx + dz * dz > 4.840000000000001)) {
                           if (!(Math.abs(player.getY() - pending.door().y) > 2.5)) {
                              BlockPos target = pending.site().offset(28, 8, 0);
                              player.playNotifySound(SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.275F, 1.4F);
                              player.connection
                                 .teleport(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, player.getYRot(), player.getXRot());
                              player.level()
                                 .playSound(
                                    null,
                                    target.getX() + 0.5,
                                    target.getY(),
                                    target.getZ() + 0.5,
                                    SoundEvents.PORTAL_TRAVEL,
                                    SoundSource.PLAYERS,
                                    0.45F,
                                    1.0F
                                 );
                              ShenlinMod.LOGGER.info("[shenlin] relic door stepped in -> {}", target);
                              if (pending.item() != null) {
                                 RelicEyeSearch.verifyNextTick(player, pending.item(), pending.level(), target);
                              }

                              close(player, "entered");
                           }
                        }
                     }
                  }
               } else {
                  release(PENDING.remove(player.getUUID()));
               }
            }
         }
      }
   }

   private static void close(ServerPlayer player, String why) {
      release(PENDING.remove(player.getUUID()));
      ModPackets.sendRelicDoor(player, 1, 0.0, 0.0, 0.0, 0);
      ShenlinMod.LOGGER.info("[shenlin] relic door closed ({})", why);
   }

   private static void release(RelicDoor.Pending pending) {
      if (pending != null && pending.ticket() != null) {
         try {
            pending.level().getChunkSource().removeRegionTicket(TicketType.PLAYER, pending.ticket(), 2, pending.ticket());
         } catch (Throwable var2) {
         }
      }
   }

   private record Pending(UUID player, ServerLevel level, BlockPos site, Vec3 door, long expiry, long openedAt, ItemStack item, ChunkPos ticket) {
   }
}

