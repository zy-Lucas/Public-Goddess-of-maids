package com.myx00x.maid.client;

import com.myx00x.maid.ShenlinMod;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import net.minecraftforge.fml.loading.FMLPaths;

public final class ShaderPack {
   private static final long INTERVAL_MS = 200L;
   private static final long OFF_HOLD_MS = 1000L;
   private static final long FILE_INTERVAL_MS = 1000L;
   private static Method instanceMethod;
   private static Method inUseMethod;
   private static Object api;
   private static boolean apiUnavailable;
   private static boolean cached;
   private static long nextProbe;
   private static long lastOn;
   private static boolean announced;
   private static boolean fileEnabled;
   private static long nextFileProbe;

   private ShaderPack() {
   }

   public static boolean inUse() {
      long now = EffectClock.now();
      if (now < nextProbe) {
         return cached;
      }

      nextProbe = now + 200L;
      boolean live = probe();
      if (live) {
         lastOn = now;
      } else if (now - lastOn < 1000L) {
         live = true;
      }

      if (!announced || live != cached) {
         announced = true;
         ShenlinMod.LOGGER.info("[shenlin] shaderpack {}", live ? "on" : "off");
      }

      cached = live;
      return cached;
   }

   private static boolean probe() {
      return Boolean.TRUE.equals(apiInUse()) ? true : enabled();
   }

   private static Boolean apiInUse() {
      if (apiUnavailable) {
         return null;
      }

      try {
         if (api == null) {
            Class<?> type = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            instanceMethod = type.getMethod("getInstance");
            inUseMethod = type.getMethod("isShaderPackInUse");
            api = instanceMethod.invoke(null);
         }

         return inUseMethod.invoke(api) instanceof Boolean flag ? flag : null;
      } catch (Throwable t) {
         apiUnavailable = true;
         return null;
      }
   }

   private static boolean enabled() {
      long now = EffectClock.now();
      if (now < nextFileProbe) {
         return fileEnabled;
      }

      nextFileProbe = now + 1000L;
      fileEnabled = readConfig();
      return fileEnabled;
   }

   private static boolean readConfig() {
      try {
         for (String file : new String[]{"oculus.properties", "iris.properties"}) {
            Path path = FMLPaths.CONFIGDIR.get().resolve(file);
            if (Files.isReadable(path)) {
               String pack = null;
               boolean on = true;

               for (String line : Files.readAllLines(path)) {
                  int eq = line.indexOf(61);
                  if (eq >= 0) {
                     String key = line.substring(0, eq).trim();
                     String value = line.substring(eq + 1).trim();
                     if ("shaderPack".equals(key)) {
                        pack = value;
                     } else if ("enableShaders".equals(key)) {
                        on = Boolean.parseBoolean(value);
                     }
                  }
               }

               if (on && pack != null && !pack.isEmpty() && !"off".equals(pack.toLowerCase(Locale.ROOT))) {
                  return true;
               }
            }
         }
      } catch (Throwable var12) {
      }

      return false;
   }
}

