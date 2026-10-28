package com.myx00x.maid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.mob.RelicWardenEntity;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.util.Mth;
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
import org.joml.Matrix4f;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT, bus = Bus.FORGE)
public final class RayEffect {
   private static final int SEGMENTS = 26;
   private static final int ARCS = 3;
   private static final float BEAM_WIDTH = 0.11F;
   private static final float ARC_WIDTH = 0.028F;
   private static final float ARC_RADIUS = 0.22F;
   private static final double ARC_PITCH = 6.0;
   private static final float GROW_FRACTION = 0.16F;
   private static final double AXIS_CLEARANCE_SQR = 4.0E-4;
   private static final int MAX_ALIVE = 24;
   private static final float COLUMN_SCALE = 2.6F;
   public static final float DEFAULT_R = 1.0F;
   public static final float DEFAULT_G = 0.1F;
   public static final float DEFAULT_B = 0.06F;
   private static final float ARC_SHADE = 0.85F;
   private static final float CORE_WASH = 0.55F;
   private static final int CORE_SEGMENTS = 10;
   private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
   private static final Vec3 EAST = new Vec3(1.0, 0.0, 0.0);
   private static final List<RayEffect.Ray> RAYS = new ArrayList<>();
   private static final List<RayEffect.Ray> NORMAL = new ArrayList<>();
   private static final List<RayEffect.Ray> THROUGH = new ArrayList<>();
   private static final List<RayEffect.Core> CORES = new ArrayList<>();
   private static long lastStageLog;
   private static int leadSkip;
   private static int expired;
   private static long lastVoidLog;
   private static long lastBindProbe;
   private static long lastGeomProbe;
   private static long lastHostProbe;
   private static long lastStageEntry;
   private static int skipLead;
   private static int skipShort;
   private static int skipAxis;
   private static int drawnRay;
   private static long lastSkipLog;

   private RayEffect() {
   }

   public static int count() {
      return RAYS.size();
   }

   public static int leadSkipped() {
      return leadSkip;
   }

   public static int expiredRemoved() {
      return expired;
   }

   public static void spawn(
      double fromX,
      double fromY,
      double fromZ,
      double toX,
      double toY,
      double toZ,
      int ticks,
      float width,
      boolean through,
      int leadMs,
      float cr,
      float cg,
      float cb,
      float coreWidth,
      float burst,
      int followId,
      boolean column,
      int hangOn
   ) {
      long lead = Math.max(0, leadMs);
      long span = Math.max(1, ticks) * 50L;
      RAYS.add(
         new RayEffect.Ray(
            new Vec3(fromX, fromY, fromZ),
            new Vec3(toX, toY, toZ),
            lead,
            span,
            Math.max(0.1F, width),
            through,
            cr,
            cg,
            cb,
            Math.max(0.0F, coreWidth),
            Math.max(0.0F, burst),
            followId,
            column,
            hangOn
         )
      );

      while (RAYS.size() > 24) {
         RAYS.remove(0);
      }

      ShockwaveEffect.spawn(fromX, fromY, fromZ, 1.6F, 20, 1.6F, 200L, 400L, 0.0F, 1.0F, 0.0F, false, true);
      ShenlinMod.LOGGER
         .info(
            "[shenlin] ray born lead={} span={} core={} burst={} bind={} col={} live={} from={}/{}/{} to={}/{}/{} clock={}/{}",
            new Object[]{
               lead,
               span,
               coreWidth,
               burst,
               followId,
               column,
               RAYS.size(),
               (float)fromX,
               (float)fromY,
               (float)fromZ,
               (float)toX,
               (float)toY,
               (float)toZ,
               EffectClock.now(),
               Util.getMillis()
            }
         );
   }

