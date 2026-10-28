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
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent.Post;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.lwjgl.opengl.GL11;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT, bus = Bus.FORGE)
public final class WardenFlashScreen {
   private static final ResourceLocation POST = ResourceLocation.fromNamespaceAndPath("goddess_of_maids", "shaders/post/warden_flash.json");
   private static final int TICKS = 4;
   private static final long SPAN = 200L;
   private static final long SEED_STEP = 50L;
   private static final double SPOT_RADIUS = 3.2;
   private static PostChain chain;
   private static boolean tried;
   private static boolean broken;
   private static int chainEpoch = PostChainRebuild.epoch();
   private static int lastWidth = -1;
   private static int lastHeight = -1;
   private static long from;
   private static long to;
   private static Entity source;
   private static Vec3[] spots;
   private static boolean toldOffscreen;
   private static boolean logged;
   private static Field passesField;
   private static boolean passesBroken;
   private static Field fovField;
   private static boolean fovBroken;

   private WardenFlashScreen() {
   }

   public static void triggerAt(Vec3 a, Vec3 b, int ticks) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && a != null) {
         source = null;
         spots = b == null ? new Vec3[]{a} : new Vec3[]{a, b};
         toldOffscreen = false;
         long now = EffectClock.now();
         long span = Math.max(1, ticks) * 50L;
         if (now >= to) {
            from = now;
         }

         to = Math.max(to, now + span);
         ShenlinMod.LOGGER
            .info(
               "[shenlin] 守护兽闪现 两处遮盖 {} 拍 甲={} 乙={}",
               new Object[]{
                  ticks,
                  a == null ? "-" : String.format("%.1f,%.1f,%.1f", a.x, a.y, a.z),
                  b == null ? "-" : String.format("%.1f,%.1f,%.1f", b.x, b.y, b.z)
               }
            );
      }
   }

   public static void trigger() {
      long now = EffectClock.now();
      if (now >= to) {
         from = now;
      }

      to = Math.max(to, now + 200L);
   }

   public static void triggerFor(Entity who) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         source = who;
         spots = null;
         toldOffscreen = false;
         trigger();
         ShenlinMod.LOGGER.info("[shenlin] 守护兽闪现 喊了一声 距={} 格", String.format("%.1f", Math.sqrt(mc.player.distanceToSqr(who))));
      }
   }

   @SubscribeEvent
   public static void onGuiPost(Post event) {
      long now = EffectClock.now();
      long span = to - from;
      if (span > 0L && now < to) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.level != null) {
            float progress = (float)(now - from) / (float)span;
            PostChain active = chain(mc);
            if (active == null) {
               if (!toldOffscreen) {
                  toldOffscreen = true;
                  ShenlinMod.LOGGER.warn("[shenlin] 守护兽闪现 没画：那条链子没建起来");
               }
            } else if (spots != null) {
               for (Vec3 at : spots) {
                  if (at != null) {
                     paintOne(active, progress, now, mc, at);
                  }
               }
            } else {
               paintOne(active, progress, now, mc, null);
            }
         } else {
            to = 0L;
         }
      } else {
         source = null;
         spots = null;
      }
   }

   private static void paintOne(PostChain active, float progress, long now, Minecraft mc, Vec3 at) {
      if (tune(active, progress, now, mc, at)) {
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
               ShenlinMod.LOGGER.info("[shenlin] warden flash chain active");
            }
         } catch (Throwable t) {
            broken = true;
            closeChain();
            ShenlinMod.LOGGER.warn("[shenlin] warden flash chain failed: {}", t.toString());
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
   }

   private static boolean tune(PostChain active, float progress, long now, Minecraft mc, Vec3 at) {
      List<PostPass> passes = passes(active);
      if (passes.isEmpty()) {
         return false;
      }

      float aspect = (float)mc.getWindow().getWidth() / Math.max(1, mc.getWindow().getHeight());
      float[] spot = at != null ? screenSpotAt(mc, aspect, at) : screenSpot(mc, aspect);
      if (spot == null) {
         if (!toldOffscreen) {
            toldOffscreen = true;
            ShenlinMod.LOGGER.info("[shenlin] 守护兽闪现 没画：它不在画面上");
         }

         return false;
      } else {
         float seed = (float)(now / 50L % 4096L);
         float strength = 1.0F - progress * progress;

         try {
            for (PostPass pass : passes) {
               EffectInstance effect = pass.getEffect();
               if (effect != null) {
                  tune(effect, "Strength", strength);
                  tune(effect, "Seed", seed);
                  tune(effect, "Aspect", aspect);
                  tune(effect, "Radius", spot[2]);
                  AbstractUniform center = effect.safeGetUniform("Center");
                  if (center != null) {
                     center.set(spot[0], spot[1]);
                  }
               }
            }

            return true;
         } catch (Throwable t) {
            return false;
         }
      }
   }

   private static void tune(EffectInstance effect, String name, float value) {
      AbstractUniform uniform = effect.safeGetUniform(name);
      if (uniform != null) {
         uniform.set(value);
      }
   }

   private static float[] screenSpot(Minecraft mc, float aspect) {
      Entity target = source;
      return target != null && !target.isRemoved() && mc.level != null
         ? screenSpotAt(mc, aspect, target.position().add(0.0, target.getBbHeight() * 0.55, 0.0))
         : null;
   }

   private static float[] screenSpotAt(Minecraft mc, float aspect, Vec3 at) {
      if (at != null && mc.level != null) {
         Camera camera = mc.gameRenderer.getMainCamera();
         Vec3 d = at.subtract(SafeCamera.pos(mc));
         Vec3 look = Vec3.directionFromRotation(SafeCamera.xRot(mc), SafeCamera.yRot(mc));
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
            return null;
         }

         double fov = realFov(mc);
         double tanY = Math.tan(Math.toRadians(fov * 0.5));
         if (tanY <= 1.0E-4) {
            return null;
         }

         double tanX = tanY * aspect;
         float u = (float)(0.5 + d.dot(tsrg2) / (fwd * tanX) * 0.5);
         float v = (float)(0.5 + d.dot(up) / (fwd * tanY) * 0.5);
         float radius = (float)(3.2 / (fwd * tanY) * 0.5);
         return new float[]{u, v, radius};
      } else {
         return null;
      }
   }

   private static double realFov(Minecraft mc) {
      if (!fovBroken) {
         try {
            if (fovField == null) {
               fovField = field(GameRenderer.class, "fov", "f_109090_");
            }

            if (fovField.get(mc.gameRenderer) instanceof Number n && n.doubleValue() > 1.0) {
               return n.doubleValue();
            }
         } catch (Throwable t) {
            fovBroken = true;
            ShenlinMod.LOGGER.warn("[shenlin] 拿不到真实视野角，退回设置里那一份: {}", t.toString());
         }
      }

      return ((Integer)mc.options.fov().get()).intValue();
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

   private static List<PostPass> passes(PostChain active) {
      if (passesBroken) {
         return List.of();
      }

      try {
         if (passesField == null) {
            passesField = field(PostChain.class, "f_110009_", "passes");
         }

         Object raw = passesField.get(active);
         if (raw == null) {
            return List.of();
         }

         List<?> list = (List<?>)raw;
         List<PostPass> found = new ArrayList<>(list.size());

         for (Object entry : list) {
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
               ShenlinMod.LOGGER.warn("[shenlin] warden flash chain resize failed: {}", t.toString());
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
            ShenlinMod.LOGGER.warn("[shenlin] warden flash post chain unavailable: {}", t.toString());
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
         ShenlinMod.LOGGER.info("[shenlin] 守护兽闪现 那条链子重建（资源重载过）");
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
}

