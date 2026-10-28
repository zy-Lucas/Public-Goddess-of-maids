package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.ShenlinMod;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public final class ChunkAnchor {
   private static final TicketType<ChunkPos> ANCHOR_TICKET = TicketType.create("shenlin_maid_anchor", Comparator.comparingLong(ChunkPos::m_45588_));
   private static final int TICKET_DISTANCE = 31;
   private static final long RECHECK_TICKS = 40L;
   private static final Map<UUID, ChunkAnchor.Held> HELD = new ConcurrentHashMap<>();
   private static final Map<String, Long> LAST_LOG = new ConcurrentHashMap<>();

   private ChunkAnchor() {
   }

   public static void ensure(EntityMaid maid) {
      if (maid != null && maid.level() != null && !maid.level().isClientSide) {
         if (maid.level() instanceof ServerLevel level) {
            UUID var8 = maid.getUUID();
            ChunkPos here = new ChunkPos(maid.blockPosition());
            ChunkAnchor.Held held = HELD.get(var8);
            if (held == null || !held.dim().equals(level.dimension()) || held.chunk() != here.toLong()) {
               try {
                  if (GuardWatch.tierOf(maid) < 3) {
                     release(var8, level.getServer());
                     return;
                  }

                  ServerLevel previous = null;
                  if (held != null && level.getServer() != null) {
                     previous = level.getServer().getLevel(held.dim());
                  }

                  level.getChunkSource().addRegionTicket(ANCHOR_TICKET, here, 31, here);
                  if (previous != null) {
                     ChunkPos old = new ChunkPos(held.chunk());
                     previous.getChunkSource().removeRegionTicket(ANCHOR_TICKET, old, 31, old);
                  }

                  HELD.put(var8, new ChunkAnchor.Held(level.dimension(), here.toLong()));
                  if (note(var8, level.getGameTime())) {
                     ShenlinMod.LOGGER
                        .info("[shenlin] maid {} chunk anchored at {} {} {}", new Object[]{var8, level.dimension().location(), here.x, here.f_45579_});
                  }
               } catch (Throwable t) {
                  ShenlinMod.LOGGER.warn("[shenlin] maid {} chunk anchor failed: {}", var8, t.toString());
               }
            }
         }
      }
   }

   public static boolean holds(EntityMaid maid) {
      if (maid != null && maid.level() != null && !maid.level().isClientSide) {
         ChunkAnchor.Held held = HELD.get(maid.getUUID());
         return held == null ? false : held.dim().equals(maid.level().dimension()) && held.chunk() == new ChunkPos(maid.blockPosition()).toLong();
      } else {
         return false;
      }
   }

   public static void release(UUID id, MinecraftServer server) {
      ChunkAnchor.Held held = HELD.remove(id);
      if (held != null && server != null) {
         ServerLevel level = server.getLevel(held.dim());
         if (level != null) {
            ChunkPos pos = new ChunkPos(held.chunk());

            try {
               level.getChunkSource().removeRegionTicket(ANCHOR_TICKET, pos, 31, pos);
            } catch (Throwable t) {
               ShenlinMod.LOGGER.warn("[shenlin] maid {} chunk anchor release failed: {}", id, t.toString());
            }
         }
      }
   }

   public static void sweep(MinecraftServer server, long gameTime) {
      if (server != null && !HELD.isEmpty() && gameTime % 40L == 0L) {
         for (UUID id : new ArrayList<>(HELD.keySet())) {
            ChunkAnchor.Held held = HELD.get(id);
            if (held != null) {
               ServerLevel level = server.getLevel(held.dim());
               if (level == null) {
                  HELD.remove(id);
               } else if (!(level.getEntity(id) instanceof EntityMaid maid && maid.level().getServer() == server && GuardWatch.tierOf(maid) >= 3)) {
                  release(id, server);
               }
            }
         }
      }
   }

   public static void releaseAll(MinecraftServer server) {
      for (UUID id : new ArrayList<>(HELD.keySet())) {
         release(id, server);
      }

      HELD.clear();
      LAST_LOG.clear();
   }

   private static boolean note(UUID id, long gameTime) {
      String key = id + "|anchor";
      Long seen = LAST_LOG.get(key);
      if (seen != null && gameTime - seen < 100L) {
         return false;
      }

      if (LAST_LOG.size() > 64) {
         LAST_LOG.entrySet().removeIf(e -> gameTime - e.getValue() > 1200L);
      }

      LAST_LOG.put(key, gameTime);
      return true;
   }

   public static void forgetAll() {
      HELD.clear();
      LAST_LOG.clear();
   }

   private record Held(ResourceKey<Level> dim, long chunk) {
   }
}

