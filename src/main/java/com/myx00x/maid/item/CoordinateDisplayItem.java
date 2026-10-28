package com.myx00x.maid.item;

import com.myx00x.maid.registry.ModItems;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public class CoordinateDisplayItem extends Item {
   private static final String KEY_X = "OriginX";
   private static final String KEY_Y = "OriginY";
   private static final String KEY_Z = "OriginZ";
   private static final String KEY_DIM = "OriginDim";
   private static final String GHOST_KEY = "shenlin_ghost";

   public CoordinateDisplayItem(Properties properties) {
      super(properties);
   }

   public static void bindOrigin(ItemStack stack, ResourceKey<Level> dimension, BlockPos core) {
      CompoundTag tag = stack.getOrCreateTag();
      tag.putInt("OriginX", core.getX());
      tag.putInt("OriginY", core.getY());
      tag.putInt("OriginZ", core.getZ());
      tag.putString("OriginDim", dimension.location().toString());
   }

   @Nullable
   public static BlockPos originOf(ItemStack stack) {
      CompoundTag tag = stack.getTag();
      return tag != null && tag.contains("OriginX") ? new BlockPos(tag.getInt("OriginX"), tag.getInt("OriginY"), tag.getInt("OriginZ")) : null;
   }

   public static String originDimOf(ItemStack stack) {
      CompoundTag tag = stack.getTag();
      return tag == null ? "" : tag.getString("OriginDim");
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      return InteractionResultHolder.pass(player.getItemInHand(hand));
   }

   public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int timeLeft) {
   }

   public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
   }

   public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
      super.inventoryTick(stack, level, entity, slot, selected);
      if (entity instanceof LivingEntity living && isGhost(living) && !holdsDisplay(living)) {
         setGhost(living, false);
      }
   }

   private static boolean holdsDisplay(LivingEntity living) {
      return living.getMainHandItem().is((Item)ModItems.COORDINATE_DISPLAY.get()) || living.getOffhandItem().is((Item)ModItems.COORDINATE_DISPLAY.get());
   }

   public static boolean isGhost(Entity entity) {
      return entity.getPersistentData().getBoolean("shenlin_ghost");
   }

   public static void setGhost(LivingEntity entity, boolean on) {
      if (on) {
         entity.getPersistentData().putBoolean("shenlin_ghost", true);
         entity.noPhysics = true;
      } else if (isGhost(entity)) {
         entity.getPersistentData().remove("shenlin_ghost");
         entity.noPhysics = false;
      }
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("item.goddess_of_maids.coordinate_display.desc1").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("item.goddess_of_maids.coordinate_display.desc2").withStyle(ChatFormatting.LIGHT_PURPLE));
   }
}

