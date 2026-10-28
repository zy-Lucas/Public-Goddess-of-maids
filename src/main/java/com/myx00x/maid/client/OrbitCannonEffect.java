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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
public final class OrbitCannonEffect {
   private static final Map<Integer, OrbitCannonEffect.Live> LIVE = new ConcurrentHashMap<>();
   private static final Map<Integer, OrbitCannonEffect.Form> FORMS = new ConcurrentHashMap<>();
   private static final long KEEP_MS = 3000L;
   private static final long FOLD_MS = 850L;
   private static final long UNFOLD_MS = 420L;
   private static final double SOLO_BLINK_TICKS = 7.0;
   private static final double CORE_BURST = 0.9;
   private static final double CORE_BURST_TAU = 1.8;
   private static final float NOSE = 0.19F;
   private static final float TAIL = 0.115F;
   private static final float WAIST = 0.115F;
   private static final float RING_R = 0.225F;
   private static final float RING_HALF = 0.026F;
   private static final int RING_SEGMENTS = 36;
   private static final int BODY_RIBS = 12;
   private static final int MUZZLE_SEGMENTS = 8;
   private static final float MUZZLE_OUT = 0.095F;
   private static final float MUZZLE_R = 0.042F;
   private static final float MUZZLE_ALPHA = 0.38F;
   private static final float MOUTH_BASE = 0.1F;
   private static final int TAIL_RINGS = 4;
   private static final float BODY_R = 0.2F;
   private static final float BODY_G = 0.48F;
   private static final float BODY_B = 1.0F;
   private static final float RING_CR = 0.34F;
   private static final float RING_CG = 0.72F;
   private static final float RING_CB = 1.0F;
   private static final float MUZ_R = 0.86F;
   private static final float MUZ_G = 0.95F;
   private static final float MUZ_B = 1.0F;
   private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
   private static final Vec3 EAST = new Vec3(1.0, 0.0, 0.0);
   private static long lastLog;
   private static long lastVoidLog;
   private static long lastFailLog;

   private OrbitCannonEffect() {
   }

   public static void set(int entityId, int state, int targetId, int idleMs) {
      if (state <= 0) {
         LIVE.remove(entityId);
         FORMS.remove(entityId);
         FormRaySound.stop(entityId);
      } else {
         long now = EffectClock.now();
         OrbitCannonEffect.Live live = LIVE.get(entityId);
         if (live == null) {
            live = new OrbitCannonEffect.Live();
            live.stateSince = now;
            live.unfoldAt = now;
            LIVE.put(entityId, live);
         } else if (live.state != state || live.targetId != targetId) {
            live.stateSince = now;
            live.foldAt = -1L;
            if (live.folded) {
               live.folded = false;
               live.unfoldAt = now;
            }
         }

         live.until = now + 3000L;
         live.state = state;
         live.targetId = targetId;
         live.idleMs = idleMs;
         live.firing = state == 2;
      }
   }

   public static void form(
      int entityId,
      double ax,
      double ay,
      double az,
      double aimX,
      double aimZ,
      long startTick,
      int chargeTicks,
      int totalTicks,
      int targetId,
      float soundRange,
      float soundFar
   ) {
      FORMS.put(
         entityId, new OrbitCannonEffect.Form(new Vec3(ax, ay, az), new Vec3(aimX, 0.0, aimZ), startTick, EffectClock.now(), chargeTicks, totalTicks, targetId)
      );
      FormRaySound.play(entityId, targetId, ax, ay, az, soundRange, soundFar);
   }

   public static boolean pending() {
      for (OrbitCannonEffect.Live live : LIVE.values()) {
         if (!live.folded) {
            return true;
         }
      }

      return false;
   }

   public static int count() {
      return LIVE.size();
   }

   public static Vec3 podWorld(int maidId, int pod) {
      OrbitCannonEffect.Live live = LIVE.get(maidId);
      return live != null && !live.folded && live.pods != null && pod >= 0 && pod < live.pods.length ? live.pods[pod] : null;
   }

