package com.myx00x.maid.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.combat.HateBoard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityMaid.class, remap = false)
public class MaidRestrictRadiusMixin {
   @Inject(method = "m_21535_", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
   private void shenlin.widenUnderOrders(CallbackInfoReturnable<Float> cir) {
      EntityMaid maid = (EntityMaid)this;
      float wide = HateBoard.takeoverRadius(maid);
      if (wide > 0.0F && cir.getReturnValueF() < wide) {
         cir.setReturnValue(wide);
      }
   }
}

