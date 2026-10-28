package com.myx00x.maid.world;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.myx00x.maid.ShenlinMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "goddess_of_maids", bus = Bus.FORGE)
public final class RelicClaimReset {
   private RelicClaimReset() {
   }

   @SubscribeEvent
   public static void onRegisterCommands(RegisterCommandsEvent event) {
      event.getDispatcher()
         .register(
            (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("shenlin_relic_reset").requires(source -> source.hasPermission(2))).executes(ctx -> {
               int freed = 0;

               for (ServerLevel level : ((CommandSourceStack)ctx.getSource()).getServer().getAllLevels()) {
                  freed += RelicSites.resetClaimed(level);
               }

               int total = freed;
               ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("已放开 " + total + " 座遗迹的认领"), true);
               ShenlinMod.LOGGER.info("[shenlin] relic claim reset: {} freed", total);
               return 1;
            })
         );
   }
}

