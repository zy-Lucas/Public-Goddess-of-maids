package com.myx00x.maid.combat.health;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import sun.misc.Unsafe;

public final class LifeValueContainerScanner {
   private static final Set<String> PACKAGE_BLACKLIST = new HashSet<>(
      Arrays.asList("java.", "it.unimi.dsi.", "gnu.trove.", "org.apache.commons.collections", "com.google.common.collect", "jdk.", "net.minecraft.")
   );
   private static final List<String> SETTER_KEYWORDS = Arrays.asList("health", "progress", "life", "hp", "vital", "amount", "current", "value");
   private static final Map<Class<?>, List<Field>> FIELD_CACHE = new HashMap<>();
   private static final Map<Class<?>, List<Method>> SETTER_CACHE = new HashMap<>();

   public static void forceSetHealthDirect(LivingEntity entity, float newHealth) {
      if (entity != null) {
         if (Float.isFinite(newHealth)) {
            HashSet<Object> visited = new HashSet<>();
            visited.add(entity);
            List<Object> reachable = collectReachableObjects(entity, visited);

            for (Object obj : reachable) {
               callLifeSetters(obj, entity, newHealth);
            }

            for (Object obj : reachable) {
               modifyMapsForEntity(obj, entity, newHealth);
            }

            callLifeSetters(entity, entity, newHealth);
            modifyMapsForEntity(entity, entity, newHealth);
         }
      }
   }

   private static void callLifeSetters(Object obj, LivingEntity entity, float value) {
      for (Method m : getLifeSetters(obj.getClass())) {
         try {
            m.setAccessible(true);
            if (m.getParameterCount() == 1) {
               invokeNumeric(m, obj, value);
            } else {
               Class<?> p0 = m.getParameterTypes()[0];
               Class<?> p1 = m.getParameterTypes()[1];
               if (LivingEntity.class.isAssignableFrom(p0)) {
                  m.invoke(obj, entity, coerce(value, p1));
               } else if (LivingEntity.class.isAssignableFrom(p1)) {
                  m.invoke(obj, coerce(value, p0), entity);
               }
            }
         } catch (Exception var7) {
         }
      }
   }

   private static List<Method> getLifeSetters(Class<?> cl) {
      return SETTER_CACHE.computeIfAbsent(
         cl,
         k -> {
            ArrayList<Method> result = new ArrayList<>();

            for (Method m2 : k.getDeclaredMethods()) {
               try {
                  int pc;
                  if (!Modifier.isStatic(m2.getModifiers()) && (pc = m2.getParameterCount()) >= 1 && pc <= 2) {
                     if (pc == 1) {
                        if (!isNumeric(m2.getParameterTypes()[0])) {
                           continue;
                        }
                     } else {
                        boolean hasEntity = false;
                        boolean hasNum = false;

                        for (Class<?> pt : m2.getParameterTypes()) {
                           if (LivingEntity.class.isAssignableFrom(pt)) {
                              hasEntity = true;
                           }

                           if (isNumeric(pt)) {
                              hasNum = true;
                           }
                        }

                        if (!hasEntity || !hasNum) {
                           continue;
                        }
                     }

                     String name;
                     if ((name = m2.getName().toLowerCase(Locale.ROOT)).equals("set") || containsAny(name, SETTER_KEYWORDS)) {
                        result.add(m2);
                     }
                  }
               } catch (Exception var14) {
               }
            }

            result.sort(
               Comparator.<Method>comparingInt(m -> m.getParameterCount())
                  .thenComparingInt(m -> isFloatLike(m.getParameterTypes()[m.getParameterCount() - 1]) ? 0 : 1)
            );
            return Collections.unmodifiableList(result);
         }
      );
   }

   private static void modifyMapsForEntity(Object obj, LivingEntity entity, float newValue) {
      for (Field f : getAllFields(obj.getClass())) {
         if (Map.class.isAssignableFrom(f.getType()) && !isPackageBlacklisted(f.getType().getName())) {
            boolean isStatic = Modifier.isStatic(f.getModifiers());

            try {
               f.setAccessible(true);
               Map map = (Map)(isStatic ? f.get(null) : f.get(obj));
               if (map != null && !map.isEmpty()) {
                  modifyMapForAllMatchingKeys(map, entity, newValue);
               }
            } catch (Exception var8) {
            }
         }
      }
   }

   private static int modifyMapForAllMatchingKeys(Map<Object, Object> map, LivingEntity entity, float newValue) {
      int written = 0;

      try {
         for (Entry<Object, Object> entry : map.entrySet()) {
            Entry<Object, Object> entry2 = entry;
            Object key = entry2.getKey();
            Object newVal;
            Object curVal;
            if (isEntityMatch(key, entity) && (curVal = entry2.getValue()) instanceof Number && (newVal = boxNumeric(curVal, newValue)) != null) {
               boolean ok = false;

               try {
                  entry2.setValue(newVal);
                  ok = Objects.equals(entry2.getValue(), newVal);
               } catch (Exception var15) {
               }

               if (!ok) {
                  try {
                     long offset = findEntryValueFieldOffset(entry2);
                     Unsafe unsafe;
                     if (offset != -1L && (unsafe = getUnsafe()) != null) {
                        unsafe.putObject(entry2, offset, newVal);
                        ok = Objects.equals(entry2.getValue(), newVal);
                     }
                  } catch (Exception var14) {
                  }
               }

               if (ok) {
                  written++;
               }
            }
         }
      } catch (Exception var16) {
      }

      return written;
   }

