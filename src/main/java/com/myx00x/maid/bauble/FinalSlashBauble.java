package com.myx00x.maid.bauble;

import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.combat.ExecutionCounter;
import com.myx00x.maid.combat.SlashPulse;
import com.myx00x.maid.combat.TargetKeeper;
import net.minecraft.world.item.ItemStack;

public class FinalSlashBauble implements IMaidBauble {
   public void onTick(EntityMaid maid, ItemStack baubleItem) {
      SlashPulse.tick(maid);
   }

   public void onTakeOff(EntityMaid maid, ItemStack baubleItem) {
      if (!maid.level().isClientSide) {
         ExecutionCounter.clearMaid(maid.getUUID());
         TargetKeeper.clear(maid.getUUID());
         SlashPulse.clear(maid.getUUID());
      }
   }
}

