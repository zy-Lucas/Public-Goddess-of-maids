package com.myx00x.maid.bauble;

import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.combat.BlackHoleKeeper;
import net.minecraft.world.item.ItemStack;

public class BlackHoleScrollBauble implements IMaidBauble {
   public void onTakeOff(EntityMaid maid, ItemStack baubleItem) {
      if (!maid.level().isClientSide) {
         BlackHoleKeeper.clear(maid.getUUID());
      }
   }
}

