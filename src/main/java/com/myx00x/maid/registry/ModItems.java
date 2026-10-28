package com.myx00x.maid.registry;

import com.myx00x.maid.block.RelicControlCoreBlock;
import com.myx00x.maid.item.BlackHoleScrollItem;
import com.myx00x.maid.item.CoordinateDisplayItem;
import com.myx00x.maid.item.DevotedHeartItem;
import com.myx00x.maid.item.EnergyCoreItem;
import com.myx00x.maid.item.FinalSlashItem;
import com.myx00x.maid.item.GuardianCoreItem;
import com.myx00x.maid.item.GuardianShieldItem;
import com.myx00x.maid.item.HoverCannonItem;
import com.myx00x.maid.item.LaodaShardItem;
import com.myx00x.maid.item.MaidAltarSpawnerItem;
import com.myx00x.maid.item.MossyPuppetSpawnEggItem;
import com.myx00x.maid.item.ProtectionCoreItem;
import com.myx00x.maid.item.RelicControlCoreItem;
import com.myx00x.maid.item.RelicEyeItem;
import com.myx00x.maid.item.RelicWardenSpawnEggItem;
import com.myx00x.maid.item.TeleportWormholeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
   public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "goddess_of_maids");
   public static final RegistryObject<Item> FINAL_SLASH = ITEMS.register("final_slash", FinalSlashItem::new);
   public static final RegistryObject<Item> GUARDIAN_SHIELD = ITEMS.register("guardian_shield", GuardianShieldItem::new);
   public static final RegistryObject<Item> DEVOTED_HEART = ITEMS.register("devoted_heart", DevotedHeartItem::new);
   public static final RegistryObject<Item> GUARDIAN_CORE = ITEMS.register("guardian_core", GuardianCoreItem::new);
   public static final RegistryObject<Item> LAODA_SHARD = ITEMS.register("laoda_shard", LaodaShardItem::new);
   public static final RegistryObject<Item> PROTECTION_CORE = ITEMS.register("protection_core", ProtectionCoreItem::new);
   public static final RegistryObject<Item> BLACK_HOLE_SCROLL = ITEMS.register("black_hole_scroll", BlackHoleScrollItem::new);
   public static final RegistryObject<Item> ENERGY_CORE = ITEMS.register("energy_core", EnergyCoreItem::new);
   public static final RegistryObject<Item> HOVER_CANNON = ITEMS.register("hover_cannon", HoverCannonItem::new);
   public static final RegistryObject<Item> TELEPORT_WORMHOLE = ITEMS.register("teleport_wormhole", TeleportWormholeItem::new);
   public static final RegistryObject<Item> MAID_ALTAR_SPAWNER = ITEMS.register("maid_altar_spawner", MaidAltarSpawnerItem::new);
   public static final RegistryObject<Item> MOSSY_PUPPET_SPAWN_EGG = ITEMS.register(
      "mossy_puppet_spawn_egg", () -> new MossyPuppetSpawnEggItem(ModEntities.MOSSY_PUPPET, new Properties())
   );
   public static final RegistryObject<Item> RELIC_WARDEN_SPAWN_EGG = ITEMS.register(
      "relic_warden_spawn_egg", () -> new RelicWardenSpawnEggItem(ModEntities.RELIC_WARDEN, new Properties())
   );
   public static final RegistryObject<Item> COORDINATE_DISPLAY = ITEMS.register(
      "coordinate_display", () -> new CoordinateDisplayItem(new Properties().stacksTo(1))
   );
   public static final RegistryObject<Item> RELIC_CONTROL_CORE = ITEMS.register(
      "relic_control_core", () -> new RelicControlCoreItem((RelicControlCoreBlock)ModBlocks.RELIC_CONTROL_CORE.get(), new Properties())
   );
   public static final RegistryObject<Item> RELIC_EYE = ITEMS.register("relic_eye", RelicEyeItem::new);

   public static void register(IEventBus bus) {
      ITEMS.register(bus);
   }
}

