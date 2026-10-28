package com.myx00x.maid.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.myx00x.maid.ShenlinMod;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent.Post;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.lwjgl.opengl.GL11;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT, bus = Bus.FORGE)
public final class SpaceBreakEffect {
   private static final ResourceLocation POST = ResourceLocation.fromNamespaceAndPath("goddess_of_maids", "shaders/post/space_break.json");
   private static final float WORLD_RADIUS = 2.33F;
   private static final float DOOR_RADIUS = 1.4F;
   private static final float DOOR_REACH = 2.2F;
   private static PostChain chain;
   private static boolean tried;
   private static boolean broken;
   private static int chainEpoch = PostChainRebuild.epoch();
   private static int lastWidth = -1;
   private static int lastHeight = -1;
   private static boolean logged;
   private static Field passesField;
   private static boolean passesBroken;
   private static Vec3 spot;
   private static Vec3 crackNormal = new Vec3(0.0, 0.0, 1.0);
   private static long startMs;
   private static long endMs;
   private static boolean toldWhy;
   private static boolean portal;
   private static long crackSerial;
   private static Vec3 crackPush = new Vec3(0.0, 1.0, 0.0);
   private static int doorPhase;
   private static long doorPhaseMs;
   private static final long FREEZE_MS = 900L;
   private static final long SHRINK_MS = 700L;
   private static final float TILES = 9.0F;

   private SpaceBreakEffect() {
   }

   public static void doorOpen(double x, double y, double z, int ticks) {
      spawn(x, y, z, Math.max(200, ticks) + 60, true);
      doorPhase = 1;
      doorPhaseMs = EffectClock.now();
   }

