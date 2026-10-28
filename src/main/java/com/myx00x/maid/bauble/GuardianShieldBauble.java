package com.myx00x.maid.bauble;

import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.combat.GuardKeeper;
import com.myx00x.maid.combat.GuardVitals;
import com.myx00x.maid.combat.MaidRecovery;
import com.myx00x.maid.combat.ShieldWatcher;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;

public class GuardianShieldBauble implements IMaidBauble {
   public void onTick(EntityMaid maid, ItemStack baubleItem) {
      if (!maid.level().isClientSide) {
         try {
            tickServer(maid);
         } catch (Throwable t) {
            ShenlinMod.LOGGER.warn("[shenlin] shield bauble tick failed: {}", t.toString());
         }
      }
   }

   private static void tickServer(EntityMaid maid) {
      if ((Boolean)Config.GUARD_REWRITE.get()) {
         GuardKeeper.onGuardedTick(maid);
         keepBackup(maid);
      } else {
         UUID id = maid.getUUID();
         if (!ShieldWatcher.hasSnapshot(id)) {
            if (MaidRecovery.clearLingering(maid)) {
               ShenlinMod.LOGGER.info("[shenlin] maid {} lingering state cleared", id);
            }

            ShieldWatcher.record(maid);
         } else {
            ShieldWatcher.beat(maid);
            long gameTime = maid.level().getGameTime();
            if (gameTime % 20L == (id.hashCode() & 2147483647L) % 20L) {
               ShieldWatcher.record(maid);
            }
         }
      }
   }

   private static void keepBackup(EntityMaid maid) {
      UUID id = maid.getUUID();
      long interval = 20L;
      long gameTime = maid.level().getGameTime();
      if (gameTime % interval == (id.hashCode() & 2147483647L) % interval) {
         ShieldWatcher.record(maid);
      }
   }

   public void onTakeOff(EntityMaid maid, ItemStack baubleItem) {
      if (!maid.level().isClientSide) {
         if ((Boolean)Config.GUARD_REWRITE.get()) {
            GuardKeeper.forget(maid.getUUID());
            GuardVitals.forget(maid);
            ShieldWatcher.release(maid.getUUID());
         } else {
            ShieldWatcher.release(maid.getUUID());
         }
      }
   }
}

