package com.myx00x.maid.event;

import com.github.tartaricacid.touhoulittlemaid.api.event.MaidDeathEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.combat.GuardKeeper;
import com.myx00x.maid.combat.MaidRecovery;
import com.myx00x.maid.combat.ShieldTier;
import com.myx00x.maid.combat.ShieldWatcher;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class MaidDeathProtection {
   @SubscribeEvent
   public static void onMaidDeath(MaidDeathEvent event) {
      try {
         EntityMaid maid = event.getMaid();
         if (maid.level().isClientSide) {
            return;
         }

         if ((Boolean)Config.GUARD_REWRITE.get()) {
            if (GuardKeeper.guards(maid)) {
               event.setCanceled(true);
            }

            return;
         }

         if (!ShieldWatcher.isProtected(maid) || !ShieldTier.canRevive(maid)) {
            return;
         }

         MaidRecovery.sanitize(maid);
         event.setCanceled(true);
         if (ShieldWatcher.shouldLog(maid.getUUID(), maid.level().getGameTime())) {
            ShenlinMod.LOGGER.info("[shenlin] shield held maid {}", maid.getUUID());
         }
      } catch (Throwable t) {
         ShenlinMod.LOGGER.error("[shenlin] shield death hook failed", t);
      }
   }
}

