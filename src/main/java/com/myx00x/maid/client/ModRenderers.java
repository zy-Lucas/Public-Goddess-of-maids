package com.myx00x.maid.client;

import com.myx00x.maid.registry.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "goddess_of_maids", bus = Bus.MOD, value = Dist.CLIENT)
public final class ModRenderers {
   private ModRenderers() {
   }

   @SubscribeEvent
   public static void onRegisterRenderers(RegisterRenderers event) {
      event.registerEntityRenderer((EntityType)ModEntities.SHENLIN_BOSS.get(), BossRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.RUINED_PHANTOM.get(), RuinedPhantasmRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.MOSSY_PUPPET.get(), MossyPuppetRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.RELIC_WARDEN.get(), RelicWardenRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.RELIC_DEBRIS.get(), RelicDebrisRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.MOSSY_PEBBLE.get(), MossyPebbleRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.WARDEN_BEAM.get(), WardenBeamRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.WARDEN_LASER.get(), WardenLaserBeamRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.WARDEN_BOLT.get(), WardenEnergyBoltRenderer::new);
   }

   @SubscribeEvent
   public static void onRegisterLayerDefinitions(RegisterLayerDefinitions event) {
      event.registerLayerDefinition(MossyPuppetModel.LAYER, MossyPuppetModel::createBodyLayer);
      event.registerLayerDefinition(RelicWardenModel.LAYER, RelicWardenModel::createBodyLayer);
   }
}

