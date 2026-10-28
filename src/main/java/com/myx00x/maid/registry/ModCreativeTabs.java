package com.myx00x.maid.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
   public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "goddess_of_maids");
   public static final RegistryObject<CreativeModeTab> MAIN = TABS.register(
      "main",
      () -> CreativeModeTab.builder()
         .title(Component.translatable("itemGroup.goddess_of_maids"))
         .icon(() -> new ItemStack((ItemLike)ModItems.FINAL_SLASH.get()))
         .displayItems((params, output) -> {
            output.accept((ItemLike)ModItems.LAODA_SHARD.get());
            output.accept((ItemLike)ModItems.FINAL_SLASH.get());
            output.accept((ItemLike)ModItems.GUARDIAN_SHIELD.get());
            output.accept((ItemLike)ModItems.DEVOTED_HEART.get());
            output.accept((ItemLike)ModItems.GUARDIAN_CORE.get());
            output.accept((ItemLike)ModItems.PROTECTION_CORE.get());
            output.accept((ItemLike)ModItems.BLACK_HOLE_SCROLL.get());
            output.accept((ItemLike)ModItems.ENERGY_CORE.get());
            output.accept((ItemLike)ModItems.HOVER_CANNON.get());
            output.accept((ItemLike)ModItems.TELEPORT_WORMHOLE.get());
            output.accept((ItemLike)ModItems.MAID_ALTAR_SPAWNER.get());
            output.accept((ItemLike)ModItems.COORDINATE_DISPLAY.get());
            output.accept((ItemLike)ModItems.RELIC_CONTROL_CORE.get());
            output.accept((ItemLike)ModItems.RELIC_EYE.get());
            output.accept((ItemLike)ModItems.MOSSY_PUPPET_SPAWN_EGG.get());
            output.accept((ItemLike)ModItems.RELIC_WARDEN_SPAWN_EGG.get());
         })
         .build()
   );

   public static void register(IEventBus bus) {
      TABS.register(bus);
   }
}

