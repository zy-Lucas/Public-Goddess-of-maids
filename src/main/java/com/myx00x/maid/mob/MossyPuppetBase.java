package com.myx00x.maid.mob;

import com.myx00x.maid.Config;
import com.myx00x.maid.registry.ModSounds;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public abstract class MossyPuppetBase extends PathfinderMob {
   private static final double SLEEP_RANGE = 20.0;
   private static final int SLEEP_DELAY = 200;
   private static final double HOME_TOLERANCE = 2.0;
   private static final int HOMING_GIVE_UP_TICKS = 400;
   private static final int HOMING_REPATH_TICKS = 20;
   private static final int ATTACK_ANIM_TICKS = 10;
   public static final int THROW_ANIM_TICKS = 38;
   protected static final int THROW_DIG_END = 18;
   protected static final int THROW_AIM_END = 25;
   protected static final int THROW_RAISE_END = 33;
   protected static final int THROW_LOOSE_TICK = 27;
   public static final double THROW_DIG_AHEAD = 0.7;
   public static final double THROW_DIG_LIFT = 0.25;
   private static final int DORMANT_FADE_TICKS = 12;
   private static final int DEATH_BLAST_DELAY = 8;
   private static final float DEATH_BLAST_DAMAGE = 10.0F;
   private static final EntityDataAccessor<Boolean> DATA_DORMANT = SynchedEntityData.defineId(MossyPuppetBase.class, EntityDataSerializers.BOOLEAN);
   private int sleepTimer;
   private int attackAnimationTick;
   private int throwAnimationTick;
   private int throwSpawnIn;
   private int throwReleaseIn;
   private boolean throwHeld;
   @Nullable
   private BlockPos homePos;
   private int homingTicks;
   private float dormantLerp;
   private float dormantLerpO;
   private boolean dormantLerpSettled;

   protected MossyPuppetBase(EntityType<? extends MossyPuppetBase> type, Level level) {
      super(type, level);
      this.setNoAi(true);
   }

   public static Builder createAttributes() {
      return PathfinderMob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, Config.puppetHealth())
         .add(Attributes.MOVEMENT_SPEED, 0.23)
         .add(Attributes.ATTACK_DAMAGE, Config.puppetAttack())
         .add(Attributes.FOLLOW_RANGE, 24.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
   }

   public boolean isDormant() {
      return (Boolean)this.entityData.get(DATA_DORMANT);
   }

   public int getAttackAnimationTick() {
      return this.attackAnimationTick;
   }

   public int getThrowAnimationTick() {
      return this.throwAnimationTick;
   }

   public float dormantLerp(float partialTicks) {
      return Mth.lerp(partialTicks, this.dormantLerpO, this.dormantLerp);
   }

   public static boolean isPrey(Player player) {
      return !player.isCreative() && !player.isSpectator();
   }

   @Nullable
   Player nearestPrey(double range) {
      return this.level().getNearestPlayer(this.getX(), this.getY(), this.getZ(), range, e -> e instanceof Player p && isPrey(p));
   }

   public void wake() {
      if (this.isDormant()) {
         this.setDormant(false);
         this.sleepTimer = 0;
         this.playSound((SoundEvent)ModSounds.MOSSY_WAKE.get(), 1.1F, 1.0F);
         if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, this.getX(), this.getY() + 1.0, this.getZ(), 22, 0.45, 0.6, 0.45, 0.02);
            server.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.4, this.getZ(), 8, 0.35, 0.3, 0.35, 0.01);
         }
      }
   }

   private void fallAsleep() {
      if (!this.isDormant()) {
         this.setDormant(true);
         this.setTarget(null);
         this.getNavigation().stop();
         this.setDeltaMovement(Vec3.ZERO);
         this.homingTicks = 0;
         this.playSound((SoundEvent)ModSounds.MOSSY_SLEEP.get(), 1.0F, 1.0F);
         if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.7, this.getZ(), 10, 0.35, 0.4, 0.35, 0.01);
         }
      }
   }

   private void setDormant(boolean dormant) {
      this.entityData.set(DATA_DORMANT, dormant);
      if (!this.level().isClientSide) {
         this.setNoAi(dormant);
      }
   }

   private boolean isAtHome() {
      if (this.homePos == null) {
         return true;
      }

      double dx = this.getX() - (this.homePos.getX() + 0.5);
      double dz = this.getZ() - (this.homePos.getZ() + 0.5);
      return dx * dx + dz * dz <= 4.0;
   }

   private void returnHome() {
      if (this.homePos != null) {
         if (++this.homingTicks >= 400) {
            this.fallAsleep();
         } else {
            if (this.homingTicks % 20 == 1) {
               this.getNavigation().moveTo(this.homePos.getX() + 0.5, this.homePos.getY(), this.homePos.getZ() + 0.5, 1.0);
            }
         }
      }
   }

   public void tick() {
      super.tick();
      if (this.attackAnimationTick > 0) {
         this.attackAnimationTick--;
      }

      if (this.throwAnimationTick > 0) {
         this.throwAnimationTick--;
      }

      if (this.level().isClientSide) {
         this.dormantLerpO = this.dormantLerp;
         float target = this.isDormant() ? 1.0F : 0.0F;
         if (!this.dormantLerpSettled) {
            this.dormantLerp = target;
            this.dormantLerpSettled = true;
         } else {
            this.dormantLerp = Mth.approach(this.dormantLerp, target, 0.083333336F);
         }
      } else {
         if (this.throwSpawnIn > 0 && --this.throwSpawnIn == 0) {
            this.throwHeld = true;
            this.onThrowSpawn();
         }

         if (this.throwReleaseIn > 0 && --this.throwReleaseIn == 0 && this.throwHeld) {
            this.throwHeld = false;
            this.onThrowRelease();
         }

         if (this.throwAnimationTick > 0) {
            int digging = 38 - this.throwAnimationTick;
            if (digging >= 5 && digging < 18 && digging % 2 == 1) {
               this.spitDigParticles();
            }
         }

         if (this.homePos == null) {
            this.homePos = this.blockPosition();
         }

         if (this.isDormant()) {
            if (this.nearestPrey(Config.puppetWakeRange()) != null) {
               this.wake();
            }
         } else if (this.getTarget() != null) {
            this.sleepTimer = 0;
            this.homingTicks = 0;
         } else if (!this.isAtHome()) {
            this.sleepTimer = 0;
            this.returnHome();
         } else {
            this.homingTicks = 0;
            if (this.nearestPrey(20.0) == null) {
               if (++this.sleepTimer >= 200) {
                  this.fallAsleep();
               }
            } else {
               this.sleepTimer = 0;
            }
         }
      }
   }

   public void die(DamageSource source) {
      super.die(source);
      if (!this.level().isClientSide) {
         this.abortThrow();
         this.setInvisible(true);
         this.burstDeathParticles();
      }
   }

   protected void tickDeath() {
      if (!this.level().isClientSide) {
         if (++this.deathTime >= 8 && !this.isRemoved()) {
            this.detonateDeathBlast();
            this.remove(RemovalReason.KILLED);
         }
      }
   }

   private void burstDeathParticles() {
      if (this.level() instanceof ServerLevel server) {
         double var15 = this.getX();
         double cy = this.getY() + 0.5;
         double cz = this.getZ();

         for (int i = 0; i < 26; i++) {
            double angle = this.getRandom().nextDouble() * Math.PI * 2.0;
            double speed = 0.32 + this.getRandom().nextDouble() * 0.26;
            double lift = (this.getRandom().nextDouble() - 0.5) * 0.2;
            server.sendParticles(
               i % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.ASH,
               var15,
               cy + this.getRandom().nextDouble() * 0.5,
               cz,
               0,
               Math.cos(angle) * speed,
               lift,
               Math.sin(angle) * speed,
               1.0
            );
         }
      }
   }

   private void detonateDeathBlast() {
      if (this.level() instanceof ServerLevel server) {
         Vec3 var10 = this.position();
         server.playSound(null, var10.x, var10.y, var10.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 2.0F, 1.0F);

         for (int radius = 0; radius < 16; radius++) {
            double angle = this.getRandom().nextDouble() * Math.PI * 2.0;
            double speed = 0.38 + this.getRandom().nextDouble() * 0.24;
            double lift = (this.getRandom().nextDouble() - 0.5) * 0.2;
            server.sendParticles(
               radius % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.ASH,
               var10.x,
               var10.y + 0.4,
               var10.z,
               0,
               Math.cos(angle) * speed,
               lift,
               Math.sin(angle) * speed,
               1.0
            );
         }

         double radius = Config.puppetDeathRadius();
         if (!(radius <= 0.0)) {
            AABB reach = new AABB(var10, var10).inflate(radius);

            for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, reach)) {
               if (victim != this && victim.isAlive() && victim.distanceToSqr(var10) <= radius * radius) {
                  victim.hurt(server.damageSources().explosion(this, this), 10.0F);
               }
            }
         }
      }
   }

   public void onRemovedFromWorld() {
      this.abortThrow();
      super.onRemovedFromWorld();
   }

   public void remove(RemovalReason reason) {
      this.abortThrow();
      super.remove(reason);
   }

   private void abortThrow() {
      if (this.throwHeld) {
         this.throwHeld = false;
         this.onThrowAborted();
      }
   }

   public boolean hurt(DamageSource source, float amount) {
      boolean hit = super.hurt(source, amount);
      if (hit) {
         this.wake();
      }

      return hit;
   }

   public boolean isPushable() {
      return !this.isDormant() && super.isPushable();
   }

   public boolean doHurtTarget(Entity target) {
      this.attackAnimationTick = 10;
      this.level().broadcastEntityEvent(this, (byte)4);
      return super.doHurtTarget(target);
   }

   private static float easeOut(float x) {
      return x * (2.0F - x);
   }

   private static float easeOutPow(float x, double power) {
      return 1.0F - (float)Math.pow(1.0 - x, power);
   }

   private static float easeBoth(float x) {
      return x * x * (3.0F - 2.0F * x);
   }

   public static float throwArmAngleAt(float elapsed) {
      if (elapsed < 3.0F) {
         return -0.88F * easeOut(elapsed / 3.0F);
      } else if (elapsed < 18.0F) {
         return -0.88F;
      } else if (elapsed < 25.0F) {
         return Mth.lerp(easeOut((elapsed - 18.0F) / 7.0F), -0.88F, -0.78F);
      } else {
         return elapsed < 33.0F
            ? Mth.lerp(easeOutPow((elapsed - 25.0F) / 8.0F, 2.2), -0.78F, -3.05F)
            : Mth.lerp(easeBoth(Mth.clamp((elapsed - 33.0F) / 5.0F, 0.0F, 1.0F)), -3.05F, 0.0F);
      }
   }

   public static float throwBodyAngleAt(float elapsed) {
      if (elapsed < 3.0F) {
         return 0.42F * easeOut(elapsed / 3.0F);
      } else if (elapsed < 18.0F) {
         return 0.42F;
      } else if (elapsed < 25.0F) {
         return Mth.lerp(easeOut((elapsed - 18.0F) / 7.0F), 0.42F, 0.28F);
      } else {
         return elapsed < 33.0F
            ? Mth.lerp(easeOutPow((elapsed - 25.0F) / 8.0F, 2.0), 0.28F, -0.18F)
            : Mth.lerp(easeBoth(Mth.clamp((elapsed - 33.0F) / 5.0F, 0.0F, 1.0F)), -0.18F, 0.0F);
      }
   }

   public static float throwHeadOffsetAt(float elapsed) {
      if (elapsed < 3.0F) {
         return 0.52F * easeOut(elapsed / 3.0F);
      } else if (elapsed < 18.0F) {
         return 0.52F;
      } else {
         return elapsed < 25.0F ? Mth.lerp(easeOut((elapsed - 18.0F) / 7.0F), 0.52F, 0.0F) : 0.0F;
      }
   }

   public static float throwStirAt(float elapsed) {
      if (!(elapsed < 3.0F) && !(elapsed > 18.0F)) {
         float fade = Math.min(Mth.clamp((elapsed - 3.0F) / 2.0F, 0.0F, 1.0F), Mth.clamp((18.0F - elapsed) / 2.0F, 0.0F, 1.0F));
         return Mth.sin((elapsed - 3.0F) * 0.8976F) * 0.14F * fade;
      } else {
         return 0.0F;
      }
   }

   public static float throwSweepAt(float elapsed) {
      if (!(elapsed < 3.0F) && !(elapsed > 18.0F)) {
         float fade = Math.min(Mth.clamp((elapsed - 3.0F) / 2.0F, 0.0F, 1.0F), Mth.clamp((18.0F - elapsed) / 2.0F, 0.0F, 1.0F));
         return Mth.cos((elapsed - 3.0F) * 0.8976F) * 0.1F * fade;
      } else {
         return 0.0F;
      }
   }

   public void startThrowAnimation() {
      this.throwAnimationTick = 38;
      this.throwSpawnIn = 27;
      this.throwReleaseIn = 27;
      this.level().broadcastEntityEvent(this, (byte)5);
   }

   protected void onThrowSpawn() {
   }

   protected void onThrowRelease() {
   }

   protected Vec3 digSpot() {
      Vec3 forward = Vec3.directionFromRotation(0.0F, this.getYRot());
      return new Vec3(this.getX() + forward.x * 0.7, this.getY() + 0.25, this.getZ() + forward.z * 0.7);
   }

   private void spitDigParticles() {
      if (this.level() instanceof ServerLevel server) {
         Vec3 var3 = this.digSpot();
         server.sendParticles(
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MOSSY_COBBLESTONE.defaultBlockState()),
            var3.x,
            var3.y,
            var3.z,
            3,
            0.28,
            0.16,
            0.28,
            0.05
         );
      }
   }

   protected void onThrowAborted() {
   }

   public void handleEntityEvent(byte id) {
      if (id == 4) {
         this.attackAnimationTick = 10;
      } else if (id == 5) {
         this.throwAnimationTick = 38;
      } else {
         super.handleEntityEvent(id);
      }
   }

   protected SoundEvent getHurtSound(DamageSource source) {
      return (SoundEvent)ModSounds.MOSSY_HURT.get();
   }

   protected SoundEvent getDeathSound() {
      return (SoundEvent)ModSounds.MOSSY_DEATH.get();
   }

   protected void playStepSound(BlockPos pos, BlockState state) {
      this.playSound(SoundEvents.STONE_STEP, 0.16F, 1.0F + this.getRandom().nextFloat() * 0.15F);
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(DATA_DORMANT, Boolean.TRUE);
   }

   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putBoolean("dormant", this.isDormant());
      if (this.homePos != null) {
         tag.putInt("homeX", this.homePos.getX());
         tag.putInt("homeY", this.homePos.getY());
         tag.putInt("homeZ", this.homePos.getZ());
      }
   }

   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.setDormant(!tag.contains("dormant") || tag.getBoolean("dormant"));
      if (tag.contains("homeX")) {
         this.homePos = new BlockPos(tag.getInt("homeX"), tag.getInt("homeY"), tag.getInt("homeZ"));
      }
   }
}

