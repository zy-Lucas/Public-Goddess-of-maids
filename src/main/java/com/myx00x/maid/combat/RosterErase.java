package com.myx00x.maid.combat;

import com.myx00x.maid.ShenlinMod;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.registries.ForgeRegistries;

final class RosterErase {
   private static final Map<String, List<Class<?>>> TABLES = new ConcurrentHashMap<>();
   private static final int SCAN_LIMIT = 6000;
   private static final boolean TRACE = false;
   private static final int MAX_DEPTH = 60;
   private static final Map<String, Optional<URL>> BY_NAMESPACE = new ConcurrentHashMap<>();
   private static final Map<String, Boolean> MISSED = new ConcurrentHashMap<>();

   private RosterErase() {
   }

   static int erase(Entity target) {
      String who = target == null ? "null" : target.getType().getDescriptionId();
      if (target == null) {
         stop(who, "target-null");
         return 0;
      }

      Class<?> type = target.getClass();
      URL from = sourceOf(type);
      if (from == null) {
         URL viaMod = byNamespace(target);
         if (viaMod != null) {
            ShenlinMod.LOGGER.info("[shenlin] roster jar by namespace for {}: {}", who, viaMod);
         }

         from = viaMod;
      }

      URL jar = from;
      trace(who, "start class={} jar={}", type.getName(), jar);
      if (jar == null) {
         stop(who, "no-jar-url");
         return 0;
      }

      ClassLoader loader = type.getClassLoader();
      if (loader == null) {
         stop(who, "no-classloader");
         return 0;
      }

      String prefix = prefixOf(type);
      List<Class<?>> holders = TABLES.computeIfAbsent(jar + "|" + prefix, k -> scan(jar, prefix, loader, who));
      if (holders.isEmpty()) {
         stop(who, "scan-found-nothing prefix=" + prefix);
         return 0;
      }

      trace(who, "scan hit {} holder(s): {}", holders.size(), names(holders));
      int removed = 0;
      int looked = 0;

      for (Class<?> holder : holders) {
         for (Field f : holder.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) {
               Class<?> kind = f.getType();
               boolean asMap = Map.class.isAssignableFrom(kind);
               if (asMap || Collection.class.isAssignableFrom(kind)) {
                  try {
                     f.setAccessible(true);
                     Object raw = f.get(null);
                     int n = 0;
                     if (asMap && raw instanceof Map<?, ?> map) {
                        looked += map.size();
                        n = drop(map, target);
                     } else if (raw instanceof Collection<?> group) {
                        looked += group.size();
                        n = dropIn(group, target);
                     }

                     if (n > 0) {
                        trace(who, "table {}#{} dropped={}", holder.getName(), f.getName(), n);
                     }

                     removed += n;
                  } catch (Throwable t) {
                     noteMiss(holder.getName() + "#" + f.getName(), t);
                  }
               }
            }
         }
      }

      if (removed == 0) {
         stop(who, "no-entry-match looked=" + looked + " holders=" + holders.size());
      } else {
         ShenlinMod.LOGGER.info("[shenlin] roster erased {} entries for {}", removed, who);
      }

