package com.myx00x.maid;

import com.mojang.logging.LogUtils;
import com.myx00x.maid.boss.BossRemoveOrder;
import com.myx00x.maid.boss.BossStateOrder;
import com.myx00x.maid.boss.ForgeListAddon;
import com.myx00x.maid.combat.SupplyOrder;
import com.myx00x.maid.compat.MixinCheck;
import com.myx00x.maid.compat.Upstream;
import com.myx00x.maid.event.BaubleSeal;
import com.myx00x.maid.event.DamnationHandler;
import com.myx00x.maid.event.HateListener;
import com.myx00x.maid.event.HeartGuardListener;
import com.myx00x.maid.event.MaidAttackListener;
import com.myx00x.maid.event.MaidDeathProtection;
import com.myx00x.maid.event.SealGate;
import com.myx00x.maid.event.ServerTickHandler;
import com.myx00x.maid.event.StallWatchdog;
import com.myx00x.maid.event.StarterGiftHandler;
import com.myx00x.maid.event.WorldResetHandler;
import com.myx00x.maid.mob.MossyPuppetEntity;
import com.myx00x.maid.mob.RelicWardenEntity;
import com.myx00x.maid.network.ModPackets;
import com.myx00x.maid.registry.ModBlocks;
import com.myx00x.maid.registry.ModCreativeTabs;
import com.myx00x.maid.registry.ModEntities;
import com.myx00x.maid.registry.ModItems;
import com.myx00x.maid.registry.ModLootModifiers;
import com.myx00x.maid.registry.ModSounds;
import com.myx00x.maid.registry.ModStructures;
import com.myx00x.maid.world.RelicRoomRestore;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod("goddess_of_maids")
public class ShenlinMod {
   public static final String MODID = "goddess_of_maids";
   public static final Logger LOGGER = LogUtils.getLogger();

   public ShenlinMod(FMLJavaModLoadingContext context) {
      IEventBus modBus = context.getModEventBus();
      LOGGER.info("[shenlin] upstream maid={} spell={}", Upstream.MAID, Upstream.SPELL);
      ModBlocks.register(modBus);
      ModItems.register(modBus);
      ModEntities.register(modBus);
      ModCreativeTabs.register(modBus);
      ModSounds.register(modBus);
      ModLootModifiers.register(modBus);
      ModStructures.register(modBus);
      context.registerConfig(Type.COMMON, Config.SPEC);
      modBus.addListener(this::onConfigLoaded);
      modBus.addListener(this::onEntityAttributes);
      listen(BaubleSeal.class);
      listen(MaidAttackListener.class);
      listen(ServerTickHandler.class, false);
      listen(MaidDeathProtection.class);
      listen(HeartGuardListener.class);
      listen(HateListener.class);
      listen(SupplyOrder.class, false);
      listen(BossRemoveOrder.class, false);
      listen(BossStateOrder.class, false);
      listen(ForgeListAddon.class, false);
      listen(RelicRoomRestore.class, false);
      listen(SealGate.class);
      listen(StarterGiftHandler.class);
      listen(DamnationHandler.class);
      listen(WorldResetHandler.class, false);
      listen(StallWatchdog.class, false);
      StallWatchdog.start();
      modBus.addListener(this::onCommonSetup);
   }

   private static void listen(Class<?> holder, boolean maidOnly) {
      if (!maidOnly || Upstream.MAID) {
         MinecraftForge.EVENT_BUS.register(holder);
      }
   }

   private static void listen(Class<?> holder) {
      listen(holder, true);
   }

   private void onCommonSetup(FMLCommonSetupEvent event) {
      ModPackets.register();
      event.enqueueWork(() -> {
         LOGGER.info("[shenlin] boss type={}", ModEntities.SHENLIN_BOSS.getId());
         MixinCheck.report();
         DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> MixinCheck::reportClient);
      });
   }

   private void onEntityAttributes(EntityAttributeCreationEvent event) {
      event.put((EntityType)ModEntities.MOSSY_PUPPET.get(), MossyPuppetEntity.createAttributes().build());
      event.put((EntityType)ModEntities.RELIC_WARDEN.get(), RelicWardenEntity.createAttributes().build());
   }

   private void onConfigLoaded(ModConfigEvent event) {
      Config.refresh();
   }
}

