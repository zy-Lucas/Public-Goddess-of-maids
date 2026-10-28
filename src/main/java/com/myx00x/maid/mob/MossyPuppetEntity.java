package com.myx00x.maid.mob;

import com.myx00x.maid.Config;
import com.myx00x.maid.registry.ModSounds;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class MossyPuppetEntity extends MossyPuppetBase {
   private static final double MELEE_RANGE = 3.0;
   private static final double MELEE_REACH = 2.2;
   private static final double APPROACH_SPEED = 1.0;
   private static final int REPATH_TICKS = 6;
   private static final double THROW_RANGE = 14.0;
   private static final int THROW_COOLDOWN = 40;

   public MossyPuppetEntity(EntityType<? extends MossyPuppetEntity> type, Level level) {
      super(type, level);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(2, new MossyPuppetEntity.AttackGoal());
      this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12.0F));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[0]));
      this.targetSelector.addGoal(2, new MossyPuppetEntity.PreyTargetGoal());
   }

   public boolean canAttack(LivingEntity target) {
      return super.canAttack(target) && !(target instanceof Player p && !isPrey(p));
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      boolean hit = super.doHurtTarget(target);
      this.playSound((SoundEvent)ModSounds.MOSSY_SWING.get(), 1.0F, 0.9F + this.getRandom().nextFloat() * 0.2F);
      return hit;
   }

   @Override
   protected void onThrowSpawn() {
      MossyPebbleEntity pebble = new MossyPebbleEntity(this.level(), this);
      Vec3 spot = this.digSpot();
      pebble.setPos(spot.x, spot.y, spot.z);
      LivingEntity target = this.getTarget();
      if (target != null) {
         double dx = target.getX() - spot.x;
         double dy = target.getY(0.4) - spot.y;
         double dz = target.getZ() - spot.z;
         double arc = Math.sqrt(dx * dx + dz * dz) * 0.18;
         pebble.shoot(dx, dy + arc, dz, 1.25F, 7.0F);
      }

      this.level().addFreshEntity(pebble);
      if (this.level() instanceof ServerLevel server) {
         server.sendParticles(
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MOSSY_COBBLESTONE.defaultBlockState()),
            spot.x,
            spot.y + 0.04,
            spot.z,
            5,
            0.12,
            0.06,
            0.12,
            0.03
         );
      }

      this.playSound((SoundEvent)ModSounds.MOSSY_THROW.get(), 1.0F, 0.95F + this.getRandom().nextFloat() * 0.1F);
   }

   @Override
   protected void onThrowRelease() {
   }

   private class AttackGoal extends Goal {
      private int repathAt;
      private int meleeCooldown;
      private int throwCooldown;

      AttackGoal() {
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean canUse() {
         LivingEntity target = MossyPuppetEntity.this.getTarget();
         return target != null && target.isAlive();
      }

      public boolean canContinueToUse() {
         return this.canUse();
      }

      public void stop() {
         MossyPuppetEntity.this.getNavigation().stop();
      }

      private void approach(LivingEntity target) {
         MossyPuppetEntity mob = MossyPuppetEntity.this;
         if (mob.tickCount >= this.repathAt) {
            this.repathAt = mob.tickCount + 6;
            mob.getNavigation().moveTo(target, 1.0);
         }
      }

      public void tick() {
         if (this.meleeCooldown > 0) {
            this.meleeCooldown--;
         }

         if (this.throwCooldown > 0) {
            this.throwCooldown--;
         }

         LivingEntity target = MossyPuppetEntity.this.getTarget();
         if (target != null) {
            MossyPuppetEntity mob = MossyPuppetEntity.this;
            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double dist = mob.distanceTo(target);
            if (mob.getThrowAnimationTick() > 0) {
               if (Config.puppetThrowPause()) {
                  mob.getNavigation().stop();
               } else {
                  this.approach(target);
               }
            } else if (dist <= 3.0) {
               if (dist <= 2.2) {
                  mob.getNavigation().stop();
                  if (this.meleeCooldown <= 0) {
                     mob.doHurtTarget(target);
                     this.meleeCooldown = Config.puppetAttackInterval();
                  }
               } else {
                  this.approach(target);
               }
            } else {
               this.approach(target);
               if (dist <= 14.0 && mob.getSensing().hasLineOfSight(target) && this.throwCooldown <= 0) {
                  mob.startThrowAnimation();
                  this.throwCooldown = 40;
               }
            }
         }
      }
   }

   private class PreyTargetGoal extends TargetGoal {
      private static final double GIVE_UP_RANGE = 32.0;
      @Nullable
      private LivingEntity locked;

      PreyTargetGoal() {
         super(MossyPuppetEntity.this, false);
      }

      public boolean requiresUpdateEveryTick() {
         return true;
      }

      public boolean canUse() {
         Player prey = MossyPuppetEntity.this.nearestPrey(this.getFollowDistance());
         if (prey == null) {
            return false;
         }

         this.locked = prey;
         return true;
      }

      public boolean canContinueToUse() {
         LivingEntity target = this.mob.getTarget();
         return target != null && target.isAlive() ? this.mob.distanceToSqr(target) <= 1024.0 : false;
      }

      public void start() {
         this.mob.setTarget(this.locked);
      }

      public void stop() {
         this.mob.setTarget(null);
         this.locked = null;
      }
   }
}

