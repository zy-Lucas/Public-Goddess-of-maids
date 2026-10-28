package com.myx00x.maid.combat;

import com.myx00x.maid.ShenlinMod;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "goddess_of_maids", bus = Bus.FORGE)
public final class EraseWatchEvents {
   private EraseWatchEvents() {
   }

   @SubscribeEvent
   public static void onJoin(EntityJoinLevelEvent event) {
      if (!event.getLevel().isClientSide()) {
         try {
            EraseWatch.onEntityJoined(event.getEntity());
         } catch (Throwable t) {
            ShenlinMod.LOGGER.warn("[shenlin] erase watch join failed: {}", t.toString());
         }
      }
   }
}

