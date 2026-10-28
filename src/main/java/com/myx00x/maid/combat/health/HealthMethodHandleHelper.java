package com.myx00x.maid.combat.health;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.LivingEntity;
import sun.misc.Unsafe;

public final class HealthMethodHandleHelper {
   private static final Unsafe UNSAFE;
   private static final Lookup IMPL_LOOKUP;
   private static final Map<Class<?>, List<MethodHandle>> HEALTH_SET_MH_CACHE;

   public static List<MethodHandle> findSetHealthMethodHandles(LivingEntity entity) {
      Class<?> clazz = entity.getClass();
      return HEALTH_SET_MH_CACHE.computeIfAbsent(clazz, HealthMethodHandleHelper::scanSetHealthMethodHandles);
   }

   private static List<MethodHandle> scanSetHealthMethodHandles(Class<?> clazz) {
      HashSet<String> names = new HashSet<>();
      names.add("setHealth");
      names.add("setHealth");
      ArrayList<Method> candidates = new ArrayList<>();

      for (Class<?> current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
         for (Method method : current.getDeclaredMethods()) {
            if (isHealthSetter(method) && names.contains(method.getName())) {
               candidates.add(method);
            }
         }
      }

      if (candidates.isEmpty()) {
         for (Class<?> var10 = clazz; var10 != null && var10 != Object.class; var10 = var10.getSuperclass()) {
            for (Method method : var10.getDeclaredMethods()) {
               String name;
               if (isHealthSetter(method) && ((name = method.getName().toLowerCase(Locale.ROOT)).contains("health") || name.contains("hp"))) {
                  candidates.add(method);
               }
            }
         }
      }

      ArrayList<MethodHandle> result = new ArrayList<>();

      for (Method method : candidates) {
         try {
            result.add(IMPL_LOOKUP.unreflect(method));
         } catch (IllegalAccessException var9) {
         }
      }

      return result;
   }

   public static boolean trySetHealthViaMethodHandle(LivingEntity entity, float health) {
      if (entity != null && Float.isFinite(health)) {
         float before = safeGetHealth(entity);
         if (Float.isNaN(before)) {
            return false;
         }

         if (tryDirectSetHealth(entity, health, before) && exactReached(entity, health)) {
            return true;
         }

         boolean moved = false;

         for (MethodHandle mh : findSetHealthMethodHandles(entity)) {
            Object[] args = buildInvokeArgs(entity, health, mh.type());
            if (args != null) {
               try {
                  float stepBefore = safeGetHealth(entity);
                  mh.invokeWithArguments(args);
                  if (exactReached(entity, health)) {
                     return true;
                  }

                  if (movedToward(entity, stepBefore, health)) {
                     moved = true;
                  }
               } catch (Throwable var9) {
               }
            }
         }

         try {
            if (ASMHealthMethodHelper.analyzeAndSetHealth(entity, health) && exactReached(entity, health)) {
               return true;
            }
         } catch (Throwable var8) {
         }

         return moved;
      } else {
         return false;
      }
   }

   private static boolean isHealthSetter(Method method) {
      int modifiers = method.getModifiers();
      if (!Modifier.isStatic(modifiers) && !Modifier.isAbstract(modifiers)) {
         Class<?>[] parameters = method.getParameterTypes();
         if (parameters.length == 1 && parameters[0] == float.class) {
            Class<?> returnType = method.getReturnType();
            return returnType == void.class || returnType == float.class || returnType == Float.class;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean tryDirectSetHealth(LivingEntity entity, float health, float before) {
      try {
         entity.setHealth(health);
         return exactReached(entity, health) || movedToward(entity, before, health);
      } catch (Throwable ignored) {
         return false;
      }
   }

   private static float safeGetHealth(LivingEntity entity) {
      try {
         return entity.getHealth();
      } catch (Throwable ignored) {
         return Float.NaN;
      }
   }

   private static boolean exactReached(LivingEntity entity, float target) {
      float after = safeGetHealth(entity);
      return !Float.isNaN(after) && Math.abs(after - target) <= 0.001F;
   }

   private static boolean movedToward(LivingEntity entity, float before, float target) {
      float after = safeGetHealth(entity);
      if (Float.isNaN(after)) {
         return false;
      } else {
         return target < before ? after < before - 0.001F : after > before + 0.001F;
      }
   }

   private static Object[] buildInvokeArgs(LivingEntity entity, float health, MethodType methodType) {
      Class<?>[] paramTypes = methodType.parameterArray();
      Object[] args = new Object[paramTypes.length];
      boolean entityFilled = false;
      boolean healthFilled = false;

      for (int i = 0; i < paramTypes.length; i++) {
         Class<?> type = paramTypes[i];
         if (!entityFilled && type.isAssignableFrom(entity.getClass())) {
            args[i] = entity;
            entityFilled = true;
         } else {
            if (healthFilled || !isNumericType(type)) {
               return null;
            }

            args[i] = convertHealthValue(type, health);
            healthFilled = true;
         }
      }

      return entityFilled && healthFilled ? args : null;
   }

   private static boolean isNumericType(Class<?> type) {
      return type == float.class
         || type == Float.class
         || type == double.class
         || type == Double.class
         || type == int.class
         || type == Integer.class
         || type == long.class
         || type == Long.class;
   }

   private static Object convertHealthValue(Class<?> type, float health) {
      if (type == float.class || type == Float.class) {
         return health;
      } else if (type == double.class || type == Double.class) {
         return (double)health;
      } else if (type == int.class || type == Integer.class) {
         return (int)health;
      } else {
         return type != long.class && type != Long.class ? health : (long)health;
      }
   }

   static {
      try {
         Field f = Unsafe.class.getDeclaredField("theUnsafe");
         f.setAccessible(true);
         UNSAFE = (Unsafe)f.get(null);
      } catch (Exception e) {
         throw new RuntimeException("Can't get Unsafe instance", e);
      }

      try {
         Field implLookupField = Lookup.class.getDeclaredField("IMPL_LOOKUP");
         Object base = UNSAFE.staticFieldBase(implLookupField);
         long offset = UNSAFE.staticFieldOffset(implLookupField);
         IMPL_LOOKUP = (Lookup)UNSAFE.getObject(base, offset);
      } catch (Exception e) {
         throw new RuntimeException("Failed to get IMPL_LOOKUP via Unsafe", e);
      }

      HEALTH_SET_MH_CACHE = new ConcurrentHashMap<>();
   }
}

