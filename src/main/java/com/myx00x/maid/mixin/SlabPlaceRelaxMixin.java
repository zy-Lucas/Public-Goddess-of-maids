package com.myx00x.maid.mixin;

import com.github.tartaricacid.touhoulittlemaid.item.ItemSmartSlab;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ItemSmartSlab.class, remap = false)
public class SlabPlaceRelaxMixin {
   @Redirect(
      method = "m_6225_",
      at = @At(
         value = "INVOKE",
         target = "Lcom/github/tartaricacid/touhoulittlemaid/util/PlaceHelper;notSuitableForPlaceMaid(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z"
      ),
      remap = false
   )
   private boolean shenlin.skipPlaceCheck(Level level, BlockPos pos) {
      return false;
   }

   @Redirect(
      method = "m_6225_",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/context/UseOnContext;getClickedFace()Lnet/minecraft/core/Direction;"),
      remap = false
   )
   private Direction shenlin.treatAsUp(UseOnContext context) {
      return Direction.UP;
   }
}

