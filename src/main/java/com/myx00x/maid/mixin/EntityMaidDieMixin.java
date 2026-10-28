package com.myx00x.maid.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.Config;
import com.myx00x.maid.combat.ElixirKeeper;
import com.myx00x.maid.combat.GuardKeeper;
import com.myx00x.maid.combat.ShieldWatcher;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityMaid.class, remap = false)
public class EntityMaidDieMixin {
   @Inject(method = "m_6667_(Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At("HEAD"), remap = false, cancellable = true)
   private void shenlin.keepBeforeDie(DamageSource cause, CallbackInfo ci) {
      EntityMaid maid = (EntityMaid)this;
      if ((Boolean)Config.GUARD_REWRITE.get()) {
         if (GuardKeeper.onLethal(maid)) {
            ci.cancel();
         }
      } else if (ShieldWatcher.onMaidDie(maid)) {
         ci.cancel();
      } else {
         ShieldWatcher.markDeath(maid);
         ElixirKeeper.keep(maid);
      }
   }

   @Inject(method = "m_6667_(Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At("RETURN"), remap = false)
   private void shenlin.restoreAfterDie(DamageSource cause, CallbackInfo ci) {
      if (!(Boolean)Config.GUARD_REWRITE.get()) {
         ElixirKeeper.restoreIfRevived((EntityMaid)this);
      }
   }
}

