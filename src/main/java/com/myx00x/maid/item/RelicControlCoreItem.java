package com.myx00x.maid.item;

import com.myx00x.maid.block.RelicControlCoreBlock;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public class RelicControlCoreItem extends BlockItem {
   public RelicControlCoreItem(RelicControlCoreBlock block, Properties properties) {
      super(block, properties);
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("block.goddess_of_maids.relic_control_core.desc1").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("block.goddess_of_maids.relic_control_core.desc2").withStyle(ChatFormatting.DARK_GRAY));
   }
}

