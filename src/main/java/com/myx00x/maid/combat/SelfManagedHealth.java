package com.myx00x.maid.combat;

import com.myx00x.maid.ShenlinMod;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.LivingEntity;

public final class SelfManagedHealth {
   private static final Map<Class<?>, Boolean> CACHE = new ConcurrentHashMap<>();

   private SelfManagedHealth() {
   }

   public static boolean is(LivingEntity target) {
      if (target == null) {
         return false;
      }

      Boolean cached = CACHE.get(target.getClass());
      if (cached != null) {
         return cached;
      }

      boolean found = false;

      try {
         for (Method m : target.getClass().getMethods()) {
            String n = m.getName();
            if ("maidspell.authoritativeHealth".equals(n) || "maidspell.authoritativeMaxHealth".equals(n)) {
               found = true;
               break;
            }
         }
      } catch (Throwable var8) {
      }

      CACHE.put(target.getClass(), found);
      return found;
   }

   public static boolean redirect(LivingEntity target, float damage, LivingEntity attacker) {
      if (target != null && !(damage <= 0.0F)) {
         try {
            Method m = target.getClass().getMethod("maidspell.redirectTrueDamage", float.class, LivingEntity.class);
            return m.invoke(target, damage, attacker) instanceof Boolean b && b;
         } catch (Throwable t) {
            ShenlinMod.LOGGER.info("[shenlin] 自管血量 boss 没有可用口子：{}", t.toString());
            return false;
         }
      } else {
         return false;
      }
   }
}

