package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class RosterSweep {
   private static final int LIST_LIMIT = 8;

   private RosterSweep() {
   }

   public static void run(ServerPlayer player) {
      MinecraftServer server = player.getServer();
      if (server != null) {
         int found = 0;
         List<String> listed = new ArrayList<>();

         for (ServerLevel level : server.getAllLevels()) {
            List<Entity> ghosts = new ArrayList<>();
            Vanisher.scanTracked(level, entity -> {
               if (!(entity instanceof Player) && !(entity instanceof EntityMaid)) {
                  if (!Vanisher.inRoster(level, entity)) {
                     ghosts.add(entity);
                  }
               }
            });

            for (Entity ghost : ghosts) {
               if (listed.size() < 8) {
                  listed.add(ghost.getType().toShortString() + " @" + ghost.getBlockX() + "," + ghost.getBlockY() + "," + ghost.getBlockZ());
               }

               found++;
               Vanisher.vanish(ghost, false);
            }
         }

         player.displayClientMessage(Component.literal(message(found, listed)), false);
      }
   }

   private static String message(int found, List<String> listed) {
      if (found == 0) {
         return "名册干净，没有残留的实体";
      }

      StringBuilder sb = new StringBuilder("清掉 ").append(found).append(" 只残留的实体：");
      sb.append(String.join("、", listed));
      if (found > listed.size()) {
         sb.append(" …");
      }

      return sb.toString();
   }
}

