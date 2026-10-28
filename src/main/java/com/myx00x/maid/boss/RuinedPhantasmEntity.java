package com.myx00x.maid.boss;

import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.combat.EntityRelist;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.Level;

public class RuinedPhantasmEntity extends Entity {
   public RuinedPhantasmBoss boss;
   private boolean releasing;

   public RuinedPhantasmEntity(EntityType<? extends Entity> type, Level level) {
      super(type, level);
      this.noPhysics = true;
   }

   public boolean isInvulnerable() {
      return true;
   }

   public boolean isPickable() {
      return true;
   }

   public boolean isPushable() {
      return false;
   }

   public void setSecondsOnFire(int seconds) {
   }

   public void setRemainingFireTicks(int ticks) {
   }

   public void remove(RemovalReason reason) {
      if (this.level().isClientSide) {
         super.remove(reason);
      } else if (this.releasing || reason != RemovalReason.KILLED && reason != RemovalReason.DISCARDED) {
         super.remove(reason);
      }
   }

   protected void defineSynchedData() {
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
   }

   void release() {
      this.releasing = true;
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide) {
         try {
            EntityRelist.clearRemovalReason(this);
            if (!EntityRelist.onLists(this, (ServerLevel)this.level())) {
               boolean back = EntityRelist.addNoEvent(this, (ServerLevel)this.level());
               if (!back) {
                  EntityRelist.addByEvent(this, (ServerLevel)this.level());
               }

               ShenlinMod.LOGGER.info("[shenlin] ruined phantasm shell relisted id={} ok={}", this.getId(), back);
            }
         } catch (Throwable var2) {
         }

         if (this.boss != null && this.boss.alive()) {
            this.setPos(this.boss.x(), this.boss.y(), this.boss.z());
            this.setYRot(this.boss.yRot());
            this.setXRot(this.boss.xRot());
         }
      }
   }

   public void faceLikeBoss() {
      if (this.boss != null) {
         this.setPos(this.boss.x(), this.boss.y(), this.boss.z());
         this.setYRot(this.boss.yRot());
         this.setXRot(this.boss.xRot());
         this.yRotO = this.boss.yRot();
         this.xRotO = this.boss.xRot();
      }
   }
}

