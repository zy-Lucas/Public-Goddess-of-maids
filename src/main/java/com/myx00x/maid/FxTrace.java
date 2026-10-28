package com.myx00x.maid;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.world.phys.Vec3;

public final class FxTrace {
   public static final boolean ON = true;
   private static final long THROTTLE_MS = 200L;
   private static final Map<String, Long> LAST_AT = new HashMap<>();

   private FxTrace() {
   }

   public static void fx(String kind, String reason, Vec3 at, String extra) {
      ShenlinMod.LOGGER
         .info("[shenlin][fx] wall={} kind={} reason={} at={} {}", new Object[]{System.currentTimeMillis(), kind, reason, fmt(at), extra == null ? "" : extra});
   }

   public static void fx(String kind, String reason, Vec3 at, long gameTime, String extra) {
      ShenlinMod.LOGGER
         .info(
            "[shenlin][fx] wall={} game={} kind={} reason={} at={} {}",
            new Object[]{System.currentTimeMillis(), gameTime, kind, reason, fmt(at), extra == null ? "" : extra}
         );
   }

   public static void throttled(String kind, String reason, Vec3 at, String extra) {
      long now = System.currentTimeMillis();
      Long last = LAST_AT.get(kind);
      if (last == null || now - last >= 200L) {
         LAST_AT.put(kind, now);
         fx(kind, reason, at, extra);
      }
   }

   public static String fmt(Vec3 at) {
      return at == null ? "null" : String.format(Locale.ROOT, "%.3f/%.3f/%.3f", at.x, at.y, at.z);
   }

   public static String f(double value) {
      return String.format(Locale.ROOT, "%.3f", value);
   }
}

