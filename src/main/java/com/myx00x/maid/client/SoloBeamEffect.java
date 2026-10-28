package com.myx00x.maid.client;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.compat.Upstream;
import com.myx00x.maid.mob.RelicWardenEntity;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT, bus = Bus.FORGE)
public final class SoloBeamEffect {
   private static final float ORB_RADIUS = 0.62F;
   private static final double ORB_ABOVE_RATIO = 0.9;
   private static final double STAR_REACH = 1.8;
   private static final float EMIT_STAR_RADIUS = 2.86F;
   private static final float EMIT_STAR_WIDTH = 2.2F;
   private static final int EMIT_STAR_TICKS = 100;
   private static final float[] EMIT_STAR_RGB = new float[]{0.36F, 0.66F, 1.0F};
   private static final double STAR_ARM = 0.13;
   private static final double HIT_STAR_REACH = 0.95;
   private static final float STAR_R = 0.36F;
   private static final float STAR_G = 0.66F;
   private static final float STAR_B = 1.0F;
   private static final float BEAM_RADIUS = 0.20666666F;
   private static final double ORB_SHRINK = 0.3333333333333333;
   private static final int BEAM_LAYERS = 12;
   private static final double SOLO_ORB_FROM = 12.0;
   private static final double ORB_RISE = 0.86;
   private static final double BEAM_RISE_TICKS = 4.0;
   private static final float FADE_THIN = 0.28F;
   private static final double BEAM_STEP = 1.2;
   private static final int BEAM_MAX_SEGMENTS = 40;
   private static final float ORB_R = 0.34F;
   private static final float ORB_G = 0.7F;
   private static final float ORB_B = 1.0F;
   private static final float OUT_LOW_R = 0.14F;
   private static final float OUT_LOW_G = 0.36F;
   private static final float OUT_HIGH_R = 0.46F;
   private static final float OUT_HIGH_G = 0.72F;
   private static final float IN_R = 0.86F;
   private static final float IN_G = 0.96F;
   private static final float INJECT_WIDTH = 0.062F;
   private static final float INJECT_ALPHA = 0.52F;
   private static final double INJECT_LAG = 3.0;
   private static final double INJECT_RISE = 8.0;
   private static final double FLASH_TICKS = 2.0;
   private static final double FLASH_NEAR = 0.6;
   private static final float FLASH_SIZE = 3.0F;
   private static final float FLASH_DARK = 1.0F;
   private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
   private static final Vec3 EAST = new Vec3(1.0, 0.0, 0.0);
   private static final Map<Integer, SoloBeamEffect.Solo> LIVE = new ConcurrentHashMap<>();
   private static long lastLog;
   private static long lastFailLog;

   private SoloBeamEffect() {
   }

   public static void spawn(
      int maidId, int targetId, long startTick, int chargeTicks, int fireTicks, int fadeTicks, double x, double y, double z, double soundRange, double soundFar
   ) {
      if (chargeTicks + fireTicks + fadeTicks <= 0) {
         LIVE.remove(maidId);
         SoloBeamSound.stop(maidId);
      } else {
         SoloBeamEffect.Solo alive = LIVE.get(maidId);
         if (chargeTicks <= 0 && alive != null && alive.targetId == targetId) {
            alive.retime(fireTicks, EffectClock.now());
         } else {
            long born = EffectClock.now();
            LIVE.put(maidId, new SoloBeamEffect.Solo(targetId, startTick, chargeTicks, fireTicks, fadeTicks, new Vec3(x, y, z), born));
            SoloBeamSound.play(maidId, soundRange, soundFar);
            ShenlinMod.LOGGER
               .info(
                  "[shenlin] solo beam born target={} start={} charge={} fire={} fade={} live={} clock={}",
                  new Object[]{targetId, startTick, chargeTicks, fireTicks, fadeTicks, LIVE.size(), born}
               );
         }
      }
   }

   public static boolean pending() {
      return !LIVE.isEmpty();
   }

   public static int count() {
      return LIVE.size();
   }

