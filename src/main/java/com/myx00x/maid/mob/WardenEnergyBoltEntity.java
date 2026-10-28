package com.myx00x.maid.mob;

import com.myx00x.maid.Config;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Vector3f;

public class WardenEnergyBoltEntity extends Entity {
   public static final float LENGTH = 0.3F;
   public static final float WIDTH = 0.1F;
   private static final int MAX_LIFE = 80;
   private static final EntityDataAccessor<Vector3f> DATA_DIR = SynchedEntityData.defineId(WardenEnergyBoltEntity.class, EntityDataSerializers.VECTOR3);
   private UUID ownerId;
   private int age;

   public WardenEnergyBoltEntity(EntityType<? extends WardenEnergyBoltEntity> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.setNoGravity(true);
      this.noCulling = true;
   }

   public void launch(RelicWardenEntity owner, Vec3 from, Vec3 dir) {
      this.ownerId = owner.getUUID();
      this.setPos(from.x, from.y, from.z);
      this.xo = from.x;
      this.yo = from.y;
      this.zo = from.z;
      this.entityData.set(DATA_DIR, new Vector3f((float)dir.x, (float)dir.y, (float)dir.z));
      this.age = 0;
   }

   private double step() {
      return Config.relicWardenBoltSpeed() / 20.0;
   }

   public Vec3 dir() {
      Vector3f v = (Vector3f)this.entityData.get(DATA_DIR);
      return new Vec3(v.x(), v.y(), v.z());
   }

   public void tick() {
      super.tick();
      this.xo = this.getX();
      this.yo = this.getY();
      this.zo = this.getZ();
      if (++this.age > 80) {
         this.discard();
      } else if (!this.level().isClientSide) {
         Vec3 from = this.position();
         Vec3 to = from.add(this.dir().scale(this.step()));
         BlockHitResult block = this.level().clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, this));
         Vec3 end = block.getType() == Type.BLOCK ? block.getLocation() : to;
         LivingEntity victim = null;
         double nearest = Double.MAX_VALUE;
         AABB sweep = new AABB(from, end).inflate(0.25);

         for (LivingEntity le : this.level().getEntitiesOfClass(LivingEntity.class, sweep)) {
            if (le.isAlive() && !le.isSpectator() && !(le instanceof RelicWardenEntity) && (this.ownerId == null || !this.ownerId.equals(le.getUUID()))) {
               Optional<Vec3> touch = le.getBoundingBox().inflate(0.15).clip(from, end);
               if (!touch.isEmpty() || le.getBoundingBox().contains(from)) {
                  double d = touch.<Double>map(v -> v.distanceToSqr(from)).orElse(0.0);
                  if (d < nearest) {
                     nearest = d;
                     victim = le;
                  }
               }
            }
         }

         if (victim != null) {
            if (this.level() instanceof ServerLevel server && this.ownerId != null && server.getEntity(this.ownerId) instanceof RelicWardenEntity warden) {
               warden.boltHit(victim, end, this.dir());
            }

            this.discard();
         } else if (block.getType() == Type.BLOCK) {
            this.discard();
         } else {
            this.setPos(end.x, end.y, end.z);
         }
      }
   }

   protected void defineSynchedData() {
      this.entityData.define(DATA_DIR, new Vector3f(0.0F, 0.0F, 1.0F));
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
   }

   public boolean shouldRenderAtSqrDistance(double distance) {
      return distance < 25600.0;
   }
}

