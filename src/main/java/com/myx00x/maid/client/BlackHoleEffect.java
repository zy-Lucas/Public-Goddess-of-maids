package com.myx00x.maid.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11C;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT, bus = Bus.FORGE)
public final class BlackHoleEffect {
   private static final int SEGMENTS = 32;
   private static final int RINGS = 16;
   private static final int MAX_ALIVE = 32;
   private static final long FADE_IN_MS = 617L;
   private static final float FADE_OUT = 0.25F;
   private static final float BEND_FULL = 0.3F;
   private static final float SHELL_FACTOR = 1.35F;
   private static final float NEAR_KEEP = 1.5F;
   private static final float HORIZON_RATIO = 0.16666667F;
   private static final long EXEC_APPEAR_MS = 350L;
   private static final long EXEC_GROW_MS = 300L;
   private static final long EXEC_HOLD_MS = 550L;
   private static final long EXEC_TOTAL_MS = 5184L;
   private static final long EXEC_SHRINK_MS = 3984L;
   private static final long EXEC_SPAN_MS = 5184L;
   private static final long DISMISS_MS = 600L;
   private static final float[] HORIZON_RGB = new float[]{0.0F, 0.0F, 0.0F};
   private static final float[] HALO_RGB = new float[]{0.3F, 0.1F, 0.58F};
   private static final float[] WHITE = new float[]{1.0F, 1.0F, 1.0F};
   private static final List<BlackHoleEffect.Hole> HOLES = new ArrayList<>();
   private static long lastStageLog;
   private static long lastVoidLog;
   private static long lastHandLog;
   private static long lastLensLog;
   private static boolean loggedLens;
   private static int lastRoute = -1;
   private static long lastGlErrLog;
   private static TextureTarget sceneCopy;
   private static int copyWidth = -1;
   private static int copyHeight = -1;

   private BlackHoleEffect() {
   }

   public static void spawn(double x, double y, double z, float size, float warp, float reach, int ticks, boolean execution) {
      long span = execution ? 5184L : Math.max(1, ticks) * 50L;
      double reachSqr = (double)reach * reach;
      int bumped = 0;

      for (BlackHoleEffect.Hole hole : HOLES) {
         if ((execution || !hole.execution) && hole.distanceSqr(x, y, z) < reachSqr) {
            if (!hole.dismissed) {
               bumped++;
            }

            hole.dismiss();
            HoleSounds.fadeNear(hole.x, hole.y, hole.z, 0.5F);
         }
      }

      float drawSize = Math.max(0.05F, size);
      float drawWarp = Math.max(0.05F, warp);
      HOLES.add(new BlackHoleEffect.Hole(x, y, z, drawSize, drawWarp, span, execution));

      while (HOLES.size() > 32) {
         HOLES.remove(0);
      }

      ShenlinMod.LOGGER
         .info(
            "[shenlin] hole born size={} warp={} reach={} span={} exec={} live={} bumped={} lens={} clock={}/{}",
            new Object[]{
               drawSize,
               drawWarp,
               reach,
               span,
               execution,
               HOLES.size(),
               bumped,
               ModShaders.blackHole() != null ? "on" : "off",
               EffectClock.now(),
               Util.getMillis()
            }
         );
   }

   public static int count() {
      return HOLES.size();
   }

   public static boolean pending() {
      return !HOLES.isEmpty();
   }

   public static void dropAll() {
      HOLES.clear();
   }

   public static void advanceAll(long now) {
      Iterator<BlackHoleEffect.Hole> it = HOLES.iterator();

      while (it.hasNext()) {
         BlackHoleEffect.Hole hole = it.next();
         hole.advance(now);
         if (hole.progress() >= 1.0F) {
            it.remove();
            ShenlinMod.LOGGER
               .info(
                  "[shenlin] hole drop live={} elapsed={} exec={} clock={}/{}",
                  new Object[]{HOLES.size(), hole.elapsed(), hole.execution, now, Util.getMillis()}
               );
         }
      }
   }

   public static boolean anyVisible() {
      for (BlackHoleEffect.Hole hole : HOLES) {
         if (hole.scale() > 0.0F) {
            return true;
         }
      }

      return false;
   }

   public static void clearNear(double x, double y, double z, float radius) {
      double sq = (double)radius * radius;

      for (BlackHoleEffect.Hole hole : HOLES) {
         if (hole.distanceSqr(x, y, z) < sq) {
            hole.dismiss();
         }
      }
   }

