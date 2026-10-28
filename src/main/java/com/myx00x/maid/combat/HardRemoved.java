package com.myx00x.maid.combat;

import com.myx00x.maid.ShenlinMod;
import java.lang.reflect.Field;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import sun.misc.Unsafe;

public final class HardRemoved {
   private static Object unsafe;
   private static long offset = -1L;
   private static boolean tried;
   private static boolean broken;

   private HardRemoved() {
   }

   public static boolean mark(Entity entity) {
      if (entity == null || broken) {
         return false;
      }

      if (!tried) {
         tried = true;

         try {
            Class<?> u = Class.forName("sun.misc.Unsafe");
            Field f = u.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = f.get(null);
            Field reason = findReasonField(Entity.class);
            if (reason == null) {
               broken = true;
               ShenlinMod.LOGGER.warn("[shenlin] hardRemoved: RemovalReason field not found");
               return false;
            }

            offset = ((Unsafe)unsafe).objectFieldOffset(reason);
         } catch (Throwable t) {
            broken = true;
            ShenlinMod.LOGGER.warn("[shenlin] hardRemoved unavailable: {}", t.toString());
            return false;
         }
      }

      try {
         Unsafe u = (Unsafe)unsafe;
         Object current = u.getObject(entity, offset);
         if (current != null) {
            return true;
         }

         u.putObject(entity, offset, RemovalReason.DISCARDED);
         return true;
      } catch (Throwable t) {
         broken = true;
         ShenlinMod.LOGGER.warn("[shenlin] hardRemoved write failed: {}", t.toString());
         return false;
      }
   }

   private static Field findReasonField(Class<?> owner) {
      for (Field f : owner.getDeclaredFields()) {
         if (f.getType().getName().endsWith(".RemovalReason")) {
            f.setAccessible(true);
            return f;
         }
      }

      Class<?> sup = owner.getSuperclass();
      return sup == null ? null : (sup == Object.class ? null : findReasonField(sup));
   }

   public static boolean available() {
      return !broken && (offset != -1L || !tried);
   }
}

