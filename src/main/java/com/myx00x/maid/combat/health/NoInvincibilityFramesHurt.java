package com.myx00x.maid.combat.health;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class NoInvincibilityFramesHurt {
   private static final Map<Class<?>, List<Field>> FIELD_CACHE = new HashMap<>();
   private static final int MAX_DEPTH = 4;
   private static final Set<String> DATAACCESSOR_BLACKLIST = new HashSet<>(
      Arrays.asList(
         "phase",
         "stage",
         "state",
         "ai",
         "goal",
         "target",
         "brain",
         "memory",
         "sensor",
         "skill",
         "ability",
         "spell",
         "cast",
         "animation",
         "swing",
         "health",
         "hp",
         "total"
      )
   );
   private static final Set<String> DATAACCESSOR_WHITELIST_FIRST = new HashSet<>(Arrays.asList("frame", "inv", "hurt", "hit"));
   private static final Set<String> DATAACCESSOR_WHITELIST_LAST = new HashSet<>(Arrays.asList("frame", "tick", "time", "cooldown", "second", "inv"));
   private static final Set<String> COLLECT_FIELD_WHITELIST_LEFT = new HashSet<>(Arrays.asList("last", "damage", "hit"));
   private static final Set<String> COLLECT_FIELD_WHITELIST_RIGHT = new HashSet<>(Arrays.asList("frame", "tick", "hurt"));

   public static boolean hurt(LivingEntity entity, DamageSource source, float amount) {
      if (entity != null && !entity.level().isClientSide) {
         LinkedHashMap<String, int[]> beforeSnapshot = new LinkedHashMap<>();
         collectHurtTimers(entity, new HashSet<>(), beforeSnapshot, 0);
         HashSet<String> zeroSnapshot = new HashSet<>();
         collectZeroTimers(entity, new HashSet<>(), zeroSnapshot, 0);
         zapTimers(entity);
         boolean hurt = entity.hurt(source, amount);

         for (Entry entry : beforeSnapshot.entrySet()) {
            String fieldPath = (String)entry.getKey();
            int[] oldValue = (int[])entry.getValue();
            restoreFieldByPath(entity, fieldPath, oldValue[0]);
         }

         rollbackZeroTimers(entity, zeroSnapshot);
         return hurt;
      } else {
         return false;
      }
   }

   public static void trySetHealth(LivingEntity entity, float amount) {
      if (entity != null && !entity.level().isClientSide) {
         LinkedHashMap<String, int[]> beforeSnapshot = new LinkedHashMap<>();
         collectHurtTimers(entity, new HashSet<>(), beforeSnapshot, 0);
         HashSet<String> zeroSnapshot = new HashSet<>();
         collectZeroTimers(entity, new HashSet<>(), zeroSnapshot, 0);
         zapTimers(entity);
         entity.setHealth(amount);

         for (Entry entry : beforeSnapshot.entrySet()) {
            String fieldPath = (String)entry.getKey();
            int[] oldValue = (int[])entry.getValue();
            restoreFieldByPath(entity, fieldPath, oldValue[0]);
         }

         rollbackZeroTimers(entity, zeroSnapshot);
      }
   }

   private static void collectHurtTimers(Object root, Set<Object> visited, Map<String, int[]> results, int depth) {
      if (root != null && depth <= 4 && visited.add(root)) {
         for (Field f : getAllFields(root.getClass())) {
            try {
               int mod = f.getModifiers();
               if (!Modifier.isStatic(mod) && !Modifier.isTransient(mod)) {
                  f.setAccessible(true);
                  if (f.getType() == int.class) {
                     String lower = f.getName().toLowerCase(Locale.ROOT);
                     boolean matchLastTick = COLLECT_FIELD_WHITELIST_LEFT.stream().anyMatch(lower::contains);
                     boolean matchDamageHurt = COLLECT_FIELD_WHITELIST_RIGHT.stream().anyMatch(lower::contains);
                     if (matchLastTick && matchDamageHurt && DATAACCESSOR_BLACKLIST.stream().noneMatch(lower::contains)) {
                        int val = f.getInt(root);
                        int hash = System.identityHashCode(root);
                        results.put(hash + "." + f.getName(), new int[]{val, hash});
                     }
                  } else {
                     Object child;
                     if (!f.getType().isPrimitive()
                        && !f.getType().getName().startsWith("java.")
                        && !f.getType().getName().startsWith("net.minecraft")
                        && !f.getType().isArray()
                        && !Collection.class.isAssignableFrom(f.getType())
                        && !Map.class.isAssignableFrom(f.getType())
                        && (child = f.get(root)) != null) {
                        collectHurtTimers(child, visited, results, depth + 1);
                     }
                  }
               }
            } catch (Exception var13) {
            }
         }
      }
   }

   private static void restoreFieldByPath(LivingEntity entity, String fieldPath, int oldValue) {
      String[] parts = fieldPath.split("\\.", 2);
      if (parts.length == 2) {
         int targetHash = Integer.parseInt(parts[0]);
         String fieldName = parts[1];
         HashSet<Object> visited = new HashSet<>();
         findAndRestore(entity, fieldName, oldValue, targetHash, visited, 0);
      }
   }

   private static void findAndRestore(Object root, String fieldName, int oldValue, int targetHash, Set<Object> visited, int depth) {
      if (root != null && depth <= 4 && visited.add(root)) {
         if (System.identityHashCode(root) == targetHash) {
            for (Field f : getAllFields(root.getClass())) {
               if (f.getName().equals(fieldName) && f.getType() == int.class) {
                  try {
                     f.setAccessible(true);
                     f.setInt(root, oldValue);
                  } catch (Exception var10) {
                  }

                  return;
               }
            }
         }

         for (Field f : getAllFields(root.getClass())) {
            try {
               int mod = f.getModifiers();
               if (!Modifier.isStatic(mod) && !Modifier.isTransient(mod)) {
                  f.setAccessible(true);
                  Object child;
                  if (!f.getType().isPrimitive()
                     && !f.getType().getName().startsWith("java.")
                     && !f.getType().getName().startsWith("net.minecraft")
                     && !f.getType().isArray()
                     && !Collection.class.isAssignableFrom(f.getType())
                     && !Map.class.isAssignableFrom(f.getType())
                     && (child = f.get(root)) != null) {
                     findAndRestore(child, fieldName, oldValue, targetHash, visited, depth + 1);
                  }
               }
            } catch (Exception var11) {
            }
         }
      }
   }

   private static boolean isBlacklisted(String name) {
      for (String black : DATAACCESSOR_BLACKLIST) {
         if (name.contains(black)) {
            return true;
         }
      }

      return false;
   }

   private static boolean isWhitelisted(String name) {
      boolean matchFirst = DATAACCESSOR_WHITELIST_FIRST.isEmpty();

      for (String white : DATAACCESSOR_WHITELIST_FIRST) {
         if (name.contains(white)) {
            matchFirst = true;
            break;
         }
      }

      boolean matchLast = DATAACCESSOR_WHITELIST_LAST.isEmpty();

      for (String white : DATAACCESSOR_WHITELIST_LAST) {
         if (name.contains(white)) {
            matchLast = true;
            break;
         }
      }

      return matchFirst && matchLast;
   }

   private static void zapTimers(LivingEntity entity) {
      entity.invulnerableTime = 0;
      entity.hurtTime = 0;
      entity.hurtDuration = 0;

      for (Field f : getAllFields(entity.getClass())) {
         try {
            if (!Modifier.isStatic(f.getModifiers()) && !Modifier.isTransient(f.getModifiers())) {
               f.setAccessible(true);
               Class<?> type = f.getType();
               Number val;
               if ((type == int.class || type == float.class)
                  && (val = (Number)f.get(entity)) != null
                  && !(val.floatValue() <= 0.0F)
                  && isWhitelisted(f.getName().toLowerCase(Locale.ROOT))) {
                  setZero(f, entity);
               }
            }
         } catch (Exception var10) {
         }
      }

      SynchedEntityData data = entity.getEntityData();

      for (Class<?> c = entity.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
         for (Field f : c.getDeclaredFields()) {
            try {
               if (Modifier.isStatic(f.getModifiers()) && EntityDataAccessor.class.isAssignableFrom(f.getType())) {
                  f.setAccessible(true);
                  EntityDataAccessor acc = (EntityDataAccessor)f.get(null);
                  String name;
                  if (acc.getSerializer() == EntityDataSerializers.INT
                     && isWhitelisted(name = f.getName().toLowerCase(Locale.ROOT))
                     && !isBlacklisted(name)
                     && (Integer)data.get(acc) > 0) {
                     data.set(acc, 0);
                  }
               }
            } catch (Exception var9) {
            }
         }
      }
   }

   private static void setZero(Field f, Object t) throws Exception {
      Class<?> type = f.getType();
      if (type == int.class || type == Integer.class) {
         f.setInt(t, 0);
      } else if (type == float.class || type == Float.class) {
         f.setFloat(t, 0.0F);
      } else if (type == double.class || type == Double.class) {
         f.setDouble(t, 0.0);
      } else if (type == long.class || type == Long.class) {
         f.setLong(t, 0L);
      } else if (type == short.class || type == Short.class) {
         f.setShort(t, (short)0);
      } else if (type != byte.class && type != Byte.class) {
         f.set(t, 0);
      } else {
         f.setByte(t, (byte)0);
      }
   }

   private static List<Field> getAllFields(Class<?> clazz) {
      return FIELD_CACHE.computeIfAbsent(clazz, k -> {
         ArrayList<Field> fields = new ArrayList<>();

         for (Class c = k; c != null && c != Object.class; c = c.getSuperclass()) {
            fields.addAll(Arrays.asList(c.getDeclaredFields()));
         }

         return Collections.unmodifiableList(fields);
      });
   }

   private static void collectZeroTimers(Object root, Set<Object> visited, Set<String> zeroFields, int depth) {
      if (root != null && depth <= 4 && visited.add(root)) {
         for (Field f : getAllFields(root.getClass())) {
            try {
               int mod = f.getModifiers();
               if (!Modifier.isStatic(mod) && !Modifier.isTransient(mod)) {
                  f.setAccessible(true);
                  if (f.getType() == int.class) {
                     String lower = f.getName().toLowerCase(Locale.ROOT);
                     boolean matchLeft = COLLECT_FIELD_WHITELIST_LEFT.stream().anyMatch(lower::contains);
                     boolean matchRight = COLLECT_FIELD_WHITELIST_RIGHT.stream().anyMatch(lower::contains);
                     boolean matchBlack = DATAACCESSOR_BLACKLIST.stream().anyMatch(lower::contains);
                     if (matchLeft && matchRight && !matchBlack && f.getInt(root) == 0) {
                        int hash = System.identityHashCode(root);
                        zeroFields.add(hash + "." + f.getName());
                     }
                  } else {
                     Object child;
                     if (!f.getType().isPrimitive()
                        && !f.getType().getName().startsWith("java.")
                        && !f.getType().getName().startsWith("net.minecraft")
                        && !f.getType().isArray()
                        && !Collection.class.isAssignableFrom(f.getType())
                        && !Map.class.isAssignableFrom(f.getType())
                        && (child = f.get(root)) != null) {
                        collectZeroTimers(child, visited, zeroFields, depth + 1);
                     }
                  }
               }
            } catch (Exception var14) {
            }
         }
      }
   }

   private static void rollbackZeroTimers(LivingEntity entity, Set<String> zeroFieldKeys) {
      for (String key : zeroFieldKeys) {
         String[] parts = key.split("\\.", 2);
         if (parts.length == 2) {
            int targetHash = Integer.parseInt(parts[0]);
            String fieldName = parts[1];
            HashSet<Object> visited = new HashSet<>();
            resetIfIncreased(entity, fieldName, targetHash, visited, 0);
         }
      }
   }

   private static void resetIfIncreased(Object root, String fieldName, int targetHash, Set<Object> visited, int depth) {
      if (root != null && depth <= 4 && visited.add(root)) {
         if (System.identityHashCode(root) == targetHash) {
            for (Field f : getAllFields(root.getClass())) {
               if (f.getName().equals(fieldName) && f.getType() == int.class) {
                  try {
                     f.setAccessible(true);
                     int current = f.getInt(root);
                     if (current > 0) {
                        f.setInt(root, 0);
                     }
                  } catch (Exception var9) {
                  }

                  return;
               }
            }
         }

         for (Field f : getAllFields(root.getClass())) {
            try {
               int mod = f.getModifiers();
               if (!Modifier.isStatic(mod) && !Modifier.isTransient(mod)) {
                  f.setAccessible(true);
                  Object child;
                  if (!f.getType().isPrimitive()
                     && !f.getType().getName().startsWith("java.")
                     && !f.getType().getName().startsWith("net.minecraft")
                     && !f.getType().isArray()
                     && !Collection.class.isAssignableFrom(f.getType())
                     && !Map.class.isAssignableFrom(f.getType())
                     && (child = f.get(root)) != null) {
                     resetIfIncreased(child, fieldName, targetHash, visited, depth + 1);
                  }
               }
            } catch (Exception var10) {
            }
         }
      }
   }

   private NoInvincibilityFramesHurt() {
   }
}

