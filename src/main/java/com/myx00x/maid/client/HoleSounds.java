package com.myx00x.maid.client;

import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT, bus = Bus.FORGE)
public final class HoleSounds {
   static final long START_MS = 8350L;
   static final long FADE_MS = 600L;
   private static final List<HoleSound> ALIVE = new ArrayList<>();
   private static final int MAX_ALIVE = 32;
   private static final int FADE_SLACK = 8;

   private HoleSounds() {
   }

   static float master() {
      return 1.0F;
   }

   public static void begin(double x, double y, double z, float warp, long lifeMs) {
      if ((Boolean)Config.BLACK_HOLE_SOUND_ENABLED.get()) {
         if (WorldGate.ready()) {
            if (ALIVE.size() >= 32) {
               for (HoleSound oldest : ALIVE) {
                  if (!oldest.isFading()) {
                     oldest.beginFadeOut();
                     break;
                  }
               }
            }

            if (ALIVE.size() >= 40) {
               HoleSound drop = ALIVE.remove(0);
               drop.stop();
            }

            double extra = (Double)Config.BLACK_HOLE_SOUND_EXTRA.get();
            HoleSound sound = new HoleSound(x, y, z, warp, extra, lifeMs);
            sound.begin();
            ALIVE.add(sound);
            ShenlinMod.LOGGER
               .info(
                  "[shenlin] hole sound begin at {} warp={} extra={} live={}",
                  new Object[]{String.format("%.2f/%.2f/%.2f", x, y, z), warp, extra, ALIVE.size()}
               );
         }
      }
   }

   public static void fadeNear(double x, double y, double z, float radius) {
      double sq = (double)radius * radius;
      int n = 0;

      for (HoleSound sound : ALIVE) {
         if (sound.distanceSqr(x, y, z) < sq) {
            sound.beginFadeOut();
            n++;
         }
      }

      if (n > 0) {
         ShenlinMod.LOGGER.info("[shenlin] hole sound fading out n={}", n);
      }
   }

   public static void clear() {
      for (HoleSound sound : ALIVE) {
         sound.stop();
      }

      ALIVE.clear();
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.END && !ALIVE.isEmpty()) {
         Iterator<HoleSound> it = ALIVE.iterator();

         while (it.hasNext()) {
            HoleSound sound = it.next();

            try {
               sound.update();
            } catch (Throwable t) {
               ShenlinMod.LOGGER.warn("[shenlin] hole sound tick failed: {}", t.toString());
               sound.stop();
               it.remove();
               continue;
            }

            if (sound.isFinished()) {
               it.remove();
               ShenlinMod.LOGGER.info("[shenlin] hole sound done live={}", ALIVE.size());
            }
         }
      }
   }

   public static int count() {
      return ALIVE.size();
   }
}

