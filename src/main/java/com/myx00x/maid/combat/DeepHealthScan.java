package com.myx00x.maid.combat;

import com.myx00x.maid.ShenlinMod;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;

final class DeepHealthScan {
   private static final long COOLDOWN_MS = 60000L;
   private static final int MAX_CLASSES = 5000;
   private static final int MAX_ENTRIES = 200000;
   private static final long BUDGET_MS = 1500L;
   private static final float TOLERANCE = 0.01F;
   private static final Map<UUID, DeepHealthScan.Job> JOBS = new ConcurrentHashMap<>();
   private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "shenlin-deep-scan");
      t.setDaemon(true);
      return t;
   });

   private DeepHealthScan() {
   }

   static void request(LivingEntity target, float want) {
      if (target != null && target.isAlive()) {
         if (target.level() != null && !target.level().isClientSide) {
            MinecraftServer server = target.level().getServer();
            if (server != null) {
               UUID id = target.getUUID();
               long now = System.currentTimeMillis();
               DeepHealthScan.Job job = JOBS.get(id);
               if (job != null) {
                  if (job.running()) {
                     return;
                  }

                  if (now - job.finishedAt < 60000L) {
                     return;
                  }
               }

               DeepHealthScan.Job fresh = new DeepHealthScan.Job();
               JOBS.put(id, fresh);
               float probe = target.getHealth();
               Class<?> type = target.getClass();
               ClassLoader loader = type.getClassLoader();
               WORKER.execute(() -> run(server, id, type, loader, probe, want, fresh));
            }
         }
      }
   }

   private static void run(MinecraftServer server, UUID id, Class<?> type, ClassLoader loader, float probe, float want, DeepHealthScan.Job job) {
      if (job.state == DeepHealthScan.State.QUEUED) {
         job.state = DeepHealthScan.State.RUNNING;
         List<DeepHealthScan.Hit> hits = Collections.emptyList();

         try {
            hits = scan(type, loader, probe);
         } catch (Throwable t) {
            ShenlinMod.LOGGER.warn("[shenlin] deep scan hiccup {}", t.toString());
         } finally {
            List<DeepHealthScan.Hit> found = hits;

            try {
               server.execute(() -> apply(id, found, want, job));
            } catch (Throwable t) {
               job.state = DeepHealthScan.State.DONE;
               job.finishedAt = System.currentTimeMillis();
            }
         }
      }
   }

   private static void apply(UUID id, List<DeepHealthScan.Hit> hits, float want, DeepHealthScan.Job job) {
      int wrote = 0;

      for (DeepHealthScan.Hit hit : hits) {
         try {
            if (hit.write(want)) {
               wrote++;
            }
         } catch (Throwable var8) {
         }
      }

      job.state = DeepHealthScan.State.DONE;
      job.finishedAt = System.currentTimeMillis();
      if (wrote > 0) {
         ShenlinMod.LOGGER.info("[shenlin] deep scan wrote {} slot(s) for {} -> {}", new Object[]{wrote, id, String.format("%.2f", want)});
      }
   }

   private static List<DeepHealthScan.Hit> scan(Class<?> type, ClassLoader loader, float probe) {
      List<DeepHealthScan.Hit> hits = new ArrayList<>();
      if (probe <= 0.0F) {
         return hits;
      }

      long deadline = System.currentTimeMillis() + 1500L;
      List<Class<?>> classes = candidates(type, loader);
      int seen = 0;

      for (Class<?> c : classes) {
         if (seen++ > 5000 || System.currentTimeMillis() > deadline) {
            break;
         }

         try {
            collect(c, probe, hits, deadline);
         } catch (Throwable var11) {
         }
      }

      return hits;
   }

   private static List<Class<?>> candidates(Class<?> type, ClassLoader loader) {
      List<Class<?>> out = new ArrayList<>();

      for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
         out.add(c);
      }

      URL jar = sourceOf(type);
      if (jar != null && "jar".equals(jar.getProtocol())) {
         String path = jar.getPath();
         int bang = path.indexOf("!/");
         if (bang > 0) {
            path = path.substring(0, bang);
         }

         try (JarFile file = new JarFile(path)) {
            for (JarEntry entry : Collections.list(file.entries())) {
               if (out.size() >= 5000) {
                  break;
               }

               String name = entry.getName();
               if (name.endsWith(".class") && !name.startsWith("META-INF/")) {
                  String cls = name.substring(0, name.length() - 6).replace('/', '.');
                  if (!cls.startsWith("java.") && !cls.startsWith("javax.") && !cls.startsWith("jdk.") && !cls.startsWith("com.myx00x.maid.")) {
                     try {
                        out.add(Class.forName(cls, false, loader));
                     } catch (Throwable var13) {
                     }
                  }
               }
            }
         } catch (Throwable var15) {
         }

         return out;
      } else {
         return out;
      }
   }

   private static URL sourceOf(Class<?> type) {
      try {
         CodeSource cs = type.getProtectionDomain().getCodeSource();
         return cs == null ? null : cs.getLocation();
      } catch (Throwable t) {
         return null;
      }
   }

   private static void collect(Class<?> holder, float probe, List<DeepHealthScan.Hit> hits, long deadline) {
      Field[] fields = holder.getDeclaredFields();

      for (Field f : fields) {
         if (Modifier.isStatic(f.getModifiers())) {
            if (System.currentTimeMillis() > deadline || hits.size() > 512) {
               return;
            }

            try {
               f.setAccessible(true);
               Class<?> t = f.getType();
               if (Map.class.isAssignableFrom(t)) {
                  if (f.get(null) instanceof Map<?, ?> map) {
                     collectMap(f, map, probe, hits, deadline);
                  }
               } else if (!Modifier.isFinal(f.getModifiers())) {
                  if (t == float.class) {
                     if (same(f.getFloat(null), probe)) {
                        hits.add(new DeepHealthScan.FieldHit(f, false));
                     }
                  } else if (t == double.class && same((float)f.getDouble(null), probe)) {
                     hits.add(new DeepHealthScan.FieldHit(f, true));
                  }
               }
            } catch (Throwable var13) {
            }
         }
      }

      try {
         for (Class<?> nested : holder.getDeclaredClasses()) {
            collect(nested, probe, hits, deadline);
         }
      } catch (Throwable var14) {
      }
   }

   private static void collectMap(Field f, Map<?, ?> map, float probe, List<DeepHealthScan.Hit> hits, long deadline) {
      int seen = 0;

      try {
         for (Entry<?, ?> entry : map.entrySet()) {
            if (++seen > 200000 || System.currentTimeMillis() > deadline) {
               return;
            }

            Object value = entry.getValue();
            if (value instanceof Float fl && same(fl, probe)) {
               hits.add(new DeepHealthScan.MapHit(map, entry.getKey(), false));
            } else if (value instanceof Double db && same((float)db.doubleValue(), probe)) {
               hits.add(new DeepHealthScan.MapHit(map, entry.getKey(), true));
            }
         }
      } catch (Throwable var12) {
      }
   }

   private static boolean same(float a, float b) {
      return a == b || Math.abs(a - b) <= 0.01F;
   }

   private record FieldHit(Field field, boolean wide) implements DeepHealthScan.Hit {
      @Override
      public boolean write(float want) {
         try {
            if (this.wide) {
               this.field.setDouble(null, want);
            } else {
               this.field.setFloat(null, want);
            }

            return true;
         } catch (Throwable t) {
            return false;
         }
      }
   }

   private sealed interface Hit permits DeepHealthScan.FieldHit, DeepHealthScan.MapHit {
      boolean write(float var1) throws Exception;
   }

   private static final class Job {
      private DeepHealthScan.State state = DeepHealthScan.State.QUEUED;
      private long finishedAt;

      private boolean running() {
         return this.state != DeepHealthScan.State.DONE;
      }
   }

   private record MapHit(Map<?, ?> map, Object key, boolean wide) implements DeepHealthScan.Hit {
      @Override
      public boolean write(float want) {
         try {
            Map raw = this.map;
            raw.put(this.key, this.wide ? (double)want : want);
            return true;
         } catch (Throwable t) {
            return false;
         }
      }
   }

   private enum State {
      QUEUED,
      RUNNING,
      DONE;
   }
}

