package com.myx00x.maid.client;

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
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT, bus = Bus.FORGE)
public final class ShockwaveEffect {
   private static final float START_RADIUS = 0.35F;
   private static final float FAST_SHARE = 0.5F;
   private static final long FAST_MS = 150L;
   private static final long SLOW_MS = 500L;
   private static final int SEGMENTS = 40;
   private static final double TAU = Math.PI * 2;
   private static final float BAND_RATIO = 0.16F;
   private static final float BAND_MIN = 0.09F;
   private static final float OUT_R = 0.88F;
   private static final float OUT_G = 0.97F;
   private static final float OUT_B = 1.0F;
   private static final float IN_R = 0.52F;
   private static final float IN_G = 0.84F;
   private static final float IN_B = 1.0F;
   private static final float IN_SHARE = 0.3F;
   private static final float[] COOL_OUT = new float[]{0.88F, 0.97F, 1.0F};
   private static final float[] COOL_IN = new float[]{0.52F, 0.84F, 1.0F};
   private static final float[] WARM_OUT = new float[]{1.0F, 0.4F, 0.28F};
   private static final float[] WARM_IN = new float[]{0.8F, 0.11F, 0.08F};
   private static final float[] VIOLET_OUT = new float[]{0.36F, 0.66F, 1.0F};
   private static final float[] VIOLET_IN = new float[]{0.08F, 0.3F, 0.95F};
   private static final int MAX_ALIVE = 48;
   private static final double TILT = 0.18;
   private static final float FLARE_SHARE = 0.85F;
   private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
   private static final Vec3 EAST = new Vec3(1.0, 0.0, 0.0);
   private static final RandomSource JITTER = RandomSource.create();
   private static final List<ShockwaveEffect.Wave> WAVES = new ArrayList<>();
   private static long lastStageLog;
   private static long lastVoidLog;
   private static long lastHandLog;

   private ShockwaveEffect() {
   }

   public static void spawn(double x, double y, double z, float radius, int ticks, float ax, float ay, float az) {
      spawn(x, y, z, radius, ticks, Math.max(0.35F, radius * 0.5F), 150L, 500L, ax, ay, az);
   }

   public static void spawn(double x, double y, double z, float radius, int ticks, float fastRadius, long fastMs, long slowMs, float ax, float ay, float az) {
      spawn(x, y, z, radius, ticks, fastRadius, fastMs, slowMs, ax, ay, az, false);
   }

   public static void spawn(
      double x, double y, double z, float radius, int ticks, float fastRadius, long fastMs, long slowMs, float ax, float ay, float az, boolean wild
   ) {
      spawn(x, y, z, radius, ticks, fastRadius, fastMs, slowMs, ax, ay, az, wild, false);
   }

   public static void spawn(
      double x,
      double y,
      double z,
      float radius,
      int ticks,
      float fastRadius,
      long fastMs,
      long slowMs,
      float ax,
      float ay,
      float az,
      boolean wild,
      boolean warm
   ) {
      spawn(x, y, z, radius, ticks, fastRadius, fastMs, slowMs, ax, ay, az, wild, warm, false);
   }

   public static void spawn(
      double x,
      double y,
      double z,
      float radius,
      int ticks,
      float fastRadius,
      long fastMs,
      long slowMs,
      float ax,
      float ay,
      float az,
      boolean wild,
      boolean warm,
      boolean violet
   ) {
      spawn(x, y, z, radius, ticks, fastRadius, fastMs, slowMs, ax, ay, az, wild, warm, violet, 1.0F);
   }

   public static void spawn(
      double x,
      double y,
      double z,
      float radius,
      int ticks,
      float fastRadius,
      long fastMs,
      long slowMs,
      float ax,
      float ay,
      float az,
      boolean wild,
      boolean warm,
      boolean violet,
      float flareScale
   ) {
      long span = Math.max(1, ticks) * 50L;
      WAVES.add(
         new ShockwaveEffect.Wave(
            x,
            y,
            z,
            Math.max(0.35F, radius),
            span,
            Math.max(0.35F, fastRadius),
            Math.max(1L, fastMs),
            Math.max(1L, slowMs),
            wild ? tiltAway(ax, ay, az) : new Vec3(ax, ay, az),
            wild,
            warm,
            violet,
            flareScale
         )
      );

      while (WAVES.size() > 48) {
         WAVES.remove(0);
      }

      ShenlinMod.LOGGER
         .info(
            "[shenlin] wave born span={} live={} ax={}/{} wild={} warm={} clock={}/{}",
            new Object[]{span, WAVES.size(), ax, ay, wild, warm, EffectClock.now(), Util.getMillis()}
         );
   }

