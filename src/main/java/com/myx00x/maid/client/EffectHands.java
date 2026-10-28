package com.myx00x.maid.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.myx00x.maid.ShenlinMod;
import net.minecraft.client.Minecraft;

public final class EffectHands {
   private static long copiedAt;
   private static TextureTarget cleanScene;
   private static int copyWidth = -1;
   private static int copyHeight = -1;
   private static boolean logged;
   private static long lastLog;

   private EffectHands() {
   }

   public static void capture(Minecraft mc, long now) {
      if (ShockwaveEffect.pending() || BlackHoleEffect.pending() || ShockWarpEffect.pending()) {
         if (copiedAt == 0L || now - copiedAt > 5L) {
            try {
               RenderTarget main = mc.getMainRenderTarget();
               if (cleanScene == null || copyWidth != main.width || copyHeight != main.height) {
                  if (cleanScene != null) {
                     cleanScene.destroyBuffers();
                  }

                  cleanScene = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
                  copyWidth = main.width;
                  copyHeight = main.height;
               }

               GlStateManager._glBindFramebuffer(36008, main.frameBufferId);
               GlStateManager._glBindFramebuffer(36009, cleanScene.frameBufferId);
               GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, copyWidth, copyHeight, 16384, 9728);
               main.bindWrite(true);
               copiedAt = now;
               if (!logged) {
                  logged = true;
                  ShenlinMod.LOGGER.info("[shenlin] clean frame capture on (waves and holes sample it)");
               } else if (now - lastLog > 5000L) {
                  lastLog = now;
                  ShenlinMod.LOGGER.info("[shenlin] wave clean frames");
               }
            } catch (Throwable t) {
               try {
                  mc.getMainRenderTarget().bindWrite(true);
               } catch (Throwable var6) {
               }

               if (cleanScene != null) {
                  try {
                     cleanScene.destroyBuffers();
                  } catch (Throwable var5) {
                  }

                  cleanScene = null;
               }

               copyWidth = -1;
               copyHeight = -1;
               ShenlinMod.LOGGER.warn("[shenlin] clean frame capture failed: {}", t.toString());
            }
         }
      }
   }

   public static RenderTarget clean(long now) {
      return cleanScene != null && now - copiedAt <= 5L ? cleanScene : null;
   }
}

