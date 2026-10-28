package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.ShenlinMod;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public final class TimeKeeper {
   private static final Map<UUID, Integer> LAST_TICK = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> LAST_LOG = new ConcurrentHashMap<>();

   private TimeKeeper() {
   }

   public static void forgetAll() {
      LAST_TICK.clear();
      LAST_LOG.clear();
   }

   public static void tick(MinecraftServer server) {
      if (server != null) {
         for (UUID id : GuardWatch.watched()) {
            try {
               tickOne(id, server);
            } catch (Throwable t) {
               ShenlinMod.LOGGER.warn("[shenlin] maid {} time keeper step failed: {}", id, t.toString());
            }
         }
      }
   }

   private static void tickOne(UUID id, MinecraftServer server) {
      EntityMaid maid = GuardWatch.held(id);
      if (maid != null && !maid.isRemoved()) {
         if (maid.level() instanceof ServerLevel home && home.getServer() == server) {
            if (GuardWatch.isVanishable(id) && !GuardWatch.onRoster(maid) || !GuardVitals.alive(maid)) {
               LAST_TICK.remove(id);
            } else if (!maid.level().isClientSide) {
               if (maid.isNoAi()) {
                  maid.setNoAi(false);
                  if (note(id)) {
                     ShenlinMod.LOGGER.info("[shenlin] maid {} noai cleared", id);
                  }
               }

               int now = maid.tickCount;
               Integer last = LAST_TICK.put(id, now);
               if (last != null && now == last) {
                  maid.tick();
                  if (note(id)) {
                     ShenlinMod.LOGGER.info("[shenlin] maid {} stopped in time, ticked by hand", id);
                  }
               }
            }
         }
      } else {
         LAST_TICK.remove(id);
      }
   }

   private static boolean note(UUID id) {
      long now = System.currentTimeMillis();
      Long seen = LAST_LOG.get(id);
      if (seen != null && now - seen < 5000L) {
         return false;
      }

      LAST_LOG.put(id, now);
      return true;
   }
}

