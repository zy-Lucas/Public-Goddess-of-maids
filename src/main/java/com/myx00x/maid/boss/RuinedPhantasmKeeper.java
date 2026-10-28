package com.myx00x.maid.boss;

import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.network.ModPackets;
import com.myx00x.maid.registry.ModEntities;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;

public final class RuinedPhantasmKeeper {
   private static final List<RuinedPhantasmBoss> LIST = new ArrayList<>();

   private RuinedPhantasmKeeper() {
   }

   public static int count() {
      return LIST.size();
   }

   public static RuinedPhantasmBoss summon(ServerLevel level, double x, double y, double z) {
      if (level == null) {
         return null;
      }

      for (RuinedPhantasmBoss b : LIST) {
         if (b.level() == level && b.pos().distanceToSqr(x, y, z) < 64.0) {
            return b;
         }
      }

      RuinedPhantasmBoss boss = new RuinedPhantasmBoss(level, x, y, z);
      LIST.add(boss);
      ensureShell(boss);
      ShenlinMod.LOGGER
         .info("[shenlin] ruined phantasm summoned at {}/{}/{}", new Object[]{String.format("%.1f", x), String.format("%.1f", y), String.format("%.1f", z)});
      return boss;
   }

   public static void tick(MinecraftServer server) {
      if (!LIST.isEmpty()) {
         LIST.removeIf(b -> !b.alive());

         for (RuinedPhantasmBoss boss : LIST) {
            try {
               boss.onMusicTick();
               if (boss.musicTick() >= 4660) {
                  dismiss(boss);
               } else {
                  ensureShell(boss);
                  ModPackets.sendRuinedPhantasmSpot(
                     boss.level(),
                     boss.proxy() == null ? 0 : boss.proxy().getId(),
                     boss.x(),
                     boss.y(),
                     boss.z(),
                     boss.yRot(),
                     boss.xRot(),
                     2000,
                     boss.alive()
                  );
                  RuinedPhantasmEntity shell = boss.proxy();
                  if (shell != null && !shell.isRemoved()) {
                     shell.faceLikeBoss();
                  }
               }
            } catch (Throwable t) {
               ShenlinMod.LOGGER.warn("[shenlin] ruined phantasm tick failed: {}", t.toString());
            }
         }
      }
   }

   private static void ensureShell(RuinedPhantasmBoss boss) {
      if (boss.alive() && boss.level() != null) {
         RuinedPhantasmEntity cur = boss.proxy();
         if (cur == null || cur.isRemoved() || cur.level() != boss.level()) {
            try {
               RuinedPhantasmEntity found = null;

               for (Entity e : boss.level().getEntities().getAll()) {
                  if (e instanceof RuinedPhantasmEntity rp && rp.boss != null && rp.boss.id().equals(boss.id())) {
                     found = rp;
                     break;
                  }
               }

               if (found != null) {
                  boss.setProxy(found);
                  found.faceLikeBoss();
                  return;
               }
            } catch (Throwable var6) {
            }

            spawnShell(boss);
         }
      }
   }

   private static void spawnShell(RuinedPhantasmBoss boss) {
      try {
         EntityType<RuinedPhantasmEntity> type = (EntityType<RuinedPhantasmEntity>)ModEntities.RUINED_PHANTOM.get();
         RuinedPhantasmEntity shell = (RuinedPhantasmEntity)type.create(boss.level());
         if (shell == null) {
            return;
         }

         shell.boss = boss;
         shell.faceLikeBoss();
         boss.level().addFreshEntity(shell);
         boss.setProxy(shell);
         ChunkPos cp = new ChunkPos((int)Math.floor(boss.x()) >> 4, (int)Math.floor(boss.z()) >> 4);
         boss.level().setChunkForced(cp.x, cp.z, true);
         ShenlinMod.LOGGER.info("[shenlin] ruined phantasm shell spawned id={}", shell.getId());
      } catch (Throwable t) {
         ShenlinMod.LOGGER.warn("[shenlin] ruined phantasm shell spawn failed: {}", t.toString());
      }
   }

   public static void dismiss(RuinedPhantasmBoss boss) {
      if (boss != null) {
         RuinedPhantasmEntity shell = boss.proxy();
         if (shell != null) {
            try {
               shell.release();
               shell.discard();
            } catch (Throwable var5) {
            }
         }

         try {
            ModPackets.sendRuinedPhantasmSpot(
               boss.level(), boss.proxy() == null ? 0 : boss.proxy().getId(), boss.x(), boss.y(), boss.z(), boss.yRot(), boss.xRot(), 0, false
            );
         } catch (Throwable var4) {
         }

         boss.finish();
         LIST.remove(boss);

         try {
            ChunkPos cp = new ChunkPos((int)Math.floor(boss.x()) >> 4, (int)Math.floor(boss.z()) >> 4);
            boss.level().setChunkForced(cp.x, cp.z, false);
         } catch (Throwable var3) {
         }

         ShenlinMod.LOGGER.info("[shenlin] ruined phantasm dismissed at music tick={}", boss.musicTick());
      }
   }

   public static void clear() {
      for (RuinedPhantasmBoss b : new ArrayList<>(LIST)) {
         try {
            b.finish();
         } catch (Throwable var3) {
         }
      }

      LIST.clear();
   }

   public static RuinedPhantasmBoss byId(UUID id) {
      for (RuinedPhantasmBoss b : LIST) {
         if (b.id().equals(id)) {
            return b;
         }
      }

      return null;
   }
}

