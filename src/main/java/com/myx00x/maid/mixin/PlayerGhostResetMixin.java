package com.myx00x.maid.mixin;

import com.myx00x.maid.item.CoordinateDisplayItem;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PlayerGhostResetMixin {
   @Inject(
      method = "m_8119_",
      remap = false,
      at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Player;f_19794_:Z", opcode = 181, shift = Shift.AFTER)
   )
   private void shenlin.keepGhostAfterReset(CallbackInfo ci) {
      Player self = (Player)this;
      if (!self.noPhysics && CoordinateDisplayItem.isGhost(self)) {
         self.noPhysics = true;
      }
   }
}

