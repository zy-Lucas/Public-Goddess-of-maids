package com.myx00x.maid.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.myx00x.maid.ShenlinMod;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
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
public final class ShockWarpEffect {
   private static final int SEGMENTS = 32;
   private static final int RINGS = 16;
   private static final int MAX_ALIVE = 8;
   private static final float SHELL_FACTOR = 1.0F;
   private static final float HOLLOW_SPREAD = 1.5F;
   private static final float RING_MAX = 0.6F;
   private static final float BEND = 1.24F;
   private static final float RING_CORE_GE = 0.33F;
   private static final float GROW_FROM = 0.12F;
   private static final float NEAR_KEEP = 1.15F;
   private static final float[] GROW_SPEED = new float[]{3.0F, 0.5F, 0.2F};
   private static final float[] GROW_TICKS = new float[]{3.0F, 3.0F, 41.0F};
   private static final float GROW_TOTAL = 17.3F;
   private static final float[] WHITE = new float[]{1.0F, 1.0F, 1.0F};
   private static final List<ShockWarpEffect.Ring> RINGS_ALIVE = new ArrayList<>();
   private static long lastStageLog;
   private static long lastVoidLog;
   private static long lastHandLog;
   private static long lastLensLog;
   private static boolean loggedLens;
   private static int lastRoute = -1;
   private static long lastGlErrLog;
   private static long lastDepthLog;
   private static TextureTarget sceneCopy;
   private static int copyWidth = -1;
   private static int copyHeight = -1;

   private ShockWarpEffect() {
   }

   public static void spawn(double x, double y, double z, float radius, float fastRadius, int fastMs, int slowMs, int kind) {
      spawn(x, y, z, radius, fastRadius, fastMs, slowMs, kind, false);
   }

   public static void spawn(double x, double y, double z, float radius, float fastRadius, int fastMs, int slowMs, int kind, boolean flat) {
      spawn(x, y, z, radius, fastRadius, fastMs, slowMs, kind, flat, false);
   }

   public static void spawn(double x, double y, double z, float radius, float fastRadius, int fastMs, int slowMs, int kind, boolean flat, boolean hollow) {
      RINGS_ALIVE.add(
         new ShockWarpEffect.Ring(x, y, z, Math.max(0.2F, radius), Math.max(0.2F, fastRadius), Math.max(1, fastMs), Math.max(1, slowMs), kind, flat, hollow)
      );

      while (RINGS_ALIVE.size() > 8) {
         RINGS_ALIVE.remove(0);
      }

      ShenlinMod.LOGGER
         .info(
            "[shenlin] warp born radius={} fast={} ms={}/{} kind={} flat={} hollow={} live={} lens={} clock={}/{}",
            new Object[]{
               radius,
               fastRadius,
               fastMs,
               slowMs,
               kind,
               flat,
               hollow,
               RINGS_ALIVE.size(),
               ModShaders.shockWarp() != null ? "on" : "off",
               EffectClock.now(),
               Util.getMillis()
            }
         );
   }

   public static int count() {
      return RINGS_ALIVE.size();
   }

   public static boolean pending() {
      return !RINGS_ALIVE.isEmpty();
   }

   public static void dropAll() {
      RINGS_ALIVE.clear();
   }

   public static void advanceAll(long now) {
      Iterator<ShockWarpEffect.Ring> it = RINGS_ALIVE.iterator();

      while (it.hasNext()) {
         ShockWarpEffect.Ring ring = it.next();
         ring.advance(now);
         if (ring.progress() >= 1.0F) {
            it.remove();
            ShenlinMod.LOGGER
               .info("[shenlin] warp drop live={} elapsed={} clock={}/{}", new Object[]{RINGS_ALIVE.size(), ring.elapsed(), now, Util.getMillis()});
         }
      }
   }

   public static boolean anyVisible() {
      for (ShockWarpEffect.Ring ring : RINGS_ALIVE) {
         if (ring.strength() > 0.0F) {
            return true;
         }
      }

      return false;
   }

