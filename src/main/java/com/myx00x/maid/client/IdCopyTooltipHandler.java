package com.myx00x.maid.client;

import com.myx00x.maid.compat.Upstream;
import com.myx00x.maid.item.ItemLookup;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT)
public final class IdCopyTooltipHandler {
   private IdCopyTooltipHandler() {
   }

   @SubscribeEvent
   public static void onTooltip(ItemTooltipEvent event) {
      if (Upstream.MAID) {
         Item copier = ItemLookup.of("touhou_little_maid", "entity_id_copy");
         if (copier != null && event.getItemStack().is(copier)) {
            event.getToolTip().add(Component.translatable("item.goddess_of_maids.entity_id_copy.mark").withStyle(ChatFormatting.GOLD));
         }
      }
   }
}

