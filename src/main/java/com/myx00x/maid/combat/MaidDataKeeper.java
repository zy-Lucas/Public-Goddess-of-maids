package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.nbt.CompoundTag;

public final class MaidDataKeeper {
   private MaidDataKeeper() {
   }

   public static CompoundTag snapshot(EntityMaid maid) {
      CompoundTag nbt = new CompoundTag();

      try {
         maid.saveWithoutId(nbt);
      } catch (Throwable t) {
         return null;
      }

      return nbt.isEmpty() ? null : nbt;
   }
}

