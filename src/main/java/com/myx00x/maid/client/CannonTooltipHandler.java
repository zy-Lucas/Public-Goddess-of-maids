package com.myx00x.maid.client;

import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.AbstractMaidContainerGui;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.compat.Upstream;
import com.myx00x.maid.registry.ModItems;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT)
public final class CannonTooltipHandler {
   private CannonTooltipHandler() {
   }

   @SubscribeEvent
   public static void onTooltip(ItemTooltipEvent event) {
      if (Upstream.MAID) {
         if (event.getItemStack().is((Item)ModItems.HOVER_CANNON.get())) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof AbstractMaidContainerGui<?> gui) {
               EntityMaid maid = gui.getMaid();
               if (maid != null) {
                  boolean powered = maid.getMaidBauble().containsItem((Item)ModItems.ENERGY_CORE.get());
                  Component line = Component.translatable(powered ? "item.goddess_of_maids.hover_cannon.power_on" : "item.goddess_of_maids.hover_cannon.power_off")
                     .withStyle(powered ? ChatFormatting.GREEN : ChatFormatting.RED);
                  List<Component> tooltip = event.getToolTip();
                  tooltip.add(Math.max(0, tooltip.size() - 1), line);
               }
            }
         }
      }
   }
}

