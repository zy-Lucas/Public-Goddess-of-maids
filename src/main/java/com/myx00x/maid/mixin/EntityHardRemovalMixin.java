package com.myx00x.maid.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.Config;
import com.myx00x.maid.combat.GuardKeeper;
import com.myx00x.maid.combat.MaidRecovery;
import com.myx00x.maid.combat.ShieldWatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Entity.class, priority = 2000, remap = false)
public class EntityHardRemovalMixin {
   @Inject(method = "m_142467_", at = @At("HEAD"), remap = false, cancellable = true)
   private void shenlin.holdGuardedMaid(RemovalReason reason, CallbackInfo ci) {
      if (this instanceof EntityMaid maid && onRemove(maid, reason)) {
         ci.cancel();
      }
   }

   @Inject(method = "m_141960_(Lnet/minecraft/world/level/entity/EntityInLevelCallback;)V", at = @At("HEAD"), remap = false, cancellable = true)
   private void shenlin.holdLevelCallback(EntityInLevelCallback callback, CallbackInfo ci) {
      if (this instanceof EntityMaid maid) {
         if (callback == null || callback == EntityInLevelCallback.NULL) {
            if (!GuardKeeper.letGo(maid, maid.getRemovalReason())) {
               if ((Boolean)Config.GUARD_REWRITE.get()) {
                  if (GuardKeeper.guards(maid)) {
                     GuardKeeper.probe(maid, "callback-null", "drop");
                     GuardKeeper.noteTrouble(maid);
                     ci.cancel();
                  }
               } else {
                  if (ShieldWatcher.onCallbackDrop(maid)) {
                     MaidRecovery.unfreeze(maid);
                     ci.cancel();
                  }
               }
            }
         }
      }
   }

   private static boolean onRemove(EntityMaid maid, RemovalReason reason) {
      if (GuardKeeper.letGo(maid, reason)) {
         return false;
      } else if ((Boolean)Config.GUARD_REWRITE.get()) {
         return GuardKeeper.onForeignRemove(maid, reason);
      } else {
         return !Config.SHIELD_HOLD_CLEARS.get() ? false : ShieldWatcher.onRemovalAttempt(maid, reason, true);
      }
   }
}