   public static void dropAll() {
      LIVE.clear();
      FORMS.clear();
      FormRaySound.dropAll();
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
                     ShenlinMod.LOGGER.info("[shenlin] orbit stage verts={} live={} forms={} world={}", new Object[]{verts, LIVE.size(), FORMS.size(), world});
                  }
               } catch (Throwable t) {
                  if (now - lastFailLog > 1000L) {
                     lastFailLog = now;
                     ShenlinMod.LOGGER.warn("[shenlin] orbit cannon draw failed: {}", t.toString());
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
      }
   }

   private static int paintInner(PoseStack stack, Vec3 camera, float partialTick, long now) {
      if (!Upstream.MAID) {
         return 0;
      }

      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.player != null) {
         Iterator<Entry<Integer, OrbitCannonEffect.Live>> it = LIVE.entrySet().iterator();

         while (it.hasNext()) {
            Entry<Integer, OrbitCannonEffect.Live> entry = it.next();
            if (now > entry.getValue().until) {
               FORMS.remove(entry.getKey());
               it.remove();
            }
         }

         if (LIVE.isEmpty()) {
            FORMS.clear();
            return 0;
         }

         double spin = (float)mc.level.getGameTime() + partialTick;
         boolean compat = ShaderPack.inUse();
         Matrix4f pose = stack.last().pose();
         VertexConsumer consumer = mc.renderBuffers().bufferSource().getBuffer(ModRenderTypes.GLOW);
         int verts = 0;

         for (Entry<Integer, OrbitCannonEffect.Live> entry : LIVE.entrySet()) {
            OrbitCannonEffect.Live live = entry.getValue();
            if (!live.folded && mc.level.getEntity(entry.getKey()) instanceof EntityMaid maid && maid.isAlive()) {
               boolean soloRing = live.state == 3;
               double soloInto = soloRing ? (now - live.stateSince) / 50.0 : -1.0;
               boolean soloVanishing = soloRing && soloInto < 7.0;
               Entity host = !soloRing && live.targetId > 0 ? mc.level.getEntity(live.targetId) : null;
               boolean chasing = host != null && host.isAlive();
               Vec3 hostPos = chasing ? host.getPosition(partialTick) : maid.getPosition(partialTick);
               double hostWidth = chasing ? host.getBbWidth() : maid.getBbWidth();
               double hostHeight = chasing ? host.getBbHeight() : maid.getBbHeight();
               Vec3 goal = HoverCannon.ringCenter(hostPos, hostHeight);
               if (soloRing) {
                  live.anchor = goal;
               } else {
                  live.anchor = live.anchor == null ? goal : HoverCannon.approach(live.anchor, goal, now - live.anchorAt);
               }

               live.anchorAt = now;
               Vec3 center = live.anchor;
               double radius = soloRing ? HoverCannon.soloRingRadius() : (chasing ? HoverCannon.chaseRadius(hostWidth) : HoverCannon.idleRadius(hostWidth));
               OrbitCannonEffect.Form form = FORMS.get(entry.getKey());
               boolean soloTakeover = form != null && soloRing && soloVanishing;
               if (form != null && !soloTakeover) {
                  double since = (now - form.bornMs) / 50.0;
                  if (!(since >= form.totalTicks)) {
                     verts += formation(consumer, pose, form, since, maid, live, center, radius, spin, compat, camera, partialTick);
                     continue;
                  }

                  FORMS.remove(entry.getKey());
               }

               if (soloTakeover) {
                  Vec3 slotBase = overhead(mc, form, partialTick);
                  if (live.pods == null) {
                     live.pods = new Vec3[3];
                  }

                  for (int pod = 0; pod < 3; pod++) {
                     live.pods[pod] = HoverCannon.formationSlot(pod, slotBase, form.aim);
                  }

                  FORMS.remove(entry.getKey());
               }

               float grow = 1.0F;
               float alpha = 1.0F;
               if (live.state == 1 && live.idleMs > 0L && now - live.stateSince > live.idleMs) {
                  if (live.foldAt < 0L) {
                     live.foldAt = now;
                  }

                  double t = (now - live.foldAt) / 850.0;
                  if (t >= 1.0) {
                     live.folded = true;
                     continue;
                  }

                  center = live.anchor.lerp(chest(maid, partialTick), t);
                  grow = (float)(1.0 - t);
                  alpha = (float)(1.0 - t * t);
               } else if (live.unfoldAt > 0L) {
                  double t = (now - live.unfoldAt) / 420.0;
                  if (t >= 1.0) {
                     live.unfoldAt = -1L;
                  } else {
                     center = chest(maid, partialTick).lerp(live.anchor, t);
                     grow = (float)t;
                     alpha = (float)t;
                  }
               }

               for (int pod = 0; pod < 3; pod++) {
                  boolean stillFiring = live.firing;
                  float podGrow = grow;
                  float podAlpha = alpha;
                  Vec3 atWorld;
                  if (soloVanishing) {
                     Vec3 was = live.pods == null ? null : live.pods[pod];
                     if (was == null) {
                        continue;
                     }

                     atWorld = was;
                     double t = Math.min(1.0, soloInto / 6.0);
                     podGrow = (float)(1.0 - t);
                     podAlpha = (float)(1.0 - t * t);
                  } else {
                     atWorld = center.add(
                        soloRing
                           ? HoverCannon.soloPodOffset(pod, spin, maid.getUUID(), radius)
                           : HoverCannon.podOffset(pod, spin, maid.getUUID(), stillFiring, radius)
                     );
                  }

                  if (!soloVanishing) {
                     if (live.pods == null) {
                        live.pods = new Vec3[3];
                     }

                     live.pods[pod] = atWorld;
                  }

                  Vec3 aim;
                  if (chasing) {
                     aim = hostPos.add(0.0, hostHeight * 0.5, 0.0).subtract(atWorld);
                  } else if (soloRing) {
                     aim = maid.getPosition(partialTick).add(0.0, maid.getBbHeight() + 1.5, 0.0).subtract(atWorld);
                  } else {
                     aim = new Vec3(atWorld.x - center.x, 0.0, atWorld.z - center.z);
                  }

                  if (!compat || !occluded(mc, camera, atWorld)) {
                     verts += pod(
                        consumer,
                        pose,
                        atWorld.subtract(camera),
                        aim,
                        spin,
                        pod,
                        HoverCannon.firePulse(pod, spin, maid.getUUID(), stillFiring),
                        podGrow,
                        podAlpha
                     );
                  }
               }
            }
         }

         mc.renderBuffers().bufferSource().endBatch(ModRenderTypes.GLOW);
         return verts;
      } else {
         if (!LIVE.isEmpty() && now - lastVoidLog > 1000L) {
            lastVoidLog = now;
            ShenlinMod.LOGGER.info("[shenlin] orbit idle left={}", LIVE.size());
         }

         return 0;
      }
   }

   private static Vec3 chest(EntityMaid maid, float partialTick) {
      return maid.getPosition(partialTick).add(0.0, maid.getBbHeight() * 0.62, 0.0);
   }

   private static Vec3 overhead(Minecraft mc, OrbitCannonEffect.Form form, float partialTick) {
      return (mc.level != null && form.targetId > 0 ? mc.level.getEntity(form.targetId) : null) instanceof LivingEntity living && living.isAlive()
         ? living.getPosition(partialTick).add(0.0, living.getBbHeight() + HoverCannon.formAbove(), 0.0)
         : form.anchor;
   }

   private static int formation(
      VertexConsumer consumer,
      Matrix4f pose,
      OrbitCannonEffect.Form form,
      double since,
      EntityMaid maid,
      OrbitCannonEffect.Live live,
      Vec3 center,
      double radius,
      double spin,
      boolean compat,
      Vec3 camera,
      float partialTick
   ) {
      Minecraft mc = Minecraft.getInstance();
      Vec3 aim = form.aim;
      Vec3 anchor = overhead(mc, form, partialTick);
      double charge = Math.max(0.0, Math.min(1.0, (since - 15.0) / Math.max(1, form.chargeTicks)));
      double after = since - (15L + form.chargeTicks);
      double fade = 1.0;
      double size;
      if (after < 0.0) {
         size = 0.16 + 0.84 * charge;
      } else if (after < 8.0) {
         size = 1.0 + 0.9 * Math.exp(-after / 1.8);
      } else {
         double t = Math.max(0.0, Math.min(1.0, (after - 8.0) / 10.0));
         double left = 1.0 - t;
         size = left * left;
         fade = left;
      }

      float fadeF = (float)fade;
      float boost = (float)(1.0 + 0.85 * charge);
      int verts = 0;

      for (int pod = 0; pod < 3; pod++) {
         Vec3 home = center.add(HoverCannon.podOffset(pod, spin, maid.getUUID(), live.firing, radius));
         Vec3 enter = live.pods != null && live.pods[pod] != null ? live.pods[pod] : home;
         Vec3 at = HoverCannon.formationPos(pod, anchor, aim, enter, home, since, form.chargeTicks);
         if (!compat || !occluded(mc, camera, at)) {
            Vec3 rel = at.subtract(camera);
            Vec3 hub = anchor.subtract(camera);
            verts += pod(consumer, pose, rel, hub.subtract(rel), spin, pod, boost, 1.0F, 1.0F);
            float feed = (float)(0.35 + 0.65 * charge);
            verts += vein(
               consumer,
               pose,
               rel,
               hub,
               HoverCannon.formChargeWidth() * feed,
               (0.3F + 0.55F * (float)charge) * fadeF * (float)(0.85 + 0.15 * Math.sin(spin * 2.2))
            );
         }
      }

      if (!compat || !occluded(mc, camera, anchor)) {
         verts += core(consumer, pose, anchor.subtract(camera), size, fade, spin);
      }

      return verts;
   }

   private static boolean occluded(Minecraft mc, Vec3 camera, Vec3 at) {
      if (mc.level == null) {
         return false;
      }

      BlockHitResult hit = mc.level.clip(new ClipContext(camera, at, Block.VISUAL, Fluid.NONE, mc.player));
      return hit.getType() != Type.MISS && mc.level.getBlockState(hit.getBlockPos()).isViewBlocking(mc.level, hit.getBlockPos());
   }

   private static int core(VertexConsumer consumer, Matrix4f pose, Vec3 at, double size, double fade, double spin) {
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
         float f = (float)fade;
         float ka = Math.min(1.0F, k);
         float beat = (float)(0.9 + 0.1 * Math.sin(spin * 0.9));
         int verts = 0;
         verts += quad(consumer, pose, at, tsrg2, up, (0.05F + 0.15F * k) * beat, 0.7F, 0.88F, 1.0F, (0.34F + 0.48F * ka) * f);
         verts += quad(consumer, pose, at, tsrg2, up, (0.09F + 0.25F * k) * beat, 0.3F, 0.62F, 1.0F, (0.2F + 0.38F * ka) * f);
         return verts + quad(consumer, pose, at, tsrg2, up, (0.14F + 0.4F * k) * beat, 0.12F, 0.34F, 0.9F, (0.1F + 0.2F * ka) * f);
      } else {
         return 0;
      }
   }

   private static int vein(VertexConsumer consumer, Matrix4f pose, Vec3 a, Vec3 b, float width, float alpha) {
      Vec3 axis = b.subtract(a);
      double length = axis.length();
      if (length < 0.08) {
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
      int verts = 0;
      int segments = 5;

      for (int i = 0; i < segments; i++) {
         double t0 = (double)i / segments;
         double t1 = (double)(i + 1) / segments;
         verts += band(
            consumer,
            pose,
            a.add(axis.scale(t0)),
            a.add(axis.scale(t1)),
            side,
            width * (float)(1.0 - 0.65 * t0),
            width * (float)(1.0 - 0.65 * t1),
            alpha
         );
      }

      return verts;
   }

   private static int band(VertexConsumer consumer, Matrix4f pose, Vec3 a, Vec3 b, Vec3 side, float wa, float wb, float alpha) {
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

   private static int pod(VertexConsumer consumer, Matrix4f pose, Vec3 at, Vec3 aim, double spin, int index, double pulse, float grow, float alpha) {
      if (!(alpha <= 0.02F) && !(grow <= 0.02F) && !(aim.lengthSqr() < 1.0E-9)) {
         float s = (float)(grow * pulse);
         Vec3 f = aim.normalize();
         Vec3 r = f.cross(UP);
         if (r.lengthSqr() < 1.0E-6) {
            r = f.cross(EAST);
         }

         r = r.normalize();
         Vec3 u = f.cross(r).normalize();
         double ring = spin * 0.09 + index * 2.1;
         float breathe = (float)(0.86 + 0.14 * Math.sin(spin * 0.12 + index * 2.4));
         int verts = 0;
         float nose = 0.19F * s;
         float tail = 0.115F * s;
         float waist = 0.115F * s;
         Vec3 tip = at.add(f.scale(nose));
         Vec3[] belt = new Vec3[12];

         for (int i = 0; i < 12; i++) {
            double ang = i * (Math.PI / 6);
            belt[i] = at.add(r.scale(Math.cos(ang) * waist)).add(u.scale(Math.sin(ang) * waist));
         }

         for (int i = 0; i < 12; i++) {
            verts += tri(consumer, pose, tip, belt[i], belt[(i + 1) % 12], 0.2F, 0.48F, 1.0F, alpha * breathe);
         }

         verts += tailCap(consumer, pose, at, f, r, u, belt, waist, tail, alpha);
         int segs = 36;
         float ringR = 0.225F * s;
         float half = 0.026F * s;

         for (int i = 0; i < segs; i++) {
            double a0 = ring + i * ((Math.PI * 2) / segs);
            double a1 = ring + (i + 1) * ((Math.PI * 2) / segs);
            Vec3 d0 = r.scale(Math.cos(a0)).add(u.scale(Math.sin(a0)));
            Vec3 d1 = r.scale(Math.cos(a1)).add(u.scale(Math.sin(a1)));
            float flow = (float)(0.55 + 0.45 * Math.sin(a0 * 2.0 - spin * 0.35));
            verts += face(
               consumer,
               pose,
               at.add(d0.scale(ringR)).add(f.scale(half)),
               at.add(d0.scale(ringR)).subtract(f.scale(half)),
               at.add(d1.scale(ringR)).subtract(f.scale(half)),
               at.add(d1.scale(ringR)).add(f.scale(half)),
               0.34F,
               0.72F,
               1.0F,
               alpha * flow,
               d0
            );
         }

         float mouth0 = nose * 0.45F;
         float mouth1 = nose + 0.095F * s;
         float mouth = 0.042F * s;
         Vec3[] edge = new Vec3[8];

         for (int i = 0; i < 8; i++) {
            double ang = i * (Math.PI / 4);
            edge[i] = r.scale(Math.cos(ang) * mouth).add(u.scale(Math.sin(ang) * mouth));
         }

         for (int i = 0; i < 8; i++) {
            Vec3 c0 = edge[i];
            Vec3 c1 = edge[(i + 1) % 8];
            verts += face(
               consumer,
               pose,
               at.add(c0).add(f.scale(mouth0)),
               at.add(c1).add(f.scale(mouth0)),
               at.add(c1).add(f.scale(mouth1)),
               at.add(c0).add(f.scale(mouth1)),
               0.86F,
               0.95F,
               1.0F,
               alpha * 0.38F,
               c0.normalize()
            );
         }

         float hot = (float)Math.max(0.0, Math.min(1.0, pulse - 0.75));
         Vec3 mouthTip = at.add(f.scale(mouth1));
         float mouthAlpha = alpha * (0.1F + 0.9F * hot);

         for (int i = 0; i < 8; i++) {
            verts += tri(
               consumer,
               pose,
               mouthTip,
               at.add(edge[i]).add(f.scale(mouth1)),
               at.add(edge[(i + 1) % 8]).add(f.scale(mouth1)),
               0.86F,
               0.95F,
               1.0F,
               mouthAlpha
            );
         }

         return verts;
      } else {
         return 0;
      }
   }

   private static int tailCap(VertexConsumer consumer, Matrix4f pose, Vec3 at, Vec3 f, Vec3 r, Vec3 u, Vec3[] belt, float waist, float tail, float alpha) {
      int verts = 0;
      Vec3[] prev = belt;

      for (int ring = 1; ring <= 4; ring++) {
         double polar = ring / 4.0 * Math.PI * 0.5;
         float rr = (float)(waist * Math.cos(polar));
         float back = (float)(tail * Math.sin(polar));
         if (ring == 4) {
            Vec3 pole = at.subtract(f.scale(back));

            for (int i = 0; i < 12; i++) {
               verts += capTri(consumer, pose, pole, prev[(i + 1) % 12], prev[i], 0.2F, 0.48F, 1.0F, alpha);
            }
            break;
         }

         Vec3[] cur = new Vec3[12];
         Vec3 center = at.subtract(f.scale(back));

         for (int i = 0; i < 12; i++) {
            double ang = i * (Math.PI / 6);
            cur[i] = center.add(r.scale(Math.cos(ang) * rr)).add(u.scale(Math.sin(ang) * rr));
         }

         for (int i = 0; i < 12; i++) {
            Vec3 a = prev[i];
            Vec3 b = prev[(i + 1) % 12];
            verts += capTri(consumer, pose, a, b, cur[(i + 1) % 12], 0.2F, 0.48F, 1.0F, alpha);
            verts += capTri(consumer, pose, a, cur[(i + 1) % 12], cur[i], 0.2F, 0.48F, 1.0F, alpha);
         }

         prev = cur;
      }

      return verts;
   }

   private static float shade(Vec3 n) {
      return 0.55F + 0.45F * (float)(n.y * 0.5 + 0.5);
   }

   private static float capShade(Vec3 n) {
      return 0.94F + 0.06F * shade(n);
   }

   private static int tri(VertexConsumer consumer, Matrix4f pose, Vec3 a, Vec3 b, Vec3 p, float r, float g, float bl, float alpha) {
      Vec3 n = b.subtract(a).cross(p.subtract(a));
      if (n.lengthSqr() < 1.0E-12) {
         return 0;
      }

      n = n.normalize();
      Vec3 mid = a.add(b).add(p).scale(0.3333333333333333);
      if (n.dot(mid) >= 0.0) {
         return 0;
      }

      float k = shade(n);
      vert(consumer, pose, a, r * k, g * k, bl * k, alpha);
      vert(consumer, pose, b, r * k, g * k, bl * k, alpha);
      vert(consumer, pose, p, r * k, g * k, bl * k, alpha);
      return 3;
   }

   private static int capTri(VertexConsumer consumer, Matrix4f pose, Vec3 a, Vec3 b, Vec3 p, float r, float g, float bl, float alpha) {
      Vec3 n = b.subtract(a).cross(p.subtract(a));
      if (n.lengthSqr() < 1.0E-12) {
         return 0;
      }

      n = n.normalize();
      Vec3 mid = a.add(b).add(p).scale(0.3333333333333333);
      if (n.dot(mid) >= 0.0) {
         return 0;
      }

      float k = capShade(n);
      vert(consumer, pose, a, r * k, g * k, bl * k, alpha);
      vert(consumer, pose, b, r * k, g * k, bl * k, alpha);
      vert(consumer, pose, p, r * k, g * k, bl * k, alpha);
      return 3;
   }

   private static int face(VertexConsumer consumer, Matrix4f pose, Vec3 a, Vec3 b, Vec3 d, Vec3 e, float r, float g, float bl, float alpha, Vec3 normal) {
      if (!(alpha <= 0.02F) && !(normal.lengthSqr() < 1.0E-12)) {
         Vec3 mid = a.add(b).add(d).add(e).scale(0.25);
         if (normal.dot(mid) >= 0.0) {
            return 0;
         }

         float k = shade(normal);
         vert(consumer, pose, a, r * k, g * k, bl * k, alpha);
         vert(consumer, pose, b, r * k, g * k, bl * k, alpha);
         vert(consumer, pose, d, r * k, g * k, bl * k, alpha);
         vert(consumer, pose, a, r * k, g * k, bl * k, alpha);
         vert(consumer, pose, d, r * k, g * k, bl * k, alpha);
         vert(consumer, pose, e, r * k, g * k, bl * k, alpha);
         return 6;
      } else {
         return 0;
      }
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

   private static final class Form {
      private final Vec3 anchor;
      private final Vec3 aim;
      private final long startTick;
      private final long bornMs;
      private final int chargeTicks;
      private final int totalTicks;
      private final int targetId;

      private Form(Vec3 anchor, Vec3 aim, long startTick, long bornMs, int chargeTicks, int totalTicks, int targetId) {
         this.anchor = anchor;
         this.aim = aim;
         this.startTick = startTick;
         this.bornMs = bornMs;
         this.chargeTicks = chargeTicks;
         this.totalTicks = totalTicks;
         this.targetId = targetId;
      }
   }

   private static final class Live {
      private long until;
      private int state;
      private int targetId;
      private long idleMs;
      private boolean firing;
      private long stateSince;
      private Vec3 anchor;
      private long anchorAt;
      private Vec3[] pods;
      private long foldAt = -1L;
      private long unfoldAt = -1L;
      private boolean folded;
   }
}

