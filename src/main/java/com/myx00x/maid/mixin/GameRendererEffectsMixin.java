package com.myx00x.maid.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.client.BlackHoleEffect;
import com.myx00x.maid.client.CannonShotEffect;
import com.myx00x.maid.client.CannonSpinEffect;
import com.myx00x.maid.client.EffectCapture;
import com.myx00x.maid.client.EffectChannel;
import com.myx00x.maid.client.EffectClock;
import com.myx00x.maid.client.OrbitCannonEffect;
import com.myx00x.maid.client.RayEffect;
import com.myx00x.maid.client.SafeCamera;
import com.myx00x.maid.client.ShaderPack;
import com.myx00x.maid.client.ShockWarpEffect;
import com.myx00x.maid.client.ShockwaveEffect;
import com.myx00x.maid.client.SoloBeamEffect;
import com.myx00x.maid.client.StarEffect;
import com.myx00x.maid.client.VolumeFogEffect;
import com.myx00x.maid.client.WardenBeamPass;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameRenderer.class, remap = false)
public class GameRendererEffectsMixin {
   private static final String RENDER_LEVEL = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V";
   private static final String CLEAR = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V";
   private static long lastLog;
   private static int frames;
   private static boolean wasBusy;

   @Inject(
      method = "m_109089_",
      remap = false,
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V",
         shift = Shift.AFTER
      )
   )
   private void shenlin.paintAfterLevel(float partialTick, long finishNanos, PoseStack poseStack, CallbackInfo ci) {
      long now = EffectClock.now();
      if (ShaderPack.inUse() && !EffectChannel.framePassAlive(now)) {
         EffectChannel.noteAtFrame(false);
         paintAll(partialTick, now, false);
      }
   }

   @Inject(
      method = "m_109093_",
      remap = false,
      at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V", ordinal = 0, shift = Shift.BEFORE)
   )
   private void shenlin.paintAfterFrame(float partialTick, long nanoTime, boolean renderLevelFlag, CallbackInfo ci) {
      if (ShaderPack.inUse()) {
         long now = EffectClock.now();
         EffectChannel.markFramePass(now);
         EffectChannel.noteAtFrame(true);
         paintAll(partialTick, now, true);
      }
   }

   private static void paintAll(float partialTick, long now, boolean primary) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.player != null) {
         String before = sizes();
         boolean want = RayEffect.pending()
            || StarEffect.pending()
            || ShockwaveEffect.pending()
            || ShockWarpEffect.pending()
            || BlackHoleEffect.pending()
            || OrbitCannonEffect.pending()
            || CannonShotEffect.pending()
            || CannonSpinEffect.pending()
            || SoloBeamEffect.pending()
            || VolumeFogEffect.pending()
            || WardenBeamPass.pending();
         frames++;
         if (!want) {
            if (wasBusy) {
               report(now, primary, before, 0, 0, 0, 0, 0, 0, 0, 0, false);
               wasBusy = false;
            }
         } else {
            wasBusy = true;
            boolean captured = EffectCapture.ready();
            Camera camera = mc.gameRenderer.getMainCamera();
            RenderSystem.backupProjectionMatrix();
            PoseStack viewStack = RenderSystem.getModelViewStack();
            viewStack.pushPose();

            try {
               if (captured) {
                  RenderSystem.setProjectionMatrix(EffectCapture.projection(), VertexSorting.DISTANCE_TO_ORIGIN);
                  viewStack.last().pose().set(EffectCapture.viewForCameraRelative());
               } else {
                  GameRendererAccessor access = (GameRendererAccessor)mc.gameRenderer;
                  double fov = access.shenlin.getFov(camera, partialTick, true);
                  RenderSystem.setProjectionMatrix(mc.gameRenderer.getProjectionMatrix(fov), VertexSorting.DISTANCE_TO_ORIGIN);
                  PoseStack view = new PoseStack();
                  if ((Boolean)mc.options.bobView().get()) {
                     access.shenlin.bobView(view, partialTick);
                  }

                  access.shenlin.bobHurt(view, partialTick);
                  view.mulPose(Axis.XP.rotationDegrees(SafeCamera.xRot(mc)));
                  view.mulPose(Axis.YP.rotationDegrees(SafeCamera.yRot(mc) + 180.0F));
                  viewStack.last().pose().set(view.last().pose());
               }

               RenderSystem.applyModelViewMatrix();
               settle(mc);
               Vec3 cameraPos = SafeCamera.pos(mc);
               PoseStack unit = new PoseStack();
               boolean compat = ShaderPack.inUse();
               int ray = RayEffect.paint(unit, cameraPos, now);
               int star = StarEffect.paint(unit, cameraPos, now, compat);
               int orbit = OrbitCannonEffect.paint(unit, cameraPos, partialTick, now);
               int shot = CannonShotEffect.paint(unit, cameraPos, partialTick, now);
               int spin = CannonSpinEffect.paint(unit, cameraPos, partialTick);
               int solo = SoloBeamEffect.paint(unit, cameraPos, partialTick, now);
               if (WardenBeamPass.pending()) {
                  try {
                     WardenBeamPass.paintAtHandPass(unit, cameraPos, partialTick);
                  } catch (Throwable t) {
                     ShenlinMod.LOGGER.warn("[shenlin] warden pull-beam shader-pass draw failed", t);
                  }
               }

               int hole = 0;
               int shock = 0;
               report(now, primary, before, ray, star, shock, hole, orbit, shot, spin, solo, captured);
            } catch (Throwable t) {
               ShenlinMod.LOGGER.warn("[shenlin] frame pass failed: {}", t.toString());
            } finally {
               viewStack.popPose();
               RenderSystem.applyModelViewMatrix();
               RenderSystem.restoreProjectionMatrix();
            }
         }
      }
   }

   private static void report(
      long now, boolean primary, String before, int ray, int star, int shock, int hole, int orbit, int shot, int spin, int solo, boolean captured
   ) {
      if (now - lastLog > 1000L) {
         long gap = now - lastLog;
         lastLog = now;
         int fps = gap > 0L ? (int)(frames * 1000L / gap) : frames;
         frames = 0;
         ShenlinMod.LOGGER
            .info(
               "[shenlin] frame pass{} compat={} in=[{}] verts=[{},{},{},{},{},{},{},{}] out=[{}] lead={} expired={} fps={} captured={} clock={}/{}",
               new Object[]{
                  primary ? "" : " (fallback)",
                  ShaderPack.inUse(),
                  before,
                  ray,
                  star,
                  shock,
                  hole,
                  orbit,
                  shot,
                  spin,
                  solo,
                  sizes(),
                  RayEffect.leadSkipped(),
                  RayEffect.expiredRemoved(),
                  fps,
                  captured,
                  now,
                  Util.getMillis()
               }
            );
      }
   }

   private static void settle(Minecraft mc) {
      mc.getMainRenderTarget().bindWrite(true);
      RenderSystem.disableScissor();
      RenderSystem.colorMask(false, false, false, false);
      RenderSystem.colorMask(true, true, true, true);
      RenderSystem.enableBlend();
      RenderSystem.enableDepthTest();
      RenderSystem.depthFunc(515);
      RenderSystem.depthMask(true);
      RenderSystem.enableCull();
   }

   private static String sizes() {
      return RayEffect.count()
         + ","
         + StarEffect.count()
         + ","
         + ShockwaveEffect.count()
         + ","
         + ShockWarpEffect.count()
         + ","
         + BlackHoleEffect.count()
         + ","
         + OrbitCannonEffect.count()
         + ","
         + CannonShotEffect.count()
         + ","
         + CannonSpinEffect.count()
         + ","
         + SoloBeamEffect.count();
   }
}

