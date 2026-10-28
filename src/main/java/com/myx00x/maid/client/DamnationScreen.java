package com.myx00x.maid.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class DamnationScreen extends Screen {
   public DamnationScreen() {
      super(Component.translatable("gui.goddess_of_maids.damned"));
   }

   protected void init() {
      int centerX = this.width / 2;
      int y = this.height / 2 + 48;
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.goddess_of_maids.damned_sorry"), button -> Damnation.confess())
            .bounds(centerX - 156, y, 150, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.goddess_of_maids.damned_quit"), button -> Minecraft.getInstance().stop())
            .bounds(centerX + 6, y, 150, 20)
            .build()
      );
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      graphics.fill(0, 0, this.width, this.height, -268435456);
      int band = Math.max(1, this.height / 8);
      graphics.fill(0, 0, this.width, band, -15597568);
      graphics.fill(0, this.height - band, this.width, this.height, -15597568);
      int centerY = this.height / 2;
      graphics.drawCenteredString(this.font, Component.translatable("gui.goddess_of_maids.damned"), this.width / 2, centerY - 62, -53714);
      graphics.drawCenteredString(
         this.font, Component.translatable("gui.goddess_of_maids.damned_cause", new Object[]{Damnation.who()}), this.width / 2, centerY - 34, -5197648
      );
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   public boolean isPauseScreen() {
      return false;
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }
}

