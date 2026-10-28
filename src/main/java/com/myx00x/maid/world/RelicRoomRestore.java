package com.myx00x.maid.world;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class RelicRoomRestore {
   private static final String ROOT = "restore_boss_room_blocks";
   private static final String SOURCE = "神临";
   private static final String ROOM = "覆苔地下遗迹大房间";

   private RelicRoomRestore() {
   }

   @SubscribeEvent
   public static void onRegisterCommands(RegisterCommandsEvent event) {
      event.getDispatcher()
         .register(
            (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("restore_boss_room_blocks").requires(source -> source.hasPermission(2)))
               .then(Commands.literal("神临").then(Commands.literal("覆苔地下遗迹大房间").executes(RelicRoomRestore::run)))
         );
   }

   private static int run(CommandContext<CommandSourceStack> context) {
      CommandSourceStack source = (CommandSourceStack)context.getSource();

      ServerPlayer player;
      try {
         player = source.getPlayerOrException();
      } catch (CommandSyntaxException e) {
         source.sendFailure(Component.literal("这条命令得由站在遗迹里的玩家来打"));
         return 0;
      }

      BlockPos core = RelicOpening.restoreNear(player);
      if (core == null) {
         source.sendFailure(Component.literal("你眼下不在一片覆苔地下遗迹里"));
         return 0;
      } else {
         source.sendSuccess(() -> Component.literal("boss 房已还原成刚生成的样子，入场动画也重置了"), true);
         return 1;
      }
   }
}

