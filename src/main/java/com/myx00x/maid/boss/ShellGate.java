package com.myx00x.maid.boss;

import com.myx00x.maid.ShenlinMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public final class ShellGate {
   private static int held;
   private static long heldLoggedAt;
   private static final long HELD_LOG_MS = 5000L;

   private ShellGate() {
   }

   public static boolean hold(ShenlinBossEntity shell, String where) {
      try {
         if (shell == null || !holding(shell)) {
            return false;
         }
      } catch (Throwable t) {
         return false;
      }

      held++;
      long now = System.currentTimeMillis();
      if (now - heldLoggedAt >= 5000L) {
         heldLoggedAt = now;
         ShenlinMod.LOGGER.info("[shenlin] shell gate held where={} id={} x{}", new Object[]{where, shell.getId(), held});
      }

      return true;
   }

   private static boolean holding(ShenlinBossEntity shell) {
      ShenlinBoss boss = shell.boss;
      if (boss == null || boss.proxy() != shell || !boss.alive()) {
         return false;
      }

      if (shell.isRemoved() || !shell.isAlive()) {
         return false;
      }

      if (Double.isFinite(shell.getX()) && Double.isFinite(shell.getY()) && Double.isFinite(shell.getZ())) {
         if (shell.level() instanceof ServerLevel level) {
            MinecraftServer server = level.getServer();
            if (server != null && server.isRunning()) {
               double dx = shell.getX() - boss.x();
               double dy = shell.getY() - boss.y();
               double dz = shell.getZ() - boss.z();
               boolean atHome = dx * dx + dy * dy + dz * dz <= 4.0;
               return !atHome || level.hasChunkAt(BlockPos.containing(boss.x(), boss.y(), boss.z()));
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }
}

