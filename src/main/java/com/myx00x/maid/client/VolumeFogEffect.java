package com.myx00x.maid.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.myx00x.maid.FxTrace;
import com.myx00x.maid.ShenlinMod;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT, bus = Bus.FORGE)
public final class VolumeFogEffect {
   private static final int SEGMENTS = 10;
   private static final int RINGS = 8;
   private static final int JET_RINGS = 4;
   public static final int MODE_STILL = 0;
   public static final int MODE_SPREAD = 1;
   public static final int MODE_JET = 2;
   public static final int MODE_DISSOLVE = 3;
   public static final int MODE_BILLOW = 4;
   private static final int PUFFS = 26;
   private static final float PUFF_MIN = 0.34F;
   private static final float PUFF_MAX = 0.72F;
   private static final float GROW_SHARE = 0.18F;
   private static final float FADE_SHARE = 0.42F;
   private static final float PEAK_ALPHA = 0.3F;
   private static final int BILLOW_OVERLAP = 8;
   private static final float BILLOW_START_SPREAD = 0.72F;
   private static final float SPREAD_RATE = 18.0F;
   private static final double SLAM_LIFT = 0.3;
   private static final double BILLOW_LIFT = 0.9;
   private static final float JET_RATE = 110.0F;
   private static final float JET_PUFF_MS = 720.0F;
   private static final int JET_MAX_PUFFS = 100;
   private static final int MAX_LOOK_CHECKS = 400;
   private static boolean lastStartSolid = false;
   private static double lastDist = -1.0;
   private static String lastWhy = "";
   private static long lastBudgetLog = 0L;
   private static int cullLogsThisFrame = 0;
   private static int cullLogsSkipped = 0;
   private static final int CULL_LOG_PER_FRAME = 3;
   private static final double SAMPLE_STEP = 0.2;
   private static final int VIS_SAMPLES = 120;
   private static final List<VolumeFogEffect.Cloud> CLOUDS = new ArrayList<>();
   private static final List<VolumeFogEffect.Jet> JETS = new ArrayList<>();
   private static long lastVoidLog = 0L;
   private static long lastDrawLog = 0L;
   private static int framesThisSecond = 0;
   private static int framesLastSecond = 0;
   private static long fpsWindowStart = 0L;
   private static long lastFadeLog = 0L;
   private static long lastAlphaLog = 0L;
   private static float lastLoggedEnvelope = -1.0F;
   private static final int VIS_HIDDEN = 0;
   private static final int VIS_NORMAL = 1;
   private static final int VIS_THROUGH = 2;

   private static float spreadAt(float progress, int mode) {
      if (mode == 4) {
         return 0.72F + 0.27999997F * (1.0F - (float)Math.exp(-progress * 18.0F));
      } else {
         return mode == 3 ? 0.72F + 0.28F * (1.0F - (float)Math.exp(-progress * 18.0F)) : 1.0F;
      }
   }

   private static double liftAt(float progress, int mode) {
      if (mode == 3) {
         return Math.sin(progress * Math.PI) * 0.3;
      } else {
         return mode == 4 ? progress * 0.9 : 0.0;
      }
   }

   private static float perPuffFor(float target) {
      double t = Math.min(0.999, Math.max(0.0, target));
      return (float)(1.0 - Math.pow(1.0 - t, 0.125));
   }

   private VolumeFogEffect() {
   }

   public static void spawn(double x, double y, double z, float radius, float r, float g, float b, int ticks) {
      spawn(x, y, z, radius, r, g, b, ticks, 0, 0.0, 0.0, 0.0, 0.0);
   }

   public static void spawn(
      double x, double y, double z, float radius, float r, float g, float b, int ticks, int mode, double dx, double dy, double dz, double speed
   ) {
      spawn(x, y, z, radius, r, g, b, ticks, mode, dx, dy, dz, speed, 1.0F);
   }

