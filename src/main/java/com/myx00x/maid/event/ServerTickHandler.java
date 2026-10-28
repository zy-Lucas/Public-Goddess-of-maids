package com.myx00x.maid.event;

import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.boss.RuinedPhantasmKeeper;
import com.myx00x.maid.boss.ShenlinBossKeeper;
import com.myx00x.maid.combat.BlackHoleKeeper;
import com.myx00x.maid.combat.EraseWatch;
import com.myx00x.maid.combat.ExecutionCounter;
import com.myx00x.maid.combat.ExecutionSequencer;
import com.myx00x.maid.combat.GuardBook;
import com.myx00x.maid.combat.GuardKeeper;
import com.myx00x.maid.combat.GuardRemoval;
import com.myx00x.maid.combat.GuardWatch;
import com.myx00x.maid.combat.HateBoard;
import com.myx00x.maid.combat.HeartGuard;
import com.myx00x.maid.combat.MaidCollectConsent;
import com.myx00x.maid.combat.MaidRemoval;
import com.myx00x.maid.combat.RetreatManager;
import com.myx00x.maid.combat.SealField;
import com.myx00x.maid.combat.ShieldWatcher;
import com.myx00x.maid.combat.SlabAudit;
import com.myx00x.maid.combat.SlashPulse;
import com.myx00x.maid.combat.TargetKeeper;
import com.myx00x.maid.combat.TimeKeeper;
import com.myx00x.maid.combat.TrueDamageOverrider;
import com.myx00x.maid.combat.WormholeRecall;
import com.myx00x.maid.compat.Upstream;
import com.myx00x.maid.world.RelicEyeSearch;
import com.myx00x.maid.world.RelicOpening;
import com.myx00x.maid.world.RelicSites;
import com.myx00x.maid.world.RelicWatch;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ServerTickHandler {
   private static long cleanupCounter = 0L;
   private static final long SLOW_MS = 3L;
   private static final Map<String, long[]> SLOW = new ConcurrentHashMap<>();
   private static final Map<String, long[]> TOTAL = new ConcurrentHashMap<>();

   @SubscribeEvent
   public static void onServerTick(ServerTickEvent event) {
      if (event.phase == Phase.END) {
         ServerTickGuard.viaEvent(event.getServer());
      }
   }

   static void core(MinecraftServer server) {
      boolean maid = Upstream.MAID;
      if (maid) {
         guard(TrueDamageOverrider::processQueued, "true damage");
         guard(ExecutionSequencer::tick, "execute");
      }

      guard(() -> ShenlinBossKeeper.tick(server), "boss");
      guard(() -> RuinedPhantasmKeeper.tick(server), "ruined phantasm");
      guard(() -> EraseWatch.tick(server), "erase watch");
      guard(() -> RelicWatch.tick(server), "relic");
      guard(() -> RelicOpening.tick(server), "relic opening");
      guard(() -> RelicSites.drain(server), "relic sites");
      guard(() -> RelicEyeSearch.tick(server), "relic eye");
      if (maid) {
         guard(() -> SealField.tick(server), "seal");
         if ((Boolean)Config.GUARD_REWRITE.get()) {
            guard(() -> TargetKeeper.sweep(server), "keeper");
            guard(() -> GuardWatch.tick(server), "watch");
            guard(() -> SlabAudit.tick(server), "slab");
            guard(() -> TimeKeeper.tick(server), "time");
            guard(() -> GuardBook.tick(server), "book");
         } else {
            guard(() -> ShieldWatcher.tickScan(server), "shield");
            guard(() -> TargetKeeper.sweep(server), "keeper");
         }

         guard(() -> HateBoard.sweep(server), "hate");
         guard(() -> BlackHoleKeeper.tick(server), "hole");
         guard(() -> WormholeRecall.sweep(server), "wormhole");
         if (++cleanupCounter % 200L == 0L) {
            ExecutionCounter.cleanup();
            if (server != null && server.overworld() != null) {
               long now = server.overworld().getGameTime();
               TargetKeeper.cleanup(now);
               SlashPulse.cleanup(now);
               RetreatManager.cleanup(now);
               MaidRemoval.cleanup(now);
               GuardRemoval.cleanup(now);
               MaidCollectConsent.cleanup(now);
               HeartGuard.cleanup(now);
               HateBoard.cleanup(now);
               GuardKeeper.sweep(now);
            }

            reportSlow();
            reportTotal();
         }
      }
   }

   private static void guard(Runnable step, String what) {
      long began = System.nanoTime();

      try {
         step.run();
      } catch (Throwable t) {
         ShenlinMod.LOGGER.error("[shenlin] {} step failed", what, t);
      }

      long nanos = System.nanoTime() - began;
      long[] all = TOTAL.computeIfAbsent(what, key -> new long[2]);
      all[0] += nanos;
      all[1]++;
      long spent = nanos / 1000000L;
      if (spent >= 3L) {
         long[] acc = SLOW.computeIfAbsent(what, key -> new long[3]);
         acc[0] += spent;
         acc[1]++;
         if (spent > acc[2]) {
            acc[2] = spent;
         }
      }
   }

   private static void reportSlow() {
      if (!SLOW.isEmpty()) {
         StringBuilder line = new StringBuilder("[shenlin] tick cost");

         for (Entry<String, long[]> entry : SLOW.entrySet()) {
            long[] acc = entry.getValue();
            line.append(' ').append(entry.getKey().replace(' ', '_')).append('=').append(acc[0]).append("ms/").append(acc[1]);
         }

         SLOW.clear();
         ShenlinMod.LOGGER.info("{}", line);
      }
   }

   private static void reportTotal() {
      if (!TOTAL.isEmpty()) {
         StringBuilder line = new StringBuilder("[shenlin] tick share");
         long sum = 0L;

         for (Entry<String, long[]> entry : TOTAL.entrySet()) {
            long[] all = entry.getValue();
            if (all[1] != 0L) {
               long micros = all[0] / all[1] / 1000L;
               sum += micros;
               line.append(' ').append(entry.getKey().replace(' ', '_')).append('=').append(micros).append("us");
            }
         }

         TOTAL.clear();
         line.append(" total=").append(sum).append("us");
         ShenlinMod.LOGGER.info("{}", line);
      }
   }
}

