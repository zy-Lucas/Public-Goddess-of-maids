package com.myx00x.maid.client;

import com.myx00x.maid.Config;
import com.myx00x.maid.registry.ModItems;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT)
public final class HeartTooltipHandler {
   private HeartTooltipHandler() {
   }

   @SubscribeEvent
   public static void onTooltip(ItemTooltipEvent event) {
      if (event.getItemStack().is((Item)ModItems.DEVOTED_HEART.get())) {
         List<Component> tooltip = event.getToolTip();
         if (!Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.goddess_of_maids.devoted_heart.shift").withStyle(ChatFormatting.DARK_GRAY));
         } else {
            double full = (Double)Config.HEART_FULL_RANGE.get();
            double near = (Double)Config.HEART_NEAR_RANGE.get();
            double mid = (Double)Config.HEART_MID_RANGE.get();
            tooltip.add(Component.translatable("item.goddess_of_maids.devoted_heart.guard1", new Object[]{num(full)}).withStyle(ChatFormatting.DARK_PURPLE));
            tooltip.add(
               Component.translatable(
                     "item.goddess_of_maids.devoted_heart.guard2", new Object[]{num(full + 1.0), num(near), pct((Double)Config.HEART_NEAR_CUT.get())}
                  )
                  .withStyle(ChatFormatting.DARK_PURPLE)
            );
            tooltip.add(
               Component.translatable(
                     "item.goddess_of_maids.devoted_heart.guard3", new Object[]{num(near + 1.0), num(mid), pct((Double)Config.HEART_MID_CUT.get())}
                  )
                  .withStyle(ChatFormatting.DARK_PURPLE)
            );
            tooltip.add(
               Component.translatable("item.goddess_of_maids.devoted_heart.guard4", new Object[]{num(mid + 1.0), pct((Double)Config.HEART_FAR_CUT.get())})
                  .withStyle(ChatFormatting.DARK_PURPLE)
            );
         }
      }
   }

   private static String num(double value) {
      return value == Math.floor(value) ? String.valueOf((int)value) : String.valueOf(value);
   }

   private static String pct(double cut) {
      return String.valueOf((int)Math.round(cut * 100.0));
   }
}

