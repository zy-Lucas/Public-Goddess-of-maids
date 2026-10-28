package com.myx00x.maid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.boss.ShenlinBossEntity;
import com.myx00x.maid.registry.ModEntities;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT, bus = Bus.FORGE)
public final class ShellModelOverlay {
   private static final Map<Integer, ShenlinBossEntity> SHADOWS = new HashMap<>();
   private static long notedAt;

   private ShellModelOverlay() {
   }

   @SubscribeEvent
   public static void onRenderLevel(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_ENTITIES) {
         Minecraft minecraft = Minecraft.getInstance();
         Set<Integer> painted = BossShellGuard.takePainted();
         ClientLevel level = minecraft.level;
         if (level == null) {
            SHADOWS.clear();
         } else {
            List<Integer> live = BossShellGuard.liveIds();
            if (live.isEmpty()) {
               SHADOWS.clear();
            } else {
               SHADOWS.keySet().retainAll(live);

               for (int id : live) {
                  if (!painted.contains(id)) {
                     double[] spot = BossShellGuard.spotStale(id);
                     if (spot != null) {
                        paintOne(minecraft, level, id, spot, event);
                     }
                  }
               }
            }
         }
      }
   }

   public static ShenlinBossEntity shadowFor(int id, ClientLevel level) {
      ShenlinBossEntity shadow = SHADOWS.get(id);
      if (shadow == null || shadow.level() != level) {
         shadow = new ShenlinBossEntity((EntityType<? extends Entity>)ModEntities.SHENLIN_BOSS.get(), level);
         shadow.markShadow(true);
         SHADOWS.put(id, shadow);
      }

      return shadow;
   }

   private static void paintOne(Minecraft minecraft, ClientLevel level, int id, double[] spot, RenderLevelStageEvent event) {
      ShenlinBossEntity shadow = shadowFor(id, level);
      shadow.setId(id);
      float yaw = (float)spot[3];
      shadow.setPos(spot[0], spot[1], spot[2]);
      shadow.setYRot(yaw);
      shadow.setXRot(0.0F);
      shadow.yRotO = yaw;
      shadow.xRotO = 0.0F;
      shadow.tickCount = (int)(level.getGameTime() & 2147483647L);
      shadow.setOldPosAndRot();
      Vec3 camera = event.getCamera().getPosition();
      if (!(shadow.distanceToSqr(camera.x, camera.y, camera.z) > 4096.0)) {
         EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
         BufferSource buffer = minecraft.renderBuffers().bufferSource();
         PoseStack pose = event.getPoseStack();
         int light = LevelRenderer.getLightColor(level, BlockPos.containing(spot[0], spot[1], spot[2]));
         BossShellGuard.shadowPass(true);

         try {
            dispatcher.render(
               shadow,
               spot[0] - camera.x,
               spot[1] - camera.y,
               spot[2] - camera.z,
               (float)spot[3],
               event.getPartialTick(),
               pose,
               buffer,
               light
            );
            note("painted", id);
         } catch (Throwable t) {
            note("failed " + t, id);
         } finally {
            BossShellGuard.shadowPass(false);
         }

         buffer.endBatch();
      }
   }

   private static void note(String what, int id) {
      long now = System.currentTimeMillis();
      if (now - notedAt >= 3000L) {
         notedAt = now;
         ShenlinMod.LOGGER.info("[shenlin] model paint {} id={}", what, id);
      }
   }
}

