package com.myx00x.maid.registry;

import com.mojang.serialization.Codec;
import com.myx00x.maid.loot.LaodaShardModifier;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries.Keys;

public class ModLootModifiers {
   public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> MODIFIERS = DeferredRegister.create(
      Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, "goddess_of_maids"
   );
   public static final RegistryObject<Codec<LaodaShardModifier>> LAODA_SHARD = MODIFIERS.register("laoda_shard", () -> LaodaShardModifier.CODEC);

   public static void register(IEventBus bus) {
      MODIFIERS.register(bus);
   }
}

