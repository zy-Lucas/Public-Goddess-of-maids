package com.myx00x.maid.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.myx00x.maid.mob.RelicWardenEntity;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "goddess_of_maids", bus = Bus.MOD, value = Dist.CLIENT)
public final class WardenHealthBar {
   private static final ResourceLocation FRAME = ResourceLocation.fromNamespaceAndPath("goddess_of_maids", "textures/gui/warden_bar_frame.png");
   private static final ResourceLocation FILL = ResourceLocation.fromNamespaceAndPath("goddess_of_maids", "textures/gui/warden_bar_fill.png");
   private static final ResourceLocation NAME = ResourceLocation.fromNamespaceAndPath("goddess_of_maids", "textures/gui/warden_bar_name.png");
   private static final int FRAMES = 12;
   private static final int BAR_W = 512;
   private static final int BAR_H = 16;
   private static final int BAR_SHEET_H = 192;
   private static final int NAME_W = 128;
   private static final int NAME_H = 48;
   private static final int NAME_SHEET_H = 576;
   private static final float BAR_WIDE = 0.77F;
   private static final float NAME_WIDE = 0.1466F;
   private static final float NAME_TOP_GAP = 2.0F;
   private static final float NAME_BAR_GAP = 2.0F;
   private static final int NAME_PAD_TOP = 9;
   private static final int NAME_PAD_BOTTOM = 5;
   private static final long FRAME_MS = 83L;
   private static final int FILL_LEFT = 3;
   private static final int FILL_RIGHT = 508;
   private static final double SHOW_RANGE = 64.0;
   private static final float BOSS_BAR_TAIL = 2.0F;

   private WardenHealthBar() {
   }

   public static boolean visibleNow() {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player != null && mc.level != null && !mc.options.hideGui) {
         RelicWardenEntity warden = nearest(mc, player);
         return warden != null && !warden.isDormant();
      } else {
         return false;
      }
   }

   public static float shiftBelowPx(int width) {
      float nameScale = width * 0.1466F / 128.0F;
      float nameRow = 34.0F * nameScale;
      float barH = 16.0F * (width * 0.77F / 512.0F);
      return 2.0F + nameRow + 2.0F + barH + 2.0F;
   }

   @SubscribeEvent
   public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
      event.registerAboveAll("warden_health_bar", WardenHealthBar::render);
   }

   private static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player != null && mc.level != null && !mc.options.hideGui) {
         RelicWardenEntity warden = nearest(mc, player);
         if (warden != null) {
            if (!warden.isDormant()) {
               int frame = (int)(EffectClock.now() / 83L % 12L);
               int v = frame * 16;
               float scale = width * 0.77F / 512.0F;
               float nameScale = width * 0.1466F / 128.0F;
               float nameRow = 34.0F * nameScale;
               int barTop = Math.round(4.0F + nameRow);
               int nameTop = Math.round(2.0F - 9.0F * nameScale);
               float barLeft = (width - 512.0F * scale) * 0.5F;
               float nameLeft = (width - 128.0F * nameScale) * 0.5F;
               RenderSystem.enableBlend();
               RenderSystem.defaultBlendFunc();
               graphics.pose().pushPose();
               graphics.pose().translate(barLeft, barTop, 0.0F);
               graphics.pose().scale(scale, scale, 1.0F);
               graphics.blit(FRAME, 0, 0, 0.0F, v, 512, 16, 512, 192);
               float pct = Mth.clamp(warden.displayHealth() / Math.max(1.0F, warden.getMaxHealth()), 0.0F, 1.0F);
               int cut = Math.round(3.0F + 505.0F * pct);
               if (cut > 3) {
                  graphics.blit(FILL, 0, 0, 0.0F, v, cut, 16, 512, 192);
               }

               graphics.pose().popPose();
               graphics.pose().pushPose();
               graphics.pose().translate(nameLeft, nameTop, 0.0F);
               graphics.pose().scale(nameScale, nameScale, 1.0F);
               graphics.blit(NAME, 0, 0, 0.0F, frame * 48, 128, 48, 128, 576);
               graphics.pose().popPose();
               RenderSystem.disableBlend();
            }
         }
      }
   }

   private static RelicWardenEntity nearest(Minecraft mc, LocalPlayer player) {
      if (mc.level == null) {
         return null;
      }

      List<RelicWardenEntity> near = mc.level.getEntitiesOfClass(RelicWardenEntity.class, player.getBoundingBox().inflate(64.0));
      RelicWardenEntity best = null;
      double bestSqr = Double.MAX_VALUE;

      for (RelicWardenEntity warden : near) {
         if (warden.isAlive()) {
            double d = warden.distanceToSqr(player);
            if (d < bestSqr) {
               bestSqr = d;
               best = warden;
            }
         }
      }

      return best;
   }
}

