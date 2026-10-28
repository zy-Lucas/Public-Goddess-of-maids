package com.myx00x.maid.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.mob.RelicWardenEntity;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT, bus = Bus.FORGE)
public final class StarEffect {
   public static final int TICKS = 6;
   private static final int SEGMENTS = 12;
   private static final float ARM_RATIO = 0.13F;
   private static final float SPIN = 0.26F;
   private static final double NEAR_CLEARANCE_SQR = 0.01;
   private static final int MAX_ALIVE = 16;
   private static final float[] STAR_RGB = new float[]{1.0F, 0.2F, 0.13F};
   private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
   private static final Vec3 EAST = new Vec3(1.0, 0.0, 0.0);
   private static final List<StarEffect.Star> STARS = new ArrayList<>();
   private static long lastVoidLog;

   private StarEffect() {
   }

   public static void spawn(double x, double y, double z, float radius) {
      spawn(x, y, z, radius, 6, false, -1);
   }

   public static void spawn(double x, double y, double z, float radius, int ticks, boolean sustain, int followId) {
      spawn(x, y, z, radius, ticks, sustain, followId, 1.0F);
   }

   public static void spawn(double x, double y, double z, float radius, int ticks, boolean sustain, int followId, float widthScale) {
      spawn(x, y, z, radius, ticks, sustain, followId, widthScale, STAR_RGB);
   }

   public static void spawn(double x, double y, double z, float radius, int ticks, boolean sustain, int followId, float widthScale, float[] rgb) {
      long span = Math.max(1, ticks) * 50L;
      STARS.add(
         new StarEffect.Star(new Vec3(x, y, z), span, Math.max(0.2F, radius), sustain, followId, Math.max(0.1F, widthScale), rgb == null ? STAR_RGB : rgb)
      );

      while (STARS.size() > 16) {
         STARS.remove(0);
      }

      ShenlinMod.LOGGER
         .info(
            "[shenlin] star born span={} sustain={} follow={} live={} clock={}/{}",
            new Object[]{span, sustain, followId, STARS.size(), EffectClock.now(), Util.getMillis()}
         );
   }

