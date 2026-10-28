package com.myx00x.maid.client;

import com.myx00x.maid.ShenlinMod;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "goddess_of_maids", bus = Bus.MOD, value = Dist.CLIENT)
public final class PostChainRebuild {
   private static volatile int epoch;

   private PostChainRebuild() {
   }

   public static int epoch() {
      return epoch;
   }

   @SubscribeEvent
   public static void onAddReloadListeners(RegisterClientReloadListenersEvent event) {
      event.registerReloadListener(new SimplePreparableReloadListener<Void>() {
         protected Void prepare(ResourceManager manager, ProfilerFiller profiler) {
            return null;
         }

         protected void apply(Void nothing, ResourceManager manager, ProfilerFiller profiler) {
            PostChainRebuild.epoch++;
            ShenlinMod.LOGGER.info("[shenlin] 资源重载，后处理链标记为待重建（第 {} 代）", PostChainRebuild.epoch);
         }
      });
   }
}

