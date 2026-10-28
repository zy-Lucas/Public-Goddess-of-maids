package com.myx00x.maid.client;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.combat.HoverCannon;
import com.myx00x.maid.compat.Upstream;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
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
public final class CannonSpinEffect {
   private static final double RISE_TICKS = 5.0;
   private static final double FALL_TICKS = 6.0;
   private static final float PILLAR_WAIST = 0.19F;
   private static final float PILLAR_HALO = 1.85F;
   private static final double BEAM_STEP = 1.2;
   private static final int BEAM_MAX_SEGMENTS = 24;
   private static final float POD_R = 0.34F;
   private static final float POD_G = 0.7F;
   private static final float POD_B = 1.0F;
   private static final float BEAM_R = 0.55F;
   private static final float BEAM_G = 0.82F;
   private static final float BEAM_B = 1.0F;
   private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
   private static final Map<Integer, CannonSpinEffect.Spin> SPINS = new ConcurrentHashMap<>();
   private static long lastLog;
   private static long lastFailLog;

   private CannonSpinEffect() {
   }

   public static void spawn(int entityId, long startTick, int totalTicks) {
      if (totalTicks <= 0) {
         SPINS.remove(entityId);
      } else {
         SPINS.put(entityId, new CannonSpinEffect.Spin(startTick, totalTicks, EffectClock.now()));
         ShenlinMod.LOGGER
            .info(
               "[shenlin] spin laser born start={} ticks={} live={} localTick={} clock={}",
               new Object[]{startTick, totalTicks, SPINS.size(), localGameTick(), EffectClock.now()}
            );
      }
   }

   private static long localGameTick() {
      Minecraft mc = Minecraft.getInstance();
      return mc.level == null ? -1L : mc.level.getGameTime();
   }

   public static boolean pending() {
      return !SPINS.isEmpty();
   }

   public static int count() {
      return SPINS.size();
   }

   public static void dropAll() {
      SPINS.clear();
      SpinRaySound.dropAll();
   }

