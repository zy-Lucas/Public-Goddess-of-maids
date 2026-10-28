package com.myx00x.maid.mob;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class WardenBeamEntity extends Entity {
   private static final EntityDataAccessor<Vector3f> DATA_OFFSET = SynchedEntityData.defineId(WardenBeamEntity.class, EntityDataSerializers.VECTOR3);
   private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(WardenBeamEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_FULL = SynchedEntityData.defineId(WardenBeamEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> DATA_WIDTH = SynchedEntityData.defineId(WardenBeamEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DATA_CORE = SynchedEntityData.defineId(WardenBeamEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(WardenBeamEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> DATA_COLUMN = SynchedEntityData.defineId(WardenBeamEntity.class, EntityDataSerializers.BOOLEAN);
   private int life;
   private int fullLife = 1;
   private float prevOffX;
   private float prevOffY;
   private float prevOffZ;
   private boolean prevReady;

   public WardenBeamEntity(EntityType<? extends WardenBeamEntity> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.setNoGravity(true);
      this.noCulling = true;
   }

   public void shape(int lifeTicks, float width, float core, int rgb, boolean column) {
      this.life = lifeTicks;
      this.fullLife = Math.max(1, lifeTicks);
      this.entityData.set(DATA_LIFE, lifeTicks);
      this.entityData.set(DATA_FULL, lifeTicks);
      this.entityData.set(DATA_WIDTH, width);
      this.entityData.set(DATA_CORE, core);
      this.entityData.set(DATA_COLOR, rgb);
      this.entityData.set(DATA_COLUMN, column);
   }

   public void aim(Vec3 from, Vec3 to) {
      this.life = this.fullLife;
      this.entityData.set(DATA_LIFE, this.life);
      this.setPos(from.x, from.y, from.z);
      this.entityData
         .set(DATA_OFFSET, new Vector3f((float)(to.x - from.x), (float)(to.y - from.y), (float)(to.z - from.z)));
   }

   public void tick() {
      if (this.level().isClientSide) {
         Vec3 now = this.offset();
         this.prevOffX = (float)now.x;
         this.prevOffY = (float)now.y;
         this.prevOffZ = (float)now.z;
         this.prevReady = true;
      }

      super.tick();
      if (!this.level().isClientSide && --this.life <= 0) {
         this.discard();
      } else {
         if (!this.level().isClientSide) {
            this.entityData.set(DATA_LIFE, Math.max(0, this.life));
         }
      }
   }

   public Vec3 offsetLerped(float partialTicks) {
      Vec3 cur = this.offset();
      return !this.prevReady
         ? cur
         : new Vec3(
            Mth.lerp(partialTicks, this.prevOffX, (float)cur.x),
            Mth.lerp(partialTicks, this.prevOffY, (float)cur.y),
            Mth.lerp(partialTicks, this.prevOffZ, (float)cur.z)
         );
   }

   public float alive() {
      int full = Math.max(1, (Integer)this.entityData.get(DATA_FULL));
      return Math.max(0.0F, Math.min(1.0F, (float)((Integer)this.entityData.get(DATA_LIFE)).intValue() / full));
   }

   public Vec3 offset() {
      Vector3f v = (Vector3f)this.entityData.get(DATA_OFFSET);
      return new Vec3(v.x(), v.y(), v.z());
   }

   public float beamWidth() {
      return (Float)this.entityData.get(DATA_WIDTH);
   }

   public float beamCore() {
      return (Float)this.entityData.get(DATA_CORE);
   }

   public int beamColor() {
      return (Integer)this.entityData.get(DATA_COLOR);
   }

   public boolean column() {
      return (Boolean)this.entityData.get(DATA_COLUMN);
   }

   protected void defineSynchedData() {
      this.entityData.define(DATA_OFFSET, new Vector3f(0.0F, 1.0F, 0.0F));
      this.entityData.define(DATA_LIFE, 1);
      this.entityData.define(DATA_FULL, 1);
      this.entityData.define(DATA_WIDTH, 2.7F);
      this.entityData.define(DATA_CORE, 0.1F);
      this.entityData.define(DATA_COLOR, 16777215);
      this.entityData.define(DATA_COLUMN, false);
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
   }

   public boolean shouldRenderAtSqrDistance(double distance) {
      double reach = 512.0 + this.offset().lengthSqr();
      return distance < reach * reach;
   }
}

