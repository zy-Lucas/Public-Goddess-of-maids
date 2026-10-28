package com.myx00x.maid.block;

import com.myx00x.maid.item.CoordinateDisplayItem;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.storage.loot.LootParams.Builder;
import net.minecraft.world.phys.BlockHitResult;

public class RelicControlCoreBlock extends Block {
   private static final float DESTROY_TICKS = 1320.0F;

   public RelicControlCoreBlock(Properties properties) {
      super(properties);
   }

   public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
      return player.getAbilities().instabuild ? 1.0F : 7.5757573E-4F;
   }

   public List<ItemStack> getDrops(BlockState state, Builder params) {
      return Collections.emptyList();
   }

   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      ItemStack held = player.getItemInHand(hand);
      if (!(held.getItem() instanceof CoordinateDisplayItem)) {
         return InteractionResult.PASS;
      }

      if (!level.isClientSide) {
         CoordinateDisplayItem.bindOrigin(held, level.dimension(), pos);
         player.displayClientMessage(Component.translatable("msg.goddess_of_maids.coord_display.bind", new Object[]{pos.getX(), pos.getY(), pos.getZ()}), true);
      }

      return InteractionResult.sidedSuccess(level.isClientSide);
   }
}

