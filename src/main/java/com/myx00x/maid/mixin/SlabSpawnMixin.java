package com.myx00x.maid.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.item.ItemSmartSlab;
import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.combat.GuardKeeper;
import com.myx00x.maid.combat.GuardWatch;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemSmartSlab.class, remap = false)
public class SlabSpawnMixin {
   @Inject(
      method = "spawnFromStore",
      at = @At(value = "INVOKE", shift = Shift.BEFORE, target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"),
      remap = false
   )
   private void shenlin.clearField(UseOnContext context, Player player, Level worldIn, EntityMaid maid, CallbackInfoReturnable<InteractionResult> cir) {
      if ((Boolean)Config.GUARD_REWRITE.get()) {
         ShenlinMod.LOGGER.info("[shenlin] maid {} spawn attempt", maid.getUUID());
         GuardWatch.reclaim(maid.getUUID(), maid);
      }
   }

   @Inject(
      method = "spawnFromStore",
      at = @At(value = "INVOKE", shift = Shift.AFTER, target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"),
      remap = false
   )
   private void shenlin.settleOnSpawn(UseOnContext context, Player player, Level worldIn, EntityMaid maid, CallbackInfoReturnable<InteractionResult> cir) {
      if ((Boolean)Config.GUARD_REWRITE.get()) {
         GuardKeeper.onUnpacked(maid);
      }
   }
}

