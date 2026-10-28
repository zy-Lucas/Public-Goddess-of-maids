package com.myx00x.maid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.myx00x.maid.ShenlinMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT, bus = Bus.FORGE)
public final class SpaceShardFx {
   private static final int COUNT = 23;
   private static final double START_RATIO = 0.88;
   private static final double END_RATIO = 1.45;
   private static final float FLY_UNTIL = 0.16F;
   private static final float FADE_FROM = 0.7F;
   private static final RenderType GLASS = RenderType.entityTranslucent(ResourceLocation.withDefaultNamespace("textures/misc/white.png"));
   private static final int GLASS_ALPHA = 235;
   private static final List<SpaceShardFx.Shard> SHARDS = new ArrayList<>();
   private static long builtSerial = -1L;
   private static long lastNote;
   private static boolean logged;

   private SpaceShardFx() {
   }

   @SubscribeEvent
   public static void onRenderLevel(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_TRANSLUCENT_BLOCKS) {
         if (SpaceBreakEffect.portalNow()) {
            if (builtSerial >= 0L) {
               builtSerial = -1L;
               SHARDS.clear();
            }
         } else {
            Vec3 spot = SpaceBreakEffect.spotNow();
            long serial = SpaceBreakEffect.serialNow();
            if (spot == null) {
               if (builtSerial >= 0L) {
                  builtSerial = -1L;
                  SHARDS.clear();
               }
            } else {
               if (builtSerial != serial) {
                  build(spot, SpaceBreakEffect.normalNow());
                  builtSerial = serial;
               }

               if (!SHARDS.isEmpty()) {
                  Minecraft minecraft = Minecraft.getInstance();
                  Vec3 camera = event.getCamera().getPosition();
                  PoseStack pose = event.getPoseStack();
                  BufferSource buffer = minecraft.renderBuffers().bufferSource();
                  float progress = SpaceBreakEffect.progressNow();
                  float alpha = 1.0F;
                  SHARDS.sort((a, b) -> Double.compare(b.center.distanceToSqr(camera), a.center.distanceToSqr(camera)));
                  pose.pushPose();
                  pose.translate(-camera.x, -camera.y, -camera.z);
                  Matrix4f matrix = pose.last().pose();
                  VertexConsumer face = buffer.getBuffer(GLASS);

                  for (SpaceShardFx.Shard shard : SHARDS) {
                     Vec3[] corners = triangleOf(shard, spot, progress);
                     float shade = 0.74F + 0.26F * Mth.clamp(shard.spin * 0.5F, 0.0F, 1.0F);
                     tri(face, matrix, corners[0], corners[1], corners[2], shade, alpha);
                  }

                  buffer.endBatch(GLASS);
                  VertexConsumer line = buffer.getBuffer(RenderType.lines());

                  for (SpaceShardFx.Shard shard : SHARDS) {
                     Vec3[] corners = triangleOf(shard, spot, progress);

                     for (int i = 0; i < 3; i++) {
                        edge(line, matrix, corners[i], corners[(i + 1) % 3], alpha);
                     }
                  }

                  buffer.endBatch(RenderType.lines());
                  pose.popPose();
               }
            }
         }
      }
   }

   private static Vec3[] triangleOf(SpaceShardFx.Shard shard, Vec3 spot, float progress) {
      double hole = Math.max(1.0, SpaceBreakEffect.reachNow());
      float travel = 1.0F - (1.0F - progress) * (1.0F - progress);
      Vec3 push = SpaceBreakEffect.pushNow();
      Vec3 ref = Math.abs(push.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
      Vec3 pu = push.cross(ref).normalize();
      Vec3 pv = push.cross(pu).normalize();
      double swirl = shard.spin * 3.4 * travel;
      double swirlR = hole * 0.55 * travel * (1.0 - travel * 0.45);
      Vec3 spiral = pu.scale(Math.cos(swirl) * swirlR).add(pv.scale(Math.sin(swirl) * swirlR));
      Vec3 origin = shard.center.add(push.scale(hole * 2.4 * travel)).add(spiral).add(shard.axis.scale(shard.lift));
      double ang = (0.06 + shard.spin * 2.4 * travel) * Math.PI * 2.0;
      Vec3 u = spinAround(shard.u, shard.tumble, ang);
      Vec3 v = spinAround(shard.v, shard.tumble, ang);
      float scale = Math.max(0.0F, 1.0F - progress);
      Vec3[] out = new Vec3[3];

      for (int i = 0; i < 3; i++) {
         out[i] = origin.add(u.scale(shard.cu[i] * scale)).add(v.scale(shard.cv[i] * scale));
      }

      return out;
   }

   private static Vec3 spinAround(Vec3 v, Vec3 axis, double ang) {
      double cos = Math.cos(ang);
      double sin = Math.sin(ang);
      double dot = axis.dot(v);
      return v.scale(cos).add(axis.cross(v).scale(sin)).add(axis.scale(dot * (1.0 - cos)));
   }

   private static void tri(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vec3 c, float shade, float fade) {
      float alpha = 0.92156863F * fade;
      glass(vc, m, a, 0.07F * shade, 0.05F * shade, 0.11F * shade, alpha);
      glass(vc, m, b, 0.1F * shade, 0.07F * shade, 0.14F * shade, alpha);
      glass(vc, m, c, 0.08F * shade, 0.06F * shade, 0.12F * shade, alpha);
      glass(vc, m, c, 0.92F * shade, 0.95F * shade, 1.0F * shade, alpha);
      glass(vc, m, a, 0.07F * shade, 0.05F * shade, 0.11F * shade, alpha);
      glass(vc, m, c, 0.08F * shade, 0.06F * shade, 0.12F * shade, alpha);
      glass(vc, m, b, 0.1F * shade, 0.07F * shade, 0.14F * shade, alpha);
      glass(vc, m, b, 0.1F * shade, 0.07F * shade, 0.14F * shade, alpha);
   }

   private static void glass(VertexConsumer vc, Matrix4f m, Vec3 p, float r, float g, float b, float a) {
      vc.vertex(m, (float)p.x, (float)p.y, (float)p.z)
         .color(r, g, b, a)
         .uv(0.5F, 0.5F)
         .overlayCoords(OverlayTexture.NO_OVERLAY)
         .uv2(15728880)
         .normal(0.0F, 1.0F, 0.0F)
         .endVertex();
   }

   private static void edge(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, float alpha) {
      float r = 0.88F;
      float g = 0.32F;
      float bl = 1.0F;
      vc.vertex(m, (float)a.x, (float)a.y, (float)a.z).color(r, g, bl, alpha).normal(0.0F, 1.0F, 0.0F).endVertex();
      vc.vertex(m, (float)b.x, (float)b.y, (float)b.z).color(r, g, bl, alpha).normal(0.0F, 1.0F, 0.0F).endVertex();
   }

   private static void build(Vec3 spot, Vec3 normal) {
      SHARDS.clear();
      float hole = Math.max(1.0F, SpaceBreakEffect.reachNow());
      RandomSource rnd = RandomSource.create(seedOf(spot));
      Vec3 n = normal.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : normal.normalize();
      Vec3 ref = Math.abs(n.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
      Vec3 u0 = n.cross(ref).normalize();
      Vec3 v0 = n.cross(u0).normalize();

      for (int i = 0; i < 23; i++) {
         double angle = rnd.nextDouble() * Math.PI * 2.0;
         double dist = hole * (0.15 + Math.sqrt(rnd.nextDouble()) * 0.83);
         double lift = (rnd.nextDouble() - 0.5) * 0.06;
         Vec3 center = spot.add(u0.scale(Math.cos(angle) * dist)).add(v0.scale(Math.sin(angle) * dist)).add(n.scale(lift));
         Vec3 axis = n;
         float spin = rnd.nextFloat();
         double sa = spin * Math.PI * 2.0;
         Vec3 tangent = u0.scale(Math.cos(sa)).add(v0.scale(Math.sin(sa))).normalize();
         Vec3 bitangent = axis.cross(tangent).normalize();
         float size = hole * (0.064F + rnd.nextFloat() * 0.124F);
         float[] baseU = new float[]{0.0F, -0.866F, 0.866F};
         float[] baseV = new float[]{1.0F, -0.5F, -0.5F};
         float[] cu = new float[3];
         float[] cv = new float[3];

         for (int k = 0; k < 3; k++) {
            float jitter = 0.88F + rnd.nextFloat() * 0.24F;
            cu[k] = baseU[k] * size * jitter;
            cv[k] = baseV[k] * size * jitter;
         }

         Vec3 tumble = randomDir(rnd);
         if (Math.abs(tumble.dot(n)) > 0.92) {
            tumble = randomDir(rnd);
         }

         SHARDS.add(new SpaceShardFx.Shard(center, axis, tangent, bitangent, tumble, cu, cv, 0.6F + rnd.nextFloat() * 1.6F, lift));
      }

      if (!logged) {
         logged = true;
         ShenlinMod.LOGGER
            .info(
               "[shenlin] shards built n={} hole={} at {}",
               new Object[]{SHARDS.size(), hole, String.format("%.1f,%.1f,%.1f", spot.x, spot.y, spot.z)}
            );
      }
   }

   private static Vec3 randomDir(RandomSource rnd) {
      Vec3 d = new Vec3(rnd.nextGaussian(), rnd.nextGaussian(), rnd.nextGaussian());
      return d.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 1.0, 0.0) : d.normalize();
   }

   private static long seedOf(Vec3 spot) {
      return (long)(spot.x * 3.41873128712E11) ^ (long)(spot.y * 1.32897987541E11) ^ (long)(spot.z * 4.2317861E7) ^ 25214903917L;
   }

   private static float smooth(float from, float to, float value) {
      if (to <= from) {
         return value >= to ? 1.0F : 0.0F;
      }

      float t = Mth.clamp((value - from) / (to - from), 0.0F, 1.0F);
      return t * t * (3.0F - 2.0F * t);
   }

   private record Shard(Vec3 center, Vec3 axis, Vec3 u, Vec3 v, Vec3 tumble, float[] cu, float[] cv, float spin, double lift) {
   }
}

