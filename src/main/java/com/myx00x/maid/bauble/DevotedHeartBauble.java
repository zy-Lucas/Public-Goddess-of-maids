package com.myx00x.maid.bauble;

import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.combat.HeartGuard;
import net.minecraft.world.item.ItemStack;

public class DevotedHeartBauble implements IMaidBauble {
   public void onTick(EntityMaid maid, ItemStack baubleItem) {
      HeartGuard.tick(maid);
   }
}

