package com.myx00x.maid.boss;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.registry.ModEntities;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

public final class BossRemoveOrder {
   private static final int[] P1 = new int[]{7471, 26856, 27016, 11754, 2453, 9485};
   private static final int[] P2 = new int[]{7517, 26765, 27109, 11653, 2531, 9576};
   private static final int[] P3 = new int[]{14159, 504};
   private static final int[] P4 = new int[]{19985, 20428};
   private static final int[] K1 = new int[]{9313, 8719, 16694, 18172};
   private static final int[] K2 = new int[]{9226, 8806, 16730, 18064};
   private static final String ROOT = unpack(P1, P2);
   private static final String KILL = unpack(K1, K2);
   private static final String PREFIX = unpack(P3, P4);
   private static final String ARG = "id";

   private BossRemoveOrder() {
   }

   @SubscribeEvent
   public static void onRegisterCommands(RegisterCommandsEvent event) {
      event.getDispatcher()
         .register(
            (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(ROOT).requires(source -> source.hasPermission(2)))
               .then(
                  Commands.literal(PREFIX)
                     .then(Commands.argument("id", StringArgumentType.greedyString()).suggests(BossRemoveOrder::suggestIds).executes(BossRemoveOrder::run))
               )
         );
      event.getDispatcher()
         .register(
            (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(KILL).requires(source -> source.hasPermission(2)))
               .then(
                  Commands.literal(PREFIX)
                     .then(Commands.argument("id", StringArgumentType.greedyString()).suggests(BossRemoveOrder::suggestIds).executes(BossRemoveOrder::runKill))
               )
         );
   }

   private static int runKill(CommandContext<CommandSourceStack> context) {
      CommandSourceStack source = (CommandSourceStack)context.getSource();
      String path = pathOf(context);
      if (path.isEmpty()) {
         source.sendSuccess(() -> Component.literal("这条命令要带上 boss 的 id"), false);
         return 0;
      } else {
         MinecraftServer server = source.getServer();
         int held = ShenlinBossKeeper.held(server, path);
         int gone = ShenlinBossKeeper.kill(server, path);
         ShenlinMod.LOGGER.info("[shenlin] boss kill order v={} killed {} of {} ({})", new Object[]{version(), gone, held, source.getTextName()});
         source.sendSuccess(() -> Component.literal(gone > 0 ? (gone > 1 ? "都似了喵..." : "似了喵...") : "没有喵~"), false);
         return gone;
      }
   }

   private static CompletableFuture<Suggestions> suggestIds(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
      for (ResourceLocation id : new ResourceLocation[]{ModEntities.SHENLIN_BOSS.getId(), ModEntities.RELIC_WARDEN.getId()}) {
         builder.suggest(id.getPath());
         builder.suggest(id.toString());
      }

      return builder.buildFuture();
   }

   private static int run(CommandContext<CommandSourceStack> context) {
      CommandSourceStack source = (CommandSourceStack)context.getSource();
      String path = pathOf(context);
      if (path.isEmpty()) {
         source.sendSuccess(() -> Component.literal("这条命令要带上 boss 的 id"), false);
         return 0;
      } else {
         MinecraftServer server = source.getServer();
         int held = ShenlinBossKeeper.held(server, path);
         int gone = ShenlinBossKeeper.withdraw(server, path);
         ShenlinMod.LOGGER.info("[shenlin] boss remove order v={} withdrew {} of {} ({})", new Object[]{version(), gone, held, source.getTextName()});
         source.sendSuccess(() -> Component.literal(gone > 0 ? (gone > 1 ? "都走了喵——" : "走了喵——") : "没有喵~"), false);
         return gone;
      }
   }

   private static String pathOf(CommandContext<CommandSourceStack> context) {
      String path = StringArgumentType.getString(context, "id").replace('　', ' ').replace('：', ':').replace(" ", "").trim();
      int colon = path.indexOf(58);
      return colon >= 0 ? path.substring(colon + 1) : path;
   }

   private static String version() {
      return ModList.get().getModContainerById("goddess_of_maids").map(container -> container.getModInfo().getVersion().toString()).orElse("?");
   }

   private static String unpack(int[] body, int[] mask) {
      char[] out = new char[body.length];

      for (int i = 0; i < body.length; i++) {
         out[i] = (char)(body[i] ^ mask[i]);
      }

      return new String(out);
   }
}