   @SubscribeEvent
   public static void onRenderStage(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_PARTICLES) {
         long now = EffectClock.now();
         if (!EffectChannel.handPassAlive(EffectClock.now())) {
            if (!STARS.isEmpty()) {
               EffectCapture.pushSure(event);

               try {
                  paint(new PoseStack(), event.getCamera().getPosition(), now, ShaderPack.inUse());
               } catch (Throwable t) {
                  STARS.clear();
                  ShenlinMod.LOGGER.warn("[shenlin] star draw failed: {}", t.toString());
               } finally {
                  EffectCapture.popSure();
               }
            }
         }
      }
   }

   public static boolean pending() {
      return !STARS.isEmpty();
   }

   public static int count() {
      return STARS.size();
   }

   public static void dropAll() {
      STARS.clear();
   }

   public static int paintAtHandPass(PoseStack poseStack, Vec3 camera, float partialTick, long now) {
      EffectCapture.beginHandPass(poseStack);

      try {
         return paint(new PoseStack(), camera, now, ShaderPack.inUse());
      } finally {
         EffectCapture.endHandPass();
      }
   }

   public static int paint(PoseStack stack, Vec3 camera, long now, boolean compat) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.player != null) {
         if (!compat) {
            ShaderInstance shader = ModShaders.slashRay();
            if (shader == null) {
               ModShaders.missing("slash_ray");
               return 0;
            }

            RenderSystem.setShader(ModShaders::slashRay);
         }

         Iterator<StarEffect.Star> it = STARS.iterator();

         while (it.hasNext()) {
            StarEffect.Star star = it.next();
            star.advance(now);
            if (!star.follow(mc)) {
               it.remove();
            } else if (star.elapsed() >= star.span()) {
               it.remove();
               ShenlinMod.LOGGER
                  .info(
                     "[shenlin] star drop elapsed={} span={} left={} clock={}/{}",
                     new Object[]{star.elapsed(), star.span(), STARS.size(), now, Util.getMillis()}
                  );
            }
         }

         if (STARS.isEmpty()) {
            return 0;
         }

         BufferSource buffers = mc.renderBuffers().bufferSource();
         RenderType type = compat ? ModRenderTypes.GLOW_THROUGH : ModRenderTypes.STAR_GLOW_THROUGH;
         stack.pushPose();
         VertexConsumer consumer = buffers.getBuffer(type);
         int verts = 0;

         for (StarEffect.Star star : STARS) {
            if (visibleThroughBlocks(mc, camera, star.center())) {
               verts += star(consumer, star, camera, compat);
            }
         }

         buffers.endBatch(type);
         stack.popPose();
         return verts;
      } else {
         if (!STARS.isEmpty() && now - lastVoidLog > 1000L) {
            lastVoidLog = now;
            ShenlinMod.LOGGER.info("[shenlin] star idle left={} who={}", STARS.size(), EffectChannel.stampedAt(now) ? "frame" : "stage");
         }

         return 0;
      }
   }

   private static boolean visibleThroughBlocks(Minecraft mc, Vec3 camera, Vec3 at) {
      if (mc.level != null && camera != null && at != null) {
         BlockHitResult hit = mc.level.clip(new ClipContext(camera, at, Block.COLLIDER, Fluid.NONE, mc.player));
         return hit.getType() == Type.MISS;
      } else {
         return false;
      }
   }

   private static int star(VertexConsumer consumer, StarEffect.Star it, Vec3 camera, boolean compat) {
      float progress = it.progress();
      float lit = it.sustain() ? flareSustain(progress) : flare(progress);
      if (lit <= 0.02F) {
         return 0;
      }

      Vec3 center = it.center().subtract(camera);
      double near = center.lengthSqr();
      if (near < 0.01) {
         lit *= (float)(near / 0.01);
         if (lit <= 0.02F) {
            return 0;
         }
      }

      Vec3 view = center.normalize();
      Vec3 vertical = UP.subtract(view.scale(UP.dot(view)));
      if (vertical.lengthSqr() < 1.0E-6) {
         vertical = EAST.subtract(view.scale(EAST.dot(view)));
      }

      vertical = vertical.normalize();
      Vec3 horizontal = vertical.cross(view).normalize();
      float spin = 0.26F * progress;
      float sin = Mth.sin(spin);
      float cos = Mth.cos(spin);
      float reach = it.radius() * lit;
      float width = reach * 0.13F * it.widthScale();
      float alpha = lit * lit;
      return bar(consumer, center, vertical.scale(cos).add(horizontal.scale(sin)), reach, width, alpha, compat, it.rgb())
         + bar(consumer, center, vertical.scale(-sin).add(horizontal.scale(cos)), reach, width, alpha, compat, it.rgb());
   }

   private static int bar(VertexConsumer consumer, Vec3 center, Vec3 dir, float reach, float width, float alpha, boolean compat, float[] rgb) {
      Vec3 side = dir.cross(center.scale(-1.0));
      if (side.lengthSqr() < 1.0E-6) {
         return 0;
      }

      side = side.normalize();
      Vec3 start = center.subtract(dir.scale(reach));
      int verts = 0;

      for (int i = 0; i < 12; i++) {
         float t0 = i / 12.0F;
         float t1 = (i + 1) / 12.0F;
         Vec3 p0 = start.add(dir.scale(reach * 2.0 * t0));
         Vec3 p1 = start.add(dir.scale(reach * 2.0 * t1));
         verts += ribbon(consumer, p0, p1, side, width * tip(t0), width * tip(t1), rgb, alpha, t0, t1, compat);
      }

      return verts;
   }

   private static float tip(float t) {
      double shape = Math.sin(Math.PI * t);
      return (float)(shape * shape);
   }

   private static float flare(float progress) {
      return (float)Math.sin(Math.PI * Math.pow(Mth.clamp(progress, 0.0F, 1.0F), 0.6F));
   }

   private static float flareSustain(float progress) {
      float p = Mth.clamp(progress, 0.0F, 1.0F);
      float rise = p < 0.1F ? p / 0.1F : 1.0F;
      float fade = p > 0.82F ? (1.0F - p) / 0.18F : 1.0F;
      float pulse = 0.92F + 0.08F * Mth.sin(p * 34.0F);
      return rise * fade * pulse;
   }

   private static int ribbon(
      VertexConsumer consumer, Vec3 a, Vec3 b, Vec3 side, float widthA, float widthB, float[] rgb, float alpha, float vA, float vB, boolean compat
   ) {
      if (compat) {
         return EffectInk.layered(consumer, a, b, side, widthA, widthB, rgb, alpha, vA, vB, EffectInk.STAR_EDGES, false, 0.0F, 3.0);
      }

      Vec3 a1 = a.add(side.scale(widthA));
      Vec3 a2 = a.subtract(side.scale(widthA));
      Vec3 b1 = b.add(side.scale(widthB));
      Vec3 b2 = b.subtract(side.scale(widthB));
      put(consumer, a1, 0.0F, vA, rgb, alpha);
      put(consumer, a2, 1.0F, vA, rgb, alpha);
      put(consumer, b2, 1.0F, vB, rgb, alpha);
      put(consumer, a1, 0.0F, vA, rgb, alpha);
      put(consumer, b2, 1.0F, vB, rgb, alpha);
      put(consumer, b1, 0.0F, vB, rgb, alpha);
      return 6;
   }

   private static void put(VertexConsumer consumer, Vec3 p, float u, float v, float[] rgb, float alpha) {
      consumer.vertex(p.x, p.y, p.z)
         .color(rgb[0], rgb[1], rgb[2], alpha)
         .uv(u, v)
         .overlayCoords(OverlayTexture.NO_OVERLAY)
         .uv2(15728880)
         .normal(0.0F, 1.0F, 0.0F)
         .endVertex();
   }

   private static final class Star {
      private Vec3 center;
      private final long span;
      private final float radius;
      private final boolean sustain;
      private final int followId;
      private final float widthScale;
      private final float[] rgb;
      private long seen = Long.MIN_VALUE;
      private long elapsed;

      private Star(Vec3 center, long span, float radius, boolean sustain, int followId, float widthScale, float[] rgb) {
         this.center = center;
         this.span = span;
         this.radius = radius;
         this.sustain = sustain;
         this.followId = followId;
         this.widthScale = widthScale;
         this.rgb = rgb;
      }

      private float[] rgb() {
         return this.rgb;
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

      private float widthScale() {
         return this.widthScale;
      }

      private Vec3 center() {
         return this.center;
      }

      private boolean follow(Minecraft mc) {
         if (this.followId < 0) {
            return true;
         } else if (mc.level == null) {
            return false;
         } else {
            Entity host = mc.level.getEntity(this.followId);
            if (host != null && host.isAlive()) {
               float partial = mc.getFrameTime();
               this.center = host instanceof RelicWardenEntity warden
                  ? warden.airEmitPointAt(partial)
                  : new Vec3(
                     Mth.lerp(partial, host.xOld, host.getX()),
                     Mth.lerp(partial, host.yOld, host.getY()),
                     Mth.lerp(partial, host.zOld, host.getZ())
                  );
               return true;
            } else {
               return false;
            }
         }
      }

      private boolean sustain() {
         return this.sustain;
      }

      private long span() {
         return this.span;
      }

      private float radius() {
         return this.radius;
      }

      private long elapsed() {
         return this.elapsed;
      }
   }
}

