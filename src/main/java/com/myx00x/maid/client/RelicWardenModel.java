package com.myx00x.maid.client;

import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.mob.RelicWardenEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class RelicWardenModel extends HierarchicalModel<RelicWardenEntity> {
   public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("goddess_of_maids", "relic_warden"), "main");
   private static final float DEG = (float) (Math.PI / 180.0);
   private static final boolean DIAG = false;
   private static final float HEAD_REST_Y = -10.0F;
   private static final float CANNON_REST_Y = -26.0F;
   private static final float CANNON_CRADLE_REST_Y = -34.0F;
   private static final float TAIL_REST_Y = -13.0F;
   private static final float BODY_BOB = 1.6F;
   private static final float CRASH_BODY_PITCH = 30.0F;
   private static final float CRASH_BACK_LEG = 38.0F;
   private static final float CRASH_FRONT_UP = 34.0F;
   private int loggedGaitTick = -1;
   private float legLift;
   private static final float LEG_RISE_TILT = 22.0F;
   private static final float LEG_LIFT_SMOOTH = 0.35F;
   private float chargeDamp;
   private static final float CHARGE_DAMP_STEP = 0.05F;
   private static final float CRASH_FRONT_LIFT = 10.0F;
   private static final float BODY_RISE_UP = 5.0F;
   private static final float HEAD_RISE_UP = 4.0F;
   private static final float SPIKE_THRUST_GE = 1.6F;
   private static final float HEAD_YAW_LIMIT = (float) (Math.PI / 2);
   private static final float HEAD_PITCH_DOWN = (float) (Math.PI / 10);
   private static final float HEAD_PITCH_UP = (float) (Math.PI / 15);
   private static final float HEAD_BASE_R = 40.0F;
   private static final float HEAD_YAW_PUSH = 9.0F;
   private static final float HEAD_PITCH_PUSH = 8.0F;
   private static final float DORMANT_BODY_SINK = 17.0F;
   private static final float DORMANT_HEAD_SINK = 9.0F;
   private static final float DORMANT_HEAD_FWD = 5.0F;
   private static final float DORMANT_HEAD_PITCH = (float) (Math.PI / 10);
   private static final float DORMANT_LEG_SINK = 20.0F;
   private static final float DORMANT_LEG_OUT = 1.43117F;
   private float fwdPrev = -1.0F;
   private final ModelPart root;
   private final ModelPart head;
   private final ModelPart jaw;
   private final ModelPart horns;
   private final ModelPart spike;
   private final ModelPart body;
   private final ModelPart cannonBase;
   private final ModelPart cannonCradle;
   private final ModelPart tail;
   private final ModelPart legFrontLeft;
   private final ModelPart legFrontRight;
   private final ModelPart legBackLeft;
   private final ModelPart legBackRight;

   public RelicWardenModel(ModelPart root) {
      this.root = root;
      this.head = root.getChild("head");
      this.jaw = this.head.getChild("jaw");
      this.horns = this.head.getChild("horns");
      this.spike = this.head.getChild("spike");
      this.body = root.getChild("body");
      this.cannonBase = this.body.getChild("cannon_base");
      this.cannonCradle = this.body.getChild("cannon_cradle");
      this.tail = root.getChild("tail");
      this.legFrontLeft = root.getChild("leg_front_left");
      this.legFrontRight = root.getChild("leg_front_right");
      this.legBackLeft = root.getChild("leg_back_left");
      this.legBackRight = root.getChild("leg_back_right");
   }

   private static void addLamp(PartDefinition cradle, String name, int u, int v, float x, float z) {
      cradle.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).addBox(x, -15.0F, z, 2.0F, 1.0F, 2.0F), PartPose.ZERO);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      PartDefinition body = root.addOrReplaceChild(
         "body",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-20.0F, -18.0F, -26.0F, 40.0F, 24.0F, 50.0F)
            .texOffs(0, 75)
            .addBox(-19.0F, -23.0F, -24.0F, 38.0F, 5.0F, 46.0F)
            .texOffs(0, 127)
            .addBox(-16.0F, -26.0F, -20.0F, 32.0F, 3.0F, 38.0F)
            .texOffs(0, 169)
            .addBox(-22.0F, -20.0F, -22.0F, 4.0F, 22.0F, 26.0F)
            .texOffs(61, 169)
            .addBox(18.0F, -20.0F, -22.0F, 4.0F, 22.0F, 26.0F),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition head = root.addOrReplaceChild(
         "head",
         CubeListBuilder.create()
            .texOffs(141, 127)
            .addBox(-14.0F, -10.0F, -50.0F, 28.0F, 20.0F, 20.0F)
            .texOffs(41, 218)
            .addBox(-9.0F, -1.0F, -55.0F, 18.0F, 11.0F, 8.0F)
            .texOffs(141, 168)
            .addBox(-15.0F, -13.0F, -48.0F, 30.0F, 4.0F, 17.0F),
         PartPose.offset(0.0F, -10.0F, 0.0F)
      );
      body.addOrReplaceChild(
         "cannon_base",
         CubeListBuilder.create()
            .texOffs(169, 75)
            .addBox(-11.0F, -4.0F, -10.0F, 22.0F, 4.0F, 20.0F)
            .texOffs(220, 190)
            .addBox(-7.0F, -8.0F, -6.0F, 14.0F, 4.0F, 12.0F),
         PartPose.offset(0.0F, -26.0F, -4.0F)
      );
      PartDefinition cannon_cradle = body.addOrReplaceChild(
         "cannon_cradle",
         CubeListBuilder.create()
            .texOffs(196, 115)
            .addBox(5.0F, -4.0F, -10.0F, 5.0F, 1.0F, 5.0F)
            .texOffs(169, 100)
            .addBox(-10.0F, -3.0F, -10.0F, 20.0F, 2.0F, 20.0F)
            .texOffs(196, 115)
            .addBox(5.0F, -4.0F, 5.0F, 5.0F, 1.0F, 5.0F)
            .texOffs(196, 115)
            .addBox(-10.0F, -4.0F, -10.0F, 5.0F, 1.0F, 5.0F)
            .texOffs(196, 115)
            .addBox(-10.0F, -4.0F, 5.0F, 5.0F, 1.0F, 5.0F)
            .texOffs(200, 117)
            .addBox(-10.0F, -6.0F, 7.0F, 3.0F, 2.0F, 3.0F)
            .texOffs(204, 119)
            .addBox(-9.0F, -7.0F, -9.0F, 1.0F, 1.0F, 1.0F)
            .texOffs(204, 119)
            .addBox(-9.0F, -8.0F, -9.0F, 1.0F, 1.0F, 1.0F)
            .texOffs(204, 119)
            .addBox(8.0F, -7.0F, -9.0F, 1.0F, 1.0F, 1.0F)
            .texOffs(204, 119)
            .addBox(8.0F, -8.0F, -9.0F, 1.0F, 1.0F, 1.0F)
            .texOffs(204, 119)
            .addBox(8.0F, -7.0F, 8.0F, 1.0F, 1.0F, 1.0F)
            .texOffs(204, 119)
            .addBox(8.0F, -8.0F, 8.0F, 1.0F, 1.0F, 1.0F)
            .texOffs(204, 119)
            .addBox(-9.0F, -7.0F, 8.0F, 1.0F, 1.0F, 1.0F)
            .texOffs(204, 119)
            .addBox(-9.0F, -8.0F, 8.0F, 1.0F, 1.0F, 1.0F)
            .texOffs(200, 117)
            .addBox(7.0F, -6.0F, -10.0F, 3.0F, 2.0F, 3.0F)
            .texOffs(200, 117)
            .addBox(-10.0F, -6.0F, -10.0F, 3.0F, 2.0F, 3.0F)
            .texOffs(200, 117)
            .addBox(7.0F, -6.0F, 7.0F, 3.0F, 2.0F, 3.0F),
         PartPose.offset(0.0F, -34.0F, -4.0F)
      );
      addLamp(cannon_cradle, "lamp_front_right", 480, 480, -7.0F, 5.0F);
      addLamp(cannon_cradle, "lamp_front_left", 496, 480, 5.0F, 5.0F);
      addLamp(cannon_cradle, "lamp_back_left", 480, 496, 5.0F, -7.0F);
      addLamp(cannon_cradle, "lamp_back_right", 496, 496, -7.0F, -7.0F);
      root.addOrReplaceChild(
         "leg_front_left",
         CubeListBuilder.create().texOffs(179, 190).addBox(-6.0F, 0.0F, -5.0F, 10.0F, 24.0F, 10.0F),
         PartPose.offset(-15.0F, 0.0F, -18.0F)
      );
      root.addOrReplaceChild(
         "leg_front_right",
         CubeListBuilder.create().texOffs(0, 218).addBox(-4.0F, 0.0F, -5.0F, 10.0F, 24.0F, 10.0F),
         PartPose.offset(15.0F, 0.0F, -18.0F)
      );
      root.addOrReplaceChild(
         "leg_back_left",
         CubeListBuilder.create().texOffs(181, 27).addBox(-8.0F, 0.0F, -7.0F, 14.0F, 24.0F, 14.0F),
         PartPose.offset(-13.0F, 0.0F, 14.0F)
      );
      root.addOrReplaceChild(
         "leg_back_right",
         CubeListBuilder.create().texOffs(122, 190).addBox(-6.0F, 0.0F, -7.0F, 14.0F, 24.0F, 14.0F),
         PartPose.offset(13.0F, 0.0F, 14.0F)
      );
      root.addOrReplaceChild(
         "tail",
         CubeListBuilder.create()
            .texOffs(220, 207)
            .addBox(-5.0F, -5.0F, 0.0F, 10.0F, 10.0F, 9.0F)
            .texOffs(236, 168)
            .addBox(-3.0F, -3.0F, 9.0F, 6.0F, 5.0F, 6.0F),
         PartPose.offset(0.0F, -13.0F, 26.0F)
      );
      head.addOrReplaceChild(
         "horns",
         CubeListBuilder.create()
            .texOffs(179, 225)
            .addBox(-17.0F, -16.0F, -49.0F, 8.0F, 8.0F, 8.0F)
            .texOffs(94, 229)
            .addBox(-20.0F, -20.0F, -56.0F, 7.0F, 7.0F, 8.0F)
            .texOffs(212, 227)
            .addBox(9.0F, -16.0F, -49.0F, 8.0F, 8.0F, 8.0F)
            .texOffs(125, 229)
            .addBox(13.0F, -20.0F, -56.0F, 7.0F, 7.0F, 8.0F),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      head.addOrReplaceChild(
         "jaw", CubeListBuilder.create().texOffs(181, 0).addBox(-8.0F, 0.0F, -53.0F, 16.0F, 5.0F, 21.0F), PartPose.offset(0.0F, 6.0F, 0.0F)
      );
      head.addOrReplaceChild(
         "spike",
         CubeListBuilder.create()
            .texOffs(320, 0)
            .addBox(-2.5F, -2.5F, -6.0F, 5.0F, 5.0F, 6.0F)
            .texOffs(344, 0)
            .addBox(-2.0F, -2.0F, -13.0F, 4.0F, 4.0F, 7.0F)
            .texOffs(368, 0)
            .addBox(-1.5F, -1.5F, -21.0F, 3.0F, 3.0F, 8.0F)
            .texOffs(392, 0)
            .addBox(-1.0F, -1.0F, -30.0F, 2.0F, 2.0F, 9.0F)
            .texOffs(416, 0)
            .addBox(-0.5F, -0.5F, -40.0F, 1.0F, 1.0F, 10.0F),
         PartPose.offset(0.0F, -6.0F, -50.0F)
      );
      return LayerDefinition.create(mesh, 512, 512);
   }

   public ModelPart root() {
      return this.root;
   }

   public void setupAnim(RelicWardenEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      float partial = Mth.frac(ageInTicks);
      float lie = entity.dormantLerp(partial);
      float stand = 1.0F - lie;
      float bodyYaw = Mth.rotLerp(partial, entity.yBodyRotO, entity.yBodyRot);
      boolean flying = entity.headFlying();
      float rel = flying ? (float)Math.toRadians(entity.headFlyAngle() - bodyYaw) : 0.0F;
      float pin = !entity.chargeActive() && !(Math.abs(entity.chargeHeadPitch()) > 1.0F) && !entity.headSwinging() ? entity.headPin(partial) : 1.0F;
      float look = Mth.clamp(netHeadYaw * (float) (Math.PI / 180.0), (float) (-Math.PI / 2), (float) (Math.PI / 2)) * stand;
      boolean fallen = entity.deathShow(partial) >= 0.0F;
      float yaw = fallen ? 0.0F : (flying ? rel : Mth.lerp(pin, look, 0.0F));
      if (entity.headSwinging()) {
         yaw = (float)Math.toRadians(entity.swingHeadYaw(partial));
      }

      float pitch;
      if (fallen) {
         pitch = 0.0F;
      } else if (entity.headSwinging()) {
         pitch = entity.swingHeadPitch(partial) * (float) (Math.PI / 180.0);
      } else if (entity.skillTick() >= 0) {
         pitch = entity.skillHeadPitch(partial) * (float) (Math.PI / 180.0);
      } else if (!entity.chargeActive() && !(Math.abs(entity.chargeHeadPitch()) > 1.0F)) {
         pitch = Mth.lerp(lie, Mth.clamp(headPitch * (float) (Math.PI / 180.0), (float) (-Math.PI / 15), (float) (Math.PI / 10)), (float) (Math.PI / 10));
      } else {
         pitch = entity.chargeHeadPitch(partial) * (float) (Math.PI / 180.0);
      }

      float r = 40.0F + 9.0F * Mth.abs(Mth.sin(yaw * 2.0F)) + 8.0F * Mth.abs(Mth.sin(pitch));
      float out = (r - 40.0F) * (1.0F - pin);
      float fling = entity.headSwinging() ? entity.swingHeadReach(partial) * 16.0F : 0.0F;
      this.head.x = -(out + fling) * Mth.sin(yaw);
      this.head.z = -(out + fling) * Mth.cos(yaw) - 5.0F * lie;
      this.head.yRot = yaw;
      this.head.xRot = pitch;
      this.jaw.xRot = 0.0F;
      float fwd = entity.headForward(partial);
      if (entity.headRecalling() && this.fwdPrev >= 0.0F && fwd > this.fwdPrev + 0.3F) {
         ShenlinMod.LOGGER
            .info("[shenlin] 头前冲 client act={} 前伸={} 上一帧={} 冲={} 飞={}", new Object[]{entity.headActTick(), fwd, this.fwdPrev, fwd - this.fwdPrev, flying});
      }

      this.fwdPrev = fwd;
      this.head.x = this.head.x - fwd * Mth.sin(rel) * 16.0F;
      this.head.z = this.head.z - fwd * Mth.cos(rel) * 16.0F;
      float phase = Mth.lerp(partial, entity.prevGaitPhase(), entity.gaitPhase());
      this.chargeDamp = Mth.lerp(0.05F, this.chargeDamp, !entity.chargeActive() && !entity.swingGliding() ? 0.0F : 1.0F);
      float walk = entity.gaitPower(partial) * stand * (1.0F - this.chargeDamp);
      float swing = Mth.cos(phase) * 0.9F * walk;
      this.legFrontLeft.xRot = swing;
      this.legBackRight.xRot = swing;
      this.legFrontRight.xRot = -swing;
      this.legBackLeft.xRot = -swing;
      float dip = (1.0F - Mth.cos(phase * 2.0F)) * 0.5F * 1.6F * walk;
      float sink = 17.0F * lie;
      this.body.y = dip + sink;
      float hLift = entity.headSwinging() ? entity.swingHeadLift(partial) * 16.0F : 0.0F;
      this.head.y = -10.0F + dip + 9.0F * lie - entity.headLift(partial) * 16.0F - hLift;
      this.cannonBase.y = -26.0F;
      this.cannonCradle.y = -34.0F;
      this.tail.y = -13.0F + dip + sink;
      float legSink = 20.0F * lie;
      float legOut = 1.43117F * lie;
      this.legFrontLeft.y = legSink;
      this.legFrontRight.y = legSink;
      this.legBackLeft.y = legSink;
      this.legBackRight.y = legSink;
      this.legFrontLeft.zRot = legOut;
      this.legBackLeft.zRot = legOut;
      this.legFrontRight.zRot = -legOut;
      this.legBackRight.zRot = -legOut;
      float crash = entity.chargeCrash(partial);
      float rise = entity.chargeRise(partial);
      if (crash > 0.0F && entity.chargeActive()) {
         float hit = Mth.sin(Mth.clamp(crash, 0.0F, 1.0F) * (float) Math.PI) * stand;
         this.body.xRot = (float) (-Math.PI / 6) * hit;
         this.body.y = dip + sink - 5.0F * hit;
         this.legBackLeft.xRot = -0.6632251F * hit;
         this.legBackRight.xRot = -0.6632251F * hit;
         this.legFrontLeft.xRot = -0.5934119F * hit;
         this.legFrontRight.xRot = -0.5934119F * hit;
         float lift = 10.0F * hit;
         this.legFrontLeft.y -= lift;
         this.legFrontRight.y -= lift;
      } else if (rise > 0.0F) {
         this.body.xRot = -0.2443461F * rise;
         this.body.y = dip + sink - 5.0F * rise;
         float want = Mth.clamp(entity.chargeLegs(partial), 0.0F, 1.0F);
         this.legLift = this.legLift + (want - this.legLift) * 0.35F;
         this.head.y -= 4.0F * rise;
         this.legFrontLeft.xRot = -0.38397244F * this.legLift;
         this.legFrontRight.xRot = -0.38397244F * this.legLift;
         this.legFrontLeft.y = this.legFrontLeft.y - 5.0F * this.legLift;
         this.legFrontRight.y = this.legFrontRight.y - 5.0F * this.legLift;
      } else {
         this.body.xRot = 0.0F;
      }

      this.spike.zScale = Math.max(0.02F, entity.chargeSpike(partial));
      float thrust = entity.chargeThrust(partial);
      if (thrust > 0.001F) {
         this.head.z -= thrust * 1.6F * 16.0F;
      }

      if (entity.chargeActive()) {
         this.tail.yRot = 0.0F;
      } else {
         this.tail.yRot = Mth.cos(ageInTicks * 0.08F) * 0.16F * stand;
      }
   }
}

