package com.myx00x.maid.event;

import com.myx00x.maid.boss.ShenlinBossKeeper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class DamnationHandler {
   @SubscribeEvent
   public static void onLoggedIn(PlayerLoggedInEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         MinecraftServer server = player.server;
         server.tell(new TickTask(server.getTickCount() + 40, () -> {
            if (!player.hasDisconnected()) {
               ShenlinBossKeeper.rejoin(player);
            }
         }));
      }
   }
}

