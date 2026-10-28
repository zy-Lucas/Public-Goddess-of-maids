package com.myx00x.maid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.combat.HoverCannon;
import com.myx00x.maid.compat.Upstream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT, bus = Bus.FORGE)
public final class CannonShotEffect {
   private static final float HEAD_OUT = 0.26F;
   private static final float HEAD_MID = 0.15F;
   private static final float HEAD_CORE = 0.075F;
   private static final float TAIL_WIDTH = 0.13F;
   private static final double TAIL_STEP = 0.22;
   private static final int TAIL_POINTS = 30;
   private static final long BOOM_MS = 320L;
   private static final long SHOT_LIFE_TICKS = 200L;
   private static final float R = 0.32F;
   private static final float G = 0.66F;
   private static final float B = 1.0F;
   private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
   private static final Vec3 EAST = new Vec3(1.0, 0.0, 0.0);
   private static final List<CannonShotEffect.Shot> SHOTS = new ArrayList<>();
   private static long lastLog;
   private static long lastFailLog;

   private CannonShotEffect() {
   }

   public static void spawn(double fx, double fy, double fz, double tx, double ty, double tz, long startTick) {
      Vec3 from = new Vec3(fx, fy, fz);
      Vec3 to = new Vec3(tx, ty, tz);
      double length = from.distanceTo(to);
      if (!(length < 0.1)) {
         SHOTS.add(new CannonShotEffect.Shot(from, to, length, startTick));
         ShenlinMod.LOGGER.info("[shenlin] cannon shot born dist={} live={}", (float)length, SHOTS.size());
      }
   }

   public static boolean pending() {
      return !SHOTS.isEmpty();
   }

   public static int count() {
      return SHOTS.size();
   }

   public static void dropAll() {
      SHOTS.clear();
   }