   private static boolean isEntityMatch(Object key, LivingEntity entity) {
      if (key == entity) {
         return true;
      } else if (key instanceof Integer && (Integer)key == entity.getId()) {
         return true;
      } else {
         return key instanceof UUID && key.equals(entity.getUUID()) ? true : entity.equals(key);
      }
   }

   @Nullable
   private static Object boxNumeric(Object reference, float newValue) {
      if (reference instanceof Double) {
         return (double)newValue;
      } else if (reference instanceof Integer) {
         return (int)newValue;
      } else if (reference instanceof Long) {
         return (long)newValue;
      } else if (reference instanceof Short) {
         return (short)newValue;
      } else if (reference instanceof Byte) {
         return (byte)newValue;
      } else {
         return reference instanceof Float ? newValue : null;
      }
   }

   private static long findEntryValueFieldOffset(Entry entry) {
      for (Class<?> cls = entry.getClass(); cls != Object.class; cls = cls.getSuperclass()) {
         for (Field f : cls.getDeclaredFields()) {
            if (f.getName().equals("value") || f.getName().equals("val")) {
               try {
                  Unsafe unsafe = getUnsafe();
                  if (unsafe != null) {
                     return unsafe.objectFieldOffset(f);
                  }
               } catch (Exception var8) {
               }
            }
         }
      }

      return -1L;
   }

   private static Unsafe getUnsafe() {
      try {
         Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
         theUnsafe.setAccessible(true);
         return (Unsafe)theUnsafe.get(null);
      } catch (Exception e) {
         return null;
      }
   }

   private static List<Object> collectReachableObjects(Object root, Set<Object> visited) {
      ArrayList<Object> result = new ArrayList<>();

      for (Field f : getAllFields(root.getClass())) {
         int mod = f.getModifiers();
         if (!Modifier.isStatic(mod) && !f.getType().isPrimitive() && !isForbiddenType(f.getType())) {
            try {
               f.setAccessible(true);
               Object val = f.get(root);
               if (val != null && visited.add(val) && !(val instanceof Map) && !(val instanceof Collection) && !isPackageBlacklisted(val.getClass().getName())) {
                  result.add(val);

                  for (Field f2 : getAllFields(val.getClass())) {
                     int mod2 = f2.getModifiers();
                     if (!Modifier.isStatic(mod2) && !f2.getType().isPrimitive() && !isForbiddenType(f2.getType())) {
                        try {
                           f2.setAccessible(true);
                           Object v2 = f2.get(val);
                           if (v2 != null
                              && visited.add(v2)
                              && !(v2 instanceof Map)
                              && !(v2 instanceof Collection)
                              && !isPackageBlacklisted(v2.getClass().getName())) {
                              result.add(v2);
                           }
                        } catch (Exception var11) {
                        }
                     }
                  }
               }
            } catch (Exception var12) {
            }
         }
      }

      return result;
   }

   private static boolean isForbiddenType(Class<?> type) {
      return !Collection.class.isAssignableFrom(type) && !Map.class.isAssignableFrom(type) ? isPackageBlacklisted(type.getName()) : true;
   }

   private static boolean isPackageBlacklisted(String className) {
      for (String prefix : PACKAGE_BLACKLIST) {
         if (className.startsWith(prefix)) {
            return true;
         }
      }

      return false;
   }

   private static List<Field> getAllFields(Class<?> cl) {
      return FIELD_CACHE.computeIfAbsent(cl, k -> {
         ArrayList<Field> fields = new ArrayList<>();

         for (Class c = k; c != null && c != Object.class; c = c.getSuperclass()) {
            fields.addAll(Arrays.asList(c.getDeclaredFields()));
         }

         return Collections.unmodifiableList(fields);
      });
   }

   private static boolean isNumeric(Class<?> t) {
      return t == int.class
         || t == long.class
         || t == float.class
         || t == double.class
         || t == short.class
         || t == byte.class
         || Number.class.isAssignableFrom(t);
   }

   private static boolean isFloatLike(Class<?> t) {
      return t == float.class || t == Float.class || t == double.class || t == Double.class;
   }

   private static boolean containsAny(String s, List<String> keywords) {
      for (String kw : keywords) {
         if (s.contains(kw)) {
            return true;
         }
      }

      return false;
   }

   private static Object coerce(float value, Class<?> targetType) {
      if (targetType == float.class || targetType == Float.class) {
         return value;
      } else if (targetType == double.class || targetType == Double.class) {
         return (double)value;
      } else if (targetType == int.class || targetType == Integer.class) {
         return (int)value;
      } else if (targetType == long.class || targetType == Long.class) {
         return (long)value;
      } else if (targetType == short.class || targetType == Short.class) {
         return (short)value;
      } else {
         return targetType != byte.class && targetType != Byte.class ? value : (byte)value;
      }
   }

   private static void invokeNumeric(Method m, Object target, float value) throws Exception {
      Class<?> pt = m.getParameterTypes()[0];
      if (pt == float.class || pt == Float.class) {
         m.invoke(target, value);
      } else if (pt == double.class || pt == Double.class) {
         m.invoke(target, value);
      } else if (pt == int.class || pt == Integer.class) {
         m.invoke(target, (int)value);
      } else if (pt != long.class && pt != Long.class) {
         m.invoke(target, value);
      } else {
         m.invoke(target, (long)value);
      }
   }

   private static String paramNames(Method m) {
      Class<?>[] pts = m.getParameterTypes();
      CharSequence[] names = new String[pts.length];

      for (int i = 0; i < pts.length; i++) {
         names[i] = pts[i].getSimpleName();
      }

      return String.join(", ", names);
   }
}

