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

public class DevotedHeartItem extends Item {
   public DevotedHeartItem() {
      super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("item.goddess_of_maids.devoted_heart.desc1").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("item.goddess_of_maids.devoted_heart.desc2").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("item.goddess_of_maids.devoted_heart.desc3").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("item.goddess_of_maids.devoted_heart.desc4").withStyle(ChatFormatting.DARK_PURPLE));
   }
}

