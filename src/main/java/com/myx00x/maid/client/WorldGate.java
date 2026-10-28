package com.myx00x.maid.client;

import net.minecraft.client.Minecraft;

public final class WorldGate {
   private static final int SETTLE_TICKS = 40;

   private WorldGate() {
   }

   public static boolean ready() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null || mc.player == null) {
         return false;
      } else {
         return mc.getOverlay() != null ? false : mc.player.tickCount >= 40;
      }
   }
}

