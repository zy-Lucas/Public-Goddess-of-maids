package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class SealField {
   private static final int CAPACITY = 256;
   private static final long LOG_GAP_MS = 2000L;
   private static final Map<UUID, SealField.Seal> SEALS = new ConcurrentHashMap<>();
   private static long failAt;

   private SealField() {
   }

   public static void seal(Entity target) {
      try {
         if (!(target != null && target.level() instanceof ServerLevel level)) {
            return;
         }

         int seconds = (Integer)Config.FINAL_SLASH_SEAL_SECONDS.get();
         double radius = ((Integer)Config.FINAL_SLASH_SEAL_RADIUS.get()).intValue();
         SealField.Seal old = SEALS.get(target.getUUID());
         if (old != null) {
            old.x = target.getX();
            old.y = target.getY();
            old.z = target.getZ();
            old.radius = radius;
            return;
         }

         if (SEALS.size() >= 256) {
            ShenlinMod.LOGGER.warn("[shenlin] seal table full at {}, dropping all seals", 256);
            SEALS.clear();
         }

         long span = seconds * 20L;
         long now = level.getGameTime();
         SEALS.put(
            target.getUUID(),
            new SealField.Seal(
               target.getUUID(),
               level.dimension(),
               target.getType(),
               target.getX(),
               target.getY(),
               target.getZ(),
               radius,
               now + span,
               span,
               now + span * 20L
            )
         );
         ShenlinMod.LOGGER
            .info(
               "[shenlin] seal {} at {}, {}, {} r={} {}s",
               new Object[]{target.getType().toShortString(), (long)target.getX(), (long)target.getY(), (long)target.getZ(), (long)radius, seconds}
            );
      } catch (Throwable t) {
         ShenlinMod.LOGGER.warn("[shenlin] seal failed: {}", t.toString());
      }
   }

   public static boolean blocks(Entity entity) {
      try {
         return hitBy(entity) != null;
      } catch (Throwable t) {
         return false;
      }
   }

   private static SealField.Seal hitBy(Entity entity) {
      if (SEALS.isEmpty() || entity == null) {
         return null;
      }

      if (!(entity instanceof Player) && !(entity instanceof EntityMaid)) {
         if (entity.level() instanceof ServerLevel level) {
            long var12 = level.getGameTime();

            for (SealField.Seal seal : SEALS.values()) {
               if (var12 <= seal.until && seal.dimension == level.dimension()) {
                  if (seal.uuid.equals(entity.getUUID())) {
                     return seal;
                  }

                  if (seal.type == entity.getType()) {
                     double dx = entity.getX() - seal.x;
                     double dy = entity.getY() - seal.y;
                     double dz = entity.getZ() - seal.z;
                     if (dx * dx + dy * dy + dz * dz <= seal.radius * seal.radius) {
                        return seal;
                     }
                  }
               }
            }

            return null;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public static void tick(MinecraftServer server) {
      if (!SEALS.isEmpty() && server != null && server.overworld() != null) {
         long now = server.overworld().getGameTime();
         SEALS.values().removeIf(sealx -> now > sealx.until);
         if (!SEALS.isEmpty()) {
            if ((Boolean)Config.FINAL_SLASH_HARD_RETRY.get()) {
               for (SealField.Seal seal : SEALS.values()) {
                  ServerLevel level = server.getLevel(seal.dimension);
                  if (level != null && now <= seal.until) {
                     try {
                        Vanisher.scanAround(level, seal.x, seal.y, seal.z, seal.radius, SealField::held);
                     } catch (Throwable t) {
                        ShenlinMod.LOGGER.warn("[shenlin] seal sweep failed: {}", t.toString());
                     }
                  }
               }
            }
         }
      }
   }

   private static void held(Entity back) {
      try {
         SealField.Seal seal = hitBy(back);
         if (seal == null) {
            return;
         }

         long wall = System.currentTimeMillis();
         if (wall - seal.lastNote >= 2000L) {
            seal.lastNote = wall;
            ShenlinMod.LOGGER
               .info(
                  "[shenlin] seal held {} back at {}, {}, {}",
                  new Object[]{back.getType().toShortString(), (long)back.getX(), (long)back.getY(), (long)back.getZ()}
               );
         }

         if (back.level() instanceof ServerLevel level) {
            seal.until = Math.min(level.getGameTime() + seal.span, seal.hardUntil);
         }

         Vanisher.vanish(back, false);
      } catch (Throwable t) {
         long wall = System.currentTimeMillis();
         if (wall - failAt >= 2000L) {
            failAt = wall;
            ShenlinMod.LOGGER.warn("[shenlin] seal strike failed: {}", t.toString());
         }
      }
   }

   private static final class Seal {
      private final UUID uuid;
      private final ResourceKey<Level> dimension;
      private final EntityType<?> type;
      private double x;
      private double y;
      private double z;
      private double radius;
      private long until;
      private final long span;
      private final long hardUntil;
      private long lastNote;

      private Seal(
         UUID uuid, ResourceKey<Level> dimension, EntityType<?> type, double x, double y, double z, double radius, long until, long span, long hardUntil
      ) {
         this.uuid = uuid;
         this.dimension = dimension;
         this.type = type;
         this.x = x;
         this.y = y;
         this.z = z;
         this.radius = radius;
         this.until = until;
         this.span = span;
         this.hardUntil = hardUntil;
      }
   }
}

