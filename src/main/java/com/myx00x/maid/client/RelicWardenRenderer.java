package com.myx00x.maid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.myx00x.maid.mob.RelicWardenEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class RelicWardenRenderer extends MobRenderer<RelicWardenEntity, RelicWardenModel> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("goddess_of_maids", "textures/entity/relic_warden.png");

   public RelicWardenRenderer(Context context) {
      super(context, new RelicWardenModel(context.bakeLayer(RelicWardenModel.LAYER)), 1.4F);
      this.addLayer(new RelicWardenEyesLayer(this));
      this.addLayer(new RelicWardenHornLayer(this));
      this.addLayer(new RelicWardenTailLayer(this));
      this.addLayer(new RelicWardenMuzzleLayer(this));
      this.addLayer(new RelicWardenJointLayer(this));
      this.addLayer(new RelicWardenHurtLayer(this));
      this.addLayer(new RelicWardenDeathLayer(this));
   }

   public ResourceLocation getTextureLocation(RelicWardenEntity entity) {
      return TEXTURE;
   }

   public void render(RelicWardenEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      int hurt = entity.hurtTime;
      entity.hurtTime = 0;

      try {
         super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
      } finally {
         entity.hurtTime = hurt;
      }

      double hx = Mth.lerp(partialTicks, entity.xo, entity.getX());
      double hy = Mth.lerp(partialTicks, entity.yo, entity.getY());
      double hz = Mth.lerp(partialTicks, entity.zo, entity.getZ());
      RayEffect.paintOnHost(poseStack, entity, new Vec3(hx, hy, hz), buffer, EffectClock.now());
   }
}