   public static void doorClose() {
      if (doorPhase == 1) {
         doorPhase = 3;
         doorPhaseMs = EffectClock.now();
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null) {
            Vec3 look = mc.player.getLookAngle();
            Vec3 front = mc.player.position().add(look.x * 2.0, 0.0, look.z * 2.0);
            spot = front;
            crackNormal = look.scale(-1.0);
         }
      }
   }

   public static void spawn(double x, double y, double z, int ticks) {
      spawn(x, y, z, ticks, false);
   }

   public static void spawnUpright(double x, double y, double z, int ticks) {
      spawnUpright(x, y, z, 0.0, 1.0, 0.0, ticks);
   }

   public static void spawnUpright(double x, double y, double z, double dx, double dy, double dz, int ticks) {
      if (ticks > 0) {
         portal = false;
         crackSerial++;
         Vec3 push = new Vec3(dx, dy, dz);
         crackPush = push.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 1.0, 0.0) : push.normalize();
         long now = EffectClock.now();
         spot = new Vec3(x, y, z);
         crackNormal = new Vec3(0.0, 1.0, 0.0);
         startMs = now;
         endMs = now + ticks * 50L;
         toldWhy = false;
         ShenlinMod.LOGGER.info("[shenlin] 空间破碎（地面）开在 {}, ticks={}", String.format("%.1f,%.1f,%.1f", x, y, z), ticks);
      }
   }

   public static void spawn(double x, double y, double z, int ticks, boolean portal) {
      if (ticks > 0) {
         SpaceBreakEffect.portal = portal;
         long now = EffectClock.now();
         spot = new Vec3(x, y, z);
         Player player = Minecraft.getInstance().player;
         crackNormal = player != null ? player.getLookAngle().scale(-1.0) : new Vec3(0.0, 0.0, -1.0);
         startMs = now;
         endMs = now + ticks * 50L;
         toldWhy = false;
         ShenlinMod.LOGGER.info("[shenlin] 空间破碎 开在 {}, ticks={}", String.format("%.1f,%.1f,%.1f", x, y, z), ticks);
      }
   }

   public static Vec3 spotNow() {
      return spot;
   }

   public static boolean portalNow() {
      return portal;
   }

   public static Vec3 normalNow() {
      return crackNormal;
   }

   public static long elapsedNow() {
      return EffectClock.now() - startMs;
   }

   public static long spanNow() {
      return endMs - startMs;
   }

   public static long serialNow() {
      return crackSerial;
   }

   public static Vec3 pushNow() {
      return crackPush;
   }

   public static float progressNow() {
      long span = endMs - startMs;
      if (span <= 0L) {
         return 0.0F;
      }

      float p = (float)(EffectClock.now() - startMs) / (float)span;
      return p < 0.0F ? 0.0F : Math.min(1.0F, p);
   }

   public static float reachNow() {
      return 2.33F;
   }

   @SubscribeEvent
   public static void onGuiPost(Post event) {
      long span = endMs - startMs;
      long now = EffectClock.now();
      if (span > 0L && now < endMs && spot != null) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.level != null) {
            int width = mc.getWindow().getWidth();
            int height = Math.max(1, mc.getWindow().getHeight());
            float aspect = (float)width / height;
            PostChain active = chain(mc);
            if (active == null) {
               why("链子没起来");
            } else if (portal && doorPhase >= 2) {
               if (doorPhase == 3 && now - doorPhaseMs >= 900L) {
                  doorPhase = 2;
                  doorPhaseMs = now;
               }

               if (shade(active, 0.5F, null, aspect, 1.0F, true)) {
                  renderPass(active, mc);
               }

               if (doorPhase == 2 && now - doorPhaseMs >= 700L) {
                  spot = null;
                  doorPhase = 0;
               }
            } else {
               SpaceBreakEffect.View view = project(mc, aspect, spot);
               if (view == null) {
                  why("圆心不在视野里");
               } else {
                  float open = 0.0F;
                  if (portal) {
                     double away = mc.player.position().distanceTo(spot);
                     open = (float)Math.max(0.0, Math.min(1.0, 1.0 - (away - 2.2F) / 1.2));
                  }

                  float progress;
                  if (portal) {
                     progress = 0.5F * Math.min(1.0F, (float)(now - startMs) / 600.0F);
                  } else {
                     progress = (float)(now - startMs) / (float)span;
                  }

                  if (shade(active, progress, view, aspect, open, false)) {
                     renderPass(active, mc);
                  }
               }
            }
         } else {
            endMs = 0L;
         }
      }
   }

   private static void renderPass(PostChain active, Minecraft mc) {
      RenderTarget main = mc.getMainRenderTarget();
      boolean blend = GL11.glIsEnabled(3042);
      boolean depth = GL11.glIsEnabled(2929);
      boolean disabled = false;

      try {
         RenderSystem.disableBlend();
         RenderSystem.disableDepthTest();
         disabled = true;
         active.process(mc.getDeltaFrameTime());
         main.bindWrite(false);
         if (!logged) {
            logged = true;
            ShenlinMod.LOGGER.info("[shenlin] space break chain active");
         }
      } catch (Throwable t) {
         broken = true;
         closeChain();
         ShenlinMod.LOGGER.warn("[shenlin] space break chain failed: {}", t.toString());
      } finally {
         if (disabled) {
            if (blend) {
               RenderSystem.enableBlend();
            }

            if (depth) {
               RenderSystem.enableDepthTest();
            }
         }

         RenderSystem.viewport(0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight());
      }
   }

   private static boolean shade(PostChain active, float progress, SpaceBreakEffect.View view, float aspect, float open, boolean freeze) {
      List<PostPass> passes = passes(active);
      if (passes.isEmpty()) {
         return false;
      }

      float seconds = (float)(System.nanoTime() / 1000000L % 1000000L) / 1000.0F;
      if (freeze) {
         float shrink = 1.0F;
         if (doorPhase == 2) {
            shrink = Math.max(0.0F, 1.0F - (float)(EffectClock.now() - doorPhaseMs) / 700.0F);
         }

         for (PostPass pass : passes) {
            EffectInstance effect = pass.getEffect();
            if (effect != null) {
               set(effect, "Progress", 0.5F);
               set(effect, "Portal", 1.0F);
               set(effect, "Open", 1.0F);
               set(effect, "Freeze", 1.0F);
               set(effect, "Shrink", shrink);
               set(effect, "Tiles", 9.0F);
               set(effect, "Time", seconds);
               set(effect, "Aspect", aspect);
            }
         }

         return true;
      } else {
         try {
            for (PostPass pass : passes) {
               EffectInstance effect = pass.getEffect();
               if (effect != null) {
                  bindDepth(effect);
                  set(effect, "Progress", progress);
                  set(effect, "Portal", portal ? 1.0F : 0.0F);
                  set(effect, "Open", open);
                  set(effect, "Freeze", 0.0F);
                  set(effect, "Radius", view.radius());
                  set(effect, "Aspect", aspect);
                  set(effect, "Time", seconds);
                  set(effect, "Squash", view.squash());
                  set(effect, "WorldRadius", portal ? 1.4F : 2.33F);
                  vec(effect, "CrackNormal", (float)crackNormal.x, (float)crackNormal.y, (float)crackNormal.z);
                  Vec3 cam = SafeCamera.pos(Minecraft.getInstance());
                  vec(effect, "CamPos", (float)cam.x, (float)cam.y, (float)cam.z);
                  vec(effect, "CrackPos", (float)spot.x, (float)spot.y, (float)spot.z);
                  set(effect, "TanX", view.tanX());
                  set(effect, "TanY", view.tanY());
                  center(effect, view.u(), view.v());
                  vec(effect, "CamLook", view.lx(), view.ly(), view.lz());
                  vec(effect, "CamRight", view.rx(), view.ry(), view.rz());
                  vec(effect, "CamUp", view.ux(), view.uy(), view.uz());
               }
            }

            return true;
         } catch (Throwable t) {
            ShenlinMod.LOGGER.warn("[shenlin] space break uniform failed: {}", t.toString());
            return false;
         }
      }
   }

   private static void center(EffectInstance effect, float x, float y) {
      AbstractUniform uniform = effect.safeGetUniform("Center");
      if (uniform != null) {
         uniform.set(x, y);
      }
   }

   private static void vec(EffectInstance effect, String name, float x, float y, float z) {
      AbstractUniform uniform = effect.safeGetUniform(name);
      if (uniform != null) {
         uniform.set(x, y, z);
      }
   }

   private static void bindDepth(EffectInstance effect) {
      Minecraft minecraft = Minecraft.getInstance();
      effect.setSampler("DepthSampler", () -> minecraft.getMainRenderTarget().getDepthTextureId());
      set(effect, "Near", 0.05F);
      set(effect, "Far", Math.max(64.0F, minecraft.options.getEffectiveRenderDistance() * 16.0F));
   }

   private static void set(EffectInstance effect, String name, float value) {
      AbstractUniform uniform = effect.safeGetUniform(name);
      if (uniform != null) {
         uniform.set(value);
      }
   }

   private static SpaceBreakEffect.View project(Minecraft mc, float aspect, Vec3 point) {
      Camera camera = mc.gameRenderer.getMainCamera();
      Vec3 d = point.subtract(camera.getPosition());
      Vec3 look = Vec3.directionFromRotation(camera.getXRot(), camera.getYRot());
      Vec3 tsrg2 = look.cross(new Vec3(0.0, 1.0, 0.0));
      Vec3 up;
      if (tsrg2.lengthSqr() < 1.0E-6) {
         tsrg2 = new Vec3(1.0, 0.0, 0.0);
         up = new Vec3(0.0, 0.0, -1.0).scale(Math.signum(look.y));
      } else {
         tsrg2 = tsrg2.normalize();
         up = tsrg2.cross(look);
      }

      double fwd = d.dot(look);
      if (fwd <= 0.1) {
         why(String.format("fwd=%.2f 在身后", fwd));
         return null;
      }

      double tanY = Math.tan(Math.toRadians(((Integer)mc.options.fov().get()).intValue() * 0.5));
      if (tanY <= 1.0E-4) {
         why(String.format("tanY=%.5f fov=%d", tanY, mc.options.fov().get()));
         return null;
      }

      double tanX = tanY * aspect;
      float u = (float)(0.5 + d.dot(tsrg2) / (fwd * tanX) * 0.5);
      float v = (float)(0.5 + d.dot(up) / (fwd * tanY) * 0.5);
      float radius = (float)(2.33F / (fwd * tanY) * 0.5);
      Vec3 toCam = camera.getPosition().subtract(point);
      float squash = toCam.lengthSqr() > 1.0E-6 ? (float)Math.abs(crackNormal.dot(toCam.normalize())) : 1.0F;
      if (!(u < -2.5F) && !(u > 3.5F) && !(v < -2.5F) && !(v > 3.5F) && !(radius < 0.004F)) {
         return new SpaceBreakEffect.View(
            u,
            v,
            radius,
            squash,
            (float)look.x,
            (float)look.y,
            (float)look.z,
            (float)tsrg2.x,
            (float)tsrg2.y,
            (float)tsrg2.z,
            (float)up.x,
            (float)up.y,
            (float)up.z,
            (float)tanX,
            (float)tanY
         );
      }

      why(String.format("fwd=%.2f u=%.2f v=%.2f r=%.3f tanY=%.3f", fwd, u, v, radius, tanY));
      return null;
   }

   private static void why(String reason) {
      if (!toldWhy) {
         toldWhy = true;
         ShenlinMod.LOGGER.info("[shenlin] 空间破碎 没画：{}", reason);
      }
   }

   private static PostChain chain(Minecraft mc) {
      rebuildIfReloaded();
      if (chain != null) {
         int width = mc.getWindow().getWidth();
         int height = mc.getWindow().getHeight();
         if (width != lastWidth || height != lastHeight) {
            lastWidth = width;
            lastHeight = height;

            try {
               chain.resize(width, height);
            } catch (Throwable t) {
               broken = true;
               ShenlinMod.LOGGER.warn("[shenlin] space break chain resize failed: {}", t.toString());
               return null;
            }
         }

         return chain;
      } else {
         if (tried || broken) {
            return null;
         }

         tried = true;

         label24:
         try {
            chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), POST);
            break label24;
         } catch (Throwable t) {
            ShenlinMod.LOGGER.warn("[shenlin] space break chain unavailable", t);
            return null;
         }
      }
   }

   private static void rebuildIfReloaded() {
      int now = PostChainRebuild.epoch();
      if (now != chainEpoch) {
         chainEpoch = now;
         closeChain();
         tried = false;
         broken = false;
         passesBroken = false;
         lastWidth = -1;
         lastHeight = -1;
         ShenlinMod.LOGGER.info("[shenlin] space break 那条链子重建（资源重载过）");
      }
   }

   private static void closeChain() {
      PostChain old = chain;
      chain = null;
      if (old != null) {
         try {
            old.close();
         } catch (Throwable var2) {
         }
      }
   }

   private static List<PostPass> passes(PostChain active) {
      if (passesBroken) {
         return List.of();
      }

      try {
         if (passesField == null) {
            passesField = field(PostChain.class, "f_110009_", "passes");
         }

         List<?> raw = (List<?>)passesField.get(active);
         if (raw == null) {
            return List.of();
         }

         List<PostPass> found = new ArrayList<>(raw.size());

         for (Object entry : raw) {
            if (entry instanceof PostPass pass) {
               found.add(pass);
            }
         }

         return found;
      } catch (Throwable t) {
         passesBroken = true;
         return List.of();
      }
   }

   private static Field field(Class<?> owner, String... names) {
      for (String name : names) {
         try {
            Field found = owner.getDeclaredField(name);
            found.setAccessible(true);
            return found;
         } catch (Throwable var7) {
         }
      }

      throw new IllegalStateException("no such field");
   }

   private record View(
      float u,
      float v,
      float radius,
      float squash,
      float lx,
      float ly,
      float lz,
      float rx,
      float ry,
      float rz,
      float ux,
      float uy,
      float uz,
      float tanX,
      float tanY
   ) {
   }
}

