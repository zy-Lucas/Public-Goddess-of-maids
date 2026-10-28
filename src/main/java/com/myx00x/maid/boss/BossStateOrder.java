package com.myx00x.maid.boss;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class BossStateOrder {
   private BossStateOrder() {
   }

   @SubscribeEvent
   public static void onRegisterCommands(RegisterCommandsEvent event) {
      event.getDispatcher()
         .register(
            (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("shenlin").requires(source -> source.hasPermission(2)))
               .then(Commands.literal("boss").executes(BossStateOrder::run))
         );
   }

   private static int run(CommandContext<CommandSourceStack> context) {
      CommandSourceStack source = (CommandSourceStack)context.getSource();

      for (String line : ShenlinBossKeeper.report(source.getServer())) {
         source.sendSuccess(() -> Component.literal(line), false);
      }

      return 1;
   }
}

