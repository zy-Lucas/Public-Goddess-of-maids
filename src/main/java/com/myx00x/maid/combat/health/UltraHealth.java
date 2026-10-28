package com.myx00x.maid.combat.health;

import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.combat.Executioner;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public final class UltraHealth {
   public static final int LEVEL_SHALLOW = 1;
   public static final int LEVEL_DEEP = 2;
   private static final float TOLERANCE = 0.01F;
   private static EntityDataAccessor<Float> healthAccessor;
   private static boolean healthResolved;
   private static final Map<Class<?>, Integer> HINT = new ConcurrentHashMap<>();

   private UltraHealth() {
   }

   public static boolean setHealth(LivingEntity entity, float hp, int level) {
      if (entity != null && !entity.level().isClientSide) {
         boolean lethal = hp <= 0.0F && !Float.isFinite(hp);
         float target = lethal ? hp : Math.max(hp, 0.0F);
         if (Float.isNaN(target)) {
            return false;
         }

         writeSynced(entity, target);
         if (check(entity, target)) {
            return true;
         }

         float stageTarget = Float.isFinite(target) ? target : 0.0F;
         if (ASMHealthMethodHelper.analyzeAndSetHealth(entity, stageTarget) && check(entity, stageTarget)) {
            return true;
         }

         if (EncodedHealthWriter.setHealth(entity, stageTarget) && check(entity, stageTarget)) {
            return true;
         }

         if (level < 2) {
            return false;
         }

         int hint = HINT.getOrDefault(entity.getClass(), 3);
         boolean ok = false;

         for (int round = 0; round < 3 && !ok; round++) {
            int stage = 3 + (hint - 3 + round) % 3;

            ok = switch (stage) {
               case 3 -> HealthMethodHandleHelper.trySetHealthViaMethodHandle(entity, stageTarget) && check(entity, stageTarget);
               case 4 -> {
                  LifeValueContainerScanner.forceSetHealthDirect(entity, stageTarget);
                  yield check(entity, stageTarget);
               }
               default -> byDerivedField(entity, stageTarget) && check(entity, stageTarget);
            };
            if (ok) {
               HINT.put(entity.getClass(), stage);
            }
         }

         return ok;
      } else {
         return false;
      }
   }

   public static boolean check(LivingEntity entity, float target) {
      float now = entity.getHealth();
      return now == target ? true : Math.abs(now - target) <= 0.01F;
   }

   public static boolean hurtThrough(LivingEntity entity, LivingEntity attacker, float amount) {
      if (entity == null || attacker == null || amount <= 0.0F) {
         return false;
      } else if (!entity.level().isClientSide && entity.isAlive()) {
         DamageSource source = lethalSource(entity, attacker);
         float before = entity.getHealth();
         NoInvincibilityFramesHurt.hurt(entity, source, amount);
         return entity.getHealth() < before - 0.01F;
      } else {
         return false;
      }
   }

   private static DamageSource lethalSource(LivingEntity entity, LivingEntity attacker) {
      return entity.damageSources().mobAttack(attacker);
   }

   private static boolean writeSynced(LivingEntity entity, float value) {
      return Executioner.writeSyncedRaw(entity, value);
   }

   private static boolean byDerivedField(LivingEntity entity, float target) {
      float max = entity.getMaxHealth();
      float shown = entity.getHealth();
      if (max <= 0.0F || shown <= 0.0F) {
         return false;
      }

      if (!Float.isFinite(target)) {
         return false;
      }

      float taken = max - shown;
      if (taken <= 0.01F) {
         return false;
      }

      float want = Math.max(0.0F, max - target);
      Object holder = null;
      Field slot = null;
      int hits = 0;

      for (Class<?> c = entity.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
         for (Field f : c.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) && !Modifier.isFinal(f.getModifiers())) {
               Class<?> type = f.getType();
               if (type == float.class || type == double.class) {
                  try {
                     f.setAccessible(true);
                     double now = type == float.class ? f.getFloat(entity) : f.getDouble(entity);
                     if (!(Math.abs(now - taken) > 0.01F)) {
                        holder = entity;
                        slot = f;
                        hits++;
                     }
                  } catch (Throwable var18) {
                  }
               }
            }
         }
      }

      if (hits == 1 && slot != null) {
         try {
            if (slot.getType() == float.class) {
               slot.setFloat(holder, want);
            } else {
               slot.setDouble(holder, want);
            }

            return true;
         } catch (Throwable t) {
            ShenlinMod.LOGGER.warn("[shenlin] derived health write failed on {}: {}", entity.getType(), t.toString());
            return false;
         }
      } else {
         return false;
      }
   }

   private static EntityDataAccessor<Float> healthAccessor() {
      if (healthResolved) {
         return healthAccessor;
      }

      healthResolved = true;

      for (Class<?> c = LivingEntity.class; c != null && c != Object.class; c = c.getSuperclass()) {
         for (Field f : c.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) && EntityDataAccessor.class.isAssignableFrom(f.getType())) {
               String key = f.getName().toUpperCase().replace("_", "");
               if (key.contains("HEALTH") && !key.contains("MAXHEALTH")) {
                  try {
                     f.setAccessible(true);
                     healthAccessor = (EntityDataAccessor<Float>)f.get(null);
                  } catch (Throwable var7) {
                  }

                  if (healthAccessor != null) {
                     return healthAccessor;
                  }
               }
            }
         }
      }

      return healthAccessor;
   }
}

