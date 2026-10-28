package com.myx00x.maid.combat;

import com.myx00x.maid.ShenlinMod;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import net.minecraft.world.entity.LivingEntity;

final class BloodLedger {
   private static final Map<String, List<Class<?>>> LEDGERS = new ConcurrentHashMap<>();
   private static final int SCAN_LIMIT = 6000;
   private static final float TOLERANCE = 0.01F;

   private BloodLedger() {
   }

   static boolean write(LivingEntity target, float value) {
      if (ownFields(target, value)) {
         return true;
      } else {
         return byLedger(target, value) ? true : byNeighbors(target, value);
      }
   }

   private static boolean byNeighbors(LivingEntity target, float value) {
      float probe = target.getHealth();
      if (probe == 0.0F) {
         return false;
      }

      boolean wrote = false;

      for (Class<?> c = target.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
         for (Field f : c.getDeclaredFields()) {
            int mod = f.getModifiers();
            if (!Modifier.isStatic(mod) && !Modifier.isFinal(mod) && !isNumeric(f.getType()) && !isForeign(f.getType())) {
               try {
                  f.setAccessible(true);
                  Object held = f.get(target);
                  if (held != null && held != target && !isForeign(held.getClass())) {
                     wrote |= pokeFields(held, probe, value);
                  }
               } catch (Throwable var11) {
               }
            }
         }
      }

      return wrote;
   }

   private static boolean pokeFields(Object holder, float probe, float value) {
      boolean wrote = false;

      for (Class<?> c = holder.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
         for (Field f : c.getDeclaredFields()) {
            int mod = f.getModifiers();
            if (!Modifier.isStatic(mod) && !Modifier.isFinal(mod)) {
               Class<?> type = f.getType();
               boolean single = type == float.class || type == Float.class;
               boolean wide = type == double.class || type == Double.class;
               if (single || wide) {
                  try {
                     f.setAccessible(true);
                     if (f.get(holder) instanceof Number n && same(n.floatValue(), probe)) {
                        if (single) {
                           f.setFloat(holder, value);
                        } else {
                           f.setDouble(holder, value);
                        }

                        wrote = true;
                     }
                  } catch (Throwable var15) {
                  }
               }
            }
         }
      }

      return wrote;
   }

   private static boolean isNumeric(Class<?> type) {
      return type.isPrimitive() || Number.class.isAssignableFrom(type) || type == String.class || type.isEnum();
   }

   private static boolean isForeign(Class<?> type) {
      String name = type.getName();
      return name.startsWith("java.")
         || name.startsWith("javax.")
         || name.startsWith("jdk.")
         || name.startsWith("sun.")
         || name.startsWith("it.unimi.")
         || name.startsWith("com.google.")
         || name.startsWith("org.apache.");
   }

   private static boolean ownFields(LivingEntity target, float value) {
      float probe = target.getHealth();
      if (probe == 0.0F) {
         return false;
      }

      boolean wrote = false;

      for (Class<?> c = target.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
         for (Field f : c.getDeclaredFields()) {
            int mod = f.getModifiers();
            if (!Modifier.isStatic(mod) && !Modifier.isFinal(mod)) {
               Class<?> type = f.getType();
               boolean single = type == float.class || type == Float.class;
               boolean wide = type == double.class || type == Double.class;
               if (single || wide) {
                  try {
                     f.setAccessible(true);
                     if (f.get(target) instanceof Number n && same(n.floatValue(), probe)) {
                        if (single) {
                           f.setFloat(target, value);
                        } else {
                           f.setDouble(target, value);
                        }

                        wrote = true;
                     }
                  } catch (Throwable var15) {
                  }
               }
            }
         }
      }

      return wrote;
   }

   private static boolean byLedger(LivingEntity target, float value) {
      UUID id = target.getUUID();
      boolean wrote = false;

      for (Class<?> holder : samePrefixHolders(target)) {
         wrote |= pokeStaticMaps(holder, target, id, value);
      }

      for (Class<?> holder : bytecodeHolders(target)) {
         wrote |= pokeStaticMaps(holder, target, id, value);
      }

      return wrote;
   }

   private static List<Class<?>> samePrefixHolders(LivingEntity target) {
      URL jar = sourceOf(target.getClass());
      if (jar == null) {
         return List.of();
      }

      ClassLoader own = target.getClass().getClassLoader();
      ClassLoader loader = own != null ? own : BloodLedger.class.getClassLoader();
      String key = jar + "|" + prefixOf(target.getClass());
      return LEDGERS.computeIfAbsent(key, k -> scan(jar, prefixOf(target.getClass()), loader));
   }

   private static List<Class<?>> bytecodeHolders(LivingEntity target) {
      List<Class<?>> out = new ArrayList<>();
      Class<?> owner = getHealthOwner(target.getClass());
      byte[] bytes = owner == null ? null : classBytes(owner);
      if (bytes == null) {
         return out;
      }

      ClassLoader loader = target.getClass().getClassLoader();

      for (String internal : numericAccessorOwners(bytes)) {
         try {
            out.add(Class.forName(internal.replace('/', '.'), false, loader));
         } catch (Throwable var8) {
         }
      }

      return out;
   }

   private static Class<?> getHealthOwner(Class<?> type) {
      for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
         for (Method m : c.getDeclaredMethods()) {
            if (m.getParameterCount() == 0 && m.getReturnType() == float.class) {
               String n = m.getName();
               if (n.equals("getHealth") || n.equals("m_21223_") || n.equals("func_110143_aJ")) {
                  return c;
               }
            }
         }
      }

