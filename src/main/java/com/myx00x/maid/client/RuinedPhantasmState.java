package com.myx00x.maid.client;

import com.myx00x.maid.ShenlinMod;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class RuinedPhantasmState {
   private static final Map<Integer, double[]> SPOT = new ConcurrentHashMap<>();
   private static final Map<Integer, Boolean> ALIVE = new ConcurrentHashMap<>();

   private RuinedPhantasmState() {
   }

   public static void apply(int id, double x, double y, double z, float yRot, float xRot, int millis, boolean alive) {
      long until = System.currentTimeMillis() + Math.max(0, millis);
      SPOT.put(id, new double[]{x, y, z, yRot, xRot, until});
      ALIVE.put(id, alive);
      if (!alive) {
         SPOT.remove(id);
      }
   }

   public static int[] liveIds() {
      long now = System.currentTimeMillis();
      int[] buf = new int[SPOT.size()];
      int n = 0;

      for (Entry<Integer, double[]> e : SPOT.entrySet()) {
         double[] spot = e.getValue();
         if (spot != null && now <= spot[5] && Boolean.TRUE.equals(ALIVE.get(e.getKey()))) {
            buf[n++] = e.getKey();
         }
      }

      int[] out = new int[n];
      System.arraycopy(buf, 0, out, 0, n);
      return out;
   }

   public static double[] spotStale(int id) {
      return SPOT.get(id);
   }

   public static boolean aliveNow(int id) {
      double[] spot = SPOT.get(id);
      return spot != null && System.currentTimeMillis() <= spot[5] && Boolean.TRUE.equals(ALIVE.get(id));
   }

   public static boolean anyAlive() {
      return liveIds().length > 0;
   }

   public static void clear() {
      SPOT.clear();
      ALIVE.clear();
      ShenlinMod.LOGGER.info("[shenlin] ruined phantasm client state cleared");
   }
}

