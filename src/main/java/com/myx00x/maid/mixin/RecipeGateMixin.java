package com.myx00x.maid.mixin;

import com.myx00x.maid.combat.RecipeGate;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RecipeManager.class, remap = false)
public class RecipeGateMixin {
   @Inject(
      method = "m_44015_(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/Container;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;",
      at = @At("RETURN"),
      cancellable = true,
      remap = false
   )
   private <C extends Container, T extends Recipe<C>> void shenlin.holdOne(
      RecipeType<T> type, C container, Level level, CallbackInfoReturnable<Optional<T>> cir
   ) {
      Optional<T> found = (Optional<T>)cir.getReturnValue();
      if (found != null && !found.isEmpty()) {
         if (RecipeGate.hold(type, found.get()) == null) {
            cir.setReturnValue(Optional.empty());
         }
      }
   }

   @Inject(method = "m_44013_(Lnet/minecraft/world/item/crafting/RecipeType;)Ljava/util/List;", at = @At("RETURN"), cancellable = true, remap = false)
   private <C extends Container, T extends Recipe<C>> void shenlin.filterAll(RecipeType<T> type, CallbackInfoReturnable<List<T>> cir) {
      List<T> all = (List<T>)cir.getReturnValue();
      if (all != null) {
         List<T> kept = RecipeGate.filtered(type, all);
         if (kept != all) {
            cir.setReturnValue(kept);
         }
      }
   }

   @Inject(
      method = "m_44056_(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/Container;Lnet/minecraft/world/level/Level;)Ljava/util/List;",
      at = @At("RETURN"),
      cancellable = true,
      remap = false
   )
   private <C extends Container, T extends Recipe<C>> void shenlin.filterMatched(
      RecipeType<T> type, C container, Level level, CallbackInfoReturnable<List<T>> cir
   ) {
      List<T> matched = (List<T>)cir.getReturnValue();
      if (matched != null) {
         List<T> kept = RecipeGate.filtered(type, matched);
         if (kept != matched) {
            cir.setReturnValue(kept);
         }
      }
   }
}

