package com.myx00x.maid.mob;

import com.myx00x.maid.registry.ModEntities;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class MossyPebbleEntity extends ThrowableItemProjectile {
   public MossyPebbleEntity(EntityType<? extends MossyPebbleEntity> type, Level level) {
      super(type, level);
   }

   public MossyPebbleEntity(Level level, LivingEntity shooter) {
      super((EntityType)ModEntities.MOSSY_PEBBLE.get(), shooter, level);
   }

   protected Item getDefaultItem() {
      return Items.MOSSY_COBBLESTONE;
   }

   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      if (!this.level().isClientSide) {
         Entity target = result.getEntity();
         float damage = 4.0F;
         if (this.getOwner() instanceof LivingEntity shooter) {
            AttributeInstance attack = shooter.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attack != null) {
               damage = (float)attack.getValue();
            }
         }

         if (target.hurt(this.damageSources().thrown(this, this.getOwner()), damage)) {
            Vec3 velocity = this.getDeltaMovement();
            target.push(velocity.x * 0.6, 0.12, velocity.z * 0.6);
         }
      }
   }

   protected void onHit(HitResult result) {
      super.onHit(result);
      if (!this.level().isClientSide) {
         if (this.level() instanceof ServerLevel server) {
            server.sendParticles(
               new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MOSSY_COBBLESTONE.defaultBlockState()),
               this.getX(),
               this.getY(),
               this.getZ(),
               10,
               0.15,
               0.15,
               0.15,
               0.08
            );
         }

         this.playSound(SoundEvents.STONE_BREAK, 0.7F, 1.05F);
         this.discard();
      }
   }
}

