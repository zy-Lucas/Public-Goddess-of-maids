package com.myx00x.maid.mixin;

import java.lang.reflect.Field;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HurtByTargetGoal.class, remap = false)
public class HurtByTargetGoalMixin {
   private static Field mobField;
   private static boolean mobResolved;

   @Inject(method = "m_26047_()V", at = @At("HEAD"), remap = false, cancellable = true, require = 0)
   private void shenlin.skipAlertWithoutAttacker(CallbackInfo ci) {
      Mob mob = mobOf(this);
      if (mob != null && mob.getLastHurtByMob() == null) {
         ci.cancel();
      }
   }

   private static Mob mobOf(Object goal) {
      resolve();
      if (mobField == null) {
         return null;
      }

      try {
         return (Mob)mobField.get(goal);
      } catch (Throwable t) {
         return null;
      }
   }

   private static void resolve() {
      if (!mobResolved) {
         mobResolved = true;

         for (String name : new String[]{"f_26135_", "mob"}) {
            try {
               Field f = TargetGoal.class.getDeclaredField(name);
               f.setAccessible(true);
               mobField = f;
               return;
            } catch (Throwable var5) {
            }
         }
      }
   }
}

