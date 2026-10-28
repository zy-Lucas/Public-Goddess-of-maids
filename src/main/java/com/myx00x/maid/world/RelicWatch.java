package com.myx00x.maid.world;

import com.myx00x.maid.Config;
import com.myx00x.maid.network.ModPackets;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.Structure;

public final class RelicWatch {
   private static final int PERIOD = 10;
   private static final ResourceKey<Structure> RELIC = ResourceKey.create(
      Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath("goddess_of_maids", "mossy_relic")
   );
   private static final Map<ServerPlayer, Boolean> INSIDE = new WeakHashMap<>();

   private RelicWatch() {
   }

   public static void tick(MinecraftServer server) {
      if (server != null && server.getTickCount() % 10 == 0) {
         for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            boolean now = inside(player);
            Boolean was = INSIDE.put(player, now);
            if (was == null || was != now) {
               ModPackets.sendRelicMusic(player, now, Config.relicMusicGapMin(), Config.relicMusicGapMax(), Config.relicWardenMusicRange());
            }
         }
      }
   }

   private static boolean inside(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      return level.structureManager().getStructureWithPieceAt(player.blockPosition(), RELIC).isValid();
   }
}

