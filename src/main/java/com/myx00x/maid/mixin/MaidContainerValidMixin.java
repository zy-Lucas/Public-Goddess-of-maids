package com.myx00x.maid.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.AbstractMaidContainer;
import com.myx00x.maid.Config;
import com.myx00x.maid.combat.GuardKeeper;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractMaidContainer.class, remap = false)
public class MaidContainerValidMixin {
   @Inject(method = "m_6875_(Lnet/minecraft/world/entity/player/Player;)Z", at = @At("RETURN"), remap = false)
   private void shenlin.traceValidity(Player player, CallbackInfoReturnable<Boolean> cir) {
      if ((Boolean)Config.GUARD_REWRITE.get() && !cir.getReturnValueZ()) {
         AbstractMaidContainer self = (AbstractMaidContainer)this;
         EntityMaid maid = self.getMaid();
         if (maid != null && !maid.level().isClientSide) {
            GuardKeeper.probe(
               maid,
               "container-invalid",
               "owned="
                  + maid.isOwnedBy(player)
                  + " alive="
                  + maid.isAlive()
                  + " sleeping="
                  + maid.isSleeping()
                  + " reach="
                  + player.canReach(maid, 3.0)
                  + " dist="
                  + String.format("%.2f", maid.distanceTo(player))
                  + " maid="
                  + (int)maid.getX()
                  + ","
                  + (int)maid.getY()
                  + ","
                  + (int)maid.getZ()
                  + " at="
                  + (int)player.getX()
                  + ","
                  + (int)player.getY()
                  + ","
                  + (int)player.getZ()
                  + " hp="
                  + maid.getHealth()
            );
         }
      }
   }
}