   @SubscribeEvent
   public static void onRenderStage(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_PARTICLES) {
         long now = EffectClock.now();
         if (!EffectChannel.handPassAlive(now)) {
            if (!HOLES.isEmpty()) {
               try {
                  EffectHands.capture(Minecraft.getInstance(), now);
                  int verts = paintAtCapture(event.getPoseStack(), event.getCamera(), now, ShaderPack.inUse());
                  if (verts > 0 && now - lastStageLog > 1000L) {
                     lastStageLog = now;
                     ShenlinMod.LOGGER.info("[shenlin] hole stage verts={} live={}", verts, HOLES.size());
                  }
               } catch (Throwable t) {
                  HOLES.clear();
                  ShenlinMod.LOGGER.warn("[shenlin] black hole stage draw failed", t);
               }
            }
         }
      }
   }

   public static int paintAtCapture(PoseStack poseStack, Camera camera, long now, boolean compat) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.player != null && !HOLES.isEmpty()) {
         boolean captured = EffectCapture.ready();
         advanceAll(now);
         Matrix4f view = captured ? EffectCapture.view() : poseStack.last().pose();
         RenderSystem.backupProjectionMatrix();
         PoseStack modelView = RenderSystem.getModelViewStack();
         modelView.pushPose();
         if (captured) {
            RenderSystem.setProjectionMatrix(EffectCapture.projection(), VertexSorting.DISTANCE_TO_ORIGIN);
            modelView.last().pose().set(EffectCapture.viewForCameraRelative());
         } else {
            modelView.last().pose().set(poseStack.last().pose());
         }

         RenderSystem.applyModelViewMatrix();

         int verts;
         try {
            verts = paint(camera.getPosition(), mc, now, compat, view);
         } finally {
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
         }

         if (verts > 0 && now - lastHandLog > 1000L) {
            lastHandLog = now;
            ShenlinMod.LOGGER.info("[shenlin] hole frame pass verts={} live={}", verts, HOLES.size());
         }

         return verts;
      } else {
         return 0;
      }
   }

   public static int paint(Vec3 camera, Minecraft mc, long now, boolean compat, Matrix4f view) {
      if (mc.level != null && mc.player != null) {
         ShaderInstance lensShader = ModShaders.blackHole();
         boolean lens = lensShader != null;
         if (!lens) {
            ModShaders.missing("black_hole");
         }

         BufferSource buffers = mc.renderBuffers().bufferSource();
         int route = compat ? 1 : 0;
         if (route != lastRoute) {
            lastRoute = route;
            ShenlinMod.LOGGER.info("[shenlin] hole paint compat={}", compat);
         }

         RenderType solidType = compat ? ModRenderTypes.GLOW_ALPHA : ModRenderTypes.SLASH_RAY;
         if (!lens) {
            return drawGlass(buffers, solidType, camera, now, compat);
         }

         if (!anyVisible()) {
            return 0;
         }

         RenderTarget source = EffectHands.clean(now);
         if (source == null && compat) {
            return 0;
         }

         if (source == null) {
            source = copyScene(mc);
         }

         RenderSystem.setShader(ModShaders::blackHole);
         lensShader.setSampler("ScreenTexture", source);
         lensShader.apply();
         return drawLens(lensShader, buffers, camera, now, view);
      } else {
         if (!HOLES.isEmpty() && now - lastVoidLog > 1000L) {
            lastVoidLog = now;
            ShenlinMod.LOGGER.info("[shenlin] hole idle left={} who={}", HOLES.size(), EffectChannel.stampedAt(now) ? "frame" : "stage");
         }

         return 0;
      }
   }

   private static int drawLens(ShaderInstance shader, BufferSource buffers, Vec3 camera, long now, Matrix4f view) {
      shader.safeGetUniform("Bend").set(0.3F);
      shader.safeGetUniform("EdgeBand").set(((Double)Config.BLACK_HOLE_EDGE_BAND.get()).floatValue());
      int verts = 0;
      float logWarp = 0.0F;
      float logHorizon = 0.0F;
      float logAlpha = 0.0F;
      Iterator<BlackHoleEffect.Hole> it = HOLES.iterator();

      while (it.hasNext()) {
         BlackHoleEffect.Hole hole = it.next();
         if (hole.progress() >= 1.0F) {
            it.remove();
         } else {
            float scale = hole.scale();
            if (!(scale <= 0.0F)) {
               float warp = hole.warp * scale;
               double ox = hole.x - camera.x;
               double oy = hole.y - camera.y;
               double oz = hole.z - camera.z;
               double keep = nearScale(ox, oy, oz, warp);
               ox *= keep;
               oy *= keep;
               oz *= keep;
               Vector3f center = new Vector3f((float)ox, (float)oy, (float)oz);
               view.transformPosition(center);
               if (!(center.z > -0.05F)) {
                  float horizon = Math.min(0.16666667F, 0.98F);
                  float alpha = hole.alpha();
                  shader.safeGetUniform("Center").set(center);
                  shader.safeGetUniform("Radius").set(warp);
                  shader.safeGetUniform("Horizon").set(horizon);
                  shader.safeGetUniform("Intensity").set(alpha);
                  shader.apply();
                  sphere(buffers.getBuffer(ModRenderTypes.BLACK_HOLE), ox, oy, oz, warp * 1.35F, WHITE, 1.0F, false);
                  verts += 3072;
                  buffers.endBatch(ModRenderTypes.BLACK_HOLE);
                  logWarp = warp;
                  logHorizon = horizon;
                  logAlpha = alpha;
               }
            }
         }
      }

      if (verts > 0 && (!loggedLens || now - lastLensLog > 1000L)) {
         loggedLens = true;
         lastLensLog = now;
         ShenlinMod.LOGGER
            .info(
               "[shenlin] hole lens live={} verts={} warp={} horizon={} inten={} bend={} clock={}/{}",
               new Object[]{HOLES.size(), verts, logWarp, logHorizon, logAlpha, 0.3F, now, Util.getMillis()}
            );
      }

      if (verts > 0 && now - lastGlErrLog > 1000L) {
         lastGlErrLog = now;
         int glErr = GL11C.glGetError();
         if (glErr != 0) {
            ShenlinMod.LOGGER.warn("[shenlin] hole lens gl error {}", glErr);
         }
      }

      return verts;
   }

   private static int drawGlass(BufferSource buffers, RenderType type, Vec3 camera, long now, boolean compat) {
      if (!compat) {
         if (ModShaders.slashRay() == null) {
            ModShaders.missing("slash_ray");
            return 0;
         }

         RenderSystem.setShader(ModShaders::slashRay);
      }

      int verts = 0;
      Iterator<BlackHoleEffect.Hole> it = HOLES.iterator();

      while (it.hasNext()) {
         BlackHoleEffect.Hole hole = it.next();
         float progress = hole.progress();
         if (progress >= 1.0F) {
            it.remove();
         } else {
            float scale = hole.scale();
            if (!(scale <= 0.0F)) {
               float warp = hole.warp * scale;
               float size = hole.size * scale;
               float alpha = hole.alpha();
               double ox = hole.x - camera.x;
               double oy = hole.y - camera.y;
               double oz = hole.z - camera.z;
               double keep = nearScale(ox, oy, oz, warp);
               ox *= keep;
               oy *= keep;
               oz *= keep;
               sphere(buffers.getBuffer(type), ox, oy, oz, warp, HALO_RGB, alpha * 0.1F, compat);
               verts += 3072;
               buffers.endBatch(type);
               sphere(buffers.getBuffer(type), ox, oy, oz, size, HORIZON_RGB, alpha, compat);
               verts += 3072;
               buffers.endBatch(type);
            }
         }
      }

      return verts;
   }

   private static double nearScale(double ox, double oy, double oz, float warp) {
      double dist = Math.sqrt(ox * ox + oy * oy + oz * oz);
      double keep = warp * 1.5F;
      return dist > 1.0E-4 && dist < keep ? keep / dist : 1.0;
   }

   private static void sphere(VertexConsumer consumer, double ox, double oy, double oz, float scale, float[] rgb, float alpha, boolean compat) {
      float pi = (float) Math.PI;

      for (int ring = 0; ring < 16; ring++) {
         float theta1 = ring * pi / 16.0F;
         float theta2 = (ring + 1) * pi / 16.0F;
         float sin1 = Mth.sin(theta1);
         float sin2 = Mth.sin(theta2);
         float y1 = Mth.cos(theta1);
         float y2 = Mth.cos(theta2);

         for (int segment = 0; segment < 32; segment++) {
            float phi1 = segment * 2.0F * pi / 32.0F;
            float phi2 = (segment + 1) * 2.0F * pi / 32.0F;
            float x1 = sin1 * Mth.cos(phi1);
            float z1 = sin1 * Mth.sin(phi1);
            float x2 = sin1 * Mth.cos(phi2);
            float z2 = sin1 * Mth.sin(phi2);
            float x3 = sin2 * Mth.cos(phi2);
            float z3 = sin2 * Mth.sin(phi2);
            float x4 = sin2 * Mth.cos(phi1);
            float z4 = sin2 * Mth.sin(phi1);
            vertex(consumer, ox, oy, oz, rgb, alpha, x1 * scale, y1 * scale, z1 * scale, compat);
            vertex(consumer, ox, oy, oz, rgb, alpha, x2 * scale, y1 * scale, z2 * scale, compat);
            vertex(consumer, ox, oy, oz, rgb, alpha, x3 * scale, y2 * scale, z3 * scale, compat);
            vertex(consumer, ox, oy, oz, rgb, alpha, x1 * scale, y1 * scale, z1 * scale, compat);
            vertex(consumer, ox, oy, oz, rgb, alpha, x3 * scale, y2 * scale, z3 * scale, compat);
            vertex(consumer, ox, oy, oz, rgb, alpha, x4 * scale, y2 * scale, z4 * scale, compat);
         }
      }
   }

   private static void vertex(VertexConsumer consumer, double ox, double oy, double oz, float[] rgb, float alpha, float x, float y, float z, boolean compat) {
      if (compat) {
         consumer.vertex(ox + x, oy + y, oz + z).color(rgb[0], rgb[1], rgb[2], alpha).endVertex();
      } else {
         consumer.vertex(ox + x, oy + y, oz + z)
            .color(rgb[0], rgb[1], rgb[2], alpha)
            .uv(0.0F, 0.0F)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(15728880)
            .normal(x, y, z)
            .endVertex();
      }
   }

   private static RenderTarget copyScene(Minecraft mc) {
      RenderTarget main = mc.getMainRenderTarget();
      if (sceneCopy == null || copyWidth != main.width || copyHeight != main.height) {
         if (sceneCopy != null) {
            sceneCopy.destroyBuffers();
         }

         sceneCopy = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
         copyWidth = main.width;
         copyHeight = main.height;
      }

      GlStateManager._glBindFramebuffer(36008, main.frameBufferId);
      GlStateManager._glBindFramebuffer(36009, sceneCopy.frameBufferId);

      try {
         GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, copyWidth, copyHeight, 16384, 9728);
      } finally {
         main.bindWrite(true);
      }

      return sceneCopy;
   }

   private static final class Hole {
      private final double x;
      private final double y;
      private final double z;
      private final float size;
      private final float warp;
      private long span;
      private final boolean execution;
      private boolean dismissed;
      private float dismissFrom;
      private long dismissStart;
      private long seen = Long.MIN_VALUE;
      private long elapsed;

      private Hole(double x, double y, double z, float size, float warp, long span, boolean execution) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.size = size;
         this.warp = warp;
         this.span = span;
         this.execution = execution;
      }

      private void dismiss() {
         if (!this.dismissed) {
            this.dismissed = true;
            this.dismissFrom = this.scale();
            this.dismissStart = this.elapsed;
            this.span = this.elapsed + 600L;
         }
      }

      private float dismissProgress() {
         if (!this.dismissed) {
            return 0.0F;
         }

         long over = this.elapsed - this.dismissStart;
         return Math.min(1.0F, Math.max(0.0F, (float)over / 600.0F));
      }

      private void advance(long now) {
         if (this.seen == Long.MIN_VALUE) {
            this.seen = now;
         } else {
            long step = now - this.seen;
            this.seen = now;
            this.elapsed += step >= 0L && step <= 2000L ? step : 50L;
         }
      }

      private float progress() {
         return Math.min(1.0F, (float)this.elapsed / (float)this.span);
      }

      private float scale() {
         if (this.dismissed) {
            return Math.max(0.0F, this.dismissFrom * (1.0F - this.dismissProgress()));
         }

         if (!this.execution) {
            return 1.0F;
         }

         if (this.elapsed < 350L) {
            return 0.0F;
         }

         long grown = this.elapsed - 350L;
         if (grown < 300L) {
            return (float)grown / 300.0F;
         }

         if (grown < 850L) {
            return 1.0F;
         }

         long shrinking = grown - 300L - 550L;
         return shrinking >= 3984L ? 0.0F : 1.0F - (float)shrinking / 3984.0F;
      }

      private float alpha() {
         if (this.dismissed) {
            return Math.max(0.0F, 1.0F - this.dismissProgress());
         }

         if (this.execution) {
            return 1.0F;
         }

         long rise = Math.min(617L, Math.max(1L, this.span * 6L / 10L));
         if (this.elapsed < rise) {
            return (float)this.elapsed / (float)rise;
         }

         float progress = Math.min(1.0F, (float)this.elapsed / (float)this.span);
         return progress > 0.75F ? (1.0F - progress) / 0.25F : 1.0F;
      }

      private double distanceSqr(double px, double py, double pz) {
         double dx = this.x - px;
         double dy = this.y - py;
         double dz = this.z - pz;
         return dx * dx + dy * dy + dz * dz;
      }

      private long elapsed() {
         return this.elapsed;
      }
   }
}

