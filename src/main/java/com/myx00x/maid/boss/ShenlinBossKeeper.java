package com.myx00x.maid.boss;

import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.combat.Annihilator;
import com.myx00x.maid.combat.RosterRestore;
import com.myx00x.maid.combat.SkinFix;
import com.myx00x.maid.mob.RelicWardenEntity;
import com.myx00x.maid.network.ModPackets;
import com.myx00x.maid.registry.ModEntities;
import com.myx00x.maid.world.WardenVitalsData;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.AbortableIterationConsumer.Continuation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ShenlinBossKeeper {
   public static final int DEFAULT_LIFE_TICKS = 4700;
   private static final List<ShenlinBoss> ALIVE = new ArrayList<>();
   private static long beat;
   private static String lastHalf = "";
   private static int shellDenied;
   private static long deniedLoggedAt;
   private static final long DENIED_LOG_MS = 5000L;
   private static String lastSwap = "";
   private static final double BOOM_RADIUS = 48.0;
   private static final double BOOM_RANGE = 220.0;
   private static final float BOOM_RADIUS_VISUAL = 120.0F;
   private static final float BOOM_FAST_RADIUS = 110.0F;
   private static final double ERASE_RADIUS = 33.0;
   private static final List<ShenlinBossKeeper.Damned> DAMNED = new ArrayList<>();
   private static final int TELL_EVERY = 10;
   private static long traceLoggedAt;
   private static final long TRACE_LOG_MS = 5000L;
   private static int posResets;
   private static long posLoggedAt;
   private static final long POS_LOG_MS = 5000L;
   private static int fixDenied;
   private static String lastMiss = "";
   private static long missLoggedAt;
   private static long relistLoggedAt;
   private static final int KEEP_RADIUS = 1;

   private ShenlinBossKeeper() {
   }

   private static void pin(ShenlinBoss boss) {
      ServerLevel level = boss.level();
      if (level != null) {
         ChunkPos at = new ChunkPos(BlockPos.containing(boss.x(), boss.y(), boss.z()));

         try {
            level.getChunkSource().addRegionTicket(TicketType.FORCED, at, 1, at);
            boss.setTicket(at);
         } catch (Throwable var4) {
         }
      }
   }

   private static void unpin(ShenlinBoss boss) {
      ChunkPos at = boss.ticket();
      if (at != null) {
         try {
            boss.level().getChunkSource().removeRegionTicket(TicketType.FORCED, at, 1, at);
         } catch (Throwable var3) {
         }

         boss.setTicket(null);
      }
   }

   public static ShenlinBoss summon(ServerLevel level, double x, double y, double z, int lifeTicks) {
      ShenlinBoss boss = new ShenlinBoss(level, x, y, z, lifeTicks);
      ALIVE.add(boss);
      BossRegistry.enroll(level, BlockPos.containing(x, y, z));
      pin(boss);
      ensureShell(boss);
      ShenlinMod.LOGGER.info("[shenlin] boss summoned id={} at {} life={}", new Object[]{boss.id(), String.format("%.2f/%.2f/%.2f", x, y, z), lifeTicks});
      return boss;
   }

   static ShenlinBoss adopt(ShenlinBossEntity shell) {
      if (shell.level() instanceof ServerLevel level) {
         ShenlinBoss var3 = new ShenlinBoss(level, shell.getX(), shell.getY(), shell.getZ(), 4700);
         BossRegistry.enroll(level, shell.blockPosition());
         pin(var3);
         var3.setProxy(shell);
         shell.boss = var3;
         ALIVE.add(var3);
         ShenlinMod.LOGGER.info("[shenlin] boss adopted shell at {}", String.format("%.2f/%.2f/%.2f", shell.getX(), shell.getY(), shell.getZ()));
         return var3;
      } else {
         return null;
      }
   }

   public static void tick(MinecraftServer server) {
      sweepWardenPositions(server);
      holdDamned(server);
      if (!ALIVE.isEmpty()) {
         if (++beat % 100L == 0L) {
            String playerDim = "-";
            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            if (!players.isEmpty()) {
               playerDim = players.get(0).level().dimension().location().toString();
            }

            StringBuilder line = new StringBuilder();

            for (ShenlinBoss boss : ALIVE) {
               line.append(" [").append(boss.level().dimension().location());
               ShenlinBossEntity shell = boss.proxy();
               if (shell == null) {
                  line.append("] shell=none age=").append(boss.age());
               } else {
                  AABB bb = shell.getBoundingBox();
                  double centerX = (bb.minX + bb.maxX) * 0.5;
                  double centerY = (bb.minY + bb.maxY) * 0.5;
                  double centerZ = (bb.minZ + bb.maxZ) * 0.5;
                  EntityDimensions want = shell.getDimensions(shell.getPose());
                  line.append("] id=")
                     .append(shell.getId())
                     .append(" rem=")
                     .append(shell.isRemoved())
                     .append(" rsn=")
                     .append(shell.getRemovalReason())
                     .append(" inv=")
                     .append(shell.isInvulnerable())
                     .append(" box=")
                     .append(String.format("%.2fx%.2f@%.1f/%.1f/%.1f", bb.getXsize(), bb.getYsize(), centerX, centerY, centerZ))
                     .append(" want=")
                     .append(String.format("%.2fx%.2f", want.width, want.height))
                     .append(" byId=")
                     .append(boss.level().getEntity(shell.getId()) == shell)
                     .append(" byUuid=")
                     .append(boss.level().getEntities().get(shell.getUUID()) == shell)
                     .append(" byType=")
                     .append(inTypeIndex(shell, boss))
                     .append(" byArea=")
                     .append(inAreaIndex(shell, boss))
                     .append(" tracked=")
                     .append(RosterRestore.tracked(boss.level(), shell))
                     .append(" age=")
                     .append(boss.age());
               }
            }

            ShenlinMod.LOGGER
               .info("[shenlin] boss probe playerDim={} denied={} fix={} pos={}{}", new Object[]{playerDim, shellDenied, fixDenied, posResets, line});
            fixDenied = 0;
            posResets = 0;
         }

         boolean tellNow = beat % 10L == 0L;

         for (ShenlinBoss boss : List.copyOf(ALIVE)) {
            try {
               if (!BossRegistry.guarded(server, boss.level().dimension(), BlockPos.containing(boss.x(), boss.y(), boss.z()))) {
                  dismiss(boss);
               } else {
                  pin(boss);
                  boss.tick();
                  if (boss.alive()) {
                     ensureShell(boss);
                     if (tellNow) {
                        tellShell(boss);
                     }
                  }
               }
            } catch (Throwable t) {
               ShenlinMod.LOGGER.warn("[shenlin] boss tick failed: {}", t.toString());
               dismiss(boss);
            }
         }

         tailCheck();
      }
   }

   private static void holdDamned(MinecraftServer server) {
      if (!DAMNED.isEmpty() && server != null) {
         Iterator<ShenlinBossKeeper.Damned> it = DAMNED.iterator();

         while (it.hasNext()) {
            ShenlinBossKeeper.Damned d = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(d.id);
            if (player == null) {
               it.remove();
            } else if (!player.isSpectator() && !player.isDeadOrDying()) {
               try {
                  player.setDeltaMovement(0.0, 0.0, 0.0);
                  player.hurtMarked = true;
                  player.setSprinting(false);
                  player.stopUsingItem();
                  if (player.tickCount % 10 == 0) {
                     player.connection.teleport(d.x, d.y, d.z, d.yaw, d.pitch);
                  }
               } catch (Throwable t) {
                  it.remove();
               }
            } else {
               it.remove();
               DamnedLedger.clear(server, d.id);
            }
         }
      }
   }

   public static void release(ServerPlayer player) {
      if (player != null) {
         MinecraftServer server = player.getServer();
         if (server != null) {
            DamnedLedger.clear(server, player.getUUID());
         }

         DAMNED.removeIf(damned -> damned.id.equals(player.getUUID()));
         ModPackets.sendDamnationClear(player);
         ShenlinMod.LOGGER.info("[shenlin] damned player released: {}", player.getName().getString());
      }
   }

   public static void rejoin(ServerPlayer player) {
      if (player != null) {
         MinecraftServer server = player.getServer();
         if (server != null && DamnedLedger.contains(server, player.getUUID())) {
            DAMNED.removeIf(damned -> damned.id.equals(player.getUUID()));
            DAMNED.add(new ShenlinBossKeeper.Damned(player));
            ModPackets.sendDamnation(player, player.getName().getString());
            ShenlinMod.LOGGER.info("[shenlin] damned player rejoined, screen restored: {}", player.getName().getString());
         }
      }
   }

   public static void shellLost(int id) {
      if ((Boolean)Config.BOSS_BOOM_ON_LOST.get()) {
         for (ShenlinBoss boss : List.copyOf(ALIVE)) {
            ShenlinBossEntity shell = boss.proxy();
            if (shell != null && shell.getId() == id) {
               ShenlinMod.LOGGER.warn("[shenlin] client reports shell {} lost, detonating", id);
               detonate(boss);
               return;
            }
         }
      }
   }

   private static void tailCheck() {
      for (ShenlinBoss boss : List.copyOf(ALIVE)) {
         ShenlinBossEntity cur = boss.proxy();
         if (cur != null && cur.level() instanceof ServerLevel level) {
            if (!RosterRestore.intact(level, cur)) {
               RosterRestore.force(level, cur, true);
            }

            if (RosterRestore.intact(level, cur)) {
               boss.found();
            }
         }
      }
   }

   private static void detonate(ShenlinBoss boss) {
      ServerLevel level = boss.level();
      double x = boss.x();
      double y = boss.y();
      double z = boss.z();
      ShenlinMod.LOGGER
         .warn(
            "[shenlin] boss shell broken at {}/{}/{}, detonating", new Object[]{String.format("%.2f", x), String.format("%.2f", y), String.format("%.2f", z)}
         );

      try {
         ModPackets.sendShockWarp(level, new Vec3(x, y + 1.0, z), 220.0, 120.0F, 110.0F, 300, 1600, 1);
      } catch (Throwable t) {
         ShenlinMod.LOGGER.warn("[shenlin] boss boom effect failed: {}", t.toString());
      }

      try {
         level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y + 1.0, z, 3, 0.4, 0.4, 0.4, 0.0);
      } catch (Throwable var18) {
      }

      try {
         level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 8.0F, 0.35F);
         level.playSound(null, x, y, z, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.2F, 0.55F);
      } catch (Throwable var17) {
      }

      try {
         int erased = Annihilator.eraseAround(level, x, y, z, 33.0);
         ShenlinMod.LOGGER.warn("[shenlin] boss boom erased {} entities", erased);
      } catch (Throwable t) {
         ShenlinMod.LOGGER.warn("[shenlin] boss boom erase failed: {}", t.toString());
      }

      MinecraftServer server = level.getServer();
      if (server != null) {
         double reach = 2304.0;

         for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() == level && !player.isSpectator() && !(player.distanceToSqr(x, y, z) > reach) && !player.isDeadOrDying()) {
               try {
                  DamnedLedger.mark(server, player.getUUID());
                  DAMNED.removeIf(damned -> damned.id.equals(player.getUUID()));
                  DAMNED.add(new ShenlinBossKeeper.Damned(player));
                  ModPackets.sendDamnation(player, player.getName().getString());
                  ShenlinMod.LOGGER.warn("[shenlin] boss boom took player {}", player.getName().getString());
               } catch (Throwable t) {
                  ShenlinMod.LOGGER.warn("[shenlin] boss boom on player failed: {}", t.toString());
               }
            }
         }
      }

      try {
         if (server != null) {
            BossRegistry.release(server, level.dimension(), BlockPos.containing(x, y, z), "被强行抹除后自爆");
         }
      } catch (Throwable var14) {
      }

      dismiss(boss);
   }

   public static void sweepWardenPositions(MinecraftServer server) {
      for (ServerLevel level : server.getAllLevels()) {
         WardenVitalsData data = WardenVitalsData.get(level);
         Map<UUID, WardenVitalsData.Row> rows = data.rows();
         if (!rows.isEmpty()) {
            for (Entry<UUID, WardenVitalsData.Row> entry : rows.entrySet()) {
               UUID id = entry.getKey();
               WardenVitalsData.Row row = entry.getValue();
               if (!row.hasPos() || level.getChunkSource().getChunkNow((int)Math.floor(row.x()) >> 4, (int)Math.floor(row.z()) >> 4) != null) {
                  RelicWardenEntity warden = level.getEntity(id) instanceof RelicWardenEntity w && !w.isRemoved() ? w : null;
                  double nowX = warden != null ? warden.getX() : Double.NaN;
                  double nowY = warden != null ? warden.getY() : Double.NaN;
                  double nowZ = warden != null ? warden.getZ() : Double.NaN;
                  if (!row.hasPos()) {
                     if (warden != null) {
                        data.markPos(id, nowX, nowY, nowZ);
                     }
                  } else {
                     double dx = nowX - row.x();
                     double dy = nowY - row.y();
                     double dz = nowZ - row.z();
                     double range = 66.0;
                     boolean normal = !Double.isNaN(dx) && dx * dx + dy * dy + dz * dz <= range * range;
                     if (normal) {
                        data.markPos(id, nowX, nowY, nowZ);
                     } else {
                        double bx = row.x();
                        double by = row.y();
                        double bz = row.z();
                        data.voidPos(id);
                        if (row.boom()) {
                           ShenlinMod.LOGGER
                              .warn(
                                 "[shenlin] warden gone or moved away uuid={} at={} now={}, blasting where it stood",
                                 new Object[]{
                                    id,
                                    String.format("%.1f,%.1f,%.1f", bx, by, bz),
                                    warden == null ? "absent" : String.format("%.1f,%.1f,%.1f", nowX, nowY, nowZ)
                                 }
                              );

                           try {
                              Vec3 spot = new Vec3(bx, by, bz);
                              if (warden != null) {
                                 warden.blastHereAndLeave(level, spot);
                              } else {
                                 RelicWardenEntity.blastAt(level, spot, true);
                              }
                           } catch (Throwable t) {
                              ShenlinMod.LOGGER.warn("[shenlin] warden salvage blast failed: {}", t.toString());
                           }
                        } else {
                           ShenlinMod.LOGGER.info("[shenlin] warden went away quietly uuid={} (boom off)", id);
                        }

                        if (warden != null && !warden.isRemoved()) {
                           warden.discard();
                        }

                        data.forget(id);
                     }
                  }
               }
            }
         }
      }
   }

   private static void ensureShell(ShenlinBoss boss) {
      ShenlinBossEntity shell = boss.proxy();
      if (shell != null && shell.getClass() != ShenlinBossEntity.class) {
         String swappedAs = shell.getClass().getName();
         boolean restored = SkinFix.repair(shell, ShenlinBossEntity.class);
         String tag = swappedAs + "/" + restored;
         if (!tag.equals(lastSwap)) {
            lastSwap = tag;
            ShenlinMod.LOGGER.info("[shenlin] boss shell class swapped, now={} restored={}", swappedAs, restored);
         }
      }

      if (shell != null) {
         shell.clearRemovalReason();
      }

      if (shell == null || !onRoster(shell, boss)) {
         if (shell != null) {
            reportMiss(shell, boss);
         }

         String via = shell != null ? relist(boss, shell) : null;
         if (via != null) {
            ShenlinMod.LOGGER.info("[shenlin] boss shell pulled back id={} age={} via={}", new Object[]{shell.getId(), boss.age(), via});
            tellShell(boss);
         } else {
            if (shell != null) {
               long now = System.currentTimeMillis();
               if (now - relistLoggedAt >= 5000L) {
                  relistLoggedAt = now;
                  ShenlinMod.LOGGER.warn("[shenlin] boss shell relist failed id={} age={}", shell.getId(), boss.age());
               }

               boss.setProxy(null);

               try {
                  detach(shell, false);
               } catch (Throwable t) {
                  ShenlinMod.LOGGER.warn("[shenlin] boss stale shell detach failed: {}", t.toString());
               }
            }

            ShenlinBossEntity fresh = new ShenlinBossEntity((EntityType<? extends Entity>)ModEntities.SHENLIN_BOSS.get(), boss.level());
            fresh.boss = boss;
            fresh.setPos(boss.x(), boss.y(), boss.z());
            fresh.setYRot(boss.yRot());
            fresh.setXRot(boss.xRot());

            boolean landed;
            try {
               landed = boss.level().addFreshEntity(fresh);
            } catch (Throwable t) {
               shellDenied++;
               long now = System.currentTimeMillis();
               if (now - deniedLoggedAt >= 5000L) {
                  deniedLoggedAt = now;
                  ShenlinMod.LOGGER.warn("[shenlin] boss shell add refused x{} (age={}): {}", new Object[]{shellDenied, boss.age(), t.toString()});
               }

               return;
            }

            if (landed) {
               afterShellIn(boss, fresh, "respawned");
            } else {
               if (!RosterRestore.force(boss.level(), fresh, true)) {
                  shellDenied++;
                  long now = System.currentTimeMillis();
                  if (now - deniedLoggedAt >= 5000L) {
                     deniedLoggedAt = now;
                     ShenlinMod.LOGGER.warn("[shenlin] boss shell refused by level x{} (age={}) — 实体入世界被外面压着", shellDenied, boss.age());
                  }

                  return;
               }

               afterShellIn(boss, fresh, "forced in");
            }
         }
      }

      ShenlinBossEntity cur = boss.proxy();
      if (cur != null && cur.level() instanceof ServerLevel level) {
         fixPosition(boss, cur);
         traceWatch(boss, cur);
         if (!RosterRestore.force(level, cur, true)) {
            fixDenied++;
            long now = System.currentTimeMillis();
            if (now - deniedLoggedAt >= 5000L) {
               deniedLoggedAt = now;
               ShenlinMod.LOGGER.warn("[shenlin] boss shell readback failed x{} (age={}) — 账没落全", fixDenied, boss.age());
            }
         }
      }
   }

   private static void fixPosition(ShenlinBoss boss, ShenlinBossEntity shell) {
      if (Double.isFinite(boss.x()) && Double.isFinite(boss.y()) && Double.isFinite(boss.z())) {
         double dx = shell.getX() - boss.x();
         double dy = shell.getY() - boss.y();
         double dz = shell.getZ() - boss.z();
         if (!(dx * dx + dy * dy + dz * dz <= 4.0)) {
            shell.setPos(boss.x(), boss.y(), boss.z());
            posResets++;
            long now = System.currentTimeMillis();
            if (now - posLoggedAt >= 5000L) {
               posLoggedAt = now;
               ShenlinMod.LOGGER.info("[shenlin] boss shell pos reset id={}", shell.getId());
            }
         }
      }
   }

   private static void afterShellIn(ShenlinBoss boss, ShenlinBossEntity fresh, String how) {
      if (shellDenied > 0) {
         ShenlinMod.LOGGER.info("[shenlin] boss shell accepted again after {} refusals (age={})", shellDenied, boss.age());
         shellDenied = 0;
      }

      boss.setProxy(fresh);
      ShenlinMod.LOGGER.info("[shenlin] boss shell {} id={} age={}", new Object[]{how, fresh.getId(), boss.age()});
      tellShell(boss);
   }

   private static String relist(ShenlinBoss boss, ShenlinBossEntity shell) {
      if (shell.level() instanceof ServerLevel level) {
         shell.resolveLists();
         Object manager = shell.listManager(level);
         if (manager == null) {
            return RosterRestore.force(level, shell, true) ? "force" : null;
         }

         shell.setPos(boss.x(), boss.y(), boss.z());
         shell.clearRemovalReason();

         try {
            if (shell.addNoEvent(manager) && shell.onLists(level)) {
               return "noEvent";
            } else if (shell.addByEvent(manager) && shell.onLists(level)) {
               return "byEvent";
            } else {
               return RosterRestore.force(level, shell, true) ? "force" : null;
            }
         } catch (Throwable t) {
            return null;
         }
      } else {
         return null;
      }
   }

   private static void reportMiss(ShenlinBossEntity shell, ShenlinBoss boss) {
      try {
         Object got = boss.level().getEntity(shell.getId());
         boolean byUuid = boss.level().getEntities().get(shell.getUUID()) == shell;
         boolean byType = inTypeIndex(shell, boss);
         boolean byArea = inAreaIndex(shell, boss);
         boolean tracked = RosterRestore.tracked(boss.level(), shell);
         String tag = shell.getId() + "/" + shell.isRemoved() + "/" + (got == null) + "/" + byUuid + "/" + byType;
         long now = System.currentTimeMillis();
         if (tag.equals(lastMiss) && now - missLoggedAt < 5000L) {
            return;
         }

         lastMiss = tag;
         missLoggedAt = now;
         ShenlinMod.LOGGER
            .warn(
               "[shenlin] boss shell miss detail id={} removed={} reason={} byId={} got={} byUuid={} byType={} byArea={} tracked={} alive={} pos={} age={}",
               new Object[]{
                  shell.getId(),
                  shell.isRemoved(),
                  shell.getRemovalReason(),
                  got == shell,
                  got == null ? "null" : got.getClass().getSimpleName(),
                  byUuid,
                  byType,
                  byArea,
                  tracked,
                  shell.isAlive(),
                  String.format("%.1f/%.1f/%.1f", shell.getX(), shell.getY(), shell.getZ()),
                  boss.age()
               }
            );
      } catch (Throwable var10) {
      }
   }

   private static void traceWatch(ShenlinBoss boss, ShenlinBossEntity shell) {
      ServerLevel level = boss.level();
      if (!RosterRestore.tracked(level, shell)) {
         long now = System.currentTimeMillis();
         if (now - traceLoggedAt >= 5000L) {
            traceLoggedAt = now;
            ShenlinMod.LOGGER.info("[shenlin] boss shell trace missing id={}", shell.getId());
         }
      }
   }

   private static void tellShell(ShenlinBoss boss) {
      ShenlinBossEntity shell = boss.proxy();
      if (shell != null) {
         boolean guarded = BossRegistry.guarded(boss.level().getServer(), boss.level().dimension(), BlockPos.containing(boss.x(), boss.y(), boss.z()));
         ModPackets.sendBossShell(boss.level(), shell.getId(), boss.x(), boss.y(), boss.z(), boss.yRot(), boss.xRot(), guarded);
      }
   }

   private static boolean onRoster(ShenlinBossEntity shell, ShenlinBoss boss) {
      if (shell != null && !shell.isRemoved()) {
         if (boss.level().getEntity(shell.getId()) != shell) {
            return false;
         }

         boolean byUuid = boss.level().getEntities().get(shell.getUUID()) == shell;
         boolean byType = inTypeIndex(shell, boss);
         boolean byArea = inAreaIndex(shell, boss);
         if (byUuid && byType && byArea) {
            return true;
         }

         String now = shell.getId() + "/" + byUuid + "/" + byType + "/" + byArea;
         if (!now.equals(lastHalf)) {
            lastHalf = now;
            ShenlinMod.LOGGER
               .warn("[shenlin] boss shell half-listed id={} byId=true byUuid={} byType={} byArea={}", new Object[]{shell.getId(), byUuid, byType, byArea});
         }

         return false;
      } else {
         return false;
      }
   }

   private static boolean inTypeIndex(ShenlinBossEntity shell, ShenlinBoss boss) {
      boolean[] found = new boolean[]{false};
      boss.level().getEntities().get(EntityTypeTest.forClass(ShenlinBossEntity.class), entity -> {
         if (entity == shell) {
            found[0] = true;
            return Continuation.ABORT;
         } else {
            return Continuation.CONTINUE;
         }
      });
      return found[0];
   }

   private static boolean inAreaIndex(ShenlinBossEntity shell, ShenlinBoss boss) {
      AABB area = shell.getType().getDimensions().makeBoundingBox(shell.position()).inflate(0.5);
      boolean[] found = new boolean[]{false};
      boss.level().getEntities().get(EntityTypeTest.forClass(ShenlinBossEntity.class), area, entity -> {
         if (entity == shell) {
            found[0] = true;
            return Continuation.ABORT;
         } else {
            return Continuation.CONTINUE;
         }
      });
      return found[0];
   }

   static void dismissShell(ShenlinBossEntity shell) {
      ShenlinBoss boss = shell.boss;
      if (boss != null && boss.proxy() == shell) {
         boss.setProxy(null);
      }

      detach(shell, true);
   }

   public static void dismiss(ShenlinBoss boss) {
      if (boss != null && boss.alive()) {
         BossRegistry.release(boss.level().getServer(), boss.level().dimension(), BlockPos.containing(boss.x(), boss.y(), boss.z()), "自然收场");
         unpin(boss);
         boss.finish();
         ALIVE.remove(boss);
         ShenlinBossEntity shell = boss.proxy();
         boss.setProxy(null);
         if (shell != null) {
            detach(shell, true);
         }

         ShenlinMod.LOGGER.info("[shenlin] boss dismissed id={} age={}", boss.id(), boss.age());
      }
   }

   private static void detach(ShenlinBossEntity shell, boolean tellClient) {
      if (tellClient && shell.level() instanceof ServerLevel level) {
         ModPackets.sendBossShellGone(level, shell.getId());
      }

      if (!shell.isRemoved()) {
         shell.release();
         shell.discard();
      }
   }

   public static ShenlinBoss of(Entity entity) {
      return entity instanceof ShenlinBossEntity shell ? shell.boss : null;
   }

   public static int count() {
      return ALIVE.size();
   }

   public static List<ShenlinBoss> alive() {
      return List.copyOf(ALIVE);
   }

   public static void clear() {
      for (ShenlinBoss boss : List.copyOf(ALIVE)) {
         unpin(boss);
         boss.finish();
         ShenlinBossEntity shell = boss.proxy();
         boss.setProxy(null);
         if (shell != null) {
            detach(shell, true);
         }
      }

      ALIVE.clear();
   }

   public static int withdraw(MinecraftServer server, String typePath) {
      if (ModEntities.SHENLIN_BOSS.getId().getPath().equals(typePath)) {
         return withdrawBosses();
      } else {
         return ModEntities.RELIC_WARDEN.getId().getPath().equals(typePath) ? withdrawWardens(server) : 0;
      }
   }

   public static int kill(MinecraftServer server, String typePath) {
      if (ModEntities.RELIC_WARDEN.getId().getPath().equals(typePath)) {
         int n = 0;

         for (RelicWardenEntity warden : wardens(server)) {
            if (!warden.isRemoved()) {
               warden.beginDeathShow();
               n++;
            }
         }

         return n;
      } else {
         return 0;
      }
   }

   public static int held(MinecraftServer server, String typePath) {
      if (ModEntities.SHENLIN_BOSS.getId().getPath().equals(typePath)) {
         return ALIVE.size();
      } else {
         return ModEntities.RELIC_WARDEN.getId().getPath().equals(typePath) ? wardens(server).size() : 0;
      }
   }

   public static List<String> report(MinecraftServer server) {
      List<String> lines = new ArrayList<>();
      List<ShenlinBoss> all = List.copyOf(ALIVE);
      lines.add("[名册] " + all.size() + " 位");
      int index = 0;

      for (ShenlinBoss boss : all) {
         lines.add("#" + ++index + " uuid=" + boss.id() + " age=" + boss.age() + " alive=" + boss.alive());
         lines.add("    位置 " + num(boss.x()) + "/" + num(boss.y()) + "/" + num(boss.z()) + " 朝向 " + num(boss.yRot()) + "/" + num(boss.xRot()));
         ShenlinBossEntity shell = boss.proxy();
         if (shell == null) {
            lines.add("    壳：没拿到");
         } else {
            ServerLevel level = boss.level();
            AABB box = shell.getBoundingBox();
            lines.add(
               "    账 byId="
                  + (level.getEntity(shell.getId()) == shell)
                  + " byUuid="
                  + (level.getEntities().get(shell.getUUID()) == shell)
                  + " byType="
                  + inTypeIndex(shell, boss)
                  + " byArea="
                  + RosterRestore.collidable(level, shell)
                  + " tracked="
                  + RosterRestore.tracked(level, shell)
                  + " 整体="
                  + RosterRestore.intact(level, shell)
            );
            lines.add("    箱 " + num(box.getXsize()) + "x" + num(box.getYsize()) + " 隐形=" + shell.isInvisible());
         }
      }

      return lines;
   }

   private static String num(double value) {
      return String.format("%.2f", value);
   }

   public static List<ShenlinBoss> guardedOnes(MinecraftServer server) {
      List<ShenlinBoss> out = new ArrayList<>();

      for (ShenlinBoss boss : ALIVE) {
         BlockPos at = BlockPos.containing(boss.pos());
         if (BossRegistry.guarded(server, boss.level().dimension(), at)) {
            out.add(boss);
         }
      }

      return out;
   }

   private static int withdrawBosses() {
      int n = 0;
      ServerLevel last = null;

      for (ShenlinBoss boss : List.copyOf(ALIVE)) {
         last = boss.level();
         BossRegistry.release(boss.level().getServer(), boss.level().dimension(), BlockPos.containing(boss.x(), boss.y(), boss.z()), "被特定指令清除");
         unpin(boss);
         boss.finish();
         ALIVE.remove(boss);
         ShenlinBossEntity shell = boss.proxy();
         boss.setProxy(null);
         if (shell != null) {
            detach(shell, true);
         }

         n++;
      }

      if (ALIVE.isEmpty() && last != null) {
         ModPackets.sendBossShellGone(last, -1);
      }

      shellDenied = 0;
      return n;
   }

   private static List<RelicWardenEntity> wardens(MinecraftServer server) {
      List<RelicWardenEntity> found = new ArrayList<>();
      if (server == null) {
         return found;
      }

      for (ServerLevel level : server.getAllLevels()) {
         found.addAll(level.getEntities(EntityTypeTest.forClass(RelicWardenEntity.class), warden -> true));
      }

      return found;
   }

   private static int withdrawWardens(MinecraftServer server) {
      int n = 0;

      for (RelicWardenEntity warden : wardens(server)) {
         if (!warden.isRemoved()) {
            warden.retire();
            n++;
         }
      }

      return n;
   }

   private static final class Damned {
      private final UUID id;
      private final double x;
      private final double y;
      private final double z;
      private final float yaw;
      private final float pitch;

      private Damned(ServerPlayer player) {
         this.id = player.getUUID();
         this.x = player.getX();
         this.y = player.getY();
         this.z = player.getZ();
         this.yaw = player.getYRot();
         this.pitch = player.getXRot();
      }
   }
}

