package com.myx00x.maid.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.myx00x.maid.ShenlinMod;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent.Post;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.lwjgl.opengl.GL11;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT, bus = Bus.FORGE)
public final class FlashEffect {
   private static final ResourceLocation POST = ResourceLocation.fromNamespaceAndPath("goddess_of_maids", "shaders/post/flash.json");
   private static PostChain chain;
   private static boolean tried;
   private static boolean broken;
   private static int chainEpoch = PostChainRebuild.epoch();
   private static int lastWidth = -1;
   private static int lastHeight = -1;
   private static long flashStart;
   private static long flashEnd;
   private static boolean logged;
   private static Field passesField;
   private static boolean passesBroken;
   private static float peakAt;
   private static boolean keepColor;

   private FlashEffect() {
   }

   public static void trigger(int ticks) {
      start(ticks, 0.0F, true);
   }

   public static void triggerSlam(int ticks) {
      start(ticks, -1.0F, false);
   }

   public static void triggerPeaked(int ticks) {
      start(ticks, 0.5F, false);
   }

   private static void start(int ticks, float peakFrac, boolean desat) {
      if (ticks > 0) {
         long now = EffectClock.now();
         if (now >= flashEnd) {
            flashStart = now;
         }

         flashEnd = Math.max(flashEnd, now + ticks * 50L);
         peakAt = peakFrac;
         keepColor = !desat;
      }
   }

   @SubscribeEvent
   public static void onGuiPost(Post event) {
      long now = EffectClock.now();
      long span = flashEnd - flashStart;
      if (span > 0L && now < flashEnd) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.level != null) {
            float progress = (float)(now - flashStart) / (float)span;
            PostChain active = chain(mc);
            if (active != null && shade(active, progress)) {
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
                     ShenlinMod.LOGGER.info("[shenlin] flash chain active");
                  }
               } catch (Throwable t) {
                  broken = true;
                  closeChain();
                  ShenlinMod.LOGGER.warn("[shenlin] flash chain failed: {}", t.toString());
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
            } else {
               overlay(event, progress);
            }
         } else {
            flashEnd = 0L;
         }
      }
   }

   private static boolean shade(PostChain active, float progress) {
      List<PostPass> passes = passes(active);
      if (passes.isEmpty()) {
         return false;
      }

      float white;
      if (peakAt < -0.001F) {
         white = Math.max(0.0F, 1.0F - progress) * 0.95F;
      } else if (peakAt <= 0.001F) {
         white = Math.max(0.0F, 1.0F - progress * 4.0F) * 0.9F;
      } else {
         float rise = progress <= peakAt ? progress / peakAt : (1.0F - progress) / (1.0F - peakAt);
         white = Math.max(0.0F, Math.min(1.0F, rise)) * 0.9F;
      }

      float tail = Math.max(0.0F, Math.min(1.0F, (progress - 0.7F) / 0.3F));
      float desaturate = keepColor ? 0.0F : 1.0F - tail * tail;

      try {
         for (PostPass pass : passes) {
            EffectInstance effect = pass.getEffect();
            if (effect != null) {
               AbstractUniform strength = effect.safeGetUniform("FlashStrength");
               if (strength != null) {
                  strength.set(desaturate);
               }

               AbstractUniform flash = effect.safeGetUniform("FlashWhite");
               if (flash != null) {
                  flash.set(white);
               }
            }
         }

         return true;
      } catch (Throwable t) {
         return false;
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

   private static void overlay(Post event, float progress) {
      float white;
      if (peakAt < -0.001F) {
         white = Math.max(0.0F, 1.0F - progress) * 0.9F;
      } else if (peakAt <= 0.001F) {
         white = Math.max(0.0F, 1.0F - progress * 3.0F) * 0.85F;
      } else {
         float rise = progress <= peakAt ? progress / peakAt : (1.0F - progress) / (1.0F - peakAt);
         white = Math.max(0.0F, Math.min(1.0F, rise)) * 0.85F;
      }

      int alpha = (int)(white * 255.0F);
      if (alpha > 0) {
         event.getGuiGraphics().fill(0, 0, event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight(), alpha << 24 | 16777215);
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
               ShenlinMod.LOGGER.warn("[shenlin] flash chain resize failed: {}", t.toString());
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
            ShenlinMod.LOGGER.warn("[shenlin] flash post chain unavailable: {}", t.toString());
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
         ShenlinMod.LOGGER.info("[shenlin] flash 那条链子重建（资源重载过）");
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

