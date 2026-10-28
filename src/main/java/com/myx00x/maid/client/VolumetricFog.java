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
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT, bus = Bus.FORGE)
public final class VolumetricFog {
   private static final ResourceLocation POST = ResourceLocation.fromNamespaceAndPath("goddess_of_maids", "shaders/post/volumetric_fog.json");
   private static final int SLOTS = 4;
   private static final long FADE_IN_MS = 220L;
   private static final long FADE_OUT_MS = 520L;
   private static final float NOISE_SCALE = 1.15F;
   private static final float DENS_MULT = 1.0F;
   public static final boolean ENABLED = false;
   public static int DEBUG_MODE = 0;
   private static final List<VolumetricFog.Ball> BALLS = new ArrayList<>();
   private static PostChain chain;
   private static boolean tried;
   private static boolean broken;
   private static int chainEpoch = PostChainRebuild.epoch();
   private static int lastWidth = -1;
   private static int lastHeight = -1;
   private static boolean logged;
   private static Field passesField;
   private static boolean passesBroken;

   private VolumetricFog() {
   }

   public static void spawn(Vec3 center, float radius, float density, long lifeMs) {
   }

   public static int count() {
      return BALLS.size();
   }

   public static void dropAll() {
      BALLS.clear();
   }

   @SubscribeEvent
   public static void onRenderLevel(RenderLevelStageEvent event) {
   }

   private static boolean shade(Minecraft mc, long now) {
      PostChain active = chain(mc);
      if (active == null) {
         return false;
      }

      Camera camera = mc.gameRenderer.getMainCamera();
      if (!camera.isInitialized()) {
         return false;
      }

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

      Vec3 pos = SafeCamera.pos(mc);
      int w = mc.getWindow().getWidth();
      int h = Math.max(1, mc.getWindow().getHeight());
      float aspect = (float)w / h;
      float tanY = (float)Math.tan(Math.toRadians(((Integer)mc.options.fov().get()).intValue() * 0.5));
      if (tanY <= 1.0E-4F) {
         return false;
      }

      List<VolumetricFog.Ball> picked = new ArrayList<>(BALLS);
      picked.sort((a, bx) -> Double.compare(a.center().distanceToSqr(pos), bx.center().distanceToSqr(pos)));
      if (picked.size() > 4) {
         picked = picked.subList(0, 4);
      }

      float[] cx = new float[4];
      float[] cy = new float[4];
      float[] cz = new float[4];
      float[] cr = new float[4];
      float[] cd = new float[4];

      for (int i = 0; i < 4; i++) {
         if (i < picked.size()) {
            VolumetricFog.Ball b = picked.get(i);
            cx[i] = (float)b.center().x;
            cy[i] = (float)b.center().y;
            cz[i] = (float)b.center().z;
            cr[i] = b.radius();
            cd[i] = b.density() * b.fadeNow();
         }
      }

      long dayTime = mc.level.getDayTime() % 24000L;
      double sunAngle = dayTime / 24000.0 * Math.PI * 2.0 - (Math.PI / 2);
      Vec3 sun = new Vec3(Math.cos(sunAngle), Math.sin(sunAngle), 0.3).normalize();
      int sky = skyColor(mc, pos);
      float ar = (sky >> 16 & 0xFF) / 255.0F;
      float ag = (sky >> 8 & 0xFF) / 255.0F;
      float ab = (sky & 0xFF) / 255.0F;
      ar *= 0.75F;
      ag *= 0.78F;
      ab *= 0.86F;
      float sunUp = Mth.clamp((float)sun.y * 3.0F, 0.0F, 1.0F);
      float lr = 1.0F * (0.3F + 0.7F * sunUp);
      float lg = 0.96F * (0.3F + 0.7F * sunUp);
      float lb = 0.88F * (0.3F + 0.7F * sunUp);
      float seconds = (float)now / 1000.0F;

      for (PostPass pass : passes(active)) {
         EffectInstance e = pass.getEffect();
         if (e != null) {
            bindDepth(e, mc);
            vec(e, "CamLook", (float)look.x, (float)look.y, (float)look.z);
            vec(e, "CamRight", (float)tsrg2.x, (float)tsrg2.y, (float)tsrg2.z);
            vec(e, "CamUp", (float)up.x, (float)up.y, (float)up.z);
            vec(e, "CamPos", (float)pos.x, (float)pos.y, (float)pos.z);
            set(e, "TanX", tanY * aspect);
            set(e, "TanY", tanY);
            set(e, "Time", seconds);
            vec4(e, "FogA", cx[0], cy[0], cz[0], cr[0]);
            vec4(e, "FogB", cx[1], cy[1], cz[1], cr[1]);
            vec4(e, "FogC", cx[2], cy[2], cz[2], cr[2]);
            vec4(e, "FogD", cx[3], cy[3], cz[3], cr[3]);
            vec4(e, "FogDens", cd[0], cd[1], cd[2], cd[3]);
            vec(e, "AmbientColor", ar, ag, ab);
            vec(e, "LightColor", lr, lg, lb);
            vec(e, "SunDir", (float)sun.x, (float)sun.y, (float)sun.z);
            set(e, "NoiseScale", 1.15F);
            set(e, "DensMult", 1.0F);
         }
      }

      return true;
   }

