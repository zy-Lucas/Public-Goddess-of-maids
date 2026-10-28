package com.myx00x.maid.registry;

import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.boss.RuinedPhantasmEntity;
import com.myx00x.maid.boss.ShenlinBossEntity;
import com.myx00x.maid.mob.MossyPebbleEntity;
import com.myx00x.maid.mob.MossyPuppetEntity;
import com.myx00x.maid.mob.RelicDebrisEntity;
import com.myx00x.maid.mob.RelicWardenEntity;
import com.myx00x.maid.mob.WardenBeamEntity;
import com.myx00x.maid.mob.WardenEnergyBoltEntity;
import com.myx00x.maid.mob.WardenLaserBeamEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
   public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "goddess_of_maids");
   public static final RegistryObject<EntityType<ShenlinBossEntity>> SHENLIN_BOSS = ENTITIES.register(
      "dance_offerer", () -> Builder.of((type, level) -> {
         try {
            return new ShenlinBossEntity(type, level);
         } catch (Throwable t) {
            ShenlinMod.LOGGER.error("[shenlin] boss shell create failed", t);
            throw t;
         }
      }, MobCategory.MISC).sized(1.2F, 2.4F).clientTrackingRange(16).updateInterval(2).fireImmune().build("goddess_of_maids:dance_offerer")
   );
   public static final RegistryObject<EntityType<RuinedPhantasmEntity>> RUINED_PHANTOM = ENTITIES.register(
      "ruined_phantasm", () -> Builder.of((type, level) -> {
         try {
            return new RuinedPhantasmEntity(type, level);
         } catch (Throwable t) {
            ShenlinMod.LOGGER.error("[shenlin] ruined phantasm shell create failed", t);
            throw t;
         }
      }, MobCategory.MISC).sized(1.2F, 2.4F).clientTrackingRange(16).updateInterval(2).fireImmune().build("goddess_of_maids:ruined_phantasm")
   );
   public static final RegistryObject<EntityType<MossyPuppetEntity>> MOSSY_PUPPET = ENTITIES.register(
      "mossy_puppet",
      () -> Builder.of(MossyPuppetEntity::new, MobCategory.MONSTER)
         .sized(0.7F, 1.35F)
         .clientTrackingRange(10)
         .fireImmune()
         .build("goddess_of_maids:mossy_puppet")
   );
   public static final RegistryObject<EntityType<RelicWardenEntity>> RELIC_WARDEN = ENTITIES.register(
      "relic_warden",
      () -> Builder.of(RelicWardenEntity::new, MobCategory.MONSTER)
         .sized(2.8F, 3.4F)
         .clientTrackingRange(12)
         .fireImmune()
         .build("goddess_of_maids:relic_warden")
   );
   public static final RegistryObject<EntityType<MossyPebbleEntity>> MOSSY_PEBBLE = ENTITIES.register(
      "mossy_pebble",
      () -> Builder.of(MossyPebbleEntity::new, MobCategory.MISC)
         .sized(0.25F, 0.25F)
         .clientTrackingRange(6)
         .updateInterval(10)
         .build("goddess_of_maids:mossy_pebble")
   );
   public static final RegistryObject<EntityType<RelicDebrisEntity>> RELIC_DEBRIS = ENTITIES.register(
      "relic_debris",
      () -> Builder.of(RelicDebrisEntity::new, MobCategory.MISC)
         .sized(1.0F, 1.0F)
         .clientTrackingRange(10)
         .updateInterval(20)
         .fireImmune()
         .build("goddess_of_maids:relic_debris")
   );
   public static final RegistryObject<EntityType<WardenBeamEntity>> WARDEN_BEAM = ENTITIES.register(
      "warden_beam",
      () -> Builder.of(WardenBeamEntity::new, MobCategory.MISC)
         .sized(0.2F, 0.2F)
         .noSave()
         .clientTrackingRange(10)
         .updateInterval(1)
         .fireImmune()
         .build("goddess_of_maids:warden_beam")
   );
   public static final RegistryObject<EntityType<WardenLaserBeamEntity>> WARDEN_LASER = ENTITIES.register(
      "warden_laser",
      () -> Builder.of(WardenLaserBeamEntity::new, MobCategory.MISC)
         .sized(0.2F, 0.2F)
         .noSave()
         .clientTrackingRange(10)
         .updateInterval(1)
         .fireImmune()
         .build("goddess_of_maids:warden_laser")
   );
   public static final RegistryObject<EntityType<WardenEnergyBoltEntity>> WARDEN_BOLT = ENTITIES.register(
      "warden_bolt",
      () -> Builder.of(WardenEnergyBoltEntity::new, MobCategory.MISC)
         .sized(0.1F, 0.1F)
         .noSave()
         .clientTrackingRange(10)
         .updateInterval(1)
         .fireImmune()
         .build("goddess_of_maids:warden_bolt")
   );

   private ModEntities() {
   }

   public static void register(IEventBus bus) {
      ENTITIES.register(bus);
   }
}