   private static Vec3 tiltAway(float ax, float ay, float az) {
      Vec3 v = new Vec3(
         ax + (JITTER.nextDouble() * 2.0 - 1.0) * 0.18, ay + (JITTER.nextDouble() * 2.0 - 1.0) * 0.18, az + (JITTER.nextDouble() * 2.0 - 1.0) * 0.18
      );
      return v.lengthSqr() < 1.0E-9 ? UP : v.normalize();
   }

   public static void spawnPod(double x, double y, double z, float radius, int ticks, float ax, float ay, float az, int maidId, int pod) {
      Vec3 at = new Vec3(x, y, z);
      Vec3 mouth = OrbitCannonEffect.podWorld(maidId, pod);
      Vec3 axis = mouth == null ? null : mouth.subtract(at);
      if (axis != null && !(axis.lengthSqr() < 1.0E-6)) {
         ShenlinMod.LOGGER
            .info(
               "[shenlin] wave pod {} mouth {} {} {} at {} {} {} axis {} {} {}",
               new Object[]{
                  pod,
                  (float)mouth.x,
                  (float)mouth.y,
                  (float)mouth.z,
                  (float)x,
                  (float)y,
                  (float)z,
                  (float)axis.x,
                  (float)axis.y,
                  (float)axis.z
               }
            );
         spawn(x, y, z, radius, ticks, (float)axis.x, (float)axis.y, (float)axis.z);
      } else {
         ShenlinMod.LOGGER.info("[shenlin] wave pod {} no mouth, fallback axis {}/{}", new Object[]{pod, ax, ay});
         spawn(x, y, z, radius, ticks, ax, ay, az);
      }
   }