   @SubscribeEvent
   public static void onRenderStage(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_PARTICLES) {
         if (!EffectChannel.handPassAlive(EffectClock.now())) {
            if (!SPINS.isEmpty()) {
               long now = EffectClock.now();

               try {
                  boolean world = EffectCapture.pushSure(event);

                  int verts;
                  try {
                     verts = paint(new PoseStack(), event.getCamera().getPosition(), event.getPartialTick());
                  } finally {
                     EffectCapture.popSure();
                  }

                  if (verts > 0 && now - lastLog > 1000L) {
                     lastLog = now;
                     ShenlinMod.LOGGER.info("[shenlin] spin laser stage verts={} live={} world={}", new Object[]{verts, SPINS.size(), world});
                  }
               } catch (Throwable t) {
                  if (now - lastFailLog > 1000L) {
                     lastFailLog = now;
                     ShenlinMod.LOGGER.warn("[shenlin] spin laser draw failed: {}", t.toString());
                  }
               }
            }
         }
      }
   }

   public static int paintAtHandPass(PoseStack poseStack, Vec3 camera, float partialTick, long now) {
      EffectCapture.beginHandPass(poseStack);

      try {
         return paint(new PoseStack(), camera, partialTick);
      } finally {
         EffectCapture.endHandPass();
      }
   }

   public static int paint(PoseStack stack, Vec3 camera, float partialTick) {
      try {
         return paintInner(stack, camera, partialTick);
      } finally {
         Minecraft.getInstance().renderBuffers().bufferSource().endBatch(ModRenderTypes.GLOW);
      }
   }

   private static int paintInner(PoseStack stack, Vec3 camera, float partialTick) {
      if (!Upstream.MAID) {
         return 0;
      }

      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.player != null && !SPINS.isEmpty()) {
         long nowMs = EffectClock.now();
         Matrix4f pose = stack.last().pose();
         VertexConsumer consumer = mc.renderBuffers().bufferSource().getBuffer(ModRenderTypes.GLOW);
         int verts = 0;
         Iterator<Entry<Integer, CannonSpinEffect.Spin>> it = SPINS.entrySet().iterator();

         while (it.hasNext()) {
            Entry<Integer, CannonSpinEffect.Spin> entry = it.next();
            CannonSpinEffect.Spin spin = entry.getValue();
            double since = (nowMs - spin.bornMs) / 50.0;
            if (!(since < 0.0)) {
               if (since >= spin.totalTicks) {
                  it.remove();
               } else if (mc.level.getEntity(entry.getKey()) instanceof EntityMaid maid && maid.isAlive()) {
                  double tick = spin.startTick + since;
                  verts += one(consumer, pose, mc, maid, spin, tick, partialTick, camera, since);
               }
            }
         }

         mc.renderBuffers().bufferSource().endBatch(ModRenderTypes.GLOW);
         return verts;
      } else {
         return 0;
      }
   }

   private static int one(
      VertexConsumer consumer,
      Matrix4f pose,
      Minecraft mc,
      EntityMaid maid,
      CannonSpinEffect.Spin spin,
      double tick,
      float partialTick,
      Vec3 camera,
      double since
   ) {
      float grow = (float)clamp01(since / 5.0);
      float fade = (float)clamp01((spin.totalTicks - since) / 6.0);
      float alpha = Math.min(grow, fade);
      if (alpha <= 0.02F) {
         return 0;
      }

      Vec3 anchor = maid.getPosition(partialTick);
      int verts = 0;

      for (int index = 0; index < 3; index++) {
         Vec3 at = anchor.add(HoverCannon.spinPodPos(index, tick));
         Vec3 out = new Vec3(at.x - anchor.x, 0.0, at.z - anchor.z);
         if (!(out.lengthSqr() < 1.0E-6)) {
            Vec3 full = at.add(out.normalize().scale(HoverCannon.spinRange()));
            Vec3 shown = at.lerp(trace(mc, at, full), alpha);
            verts += pillar(consumer, pose, at.subtract(camera), tick, index, alpha);
            verts += beam(consumer, pose, at.subtract(camera), shown.subtract(camera), alpha);
         }
      }

      return verts;
   }

   private static Vec3 trace(Minecraft mc, Vec3 from, Vec3 to) {
      if (mc.level == null) {
         return to;
      }

      BlockHitResult hit = mc.level.clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, mc.player));
      return hit.getType() == Type.MISS ? to : hit.getLocation();
   }

   private static int pillar(VertexConsumer consumer, Matrix4f pose, Vec3 at, double tick, int index, float alpha) {
      float half = (float)(HoverCannon.spinPodHeight() * 0.5);
      double roll = tick * 0.16 + index * 2.1;
      Vec3 side = new Vec3(Math.cos(roll), 0.0, Math.sin(roll));
      Vec3 side2 = new Vec3(-Math.sin(roll), 0.0, Math.cos(roll));
      float breathe = (float)(0.88 + 0.12 * Math.sin(tick * 0.22 + index * 2.4));
      int verts = 0;
      verts += spindle(consumer, pose, at, side, side2, half * 1.06F, 0.3515F, 0.187F, 0.385F, 1.0F, alpha * 0.3F * breathe);
      verts += spindle(consumer, pose, at, side, side2, half * 0.92F, 0.19F, 0.34F, 0.7F, 1.0F, alpha * 0.62F * breathe);
      verts += spindle(consumer, pose, at, side, side2, half * 0.66F, 0.079799995F, 0.86F, 0.95F, 1.0F, alpha * 0.85F * breathe);
      return verts + haloRing(consumer, pose, at, side, side2, tick, alpha);
   }

   private static int spindle(
      VertexConsumer consumer, Matrix4f pose, Vec3 at, Vec3 side, Vec3 side2, float half, float waist, float r, float g, float b, float alpha
   ) {
      if (alpha <= 0.02F) {
         return 0;
      }

      Vec3 top = at.add(UP.scale(half));
      Vec3 bottom = at.subtract(UP.scale(half));
      Vec3[] belt = new Vec3[]{
         at.add(side.scale(waist)), at.add(side2.scale(waist)), at.subtract(side.scale(waist)), at.subtract(side2.scale(waist))
      };
      int verts = 0;

      for (int i = 0; i < 4; i++) {
         Vec3 a = belt[i];
         Vec3 c = belt[i + 1 & 3];
         verts += tri(consumer, pose, top, a, c, r, g, b, alpha);
         verts += tri(consumer, pose, bottom, c, a, r, g, b, alpha);
      }

      return verts;
   }

   private static int haloRing(VertexConsumer consumer, Matrix4f pose, Vec3 at, Vec3 side, Vec3 side2, double tick, float alpha) {
      int segments = 14;
      float radius = 0.33249998F;
      float thickness = 0.030399999F;
      int verts = 0;

      for (int i = 0; i < segments; i++) {
         double a0 = i * ((Math.PI * 2) / segments);
         double a1 = (i + 1) * ((Math.PI * 2) / segments);
         Vec3 d0 = side.scale(Math.cos(a0)).add(side2.scale(Math.sin(a0)));
         Vec3 d1 = side.scale(Math.cos(a1)).add(side2.scale(Math.sin(a1)));
         float flow = (float)(0.65 + 0.35 * Math.sin(a0 * 3.0 - tick * 0.42));
         verts += face(
            consumer,
            pose,
            at.add(d0.scale(radius)).add(UP.scale(thickness)),
            at.add(d0.scale(radius)).subtract(UP.scale(thickness)),
            at.add(d1.scale(radius)).subtract(UP.scale(thickness)),
            at.add(d1.scale(radius)).add(UP.scale(thickness)),
            0.62F,
            0.88F,
            1.0F,
            alpha * flow,
            d0
         );
      }

      return verts;
   }

   private static int beam(VertexConsumer consumer, Matrix4f pose, Vec3 from, Vec3 to, float alpha) {
      Vec3 axis = to.subtract(from);
      double length = axis.length();
      if (!(length < 0.06) && !(alpha <= 0.02F)) {
         Vec3 unit = axis.scale(1.0 / length);
         int segments = (int)Math.max(3.0, Math.min(24.0, length / 1.2));
         float width = HoverCannon.spinBeamWidth();
         int verts = 0;

         for (int i = 0; i < segments; i++) {
            Vec3 a = from.add(axis.scale((double)i / segments));
            Vec3 b = from.add(axis.scale((double)(i + 1) / segments));
            verts += band(consumer, pose, a, b, unit, width, 0.385F, 0.574F, 1.0F, alpha * 0.22F);
            verts += band(consumer, pose, a, b, unit, width * 0.52F, 0.55F, 0.82F, 1.0F, alpha * 0.48F);
            verts += band(consumer, pose, a, b, unit, width * 0.22F, 0.92F, 0.98F, 1.0F, alpha * 0.82F);
         }

         return verts;
      } else {
         return 0;
      }
   }

   private static int band(VertexConsumer consumer, Matrix4f pose, Vec3 a, Vec3 b, Vec3 unit, float width, float r, float g, float bl, float alpha) {
      if (alpha <= 0.02F) {
         return 0;
      }

      Vec3 view = a.add(b).scale(0.5).scale(-1.0);
      if (view.lengthSqr() < 1.0E-6) {
         return 0;
      }

      Vec3 side = unit.cross(view.normalize());
      if (side.lengthSqr() < 1.0E-6) {
         return 0;
      }

      side = side.normalize();
      Vec3 a1 = a.add(side.scale(width));
      Vec3 a2 = a.subtract(side.scale(width));
      Vec3 b1 = b.add(side.scale(width));
      Vec3 b2 = b.subtract(side.scale(width));
      vert(consumer, pose, a1, r, g, bl, alpha);
      vert(consumer, pose, a2, r, g, bl, alpha);
      vert(consumer, pose, b2, r, g, bl, alpha);
      vert(consumer, pose, a1, r, g, bl, alpha);
      vert(consumer, pose, b2, r, g, bl, alpha);
      vert(consumer, pose, b1, r, g, bl, alpha);
      return 6;
   }

   private static float shade(Vec3 n) {
      return 0.58F + 0.42F * (float)(n.y * 0.5 + 0.5);
   }

   private static int tri(VertexConsumer consumer, Matrix4f pose, Vec3 a, Vec3 b, Vec3 c, float r, float g, float bl, float alpha) {
      Vec3 n = b.subtract(a).cross(c.subtract(a));
      if (n.lengthSqr() < 1.0E-12) {
         return 0;
      }

      n = n.normalize();
      Vec3 mid = a.add(b).add(c).scale(0.3333333333333333);
      if (n.dot(mid) >= 0.0) {
         return 0;
      }

      float k = shade(n);
      vert(consumer, pose, a, r * k, g * k, bl * k, alpha);
      vert(consumer, pose, b, r * k, g * k, bl * k, alpha);
      vert(consumer, pose, c, r * k, g * k, bl * k, alpha);
      return 3;
   }

   private static int face(VertexConsumer consumer, Matrix4f pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d, float r, float g, float bl, float alpha, Vec3 normal) {
      if (!(alpha <= 0.02F) && !(normal.lengthSqr() < 1.0E-12)) {
         Vec3 mid = a.add(b).add(c).add(d).scale(0.25);
         if (normal.dot(mid) >= 0.0) {
            return 0;
         }

         float k = shade(normal);
         vert(consumer, pose, a, r * k, g * k, bl * k, alpha);
         vert(consumer, pose, b, r * k, g * k, bl * k, alpha);
         vert(consumer, pose, c, r * k, g * k, bl * k, alpha);
         vert(consumer, pose, a, r * k, g * k, bl * k, alpha);
         vert(consumer, pose, c, r * k, g * k, bl * k, alpha);
         vert(consumer, pose, d, r * k, g * k, bl * k, alpha);
         return 6;
      } else {
         return 0;
      }
   }

   private static void vert(VertexConsumer consumer, Matrix4f pose, Vec3 p, float r, float g, float b, float alpha) {
      consumer.vertex(pose, (float)p.x, (float)p.y, (float)p.z).color(r, g, b, alpha).endVertex();
   }

   private static double clamp01(double v) {
      return v < 0.0 ? 0.0 : Math.min(v, 1.0);
   }

   private static final class Spin {
      private final long startTick;
      private final int totalTicks;
      private final long bornMs;

      private Spin(long startTick, int totalTicks, long bornMs) {
         this.startTick = startTick;
         this.totalTicks = totalTicks;
         this.bornMs = bornMs;
      }
   }
}

