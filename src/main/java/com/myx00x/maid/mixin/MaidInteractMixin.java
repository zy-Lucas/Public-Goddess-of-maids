package com.myx00x.maid.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.Config;
import com.myx00x.maid.combat.GuardKeeper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityMaid.class, remap = false)
public class MaidInteractMixin {
   @Inject(method = "m_6071_", at = @At("HEAD"), remap = false)
   private void shenlin.reviveBeforeGui(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
      EntityMaid maid = (EntityMaid)this;
      if ((Boolean)Config.GUARD_REWRITE.get() && !maid.level().isClientSide) {
         GuardKeeper.clearRemovalMark(maid);
         if (GuardKeeper.guards(maid)) {
            GuardKeeper.noZeroSkin(maid);
         }
      }
   }
}

