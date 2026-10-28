package com.myx00x.maid.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.combat.GuardKeeper;
import com.myx00x.maid.combat.TargetKeeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityMaid.class, remap = false)
public class MaidTickMixin {
   @Inject(method = "m_8119_", at = @At("HEAD"), remap = false, require = 0)
   private void shenlin.keepTargets(CallbackInfo ci) {
      try {
         EntityMaid maid = (EntityMaid)this;
         if ((Boolean)Config.GUARD_REWRITE.get() && !maid.level().isClientSide) {
            GuardKeeper.clearRemovalMark(maid);
         }

         TargetKeeper.onMaidTick(maid);
      } catch (Throwable t) {
         ShenlinMod.LOGGER.warn("[shenlin] maid tick hook failed: {}", t.toString());
      }
   }
}

