package com.myx00x.maid.combat;

import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.LivingEntity;

public final class RetreatManager {
   private static final Map<UUID, Map<UUID, RetreatManager.Record>> RECORDS = new ConcurrentHashMap<>();

   private RetreatManager() {
   }

   public static void recordDeath(UUID maidId, LivingEntity target, long now) {
      if ((Boolean)Config.RETREAT_ENABLED.get() && target != null) {
         UUID targetId = target.getUUID();
         Map<UUID, RetreatManager.Record> byTarget = RECORDS.computeIfAbsent(maidId, k -> new ConcurrentHashMap<>());
         RetreatManager.Record record = byTarget.computeIfAbsent(targetId, k -> new RetreatManager.Record());
         if (record.avoidUntil <= now) {
            if (now - record.windowStart > ((Integer)Config.RETREAT_WINDOW_TICKS.get()).intValue()) {
               record.windowStart = now;
               record.kills = 0;
            }

            if (++record.kills >= (Integer)Config.RETREAT_KILLS.get()) {
               record.kills = 0;
               record.windowStart = now;
               record.avoidUntil = now + ((Integer)Config.RETREAT_AVOID_TICKS.get()).intValue();
               ShenlinMod.LOGGER
                  .info("[shenlin] maid {} backs off {} after {} deaths", new Object[]{maidId, target.getType().getDescriptionId(), Config.RETREAT_KILLS.get()});
            }
         }
      }
   }

   public static boolean isAvoided(UUID maidId, UUID targetId, long now) {
      if ((Boolean)Config.RETREAT_ENABLED.get() && targetId != null) {
         Map<UUID, RetreatManager.Record> byTarget = RECORDS.get(maidId);
         if (byTarget == null) {
            return false;
         }

         RetreatManager.Record record = byTarget.get(targetId);
         return record != null && record.avoidUntil > now;
      } else {
         return false;
      }
   }

   public static void clear(UUID maidId) {
      RECORDS.remove(maidId);
   }

   public static void cleanup(long now) {
      long window = (Integer)Config.RETREAT_WINDOW_TICKS.get() + (Integer)Config.RETREAT_AVOID_TICKS.get();
      RECORDS.entrySet().removeIf(entry -> {
         entry.getValue().entrySet().removeIf(e -> now - e.getValue().windowStart > window && e.getValue().avoidUntil <= now);
         return entry.getValue().isEmpty();
      });
   }

   private static final class Record {
      private int kills;
      private long windowStart;
      private long avoidUntil;
   }
}