      return removed;
   }

   private static int drop(Map<?, ?> map, Entity target) {
      UUID id = target.getUUID();
      int removed = 0;

      try {
         Iterator<? extends Entry<?, ?>> it = map.entrySet().iterator();

         while (it.hasNext()) {
            Entry<?, ?> entry = (Entry<?, ?>)it.next();
            if (holds(entry.getValue(), target)) {
               Object key = entry.getKey();
               if (key == target || id.equals(key) || key instanceof Entity ke && ke.getUUID().equals(id)) {
                  it.remove();
                  removed++;
               }
            }
         }
      } catch (Throwable t) {
         noteMiss("drop-failed", t);
      }

      return removed;
   }

   private static int dropIn(Collection<?> group, Entity target) {
      int removed = 0;

      try {
         Iterator<?> it = group.iterator();

         while (it.hasNext()) {
            if (holds(it.next(), target)) {
               it.remove();
               removed++;
            }
         }
      } catch (Throwable t) {
         noteMiss("dropIn-failed", t);
      }

      return removed;
   }

   private static boolean holds(Object value, Entity target) {
      return holds(value, target, new IdentityHashMap<>(), 0);
   }

   private static boolean holds(Object value, Entity target, IdentityHashMap<Object, Boolean> seen, int depth) {
      if (value != null && depth <= 60) {
         if (value == target) {
            return true;
         } else if (value instanceof Entity entity) {
            return entity.getUUID().equals(target.getUUID());
         } else {
            if (seen.put(value, Boolean.TRUE) != null) {
               return false;
            }

            if (value instanceof Map<?, ?> map) {
               for (Object one : map.values()) {
                  if (holds(one, target, seen, depth + 1)) {
                     return true;
                  }
               }

               return false;
            } else if (value instanceof Iterable) {
               for (Object one : (Iterable)value) {
                  if (holds(one, target, seen, depth + 1)) {
                     return true;
                  }
               }

               return false;
            } else {
               Class<?> shell = value.getClass();
               if (shell.getName().startsWith("java.")) {
                  return false;
               }

               for (Field f : shell.getDeclaredFields()) {
                  if (!Modifier.isStatic(f.getModifiers())) {
                     Class<?> kind = f.getType();
                     if (Entity.class.isAssignableFrom(kind) || Map.class.isAssignableFrom(kind) || Iterable.class.isAssignableFrom(kind)) {
                        try {
                           f.setAccessible(true);
                           if (holds(f.get(value), target, seen, depth + 1)) {
                              return true;
                           }
                        } catch (Throwable t) {
                           noteMiss("holds:" + shell.getName() + "#" + f.getName(), t);
                        }
                     }
                  }
               }

               return false;
            }
         }
      } else {
         return false;
      }
   }

   private static URL sourceOf(Class<?> type) {
      try {
         CodeSource code = type.getProtectionDomain() != null ? type.getProtectionDomain().getCodeSource() : null;
         return code != null ? code.getLocation() : null;
      } catch (Throwable t) {
         noteMiss("sourceOf:" + type.getName(), t);
         return null;
      }
   }

   private static URL byNamespace(Entity target) {
      String ns;
      try {
         ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
         if (id == null) {
            return null;
         }

         ns = id.getNamespace();
      } catch (Throwable t) {
         noteMiss("byNamespace", t);
         return null;
      }

      return !"minecraft".equals(ns) && !"forge".equals(ns) ? BY_NAMESPACE.computeIfAbsent(ns, RosterErase::jarOfMod).orElse(null) : null;
   }

   private static Optional<URL> jarOfMod(String modid) {
      try {
         IModFileInfo info = ModList.get().getModFileById(modid);
         if (info == null) {
            return Optional.empty();
         }

         Path path = info.getFile().getFilePath();
         return path != null && Files.isRegularFile(path) ? Optional.of(path.toFile().toURI().toURL()) : Optional.empty();
      } catch (Throwable t) {
         noteMiss("jarOfMod:" + modid, t);
         return Optional.empty();
      }
   }

   private static File fileOf(URL url) {
      if (url == null) {
         return null;
      }

      String s = url.toString();
      if (s.startsWith("jar:")) {
         s = s.substring(4);
      }

      int bang = s.indexOf("!/");
      if (bang >= 0) {
         s = s.substring(0, bang);
      }

      int jarEnd = s.indexOf(".jar!");
      if (jarEnd >= 0) {
         s = s.substring(0, jarEnd + 4);
      }

      for (String scheme : new String[]{"union:", "file:"}) {
         if (s.startsWith(scheme)) {
            s = s.substring(scheme.length());
            break;
         }
      }

      s = decode(s);
      int hash = s.indexOf(35);
      if (hash >= 0) {
         s = s.substring(0, hash);
      }

      if (s.length() > 2 && s.charAt(0) == '/' && s.charAt(2) == ':') {
         s = s.substring(1);
      }

      try {
         File f = new File(s);
         if (f.isFile()) {
            return f;
         }

         try {
            File viaUri = new File(url.toURI());
            if (viaUri.isFile()) {
               return viaUri;
            }
         } catch (Throwable var8) {
         }

         return f;
      } catch (Throwable t) {
         noteMiss("fileOf:" + url, t);
         return null;
      }
   }

   private static String decode(String s) {
      try {
         return URLDecoder.decode(s, StandardCharsets.UTF_8);
      } catch (Throwable t) {
         return s;
      }
   }

   private static String prefixOf(Class<?> type) {
      String name = type.getName();
      int first = name.indexOf(46);
      if (first < 0) {
         return name;
      }

      int second = name.indexOf(46, first + 1);
      return second < 0 ? name.substring(0, first) : name.substring(0, second);
   }

   private static List<Class<?>> scan(URL jar, String prefix, ClassLoader loader, String who) {
      List<Class<?>> found = new ArrayList<>();
      File file = fileOf(jar);
      if (file == null) {
         stop(who, "no-jar-file url=" + jar);
         return found;
      }

      if (!file.isFile()) {
         stop(who, "jar-not-a-file url=" + jar + " path=" + file.getPath());
         return found;
      }

      trace(who, "scanning {} prefix={}", file.getName(), prefix);

      try (JarFile archive = new JarFile(file)) {
         int seen = 0;
         int total = 0;
         int held = 0;
         Enumeration<JarEntry> entries = archive.entries();

         while (entries.hasMoreElements() && seen < 6000) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.endsWith(".class") && !name.startsWith("META-INF/")) {
               String type = name.substring(0, name.length() - 6).replace('/', '.');
               if (type.startsWith(prefix)) {
                  seen++;

                  try {
                     Class<?> c = Class.forName(type, false, loader);
                     total++;
                     if (holdsTable(c)) {
                        found.add(c);
                        held++;
                     }
                  } catch (Throwable t) {
                     noteMiss(type, t);
                  }
               }
            }
         }

         trace(who, "scan done: walked={} loaded={} withTable={} limitHit={}", seen, total, held, seen >= 6000);
      } catch (Throwable t) {
         ShenlinMod.LOGGER.warn("[shenlin] roster table scan failed: {}", t.toString());
      }

      return found;
   }

   private static void stop(String who, String why) {
      ShenlinMod.LOGGER.info("[shenlin] roster stop {}: {}", who, why);
   }

   private static void trace(String who, String fmt, Object... args) {
   }

   private static String names(List<Class<?>> types) {
      StringBuilder sb = new StringBuilder();

      for (Class<?> c : types) {
         if (sb.length() > 0) {
            sb.append(',');
         }

         sb.append(c.getSimpleName());
      }

      return sb.toString();
   }

   private static void noteMiss(String type, Throwable cause) {
      if (!type.contains(".mixin.") && !type.contains(".compat.")) {
         if (MISSED.putIfAbsent(type, Boolean.TRUE) == null) {
            ShenlinMod.LOGGER.warn("[shenlin] roster scan skipped {}: {}", type, cause.toString());
         }
      }
   }

   private static boolean holdsTable(Class<?> type) {
      for (Field f : type.getDeclaredFields()) {
         if (Modifier.isStatic(f.getModifiers())) {
            Class<?> kind = f.getType();
            if (Map.class.isAssignableFrom(kind) || Collection.class.isAssignableFrom(kind)) {
               return true;
            }
         }
      }

      return false;
   }
}

