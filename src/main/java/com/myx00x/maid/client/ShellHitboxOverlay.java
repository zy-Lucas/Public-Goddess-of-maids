package com.myx00x.maid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.myx00x.maid.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT, bus = Bus.FORGE)
public final class ShellHitboxOverlay {
   private ShellHitboxOverlay() {
   }

   @SubscribeEvent
   public static void onRenderLevel(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_ENTITIES) {
         Minecraft minecraft = Minecraft.getInstance();
         if (minecraft.level != null && minecraft.getEntityRenderDispatcher() != null && minecraft.getEntityRenderDispatcher().shouldRenderHitBoxes()) {
            EntityDimensions size = ((EntityType)ModEntities.SHENLIN_BOSS.get()).getDimensions();
            if (size.width > 0.0F && size.height > 0.0F) {
               Vec3 camera = event.getCamera().getPosition();
               PoseStack pose = event.getPoseStack();

               for (int id : BossShellGuard.liveIds()) {
                  double[] spot = BossShellGuard.spotStale(id);
                  if (spot != null) {
                     drawOne(minecraft, pose, camera, spot, size);
                  }
               }
            }
         }
      }
   }

   private static void drawOne(Minecraft minecraft, PoseStack pose, Vec3 camera, double[] spot, EntityDimensions size) {
      pose.pushPose();
      pose.translate(spot[0] - camera.x, spot[1] - camera.y, spot[2] - camera.z);
      AABB box = new AABB(-size.width * 0.5, 0.0, -size.width * 0.5, size.width * 0.5, size.height, size.width * 0.5);

      try {
         VertexConsumer lines = minecraft.renderBuffers().bufferSource().getBuffer(RenderType.lines());
         LevelRenderer.renderLineBox(pose, lines, box, 1.0F, 1.0F, 1.0F, 1.0F);
         double eyeHeight = size.height * 0.85;
         LevelRenderer.renderLineBox(
            pose, lines, box.minX, eyeHeight - 0.01, box.minZ, box.maxX, eyeHeight + 0.01, box.maxZ, 1.0F, 0.0F, 0.0F, 1.0F
         );
         Vec3 look = viewVector(spot[4], spot[3]);
         LevelRenderer.renderLineBox(
            pose, lines, 0.0, eyeHeight, 0.0, look.x * 2.0, eyeHeight + look.y * 2.0, look.z * 2.0, 0.0F, 0.0F, 1.0F, 1.0F
         );
         minecraft.renderBuffers().bufferSource().endBatch(RenderType.lines());
      } catch (Throwable var10) {
      }

      pose.popPose();
   }

   private static Vec3 viewVector(double pitch, double yaw) {
      double f = Math.toRadians(pitch);
      double f1 = Math.toRadians(-yaw);
      double cos = Math.cos(f1);
      double sin = Math.sin(f1);
      double cosPitch = Math.cos(f);
      double sinPitch = Math.sin(f);
      return new Vec3(sin * cosPitch, -sinPitch, cos * cosPitch);
   }
}