   @SubscribeEvent
   public static void onRenderStage(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_PARTICLES) {
         long now = EffectClock.now();
         if (!EffectChannel.handPassAlive(now)) {
            if (!WAVES.isEmpty()) {
               try {
                  int verts = paintAtCapture(event.getPoseStack(), event.getCamera(), now);
                  if (verts > 0 && now - lastStageLog > 1000L) {
                     lastStageLog = now;
                     ShenlinMod.LOGGER.info("[shenlin] wave stage verts={} live={}", verts, WAVES.size());
                  }
               } catch (Throwable t) {
                  WAVES.clear();
                  ShenlinMod.LOGGER.warn("[shenlin] shockwave draw failed: {}", t.toString());
               }
            }
         }
      }
   }

   public static boolean pending() {
      return !WAVES.isEmpty();
   }

   public static void dropAll() {
      WAVES.clear();
   }

   public static int paintAtCapture(PoseStack poseStack, Camera camera, long now) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.player != null && !WAVES.isEmpty()) {
         boolean captured = EffectCapture.ready();
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
            verts = paint(camera.getPosition(), new Vec3(camera.getLookVector()), now);
         } finally {
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
         }

         if (verts > 0 && now - lastHandLog > 1000L) {
            lastHandLog = now;
            ShenlinMod.LOGGER.info("[shenlin] wave hand pass verts={} live={}", verts, WAVES.size());
         }

         return verts;
      } else {
         return 0;
      }
   }

   public static int count() {
      return WAVES.size();
   }

   public static int paint(Vec3 camera, Vec3 look, long now) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.player != null) {
         BufferSource buffers = mc.renderBuffers().bufferSource();
         VertexConsumer consumer = buffers.getBuffer(ModRenderTypes.GLOW);
         boolean atFrame = EffectChannel.stampedAt(now);
         int verts = 0;
         Iterator<ShockwaveEffect.Wave> it = WAVES.iterator();

         while (it.hasNext()) {
            ShockwaveEffect.Wave wave = it.next();
            wave.advance(now);
            float progress = wave.progress();
            if (progress >= 1.0F) {
               it.remove();
               ShenlinMod.LOGGER
                  .info(
                     "[shenlin] wave drop live={} span={} left={} clock={}/{}", new Object[]{wave.elapsed(), wave.span(), WAVES.size(), now, Util.getMillis()}
                  );
            } else if (look == null || ViewGate.sees(camera, look, wave.x(), wave.y(), wave.z())) {
               float size = radiusAt(wave.elapsed(), wave.fastRadius(), wave.fastMs(), wave.radius(), wave.slowMs());
               float alpha = (float)Math.pow(1.0F - progress, 0.9);
               if (wave.route() == null) {
                  wave.noteRoute(atFrame ? "frame" : "stage");
                  ShenlinMod.LOGGER.info("[shenlin] wave route={} live={}", atFrame ? "frame" : "stage", WAVES.size());
               }

               verts += disc(
                  consumer,
                  wave.x() - camera.x,
                  wave.y() - camera.y,
                  wave.z() - camera.z,
                  size,
                  size - Math.max(0.09F, size * 0.16F),
                  wave.axis(),
                  alpha,
                  wave.innerColor(),
                  wave.outerColor()
               );
               if (wave.wild()) {
                  verts += flare(
                     consumer, wave.x() - camera.x, wave.y() - camera.y, wave.z() - camera.z, size * 0.85F * wave.flareScale(), alpha
                  );
               }
            }
         }

         if (verts > 0) {
            buffers.endBatch(ModRenderTypes.GLOW);
         }

         return verts;
      } else {
         if (!WAVES.isEmpty() && now - lastVoidLog > 1000L) {
            lastVoidLog = now;
            ShenlinMod.LOGGER.info("[shenlin] wave idle left={} who={}", WAVES.size(), EffectChannel.stampedAt(now) ? "frame" : "stage");
         }

         return 0;
      }
   }

   private static float radiusAt(long age, float fastRadius, long fastMs, float full, long slowMs) {
      if (age <= fastMs) {
         float p = (float)age / (float)fastMs;
         return 0.35F + (fastRadius - 0.35F) * p;
      } else if (age <= fastMs + slowMs) {
         float p = (float)(age - fastMs) / (float)slowMs;
         return fastRadius + (full - fastRadius) * p;
      } else {
         return full;
      }
   }

   private static int disc(
      VertexConsumer consumer, double ox, double oy, double oz, float outer, float inner, Vec3 axis, float alpha, float[] inCol, float[] outCol
   ) {
      if (!(alpha <= 0.02F) && !(outer <= 0.05F)) {
         Vec3 n = axis.lengthSqr() < 1.0E-9 ? UP : axis.normalize();
         Vec3 helper = Math.abs(n.y) > 0.9 ? EAST : UP;
         Vec3 tsrg2 = n.cross(helper);
         if (tsrg2.lengthSqr() < 1.0E-9) {
            return 0;
         }

         tsrg2 = tsrg2.normalize();
         Vec3 up = n.cross(tsrg2).normalize();
         float inner0 = Math.max(0.0F, Math.min(inner, outer - 0.02F));
         Vec3 center = new Vec3(ox, oy, oz);
         int verts = 0;

         for (int i = 0; i < 40; i++) {
            double a0 = i * (Math.PI * 2) / 40.0;
            double a1 = (i + 1) * (Math.PI * 2) / 40.0;
            Vec3 d0 = tsrg2.scale(Math.cos(a0)).add(up.scale(Math.sin(a0)));
            Vec3 d1 = tsrg2.scale(Math.cos(a1)).add(up.scale(Math.sin(a1)));
            Vec3 i0 = center.add(d0.scale(inner0));
            Vec3 o0 = center.add(d0.scale(outer));
            Vec3 i1 = center.add(d1.scale(inner0));
            Vec3 o1 = center.add(d1.scale(outer));
            float dim = alpha * 0.3F;
            vertex(consumer, (float)i0.x, (float)i0.y, (float)i0.z, inCol[0], inCol[1], inCol[2], dim);
            vertex(consumer, (float)o0.x, (float)o0.y, (float)o0.z, outCol[0], outCol[1], outCol[2], alpha);
            vertex(consumer, (float)o1.x, (float)o1.y, (float)o1.z, outCol[0], outCol[1], outCol[2], alpha);
            vertex(consumer, (float)i0.x, (float)i0.y, (float)i0.z, inCol[0], inCol[1], inCol[2], dim);
            vertex(consumer, (float)o1.x, (float)o1.y, (float)o1.z, outCol[0], outCol[1], outCol[2], alpha);
            vertex(consumer, (float)i1.x, (float)i1.y, (float)i1.z, inCol[0], inCol[1], inCol[2], dim);
            verts += 6;
         }

         return verts;
      } else {
         return 0;
      }
   }

   private static int flare(VertexConsumer consumer, double ox, double oy, double oz, float size, float alpha) {
      if (!(alpha <= 0.02F) && !(size <= 0.02F)) {
         Vec3 at = new Vec3(ox, oy, oz);
         Vec3 view = at.scale(-1.0);
         if (view.lengthSqr() < 1.0E-9) {
            return 0;
         }

         view = view.normalize();
         Vec3 tsrg2 = view.cross(UP);
         if (tsrg2.lengthSqr() < 1.0E-9) {
            tsrg2 = view.cross(EAST);
         }

         tsrg2 = tsrg2.normalize();
         Vec3 up = tsrg2.cross(view).normalize();
         int verts = 0;
         verts += quad(consumer, at, tsrg2, up, size, 0.88F, 0.97F, 1.0F, alpha * 0.22F);
         verts += quad(consumer, at, tsrg2, up, size * 0.55F, 0.94F, 0.99F, 1.0F, alpha * 0.34F);
         return verts + quad(consumer, at, tsrg2, up, size * 0.26F, 1.0F, 1.0F, 1.0F, alpha * 0.55F);
      } else {
         return 0;
      }
   }

   private static int quad(VertexConsumer consumer, Vec3 at, Vec3 tsrg2, Vec3 up, float size, float r, float g, float b, float alpha) {
      Vec3 a = at.add(tsrg2.scale(size)).add(up.scale(size));
      Vec3 c = at.subtract(tsrg2.scale(size)).add(up.scale(size));
      Vec3 d = at.subtract(tsrg2.scale(size)).subtract(up.scale(size));
      Vec3 e = at.add(tsrg2.scale(size)).subtract(up.scale(size));
      vertex(consumer, (float)a.x, (float)a.y, (float)a.z, r, g, b, alpha);
      vertex(consumer, (float)c.x, (float)c.y, (float)c.z, r, g, b, alpha);
      vertex(consumer, (float)d.x, (float)d.y, (float)d.z, r, g, b, alpha);
      vertex(consumer, (float)a.x, (float)a.y, (float)a.z, r, g, b, alpha);
      vertex(consumer, (float)d.x, (float)d.y, (float)d.z, r, g, b, alpha);
      vertex(consumer, (float)e.x, (float)e.y, (float)e.z, r, g, b, alpha);
      return 6;
   }

   private static void vertex(VertexConsumer consumer, float x, float y, float z, float r, float g, float b, float alpha) {
      consumer.vertex(x, y, z).color(r, g, b, alpha).endVertex();
   }

   private static final class Wave {
      private final double x;
      private final double y;
      private final double z;
      private final float radius;
      private final long span;
      private final float fastRadius;
      private final long fastMs;
      private final long slowMs;
      private final Vec3 axis;
      private final boolean wild;
      private final boolean warm;
      private final boolean violet;
      private final float flareScale;
      private long seen = Long.MIN_VALUE;
      private long elapsed;
      private String route;

      private Wave(double x, double y, double z, float radius, long span, float fastRadius, long fastMs, long slowMs, Vec3 axis, boolean wild, boolean warm) {
         this(x, y, z, radius, span, fastRadius, fastMs, slowMs, axis, wild, warm, false, 1.0F);
      }

      private Wave(
         double x,
         double y,
         double z,
         float radius,
         long span,
         float fastRadius,
         long fastMs,
         long slowMs,
         Vec3 axis,
         boolean wild,
         boolean warm,
         boolean violet
      ) {
         this(x, y, z, radius, span, fastRadius, fastMs, slowMs, axis, wild, warm, violet, 1.0F);
      }

      private Wave(
         double x,
         double y,
         double z,
         float radius,
         long span,
         float fastRadius,
         long fastMs,
         long slowMs,
         Vec3 axis,
         boolean wild,
         boolean warm,
         boolean violet,
         float flareScale
      ) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.radius = radius;
         this.span = span;
         this.fastRadius = fastRadius;
         this.fastMs = fastMs;
         this.slowMs = slowMs;
         this.axis = axis;
         this.wild = wild;
         this.warm = warm;
         this.violet = violet;
         this.flareScale = flareScale;
      }

      private float flareScale() {
         return this.flareScale;
      }

      private float[] outerColor() {
         return this.violet ? ShockwaveEffect.VIOLET_OUT : (this.warm ? ShockwaveEffect.WARM_OUT : ShockwaveEffect.COOL_OUT);
      }

      private float[] innerColor() {
         return this.violet ? ShockwaveEffect.VIOLET_IN : (this.warm ? ShockwaveEffect.WARM_IN : ShockwaveEffect.COOL_IN);
      }

      private void advance(long now) {
         if (this.seen == Long.MIN_VALUE) {
            this.seen = now;
         } else {
            long step = now - this.seen;
            this.seen = now;
            this.elapsed += step >= 0L && step <= 250L ? step : 50L;
         }
      }

      private float progress() {
         return Math.min(1.0F, (float)this.elapsed / (float)this.span);
      }

      private double x() {
         return this.x;
      }

      private double y() {
         return this.y;
      }

      private double z() {
         return this.z;
      }

      private float radius() {
         return this.radius;
      }

      private long span() {
         return this.span;
      }

      private float fastRadius() {
         return this.fastRadius;
      }

      private long fastMs() {
         return this.fastMs;
      }

      private long slowMs() {
         return this.slowMs;
      }

      private Vec3 axis() {
         return this.axis;
      }

      private boolean wild() {
         return this.wild;
      }

      private long elapsed() {
         return this.elapsed;
      }

      private String route() {
         return this.route;
      }

      private void noteRoute(String where) {
         this.route = where;
      }
   }
}

