package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class GuardVitals {
   private static final Map<UUID, Float> TRUE_HEALTH = new ConcurrentHashMap<>();
   private static final Map<UUID, Float> TRUE_MAX = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> ENROLLED_AT = new ConcurrentHashMap<>();
   private static final int CAPACITY = 4096;
   private static final float MAX_FLOOR = 2.0F;

   private GuardVitals() {
   }

   public static void enroll(EntityMaid maid) {
      UUID id = maid.getUUID();
      float max = maxOf(maid);
      TRUE_HEALTH.putIfAbsent(id, max);
      TRUE_MAX.putIfAbsent(id, max);
      ENROLLED_AT.putIfAbsent(id, maid.level() == null ? 0L : maid.level().getGameTime());
      trim();
   }

   public static float health(EntityMaid maid) {
      UUID id = maid.getUUID();
      Float value = TRUE_HEALTH.get(id);
      return value != null ? value : maxOf(maid);
   }

   public static float max(EntityMaid maid) {
      UUID id = maid.getUUID();
      Float value = TRUE_MAX.get(id);
      return value != null ? value : maxOf(maid);
   }

   public static boolean alive(EntityMaid maid) {
      return health(maid) > 0.0F;
   }

   public static float skinOf(EntityMaid maid) {
      float raw = Executioner.syncedHealth(maid);
      return raw > 0.0F ? raw : maid.getHealth();
   }

   public static boolean overCeiling(EntityMaid maid) {
      float ceiling = ceilingOf(maid);
      return ceiling > 0.0F && skinOf(maid) > ceiling + 0.01F;
   }

   public static float ceilingOf(EntityMaid maid) {
      return Math.max(max(maid), maxOf(maid));
   }

   public static void clear(EntityMaid maid) {
      UUID id = maid.getUUID();
      TRUE_MAX.putIfAbsent(id, maxOf(maid));
      TRUE_HEALTH.put(id, 0.0F);
      ENROLLED_AT.putIfAbsent(id, maid.level() == null ? 0L : maid.level().getGameTime());
   }

   public static void restore(EntityMaid maid) {
      UUID id = maid.getUUID();
      Float now = TRUE_HEALTH.get(id);
      if (now != null && now <= 0.0F) {
         TRUE_HEALTH.put(id, max(maid));
      }
   }

   public static void forget(EntityMaid maid) {
      forget(maid.getUUID());
   }

   public static void forget(UUID id) {
      TRUE_HEALTH.remove(id);
      TRUE_MAX.remove(id);
      ENROLLED_AT.remove(id);
      SlabAudit.forget(id);
   }

   public static void reconnect(EntityMaid maid) {
      UUID id = maid.getUUID();
      if (!TRUE_HEALTH.containsKey(id)) {
         enroll(maid);
      }
   }

   private static void trim() {
      int over = TRUE_HEALTH.size() - 4096;
      if (over > 0) {
         List<UUID> order = new ArrayList<>(ENROLLED_AT.keySet());
         order.sort(Comparator.comparingLong(idx -> ENROLLED_AT.getOrDefault(idx, 0L)));

         for (UUID id : order) {
            if (over-- <= 0) {
               break;
            }

            TRUE_HEALTH.remove(id);
            TRUE_MAX.remove(id);
            ENROLLED_AT.remove(id);
         }
      }
   }

   public static boolean fixCeiling(EntityMaid maid) {
      float real = max(maid);
      if (real <= 2.0F) {
         return false;
      }

      try {
         AttributeInstance attribute = maid.getAttribute(Attributes.MAX_HEALTH);
         if (attribute == null) {
            return false;
         }

         if (attribute.getBaseValue() >= real - 0.01) {
            return false;
         }

         attribute.setBaseValue(real);
         if (maid.getHealth() > real) {
            maid.setHealth(real);
         }

         return true;
      } catch (Throwable t) {
         return false;
      }
   }

   private static float maxOf(EntityMaid maid) {
      try {
         float max = maid.getMaxHealth();
         if (max > 2.0F) {
            return max;
         }

         AttributeInstance attribute = maid.getAttribute(Attributes.MAX_HEALTH);
         return attribute != null ? (float)attribute.getAttribute().getDefaultValue() : 20.0F;
      } catch (Throwable t) {
         return 20.0F;
      }
   }
}

