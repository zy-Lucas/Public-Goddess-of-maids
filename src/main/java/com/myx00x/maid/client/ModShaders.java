package com.myx00x.maid.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.myx00x.maid.ShenlinMod;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT, bus = Bus.MOD)
public final class ModShaders {
   private static final Set<String> MISSING = ConcurrentHashMap.newKeySet();
   private static ShaderInstance slashRay;
   private static ShaderInstance starCore;
   private static ShaderInstance blackHole;
   private static ShaderInstance shockWarp;
   private static ShaderInstance volumetricFog;

   private ModShaders() {
   }

   public static ShaderInstance slashRay() {
      return slashRay;
   }

   public static ShaderInstance starCore() {
      return starCore;
   }

   public static ShaderInstance blackHole() {
      return blackHole;
   }

   public static ShaderInstance volumetricFog() {
      return volumetricFog;
   }

   public static ShaderInstance shockWarp() {
      return shockWarp;
   }

   public static void missing(String name) {
      if (MISSING.add(name)) {
         ShenlinMod.LOGGER.warn("[shenlin] {} shader missing, effect hidden", name);
      }
   }

   @SubscribeEvent
   public static void onRegisterShaders(RegisterShadersEvent event) {
      register(event, "slash_ray", "slash ray", shader -> slashRay = shader);
      register(event, "star_core", "star core", shader -> starCore = shader);
      register(event, "black_hole", "black hole", shader -> blackHole = shader);
      register(event, "shock_warp", "shock warp", shader -> shockWarp = shader);
      register(event, "volumetric_fog", "volumetric fog", shader -> volumetricFog = shader);
   }

   private static void register(RegisterShadersEvent event, String name, String label, Consumer<ShaderInstance> sink) {
      try {
         event.registerShader(
            new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath("goddess_of_maids", name), DefaultVertexFormat.NEW_ENTITY),
            shader -> {
               sink.accept(shader);
               ShenlinMod.LOGGER.info("[shenlin] {} shader ready", label);
            }
         );
      } catch (Exception e) {
         ShenlinMod.LOGGER.warn("[shenlin] {} shader unavailable: {}", label, e.toString());
      }
   }
}

