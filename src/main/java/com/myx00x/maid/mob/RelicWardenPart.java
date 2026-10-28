package com.myx00x.maid.mob;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.entity.PartEntity;

public class RelicWardenPart extends PartEntity<RelicWardenEntity> {
   private static final EntityDimensions FALLBACK = EntityDimensions.scalable(1.0F, 1.0F);
   private final EntityDimensions size;
   private final float depth;
   private final int slot;

   public RelicWardenPart(RelicWardenEntity parent, int slot, float width, float height) {
      this(parent, slot, width, height, width);
   }

   public RelicWardenPart(RelicWardenEntity parent, int slot, float width, float height, float depth) {
      super(parent);
      this.size = EntityDimensions.scalable(width, height);
      this.depth = depth;
      this.slot = slot;
      this.refreshDimensions();
   }

   public int getId() {
      RelicWardenEntity owner = (RelicWardenEntity)this.getParent();
      return owner == null ? 0 : -(owner.getId() * 4) - this.slot;
   }

   protected void defineSynchedData() {
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
   }

   protected AABB makeBoundingBox() {
      EntityDimensions dim = this.getDimensions(Pose.STANDING);
      float halfWidth = dim.width / 2.0F;
      float d = this.depth;
      float halfDepth = (d > 0.0F ? d : dim.width) / 2.0F;
      double rad = Math.toRadians(this.getYRot());
      double s = Math.abs(Mth.sin((float)rad));
      double c = Math.abs(Mth.cos((float)rad));
      double dx = s * halfDepth + c * halfWidth;
      double dz = c * halfDepth + s * halfWidth;
      return new AABB(this.getX() - dx, this.getY(), this.getZ() - dz, this.getX() + dx, this.getY() + dim.height, this.getZ() + dz);
   }

   public void syncBox() {
      this.setBoundingBox(this.makeBoundingBox());
   }

   public boolean isPickable() {
      return true;
   }

   public boolean hurt(DamageSource source, float amount) {
      return !this.isInvulnerableTo(source) && ((RelicWardenEntity)this.getParent()).hurt(source, amount);
   }

   public boolean canBeCollidedWith() {
      return false;
   }

   public EntityDimensions getDimensions(Pose pose) {
      EntityDimensions dim = this.size;
      return dim == null ? FALLBACK : dim;
   }
}

