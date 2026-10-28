package com.myx00x.maid.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.AbstractMaidContainer;
import com.myx00x.maid.Config;
import com.myx00x.maid.combat.GuardKeeper;
import com.myx00x.maid.combat.ShieldWatcher;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractMaidContainer.class, remap = false)
public class MaidContainerCloseMixin {
   @Inject(method = "m_6877_(Lnet/minecraft/world/entity/player/Player;)V", at = @At("TAIL"), remap = false)
   private void shenlin.keepOnClose(Player player, CallbackInfo ci) {
      if (!player.level().isClientSide) {
         EntityMaid maid = ((AbstractMaidContainer)this).getMaid();
         if ((Boolean)Config.GUARD_REWRITE.get() && maid != null) {
            GuardKeeper.probe(maid, "container-closed", player.getName().getString());
         }

         if (maid != null && ShieldWatcher.hasSnapshot(maid.getUUID())) {
            ShieldWatcher.record(maid);
         }
      }
   }
}

