package com.myx00x.maid.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.combat.GuardKeeper;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityMaid.class, remap = false)
public class MaidSkinHurtMixin {
   @Inject(method = "m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("RETURN"), remap = false)
   private void shenlin.settlePinnedMaid(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
      if (cir.getReturnValueZ()) {
         GuardKeeper.onSkinSettled((EntityMaid)this);
      }
   }
}

