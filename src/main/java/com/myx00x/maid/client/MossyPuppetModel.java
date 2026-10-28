package com.myx00x.maid.client;

import com.myx00x.maid.mob.MossyPuppetBase;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class MossyPuppetModel extends HierarchicalModel<MossyPuppetBase> {
   public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("goddess_of_maids", "mossy_puppet"), "main");
   private final ModelPart root;
   private final ModelPart head;
   private final ModelPart body;
   private final ModelPart rightArm;
   private final ModelPart leftArm;
   private final ModelPart rightLeg;
   private final ModelPart leftLeg;
   private float sleepAmount;
   private float throwHeadOffset;

   public MossyPuppetModel(ModelPart root) {
      this.root = root;
      this.head = root.getChild("head");
      this.body = root.getChild("body");
      this.rightArm = root.getChild("right_arm");
      this.leftArm = root.getChild("left_arm");
      this.rightLeg = root.getChild("right_leg");
      this.leftLeg = root.getChild("left_leg");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild(
         "head",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-4.0F, -12.0F, -5.5F, 8.0F, 10.0F, 8.0F)
            .texOffs(24, 0)
            .addBox(-1.0F, -5.0F, -7.5F, 2.0F, 4.0F, 2.0F),
         PartPose.offset(0.0F, -7.0F, -2.0F)
      );
      root.addOrReplaceChild(
         "body",
         CubeListBuilder.create()
            .texOffs(0, 40)
            .addBox(-9.0F, -2.0F, -6.0F, 18.0F, 12.0F, 11.0F)
            .texOffs(0, 70)
            .addBox(-4.5F, 10.0F, -3.0F, 9.0F, 5.0F, 6.0F, new CubeDeformation(0.5F)),
         PartPose.offset(0.0F, -7.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "right_arm", CubeListBuilder.create().texOffs(60, 21).addBox(-13.0F, -2.5F, -3.0F, 4.0F, 30.0F, 6.0F), PartPose.offset(0.0F, -7.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "left_arm", CubeListBuilder.create().texOffs(60, 58).addBox(9.0F, -2.5F, -3.0F, 4.0F, 30.0F, 6.0F), PartPose.offset(0.0F, -7.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "right_leg", CubeListBuilder.create().texOffs(37, 0).addBox(-3.5F, -3.0F, -3.0F, 6.0F, 16.0F, 5.0F), PartPose.offset(-4.0F, 11.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "left_leg",
         CubeListBuilder.create().texOffs(60, 0).mirror().addBox(-3.5F, -3.0F, -3.0F, 6.0F, 16.0F, 5.0F),
         PartPose.offset(5.0F, 11.0F, 0.0F)
      );
      return LayerDefinition.create(mesh, 128, 128);
   }

   public ModelPart root() {
      return this.root;
   }

   public void setupAnim(MossyPuppetBase entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      float lookPitch = headPitch * (float) (Math.PI / 180.0);
      this.head.xRot = Mth.lerp(this.sleepAmount, lookPitch, 0.32F) + this.throwHeadOffset;
      this.head.yRot = netHeadYaw * (float) (Math.PI / 180.0) * (1.0F - this.sleepAmount);
      this.rightLeg.xRot = -1.5F * Mth.triangleWave(limbSwing, 13.0F) * limbSwingAmount;
      this.leftLeg.xRot = 1.5F * Mth.triangleWave(limbSwing, 13.0F) * limbSwingAmount;
   }

   public void prepareMobModel(MossyPuppetBase entity, float limbSwing, float limbSwingAmount, float partialTicks) {
      this.sleepAmount = entity.dormantLerp(partialTicks);
      int throwTick = entity.getThrowAnimationTick();
      int attackTick = entity.getAttackAnimationTick();
      this.body.xRot = 0.0F;
      this.throwHeadOffset = 0.0F;
      this.rightArm.yRot = 0.0F;
      this.leftArm.yRot = 0.0F;
      if (throwTick > 0) {
         float elapsed = 38 - throwTick + partialTicks;
         float arm = MossyPuppetBase.throwArmAngleAt(elapsed);
         float stir = MossyPuppetBase.throwStirAt(elapsed);
         float sweep = MossyPuppetBase.throwSweepAt(elapsed);
         this.rightArm.xRot = arm + stir;
         this.leftArm.xRot = arm - stir;
         this.rightArm.yRot = sweep;
         this.leftArm.yRot = -sweep;
         this.body.xRot = MossyPuppetBase.throwBodyAngleAt(elapsed);
         this.throwHeadOffset = MossyPuppetBase.throwHeadOffsetAt(elapsed);
      } else if (attackTick > 0) {
         this.rightArm.xRot = -2.0F + 1.5F * Mth.triangleWave(attackTick - partialTicks, 10.0F);
         this.leftArm.xRot = -2.0F + 1.5F * Mth.triangleWave(attackTick - partialTicks, 10.0F);
      } else {
         this.rightArm.xRot = (-0.2F + 1.5F * Mth.triangleWave(limbSwing, 13.0F)) * limbSwingAmount;
         this.leftArm.xRot = (-0.2F - 1.5F * Mth.triangleWave(limbSwing, 13.0F)) * limbSwingAmount;
      }
   }
}

