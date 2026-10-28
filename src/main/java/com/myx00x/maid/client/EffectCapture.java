package com.myx00x.maid.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT, bus = Bus.FORGE)
public final class EffectCapture {
   private static final Matrix4f PROJECTION = new Matrix4f();
   private static final Matrix4f VIEW = new Matrix4f();
   private static long stampedAt = Long.MIN_VALUE;
   private static final long FRESH_MS = 500L;
   private static final Matrix4f LAST_MV = new Matrix4f();
   private static final Matrix4f LAST_PROJ = new Matrix4f();

   private EffectCapture() {
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onStage(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_PARTICLES) {
         PROJECTION.set(RenderSystem.getProjectionMatrix());
         VIEW.set(event.getPoseStack().last().pose());
         stampedAt = EffectClock.now();
      }
   }

   public static boolean ready() {
      return stampedAt != Long.MIN_VALUE && EffectClock.now() - stampedAt <= 500L;
   }

   public static Matrix4f projection() {
      return new Matrix4f(PROJECTION);
   }

   public static Matrix4f view() {
      return new Matrix4f(VIEW);
   }

   public static Matrix4f viewForCameraRelative() {
      Matrix4f m = new Matrix4f(VIEW);
      m.m30(0.0F);
      m.m31(0.0F);
      m.m32(0.0F);
      return m;
   }

   public static void beginHandPass(PoseStack poseStack) {
      RenderSystem.backupProjectionMatrix();
      PoseStack mv = RenderSystem.getModelViewStack();
      mv.pushPose();
      if (ready()) {
         RenderSystem.setProjectionMatrix(projection(), VertexSorting.DISTANCE_TO_ORIGIN);
         mv.last().pose().set(viewForCameraRelative());
      } else {
         mv.last().pose().set(poseStack.last().pose());
      }

      RenderSystem.applyModelViewMatrix();
   }

   public static void endHandPass() {
      PoseStack mv = RenderSystem.getModelViewStack();
      mv.popPose();
      RenderSystem.applyModelViewMatrix();
      RenderSystem.restoreProjectionMatrix();
   }

   public static float viewTranslateX() {
      return VIEW.m30();
   }

   public static float viewTranslateY() {
      return VIEW.m31();
   }

   public static float viewTranslateZ() {
      return VIEW.m32();
   }

   public static float[] projectToScreen(double x, double y, double z) {
      Vector4f v = new Vector4f((float)x, (float)y, (float)z, 1.0F);
      LAST_MV.transform(v);
      LAST_PROJ.transform(v);
      return v.w <= 1.0E-4F ? null : new float[]{(v.x / v.w + 1.0F) * 0.5F, (1.0F - v.y / v.w) * 0.5F};
   }

   public static String lastModelViewDump() {
      return String.format(
         "%.3f %.3f %.3f %.3f | %.3f %.3f %.3f %.3f | %.3f %.3f %.3f %.3f",
         LAST_MV.m00(),
         LAST_MV.m01(),
         LAST_MV.m02(),
         LAST_MV.m03(),
         LAST_MV.m10(),
         LAST_MV.m11(),
         LAST_MV.m12(),
         LAST_MV.m13(),
         LAST_MV.m20(),
         LAST_MV.m21(),
         LAST_MV.m22(),
         LAST_MV.m23()
      );
   }

   public static boolean pushSure(RenderLevelStageEvent event) {
      RenderSystem.backupProjectionMatrix();
      PoseStack mv = RenderSystem.getModelViewStack();
      mv.pushPose();
      RenderSystem.setProjectionMatrix(projection(), VertexSorting.DISTANCE_TO_ORIGIN);
      mv.last().pose().set(viewForCameraRelative());
      RenderSystem.applyModelViewMatrix();
      LAST_MV.set(mv.last().pose());
      LAST_PROJ.set(projection());
      return ready();
   }

   public static void popSure() {
      RenderSystem.getModelViewStack().popPose();
      RenderSystem.applyModelViewMatrix();
      RenderSystem.restoreProjectionMatrix();
   }
}

