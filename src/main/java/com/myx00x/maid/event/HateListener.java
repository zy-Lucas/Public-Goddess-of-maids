package com.myx00x.maid.event;

import com.myx00x.maid.combat.HateBoard;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class HateListener {
   @SubscribeEvent
   public static void onOwnerStrike(AttackEntityEvent event) {
      if (event.getTarget() instanceof LivingEntity target) {
         HateBoard.strike(event.getEntity(), target);
      }
   }

   @SubscribeEvent
   public static void onOwnerAttacked(LivingAttackEvent event) {
      if (event.getEntity() instanceof Player owner && !owner.level().isClientSide) {
         if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            HateBoard.hurt(owner, attacker);
         }
      }
   }
}

