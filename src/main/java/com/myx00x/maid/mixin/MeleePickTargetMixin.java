package com.myx00x.maid.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.combat.HateBoard;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = StartAttacking.class, remap = false)
public class MeleePickTargetMixin {
   @ModifyVariable(method = "m_257741_", at = @At("HEAD"), argsOnly = true, index = 1, remap = false, require = 0)
   private static Function<Mob, Optional<? extends LivingEntity>> shenlin.noSelfPick(Function<Mob, Optional<? extends LivingEntity>> finder) {
      return mob -> mob instanceof EntityMaid maid && HateBoard.underOrders(maid) ? Optional.empty() : finder.apply(mob);
   }
}

