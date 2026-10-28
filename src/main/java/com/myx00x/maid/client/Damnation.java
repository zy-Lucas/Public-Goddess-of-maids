package com.myx00x.maid.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.myx00x.maid.network.ModPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class Damnation {
   private static volatile boolean active;
   private static volatile boolean pending;
   private static volatile String who = "";
   private static float lockYaw;
   private static float lockPitch;
   private static double lockX;
   private static double lockY;
   private static double lockZ;

   private Damnation() {
   }

   public static void begin(String cause) {
      Minecraft mc = Minecraft.getInstance();
      who = cause == null ? "" : cause;
      active = true;
      pending = true;
      latch(mc.player);
      if (!(mc.screen instanceof DamnationScreen)) {
         mc.setScreen(new DamnationScreen());
      }
   }

   private static void latch(Player player) {
      if (pending && player != null) {
         lockYaw = player.getYRot();
         lockPitch = player.getXRot();
         lockX = player.getX();
         lockY = player.getY();
         lockZ = player.getZ();
         pending = false;
      }
   }

   public static boolean active() {
      return active;
   }

   public static String who() {
      return who;
   }

   public static void confess() {
      ModPackets.confessDamnation();
      clear();
   }

   public static void clear() {
      active = false;
      pending = false;
      Minecraft mc = Minecraft.getInstance();
      if (mc.screen instanceof DamnationScreen) {
         mc.setScreen(null);
      }
   }

   public static void hold(Player player) {
      if (active && player != null) {
         latch(player);
         if (!pending) {
            player.setDeltaMovement(0.0, 0.0, 0.0);
            player.setYRot(lockYaw);
            player.setXRot(lockPitch);
            player.yRotO = lockYaw;
            player.xRotO = lockPitch;
            if (player.distanceToSqr(lockX, lockY, lockZ) > 1.0E-4) {
               player.setPos(lockX, lockY, lockZ);
            }

            player.setSprinting(false);
            player.setShiftKeyDown(false);
            player.stopUsingItem();
            Minecraft mc = Minecraft.getInstance();
            if (!(mc.screen instanceof DamnationScreen)) {
               mc.setScreen(new DamnationScreen());
            }
         }
      }
   }

   public static void paint(Minecraft mc) {
      if (active && mc.font != null && !(mc.screen instanceof DamnationScreen)) {
         latch(mc.player);
         int width = mc.getWindow().getGuiScaledWidth();
         int height = mc.getWindow().getGuiScaledHeight();
         RenderSystem.disableDepthTest();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

         try {
            GuiGraphics graphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
            graphics.fill(0, 0, width, height, -268435456);
            int center = height / 2;
            graphics.drawCenteredString(mc.font, Component.translatable("gui.goddess_of_maids.damned"), width / 2, center - 30, -53714);
            graphics.drawCenteredString(mc.font, Component.translatable("gui.goddess_of_maids.damned_cause", new Object[]{who}), width / 2, center - 6, -5197648);
            graphics.flush();
         } catch (Throwable var5) {
         }

         RenderSystem.enableDepthTest();
      }
   }
}

