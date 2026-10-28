package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;

public final class Nudge {
   private Nudge() {
   }

   public static void strike(Player owner, LivingEntity target) {
      double amount = (Double)Config.HATE_MARK_DAMAGE.get();
      if (!(amount <= 0.0) && !(target instanceof Player) && !(target instanceof EntityMaid)) {
         boolean hit = false;

         try {
            hit = target.hurt(owner.damageSources().playerAttack(owner), (float)amount);
         } catch (Throwable t) {
            ShenlinMod.LOGGER.warn("[shenlin] hate mark strike failed: {}", t.getMessage());
         }

         boolean forgiven = forgive(target, owner);
         ShenlinMod.LOGGER.info("[shenlin] hate nudge {} dmg={} hit={} forgiven={}", new Object[]{target.getEncodeId(), amount, hit, forgiven});
      }
   }

   private static boolean forgive(LivingEntity target, Player owner) {
      boolean done = false;

      try {
         if (target.getLastHurtByMob() == owner) {
            target.setLastHurtByMob(null);
            done = true;
         }

         if (target instanceof NeutralMob neutral && owner.getUUID().equals(neutral.getPersistentAngerTarget())) {
            neutral.setPersistentAngerTarget(null);
            neutral.setRemainingPersistentAngerTime(0);
            done = true;
         }

         if (target instanceof Warden warden) {
            warden.clearAnger(owner);
         }

         if (target instanceof Mob mob && mob.getTarget() == owner) {
            mob.setTarget(null);
            Brain brain = mob.getBrain();
            if (brain != null && hunting(brain, owner)) {
               brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
            }

            done = true;
         }
      } catch (Throwable t) {
         ShenlinMod.LOGGER.warn("[shenlin] hate mark forgive failed: {}", t.getMessage());
      }

      return done;
   }

   private static boolean hunting(Brain<?> brain, Player owner) {
      try {
         return brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) == owner;
      } catch (Throwable t) {
         return false;
      }
   }
}