   @SubscribeEvent
   public static void onRenderStage(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_PARTICLES) {
         long now = EffectClock.now();
         if (!EffectChannel.handPassAlive(EffectClock.now())) {
            if (!RAYS.isEmpty()) {
               try {
                  boolean world = EffectCapture.pushSure(event);

                  int verts;
                  try {
                     verts = paint(new PoseStack(), event.getCamera().getPosition(), now);
                  } finally {
                     EffectCapture.popSure();
                  }

                  if (now - lastStageEntry > 1000L && (!RAYS.isEmpty() || verts > 0)) {
                     lastStageEntry = now;
                     ShenlinMod.LOGGER
                        .info("[shenlin] stage entry 手上={} 顶点={} captured={} 让位={}", new Object[]{RAYS.size(), verts, world, EffectChannel.handPassAlive(now)});
                  }

                  if (verts > 0 && now - lastStageLog > 1000L) {
                     lastStageLog = now;
                     Camera cam = event.getCamera();
                     Vec3 camAt = cam.getPosition();
                     ShenlinMod.LOGGER
                        .info(
                           "[shenlin] stage pass verts={} captured={} viewT={}/{}/{} cam={}/{}/{} yaw={} pitch={}",
                           new Object[]{
                              verts,
                              world,
                              EffectCapture.viewTranslateX(),
                              EffectCapture.viewTranslateY(),
                              EffectCapture.viewTranslateZ(),
                              (float)camAt.x,
                              (float)camAt.y,
                              (float)camAt.z,
                              cam.getYRot(),
                              cam.getXRot()
                           }
                        );
                  }
               } catch (Throwable t) {
                  RAYS.clear();
                  ShenlinMod.LOGGER.warn("[shenlin] ray draw failed: {}", t.toString());
               }
            }
         }
      }
   }

   public static boolean pending() {
      return !RAYS.isEmpty();
   }

   public static void dropAll() {
      RAYS.clear();
   }

   public static boolean wantsNow() {
      for (RayEffect.Ray ray : RAYS) {
         if (ray.elapsed() > ray.leadMs()) {
            return true;
         }
      }

      return false;
   }

   public static int paintAtHandPass(PoseStack poseStack, Vec3 camera, long now) {
      EffectCapture.beginHandPass(poseStack);

      try {
         return paint(new PoseStack(), camera, now);
      } finally {
         EffectCapture.endHandPass();
      }
   }

   public static int paint(PoseStack stack, Vec3 camera, long now) {
      return paintInternal(stack, camera, now, -1, null, true);
   }

   public static int paintOnHost(PoseStack stack, RelicWardenEntity host, Vec3 hostPos, MultiBufferSource source, long now) {
      int got = 0;

      for (RayEffect.Ray ray : RAYS) {
         if (ray.hangOn() == host.getId()) {
            got++;
         }
      }

      long probe = EffectClock.now();
      if (probe - lastHostProbe > 1000L) {
         lastHostProbe = probe;
         Matrix4f mv = stack.last().pose();
         ShenlinMod.LOGGER
            .info(
               "[shenlin] host paint host={} 该画={} 总={} poseT={}/{}/{} pose00={} hostPos={}/{}/{}",
               new Object[]{
                  host.getId(),
                  got,
                  RAYS.size(),
                  mv.m30(),
                  mv.m31(),
                  mv.m32(),
                  mv.m00(),
                  (float)hostPos.x,
                  (float)hostPos.y,
                  (float)hostPos.z
               }
            );
      }

      return 0;
   }

   private static int paintInternal(PoseStack stack, Vec3 origin, long now, int hostId, BufferSource external, boolean clearAxis) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.player != null) {
         BufferSource buffers = external != null ? external : mc.renderBuffers().bufferSource();
         boolean ownBatch = external == null;
         NORMAL.clear();
         THROUGH.clear();
         leadSkip = 0;
         expired = 0;
         Iterator<RayEffect.Ray> it = RAYS.iterator();

         while (it.hasNext()) {
            RayEffect.Ray ray = it.next();
            ray.advance(now);
            resolveBind(ray);
            if (hostId < 0) {
               long probeAt = EffectClock.now();
               if (probeAt - lastGeomProbe > 1000L) {
                  lastGeomProbe = probeAt;
                  Vec3 wp = ray.from();
                  float[] uv = EffectCapture.projectToScreen(wp.x, wp.y, wp.z);
                  ShenlinMod.LOGGER
                     .info(
                        "[shenlin] geom probe 世界={}/{}/{} 屏幕={} shader={} mv=[{}]",
                        new Object[]{
                           (float)wp.x,
                           (float)wp.y,
                           (float)wp.z,
                           uv == null ? "在相机身后(w<=0)" : String.format("u=%.3f v=%.3f", uv[0], uv[1]),
                           ShaderPack.inUse(),
                           EffectCapture.lastModelViewDump()
                        }
                     );
               }
            }

            if (ray.elapsed() >= ray.leadMs() + ray.span()) {
               it.remove();
               expired++;
               ShenlinMod.LOGGER
                  .info(
                     "[shenlin] ray drop live={} lead={} span={} left={} clock={}/{}",
                     new Object[]{ray.elapsed(), ray.leadMs(), ray.span(), RAYS.size(), now, Util.getMillis()}
                  );
            } else if (hostId < 0 || ray.hangOn() == hostId) {
               (ray.through() ? THROUGH : NORMAL).add(ray);
            }
         }

         if (NORMAL.isEmpty() && THROUGH.isEmpty()) {
            return 0;
         }

         int verts = 0;
         stack.pushPose();
         if (!NORMAL.isEmpty()) {
            verts += batch(buffers, ModRenderTypes.GLOW_ALPHA, NORMAL, stack, origin, clearAxis, ownBatch);
         }

         if (!THROUGH.isEmpty()) {
            verts += batch(buffers, ModRenderTypes.GLOW_ALPHA_THROUGH, THROUGH, stack, origin, clearAxis, ownBatch);
         }

         verts += drawCores(buffers, stack, ownBatch);
         stack.popPose();
         return verts;
      } else {
         if (!RAYS.isEmpty() && now - lastVoidLog > 1000L) {
            lastVoidLog = now;
            ShenlinMod.LOGGER.info("[shenlin] ray idle left={} who={}", RAYS.size(), EffectChannel.stampedAt(now) ? "frame" : "stage");
         }

         return 0;
      }
   }

   private static int batch(BufferSource buffers, RenderType type, List<RayEffect.Ray> rays, PoseStack stack, Vec3 origin, boolean clearAxis, boolean ownBatch) {
      VertexConsumer consumer = buffers.getBuffer(type);
      int verts = 0;

      for (RayEffect.Ray ray : rays) {
         if (ray.elapsed() <= ray.leadMs()) {
            leadSkip++;
            skipLead++;
         } else {
            float progress = ray.progress();
            float fade = (float)Math.pow(1.0F - progress, 1.3);
            Vec3 from = ray.from().subtract(origin);
            Vec3 full = ray.to().subtract(origin).subtract(from);
            Vec3 axis = full.scale(Math.min(1.0, progress / 0.16F));
            double length = axis.length();
            if (length < 0.02) {
               skipShort++;
            } else {
               Vec3 unit = axis.scale(1.0 / length);
               Vec3 mid = from.add(axis.scale(0.5));
               if (clearAxis) {
                  double alongAxis = from.dot(unit);
                  if (from.lengthSqr() - alongAxis * alongAxis < 4.0E-4) {
                     skipAxis++;
                     continue;
                  }
               }

               Vec3 side = unit.cross(mid.scale(-1.0));
               if (side.lengthSqr() < 1.0E-6) {
                  side = unit.cross(UP);
                  if (side.lengthSqr() < 1.0E-6) {
                     side = unit.cross(EAST);
                  }
               }

               side = side.normalize();
               float wide = 0.11F * ray.width() * (1.0F - 0.6F * progress) * (ray.column() ? 2.6F : 1.0F);
               drawnRay++;
               verts += beam(consumer, from, unit, length, side, fade, wide, ray.rgb());
               if (ray.column()) {
                  Vec3 cross = unit.cross(side);
                  if (cross.lengthSqr() > 1.0E-6) {
                     verts += beam(consumer, from, unit, length, cross.normalize(), fade, wide, ray.rgb());
                  }
               } else {
                  verts += arcs(consumer, from, unit, length, side, fade, ray.elapsed(), ray.width(), ray.arcRgb(), ray.burst(), ray.arcSeed());
               }

               if (ray.coreWidth() > 0.0F) {
                  CORES.add(new RayEffect.Core(from, unit, length, side, ray.coreWidth() * ray.width() * (1.0F - 0.5F * progress), ray.rgb(), fade));
               }
            }
         }
      }

      if (ownBatch) {
         buffers.endBatch(type);
      }

      long skipNow = EffectClock.now();
      if (skipNow - lastSkipLog > 1000L && skipLead + skipShort + skipAxis + drawnRay > 0) {
         lastSkipLog = skipNow;
         ShenlinMod.LOGGER.info("[shenlin] ray batch 画了={} 压后={} 太短={} 贴轴={}", new Object[]{drawnRay, skipLead, skipShort, skipAxis});
         skipLead = 0;
         skipShort = 0;
         skipAxis = 0;
         drawnRay = 0;
      }

      return verts;
   }

   private static void resolveBind(RayEffect.Ray ray) {
      if (ray.followId() >= 0) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level != null && mc.level.getEntity(ray.followId()) instanceof RelicWardenEntity warden) {
            float var11 = mc.getFrameTime();
            Vec3 from = warden.spikePointAt(0.0, var11);
            Vec3 dir = warden.spikeAimDir(var11);
            long probeNow = EffectClock.now();
            if (probeNow - lastBindProbe > 1000L) {
               lastBindProbe = probeNow;
               Vec3 host = warden.position();
               Vec3 pack = ray.bindOrigin();
               ShenlinMod.LOGGER
                  .info(
                     "[shenlin] bind probe 客户端算={}/{}/{} 包里={}/{}/{} 兽={}/{}/{} dir={}/{}/{} 差={}",
                     new Object[]{
                        (float)from.x,
                        (float)from.y,
                        (float)from.z,
                        (float)pack.x,
                        (float)pack.y,
                        (float)pack.z,
                        (float)host.x,
                        (float)host.y,
                        (float)host.z,
                        (float)dir.x,
                        (float)dir.y,
                        (float)dir.z,
                        String.format("%.2f", from.distanceTo(pack))
                     }
                  );
            }

            Vec3 far = from.add(dir.scale(ray.bindLength()));
            Vec3 to = far;
            BlockHitResult hit = mc.level.clip(new ClipContext(from, far, Block.COLLIDER, Fluid.NONE, warden));
            if (hit.getType() != Type.MISS) {
               to = hit.getLocation();
            }

            ray.bind(from, to);
         }
      }
   }

   private static int beam(VertexConsumer consumer, Vec3 from, Vec3 unit, double length, Vec3 side, float alpha, float width, float[] rgb) {
      int verts = 0;

      for (int i = 0; i < 26; i++) {
         float t0 = i / 26.0F;
         float t1 = (i + 1) / 26.0F;
         Vec3 p0 = from.add(unit.scale(length * t0));
         Vec3 p1 = from.add(unit.scale(length * t1));
         float w0 = width * taper(t0);
         float w1 = width * taper(t1);
         float vA = (float)(length * t0);
         float vB = (float)(length * t1);
         verts += ribbon(consumer, p0, p1, side, w0, w1, rgb, alpha, vA, vB, EffectInk.EDGES);
      }

      return verts;
   }

   private static int arcs(
      VertexConsumer consumer, Vec3 from, Vec3 unit, double length, Vec3 side, float alpha, long lived, float width, float[] rgb, float burst, float seed
   ) {
      Vec3 across = unit.cross(side).normalize();
      double pitch = 6.0 * (1.0 + 1.7 * burst);
      int count = burst > 0.0F ? 3 + Math.round(3.0F * burst) : 3;
      double turns = Mth.clamp(length / pitch, 1.5, 64.0);
      int segments = (int)Mth.clamp(turns * 8.0, 20.0, 220.0);
      float thick = 0.028F * width * (1.0F + 0.35F * burst);
      float spread = 0.22F * (1.0F + 0.8F * burst);
      int verts = 0;

      for (int a = 0; a < count; a++) {
         float phase = burst > 0.0F ? (float)(a * 2.399963229 + seed) : (float)(a * Math.PI * 2.0 / count + seed);
         Vec3 prev = arcPoint(from, unit, side, across, length, 0.0F, turns, phase, lived, a, spread, burst);

         for (int i = 1; i <= segments; i++) {
            float t = (float)i / segments;
            Vec3 cur = arcPoint(from, unit, side, across, length, t, turns, phase, lived, a, spread, burst);
            double theta = t * turns * Math.PI * 2.0 + phase + lived * 0.012;
            float spark = burst > 0.0F ? (float)(1.0 - 0.65 * burst + 0.65 * burst * Math.pow(Math.abs(Math.sin(theta * 1.7)), 0.55)) : 1.0F;
            verts += ribbon(
               consumer,
               prev,
               cur,
               side,
               thick,
               thick,
               rgb,
               alpha * 0.85F * spark,
               (float)(length * (i - 1) / segments),
               (float)(length * t),
               EffectInk.ARC_EDGES
            );
            prev = cur;
         }
      }

      return verts;
   }

   private static Vec3 arcPoint(
      Vec3 from, Vec3 unit, Vec3 u, Vec3 v, double length, float t, double turns, float phase, long lived, int index, float spread, float burst
   ) {
      double theta = t * turns * Math.PI * 2.0 + phase + lived * 0.012;
      double wobble = 1.0 + (0.3 + 0.55 * burst) * Math.sin(theta * 3.1 + index * 2.3) + (0.12 + 0.4 * burst) * Math.sin(theta * 7.7 + lived * 0.02);
      double open = burst > 0.0F ? 0.12 + 1.9 * t : 0.45 + 0.75 * t;
      double radius = spread * Math.max(0.15, wobble) * open;
      return from.add(unit.scale(length * t)).add(u.scale(Math.cos(theta) * radius)).add(v.scale(Math.sin(theta) * radius));
   }

   private static int drawCores(BufferSource buffers, PoseStack stack, boolean ownBatch) {
      if (CORES.isEmpty()) {
         return 0;
      }

      VertexConsumer consumer = buffers.getBuffer(ModRenderTypes.GLOW);
      int verts = 0;

      for (RayEffect.Core c : CORES) {
         verts += core(consumer, c);
      }

      CORES.clear();
      if (ownBatch) {
         buffers.endBatch(ModRenderTypes.GLOW);
      }

      return verts;
   }

   private static int core(VertexConsumer consumer, RayEffect.Core c) {
      if (!(c.alpha() <= 0.02F) && !(c.half() <= 0.0F)) {
         float r = c.rgb()[0] + (1.0F - c.rgb()[0]) * 0.55F;
         float g = c.rgb()[1] + (1.0F - c.rgb()[1]) * 0.55F;
         float b = c.rgb()[2] + (1.0F - c.rgb()[2]) * 0.55F;
         int verts = 0;

         for (int i = 0; i < 10; i++) {
            float t0 = i / 10.0F;
            float t1 = (i + 1) / 10.0F;
            verts += solid(
               consumer,
               c.from().add(c.unit().scale(c.length() * t0)),
               c.from().add(c.unit().scale(c.length() * t1)),
               c.side(),
               c.half() * taper(t0),
               c.half() * taper(t1),
               r,
               g,
               b,
               c.alpha()
            );
         }

         return verts;
      } else {
         return 0;
      }
   }

   private static int solid(VertexConsumer consumer, Vec3 a, Vec3 b, Vec3 side, float wideA, float wideB, float r, float g, float bl, float alpha) {
      Vec3 a1 = a.add(side.scale(wideA));
      Vec3 a2 = a.subtract(side.scale(wideA));
      Vec3 b1 = b.add(side.scale(wideB));
      Vec3 b2 = b.subtract(side.scale(wideB));
      flat(consumer, a1, r, g, bl, alpha);
      flat(consumer, a2, r, g, bl, alpha);
      flat(consumer, b2, r, g, bl, alpha);
      flat(consumer, a1, r, g, bl, alpha);
      flat(consumer, b2, r, g, bl, alpha);
      flat(consumer, b1, r, g, bl, alpha);
      return 6;
   }

   private static void flat(VertexConsumer consumer, Vec3 p, float r, float g, float b, float alpha) {
      consumer.vertex(p.x, p.y, p.z).color(r, g, b, alpha).endVertex();
   }

   private static float taper(float t) {
      return (float)(0.3 + 0.7 * Math.sin(Math.PI * t));
   }

   private static int ribbon(
      VertexConsumer consumer, Vec3 a, Vec3 b, Vec3 side, float widthA, float widthB, float[] rgb, float alpha, float vA, float vB, float[] edges
   ) {
      return EffectInk.layered(consumer, a, b, side, widthA, widthB, rgb, alpha, vA, vB, edges, true);
   }

   private record Core(Vec3 from, Vec3 unit, double length, Vec3 side, float half, float[] rgb, float alpha) {
   }

   private static final class Ray {
      private Vec3 from;
      private Vec3 to;
      private final Vec3 bindOrigin;
      private final long leadMs;
      private final long span;
      private final float width;
      private final boolean through;
      private final float[] rgb;
      private final float[] arcRgb;
      private final float coreWidth;
      private final float burst;
      private final int followId;
      private final int hangOn;
      private final boolean column;
      private final double bindLength;
      private final float arcSeed;
      private long seen = Long.MIN_VALUE;
      private long elapsed;

      private Ray(
         Vec3 from,
         Vec3 to,
         long leadMs,
         long span,
         float width,
         boolean through,
         float cr,
         float cg,
         float cb,
         float coreWidth,
         float burst,
         int followId,
         boolean column,
         int hangOn
      ) {
         this.from = from;
         this.to = to;
         this.leadMs = leadMs;
         this.span = span;
         this.width = width;
         this.through = through;
         this.rgb = new float[]{cr, cg, cb};
         this.arcRgb = new float[]{cr * 0.85F, cg * 0.85F, cb * 0.85F};
         this.coreWidth = coreWidth;
         this.burst = burst;
         this.followId = followId;
         this.column = column;
         this.hangOn = hangOn;
         this.bindOrigin = from;
         this.bindLength = from.distanceTo(to);
         double hash = from.x * 127.1 + from.y * 311.7 + from.z * 74.7 + to.x * 269.5 + to.y * 183.3 + to.z * 246.1;
         this.arcSeed = (float)((hash * 0.61803398875 % 1.0 + 1.0) % 1.0) * (float) (Math.PI * 2);
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
         return this.elapsed <= this.leadMs ? 0.0F : Math.min(1.0F, (float)(this.elapsed - this.leadMs) / (float)this.span);
      }

      private void bind(Vec3 f, Vec3 t) {
         this.from = f;
         this.to = t;
      }

      private int followId() {
         return this.followId;
      }

      private int hangOn() {
         return this.hangOn;
      }

      private boolean column() {
         return this.column;
      }

      private double bindLength() {
         return this.bindLength;
      }

      private Vec3 bindOrigin() {
         return this.bindOrigin;
      }

      private Vec3 from() {
         return this.from;
      }

      private Vec3 to() {
         return this.to;
      }

      private long leadMs() {
         return this.leadMs;
      }

      private long span() {
         return this.span;
      }

      private float width() {
         return this.width;
      }

      private float[] rgb() {
         return this.rgb;
      }

      private float[] arcRgb() {
         return this.arcRgb;
      }

      private float coreWidth() {
         return this.coreWidth;
      }

      private float burst() {
         return this.burst;
      }

      private float arcSeed() {
         return this.arcSeed;
      }

      private boolean through() {
         return this.through;
      }

      private long elapsed() {
         return this.elapsed;
      }
   }
}

