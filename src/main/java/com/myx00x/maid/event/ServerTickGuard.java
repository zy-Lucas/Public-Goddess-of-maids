package com.myx00x.maid.event;

import com.myx00x.maid.ShenlinMod;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

public final class ServerTickGuard {
   private static long lastTick = Long.MIN_VALUE;
   private static boolean lastFallback;
   private static boolean primed;
   private static int quietTicks;

   private ServerTickGuard() {
   }

   public static void viaEvent(MinecraftServer server) {
      run(server, false);
   }

   public static void viaFallback(MinecraftServer server) {
      run(server, true);
   }

   private static void run(MinecraftServer server, boolean fallback) {
      if (server != null) {
         long tick = server.getTickCount();
         if (tick != lastTick) {
            lastTick = tick;
            if (primed && fallback != lastFallback) {
               if (fallback) {
                  quietTicks = 0;
                  ShenlinMod.LOGGER
                     .warn(
                        "[shenlin] world tick events went quiet, running on the vanilla tick instead (bus={})", MinecraftForge.EVENT_BUS.getClass().getName()
                     );
               } else {
                  ShenlinMod.LOGGER.info("[shenlin] world tick events are back (fallback ran {} ticks)", quietTicks);
               }
            }

            primed = true;
            lastFallback = fallback;
            if (fallback) {
               quietTicks++;
            }

            try {
               ServerTickHandler.core(server);
            } catch (Throwable t) {
               ShenlinMod.LOGGER.error("[shenlin] tick dispatch failed", t);
            }
         }
      }
   }

   public static void reset() {
      lastTick = Long.MIN_VALUE;
      lastFallback = false;
      primed = false;
      quietTicks = 0;
   }
}

