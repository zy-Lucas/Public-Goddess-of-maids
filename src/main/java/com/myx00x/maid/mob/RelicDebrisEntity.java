package com.myx00x.maid.mob;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class RelicDebrisEntity extends Entity {
   private static final EntityDataAccessor<BlockState> DATA_BLOCK = SynchedEntityData.defineId(RelicDebrisEntity.class, EntityDataSerializers.BLOCK_STATE);
   private static final EntityDataAccessor<Float> DATA_SCALE = SynchedEntityData.defineId(RelicDebrisEntity.class, EntityDataSerializers.FLOAT);
   private static final int LIFE_TICKS = 120;
   private int life = 120;

   public static int lifeTicks() {
      return 120;
   }

   public RelicDebrisEntity(EntityType<? extends RelicDebrisEntity> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.setNoGravity(true);
   }

   public void shape(BlockState state, float scale) {
      this.entityData.set(DATA_BLOCK, state);
      this.entityData.set(DATA_SCALE, scale);
   }

   public BlockState blockState() {
      return (BlockState)this.entityData.get(DATA_BLOCK);
   }

   public float debrisScale() {
      return (Float)this.entityData.get(DATA_SCALE);
   }

   protected void defineSynchedData() {
      this.entityData.define(DATA_BLOCK, Blocks.COBBLESTONE.defaultBlockState());
      this.entityData.define(DATA_SCALE, 1.0F);
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      tag.putString("Block", BuiltInRegistries.BLOCK.getKey(this.blockState().getBlock()).toString());
      tag.putFloat("Scale", this.debrisScale());
      tag.putInt("Life", this.life);
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      ResourceLocation id = ResourceLocation.tryParse(tag.getString("Block"));
      Block block = id == null ? Blocks.COBBLESTONE : (Block)BuiltInRegistries.BLOCK.get(id);
      this.entityData.set(DATA_BLOCK, block.defaultBlockState());
      this.entityData.set(DATA_SCALE, tag.getFloat("Scale"));
      this.life = tag.getInt("Life");
   }

   public void tick() {
      super.tick();
      this.setDeltaMovement(Vec3.ZERO);
      if (!this.level().isClientSide && --this.life <= 0) {
         this.discard();
      }
   }

   public boolean isPushable() {
      return false;
   }

   public boolean canBeCollidedWith() {
      return false;
   }
}

