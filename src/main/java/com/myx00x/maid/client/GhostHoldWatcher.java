package com.myx00x.maid.client;

import com.myx00x.maid.item.CoordinateDisplayItem;
import com.myx00x.maid.network.ModPackets;
import com.myx00x.maid.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT, bus = Bus.FORGE)
public final class GhostHoldWatcher {
   private static final float SINK_DOWN_MS = 150.0F;
   private static final float SINK_UP_MS = 50.0F;
   private static final float SINK_FULL = 0.999F;
   private static boolean on;
   private static ClientLevel boundLevel;
   private static long sinkSince = -1L;
   private static float sinkFrom;
   private static float sinkGoal;

   private GhostHoldWatcher() {
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.END) {
         Minecraft minecraft = Minecraft.getInstance();
         LocalPlayer player = minecraft.player;
         if (player == null) {
            clear(player);
         } else {
            if (minecraft.level != boundLevel) {
               boundLevel = minecraft.level;
               clear(player);
            }

            boolean holding = minecraft.options.keyUse.isDown() && shenlin.holdsDisplay(player);
            float goal = holding ? 1.0F : 0.0F;
            if (goal != sinkGoal) {
               sinkFrom = sinkAmount();
               sinkSince = System.currentTimeMillis();
               sinkGoal = goal;
            }

            if (holding) {
               if (!on && sinkAmount() >= 0.999F) {
                  on = true;
                  CoordinateDisplayItem.setGhost(player, true);
                  ModPackets.sendGhost(true);
               }
            } else {
               clear(player);
            }
         }
      }
   }

   private static void clear(LocalPlayer player) {
      if (on) {
         on = false;
         if (player != null) {
            CoordinateDisplayItem.setGhost(player, false);
            ModPackets.sendGhost(false);
         }
      }
   }

   public static float sinkAmount() {
      if (sinkSince < 0L) {
         return 0.0F;
      }

      float span = sinkGoal > sinkFrom ? 150.0F : 50.0F;
      float t = (float)(System.currentTimeMillis() - sinkSince) / span;
      if (t >= 1.0F) {
         return sinkGoal;
      }

      if (t <= 0.0F) {
         return sinkFrom;
      }

      float eased = 1.0F - (1.0F - t) * (1.0F - t);
      return sinkFrom + (sinkGoal - sinkFrom) * eased;
   }

   private static boolean shenlin.holdsDisplay(LocalPlayer player) {
      ItemStack main = player.getMainHandItem();
      ItemStack off = player.getOffhandItem();
      return main.is((Item)ModItems.COORDINATE_DISPLAY.get()) || off.is((Item)ModItems.COORDINATE_DISPLAY.get());
   }
}

