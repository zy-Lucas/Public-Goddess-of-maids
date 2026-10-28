package com.myx00x.maid.client;

import com.myx00x.maid.mixin.MusicManagerAccessor;
import com.myx00x.maid.mixin.SoundVolumeAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT, bus = Bus.FORGE)
public final class OriginalMusicFader {
   private static final int FADE_TICKS = 30;
   private static final float FLOOR = 0.02F;
   private static final float STEP = 0.74F;
   private static SoundInstance target;
   private static int ticksLeft;

   private OriginalMusicFader() {
   }

   public static void request(Minecraft minecraft) {
      if (minecraft != null && minecraft.getMusicManager() != null) {
         MusicManager manager = minecraft.getMusicManager();
         SoundInstance current = ((MusicManagerAccessor)manager).shenlin.currentMusic();
         if (current != null && current != target) {
            target = current;
            ticksLeft = 30;
         }
      }
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.END) {
         Minecraft minecraft = Minecraft.getInstance();
         if (target != null && minecraft.getMusicManager() != null) {
            if (--ticksLeft <= 0) {
               minecraft.getMusicManager().stopPlaying();
               target = null;
            } else {
               float now = target.getVolume();
               float next = now * 0.74F;
               ((SoundVolumeAccessor)target).shenlin.setVolume(next <= 0.02F ? 0.02F : next);
            }
         }
      }
   }
}

