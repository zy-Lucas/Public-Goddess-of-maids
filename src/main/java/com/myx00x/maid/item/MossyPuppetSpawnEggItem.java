package com.myx00x.maid.item;

import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item.Properties;
import net.minecraftforge.common.ForgeSpawnEggItem;

public class MossyPuppetSpawnEggItem extends ForgeSpawnEggItem {
   public MossyPuppetSpawnEggItem(Supplier<? extends EntityType<? extends Mob>> type, Properties properties) {
      super(type, 16777215, 16777215, properties);
   }

   public int getColor(int tintIndex) {
      return -1;
   }
}

