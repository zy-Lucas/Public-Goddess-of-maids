package com.myx00x.maid.mixin;

import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemStack.class, remap = false)
public class SlabPriorityMixin {
   @Inject(method = "onItemUseFirst", at = @At("HEAD"), remap = false, cancellable = true)
   private void shenlin.spawnBeforeBlock(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
      if ((Boolean)Config.SERVANT_BELL_SLAB_PRIORITY.get()) {
         ItemStack self = (ItemStack)this;
         if (self.is((Item)InitItems.SMART_SLAB_HAS_MAID.get()) || self.is((Item)InitItems.SMART_SLAB_INIT.get())) {
            InteractionResult result = self.useOn(context);
            if (result.consumesAction()) {
               ShenlinMod.LOGGER
                  .info("[shenlin] slab {} placed ahead of the block", context.getPlayer() == null ? "?" : context.getPlayer().getGameProfile().getName());
               cir.setReturnValue(result);
            }
         }
      }
   }
}