   @SubscribeEvent
   public static void onRenderStage(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_PARTICLES) {
         long now = EffectClock.now();
         if (!EffectChannel.handPassAlive(now)) {
            if (!RINGS_ALIVE.isEmpty()) {
               try {
                  EffectHands.capture(Minecraft.getInstance(), now);
                  int verts = paintAtCapture(event.getPoseStack(), event.getCamera(), now, ShaderPack.inUse());
                  if (verts > 0 && now - lastStageLog > 1000L) {
                     lastStageLog = now;
                     ShenlinMod.LOGGER.info("[shenlin] warp stage verts={} live={}", verts, RINGS_ALIVE.size());
                  }
               } catch (Throwable t) {
                  RINGS_ALIVE.clear();
                  ShenlinMod.LOGGER.warn("[shenlin] shock warp stage draw failed", t);
               }
            }
         }
      }
   }

   public static int paintAtCapture(PoseStack poseStack, Camera camera, long now, boolean compat) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.player != null && !RINGS_ALIVE.isEmpty()) {
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
            ShenlinMod.LOGGER.info("[shenlin] warp frame pass verts={} live={}", verts, RINGS_ALIVE.size());
         }

         return verts;
      } else {
         return 0;
      }
   }

   public static int paint(Vec3 camera, Minecraft mc, long now, boolean compat, Matrix4f view) {
      if (mc.level != null && mc.player != null) {
         ShaderInstance shader = ModShaders.shockWarp();
         if (shader == null) {
            ModShaders.missing("shock_warp");
            return 0;
         }

         int route = compat ? 1 : 0;
         if (route != lastRoute) {
            lastRoute = route;
            ShenlinMod.LOGGER.info("[shenlin] warp paint compat={}", compat);
         }

         if (!anyVisible()) {
            return 0;
         }

         BufferSource buffers = mc.renderBuffers().bufferSource();
         RenderTarget source = EffectHands.clean(now);
         if (source == null && compat) {
            return 0;
         }

         if (source == null) {
            source = copyScene(mc);
         }

         RenderSystem.setShader(ModShaders::shockWarp);
         boolean depthWasOn = GL11C.glIsEnabled(2929);
         RenderSystem.disableDepthTest();
         shader.setSampler("ScreenTexture", source);
         shader.apply();
         shader.safeGetUniform("Bend").set(1.24F);
         int verts = 0;
         float logRing = 0.0F;
         float logShell = 0.0F;
         float logAlpha = 0.0F;
         Iterator<ShockWarpEffect.Ring> it = RINGS_ALIVE.iterator();

         while (it.hasNext()) {
            ShockWarpEffect.Ring ring = it.next();
            if (ring.progress() >= 1.0F) {
               it.remove();
            } else {
               float alpha = ring.strength();
               if (!(alpha <= 0.0F)) {
                  float shell = ring.radius * ring.growRatio() * 1.0F;
                  float geo = shell * (ring.hollow ? 1.5F : 1.0F);
                  float ringAt = 0.6F * ring.growRatio();
                  double ox = ring.x - camera.x;
                  double oy = ring.y - camera.y;
                  double oz = ring.z - camera.z;
                  double keep = nearScale(ox, oy, oz, shell);
                  ox *= keep;
                  oy *= keep;
                  oz *= keep;
                  Vector3f center = new Vector3f((float)ox, (float)oy, (float)oz);
                  view.transformPosition(center);
                  if (!(center.z > -0.05F)) {
                     shader.safeGetUniform("Center").set(center);
                     shader.safeGetUniform("Radius").set(shell);
                     shader.safeGetUniform("Ring").set(1.0F);
                     shader.safeGetUniform("RingWidth").set(0.33F / Math.max(shell, 0.01F));
                     shader.safeGetUniform("Flat").set(ring.flat ? 1.0F : 0.0F);
                     shader.safeGetUniform("Hollow").set(ring.hollow ? 1.0F : 0.0F);
                     shader.safeGetUniform("Intensity").set(alpha);
                     shader.apply();
                     sphere(buffers.getBuffer(ModRenderTypes.SHOCK_WARP), ox, oy, oz, geo, WHITE, 1.0F, false);
                     verts += 3072;
                     buffers.endBatch(ModRenderTypes.SHOCK_WARP);
                     logRing = ringAt;
                     logShell = shell;
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
                  "[shenlin] warp live={} verts={} shell={} ring={} inten={} bend={} clock={}/{}",
                  new Object[]{RINGS_ALIVE.size(), verts, logShell, logRing, logAlpha, 1.24F, now, Util.getMillis()}
               );
         }

         if (verts > 0 && now - lastGlErrLog > 1000L) {
            lastGlErrLog = now;
            int glErr = GL11C.glGetError();
            if (glErr != 0) {
               ShenlinMod.LOGGER.warn("[shenlin] warp gl error {}", glErr);
            }
         }

         if (verts > 0 && now - lastDepthLog > 1000L) {
            lastDepthLog = now;
            ShenlinMod.LOGGER
               .info("[shenlin] warp depth test was {} (we turned it off) live={} verts={}", new Object[]{depthWasOn ? "ON" : "off", RINGS_ALIVE.size(), verts});
         }

         return verts;
      } else {
         if (!RINGS_ALIVE.isEmpty() && now - lastVoidLog > 1000L) {
            lastVoidLog = now;
            ShenlinMod.LOGGER.info("[shenlin] warp idle left={} who={}", RINGS_ALIVE.size(), EffectChannel.stampedAt(now) ? "frame" : "stage");
         }

         return 0;
      }
   }

   private static double nearScale(double ox, double oy, double oz, float shell) {
      double dist = Math.sqrt(ox * ox + oy * oy + oz * oz);
      double keep = shell * 1.15F;
      return 1.0;
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

   private static final class Ring {
      private final double x;
      private final double y;
      private final double z;
      private final float radius;
      private final float fastRadius;
      private final long fastMs;
      private final long slowMs;
      private final int kind;
      private final boolean flat;
      private final boolean hollow;
      private long seen = Long.MIN_VALUE;
      private long elapsed;

      private Ring(double x, double y, double z, float radius, float fastRadius, long fastMs, long slowMs, int kind, boolean flat, boolean hollow) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.radius = radius;
         this.fastRadius = fastRadius;
         this.fastMs = fastMs;
         this.slowMs = slowMs;
         this.kind = kind;
         this.flat = flat;
         this.hollow = hollow;
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
         return Math.min(1.0F, (float)this.elapsed / (float)(this.fastMs + this.slowMs));
      }

      private float growRatio() {
         if (this.kind == 1) {
            float tick = (float)this.elapsed / 50.0F;
            return Mth.clamp(taperWalk(tick) / 17.3F, 0.0F, 1.0F);
         }

         float t = Math.min(1.0F, (float)this.elapsed / (float)(this.fastMs + this.slowMs));
         float from = 0.12F * this.fastRadius;
         float whole = this.radius - from;
         if (whole <= 1.0E-4F) {
            return Math.min(1.0F, t);
         }

         float power = 2.0F;
         float shape = (this.fastRadius - from) / whole;
         float at = (float)this.fastMs / (float)(this.fastMs + this.slowMs);
         if (shape > 0.001F && shape < 0.999F && at > 0.001F && at < 0.999F) {
            float k = (float)(Math.log(1.0F - shape) / Math.log(1.0F - at));
            if (k > 0.2F && k < 20.0F) {
               power = k;
            }
         }

         float now = from + whole * (1.0F - (float)Math.pow(1.0F - t, power));
         return Mth.clamp(now / Math.max(this.radius, 1.0E-4F), 0.0F, 1.0F);
      }

      private static float taperWalk(float tick) {
         float v1 = ShockWarpEffect.GROW_SPEED[0];
         float v2 = ShockWarpEffect.GROW_SPEED[1];
         float v3 = ShockWarpEffect.GROW_SPEED[2];
         float t1 = ShockWarpEffect.GROW_TICKS[0];
         float t2 = ShockWarpEffect.GROW_TICKS[1];
         float t3 = ShockWarpEffect.GROW_TICKS[2];
         float hold1 = t1 - 1.0F;
         float hold2 = t2 - 1.0F;
         float s1 = v1 * hold1;
         float s2 = s1 + (v1 + v2) * 0.5F;
         float s3 = s2 + v2 * hold2;
         float s4 = s3 + (v2 + v3) * 0.5F;
         if (tick <= hold1) {
            return v1 * tick;
         } else if (tick <= t1) {
            float x = tick - hold1;
            return s1 + v1 * x - 0.5F * (v1 - v2) * x * x;
         } else if (tick <= t1 + hold2) {
            return s2 + v2 * (tick - t1);
         } else if (tick <= t1 + t2) {
            float x = tick - (t1 + hold2);
            return s3 + v2 * x - 0.5F * (v2 - v3) * x * x;
         } else {
            float tailTicks = t3;
            return s4 + v3 * Math.min(tick - (t1 + t2), tailTicks);
         }
      }

      private float strength() {
         long span = this.fastMs + this.slowMs;
         if (this.elapsed <= 0L || this.elapsed >= span) {
            return 0.0F;
         } else if (this.kind != 1 && this.kind != 2) {
            float p = (float)this.elapsed / (float)span;
            return (float)Math.pow(1.0F - p, 0.7);
         } else {
            return this.elapsed <= this.fastMs ? 1.0F : Math.max(0.0F, 1.0F - (float)(this.elapsed - this.fastMs) / (float)this.slowMs);
         }
      }

      private long elapsed() {
         return this.elapsed;
      }
   }
}

