package com.myx00x.maid.mob;

import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;

public final class WardenVitalsKeeper {
   private static final long DEFAULT_INTERVAL_MS = 10L;
   private static final long GRUMBLE_GAP_MS = 30000L;
   private static Thread thread;
   private static long lastGrumble;

   private WardenVitalsKeeper() {
   }

   public static synchronized void start() {
      if (thread == null) {
         thread = new Thread(WardenVitalsKeeper::loop, "shenlin-warden-vitals");
         thread.setDaemon(true);
         thread.start();
      }
   }

   private static void loop() {
      while (true) {
         try {
            Thread.sleep(Math.max(1L, Config.relicWardenSweepMillis()));

            for (RelicWardenEntity warden : RelicWardenEntity.watched()) {
               warden.settleVitals(false);
            }
         } catch (InterruptedException e) {
            return;
         } catch (Throwable t) {
            long stamp = System.currentTimeMillis();
            if (stamp - lastGrumble > 30000L) {
               lastGrumble = stamp;
               ShenlinMod.LOGGER.warn("[shenlin] warden vitals keeper hiccup", t);
            }
         }
      }
   }
}

