package com.myx00x.maid.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public class BlackHoleScrollItem extends Item {
   public BlackHoleScrollItem() {
      super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("item.goddess_of_maids.black_hole_scroll.desc1").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("item.goddess_of_maids.black_hole_scroll.desc2").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("item.goddess_of_maids.black_hole_scroll.desc3").withStyle(ChatFormatting.DARK_RED));
      tooltip.add(Component.translatable("item.goddess_of_maids.black_hole_scroll.desc4").withStyle(ChatFormatting.DARK_GRAY));
   }

   public boolean isFoil(ItemStack stack) {
      return true;
   }
}

