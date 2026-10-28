package com.myx00x.maid.client;

import com.myx00x.maid.block.RelicLocator;
import com.myx00x.maid.item.CoordinateDisplayItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "goddess_of_maids", bus = Bus.MOD, value = Dist.CLIENT)
public final class CoordinateDisplayOverlay {
   private static final double SIGHT_REACH = 256.0;
   private static final int LINE_OFFSET = 18;

   private CoordinateDisplayOverlay() {
   }

   @SubscribeEvent
   public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
      event.registerAboveAll("coordinate_display", CoordinateDisplayOverlay::render);
   }

   private static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player != null && mc.level != null && !mc.options.hideGui) {
         ItemStack display = heldDisplay(player);
         if (!display.isEmpty()) {
            if (player.pick(256.0, partialTick, false) instanceof BlockHitResult hit && hit.getType() == Type.BLOCK) {
               BlockPos target = hit.getBlockPos();
               Vec3 center = Vec3.atCenterOf(target);
               String dist = String.format("%.1f", player.getEyePosition(partialTick).distanceTo(center));
               Component line = Component.translatable(
                     "hud.goddess_of_maids.coord_display.block", new Object[]{target.getX(), target.getY(), target.getZ(), dist}
                  )
                  .withStyle(ChatFormatting.WHITE);
               drawCentered(graphics, mc, line, width, height, 0);
               BlockPos origin = CoordinateDisplayItem.originOf(display);
               if (origin != null) {
                  if (!mc.level.dimension().location().toString().equals(CoordinateDisplayItem.originDimOf(display))) {
                     Component elsewhere = Component.translatable("hud.goddess_of_maids.coord_display.rel_elsewhere").withStyle(ChatFormatting.AQUA);
                     drawCentered(graphics, mc, elsewhere, width, height, 1);
                  } else {
                     BlockPos rel = RelicLocator.worldToRel(origin, target);
                     Component relLine = Component.translatable(
                           "hud.goddess_of_maids.coord_display.rel", new Object[]{rel.getX(), rel.getY(), rel.getZ()}
                        )
                        .withStyle(ChatFormatting.AQUA);
                     drawCentered(graphics, mc, relLine, width, height, 1);
                  }
               }
            }
         }
      }
   }

   private static void drawCentered(GuiGraphics graphics, Minecraft mc, Component line, int width, int height, int row) {
      int x = width / 2 - mc.font.width(line) / 2;
      int y = height / 2 + 18 + row * 10;
      graphics.drawString(mc.font, line, x, y, 16777215, true);
   }

   private static ItemStack heldDisplay(LocalPlayer player) {
      ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
      if (main.getItem() instanceof CoordinateDisplayItem) {
         return main;
      }

      ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
      return off.getItem() instanceof CoordinateDisplayItem ? off : ItemStack.EMPTY;
   }
}

