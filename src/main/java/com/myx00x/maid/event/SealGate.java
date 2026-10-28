package com.myx00x.maid.event;

import com.myx00x.maid.combat.SealField;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SealGate {
   @SubscribeEvent
   public static void onJoinLevel(EntityJoinLevelEvent event) {
      try {
         if (SealField.blocks(event.getEntity())) {
            event.setCanceled(true);
         }
      } catch (Throwable var2) {
      }
   }
}

