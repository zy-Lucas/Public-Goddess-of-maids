package com.myx00x.maid.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.Config;
import com.myx00x.maid.combat.GuardKeeper;
import com.myx00x.maid.combat.ShieldWatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTickList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityTickList.class, remap = false)
public class EntityTickListMixin {
   @Inject(method = "m_156912_(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), remap = false, cancellable = true)
   private void shenlin.holdTickingMaid(Entity entity, CallbackInfo ci) {
      if (entity instanceof EntityMaid maid) {
         if (!GuardKeeper.letGo(maid, maid.getRemovalReason())) {
            if ((Boolean)Config.GUARD_REWRITE.get()) {
               if (GuardKeeper.guards(maid)) {
                  GuardKeeper.probe(maid, "ticklist-evict", maid.getRemovalReason());
                  GuardKeeper.noteTrouble(maid);
                  ci.cancel();
               }
            } else {
               if (ShieldWatcher.onTickListEvict(entity)) {
                  ci.cancel();
               }
            }
         }
      }
   }
}

