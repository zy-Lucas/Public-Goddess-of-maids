package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import com.github.tartaricacid.touhoulittlemaid.world.data.MaidInfo;
import com.github.tartaricacid.touhoulittlemaid.world.data.MaidWorldData;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.registry.ModItems;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class WormholeRecall {
   private static final TicketType<ChunkPos> RECALL_TICKET = TicketType.create("shenlin_wormhole_recall", Comparator.comparingLong(ChunkPos::m_45588_));
   private static final int TICKET_DISTANCE = 31;
   private static final int WAIT_TICKS = 100;
   private static final Map<UUID, WormholeRecall.Pending> WAITING = new ConcurrentHashMap<>();
   private static final Map<UUID, WormholeRecall.Handover> MOVING = new ConcurrentHashMap<>();
   private static final Map<UUID, WormholeRecall.Eviction> LEFTOVER = new ConcurrentHashMap<>();

   private WormholeRecall() {
   }

   public static boolean recall(ServerLevel level, Player player, ItemStack bell, UUID id) {
      if (id != null && player != null && bell != null) {
         MinecraftServer server = level.getServer();
         if (server == null) {
            return false;
         }

         if (seek(level, id) != null) {
            return false;
         }

         for (ServerLevel each : server.getAllLevels()) {
            if (each != level) {
               EntityMaid maid = seek(each, id);
               if (maid != null) {
                  if (!hasWormhole(maid)) {
                     return false;
                  }

                  EntityMaid fresh = carry(level, player, maid);
                  if (fresh == null) {
                     return false;
                  }

                  MOVING.put(id, new WormholeRecall.Handover(player.getUUID(), each.dimension(), level.dimension(), fresh, maid));
                  chime(level, player, bell);
                  return true;
               }
            }
         }

         MaidInfo info = newestRecord(level, player, id);
         ServerLevel home = info == null ? null : levelOf(server, info.getDimension());
         BlockPos pos = info == null ? null : info.getChunkPos();
         if (home != null && pos != null) {
            ChunkPos at = new ChunkPos(pos);
            wake(home, at);
            WAITING.put(id, new WormholeRecall.Pending(player.getUUID(), home.dimension(), at, home.getGameTime() + 100L));
            ShenlinMod.LOGGER
               .info("[shenlin] wormhole wait {} at {} {} {} ({} tick)", new Object[]{id, home.dimension().location(), at.x, at.z, 100});
            chime(level, player, bell);
            return true;
         } else {
            ShenlinMod.LOGGER.info("[shenlin] wormhole miss {} no record and nowhere loaded", id);
            return false;
         }
      } else {
         return false;
      }
   }

   public static void sweep(MinecraftServer server) {
      if (server != null) {
         advance(server);
         evict(server);
         if (!WAITING.isEmpty()) {
            Iterator<Entry<UUID, WormholeRecall.Pending>> it = WAITING.entrySet().iterator();

            while (it.hasNext()) {
               Entry<UUID, WormholeRecall.Pending> entry = it.next();
               UUID id = entry.getKey();
               WormholeRecall.Pending p = entry.getValue();

               try {
                  ServerLevel home = server.getLevel(p.dim());
                  if (home == null) {
                     it.remove();
                     ShenlinMod.LOGGER.info("[shenlin] wormhole miss {} dimension gone", id);
                  } else {
                     EntityMaid maid = seek(home, id);
                     if (maid == null) {
                        if (home.getGameTime() >= p.deadline()) {
                           release(server, p);
                           it.remove();
                           ShenlinMod.LOGGER.info("[shenlin] wormhole miss {} expired after {} tick", id, 100);
                        } else if (home.getGameTime() % 20L == 0L) {
                           ShenlinMod.LOGGER
                              .info("[shenlin] wormhole wait {} still inside {} {} {}", new Object[]{id, p.dim().location(), p.at().x, p.at().f_45579_});
                        }
                     } else {
                        ServerPlayer player = server.getPlayerList().getPlayer(p.owner());
                        if (player == null) {
                           release(server, p);
                           it.remove();
                           ShenlinMod.LOGGER.info("[shenlin] wormhole miss {} owner offline", id);
                        } else if (!hasWormhole(maid)) {
                           release(server, p);
                           it.remove();
                           ShenlinMod.LOGGER.info("[shenlin] wormhole miss {} no wormhole", id);
                        } else {
                           ServerLevel to = (ServerLevel)player.level();
                           if (to == home) {
                              if (shove(home, player, maid)) {
                                 release(server, p);
                                 it.remove();
                              }
                           } else {
                              EntityMaid fresh = carry(to, player, maid);
                              if (fresh == null) {
                                 release(server, p);
                                 it.remove();
                              } else {
                                 MOVING.put(id, new WormholeRecall.Handover(p.owner(), home.dimension(), to.dimension(), fresh, maid));
                                 release(server, p);
                                 it.remove();
                              }
                           }
                        }
                     }
                  }
               } catch (Throwable t) {
                  ShenlinMod.LOGGER.warn("[shenlin] wormhole sweep failed: {}", t.toString());
               }
            }
         }
      }
   }

   private static void advance(MinecraftServer server) {
      if (!MOVING.isEmpty()) {
         Iterator<Entry<UUID, WormholeRecall.Handover>> it = MOVING.entrySet().iterator();

         while (it.hasNext()) {
            Entry<UUID, WormholeRecall.Handover> entry = it.next();
            UUID id = entry.getKey();
            WormholeRecall.Handover h = entry.getValue();

            try {
               ServerLevel to = server.getLevel(h.to());
               ServerLevel from = server.getLevel(h.from());
               if (to == null || from == null) {
                  it.remove();
                  ShenlinMod.LOGGER.info("[shenlin] wormhole miss {} dimension gone in handover", id);
                  continue;
               }

               GuardRemoval.markHandover(h.origin());
               GuardRemoval.markDimensionMove(h.origin());
               h.origin().setRemoved(RemovalReason.CHANGED_DIMENSION);
               to.addDuringTeleport(h.fresh());
               settle(h.fresh());
               LEFTOVER.put(id, new WormholeRecall.Eviction(h.from(), h.origin(), h.origin().level().getGameTime() + 4L));
               ShenlinMod.LOGGER.info("[shenlin] wormhole recall {} {} -> {}", new Object[]{id, h.from().location(), h.to().location()});
            } catch (Throwable t) {
               ShenlinMod.LOGGER.warn("[shenlin] wormhole move failed: {}", t.toString());
            }

            it.remove();
         }
      }
   }

   private static void evict(MinecraftServer server) {
      if (!LEFTOVER.isEmpty()) {
         Iterator<Entry<UUID, WormholeRecall.Eviction>> it = LEFTOVER.entrySet().iterator();

         while (it.hasNext()) {
            Entry<UUID, WormholeRecall.Eviction> entry = it.next();
            UUID id = entry.getKey();
            WormholeRecall.Eviction e = entry.getValue();
            EntityMaid shell = e.shell();
            ServerLevel from = server.getLevel(e.from());
            if (from == null) {
               it.remove();
               GuardRemoval.endHandover(id);
            } else {
               long now = from.getGameTime();
               if (now >= e.deadline()) {
                  boolean stillThere = Vanisher.inRoster(from, shell);
                  if (stillThere) {
                     Vanisher.vanishMaid(shell, false);
                     stillThere = Vanisher.inRoster(from, shell);
                     if (!stillThere) {
                        ShenlinMod.LOGGER
                           .warn(
                              "[shenlin] wormhole evict {} old copy in {} sent off at {} {}",
                              new Object[]{id, e.from().location(), (int)shell.getX(), (int)shell.getZ()}
                           );
                     } else if (now % 20L == 0L) {
                        ShenlinMod.LOGGER
                           .warn(
                              "[shenlin] wormhole evict {} could not be sent off, still there at {} {}",
                              new Object[]{id, (int)shell.getX(), (int)shell.getZ()}
                           );
                     }
                  } else {
                     ShenlinMod.LOGGER
                        .warn(
                           "[shenlin] wormhole evict {} old copy in {} cleaned at {} {}",
                           new Object[]{id, e.from().location(), (int)shell.getX(), (int)shell.getZ()}
                        );
                     it.remove();
                     GuardRemoval.endHandover(id);
                  }
               }
            }
         }
      }
   }

   private static EntityMaid seek(ServerLevel level, UUID id) {
      if (level.getEntity(id) instanceof EntityMaid maid) {
         return maid;
      } else {
         List<? extends EntityMaid> hit = level.getEntities(EntityMaid.TYPE, maid -> maid.getUUID().equals(id));
         return hit.isEmpty() ? null : hit.get(0);
      }
   }

   private static boolean hasWormhole(EntityMaid maid) {
      return ItemsUtil.hasBaubleItemInMaid(maid, (Item)ModItems.TELEPORT_WORMHOLE.get());
   }

   private static void wake(ServerLevel level, ChunkPos at) {
      level.getChunkSource().addRegionTicket(RECALL_TICKET, at, 31, at);
      level.getChunk(at.x, at.z);
   }

   private static void release(MinecraftServer server, WormholeRecall.Pending p) {
      try {
         ServerLevel home = server.getLevel(p.dim());
         if (home != null) {
            home.getChunkSource().removeRegionTicket(RECALL_TICKET, p.at(), 31, p.at());
         }
      } catch (Throwable var3) {
      }
   }

   private static MaidInfo newestRecord(Level level, Player player, UUID id) {
      MaidWorldData data = MaidWorldData.get(level);
      if (data == null) {
         return null;
      }

      List<MaidInfo> infos = data.getPlayerMaidInfos(player);
      if (infos != null && !infos.isEmpty()) {
         MaidInfo best = null;

         for (MaidInfo info : infos) {
            if (id.equals(info.getEntityId()) && (best == null || info.getTimestamp() > best.getTimestamp())) {
               best = info;
            }
         }

         return best;
      } else {
         return null;
      }
   }

   private static ServerLevel levelOf(MinecraftServer server, String dimension) {
      if (server != null && dimension != null && !dimension.isEmpty()) {
         try {
            ResourceLocation key = ResourceLocation.tryParse(dimension);
            return key == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, key));
         } catch (Throwable t) {
            return null;
         }
      } else {
         return null;
      }
   }

   private static EntityMaid carry(ServerLevel to, Player player, EntityMaid maid) {
      UUID id = maid.getUUID();
      double x = player.getX() + (player.getRandom().nextInt(3) - 1);
      double y = player.getY();
      double z = player.getZ() + (player.getRandom().nextInt(3) - 1);

      try {
         maid.unRide();
         if (maid.getType().create(to) instanceof EntityMaid she) {
            she.restoreFrom(maid);
            she.setUUID(id);
            she.moveTo(x, y, z, maid.getYRot(), maid.getXRot());
            she.setYHeadRot(maid.getYRot());
            she.setDeltaMovement(Vec3.ZERO);
            she.fallDistance = 0.0F;
            return she;
         } else {
            ShenlinMod.LOGGER.warn("[shenlin] wormhole move {} cannot be created in {}", id, to.dimension().location());
            return null;
         }
      } catch (Throwable t) {
         ShenlinMod.LOGGER.warn("[shenlin] wormhole move failed: {}", t.toString());
         return null;
      }
   }

   private static boolean shove(ServerLevel level, Player player, EntityMaid maid) {
      if (maid.isRemoved()) {
         return false;
      }

      settle(maid);
      maid.moveTo(player.getX() + (player.getRandom().nextInt(3) - 1), player.getY(), player.getZ() + (player.getRandom().nextInt(3) - 1));
      ShenlinMod.LOGGER.info("[shenlin] wormhole recall {} within {}", maid.getUUID(), level.dimension().location());
      return true;
   }

   private static void settle(EntityMaid she) {
      she.setHomeModeEnable(false);
      if (she.isPassenger()) {
         she.stopRiding();
      }

      she.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 1, true, false));
   }

   private static void chime(ServerLevel level, Player player, ItemStack bell) {
      level.playSound(null, player.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.0F, 1.0F);
      player.getCooldowns().addCooldown(bell.getItem(), 20);
      if (bell.getTag() != null) {
         bell.getTag().remove("ServantBellShow");
      }
   }

   public static void forgetAll() {
      WAITING.clear();
      MOVING.clear();
      LEFTOVER.clear();
   }

   private record Eviction(ResourceKey<Level> from, EntityMaid shell, long deadline) {
   }

   private record Handover(UUID owner, ResourceKey<Level> from, ResourceKey<Level> to, EntityMaid fresh, EntityMaid origin) {
   }

   private record Pending(UUID owner, ResourceKey<Level> dim, ChunkPos at, long deadline) {
   }
}

