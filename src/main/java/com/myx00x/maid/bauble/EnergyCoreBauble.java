package com.myx00x.maid.bauble;

import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.ShenlinMod;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.ItemHandlerHelper;

public class EnergyCoreBauble implements IMaidBauble {
   private static final int PERIOD_TICKS = 1200;

   public void onTick(EntityMaid maid, ItemStack baubleItem) {
      if (!maid.level().isClientSide) {
         long gameTime = maid.level().getGameTime();
         int phase = (maid.getUUID().hashCode() & 2147483647) % 1200;
         if (gameTime % 1200L == phase) {
            try {
               ItemStack leftover = ItemHandlerHelper.insertItemStacked(maid.getAvailableInv(false), new ItemStack(Items.IRON_NUGGET), false);
               if (leftover.isEmpty()) {
                  return;
               }

               ItemEntity drop = new ItemEntity(maid.level(), maid.getX(), maid.getY() + 0.5, maid.getZ(), leftover);
               drop.setDefaultPickUpDelay();
               maid.level().addFreshEntity(drop);
            } catch (Throwable t) {
               ShenlinMod.LOGGER.error("[shenlin] energy core tick failed", t);
            }
         }
      }
   }

   public boolean syncClient(EntityMaid maid, ItemStack baubleItem) {
      return true;
   }
}