   public static void spawn(
      double x,
      double y,
      double z,
      float radius,
      float r,
      float g,
      float b,
      int ticks,
      int mode,
      double dx,
      double dy,
      double dz,
      double speed,
      float alphaScale
   ) {
      int span = Math.max(200, ticks * 50);
      float thick = Math.max(0.05F, alphaScale);
      double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
      double nx = len < 1.0E-6 ? 0.0 : dx / len;
      double ny = len < 1.0E-6 ? 0.0 : dy / len;
      double nz = len < 1.0E-6 ? 0.0 : dz / len;
      if (mode == 2) {
         JETS.add(
            new VolumeFogEffect.Jet(
               x, y, z, Math.max(0.3F, radius), r, g, b, span, nx, ny, nz, Math.max(0.0, speed), thick, Double.doubleToLongBits(x * 31.0 + y * 17.0 + z * 7.0)
            )
         );
      } else {
         CLOUDS.add(new VolumeFogEffect.Cloud(x, y, z, Math.max(0.4F, radius), r, g, b, span, mode, thick, Double.doubleToLongBits(x * 31.0 + y * 17.0 + z)));
      }

      FxTrace.fx(
         "volumeFog",
         "spawn",
         new Vec3(x, y, z),
         System.currentTimeMillis(),
         "mode="
            + modeName(mode)
            + " radius="
            + FxTrace.f(radius)
            + " rgb="
            + FxTrace.f(r)
            + "/"
            + FxTrace.f(g)
            + "/"
            + FxTrace.f(b)
            + " dir="
            + FxTrace.f(nx)
            + "/"
            + FxTrace.f(ny)
            + "/"
            + FxTrace.f(nz)
            + " speed="
            + FxTrace.f(speed)
            + " ticks="
            + ticks
            + " alphaScale="
            + FxTrace.f(thick)
            + " live="
            + (CLOUDS.size() + JETS.size())
      );
   }

   public static String modeName(int mode) {
      return switch (mode) {
         case 1 -> "spread";
         case 2 -> "jet";
         case 3 -> "dissolve";
         case 4 -> "billow";
         default -> "still";
      };
   }

   public static int count() {
      return CLOUDS.size() + JETS.size();
   }

   public static boolean pending() {
      return !CLOUDS.isEmpty() || !JETS.isEmpty();
   }

   public static void dropAll() {
      CLOUDS.clear();
      JETS.clear();
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void onRenderStage(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_PARTICLES) {
         if (!CLOUDS.isEmpty() || !JETS.isEmpty()) {
            long now = EffectClock.now();
            if (!EffectChannel.handPassAlive(now)) {
               try {
                  int verts = paintAtCapture(event.getPoseStack(), event.getCamera(), now);
                  if (verts > 0 && now - lastDrawLog > 1000L) {
                     lastDrawLog = now;
                     ShenlinMod.LOGGER.info("[shenlin] fog stage verts={} live={}", verts, CLOUDS.size() + JETS.size());
                  }
               } catch (Throwable t) {
                  dropAll();
                  ShenlinMod.LOGGER.warn("[shenlin] volume fog draw failed: {}", t.toString());
               }
            }
         }
      }
   }

