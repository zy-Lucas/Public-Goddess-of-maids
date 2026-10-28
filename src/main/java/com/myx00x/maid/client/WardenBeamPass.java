package com.myx00x.maid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.myx00x.maid.mob.WardenLaserBeamEntity;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class WardenBeamPass {
   public static final boolean PASS_ENABLED = true;
   private static final List<WardenLaserBeamEntity> FRAME = new ArrayList<>();

   private WardenBeamPass() {
   }

   public static boolean pending() {
      collect();
      return !FRAME.isEmpty();
   }

   private static void collect() {
      FRAME.clear();
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null) {
         for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof WardenLaserBeamEntity beam && beam.spray() && !beam.isRemoved()) {
               FRAME.add(beam);
            }
         }
      }
   }

   public static void paintAtHandPass(PoseStack poseStack, Vec3 camPos, float partialTicks) {
      collect();
      if (!FRAME.isEmpty()) {
         Minecraft mc = Minecraft.getInstance();
         BufferSource buffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
         Set<RenderType> used = new LinkedHashSet<>();

         for (WardenLaserBeamEntity beam : FRAME) {
            if (mc.getEntityRenderDispatcher().getRenderer(beam) instanceof WardenLaserBeamRenderer renderer) {
               double px = Mth.lerp(partialTicks, beam.xOld, beam.getX());
               double py = Mth.lerp(partialTicks, beam.yOld, beam.getY());
               double pz = Mth.lerp(partialTicks, beam.zOld, beam.getZ());
               poseStack.pushPose();
               poseStack.translate(px - camPos.x, py - camPos.y, pz - camPos.z);
               renderer.render(beam, 0.0F, partialTicks, poseStack, buffer, 15728880);
               poseStack.popPose();
               RenderType type = ModRenderTypes.GLOWING.apply(renderer.getTextureLocation(beam));
               buffer.endBatch(type);
               used.add(type);
            }
         }
      }
   }
}

