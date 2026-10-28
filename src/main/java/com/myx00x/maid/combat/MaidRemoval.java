package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.Entity.RemovalReason;

public final class MaidRemoval {
   private static final int REMOVAL_HOPS = 2;
   private static final long PERMIT_TTL_TICKS = 40L;
   private static final Map<UUID, MaidRemoval.Permit> PERMITS = new ConcurrentHashMap<>();

   private MaidRemoval() {
   }

   public static void permit(EntityMaid maid) {
      if (!maid.level().isClientSide) {
         PERMITS.put(maid.getUUID(), new MaidRemoval.Permit(2, maid.level().getGameTime() + 40L));
      }
   }

   public static void purge(EntityMaid maid) {
      if (!maid.level().isClientSide) {
         UUID maidId = maid.getUUID();
         if ((Boolean)Config.SHIELD_KEEP_ON_COLLECT.get()) {
            ShieldWatcher.stash(maid);
         } else {
            ShieldWatcher.release(maidId);
         }

         permit(maid);
         ShenlinMod.LOGGER.info("[shenlin] maid {} purged by request", maidId);
      }
   }

   public static void forceRemove(EntityMaid maid) {
      if (!maid.isRemoved()) {
         permit(maid);
         UUID maidId = maid.getUUID();

         try {
            maid.setRemoved(RemovalReason.DISCARDED);
         } catch (Throwable t) {
            ShenlinMod.LOGGER.warn("[shenlin] maid {} removal threw: {}", maidId, t.getMessage());
         }

         if (!maid.isRemoved() && !maid.isDeadOrDying()) {
            ShieldWatcher.unstash(maidId);
            ShenlinMod.LOGGER.info("[shenlin] maid {} still here, kept outside", maidId);
         }
      }
   }

   public static boolean consumePermit(EntityMaid maid) {
      UUID maidId = maid.getUUID();
      MaidRemoval.Permit permit = PERMITS.get(maidId);
      if (permit == null) {
         return false;
      }

      long now = maid.level().getGameTime();
      if (now > permit.expireAt) {
         PERMITS.remove(maidId);
         return false;
      }

      if (--permit.hops <= 0) {
         PERMITS.remove(maidId);
      }

      return true;
   }

   public static void cleanup(long now) {
      PERMITS.entrySet().removeIf(e -> now > e.getValue().expireAt);
   }

   private static final class Permit {
      private int hops;
      private final long expireAt;

      private Permit(int hops, long expireAt) {
         this.hops = hops;
         this.expireAt = expireAt;
      }
   }
}

