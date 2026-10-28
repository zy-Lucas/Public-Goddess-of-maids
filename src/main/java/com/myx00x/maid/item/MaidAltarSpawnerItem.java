package com.myx00x.maid.item;

import com.github.tartaricacid.touhoulittlemaid.api.block.IMultiBlock;
import com.github.tartaricacid.touhoulittlemaid.block.multiblock.MultiBlockAltar;
import com.github.tartaricacid.touhoulittlemaid.block.multiblock.MultiBlockManager;
import com.github.tartaricacid.touhoulittlemaid.init.InitBlocks;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.compat.Upstream;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

public class MaidAltarSpawnerItem extends Item {
   private static final int HORIZONTAL_OFFSET = 3;

   public MaidAltarSpawnerItem() {
      super(new Properties().stacksTo(1).rarity(Rarity.RARE));
   }

   public InteractionResult useOn(UseOnContext context) {
      Player player = context.getPlayer();
      if (player == null) {
         return InteractionResult.PASS;
      } else if (context.getLevel().isClientSide()) {
         return InteractionResult.SUCCESS;
      } else {
         return this.raise((ServerLevel)context.getLevel(), player, context.getItemInHand()) ? InteractionResult.SUCCESS : InteractionResult.PASS;
      }
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (level.isClientSide()) {
         return InteractionResultHolder.success(stack);
      } else {
         return this.raise((ServerLevel)level, player, stack) ? InteractionResultHolder.success(stack) : InteractionResultHolder.pass(stack);
      }
   }

   private boolean raise(ServerLevel level, Player player, ItemStack stack) {
      if (!Upstream.MAID) {
         player.displayClientMessage(Component.translatable("item.goddess_of_maids.maid_altar_spawner.no_maid").withStyle(ChatFormatting.DARK_PURPLE), true);
         return false;
      }

      try {
         Vec3 look = player.getLookAngle();
         Direction facing = Direction.getNearest(look.x, 0.0, look.z);

         for (IMultiBlock multi : MultiBlockManager.getMultiBlockList()) {
            if (multi instanceof MultiBlockAltar) {
               StructureTemplate template = multi.getTemplate(level, facing);
               if (template != null) {
                  BlockPos origin = BlockPos.containing(player.getX(), player.getY(), player.getZ()).offset(-3, 0, -3);
                  multi.build(level, origin, facing, template);
                  this.setBase(level, origin, template);
                  level.playSound(null, origin, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 0.8F);
                  if (!player.isCreative()) {
                     stack.shrink(1);
                  }

                  return true;
               }
            }
         }
      } catch (Throwable e) {
         ShenlinMod.LOGGER.error("祭坛没立起来", e);
         player.displayClientMessage(Component.translatable("item.goddess_of_maids.maid_altar_spawner.failed").withStyle(ChatFormatting.RED), true);
      }

      return false;
   }

   private void setBase(ServerLevel level, BlockPos origin, StructureTemplate template) {
      Block altar = (Block)InitBlocks.ALTAR.get();
      int sx = template.getSize().getX();
      int sz = template.getSize().getZ();

      for (int x = 0; x < sx; x++) {
         for (int z = 0; z < sz; z++) {
            BlockPos foot = origin.offset(x, 0, z);
            if (level.getBlockState(foot).is(altar)) {
               level.setBlockAndUpdate(foot.below(), Blocks.CRYING_OBSIDIAN.defaultBlockState());
            }
         }
      }
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("item.goddess_of_maids.maid_altar_spawner.desc1").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("item.goddess_of_maids.maid_altar_spawner.desc2").withStyle(ChatFormatting.DARK_PURPLE));
   }
}

