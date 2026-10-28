package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.ShenlinMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public final class Annihilator {
   private Annihilator() {
   }

   public static boolean erase(LivingEntity target) {
      if (target != null && !target.isRemoved() && !(target instanceof Player)) {
         if (target instanceof EntityMaid guarded && GuardWatch.tierOf(guarded) != 0) {
            ShenlinMod.LOGGER.info("[shenlin] annihilate refused, maid {} still guarded", guarded.getUUID());
            return false;
         } else {
            try {
               RosterErase.erase(target);
            } catch (Throwable t) {
               ShenlinMod.LOGGER.warn("[shenlin] annihilate roster failed: {}", t.toString());
            }

            try {
               Executioner.setHealthSilently(target, 0.0F);
            } catch (Throwable var6) {
            }

            try {
               if (!target.isRemoved()) {
                  target.discard();
               }
            } catch (Throwable var5) {
            }

            try {
               if (target instanceof EntityMaid maid) {
                  Vanisher.vanishMaid(maid, false);
               } else {
                  Vanisher.vanish(target, false);
               }
            } catch (Throwable t) {
               ShenlinMod.LOGGER.warn("[shenlin] annihilate vanish failed: {}", t.toString());
            }

            try {
               HardRemoved.mark(target);
            } catch (Throwable var3) {
            }

            try {
               EraseWatch.register(target);
            } catch (Throwable t) {
               ShenlinMod.LOGGER.warn("[shenlin] annihilate watch register failed: {}", t.toString());
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public static int eraseAround(ServerLevel level, double x, double y, double z, double radius) {
      if (level != null && !(radius <= 0.0)) {
         AABB area = new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
         int count = 0;

         for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (!(living instanceof Player) && !(living instanceof EntityMaid maid && GuardWatch.tierOf(maid) != 0) && erase(living)) {
               count++;
            }
         }

         return count;
      } else {
         return 0;
      }
   }
}

