package com.myx00x.maid.mob;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class WardenLaserBeamEntity extends Entity {
   public static final double RADIUS = 64.0;
   private static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(WardenLaserBeamEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> PITCH = SynchedEntityData.defineId(WardenLaserBeamEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(WardenLaserBeamEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> FIXED_FRAME = SynchedEntityData.defineId(WardenLaserBeamEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> WARM = SynchedEntityData.defineId(WardenLaserBeamEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> FADE = SynchedEntityData.defineId(WardenLaserBeamEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> WIDTH = SynchedEntityData.defineId(WardenLaserBeamEntity.class, EntityDataSerializers.FLOAT);
   public double collidePosX;
   public double collidePosY;
   public double collidePosZ;
   public double prevCollidePosX;
   public double prevCollidePosY;
   public double prevCollidePosZ;
   public float renderYaw;
   public float renderPitch;
   public float prevYaw;
   public float prevPitch;
   public int appear;
   public boolean on = true;
   public Direction blockSide;
   private double endPosX;
   private double endPosY;
   private double endPosZ;
   private int life;
   private boolean aimedThisTick;
   private boolean primed;
   private static final int FADE_TICKS = 8;
   private int fadeLife;
   private int fadeTicks = 8;
   private static final EntityDataAccessor<Boolean> SPRAY = SynchedEntityData.defineId(WardenLaserBeamEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> STAR_BLUE = SynchedEntityData.defineId(WardenLaserBeamEntity.class, EntityDataSerializers.BOOLEAN);
   public boolean starSpawned;

   public WardenLaserBeamEntity(EntityType<? extends WardenLaserBeamEntity> type, Level level) {
      super(type, level);
      this.blocksBuilding = true;
      this.noPhysics = true;
      this.setNoGravity(true);
      this.noCulling = true;
   }

   public void aim(Vec3 from, Vec3 to, int lifeTicks) {
      this.aim(from, to, lifeTicks, -1, false);
   }

   public void aim(Vec3 from, Vec3 to, int lifeTicks, int fixedFrame) {
      this.aim(from, to, lifeTicks, fixedFrame, false);
   }

   public void aim(Vec3 from, Vec3 to, int lifeTicks, int fixedFrame, boolean warm) {
      this.aim(from, to, lifeTicks, fixedFrame, warm, 8);
   }

   public void aim(Vec3 from, Vec3 to, int lifeTicks, int fixedFrame, boolean warm, int fadeTicks) {
      this.aim(from, to, lifeTicks, fixedFrame, warm, fadeTicks, 1.0F);
   }

   public void aim(Vec3 from, Vec3 to, int lifeTicks, int fixedFrame, boolean warm, float widthScale) {
      this.aim(from, to, lifeTicks, fixedFrame, warm, 8, widthScale);
   }

   public void aim(Vec3 from, Vec3 to, int lifeTicks, int fixedFrame, boolean warm, int fadeTicks, float widthScale) {
      this.setPos(from.x, from.y, from.z);
      this.xo = from.x;
      this.yo = from.y;
      this.zo = from.z;
      Vec3 d = to.subtract(from);
      double flat = Math.sqrt(d.x * d.x + d.z * d.z);
      this.entityData.set(YAW, (float)Math.atan2(d.z, d.x));
      this.entityData.set(PITCH, (float)Math.atan2(d.y, flat));
      this.entityData.set(LIFE, Math.max(1, lifeTicks));
      this.entityData.set(FIXED_FRAME, fixedFrame);
      this.entityData.set(WARM, warm);
      this.entityData.set(FADE, -1);
      this.fadeLife = 0;
      this.fadeTicks = Math.max(0, fadeTicks);
      this.entityData.set(WIDTH, Math.max(0.05F, widthScale));
      this.life = Math.max(1, lifeTicks);
      this.on = true;
      this.aimedThisTick = true;
   }

   public void tick() {
      super.tick();
      this.prevCollidePosX = this.collidePosX;
      this.prevCollidePosY = this.collidePosY;
      this.prevCollidePosZ = this.collidePosZ;
      this.prevYaw = this.renderYaw;
      this.prevPitch = this.renderPitch;
      if (this.level().isClientSide) {
         this.renderYaw = this.getYaw();
         this.renderPitch = this.getPitch();
      }

      if (!this.level().isClientSide) {
         if (this.aimedThisTick) {
            this.aimedThisTick = false;
            this.on = true;
            this.fadeLife = 0;
            this.entityData.set(FADE, -1);
         } else if (--this.life <= 0 && this.on) {
            this.on = false;
            this.fadeLife = Math.max(0, this.fadeTicks);
            this.entityData.set(FADE, this.fadeLife > 0 ? 1000 : 0);
         }
      }

      if (!this.level().isClientSide && this.fadeLife > 0) {
         this.fadeLife--;
         this.entityData.set(FADE, (int)(this.fadeLife * 1000L / Math.max(1, this.fadeTicks)));
      }

      if (!this.on && this.appear == 0 && this.fadeLife <= 0) {
         this.discard();
      } else {
         if (this.on) {
            if (this.appear < 3) {
               this.appear++;
            }
         } else if (this.appear > 0) {
            this.appear--;
         }

         this.calculateEndPos();
         this.raycast();
         if (!this.primed) {
            this.prevCollidePosX = this.collidePosX;
            this.prevCollidePosY = this.collidePosY;
            this.prevCollidePosZ = this.collidePosZ;
            this.prevYaw = this.renderYaw;
            this.prevPitch = this.renderPitch;
            this.primed = true;
         }
      }
   }

   private void calculateEndPos() {
      float yaw = this.level().isClientSide ? this.renderYaw : this.getYaw();
      float pitch = this.level().isClientSide ? this.renderPitch : this.getPitch();
      this.endPosX = this.getX() + 64.0 * Math.cos(yaw) * Math.cos(pitch);
      this.endPosZ = this.getZ() + 64.0 * Math.sin(yaw) * Math.cos(pitch);
      this.endPosY = this.getY() + 64.0 * Math.sin(pitch);
   }

   private void raycast() {
      BlockHitResult hit = this.level()
         .clip(
            new ClipContext(
               new Vec3(this.getX(), this.getY(), this.getZ()),
               new Vec3(this.endPosX, this.endPosY, this.endPosZ),
               Block.COLLIDER,
               Fluid.NONE,
               this
            )
         );
      if (hit.getType() == Type.BLOCK) {
         Vec3 at = hit.getLocation();
         this.collidePosX = at.x;
         this.collidePosY = at.y;
         this.collidePosZ = at.z;
         this.blockSide = hit.getDirection();
      } else {
         this.collidePosX = this.endPosX;
         this.collidePosY = this.endPosY;
         this.collidePosZ = this.endPosZ;
         this.blockSide = null;
      }
   }

   public float getYaw() {
      return (Float)this.entityData.get(YAW);
   }

   public float getPitch() {
      return (Float)this.entityData.get(PITCH);
   }

   public int fixedFrame() {
      return (Integer)this.entityData.get(FIXED_FRAME);
   }

   public float fade() {
      int f = (Integer)this.entityData.get(FADE);
      return f < 0 ? 1.0F : Math.max(0.0F, Math.min(1.0F, f / 1000.0F));
   }

   public float widthScale() {
      return (Float)this.entityData.get(WIDTH);
   }

   public void setSpray(boolean spray) {
      this.entityData.set(SPRAY, spray);
   }

   public void setBlueStar(boolean on) {
      this.entityData.set(STAR_BLUE, on);
   }

   public boolean blueStar() {
      return (Boolean)this.entityData.get(STAR_BLUE);
   }

   public boolean spray() {
      return (Boolean)this.entityData.get(SPRAY);
   }

   public boolean readyToRender() {
      return this.primed;
   }

   public boolean warm() {
      return (Boolean)this.entityData.get(WARM);
   }

   protected void defineSynchedData() {
      this.entityData.define(YAW, 0.0F);
      this.entityData.define(PITCH, 0.0F);
      this.entityData.define(LIFE, 1);
      this.entityData.define(FIXED_FRAME, -1);
      this.entityData.define(WARM, false);
      this.entityData.define(FADE, -1);
      this.entityData.define(WIDTH, 1.0F);
      this.entityData.define(SPRAY, false);
      this.entityData.define(STAR_BLUE, false);
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
   }

   public boolean shouldRenderAtSqrDistance(double distance) {
      double reach = 128.0;
      return distance < reach * reach;
   }
}

