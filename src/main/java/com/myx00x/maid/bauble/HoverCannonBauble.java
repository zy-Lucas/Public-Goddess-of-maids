package com.myx00x.maid.bauble;

import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.combat.HoverCannon;
import net.minecraft.world.item.ItemStack;

public class HoverCannonBauble implements IMaidBauble {
   public void onTick(EntityMaid maid, ItemStack baubleItem) {
      HoverCannon.tick(maid);
   }

   public void onTakeOff(EntityMaid maid, ItemStack baubleItem) {
      HoverCannon.stop(maid);
   }

   public boolean syncClient(EntityMaid maid, ItemStack baubleItem) {
      return true;
   }
}

