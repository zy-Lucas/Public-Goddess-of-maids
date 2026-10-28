package com.myx00x.maid.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.client.BlackHoleEffect;
import com.myx00x.maid.client.CannonShotEffect;
import com.myx00x.maid.client.CannonSpinEffect;
import com.myx00x.maid.client.EffectChannel;
import com.myx00x.maid.client.EffectClock;
import com.myx00x.maid.client.EffectHands;
import com.myx00x.maid.client.HoleSounds;
import com.myx00x.maid.client.OrbitCannonEffect;
import com.myx00x.maid.client.RayEffect;
import com.myx00x.maid.client.ShaderPack;
import com.myx00x.maid.client.ShockWarpEffect;
import com.myx00x.maid.client.ShockwaveEffect;
import com.myx00x.maid.client.SoloBeamEffect;
import com.myx00x.maid.client.SoloBeamSound;
import com.myx00x.maid.client.StarEffect;
import com.myx00x.maid.client.VolumeFogEffect;
import com.myx00x.maid.client.WardenBeamPass;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameRenderer.class, remap = false)
public class GameRendererHandMixin {
   @Shadow
   @Final
   private Camera mainCamera;
   private static final String RENDER_LEVEL = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V";

   @Inject(
      method = "m_109089_",
      remap = false,
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V",
         shift = Shift.AFTER
      )
   )
   private void shenlin.paintBeforeHand(float partialTick, long finishNanos, PoseStack poseStack, CallbackInfo ci) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.player != null) {
         long now = EffectClock.now();
         EffectChannel.markHandPass(now);
         boolean waves = ShockwaveEffect.pending();
         boolean holes = BlackHoleEffect.pending();
         boolean warps = ShockWarpEffect.pending();
         boolean fogs = VolumeFogEffect.pending();
         boolean beams = !ShaderPack.inUse()
            && (
               RayEffect.pending()
                  || StarEffect.pending()
                  || OrbitCannonEffect.pending()
                  || CannonShotEffect.pending()
                  || CannonSpinEffect.pending()
                  || SoloBeamEffect.pending()
                  || WardenBeamPass.pending()
            );
         if (waves || holes || warps || fogs || beams) {
            BlackHoleEffect.advanceAll(now);
            ShockWarpEffect.advanceAll(now);
            if (BlackHoleEffect.anyVisible() || ShockWarpEffect.anyVisible()) {
               EffectHands.capture(mc, now);
            }

            if (waves) {
               try {
                  ShockwaveEffect.paintAtCapture(poseStack, this.mainCamera, now);
               } catch (Throwable t) {
                  ShockwaveEffect.dropAll();
                  ShenlinMod.LOGGER.warn("[shenlin] shockwave hand-pass draw failed: {}", t.toString());
               }
            }

            if (warps) {
               try {
                  ShockWarpEffect.paintAtCapture(poseStack, this.mainCamera, now, ShaderPack.inUse());
               } catch (Throwable t) {
                  ShockWarpEffect.dropAll();
                  ShenlinMod.LOGGER.warn("[shenlin] shock warp hand-pass draw failed", t);
               }
            }

            if (fogs) {
               try {
                  VolumeFogEffect.paintAtCapture(poseStack, this.mainCamera, now);
               } catch (Throwable t) {
                  VolumeFogEffect.dropAll();
                  ShenlinMod.LOGGER.warn("[shenlin] volume fog hand-pass draw failed: {}", t.toString());
               }
            }

            if (beams) {
               Vec3 camPos = this.mainCamera.getPosition();
               if (RayEffect.pending()) {
                  try {
                     RayEffect.paintAtHandPass(poseStack, camPos, now);
                  } catch (Throwable t) {
                     ShenlinMod.LOGGER.warn("[shenlin] ray hand-pass draw failed", t);
                  }
               }

               if (WardenBeamPass.pending()) {
                  try {
                     WardenBeamPass.paintAtHandPass(poseStack, camPos, partialTick);
                  } catch (Throwable t) {
                     ShenlinMod.LOGGER.warn("[shenlin] warden pull-beam hand-pass draw failed", t);
                  }
               }

               if (StarEffect.pending()) {
                  try {
                     StarEffect.paintAtHandPass(poseStack, camPos, partialTick, now);
                  } catch (Throwable t) {
                     ShenlinMod.LOGGER.warn("[shenlin] star hand-pass draw failed", t);
                  }
               }

               if (OrbitCannonEffect.pending()) {
                  try {
                     OrbitCannonEffect.paintAtHandPass(poseStack, camPos, partialTick, now);
                  } catch (Throwable t) {
                     ShenlinMod.LOGGER.warn("[shenlin] orbit cannon hand-pass draw failed", t);
                  }
               }

               if (CannonShotEffect.pending()) {
                  try {
                     CannonShotEffect.paintAtHandPass(poseStack, camPos, partialTick, now);
                  } catch (Throwable t) {
                     ShenlinMod.LOGGER.warn("[shenlin] cannon shot hand-pass draw failed", t);
                  }
               }

               if (CannonSpinEffect.pending()) {
                  try {
                     CannonSpinEffect.paintAtHandPass(poseStack, camPos, partialTick, now);
                  } catch (Throwable t) {
                     ShenlinMod.LOGGER.warn("[shenlin] cannon spin hand-pass draw failed", t);
                  }
               }

               if (SoloBeamEffect.pending()) {
                  try {
                     SoloBeamEffect.paintAtHandPass(poseStack, camPos, partialTick, now);
                  } catch (Throwable t) {
                     ShenlinMod.LOGGER.warn("[shenlin] solo beam hand-pass draw failed", t);
                  }
               }
            }

            if (holes) {
               try {
                  BlackHoleEffect.paintAtCapture(poseStack, this.mainCamera, now, ShaderPack.inUse());
               } catch (Throwable t) {
                  BlackHoleEffect.dropAll();
                  ShenlinMod.LOGGER.warn("[shenlin] black hole hand-pass draw failed", t);
               }
            }
         }
      } else {
         HoleSounds.clear();
         BlackHoleEffect.dropAll();
         ShockwaveEffect.dropAll();
         ShockWarpEffect.dropAll();
         RayEffect.dropAll();
         StarEffect.dropAll();
         OrbitCannonEffect.dropAll();
         CannonShotEffect.dropAll();
         CannonSpinEffect.dropAll();
         SoloBeamEffect.dropAll();
         SoloBeamSound.dropAll();
      }
   }
}

