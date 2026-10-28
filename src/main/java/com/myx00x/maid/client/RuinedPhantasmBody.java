package com.myx00x.maid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT, bus = Bus.FORGE)
public final class RuinedPhantasmBody {
   private static final double SHOVE_BASE = 0.05;
   private static final double SHOVE_MAX = 0.04;

   private RuinedPhantasmBody() {
   }

   @SubscribeEvent
   public static void onLoggingOut(LoggingOut event) {
      try {
         RuinedPhantasmState.clear();
      } catch (Throwable var2) {
      }
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.END) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level != null && mc.player != null && !mc.isPaused()) {
            Player player = mc.player;
            if (!player.isSpectator() && !player.isPassenger() && !player.isDeadOrDying()) {
               EntityDimensions size = ((EntityType)ModEntities.RUINED_PHANTOM.get()).getDimensions();
               if (size.width > 0.0F && size.height > 0.0F) {
                  for (int id : RuinedPhantasmState.liveIds()) {
                     double[] spot = RuinedPhantasmState.spotStale(id);
                     if (spot != null) {
                        pushFrom(player, spot, size);
                     }
                  }
               }
            }
         }
      }
   }

   private static void pushFrom(Player player, double[] spot, EntityDimensions size) {
      try {
         AABB crate = crateOf(spot, size);
         AABB body = player.getBoundingBox();
         if (!body.intersects(crate)) {
            return;
         }

         double overlapX = Math.min(body.maxX - crate.minX, crate.maxX - body.minX);
         double overlapZ = Math.min(body.maxZ - crate.minZ, crate.maxZ - body.minZ);
         if (overlapX <= 0.0 || overlapZ <= 0.0) {
            return;
         }

         boolean alongX = overlapX < overlapZ;
         double depth = alongX ? overlapX : overlapZ;
         double cx = (body.minX + body.maxX) * 0.5;
         double cz = (body.minZ + body.maxZ) * 0.5;
         double sign = alongX ? (cx < spot[0] ? -1.0 : 1.0) : (cz < spot[2] ? -1.0 : 1.0);
         double shove = 0.05 + Math.min(0.04, depth * 0.15);
         if (alongX) {
            player.push(sign * shove, 0.0, 0.0);
         } else {
            player.push(0.0, 0.0, sign * shove);
         }
      } catch (Throwable var20) {
      }
   }

   private static AABB crateOf(double[] spot, EntityDimensions size) {
      double half = size.width * 0.5;
      return new AABB(spot[0] - half, spot[1], spot[2] - half, spot[0] + half, spot[1] + size.height, spot[2] + half);
   }

   @SubscribeEvent
   public static void onRenderLevel(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_ENTITIES) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level != null && mc.getEntityRenderDispatcher() != null && mc.getEntityRenderDispatcher().shouldRenderHitBoxes()) {
            EntityDimensions size = ((EntityType)ModEntities.RUINED_PHANTOM.get()).getDimensions();
            if (size.width > 0.0F && size.height > 0.0F) {
               Vec3 camera = SafeCamera.pos(mc);
               PoseStack pose = event.getPoseStack();

               for (int id : RuinedPhantasmState.liveIds()) {
                  double[] spot = RuinedPhantasmState.spotStale(id);
                  if (spot != null) {
                     drawOne(mc, pose, camera, spot, size);
                  }
               }
            }
         }
      }
   }

   private static void drawOne(Minecraft mc, PoseStack pose, Vec3 camera, double[] spot, EntityDimensions size) {
      pose.pushPose();

      try {
         pose.translate(spot[0] - camera.x, spot[1] - camera.y, spot[2] - camera.z);
         AABB box = new AABB(-size.width * 0.5, 0.0, -size.width * 0.5, size.width * 0.5, size.height, size.width * 0.5);
         VertexConsumer lines = mc.renderBuffers().bufferSource().getBuffer(RenderType.lines());
         LevelRenderer.renderLineBox(pose, lines, box, 1.0F, 1.0F, 1.0F, 1.0F);
         double eyeHeight = size.height * 0.85;
         LevelRenderer.renderLineBox(
            pose, lines, box.minX, eyeHeight - 0.01, box.minZ, box.maxX, eyeHeight + 0.01, box.maxZ, 1.0F, 0.0F, 0.0F, 1.0F
         );
         Vec3 look = viewVector(spot[4], spot[3]);
         LevelRenderer.renderLineBox(
            pose, lines, 0.0, eyeHeight, 0.0, look.x * 2.0, eyeHeight + look.y * 2.0, look.z * 2.0, 0.0F, 0.0F, 1.0F, 1.0F
         );
         mc.renderBuffers().bufferSource().endBatch(RenderType.lines());
      } catch (Throwable t) {
         ShenlinMod.LOGGER.debug("[shenlin] ruined phantasm hitbox draw failed: {}", t.toString());
      } finally {
         pose.popPose();
      }
   }

   private static Vec3 viewVector(double pitch, double yaw) {
      double yawRad = Math.toRadians(-yaw) - Math.PI;
      double pitchRad = Math.toRadians(-pitch);
      return new Vec3(Math.sin(yawRad) * -Math.cos(pitchRad), Math.sin(pitchRad), Math.cos(yawRad) * -Math.cos(pitchRad));
   }
}

