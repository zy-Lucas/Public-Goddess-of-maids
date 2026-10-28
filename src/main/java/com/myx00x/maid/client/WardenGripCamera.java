package com.myx00x.maid.client;

import com.myx00x.maid.mob.RelicWardenEntity;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT, bus = Bus.FORGE)
public final class WardenGripCamera {
   private static final double NEAR = 16.0;
   private static CameraType saved;
   private static boolean gripped;

   private WardenGripCamera() {
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.END) {
         Minecraft mc = Minecraft.getInstance();
         LocalPlayer player = mc.player;
         if (player != null && mc.level != null) {
            boolean now = gripped(mc, player);
            if (now && !gripped) {
               saved = mc.options.getCameraType();
               gripped = true;
            }

            if (now) {
               if (mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
                  mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
               }
            } else {
               if (gripped) {
                  restoreCamera(mc);
               }
            }
         } else {
            restoreCamera(mc);
         }
      }
   }

   private static boolean gripped(Minecraft mc, LocalPlayer player) {
      if (mc.level == null) {
         return false;
      }

      for (RelicWardenEntity warden : mc.level.getEntitiesOfClass(RelicWardenEntity.class, player.getBoundingBox().inflate(16.0))) {
         if (warden.chargeVictimId() == player.getId()) {
            return true;
         }
      }

      return false;
   }

   private static void restoreCamera(Minecraft mc) {
      if (saved != null) {
         mc.options.setCameraType(saved);
      }

      reset();
   }

   private static void reset() {
      gripped = false;
      saved = null;
   }
}

