package com.myx00x.maid.boss;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.CommandNode;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.registry.ModEntities;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ForgeListAddon {
   private static Command<CommandSourceStack> installed;
   private static long notedAt;

   private ForgeListAddon() {
   }

   @SubscribeEvent
   public static void onServerStarted(ServerStartedEvent event) {
      install(event.getServer());
   }

   @SubscribeEvent
   public static void onServerTick(ServerTickEvent event) {
      if (event.phase == Phase.END) {
         MinecraftServer server = event.getServer();
         if (server.getTickCount() % 20 == 0) {
            CommandNode<CommandSourceStack> list = listNode(server);
            if (list != null && installed != null && list.getCommand() != installed) {
               ShenlinMod.LOGGER.info("[shenlin] forge entity list taken back");
               setCommand(list, installed);
            }
         }
      }
   }

   private static CommandNode<CommandSourceStack> listNode(MinecraftServer server) {
      CommandNode<CommandSourceStack> forge = server.getCommands().getDispatcher().getRoot().getChild("forge");
      if (forge == null) {
         return null;
      }

      CommandNode<CommandSourceStack> entity = forge.getChild("entity");
      return entity == null ? null : entity.getChild("list");
   }

   private static void install(MinecraftServer server) {
      try {
         CommandNode<CommandSourceStack> list = listNode(server);
         if (list == null) {
            return;
         }

         if (installed == null) {
            installed = context -> {
               report((CommandSourceStack)context.getSource());
               return 1;
            };
         }

         setCommand(list, installed);
         ShenlinMod.LOGGER.info("[shenlin] forge entity list taken over");
      } catch (Throwable t) {
         ShenlinMod.LOGGER.warn("[shenlin] forge entity list hook skipped: {}", t.toString());
      }
   }

   private static void setCommand(CommandNode<CommandSourceStack> node, Command<CommandSourceStack> command) {
      try {
         Field slot = CommandNode.class.getDeclaredField("command");
         slot.setAccessible(true);
         slot.set(node, command);
      } catch (Throwable t) {
         ShenlinMod.LOGGER.warn("[shenlin] forge entity list set failed: {}", t.toString());
      }
   }

   private static void report(CommandSourceStack source) {
      Map<String, Integer> counts = new LinkedHashMap<>();
      int total = 0;
      int seen = 0;

      for (ServerLevel level : source.getServer().getAllLevels()) {
         for (Entity entity : rosterOf(level)) {
            if (!entity.isRemoved()) {
               counts.merge(entity.getType().builtInRegistryHolder().key().location().toString(), 1, Integer::sum);
               total++;
            }
         }
      }

      String ours = ModEntities.SHENLIN_BOSS.getId().toString();
      seen = counts.getOrDefault(ours, 0);
      int guarded = ShenlinBossKeeper.guardedOnes(source.getServer()).size();
      note(total, seen, guarded);
      if (guarded != seen) {
         total += guarded - seen;
         if (guarded > 0) {
            counts.put(ours, guarded);
         } else {
            counts.remove(ours);
         }
      }

      int sum = total;
      source.sendSuccess(() -> Component.translatable("commands.forge.entity.list.multiple.header", new Object[]{sum}), false);

      for (Entry<String, Integer> e : counts.entrySet()) {
         String line = e.getValue() + ": " + e.getKey();
         source.sendSuccess(() -> Component.literal(line), false);
      }
   }

   private static void note(int total, int seen, int guarded) {
      long now = System.currentTimeMillis();
      if (now - notedAt >= 5000L) {
         notedAt = now;
         ShenlinMod.LOGGER.info("[shenlin] forge list total={} seen={} guarded={}", new Object[]{total, seen, guarded});
      }
   }

   private static List<Entity> rosterOf(ServerLevel level) {
      List<Entity> out = new ArrayList<>();

      try {
         Object getter = level.getEntities();
         Field inner = getter.getClass().getDeclaredField("f_156940_");
         inner.setAccessible(true);
         Object lookup = inner.get(getter);
         Field byId = lookup.getClass().getDeclaredField("f_156807_");
         byId.setAccessible(true);
         if (byId.get(lookup) instanceof Map<?, ?> raw) {
            for (Object v : raw.values()) {
               if (v instanceof Entity e && !e.isRemoved()) {
                  out.add(e);
               }
            }

            return out;
         }
      } catch (Throwable var10) {
      }

      level.getEntities().getAll().forEach(out::add);
      return out;
   }
}

