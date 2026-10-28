package com.myx00x.maid.item;

import com.myx00x.maid.mob.RelicWardenEntity;
import com.myx00x.maid.world.RelicDoor;
import com.myx00x.maid.world.RelicEyeSearch;
import com.myx00x.maid.world.RelicSites;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class RelicEyeItem extends Item {
   private static final String TAG_SITE = "Site";
   private static final String TAG_DIM = "SiteDim";
   private static final int LAND_X = 28;
   private static final int LAND_Y = 8;
   private static final int LAND_Z = 0;

   public RelicEyeItem() {
      super(new Properties().stacksTo(1));
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (level.isClientSide) {
         return InteractionResultHolder.success(stack);
      }

      if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
         CompoundTag tag = stack.getTag();
         if (tag != null && tag.contains("Site")) {
            BlockPos site = BlockPos.of(tag.getLong("Site"));
            String dim = tag.getString("SiteDim");
            ResourceLocation id = dim.isEmpty() ? null : ResourceLocation.tryParse(dim);
            ServerLevel target = id == null ? null : serverPlayer.server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
            if (target == null) {
               serverPlayer.displayClientMessage(Component.translatable("item.goddess_of_maids.relic_eye.none"), true);
               return InteractionResultHolder.success(stack);
            }

            if (target != serverPlayer.serverLevel()) {
               land(serverPlayer, target, site);
               RelicEyeSearch.verifyNextTick(serverPlayer, stack, target, site);
               return InteractionResultHolder.success(stack);
            }

            if (!RelicSites.get(target).isClaimed(site)) {
               RelicDoor.open(serverPlayer, site, stack);
               return InteractionResultHolder.success(stack);
            }

            tag.remove("Site");
            tag.remove("SiteDim");
         }

         BlockPos known = nearestKnown(serverLevel, serverPlayer);
         if (known != null) {
            RelicDoor.open(serverPlayer, known, stack);
            return InteractionResultHolder.success(stack);
         } else {
            serverPlayer.displayClientMessage(Component.translatable("item.goddess_of_maids.relic_eye.searching"), true);
            RelicEyeSearch.start(serverPlayer, stack, hand, serverLevel);
            return InteractionResultHolder.success(stack);
         }
      } else {
         return InteractionResultHolder.pass(stack);
      }
   }

   private static BlockPos nearestKnown(ServerLevel level, ServerPlayer player) {
      BlockPos from = player.blockPosition();
      BlockPos best = null;
      double bestDist = Double.MAX_VALUE;

      for (BlockPos site : RelicSites.get(level).unexplored()) {
         double d = site.distSqr(from);
         if (!(d >= bestDist)) {
            if (!stillGuarded(level, site)) {
               RelicSites.get(level).markClaimed(site);
            } else {
               bestDist = d;
               best = site;
            }
         }
      }

      return best;
   }

   private static boolean stillGuarded(ServerLevel level, BlockPos site) {
      return level.getChunkSource().getChunkNow(site.getX() >> 4, site.getZ() >> 4) == null
         ? true
         : !level.getEntitiesOfClass(RelicWardenEntity.class, new AABB(site).inflate(48.0)).isEmpty();
   }

   public static void bind(ItemStack stack, ServerLevel level, BlockPos site) {
      CompoundTag tag = stack.getOrCreateTag();
      tag.putLong("Site", site.asLong());
      tag.putString("SiteDim", level.dimension().location().toString());
   }

   public static void land(ServerPlayer player, ServerLevel level, BlockPos site) {
      double x = site.getX() + 28 + 0.5;
      double y = site.getY() + 8;
      double z = site.getZ() + 0 + 0.5;
      if (player.level() != level) {
         player.teleportTo(level, x, y, z, player.getYRot(), player.getXRot());
      } else {
         player.connection.teleport(x, y, z, player.getYRot(), player.getXRot());
      }
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("item.goddess_of_maids.relic_eye.desc1").withStyle(ChatFormatting.LIGHT_PURPLE));
      CompoundTag tag = stack.getTag();
      if (tag != null && tag.contains("Site")) {
         BlockPos site = BlockPos.of(tag.getLong("Site"));
         tooltip.add(
            Component.translatable("item.goddess_of_maids.relic_eye.bound", new Object[]{site.getX(), site.getY(), site.getZ()})
               .withStyle(ChatFormatting.GREEN)
         );
      }
   }
}

