package com.myx00x.maid.event;

import com.github.tartaricacid.touhoulittlemaid.api.event.MaidBaubleChangeEvent.TakeOff;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.AbstractMaidContainer;
import com.github.tartaricacid.touhoulittlemaid.inventory.handler.BaubleItemHandler;
import com.myx00x.maid.ShenlinMod;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class BaubleSeal {
   private static final Set<String> SEALED = Set.of(
      "final_slash",
      "guardian_shield",
      "devoted_heart",
      "black_hole_scroll",
      "energy_core",
      "hover_cannon",
      "teleport_wormhole",
      "chaos_book",
      "dream_cat_crystal"
   );

   private BaubleSeal() {
   }

   @SubscribeEvent
   public static void onTakeOff(TakeOff event) {
      EntityMaid maid = event.getMaid();
      ItemStack stack = event.getBaubleItem();
      if (maid != null && !maid.level().isClientSide() && !stack.isEmpty()) {
         if (SEALED.contains(stack.getItem().builtInRegistryHolder().key().location().getPath())) {
            if (!uiOpen(maid)) {
               BaubleItemHandler handler = maid.getMaidBauble();
               if (handler != null) {
                  ItemStack back = handler.insertItem(0, stack.copy(), false);
                  ShenlinMod.LOGGER.info("[shenlin] 饰品焊住 {} 上的 {}（界外被脱，已塞回；没退回的余量 {}）", new Object[]{maid.getUUID(), stack.getItem(), back.getCount()});
               }
            }
         }
      }
   }

   private static boolean uiOpen(EntityMaid maid) {
      if (maid.level() instanceof ServerLevel level) {
         if (level.getServer() == null) {
            return false;
         }

         for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.containerMenu instanceof AbstractMaidContainer open && open.getMaid() == maid) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }
}

