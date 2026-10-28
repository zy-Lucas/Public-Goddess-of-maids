package com.myx00x.maid.event;

import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.item.ItemLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class StarterGiftHandler {
   private static final String GIVEN_TAG = "shenlin_id_copy_given";

   @SubscribeEvent
   public static void onLoggedIn(PlayerLoggedInEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         CompoundTag data = player.getPersistentData();
         if (!data.getBoolean("shenlin_id_copy_given")) {
            Item copier = ItemLookup.of("touhou_little_maid", "entity_id_copy");
            if (copier == null) {
               ShenlinMod.LOGGER.info("[shenlin] starter gift skipped: entity_id_copy not found");
            } else {
               ItemStack stack = new ItemStack(copier);
               if (!player.getInventory().add(stack)) {
                  player.drop(stack, false);
               }

               data.putBoolean("shenlin_id_copy_given", true);
               ShenlinMod.LOGGER.info("[shenlin] starter gift: entity_id_copy");
            }
         }
      }
   }
}

