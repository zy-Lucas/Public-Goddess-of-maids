package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.ShenlinMod;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.entity.EntityTickList;

public final class MaidRecovery {
   private static boolean tickResolved = false;
   private static Field tickListField;
   private static Method tickAdd;
   private static final Map<UUID, Long> LAST_LOG = new ConcurrentHashMap<>();

   private MaidRecovery() {
   }

   public static void sanitize(EntityMaid maid) {
      if (maid.getHealth() > 0.0F && !maid.isDeadOrDying()) {
         unlock(maid);
      } else {
         int effects = GuardClean.harmful(maid);
         boolean fe = GuardClean.lingering(maid);
         AttributeInstance max = maid.getAttribute(Attributes.MAX_HEALTH);
         if (max != null && (max.getBaseValue() <= 0.0 || maid.getMaxHealth() <= 0.0F)) {
            max.setBaseValue(max.getAttribute().getDefaultValue());
            if (maid.getMaxHealth() <= 0.0F) {
               for (AttributeModifier one : new ArrayList(max.getModifiers())) {
                  if (one.getAmount() < 0.0) {
                     max.removeModifier(one);
                  }
               }
            }
         }

         Executioner.putSkin(maid, Math.max(maid.getMaxHealth(), 1.0F));
         maid.deathTime = 0;
         maid.hurtTime = 0;
         maid.fallDistance = 0.0F;
         if ((effects > 0 || fe) && shouldLog(maid)) {
            ShenlinMod.LOGGER.info("[shenlin] maid {} cleansed effects={} fe={}", new Object[]{maid.getUUID(), effects, fe});
         }
      }
   }

   public static void unfreeze(EntityMaid maid) {
      boolean touched = false;
      if (maid.isInvisible()) {
         maid.setInvisible(false);
         touched = true;
      }

      if (maid.deathTime != 0) {
         maid.deathTime = 0;
         touched = true;
      }

      if (maid.hurtTime != 0) {
         maid.hurtTime = 0;
         touched = true;
      }

      if (backIntoTickList(maid)) {
         touched = true;
      }

      if (touched && shouldLog(maid)) {
         ShenlinMod.LOGGER.info("[shenlin] maid {} shaken out of a freeze", maid.getUUID());
      }
   }

   private static boolean backIntoTickList(EntityMaid maid) {
      if (maid.level() instanceof ServerLevel level) {
         resolveTickList();
         if (tickListField != null && tickAdd != null) {
            try {
               EntityTickList list = (EntityTickList)tickListField.get(level);
               if (list != null && !list.contains(maid)) {
                  tickAdd.invoke(list, maid);
                  return true;
               } else {
                  return false;
               }
            } catch (Throwable t) {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static void resolveTickList() {
      if (!tickResolved) {
         tickResolved = true;
         tickListField = findField(ServerLevel.class, "f_143243_", "entityTickList");
         tickAdd = findMethod(EntityTickList.class, "m_156908_", "add", Entity.class);
      }
   }

   private static Field findField(Class<?> owner, String... names) {
      for (String name : names) {
         try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
         } catch (Throwable var7) {
         }
      }

      return null;
   }

   private static Method findMethod(Class<?> owner, String srg, String official, Class<?>... args) {
      for (String name : new String[]{srg, official}) {
         try {
            Method method = owner.getDeclaredMethod(name, args);
            method.setAccessible(true);
            return method;
         } catch (Throwable var9) {
         }
      }

      return null;
   }

   public static void unlock(EntityMaid maid) {
      if (maid.isNoAi()) {
         maid.setNoAi(false);
      }

      Brain<?> brain = maid.getBrain();
      if (brain != null && !brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
         brain.eraseMemory(MemoryModuleType.WALK_TARGET);
         brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(maid.position(), 0.7F, 0));
      }
   }

   public static boolean clearLingering(EntityMaid maid) {
      return GuardClean.lingering(maid);
   }

   private static boolean shouldLog(EntityMaid maid) {
      long now = maid.level().getGameTime();
      Long last = LAST_LOG.get(maid.getUUID());
      if (last != null && now - last < 100L) {
         return false;
      }

      if (LAST_LOG.size() > 64) {
         LAST_LOG.entrySet().removeIf(e -> now - e.getValue() > 1200L);
      }

      LAST_LOG.put(maid.getUUID(), now);
      return true;
   }
}

