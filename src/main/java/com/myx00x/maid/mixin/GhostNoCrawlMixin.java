package com.myx00x.maid.mixin;

import com.myx00x.maid.item.CoordinateDisplayItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public class GhostNoCrawlMixin {
   @ModifyVariable(method = "m_20124_", at = @At("HEAD"), argsOnly = true, remap = false)
   private Pose shenlin.noCrawlWhileGhost(Pose pose) {
      if (pose != Pose.SWIMMING) {
         return pose;
      } else if (this instanceof Player self) {
         return !CoordinateDisplayItem.isGhost(self) ? pose : Pose.STANDING;
      } else {
         return pose;
      }
   }
}

