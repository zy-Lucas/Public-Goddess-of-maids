package com.myx00x.maid.event;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.Config;
import com.myx00x.maid.combat.HeartGuard;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class HeartGuardListener {
   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void onChangeTarget(LivingChangeTargetEvent event) {
      if ((Boolean)Config.DEVOTED_HEART_ENABLED.get() && !event.getEntity().level().isClientSide) {
         if (!(event.getEntity() instanceof EntityMaid)) {
            HeartGuard.noteEvent();
            if (event.getNewTarget() instanceof Player owner) {
               EntityMaid guard = HeartGuard.findGuard(owner);
               if (guard == null) {
                  HeartGuard.noteMiss();
               } else {
                  if (guard != event.getEntity()) {
                     event.setNewTarget(guard);
                     HeartGuard.noteSwap();
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onOwnerStrike(AttackEntityEvent event) {
      if (event.getTarget() instanceof LivingEntity target) {
         HeartGuard.ownerStruck(event.getEntity(), target);
      }
   }

   @SubscribeEvent
   public static void onOwnerHurt(LivingHurtEvent event) {
      if (event.getEntity() instanceof Player owner) {
         if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            HeartGuard.ownerHurt(owner, attacker);
         }
      }
   }

   @SubscribeEvent
   public static void onOwnerDamaged(LivingDamageEvent event) {
      if (event.getEntity() instanceof Player owner && !owner.level().isClientSide) {
         if (!event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)) {
            double cut = HeartGuard.damageCut(owner);
            if (cut > 0.0 && cut < 1.0) {
               event.setAmount((float)(event.getAmount() * (1.0 - cut)));
            }
         }
      }
   }

   @SubscribeEvent
   public static void onOwnerAttacked(LivingAttackEvent event) {
      if (event.getEntity() instanceof Player owner) {
         if (!owner.level().isClientSide) {
            if (!event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)) {
               if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                  HeartGuard.ownerHurt(owner, attacker);
               }

               if (HeartGuard.damageCut(owner) >= 1.0) {
                  event.setCanceled(true);
               }
            }
         }
      }
   }
}

