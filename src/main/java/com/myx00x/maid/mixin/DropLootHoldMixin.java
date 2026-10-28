package com.myx00x.maid.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.combat.GuardKeeper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LivingEntity.class, remap = false)
public class DropLootHoldMixin {
   @Inject(method = "m_6668_(Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At("HEAD"), remap = false, cancellable = true)
   private void shenlin.holdLoot(DamageSource cause, CallbackInfo ci) {
      if (this instanceof EntityMaid maid && GuardKeeper.guards(maid)) {
         ci.cancel();
      }
   }
}

