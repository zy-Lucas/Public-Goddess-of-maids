package com.myx00x.maid.client;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.myx00x.maid.FxTrace;
import com.myx00x.maid.network.ModPackets;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "goddess_of_maids", bus = Bus.FORGE)
public final class SpaceBreakCommand {
   private static final double BREAK_REACH = 6.0;
   private static final double PORTAL_REACH = 5.0;
   private static final int BREAK_TICKS = 400;
   private static final int PORTAL_TICKS = 1200;
   private static final double FOG_REACH = 8.0;
   private static final float FOG_RADIUS = 2.0F;
   private static final int FOG_TICKS = 120;
   private static final int FOG_JET_TICKS = 70;
   private static final double FOG_JET_SPEED = 7.0;

   private SpaceBreakCommand() {
   }

   @SubscribeEvent
   public static void onRegister(RegisterCommandsEvent event) {
      event.getDispatcher()
         .register(
            (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("shenlin").requires(source -> source.hasPermission(2)))
               .then(
                  ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("特效测试")
                              .then(Commands.literal("破碎").executes(ctx -> fire(ctx, 2, 6.0, 400))))
                           .then(Commands.literal("传送门").executes(ctx -> fire(ctx, 0, 5.0, 1200))))
                        .then(Commands.literal("体积雾2").executes(ctx -> vfog(ctx, 8.0, 2.0F, 120))))
                     .then(
                        ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("体积雾")
                                       .executes(ctx -> fog(ctx, 8.0, 2.0F, 120, 1.0F, 1.0F, 1.0F, 0)))
                                    .then(Commands.literal("扩散").executes(ctx -> fog(ctx, 8.0, 2.0F, 120, 1.0F, 1.0F, 1.0F, 1))))
                                 .then(Commands.literal("喷出").executes(ctx -> fog(ctx, 8.0, 2.0F, 70, 1.0F, 1.0F, 1.0F, 2))))
                              .then(Commands.literal("毒雾").executes(ctx -> fog(ctx, 8.0, 2.0F, 120, 0.42F, 0.92F, 0.36F, 0))))
                           .then(Commands.literal("血雾").executes(ctx -> fog(ctx, 8.0, 2.0F, 120, 0.86F, 0.16F, 0.2F, 0)))
                     )
               )
         );
   }

   private static int vfog(CommandContext<CommandSourceStack> ctx, double reach, float radius, int ticks) {
      ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayer();
      if (player == null) {
         return 0;
      }

      Vec3 eye = player.getEyePosition();
      Vec3 dir = player.getLookAngle();
      Vec3 at = eye.add(dir.scale(reach));
      ModPackets.sendVolumetricFog(player.serverLevel(), at, radius, 0.85F, ticks);
      ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("体积雾2（新架构）已放出"), false);
      return 1;
   }

   private static int fog(CommandContext<CommandSourceStack> ctx, double reach, float radius, int ticks, float r, float g, float b, int mode) {
      try {
         ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
         Vec3 look = player.getLookAngle();
         Vec3 spot = player.position().add(look.scale(reach));
         ModPackets.sendFog(player, spot.x, spot.y, spot.z, radius, r, g, b, ticks, mode, look.x, look.y, look.z, 7.0);
         FxTrace.fx(
            "volumeFog",
            "command",
            spot,
            System.currentTimeMillis(),
            "mode=" + modeName(mode) + " radius=" + FxTrace.f(radius) + " rgb=" + FxTrace.f(r) + "/" + FxTrace.f(g) + "/" + FxTrace.f(b) + " ticks=" + ticks
         );
         return 1;
      } catch (Exception e) {
         return 0;
      }
   }

   private static String modeName(int mode) {
      return switch (mode) {
         case 1 -> "spread";
         case 2 -> "jet";
         default -> "still";
      };
   }

   private static int fire(CommandContext<CommandSourceStack> ctx, int action, double reach, int ticks) {
      try {
         ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
         Vec3 look = player.getLookAngle();
         Vec3 spot = player.position().add(look.scale(reach));
         ModPackets.sendRelicDoor(player, action, spot.x, spot.y, spot.z, ticks);
         return 1;
      } catch (Exception e) {
         return 0;
      }
   }
}

