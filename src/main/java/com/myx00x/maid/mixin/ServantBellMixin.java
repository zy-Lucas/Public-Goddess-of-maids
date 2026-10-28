package com.myx00x.maid.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.SchedulePos;
import com.github.tartaricacid.touhoulittlemaid.item.ItemServantBell;
import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.combat.WormholeRecall;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ItemServantBell.class, remap = false)
public class ServantBellMixin {
   @Unique
   private static List<UUID> shenlin.homeKept;

   @Shadow(remap = false)
   private UUID getMaidUuid(ItemStack stack) {
      throw new AssertionError();
   }

   @Inject(
      method = "m_5551_(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;I)V",
      at = @At("HEAD"),
      cancellable = true,
      remap = false
   )
   private void shenlin.wormhole(ItemStack stack, Level level, LivingEntity user, int timeLeft, CallbackInfo ci) {
      if (timeLeft >= 20 && level instanceof ServerLevel server && user instanceof Player player) {
         try {
            if (WormholeRecall.recall(server, player, stack, this.getMaidUuid(stack))) {
               ci.cancel();
            }
         } catch (Throwable t) {
            ShenlinMod.LOGGER.warn("[shenlin] wormhole recall failed: {}", t.toString());
         }
      }
   }

   @Inject(method = "teleportMaid", at = @At("HEAD"), remap = false)
   private void shenlin.snapshotHome(Player player, List<? extends EntityMaid> maids, CallbackInfo ci) {
      List<UUID> kept = new ArrayList<>();
      if ((Boolean)Config.SERVANT_BELL_KEEP_HOME.get()) {
         for (EntityMaid maid : maids) {
            if (maid.isHomeModeEnable()) {
               kept.add(maid.getUUID());
            }
         }
      }

      shenlin.homeKept = kept;
   }

   @Inject(method = "teleportMaid", at = @At("RETURN"), remap = false)
   private void shenlin.restoreHome(Player player, List<? extends EntityMaid> maids, CallbackInfo ci) {
      List<UUID> kept = shenlin.homeKept;
      shenlin.homeKept = null;
      if (kept != null && !kept.isEmpty()) {
         for (EntityMaid maid : maids) {
            if (kept.contains(maid.getUUID())) {
               maid.setHomeModeEnable(true);
               if ((Boolean)Config.SERVANT_BELL_MOVE_HOME.get()) {
                  shenlin.moveHome(maid);
                  ShenlinMod.LOGGER
                     .info("[shenlin] bell move home maid {} to {} {} {}", new Object[]{maid.getUUID(), maid.getBlockX(), maid.getBlockY(), maid.getBlockZ()});
               } else {
                  ShenlinMod.LOGGER.info("[shenlin] bell keep home maid {}", maid.getUUID());
               }
            }
         }
      }
   }

   @Unique
   private static void shenlin.moveHome(EntityMaid maid) {
      SchedulePos pos = maid.getSchedulePos();
      BlockPos here = maid.blockPosition();
      pos.setDimension(maid.level().dimension().location());
      pos.setWorkPos(here);
      pos.setIdlePos(here);
      pos.setSleepPos(here);
      pos.setConfigured(true);
      pos.restrictTo(maid);
   }
}

