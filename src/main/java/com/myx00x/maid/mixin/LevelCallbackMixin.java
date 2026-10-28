package com.myx00x.maid.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.Config;
import com.myx00x.maid.combat.GuardKeeper;
import com.myx00x.maid.combat.ShieldWatcher;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.entity.EntityAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.level.entity.PersistentEntitySectionManager.Callback", remap = false)
public class LevelCallbackMixin {
   @Shadow(remap = false)
   @Final
   private EntityAccess entity;

   @Inject(method = "m_142472_(Lnet/minecraft/world/entity/Entity.RemovalReason;)V", at = @At("HEAD"), remap = false, cancellable = true)
   private void shenlin.holdGuardedMaid(RemovalReason reason, CallbackInfo ci) {
      if (this.entity instanceof EntityMaid maid) {
         if (!GuardKeeper.letGo(maid, reason)) {
            if (!(Boolean)Config.GUARD_REWRITE.get()) {
               if ((Boolean)Config.SHIELD_HOLD_CLEARS.get()) {
                  if (ShieldWatcher.onRemovalAttempt(maid, reason, false)) {
                     ci.cancel();
                  }
               }
            } else {
               if (GuardKeeper.guards(maid)) {
                  GuardKeeper.probe(maid, "level-callback", reason);
                  GuardKeeper.noteTrouble(maid);
                  ci.cancel();
               }
            }
         }
      }
   }
}