   @SubscribeEvent
   public static void onRenderStage(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_PARTICLES) {
         if (!EffectChannel.handPassAlive(EffectClock.now())) {
            if (!SHOTS.isEmpty()) {
               long now = EffectClock.now();

               try {
                  boolean world = EffectCapture.pushSure(event);

                  int verts;
                  try {
                     verts = paint(new PoseStack(), event.getCamera().getPosition(), event.getPartialTick(), now);
                  } finally {
                     EffectCapture.popSure();
                  }

                  if (verts > 0 && now - lastLog > 1000L) {
                     lastLog = now;
                     ShenlinMod.LOGGER.info("[shenlin] cannon shot stage verts={} live={} world={}", new Object[]{verts, SHOTS.size(), world});
                  }
               } catch (Throwable t) {
                  SHOTS.clear();
                  ShenlinMod.LOGGER.warn("[shenlin] cannon shot draw failed: {}", t.toString());
               }
            }
         }
      }
   }

   public static int paintAtHandPass(PoseStack poseStack, Vec3 camera, float partialTick, long now) {
      EffectCapture.beginHandPass(poseStack);

      try {
         return paint(new PoseStack(), camera, partialTick, now);
      } finally {
         EffectCapture.endHandPass();
      }
   }

   public static int paint(PoseStack stack, Vec3 camera, float partialTick, long now) {
      if (!Upstream.MAID) {
         return 0;
      }

      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.player != null && !SHOTS.isEmpty()) {
         double tick = (float)mc.level.getGameTime() + partialTick;
         Matrix4f pose = stack.last().pose();
         VertexConsumer consumer = mc.renderBuffers().bufferSource().getBuffer(ModRenderTypes.GLOW);
         int verts = 0;
         Iterator<CannonShotEffect.Shot> it = SHOTS.iterator();

         while (it.hasNext()) {
            CannonShotEffect.Shot shot = it.next();
            if (tick - shot.startTick > 200.0) {
               it.remove();
            } else if (shot.boomAt > 0L) {
               double t = (now - shot.boomAt) / 320.0;
               if (t >= 1.0) {
                  it.remove();
               } else {
                  verts += boom(consumer, pose, shot.to.subtract(camera), t);
               }
            } else {
               double travelled = Math.max(0.0, (tick - shot.startTick) * HoverCannon.shotSpeedPerTick());
               double reached = Math.min(travelled, shot.length);

               while (shot.sampled + 0.22 <= reached) {
                  shot.sampled += 0.22;
                  shot.tail.add(HoverCannon.shotPos(shot.from, shot.to, shot.sampled));
                  if (shot.tail.size() > 30) {
                     shot.tail.remove(0);
                  }
               }

               Vec3 head = HoverCannon.shotPos(shot.from, shot.to, reached).subtract(camera);
               verts += tail(consumer, pose, shot, head, camera);
               verts += head(consumer, pose, head);
               if (travelled >= shot.length) {
                  shot.boomAt = now;
               }
            }
         }

         mc.renderBuffers().bufferSource().endBatch(ModRenderTypes.GLOW);
         return verts;
      } else {
         return 0;
      }
   }

   private static int tail(VertexConsumer consumer, Matrix4f pose, CannonShotEffect.Shot shot, Vec3 head, Vec3 camera) {
      int points = shot.tail.size();
      int verts = 0;
      Vec3 prev = null;

      for (int i = 0; i < points; i++) {
         Vec3 cur = shot.tail.get(i).subtract(camera);
         if (prev != null) {
            verts += strand(consumer, pose, prev, cur, (float)(i + 1) / points);
         }

         prev = cur;
      }

      if (prev != null) {
         verts += strand(consumer, pose, prev, head, 1.0F);
      }

      return verts;
   }

   private static int strand(VertexConsumer consumer, Matrix4f pose, Vec3 a, Vec3 b, float k) {
      Vec3 axis = b.subtract(a);
      double length = axis.length();
      if (length < 1.0E-4) {
         return 0;
      }

      Vec3 unit = axis.scale(1.0 / length);
      Vec3 view = a.add(b).scale(0.5).scale(-1.0);
      if (view.lengthSqr() < 1.0E-6) {
         return 0;
      }

      Vec3 side = unit.cross(view.normalize());
      if (side.lengthSqr() < 1.0E-6) {
         return 0;
      }

      side = side.normalize();
      float half = 0.065F * k;
      float alpha = 0.52F * k;
      Vec3 a1 = a.add(side.scale(half));
      Vec3 a2 = a.subtract(side.scale(half));
      Vec3 b1 = b.add(side.scale(half));
      Vec3 b2 = b.subtract(side.scale(half));
      vert(consumer, pose, a1, 0.32F, 0.66F, 1.0F, alpha);
      vert(consumer, pose, a2, 0.32F, 0.66F, 1.0F, alpha);
      vert(consumer, pose, b2, 0.32F, 0.66F, 1.0F, alpha);
      vert(consumer, pose, a1, 0.32F, 0.66F, 1.0F, alpha);
      vert(consumer, pose, b2, 0.32F, 0.66F, 1.0F, alpha);
      vert(consumer, pose, b1, 0.32F, 0.66F, 1.0F, alpha);
      return 6;
   }

   private static int head(VertexConsumer consumer, Matrix4f pose, Vec3 at) {
      Vec3[] basis = basis(at);
      if (basis == null) {
         return 0;
      }

      Vec3 tsrg2 = basis[0];
      Vec3 up = basis[1];
      int verts = 0;
      verts += quad(consumer, pose, at, tsrg2, up, 0.26F, 0.32F, 0.66F, 1.0F, 0.5F);
      verts += quad(consumer, pose, at, tsrg2, up, 0.15F, 0.55F, 0.8F, 1.0F, 0.85F);
      return verts + quad(consumer, pose, at, tsrg2, up, 0.075F, 0.96F, 0.99F, 1.0F, 1.0F);
   }

   private static int boom(VertexConsumer consumer, Matrix4f pose, Vec3 at, double t) {
      Vec3[] basis = basis(at);
      if (basis == null) {
         return 0;
      }

      Vec3 tsrg2 = basis[0];
      Vec3 up = basis[1];
      float fade = (float)(1.0 - t);
      float spread = (float)(0.45 + 2.1 * t);
      int verts = 0;
      verts += quad(consumer, pose, at, tsrg2, up, (0.3F + 0.55F * (float)t) * fade + 0.12F, 0.8F, 0.92F, 1.0F, 0.8F * fade);
      verts += quad(consumer, pose, at, tsrg2, up, (0.55F + 1.05F * (float)t) * fade + 0.18F, 0.32F, 0.66F, 1.0F, 0.3F * fade);

      for (int i = 0; i < 8; i++) {
         double angle = i * Math.PI / 4.0;
         Vec3 off = tsrg2.scale(Math.cos(angle) * spread).add(up.scale(Math.sin(angle) * spread));
         verts += quad(consumer, pose, at.add(off), tsrg2, up, 0.3F * fade + 0.04F, 0.32F, 0.66F, 1.0F, 0.55F * fade);
      }

      return verts;
   }

   private static Vec3[] basis(Vec3 at) {
      Vec3 view = at.scale(-1.0);
      if (view.lengthSqr() < 1.0E-6) {
         return null;
      }

      view = view.normalize();
      Vec3 tsrg2 = view.cross(UP);
      if (tsrg2.lengthSqr() < 1.0E-6) {
         tsrg2 = view.cross(EAST);
      }

      tsrg2 = tsrg2.normalize();
      return new Vec3[]{tsrg2, tsrg2.cross(view).normalize()};
   }

   private static int quad(VertexConsumer consumer, Matrix4f pose, Vec3 at, Vec3 tsrg2, Vec3 up, float size, float r, float g, float b, float alpha) {
      Vec3 a = at.add(tsrg2.scale(size)).add(up.scale(size));
      Vec3 c = at.subtract(tsrg2.scale(size)).add(up.scale(size));
      Vec3 d = at.subtract(tsrg2.scale(size)).subtract(up.scale(size));
      Vec3 e = at.add(tsrg2.scale(size)).subtract(up.scale(size));
      vert(consumer, pose, a, r, g, b, alpha);
      vert(consumer, pose, c, r, g, b, alpha);
      vert(consumer, pose, d, r, g, b, alpha);
      vert(consumer, pose, a, r, g, b, alpha);
      vert(consumer, pose, d, r, g, b, alpha);
      vert(consumer, pose, e, r, g, b, alpha);
      return 6;
   }

   private static void vert(VertexConsumer consumer, Matrix4f pose, Vec3 p, float r, float g, float b, float alpha) {
      consumer.vertex(pose, (float)p.x, (float)p.y, (float)p.z).color(r, g, b, alpha).endVertex();
   }

   private static final class Shot {
      private final Vec3 from;
      private final Vec3 to;
      private final double length;
      private final long startTick;
      private final List<Vec3> tail = new ArrayList<>();
      private double sampled;
      private long boomAt = -1L;

      private Shot(Vec3 from, Vec3 to, double length, long startTick) {
         this.from = from;
         this.to = to;
         this.length = length;
         this.startTick = startTick;
      }
   }
}

