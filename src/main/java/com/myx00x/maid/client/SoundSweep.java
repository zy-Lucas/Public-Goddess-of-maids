package com.myx00x.maid.client;

import com.myx00x.maid.ShenlinMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT, bus = Bus.FORGE)
public final class SoundSweep {
   private SoundSweep() {
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.END) {
         try {
            SoloBeamSound.sweep();
         } catch (Throwable t) {
            ShenlinMod.LOGGER.warn("[shenlin] solo beam sound sweep failed: {}", t.toString());
         }

         try {
            SpinRaySound.sweep();
         } catch (Throwable t) {
            ShenlinMod.LOGGER.warn("[shenlin] spin ray sound sweep failed: {}", t.toString());
         }

         try {
            FormRaySound.sweep();
         } catch (Throwable t) {
            ShenlinMod.LOGGER.warn("[shenlin] form ray sound sweep failed: {}", t.toString());
         }
      }
   }
}

