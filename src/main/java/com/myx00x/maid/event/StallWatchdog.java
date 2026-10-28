package com.myx00x.maid.event;

import com.myx00x.maid.ShenlinMod;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class StallWatchdog {
   private static final long STALL_MS = 8000L;
   private static final long HEAL_MS = 2000L;
   private static final long POLL_MS = 250L;
   private static volatile long serverBeat;
   private static volatile long clientBeat;
   private static volatile boolean serverArmed = true;
   private static volatile boolean clientArmed = true;
   private static Thread thread;

   private StallWatchdog() {
   }

   public static synchronized void start() {
      if (thread == null) {
         long now = nowMs();
         serverBeat = now;
         clientBeat = now;
         thread = new Thread(StallWatchdog::loop, "shenlin-stall-watch");
         thread.setDaemon(true);
         thread.start();
      }
   }

   public static void beatServer() {
      serverBeat = nowMs();
   }

   public static void beatClient() {
      clientBeat = nowMs();
   }

   @SubscribeEvent
   public static void onServerTick(ServerTickEvent event) {
      if (event.phase == Phase.END) {
         beatServer();
      }
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.END) {
         beatClient();
      }
   }

   private static void loop() {
      while (true) {
         try {
            Thread.sleep(250L);
            long now = nowMs();
            long serverGap = now - serverBeat;
            long clientGap = now - clientBeat;
            if (serverGap < 2000L) {
               serverArmed = true;
            }

            if (clientGap < 2000L) {
               clientArmed = true;
            }

            if (serverGap > 8000L && serverArmed) {
               serverArmed = false;
               dump("Server thread", serverGap);
            }

            if (clientGap > 8000L && clientArmed) {
               clientArmed = false;
               dump("Render thread", clientGap);
            }
         } catch (InterruptedException e) {
            return;
         } catch (Throwable t) {
            try {
               Thread.sleep(1000L);
            } catch (InterruptedException e) {
               return;
            }
         }
      }
   }

   private static void dump(String threadName, long gapMs) {
      ShenlinMod.LOGGER.error("[shenlin] {} stalled {}ms, stack follows", threadName, gapMs);
      ThreadMXBean bean = ManagementFactory.getThreadMXBean();

      try {
         long[] dead = bean.findDeadlockedThreads();
         if (dead != null && dead.length > 0) {
            for (ThreadInfo info : bean.getThreadInfo(dead, Integer.MAX_VALUE)) {
               ShenlinMod.LOGGER.error("[shenlin] deadlock: {}", info == null ? "?" : info.toString());
            }
         }
      } catch (Throwable var10) {
      }

      Map<Thread, StackTraceElement[]> all;
      try {
         all = Thread.getAllStackTraces();
      } catch (Throwable t) {
         ShenlinMod.LOGGER.error("[shenlin] stack dump failed", t);
         return;
      }

      boolean found = false;

      for (Entry<Thread, StackTraceElement[]> entry : all.entrySet()) {
         Thread it = entry.getKey();
         if (threadName.equals(it.getName())) {
            found = true;
            writeStack(it, entry.getValue(), Integer.MAX_VALUE);
         }
      }

      for (Entry<Thread, StackTraceElement[]> entry : all.entrySet()) {
         Thread it = entry.getKey();
         if (!threadName.equals(it.getName()) && busyName(it.getName())) {
            writeStack(it, entry.getValue(), 12);
         }
      }

      if (!found) {
         ShenlinMod.LOGGER.error("[shenlin] --- {} not found in thread list ---", threadName);
      }
   }

   private static boolean busyName(String name) {
      return name.startsWith("Worker-Main")
         || name.startsWith("Chunk Render")
         || name.startsWith("Render task")
         || name.startsWith("IO-Worker")
         || name.startsWith("Netty");
   }

   private static void writeStack(Thread thread, StackTraceElement[] stack, int limit) {
      ShenlinMod.LOGGER.error("[shenlin] --- {} state={} ---", thread.getName(), thread.getState());
      int n = Math.min(stack.length, limit);

      for (int i = 0; i < n; i++) {
         ShenlinMod.LOGGER.error("[shenlin]   at {}", stack[i]);
      }
   }

   private static long nowMs() {
      return System.nanoTime() / 1000000L;
   }
}