   public static int stageOf(int maidId) {
      SoloBeamEffect.Solo solo = LIVE.get(maidId);
      if (solo == null) {
         return -1;
      } else {
         double tick = (EffectClock.now() - solo.bornMs) / 50.0;
         double charge = Math.max(1.0, solo.chargeTicks);
         if (tick >= charge + Math.max(1.0, solo.fireTicks)) {
            return 2;
         } else {
            return tick >= charge ? 1 : 0;
         }
      }
   }

   public static void dropAll() {
      LIVE.clear();
   }

   @SubscribeEvent
   public static void onRenderStage(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_PARTICLES) {
         if (!EffectChannel.handPassAlive(EffectClock.now())) {
            if (!LIVE.isEmpty()) {
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
                     ShenlinMod.LOGGER.info("[shenlin] solo beam stage verts={} live={} world={}", new Object[]{verts, LIVE.size(), world});
                  }
               } catch (Throwable t) {
                  if (now - lastFailLog > 1000L) {
                     lastFailLog = now;
                     ShenlinMod.LOGGER.warn("[shenlin] solo beam draw failed: {}", t.toString());
                  }
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
      try {
         return paintInner(stack, camera, partialTick, now);
      } finally {
         Minecraft.getInstance().renderBuffers().bufferSource().endBatch(ModRenderTypes.GLOW);
         Minecraft.getInstance().renderBuffers().bufferSource().endBatch(ModRenderTypes.GLOW_ALPHA);
      }
   }

   private static int paintInner(PoseStack stack, Vec3 camera, float partialTick, long now) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.player != null && !LIVE.isEmpty()) {
         Matrix4f pose = stack.last().pose();
         VertexConsumer consumer = mc.renderBuffers().bufferSource().getBuffer(ModRenderTypes.GLOW);
         int verts = 0;
         boolean flash = false;
         Iterator<Entry<Integer, SoloBeamEffect.Solo>> it = LIVE.entrySet().iterator();

         while (it.hasNext()) {
            Entry<Integer, SoloBeamEffect.Solo> entry = it.next();
            SoloBeamEffect.Solo solo = entry.getValue();
            double since = (now - solo.bornMs) / 50.0;
            if (!(since < 0.0)) {
               if (since >= solo.totalTicks()) {
                  it.remove();
               } else {
                  Entity caster = mc.level.getEntity(entry.getKey());
                  if (caster != null && caster.isAlive()) {
                     if (flashing(solo, since)) {
                        flash = true;
                     }

                     verts += one(consumer, pose, mc, caster, solo, since, partialTick, camera);
                  }
               }
            }
         }

         if (flash) {
            verts += flush(consumer, pose, mc);
         }

         mc.renderBuffers().bufferSource().endBatch(ModRenderTypes.GLOW);
         if (flash) {
            verts += darkBeams(pose, mc, partialTick, camera, now);
         }

         return verts;
      } else {
         return 0;
      }
   }

   private static int one(
      VertexConsumer consumer, Matrix4f pose, Minecraft mc, Entity caster, SoloBeamEffect.Solo solo, double since, float partialTick, Vec3 camera
   ) {
      if (!isMaid(caster) && !solo.starSpawned) {
         solo.starSpawned = true;
         Vec3 starAt = orbPoint(caster, partialTick);
         StarEffect.spawn(starAt.x, starAt.y, starAt.z, 2.86F, 100, true, caster.getId(), 2.2F, EMIT_STAR_RGB);
      }

      double charge = Math.max(1.0, solo.chargeTicks);
      double fire = Math.max(1.0, solo.fireTicks);
      double fade = Math.max(0.0, solo.fadeTicks);
      double fireEnd = charge + fire;
      boolean firing = since >= charge;
      float alive = 1.0F;
      double size;
      if (!firing) {
         double orbFrom = Math.min(12.0, Math.max(1.0, charge - 2.0));
         if (since < orbFrom) {
            return 0;
         }

         double t = (since - orbFrom) / Math.max(1.0, charge - orbFrom);
         size = smooth(Math.min(1.0, t / 0.86));
      } else {
         size = 0.3333333333333333;
         if (since > fireEnd) {
            float t = (float)clamp01((since - fireEnd) / Math.max(1.0, fade));
            size = 0.3333333333333333 * (1.0 - 0.85 * t);
            alive = 1.0F - t;
         }
      }

      Vec3 orb = orbPoint(caster, partialTick);
      Vec3 rel = orb.subtract(camera);
      int verts = 0;
      if (size > 0.0) {
         if (isMaid(caster)) {
            verts += orbGlow(consumer, pose, rel, size, since, alive);
         }

         float born = (float)clamp01((since - 12.0 - 3.0) / 8.0);
         if (born > 0.0F) {
            verts += inject(consumer, pose, caster, orb, camera, born * alive, since);
         }
      }

      if (firing) {
         Vec3 edge = targetEdge(mc, caster, solo, partialTick);
         double grow = smooth(clamp01((since - charge) / 4.0));
         Vec3 shown = orb.lerp(edge, grow);
         float width = 0.20666666F * (1.0F - 0.72F * (1.0F - alive));
         verts += beam(consumer, pose, rel, shown.subtract(camera), width, alive, since, false);
      }

      return verts;
   }

   private static int blueStar(VertexConsumer consumer, Matrix4f pose, Vec3 rel, double reach, float fade, double since, boolean pulse) {
      if (!(reach <= 0.0) && !(fade <= 0.02F)) {
         Vec3 view = rel.scale(-1.0);
         if (view.lengthSqr() < 1.0E-6) {
            return 0;
         }

         view = view.normalize();
         Vec3 tsrg2 = view.cross(UP);
         if (tsrg2.lengthSqr() < 1.0E-6) {
            tsrg2 = view.cross(EAST);
         }

         tsrg2 = tsrg2.normalize();
         Vec3 up = tsrg2.cross(view).normalize();
         float beat = pulse ? (float)(0.72 + 0.28 * Math.sin(since * 1.9)) : (float)(0.92 + 0.08 * Math.sin(since * 0.9));
         float alpha = fade * beat;
         double roll = since * 0.14;
         Vec3 a1 = tsrg2.scale(Math.cos(roll)).add(up.scale(Math.sin(roll)));
         Vec3 a2 = tsrg2.scale(-Math.sin(roll)).add(up.scale(Math.cos(roll)));
         double w = reach * 0.13;
         int verts = 0;
         verts += diamond(
            consumer,
            pose,
            rel.add(a1.scale(reach)),
            rel.add(a2.scale(w)),
            rel.subtract(a1.scale(reach)),
            rel.subtract(a2.scale(w)),
            0.36F,
            0.66F,
            1.0F,
            0.8F * alpha
         );
         verts += diamond(
            consumer,
            pose,
            rel.add(a2.scale(reach)),
            rel.subtract(a1.scale(w)),
            rel.subtract(a2.scale(reach)),
            rel.add(a1.scale(w)),
            0.36F,
            0.66F,
            1.0F,
            0.8F * alpha
         );
         return verts + quad(consumer, pose, rel, tsrg2, up, (float)(reach * 0.3), 0.86F, 0.95F, 1.0F, 0.7F * alpha);
      } else {
         return 0;
      }
   }

   private static Vec3 targetEdge(Minecraft mc, Entity caster, SoloBeamEffect.Solo solo, float partialTick) {
      if ((mc.level != null && solo.targetId > 0 ? mc.level.getEntity(solo.targetId) : null) instanceof LivingEntity living && living.isAlive()) {
         Vec3 mid = living.getPosition(partialTick).add(0.0, living.getBbHeight() * 0.5, 0.0);
         Vec3 orb = orbPoint(caster, partialTick);
         return living.getBoundingBox().clip(orb, mid).orElse(mid);
      } else {
         return solo.fallback;
      }
   }

   private static int orbGlow(VertexConsumer consumer, Matrix4f pose, Vec3 at, double size, double since, float fade) {
      Vec3 view = at.scale(-1.0);
      if (!(view.lengthSqr() < 1.0E-6) && !(size <= 0.0)) {
         view = view.normalize();
         Vec3 tsrg2 = view.cross(UP);
         if (tsrg2.lengthSqr() < 1.0E-6) {
            tsrg2 = view.cross(EAST);
         }

         tsrg2 = tsrg2.normalize();
         Vec3 up = tsrg2.cross(view).normalize();
         float k = (float)size;
         float beat = (float)(0.9 + 0.1 * Math.sin(since * 0.85));
         int verts = 0;
         verts += quad(consumer, pose, at, tsrg2, up, 0.62F * (0.2F + 0.8F * k) * beat, 0.187F, 0.385F, 1.0F, (0.26F + 0.3F * k) * fade);
         verts += quad(consumer, pose, at, tsrg2, up, 0.62F * (0.42F + 0.58F * k) * beat, 0.34F, 0.7F, 1.0F, (0.2F + 0.36F * k) * fade);
         verts += quad(consumer, pose, at, tsrg2, up, 0.62F * (0.62F + 0.38F * k) * beat, 0.88F, 0.96F, 1.0F, (0.14F + 0.42F * k) * fade);
         return verts + orbRing(consumer, pose, at, tsrg2, up, k, since, fade);
      } else {
         return 0;
      }
   }

   private static int orbRing(VertexConsumer consumer, Matrix4f pose, Vec3 at, Vec3 tsrg2, Vec3 up, float k, double since, float fade) {
      int segments = 18;
      float radius = 0.62F * (0.55F + 0.45F * k) * 1.18F;
      float half = 0.0341F;
      double roll = since * 0.18;
      int verts = 0;

      for (int i = 0; i < segments; i++) {
         double a0 = roll + i * ((Math.PI * 2) / segments);
         double a1 = roll + (i + 1) * ((Math.PI * 2) / segments);
         Vec3 d0 = tsrg2.scale(Math.cos(a0)).add(up.scale(Math.sin(a0)));
         Vec3 d1 = tsrg2.scale(Math.cos(a1)).add(up.scale(Math.sin(a1)));
         float flow = (float)(0.6 + 0.4 * Math.sin(a0 * 3.0 - since * 0.38));
         verts += face(
            consumer,
            pose,
            at.add(d0.scale(radius)).add(UP.scale(half)),
            at.add(d0.scale(radius)).subtract(UP.scale(half)),
            at.add(d1.scale(radius)).subtract(UP.scale(half)),
            at.add(d1.scale(radius)).add(UP.scale(half)),
            0.6F,
            0.86F,
            1.0F,
            (0.2F + 0.55F * k) * flow * fade,
            d0
         );
      }

      return verts;
   }

   private static int inject(VertexConsumer consumer, Matrix4f pose, Entity caster, Vec3 orb, Vec3 camera, float fade, double since) {
      if (Upstream.MAID && caster instanceof EntityMaid) {
         if (fade <= 0.02F) {
            return 0;
         }

         Vec3 hub = orb.subtract(camera);
         int verts = 0;

         for (int pod = 0; pod < 3; pod++) {
            Vec3 at = OrbitCannonEffect.podWorld(caster.getId(), pod);
            if (at != null) {
               float flow = (float)(0.62 + 0.38 * Math.sin(since * 0.3 + pod * 2.1));
               verts += vein(consumer, pose, at.subtract(camera), hub, 0.062F * flow, 0.52F * flow * fade);
            }
         }

         return verts;
      } else {
         return 0;
      }
   }

   private static int vein(VertexConsumer consumer, Matrix4f pose, Vec3 a, Vec3 b, float width, float alpha) {
      Vec3 axis = b.subtract(a);
      double length = axis.length();
      if (!(length < 0.08) && !(alpha <= 0.02F)) {
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
         int verts = 0;
         int segments = 5;

         for (int i = 0; i < segments; i++) {
            double t0 = (double)i / segments;
            double t1 = (double)(i + 1) / segments;
            verts += spine(
               consumer,
               pose,
               a.add(axis.scale(t0)),
               a.add(axis.scale(t1)),
               side,
               width * (float)(1.0 - 0.6 * t0),
               width * (float)(1.0 - 0.6 * t1),
               alpha
            );
         }

         return verts;
      } else {
         return 0;
      }
   }

   private static int spine(VertexConsumer consumer, Matrix4f pose, Vec3 a, Vec3 b, Vec3 side, float wa, float wb, float alpha) {
      Vec3 a1 = a.add(side.scale(wa));
      Vec3 a2 = a.subtract(side.scale(wa));
      Vec3 b1 = b.add(side.scale(wb));
      Vec3 b2 = b.subtract(side.scale(wb));
      vert(consumer, pose, a1, 0.3F, 0.66F, 1.0F, alpha);
      vert(consumer, pose, a2, 0.3F, 0.66F, 1.0F, alpha);
      vert(consumer, pose, b2, 0.3F, 0.66F, 1.0F, alpha);
      vert(consumer, pose, a1, 0.3F, 0.66F, 1.0F, alpha);
      vert(consumer, pose, b2, 0.3F, 0.66F, 1.0F, alpha);
      vert(consumer, pose, b1, 0.3F, 0.66F, 1.0F, alpha);
      return 6;
   }

   private static int beam(VertexConsumer consumer, Matrix4f pose, Vec3 from, Vec3 to, float width, float fade, double since, boolean dark) {
      Vec3 axis = to.subtract(from);
      double length = axis.length();
      if (!(length < 0.08) && !(fade <= 0.02F)) {
         Vec3 unit = axis.scale(1.0 / length);
         int segments = (int)Math.max(3.0, Math.min(40.0, length / 1.2));
         int layers = dark ? 1 : 12;
         double hue = 0.5 + 0.5 * Math.sin(since * 0.22);
         float outR = (float)(0.14F + 0.32F * hue);
         float outG = (float)(0.36F + 0.36F * hue);
         int verts = 0;

         for (int i = 0; i < segments; i++) {
            Vec3 a = from.add(axis.scale((double)i / segments));
            Vec3 b = from.add(axis.scale((double)(i + 1) / segments));

            for (int layer = 0; layer < layers; layer++) {
               float t = dark ? 0.0F : layer / 11.0F;
               float w = width * (1.0F - 0.82F * t);
               float r = dark ? 0.0F : outR + (0.86F - outR) * t * t;
               float g = dark ? 0.0F : outG + (0.96F - outG) * t * t;
               float bl = dark ? 0.0F : 1.0F;
               float alpha = dark ? fade : fade * (0.055F + 0.62F * t * t * t);
               verts += band(consumer, pose, a, b, unit, w, r, g, bl, alpha);
            }
         }

         return verts;
      } else {
         return 0;
      }
   }

   private static boolean flashing(SoloBeamEffect.Solo solo, double since) {
      double into = since - Math.max(1.0, solo.chargeTicks);
      return into >= 0.0 && into < 2.0;
   }

   private static int flush(VertexConsumer consumer, Matrix4f pose, Minecraft mc) {
      Vec3 look = SafeCamera.look(mc);
      if (look.lengthSqr() < 1.0E-6) {
         look = new Vec3(0.0, 0.0, -1.0);
      }

      Vec3 fwd = look.normalize();
      Vec3 tsrg2 = fwd.cross(UP);
      if (tsrg2.lengthSqr() < 1.0E-6) {
         tsrg2 = fwd.cross(EAST);
      }

      tsrg2 = tsrg2.normalize();
      Vec3 up = tsrg2.cross(fwd).normalize();
      return quad(consumer, pose, fwd.scale(0.6), tsrg2, up, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F);
   }

   private static int darkBeams(Matrix4f pose, Minecraft mc, float partialTick, Vec3 camera, long now) {
      VertexConsumer consumer = mc.renderBuffers().bufferSource().getBuffer(ModRenderTypes.GLOW_ALPHA);
      int verts = 0;

      for (Entry<Integer, SoloBeamEffect.Solo> entry : LIVE.entrySet()) {
         SoloBeamEffect.Solo solo = entry.getValue();
         double since = (now - solo.bornMs) / 50.0;
         if (!(since < 0.0) && !(since >= solo.totalTicks()) && flashing(solo, since)) {
            Entity caster = mc.level == null ? null : mc.level.getEntity(entry.getKey());
            if (caster != null && caster.isAlive()) {
               verts += darkBeam(consumer, pose, mc, caster, solo, since, partialTick, camera);
            }
         }
      }

      mc.renderBuffers().bufferSource().endBatch(ModRenderTypes.GLOW_ALPHA);
      return verts;
   }

   private static int darkBeam(
      VertexConsumer consumer, Matrix4f pose, Minecraft mc, Entity caster, SoloBeamEffect.Solo solo, double since, float partialTick, Vec3 camera
   ) {
      Vec3 orb = orbPoint(caster, partialTick);
      Vec3 edge = targetEdge(mc, caster, solo, partialTick);
      double grow = smooth(clamp01((since - Math.max(1.0, solo.chargeTicks)) / 4.0));
      Vec3 shown = orb.lerp(edge, grow);
      return beam(consumer, pose, orb.subtract(camera), shown.subtract(camera), 0.20666666F, 1.0F, since, true);
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

   private static int diamond(VertexConsumer consumer, Matrix4f pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d, float r, float g, float bl, float alpha) {
      if (alpha <= 0.02F) {
         return 0;
      }

      vert(consumer, pose, a, r, g, bl, alpha);
      vert(consumer, pose, b, r, g, bl, alpha);
      vert(consumer, pose, c, r, g, bl, alpha);
      vert(consumer, pose, a, r, g, bl, alpha);
      vert(consumer, pose, c, r, g, bl, alpha);
      vert(consumer, pose, d, r, g, bl, alpha);
      return 6;
   }

   private static int quad(VertexConsumer consumer, Matrix4f pose, Vec3 at, Vec3 tsrg2, Vec3 up, float size, float r, float g, float b, float alpha) {
      if (!(alpha <= 0.02F) && !(size <= 0.0F)) {
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

   private static Vec3 orbPoint(Entity caster, float partialTick) {
      if (Upstream.MAID && caster instanceof EntityMaid) {
         return caster.getPosition(partialTick).add(0.0, caster.getBbHeight() + 1.5, 0.0);
      } else {
         return caster instanceof RelicWardenEntity warden
            ? warden.airEmitPointAt(partialTick)
            : caster.getPosition(partialTick).add(0.0, caster.getBbHeight() * 0.9, 0.0);
      }
   }

   private static boolean isMaid(Entity caster) {
      return Upstream.MAID && caster instanceof EntityMaid;
   }

   private static double smooth(double t) {
      return t * t * (3.0 - 2.0 * t);
   }

   private static final class Solo {
      private final int targetId;
      private final long startTick;
      private final int chargeTicks;
      private int fireTicks;
      private final int fadeTicks;
      private final Vec3 fallback;
      private final long bornMs;
      private boolean starSpawned;

      private Solo(int targetId, long startTick, int chargeTicks, int fireTicks, int fadeTicks, Vec3 fallback, long bornMs) {
         this.targetId = targetId;
         this.startTick = startTick;
         this.chargeTicks = chargeTicks;
         this.fireTicks = fireTicks;
         this.fadeTicks = fadeTicks;
         this.fallback = fallback;
         this.bornMs = bornMs;
      }

      private void retime(int remain, long now) {
         double since = (now - this.bornMs) / 50.0;
         int burned = (int)Math.max(0.0, since - Math.max(1, this.chargeTicks));
         this.fireTicks = Math.max(1, burned + Math.max(1, remain));
      }

      private double totalTicks() {
         return Math.max(1.0, this.chargeTicks) + Math.max(1.0, this.fireTicks) + Math.max(0.0, this.fadeTicks);
      }
   }
}

