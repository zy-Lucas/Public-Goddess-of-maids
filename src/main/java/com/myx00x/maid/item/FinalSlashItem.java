package com.myx00x.maid.item;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.inventory.handler.BaubleItemHandler;
import com.myx00x.maid.Config;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public class FinalSlashItem extends Item {
   private static final String ERASE_TAG = "EraseMode";

   public FinalSlashItem() {
      super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
   }

   public static boolean eraseOn(ItemStack stack) {
      return stack.hasTag() && stack.getTag().getBoolean("EraseMode");
   }

   private static void setEraseOn(ItemStack stack, boolean on) {
      if (on) {
         stack.getOrCreateTag().putBoolean("EraseMode", true);
      } else if (stack.hasTag()) {
         stack.getTag().remove("EraseMode");
         if (stack.getTag().isEmpty()) {
            stack.setTag(null);
         }
      }
   }

   public static boolean eraseOn(EntityMaid maid) {
      try {
         BaubleItemHandler baubles = maid.getMaidBauble();

         for (int slot = 0; slot < baubles.getSlots(); slot++) {
            ItemStack stack = baubles.getStackInSlot(slot);
            if (stack.getItem() instanceof FinalSlashItem) {
               return eraseOn(stack);
            }
         }
      } catch (Throwable var4) {
      }

      return false;
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (!player.isShiftKeyDown()) {
         return InteractionResultHolder.pass(stack);
      }

      if (!level.isClientSide) {
         boolean on = eraseOn(stack);
         if (!on && !(Boolean)Config.FINAL_SLASH_ERASE_ALLOWED.get()) {
            player.displayClientMessage(Component.translatable("message.goddess_of_maids.final_slash.erase_disabled"), true);
            return InteractionResultHolder.fail(stack);
         }

         setEraseOn(stack, !on);
         player.displayClientMessage(Component.translatable(on ? "message.goddess_of_maids.final_slash.erase_off" : "message.goddess_of_maids.final_slash.erase_on"), true);
         player.inventoryMenu.broadcastChanges();
      }

      return InteractionResultHolder.success(stack);
   }

   public boolean isFoil(ItemStack stack) {
      return eraseOn(stack) || super.isFoil(stack);
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("item.goddess_of_maids.final_slash.desc1").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("item.goddess_of_maids.final_slash.desc2").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("item.goddess_of_maids.final_slash.desc3").withStyle(ChatFormatting.DARK_PURPLE));
      tooltip.add(Component.translatable("item.goddess_of_maids.final_slash.link_cercis").withStyle(ChatFormatting.DARK_PURPLE));
      tooltip.add(Component.translatable("item.goddess_of_maids.final_slash.desc4").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable(eraseOn(stack) ? "item.goddess_of_maids.final_slash.erase_on" : "item.goddess_of_maids.final_slash.erase_off"));
      tooltip.add(Component.translatable("item.goddess_of_maids.final_slash.desc5").withStyle(ChatFormatting.DARK_GRAY));
      tooltip.add(Component.translatable("item.goddess_of_maids.final_slash.desc6").withStyle(ChatFormatting.RED));
   }
}

