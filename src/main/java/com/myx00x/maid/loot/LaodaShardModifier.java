package com.myx00x.maid.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.myx00x.maid.registry.ModItems;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

public class LaodaShardModifier extends LootModifier {
   public static final Codec<LaodaShardModifier> CODEC = RecordCodecBuilder.create(
      inst -> inst.group(
            IGlobalLootModifier.LOOT_CONDITIONS_CODEC.fieldOf("conditions").forGetter(m -> m.conditions),
            Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(m -> m.count)
         )
         .apply(inst, LaodaShardModifier::new)
   );
   private final int count;

   public LaodaShardModifier(LootItemCondition[] conditions, int count) {
      super(conditions);
      this.count = count;
   }

   protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
      loot.add(new ItemStack((ItemLike)ModItems.LAODA_SHARD.get(), this.count));
      return loot;
   }

   public Codec<? extends IGlobalLootModifier> codec() {
      return CODEC;
   }
}