      return null;
   }

   private static byte[] classBytes(Class<?> type) {
      String path = type.getName().replace('.', '/') + ".class";
      ClassLoader loader = type.getClassLoader();
      if (loader == null) {
         loader = ClassLoader.getSystemClassLoader();
      }

      try (InputStream in = loader.getResourceAsStream(path)) {
         return in != null ? in.readAllBytes() : null;
      } catch (Throwable t) {
         return null;
      }
   }

   private static List<String> numericAccessorOwners(byte[] b) {
      List<String> out = new ArrayList<>();
      if (b != null && b.length >= 10 && readU4(b) == 3405691582L) {
         int count = readU2(b, 8);
         int[] tag = new int[count];
         int[] first = new int[count];
         int[] second = new int[count];
         String[] text = new String[count];
         int p = 10;

         for (int i = 1; i < count; i++) {
            if (p >= b.length) {
               return List.of();
            }

            int t = b[p] & 255;
            tag[i] = t;
            p++;
            if (t == 1) {
               int len = readU2(b, p);
               p += 2;
               if (len < 0 || p + len > b.length) {
                  return List.of();
               }

               text[i] = new String(b, p, len, StandardCharsets.UTF_8);
               p += len;
            } else if (t == 7 || t == 8 || t == 16 || t == 19 || t == 20) {
               first[i] = readU2(b, p);
               p += 2;
            } else if (t == 15) {
               p += 3;
            } else if (t != 3 && t != 4 && t != 9 && t != 10 && t != 11 && t != 12 && t != 17 && t != 18) {
               if (t != 5 && t != 6) {
                  return List.of();
               }

               p += 8;
               i++;
            } else {
               first[i] = readU2(b, p);
               second[i] = readU2(b, p + 2);
               p += 4;
            }
         }

         for (int i = 1; i < count; i++) {
            if (tag[i] == 10 || tag[i] == 11) {
               int cls = first[i];
               int nt = second[i];
               if (cls > 0 && cls < count && nt > 0 && nt < count && tag[cls] == 7 && tag[nt] == 12) {
                  String desc = text[second[nt]];
                  if (desc != null && desc.length() >= 4 && desc.startsWith("(L") && desc.endsWith(")F")) {
                     String inner = desc.substring(2, desc.length() - 2);
                     if (inner.endsWith(";") && inner.indexOf(59) == inner.length() - 1) {
                        String name = text[first[cls]];
                        if (name != null) {
                           out.add(name);
                        }
                     }
                  }
               }
            }
         }

         return out;
      } else {
         return out;
      }
   }

   private static boolean pokeStaticMaps(Class<?> holder, LivingEntity target, UUID id, float value) {
      boolean wrote = pokeOneClass(holder, target, id, value);

      for (Class<?> nested : holder.getDeclaredClasses()) {
         wrote |= pokeOneClass(nested, target, id, value);
      }

      return wrote;
   }

   private static boolean pokeOneClass(Class<?> holder, LivingEntity target, UUID id, float value) {
      boolean wrote = false;

      for (Field f : holder.getDeclaredFields()) {
         if (Modifier.isStatic(f.getModifiers()) && Map.class.isAssignableFrom(f.getType())) {
            try {
               f.setAccessible(true);
               if (f.get(null) instanceof Map<?, ?> map) {
                  wrote |= poke(map, target, id, value);
               }
            } catch (Throwable var11) {
            }
         }
      }

      return wrote;
   }

   private static int readU2(byte[] b, int i) {
      return (b[i] & 0xFF) << 8 | b[i + 1] & 0xFF;
   }

   private static long readU4(byte[] b) {
      return (long)(b[0] & 0xFF) << 24 | (b[1] & 255) << 16 | (b[2] & 255) << 8 | b[3] & 0xFF;
   }

   private static boolean poke(Map<?, ?> map, LivingEntity target, UUID id, float value) {
      boolean wrote = false;
      int netId = target.getId();

      for (Entry<?, ?> entry : map.entrySet()) {
         Object key = entry.getKey();
         if ((key == target || id.equals(key) || key instanceof Integer n && n == netId) && entry.getValue() instanceof Number) {
            try {
               Entry raw = entry;
               raw.setValue(value);
               wrote = true;
            } catch (Throwable var10) {
            }
         }
      }

      return wrote;
   }

   private static URL sourceOf(Class<?> type) {
      try {
         CodeSource code = type.getProtectionDomain() != null ? type.getProtectionDomain().getCodeSource() : null;
         return code != null ? code.getLocation() : null;
      } catch (Throwable t) {
         return null;
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

   private static List<Class<?>> scan(URL jar, String prefix, ClassLoader loader) {
      List<Class<?>> found = new ArrayList<>();

      File file;
      try {
         file = new File(jar.toURI());
      } catch (Throwable t) {
         return found;
      }

      if (!file.isFile()) {
         return found;
      }

      try (JarFile archive = new JarFile(file)) {
         int seen = 0;
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
                     if (holdsLedger(c)) {
                        found.add(c);
                     }
                  } catch (Throwable var13) {
                  }
               }
            }
         }
      } catch (Throwable t) {
         ShenlinMod.LOGGER.warn("[shenlin] ledger scan failed: {}", t.toString());
      }

      return found;
   }

   private static boolean holdsLedger(Class<?> type) {
      for (Field f : type.getDeclaredFields()) {
         if (Modifier.isStatic(f.getModifiers()) && Map.class.isAssignableFrom(f.getType())) {
            return true;
         }
      }

      return false;
   }

   private static boolean same(float a, float b) {
      return a == b || Math.abs(a - b) <= 0.01F;
   }
}

