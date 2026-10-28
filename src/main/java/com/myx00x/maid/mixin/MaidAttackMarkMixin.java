package com.myx00x.maid.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.misc.DefaultMonsterType;
import com.github.tartaricacid.touhoulittlemaid.entity.misc.MonsterType;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.combat.HateBoard;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DefaultMonsterType.class, remap = false)
public class MaidAttackMarkMixin {
   @Inject(method = "canAttack", at = @At("HEAD"), remap = false, cancellable = true, require = 0)
   private static void shenlin.markWins(EntityMaid maid, LivingEntity target, MonsterType type, CallbackInfoReturnable<Boolean> cir) {
      if (type == MonsterType.NEUTRAL && HateBoard.markedBy(maid, target)) {
         cir.setReturnValue(true);
      }
   }
}

