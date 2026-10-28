package com.myx00x.maid.registry;

import com.myx00x.maid.block.RelicControlCoreBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
   public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "goddess_of_maids");
   public static final RegistryObject<Block> RELIC_CONTROL_CORE = BLOCKS.register(
      "relic_control_core",
      () -> new RelicControlCoreBlock(Properties.of().mapColor(MapColor.METAL).strength(66.0F, 66.0F).sound(SoundType.METAL))
   );

   public static void register(IEventBus bus) {
      BLOCKS.register(bus);
   }
}

