package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.ShenlinMod;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.warden.Warden;

public final class TurnTarget {
   private static final int AGRO_ANGER = 150;
   private static final Set<String> TARGET_NAMES = Set.of("target", "attackTarget", "angerTarget");
   private static final Map<Class<?>, Optional<Field>> FIELDS = new ConcurrentHashMap<>();

   private TurnTarget() {
   }

   public static boolean turn(LivingEntity enemy, EntityMaid maid, LivingEntity owner) {
      if (enemy instanceof Mob mob && !(mob instanceof EntityMaid) && !enemy.isRemoved() && enemy.isAlive()) {
         if (enemy instanceof Warden warden) {
            appease(warden, maid, owner);
         }

         if (mob.getTarget() != maid) {
            mob.setTarget(maid);
         }

         writeBrain(mob, maid);
         return mob.getTarget() != maid && !brainAims(mob, maid) ? forceInto(enemy, maid) : true;
      } else {
         return false;
      }
   }

   private static void appease(Warden warden, EntityMaid maid, LivingEntity owner) {
      try {
         if (owner != null && owner != maid) {
            warden.clearAnger(owner);
         }

         if (warden.getTarget() != maid) {
            warden.increaseAngerAt(maid, 150, true);
         }
      } catch (Throwable t) {
         ShenlinMod.LOGGER.debug("[shenlin] warden appease failed", t);
      }
   }

   private static void writeBrain(Mob mob, LivingEntity target) {
      try {
         Brain brain = mob.getBrain();
         if (brain != null && brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
            brain.setMemory(MemoryModuleType.ATTACK_TARGET, target);
            brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
         }
      } catch (Throwable var3) {
      }
   }

   private static boolean brainAims(Mob mob, EntityMaid maid) {
      try {
         Brain brain = mob.getBrain();
         return brain != null && brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) == maid;
      } catch (Throwable t) {
         return false;
      }
   }

   private static boolean forceInto(LivingEntity enemy, EntityMaid maid) {
      Field field = FIELDS.computeIfAbsent(enemy.getClass(), TurnTarget::findTargetField).orElse(null);
      if (field == null) {
         return false;
      }

      try {
         field.set(enemy, maid);
         return field.get(enemy) == maid;
      } catch (Throwable t) {
         return false;
      }
   }

   private static Optional<Field> findTargetField(Class<?> type) {
      for (Class<?> c = type; c != null && c != Mob.class && c != Object.class; c = c.getSuperclass()) {
         for (Field f : c.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) && TARGET_NAMES.contains(f.getName()) && LivingEntity.class.isAssignableFrom(f.getType())) {
               try {
                  f.setAccessible(true);
                  return Optional.of(f);
               } catch (Throwable ignored) {
                  return Optional.empty();
               }
            }
         }
      }

      return Optional.empty();
   }
}

