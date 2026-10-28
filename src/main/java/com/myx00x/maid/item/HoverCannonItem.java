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

public class HoverCannonItem extends Item {
   public HoverCannonItem() {
      super(new Properties().stacksTo(1).rarity(Rarity.RARE));
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("item.goddess_of_maids.hover_cannon.desc1").withStyle(ChatFormatting.BLUE));
      tooltip.add(Component.translatable("item.goddess_of_maids.hover_cannon.desc2").withStyle(ChatFormatting.GOLD));
      tooltip.add(Component.translatable("item.goddess_of_maids.hover_cannon.desc3").withStyle(ChatFormatting.GRAY));
   }
}

