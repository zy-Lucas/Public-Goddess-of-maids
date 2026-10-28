package com.myx00x.maid.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.combat.GuardRemoval;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Entity.class, remap = false)
public class DimensionMoveMarkMixin {
   @Inject(method = "m_6089_()V", at = @At("HEAD"), remap = false)
   private void shenlin.markDimensionMove(CallbackInfo ci) {
      if (this instanceof EntityMaid maid) {
         GuardRemoval.markDimensionMove(maid);
      }
   }

   @Inject(method = "m_264318_(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z", at = @At("HEAD"), remap = false)
   private void shenlin.markDimensionTeleport(CallbackInfoReturnable<Boolean> cir) {
      if (this instanceof EntityMaid maid) {
         GuardRemoval.markDimensionMove(maid);
      }
   }
}

