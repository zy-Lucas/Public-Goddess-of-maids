package com.myx00x.maid.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.myx00x.maid.ShenlinMod;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
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

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT, bus = Bus.FORGE)
public final class WardenTailTrail {
   private static final int POINTS = 8;
   private static final double MAX_LENGTH = 3.6;
   private static final double MIN_STEP = 0.12;
   private static final float ROOT_WIDTH = 0.4F;
   private static final float ALPHA = 0.85F;
   private static final float[] HOT = new float[]{1.0F, 0.42F, 0.14F};
   private static final float[] COLD = new float[]{0.55F, 0.08F, 0.04F};
   private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
   private static final Map<Integer, Deque<Vec3>> TRAILS = new HashMap<>();
   private static final boolean DIAG = false;
   private static long SHENLIN_DIAG_LAST;

   private WardenTailTrail() {
   }

   public static void record(int id, Vec3 jet) {
      Deque<Vec3> line = TRAILS.computeIfAbsent(id, key -> new ArrayDeque<>());
      Vec3 head = line.peekFirst();
      if (head == null || !(head.distanceToSqr(jet) < 0.0144)) {
         line.addFirst(jet);
         trim(line);
      }
   }

   public static void stop(int id) {
      TRAILS.remove(id);
   }

   private static void trim(Deque<Vec3> line) {
      double total = 0.0;
      Vec3 prev = null;
      int keep = 0;

      for (Vec3 p : line) {
         if (prev != null) {
            total += p.distanceTo(prev);
            if (total > 3.6) {
               break;
            }
         }

         prev = p;
         keep++;
      }

      keep = Math.min(keep, 8);

      while (line.size() > keep) {
         line.removeLast();
      }
   }

   public static boolean pending() {
      return !TRAILS.isEmpty();
   }

   public static void dropAll() {
      TRAILS.clear();
   }

   @SubscribeEvent
   public static void onRenderStage(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_PARTICLES) {
         ClientLevel level = Minecraft.getInstance().level;
         if (level == null) {
            TRAILS.clear();
         } else {
            TRAILS.keySet().removeIf(id -> level.getEntity(id) == null);
            if (!ShaderPack.inUse() || !EffectChannel.drawAtFrame()) {
               if (!TRAILS.isEmpty()) {
                  try {
                     paint(event.getPoseStack(), event.getCamera().getPosition(), ShaderPack.inUse());
                  } catch (Throwable t) {
                     TRAILS.clear();
                     ShenlinMod.LOGGER.warn("[shenlin] warden tail trail draw failed: {}", t.toString());
                  }
               }
            }
         }
      }
   }

   public static int paint(PoseStack stack, Vec3 camera, boolean compat) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.player != null) {
         ShaderInstance trailShader = ModShaders.slashRay();
         if (trailShader == null) {
            ModShaders.missing("slash_ray");
            return 0;
         }

         if (!compat) {
            RenderSystem.setShader(ModShaders::slashRay);
         }

         BufferSource buffers = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
         RenderType type = compat ? ModRenderTypes.GLOW : ModRenderTypes.STAR_GLOW;
         long nowMs = System.currentTimeMillis();
         stack.pushPose();
         Matrix4f pose = stack.last().pose();
         VertexConsumer consumer = buffers.getBuffer(type);
         int verts = 0;

         for (Deque<Vec3> line : TRAILS.values()) {
            verts += trail(consumer, pose, line, camera, compat);
         }

         buffers.endBatch(type);
         stack.popPose();
         return verts;
      } else {
         return 0;
      }
   }

   private static int trail(VertexConsumer consumer, Matrix4f pose, Deque<Vec3> line, Vec3 camera, boolean compat) {
      int count = line.size();
      if (count < 2) {
         return 0;
      }

      float phase = (float)(EffectClock.now() / 50L % 628L) * 0.01F;
      int verts = 0;
      Vec3 prev = null;
      int i = 0;

      for (Vec3 raw : line) {
         Vec3 p = raw.subtract(camera);
         if (prev != null) {
            float t0 = (float)i / (count - 1);
            float t1 = (float)(i + 1) / (count - 1);
            Vec3 dir = p.subtract(prev);
            if (dir.lengthSqr() < 1.0E-8) {
               prev = p;
               i++;
               continue;
            }

            Vec3 mid = prev.add(p).scale(0.5);
            Vec3 side = dir.cross(mid.scale(-1.0));
            if (side.lengthSqr() < 1.0E-8) {
               prev = p;
               i++;
               continue;
            }

            side = side.normalize();
            verts += ribbon(
               consumer,
               pose,
               prev,
               p,
               side,
               0.4F * width(t0, phase),
               0.4F * width(t1, phase),
               mix(t0),
               mix(t1),
               0.85F * (1.0F - t0),
               0.85F * (1.0F - t1),
               t0,
               t1,
               compat
            );
         }

         prev = p;
         i++;
      }

      return verts;
   }

   private static float width(float t, float phase) {
      float taper = Math.max(0.0F, 1.0F - t);
      return taper * taper * (0.86F + 0.14F * Mth.sin(t * 9.0F + phase));
   }

   private static float[] mix(float t) {
      float k = Mth.clamp(t, 0.0F, 1.0F);
      return new float[]{Mth.lerp(k, HOT[0], COLD[0]), Mth.lerp(k, HOT[1], COLD[1]), Mth.lerp(k, HOT[2], COLD[2])};
   }

   private static int ribbon(
      VertexConsumer consumer,
      Matrix4f pose,
      Vec3 a,
      Vec3 b,
      Vec3 side,
      float widthA,
      float widthB,
      float[] rgbA,
      float[] rgbB,
      float alphaA,
      float alphaB,
      float vA,
      float vB,
      boolean compat
   ) {
      if (compat) {
         return EffectInk.layered(consumer, a, b, side, widthA, widthB, rgbA, (alphaA + alphaB) * 0.5F, vA, vB, EffectInk.EDGES, false);
      }

      Vec3 a1 = a.add(side.scale(widthA));
      Vec3 a2 = a.subtract(side.scale(widthA));
      Vec3 b1 = b.add(side.scale(widthB));
      Vec3 b2 = b.subtract(side.scale(widthB));
      put(consumer, pose, a1, 0.0F, vA, rgbA, alphaA);
      put(consumer, pose, a2, 1.0F, vA, rgbA, alphaA);
      put(consumer, pose, b2, 1.0F, vB, rgbB, alphaB);
      put(consumer, pose, a1, 0.0F, vA, rgbA, alphaA);
      put(consumer, pose, b2, 1.0F, vB, rgbB, alphaB);
      put(consumer, pose, b1, 0.0F, vB, rgbB, alphaB);
      return 6;
   }

   private static void put(VertexConsumer consumer, Matrix4f pose, Vec3 p, float u, float v, float[] rgb, float alpha) {
      consumer.vertex(pose, (float)p.x, (float)p.y, (float)p.z)
         .color(rgb[0], rgb[1], rgb[2], alpha)
         .uv(u, v)
         .overlayCoords(OverlayTexture.NO_OVERLAY)
         .uv2(15728880)
         .normal(0.0F, 1.0F, 0.0F)
         .endVertex();
   }
}