   public static int paintAtCapture(PoseStack poseStack, Camera camera, long now) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.player != null && (!CLOUDS.isEmpty() || !JETS.isEmpty())) {
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

         return verts;
      } else {
         return 0;
      }
   }

   public static int paint(Vec3 camera, Vec3 look, long now) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.player != null) {
         Vec3 forward = look.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : look.normalize();
         Vec3 tsrg2 = forward.cross(new Vec3(0.0, 1.0, 0.0));
         if (tsrg2.lengthSqr() < 1.0E-4) {
            tsrg2 = new Vec3(1.0, 0.0, 0.0);
         }

         tsrg2 = tsrg2.normalize();
         Vec3 up = tsrg2.cross(forward).normalize();
         BufferSource buffers = mc.renderBuffers().bufferSource();
         VertexConsumer normal = buffers.getBuffer(ModRenderTypes.GLOW_ALPHA);
         VertexConsumer through = buffers.getBuffer(ModRenderTypes.GLOW_ALPHA_THROUGH);
         int[] lookBudget = new int[]{400};
         int verts = 0;
         cullLogsThisFrame = 0;
         cullLogsSkipped = 0;
         framesThisSecond++;
         Iterator<VolumeFogEffect.Cloud> it = CLOUDS.iterator();

         while (it.hasNext()) {
            VolumeFogEffect.Cloud cloud = it.next();
            cloud.advance(now);
            float life = cloud.progress();
            if (life >= 1.0F) {
               it.remove();
               ShenlinMod.LOGGER
                  .info("[shenlin] fog drop elapsed={} span={} left={} why=寿命走完", new Object[]{cloud.elapsed(), cloud.span, CLOUDS.size() + JETS.size()});
            } else {
               float envelope = envelopeAt(life, cloud.mode);
               if (now - fpsWindowStart >= 1000L) {
                  fpsWindowStart = now;
                  framesLastSecond = framesThisSecond;
                  framesThisSecond = 0;
               }

               if (!(envelope <= 0.001F)) {
                  int drawn = 0;
                  int skipped = 0;
                  int throughCount = 0;
                  int hiddenCount = 0;
                  float grow = growAt(life, cloud.mode);

                  for (int i : cloud.drawOrder(camera)) {
                     int o = i * 8;
                     float breathe = 0.88F + 0.12F * Mth.sin(cloud.puffs[o + 6] + life * 6.2832F * cloud.puffs[o + 7]);
                     float spread = spreadAt(life, cloud.mode);
                     double lift = liftAt(life, cloud.mode);
                     double px = cloud.x + cloud.puffs[o] * grow * spread;
                     double py = cloud.y + cloud.puffs[o + 1] * grow * spread + lift;
                     double pz = cloud.z + cloud.puffs[o + 2] * grow * spread;
                     float radius = cloud.radius * cloud.puffs[o + 3] * grow * breathe;
                     float alpha;
                     if (cloud.mode == 4) {
                        alpha = perPuffFor(envelope * Math.min(1.0F, cloud.alphaScale * 0.52F)) * cloud.puffs[o + 5];
                        alpha += (cloud.puffs[o + 4] - 0.5F) * 0.0019607844F;
                     } else {
                        alpha = envelope * 0.3F * cloud.alphaScale * cloud.puffs[o + 5];
                     }

                     if (!(alpha <= 1.0E-4F)) {
                        if (cloud.culled[i]) {
                           skipped++;
                        } else {
                           byte cachedVis = cloud.visCache[i];
                           int vis;
                           if (cachedVis < 0) {
                              vis = puffVisibility(px, py, pz, radius, lookBudget, cloud.mode);
                              cloud.visCache[i] = (byte)vis;
                           } else {
                              vis = cachedVis;
                           }

                           if (vis == 0) {
                              if (cloud.mode != 4) {
                                 cloud.culled[i] = true;
                              }

                              hiddenCount++;
                              logCull("cloud", px, py, pz, radius, life, cloud.progress(), cloud.elapsed(), cloud.span);
                           } else {
                              if (vis == 2) {
                                 throughCount++;
                              }

                              drawn++;
                              verts += puff(
                                 vis == 2 ? through : normal,
                                 tsrg2,
                                 up,
                                 px - camera.x,
                                 py - camera.y,
                                 pz - camera.z,
                                 radius,
                                 alpha,
                                 cloud.r,
                                 cloud.g,
                                 cloud.b,
                                 8
                              );
                           }
                        }
                     }
                  }

                  if (now - lastFadeLog >= 200L) {
                     lastFadeLog = now;
                     ShenlinMod.LOGGER
                        .info(
                           "[shenlin] fogFade progress={} envelope={} elapsed={} span={} fps={} drawn={} hidden={} through={} live={} foreverCut={} cutLogsHidden={} mode={} grow={} r0={} why={}",
                           new Object[]{
                              FxTrace.f(life),
                              FxTrace.f(envelope),
                              cloud.elapsed(),
                              cloud.span,
                              framesLastSecond,
                              drawn,
                              hiddenCount,
                              throughCount,
                              CLOUDS.size() + JETS.size(),
                              cloud.culledCount(),
                              cullLogsSkipped,
                              cloud.mode,
                              FxTrace.f(grow),
                              FxTrace.f(cloud.radius * cloud.puffs[3] * grow * 1.0F),
                              lastWhy
                           }
                        );
                     if (envelope > 0.05F && now - lastAlphaLog > 1000L) {
                        lastAlphaLog = now;
                        float tgt = cloud.mode == 4 ? Math.min(1.0F, envelope * Math.min(1.0F, cloud.alphaScale * 0.52F)) : envelope;
                        float per = cloud.mode == 4 ? perPuffFor(tgt) : envelope * 0.3F * cloud.alphaScale;
                        ShenlinMod.LOGGER
                           .info(
                              "[shenlin] fogAlpha target={} perPuff={} overlap={} combined={}",
                              new Object[]{
                                 FxTrace.f(tgt),
                                 FxTrace.f(per),
                                 cloud.mode == 4 ? 8 : "—",
                                 FxTrace.f(1.0F - (float)Math.pow(1.0F - per, cloud.mode == 4 ? 8.0 : 1.0))
                              }
                           );
                     }

                     lastLoggedEnvelope = envelope;
                  }
               }
            }
         }

         Iterator<VolumeFogEffect.Jet> jt = JETS.iterator();

         while (jt.hasNext()) {
            VolumeFogEffect.Jet jet = jt.next();
            jet.advance(now);
            boolean done = jet.elapsed() >= jet.span;
            if (done && jet.puffs.isEmpty()) {
               jt.remove();
               ShenlinMod.LOGGER
                  .info("[shenlin] jet drop elapsed={} span={} left={} why=喷流走完且最后一颗已散", new Object[]{jet.elapsed(), jet.span, CLOUDS.size() + JETS.size()});
            } else {
               if (!done) {
                  jet.emit();
               }

               Iterator<VolumeFogEffect.JetPuff> pt = jet.puffs.iterator();

               while (pt.hasNext()) {
                  VolumeFogEffect.JetPuff p = pt.next();
                  float age = p.ageMs(now);
                  if (age >= 720.0F) {
                     pt.remove();
                  } else {
                     float t = age / 720.0F;
                     double[] at = p.at(now);
                     float radius = p.radius * (1.0F + 0.85F * t);
                     float alpha = 0.3F * p.alphaMul * (1.0F - t) * (1.0F - t);
                     if (!(alpha <= 1.0E-4F) && !p.culled) {
                        int vis;
                        if (p.visCache < 0) {
                           vis = puffVisibility(at[0], at[1], at[2], radius, lookBudget, 2);
                           p.visCache = (byte)vis;
                        } else {
                           vis = p.visCache;
                        }

                        if (vis == 0) {
                           p.culled = true;
                           logCull("jet", at[0], at[1], at[2], radius, t, t, (long)age, 720L);
                        } else {
                           verts += puff(
                              vis == 2 ? through : normal,
                              tsrg2,
                              up,
                              at[0] - camera.x,
                              at[1] - camera.y,
                              at[2] - camera.z,
                              radius,
                              alpha,
                              jet.r,
                              jet.g,
                              jet.b,
                              4
                           );
                        }
                     }
                  }
               }
            }
         }

         if (verts > 0) {
            buffers.endBatch(ModRenderTypes.GLOW_ALPHA);
            buffers.endBatch(ModRenderTypes.GLOW_ALPHA_THROUGH);
         }

         return verts;
      } else {
         if ((!CLOUDS.isEmpty() || !JETS.isEmpty()) && now - lastVoidLog > 1000L) {
            lastVoidLog = now;
            ShenlinMod.LOGGER.info("[shenlin] fog idle left={}", CLOUDS.size() + JETS.size());
         }

         return 0;
      }
   }

   private static float envelopeAt(float progress) {
      return envelopeAt(progress, 0);
   }

   private static float envelopeAt(float progress, int mode) {
      if (mode == 4) {
         return 1.0F - progress;
      } else if (mode == 3) {
         float rise = progress < 0.08F ? progress / 0.08F : 1.0F;
         float fade = (float)Math.pow(1.0 - progress, 0.85);
         return rise * fade;
      } else if (mode == 1) {
         float rise = progress < 0.12F ? progress / 0.12F : 1.0F;
         float fade = (float)Math.pow(1.0 - progress, 1.35);
         return rise * fade;
      } else if (progress < 0.18F) {
         float u = progress / 0.18F;
         return u * u;
      } else if (progress > 0.58000004F) {
         float u = (progress - 0.58000004F) / 0.42F;
         return (1.0F - u) * (1.0F - u);
      } else {
         return 1.0F;
      }
   }

   private static float growAt(float progress, int mode) {
      if (mode == 4) {
         return 1.0F + 3.2F * progress;
      } else if (mode == 3) {
         return 1.0F + 1.5F * progress;
      } else {
         return mode == 1 ? 1.0F + 1.6F * progress : 0.85F + 0.3F * progress;
      }
   }

   private static int puffVisibility(double px, double py, double pz, float radius, int[] budget, int mode) {
      lastStartSolid = false;
      lastDist = -1.0;
      if (budget[0] <= 0) {
         lastWhy = "这一帧的射线预算用完了，按可见处理";
         return 1;
      }

      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         lastWhy = "客户端世界还没拿到";
         return 1;
      }

      budget[0]--;
      Vec3 eye = SafeCamera.pos(mc);
      double ex = eye.x - px;
      double ey = eye.y - py;
      double ez = eye.z - pz;
      double toEye = Math.sqrt(ex * ex + ey * ey + ez * ez);
      if (toEye < 1.0E-4) {
         return 1;
      }

      ex /= toEye;
      ey /= toEye;
      ez /= toEye;
      MutableBlockPos cursor = new MutableBlockPos();
      int crossed = 0;
      boolean inSolid = false;
      int samples = (int)Math.min(120.0, Math.max(1.0, Math.ceil(toEye / 0.2)));
      double step = toEye / (samples + 1);

      for (int i = 1; i <= samples && crossed < 2; i++) {
         double d = step * i;
         if (insideSolid(mc, cursor, px + ex * d, py + ey * d, pz + ez * d)) {
            if (!inSolid) {
               crossed++;
            }

            inSolid = true;
         } else {
            inSolid = false;
         }
      }

      lastStartSolid = false;
      lastDist = toEye;
      if (crossed >= 2) {
         lastWhy = "两层以上的实心挡住了它和眼睛之间";
         return 0;
      }

      if (crossed == 1) {
         lastWhy = "只穿过一片实心，不算被挡住";
         return 2;
      }

      double e = radius * 0.92;
      double ux = Math.abs(ey) < 0.9 ? 0.0 : 1.0;
      double uy = Math.abs(ey) < 0.9 ? 1.0 : 0.0;
      double b1x = ey * 0.0 - ez * uy;
      double b1y = ez * ux - ex * 0.0;
      double b1z = ex * uy - ey * ux;
      double b1l = Math.sqrt(b1x * b1x + b1y * b1y + b1z * b1z);
      if (b1l < 1.0E-6) {
         b1l = 1.0;
      }

      b1x /= b1l;
      b1y /= b1l;
      b1z /= b1l;
      double b2x = ey * b1z - ez * b1y;
      double b2y = ez * b1x - ex * b1z;
      double b2z = ex * b1y - ey * b1x;

      for (int k = 0; k < 3; k++) {
         double ang = (Math.PI * 2) * k / 3.0;
         double ca = Math.cos(ang);
         double sa = Math.sin(ang);
         double dx2 = b1x * ca + b2x * sa;
         double dy2 = b1y * ca + b2y * sa;
         double dz2 = b1z * ca + b2z * sa;
         if (insideSolid(mc, cursor, px + dx2 * e, py + dy2 * e, pz + dz2 * e)) {
            lastWhy = "射线顺着缝过去，但身上压着方块（边缘 3 点之一）";
            return 2;
         }
      }

      lastWhy = "一路没挡，正常画";
      return 1;
   }

   private static void logCull(String kind, double x, double y, double z, float radius, float progress, float life, long elapsed, long span) {
      if (cullLogsThisFrame >= 3) {
         cullLogsSkipped++;
      } else {
         cullLogsThisFrame++;
         ShenlinMod.LOGGER
            .info(
               "[shenlin] fog cut kind={} at={} radius={} startSolid={} dist={} progress={} elapsed={}/{} why={}",
               new Object[]{
                  kind,
                  String.format("%.2f,%.2f,%.2f", x, y, z),
                  FxTrace.f(radius),
                  lastStartSolid,
                  lastDist < 0.0 ? "-" : String.format("%.1f", lastDist),
                  FxTrace.f(progress),
                  elapsed,
                  span,
                  lastWhy
               }
            );
      }
   }

   private static boolean insideSolid(Minecraft mc, MutableBlockPos cursor, double x, double y, double z) {
      cursor.set(Mth.floor(x), Mth.floor(y), Mth.floor(z));
      return solidAt(mc, cursor);
   }

   private static boolean solidAt(Minecraft mc, BlockPos pos) {
      if (mc.level == null) {
         return false;
      }

      try {
         return !mc.level.getBlockState(pos).getCollisionShape(mc.level, pos).isEmpty();
      } catch (Throwable t) {
         return false;
      }
   }

   private static int puff(
      VertexConsumer consumer, Vec3 tsrg2, Vec3 up, double cx, double cy, double cz, float radius, float alpha, float r, float g, float b, int rings
   ) {
      int verts = 0;
      float[] ringR = new float[rings + 1];
      float[] ringA = new float[rings + 1];

      for (int k = 0; k <= rings; k++) {
         float t = (float)k / rings;
         ringR[k] = radius * t;
         ringA[k] = (float)(alpha * Math.pow(1.0 - t, 1.7));
      }

      for (int i = 0; i < 10; i++) {
         int j = (i + 1) % 10;
         float ang0 = (float)((Math.PI * 2) * i / 10.0);
         float ang1 = (float)((Math.PI * 2) * j / 10.0);
         float c0 = Mth.cos(ang0);
         float s0 = Mth.sin(ang0);
         float c1 = Mth.cos(ang1);
         float s1 = Mth.sin(ang1);

         for (int k = 0; k < rings; k++) {
            float rIn = ringR[k];
            float rOut = ringR[k + 1];
            float aIn = ringA[k];
            float aOut = ringA[k + 1];
            verts += v(consumer, cx, cy, cz, tsrg2, up, c0 * rIn, s0 * rIn, r, g, b, aIn);
            verts += v(consumer, cx, cy, cz, tsrg2, up, c0 * rOut, s0 * rOut, r, g, b, aOut);
            verts += v(consumer, cx, cy, cz, tsrg2, up, c1 * rOut, s1 * rOut, r, g, b, aOut);
            verts += v(consumer, cx, cy, cz, tsrg2, up, c0 * rIn, s0 * rIn, r, g, b, aIn);
            verts += v(consumer, cx, cy, cz, tsrg2, up, c1 * rOut, s1 * rOut, r, g, b, aOut);
            verts += v(consumer, cx, cy, cz, tsrg2, up, c1 * rIn, s1 * rIn, r, g, b, aIn);
         }
      }

      return verts;
   }

   private static int v(
      VertexConsumer consumer, double cx, double cy, double cz, Vec3 tsrg2, Vec3 up, float dx, float dy, float r, float g, float b, float alpha
   ) {
      double x = cx + tsrg2.x * dx + up.x * dy;
      double y = cy + tsrg2.y * dx + up.y * dy;
      double z = cz + tsrg2.z * dx + up.z * dy;
      consumer.vertex(x, y, z).color(r, g, b, Math.max(0.0F, alpha)).endVertex();
      return 1;
   }

   private static final class Cloud {
      private final double x;
      private final double y;
      private final double z;
      private final float radius;
      private final float r;
      private final float g;
      private final float b;
      private final int span;
      private final int mode;
      private final float alphaScale;
      private final float[] puffs = new float[208];
      private final boolean[] culled = new boolean[26];
      private final byte[] visCache = new byte[26];
      private final int[] order = new int[26];
      private final float[] orderKey = new float[26];
      private long seen = Long.MIN_VALUE;
      private long elapsed;

      int culledCount() {
         int n = 0;

         for (boolean one : this.culled) {
            if (one) {
               n++;
            }
         }

         return n;
      }

      Cloud(double x, double y, double z, float radius, float r, float g, float b, int span, int mode, float alphaScale, long seed) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.radius = radius;
         this.r = r;
         this.g = g;
         this.b = b;
         this.span = span;
         this.mode = mode;
         this.alphaScale = alphaScale;
         Arrays.fill(this.visCache, (byte)-1);
         RandomSource random = RandomSource.create(seed);

         for (int i = 0; i < 26; i++) {
            double u = random.nextDouble() * 2.0 - 1.0;
            double theta = random.nextDouble() * Math.PI * 2.0;
            double s = Math.sqrt(Math.max(0.0, 1.0 - u * u));
            double rr = Math.cbrt(random.nextDouble()) * radius * 0.78;
            int o = i * 8;
            this.puffs[o] = (float)(Math.cos(theta) * s * rr);
            this.puffs[o + 1] = (float)(u * rr);
            this.puffs[o + 2] = (float)(Math.sin(theta) * s * rr);
            this.puffs[o + 3] = 0.34F + random.nextFloat() * 0.38000003F;
            this.puffs[o + 4] = random.nextFloat();
            this.puffs[o + 5] = mode == 4 ? 0.45F + random.nextFloat() * 0.55F : 0.92F + random.nextFloat() * 0.08F;
            this.puffs[o + 6] = random.nextFloat() * 6.2832F;
            this.puffs[o + 7] = 0.5F + random.nextFloat() * 0.8F;
         }
      }

      long elapsed() {
         return this.elapsed;
      }

      void advance(long now) {
         if (this.seen == Long.MIN_VALUE) {
            this.seen = now;
         } else {
            long step = now - this.seen;
            this.seen = now;
            this.elapsed += step >= 0L && step <= 250L ? step : 50L;
         }
      }

      float progress() {
         return this.span <= 0 ? 1.0F : Mth.clamp((float)this.elapsed / this.span, 0.0F, 1.0F);
      }

      int[] drawOrder(Vec3 camera) {
         for (int i = 0; i < 26; i++) {
            this.order[i] = i;
            int o = i * 8;
            double dx = this.x + this.puffs[o] - camera.x;
            double dy = this.y + this.puffs[o + 1] - camera.y;
            double dz = this.z + this.puffs[o + 2] - camera.z;
            this.orderKey[i] = (float)(dx * dx + dy * dy + dz * dz);
         }

         for (int i = 1; i < 26; i++) {
            int idx = this.order[i];
            float key = this.orderKey[idx];

            int j;
            for (j = i - 1; j >= 0 && this.orderKey[this.order[j]] < key; j--) {
               this.order[j + 1] = this.order[j];
            }

            this.order[j + 1] = idx;
         }

         return this.order;
      }
   }

   private static final class Jet {
      private final double x;
      private final double y;
      private final double z;
      private final float radius;
      private final float r;
      private final float g;
      private final float b;
      private final int span;
      private final double dirX;
      private final double dirY;
      private final double dirZ;
      private final double speed;
      private final float alphaScale;
      private final RandomSource random;
      private final List<VolumeFogEffect.JetPuff> puffs = new ArrayList<>();
      private long seen = Long.MIN_VALUE;
      private long elapsed;
      private double pending;

      Jet(
         double x,
         double y,
         double z,
         float radius,
         float r,
         float g,
         float b,
         int span,
         double dirX,
         double dirY,
         double dirZ,
         double speed,
         float alphaScale,
         long seed
      ) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.radius = radius;
         this.r = r;
         this.g = g;
         this.b = b;
         this.span = span;
         this.dirX = dirX;
         this.dirY = dirY;
         this.dirZ = dirZ;
         this.speed = speed;
         this.alphaScale = Math.max(0.05F, alphaScale);
         this.random = RandomSource.create(seed);
      }

      long elapsed() {
         return this.elapsed;
      }

      void advance(long now) {
         if (this.seen == Long.MIN_VALUE) {
            this.seen = now;
         } else {
            long step = now - this.seen;
            this.seen = now;
            this.elapsed += step >= 0L && step <= 250L ? step : 16L;
         }
      }

      void emit() {
         if (this.puffs.size() < 100) {
            this.pending += 1.76;
            int want = (int)this.pending;
            if (want > 0) {
               this.pending -= want;

               for (int i = 0; i < want && this.puffs.size() < 100; i++) {
                  double ox = this.random.nextGaussian() * this.radius * 0.45;
                  double oy = this.random.nextGaussian() * this.radius * 0.45;
                  double oz = this.random.nextGaussian() * this.radius * 0.45;
                  double jitter = 0.22;
                  double vx = this.dirX + this.random.nextGaussian() * jitter * 0.5;
                  double vy = this.dirY + this.random.nextGaussian() * jitter * 0.5;
                  double vz = this.dirZ + this.random.nextGaussian() * jitter * 0.5;
                  double vlen = Math.sqrt(vx * vx + vy * vy + vz * vz);
                  if (vlen < 1.0E-6) {
                     vx = this.dirX;
                     vy = this.dirY;
                     vz = this.dirZ;
                     vlen = 1.0;
                  }

                  double sp = this.speed * (0.82 + this.random.nextDouble() * 0.36);
                  this.puffs
                     .add(
                        new VolumeFogEffect.JetPuff(
                           this.x + ox,
                           this.y + oy,
                           this.z + oz,
                           vx / vlen * sp,
                           vy / vlen * sp,
                           vz / vlen * sp,
                           this.radius * (0.55F + this.random.nextFloat() * 0.55F),
                           (0.92F + this.random.nextFloat() * 0.08F) * this.alphaScale,
                           EffectClock.now()
                        )
                     );
               }
            }
         }
      }
   }

   private static final class JetPuff {
      private final double x;
      private final double y;
      private final double z;
      private final double vx;
      private final double vy;
      private final double vz;
      private final float radius;
      private final float alphaMul;
      private final long born;
      private boolean culled;
      private byte visCache = -1;

      JetPuff(double x, double y, double z, double vx, double vy, double vz, float radius, float alphaMul, long born) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.vx = vx;
         this.vy = vy;
         this.vz = vz;
         this.radius = radius;
         this.alphaMul = alphaMul;
         this.born = born;
      }

      float ageMs(long now) {
         return (float)Math.max(0L, now - this.born);
      }

      double[] at(long now) {
         double t = Math.max(0.0, (now - this.born) / 1000.0);
         double drag = Math.pow(t, 0.85);
         return new double[]{this.x + this.vx * drag, this.y + this.vy * drag, this.z + this.vz * drag};
      }
   }
}

