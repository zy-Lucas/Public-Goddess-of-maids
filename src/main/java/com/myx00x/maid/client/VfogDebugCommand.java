package com.myx00x.maid.client;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.myx00x.maid.ShenlinMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT, bus = Bus.FORGE)
public final class VfogDebugCommand {
   private VfogDebugCommand() {
   }

   @SubscribeEvent
   public static void onRegister(RegisterClientCommandsEvent event) {
      event.getDispatcher()
         .register(
            (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("vfogdebug")
                        .then(Commands.literal("0").executes(ctx -> set((CommandSourceStack)ctx.getSource(), 0))))
                     .then(Commands.literal("1").executes(ctx -> set((CommandSourceStack)ctx.getSource(), 1))))
                  .then(Commands.literal("2").executes(ctx -> set((CommandSourceStack)ctx.getSource(), 2))))
               .then(Commands.literal("3").executes(ctx -> set((CommandSourceStack)ctx.getSource(), 3)))
         );
   }

   private static int set(CommandSourceStack source, int mode) {
      VolumetricFog.DEBUG_MODE = mode;

      String note = switch (mode) {
         case 1 -> "跳过雾，原样输出画面";
         case 2 -> "整屏纯红";
         case 3 -> "深度当灰度";
         default -> "正常画雾";
      };
      source.sendSuccess(() -> Component.literal("体积雾诊断 = " + mode + "（" + note + "）"), false);
      ShenlinMod.LOGGER.info("[shenlin] vfog debug mode -> {}", mode);
      return 1;
   }
}