   private static void draw(Minecraft mc) {
      PostChain active = chain(mc);
      if (active != null) {
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
         } catch (Throwable t) {
            broken = true;
            closeChain();
            ShenlinMod.LOGGER.warn("[shenlin] volumetric fog chain failed: {}", t.toString());
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
               ShenlinMod.LOGGER.warn("[shenlin] volumetric fog chain resize failed: {}", t.toString());
               return null;
            }
         }

         if (!logged) {
            logged = true;
            ShenlinMod.LOGGER.info("[shenlin] volumetric fog chain active (raymarch mode)");
         }

         return chain;
      } else {
         if (tried || broken) {
            return null;
         }

         tried = true;

         label28:
         try {
            chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), POST);
            break label28;
         } catch (Throwable t) {
            ShenlinMod.LOGGER.warn("[shenlin] volumetric fog chain unavailable", t);
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
         ShenlinMod.LOGGER.warn("[shenlin] volumetric fog: cannot reach post passes: {}", t.toString());
         return List.of();
      }
   }

   private static Field field(Class<?> owner, String... names) {
      for (String name : names) {
         try {
            Field f = owner.getDeclaredField(name);
            f.setAccessible(true);
            return f;
         } catch (Throwable var7) {
         }
      }

      throw new IllegalStateException("no such field");
   }

   private static void bindDepth(EffectInstance effect, Minecraft mc) {
      effect.setSampler("DepthSampler", () -> mc.getMainRenderTarget().getDepthTextureId());
      set(effect, "Near", 0.05F);
      set(effect, "Far", Math.max(64.0F, mc.options.getEffectiveRenderDistance() * 16.0F));
   }

   public static int skyColor(Minecraft mc, Vec3 at) {
      try {
         ClientLevel level = mc.level;
         if (level == null) {
            return 12106948;
         }

         Vec3 c = level.getSkyColor(at, mc.getDeltaFrameTime());
         int r = Mth.clamp((int)(c.x * 255.0), 0, 255);
         int g = Mth.clamp((int)(c.y * 255.0), 0, 255);
         int b = Mth.clamp((int)(c.z * 255.0), 0, 255);
         return r << 16 | g << 8 | b;
      } catch (Throwable t) {
         return 12106948;
      }
   }

   private static void set(EffectInstance e, String name, float value) {
      AbstractUniform u = e.safeGetUniform(name);
      if (u != null) {
         u.set(value);
      }
   }

   private static void vec(EffectInstance e, String name, float x, float y, float z) {
      AbstractUniform u = e.safeGetUniform(name);
      if (u != null) {
         u.set(x, y, z);
      }
   }

   private static void vec4(EffectInstance e, String name, float x, float y, float z, float w) {
      AbstractUniform u = e.safeGetUniform(name);
      if (u != null) {
         u.set(new Vector4f(x, y, z, w));
      }
   }

   public record Ball(Vec3 center, float radius, float density, long bornMs, long lifeMs) {
      public float fadeNow() {
         long now = EffectClock.now();
         long age = now - this.bornMs;
         if (age >= 0L && age <= this.lifeMs) {
            float in = Mth.clamp((float)age / 220.0F, 0.0F, 1.0F);
            long left = this.lifeMs - age;
            float out = Mth.clamp((float)left / 520.0F, 0.0F, 1.0F);
            return in * in * (out * out);
         } else {
            return 0.0F;
         }
      }
   }
}

