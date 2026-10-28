package com.myx00x.maid.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.myx00x.maid.ShenlinMod;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent.Post;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.lwjgl.opengl.GL11;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT, bus = Bus.FORGE)
public final class ScreenRippleEffect {
   private static final ResourceLocation POST = ResourceLocation.fromNamespaceAndPath("goddess_of_maids", "shaders/post/screen_ripple.json");
   private static final float RING_FROM = 0.06F;
   private static final float BAND_W = 0.07F;
   private static final float BEND = 0.16F;
   private static final float RIM = 0.13F;
   private static final double GROW_POW = 0.75;
   private static final int MAX_ALIVE = 4;
   private static final List<ScreenRippleEffect.Ripple> ALIVE = new ArrayList<>();
   private static PostChain chain;
   private static boolean tried;
   private static boolean broken;
   private static int chainEpoch = PostChainRebuild.epoch();
   private static int lastWidth = -1;
   private static int lastHeight = -1;
   private static boolean toldWhy;
   private static boolean logged;
   private static Field passesField;
   private static boolean passesBroken;

   private ScreenRippleEffect() {
   }

   public static void spawn(int entityId, double x, double y, double z, int ticks) {
      long now = EffectClock.now();
      long span = Math.max(1, ticks) * 50L;
      ALIVE.add(new ScreenRippleEffect.Ripple(entityId, now, now + span, new Vec3(x, y, z)));

      while (ALIVE.size() > 4) {
         ALIVE.remove(0);
      }

      toldWhy = false;
      ShenlinMod.LOGGER.info("[shenlin] 屏幕波纹 喊了一声 落点={} ticks={} live={}", new Object[]{String.format("%.1f,%.1f,%.1f", x, y, z), ticks, ALIVE.size()});
   }

   @SubscribeEvent
   public static void onGuiPost(Post event) {
      long now = EffectClock.now();
      dropFinished(now);
      if (!ALIVE.isEmpty()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.level != null) {
            PostChain active = chain(mc);
            if (active == null) {
               why("那条链子没建起来");
            } else {
               float aspect = (float)mc.getWindow().getWidth() / Math.max(1, mc.getWindow().getHeight());
               RenderTarget main = mc.getMainRenderTarget();
               boolean blend = GL11.glIsEnabled(3042);
               boolean depth = GL11.glIsEnabled(2929);
               boolean disabled = false;

               try {
                  RenderSystem.disableBlend();
                  RenderSystem.disableDepthTest();
                  disabled = true;
                  Camera rippleCam = mc.gameRenderer.getMainCamera();
                  Vec3 rippleEye = rippleCam.getPosition();
                  Vec3 rippleLook = new Vec3(rippleCam.getLookVector());

                  for (ScreenRippleEffect.Ripple ripple : ALIVE) {
                     if (ViewGate.sees(rippleEye, rippleLook, ripple.spot.x, ripple.spot.y, ripple.spot.z)) {
                        long span = ripple.to - ripple.from;
                        if (span > 0L) {
                           float progress = (float)(now - ripple.from) / (float)span;
                           if (tune(active, ripple, progress, mc, aspect)) {
                              active.process(mc.getDeltaFrameTime());
                              main.bindWrite(false);
                           }
                        }
                     }
                  }

                  if (!logged) {
                     logged = true;
                     ShenlinMod.LOGGER.info("[shenlin] 屏幕波纹 在跑");
                  }
               } catch (Throwable t) {
                  broken = true;
                  closeChain();
                  ShenlinMod.LOGGER.warn("[shenlin] 屏幕波纹 失败了: {}", t.toString());
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
         } else {
            ALIVE.clear();
         }
      }
   }

   private static void dropFinished(long now) {
      Iterator<ScreenRippleEffect.Ripple> it = ALIVE.iterator();

      while (it.hasNext()) {
         if (now >= it.next().to) {
            it.remove();
         }
      }
   }

   private static boolean tune(PostChain active, ScreenRippleEffect.Ripple ripple, float progress, Minecraft mc, float aspect) {
      List<PostPass> passes = passes(active);
      if (passes.isEmpty()) {
         why("没拿到那几趟 pass");
         return false;
      }

      float[] center = screenCenter(mc, aspect, ripple.spot);
      float reach = farthestCorner(center[0], center[1], aspect) + 0.07F;
      float ring = (float)(0.06F + (reach - 0.06F) * Math.pow(progress, 0.75));
      float strength = 1.0F - progress;

      try {
         for (PostPass pass : passes) {
            EffectInstance effect = pass.getEffect();
            if (effect != null) {
               tune(effect, "Strength", strength);
               tune(effect, "Ring", ring);
               tune(effect, "Width", 0.07F);
               tune(effect, "Bend", 0.16F);
               tune(effect, "Rim", 0.13F);
               tune(effect, "Aspect", aspect);
               AbstractUniform at = effect.safeGetUniform("Center");
               if (at != null) {
                  at.set(center[0], center[1]);
               }
            }
         }

         return true;
      } catch (Throwable t) {
         return false;
      }
   }

   private static float farthestCorner(float cx, float cy, float aspect) {
      float best = 0.0F;

      for (int i = 0; i < 4; i++) {
         float x = (i & 1) == 0 ? 0.0F : 1.0F;
         float y = (i & 2) == 0 ? 0.0F : 1.0F;
         float dx = (x - cx) * aspect;
         float dy = y - cy;
         best = Math.max(best, (float)Math.sqrt(dx * dx + dy * dy));
      }

      return best;
   }

   private static void tune(EffectInstance effect, String name, float value) {
      AbstractUniform uniform = effect.safeGetUniform(name);
      if (uniform != null) {
         uniform.set(value);
      }
   }

   private static float[] screenCenter(Minecraft mc, float aspect, Vec3 spot) {
      float[] none = new float[]{0.5F, 0.5F};
      if (spot != null && mc.level != null) {
         Camera camera = mc.gameRenderer.getMainCamera();
         Vec3 d = spot.subtract(SafeCamera.pos(mc));
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
            return none;
         }

         double tanY = Math.tan(Math.toRadians(((Integer)mc.options.fov().get()).intValue() * 0.5));
         if (tanY <= 1.0E-4) {
            return none;
         }

         double tanX = tanY * aspect;
         float u = (float)(0.5 + d.dot(tsrg2) / (fwd * tanX) * 0.5);
         float v = (float)(0.5 + d.dot(up) / (fwd * tanY) * 0.5);
         return !(u < 0.03F) && !(u > 0.97F) && !(v < 0.03F) && !(v > 0.97F) ? new float[]{u, v} : none;
      } else {
         return none;
      }
   }

   private static void why(String reason) {
      if (!toldWhy) {
         toldWhy = true;
         ShenlinMod.LOGGER.info("[shenlin] 屏幕波纹 没画：{}", reason);
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
               ShenlinMod.LOGGER.warn("[shenlin] 屏幕波纹 那条链子改尺寸失败: {}", t.toString());
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
            ShenlinMod.LOGGER.warn("[shenlin] 屏幕波纹 那条链子建不起来: {}", t.toString());
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
         ShenlinMod.LOGGER.info("[shenlin] 屏幕波纹 那条链子重建（资源重载过）");
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

   private static final class Ripple {
      private final int entityId;
      private final long from;
      private final long to;
      private final Vec3 spot;

      private Ripple(int entityId, long from, long to, Vec3 spot) {
         this.entityId = entityId;
         this.from = from;
         this.to = to;
         this.spot = spot;
      }
   }
}

