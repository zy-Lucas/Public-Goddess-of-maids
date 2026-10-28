package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.ShenlinMod;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.Entity.RemovalReason;

public final class GuardRemoval {
   private static final int HOPS = 4;
   private static final long TTL_TICKS = 40L;
   private static final Map<UUID, GuardRemoval.Permit> PERMITS = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> CHUNK_UNLOAD = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> DIMENSION_MOVE = new ConcurrentHashMap<>();
   private static final long MOVE_TTL = 20L;
   private static final Map<UUID, Long> HANDOVER = new ConcurrentHashMap<>();
   private static final long HANDOVER_TTL = 300L;

   private GuardRemoval() {
   }

   public static void permit(EntityMaid maid) {
      if (!maid.level().isClientSide) {
         PERMITS.put(maid.getUUID(), new GuardRemoval.Permit(4, maid.level().getGameTime() + 40L, maid));
      }
   }

   public static void markChunkUnload(EntityMaid maid) {
      stamp(CHUNK_UNLOAD, maid);
   }

   public static boolean chunkUnloading(EntityMaid maid) {
      return fresh(CHUNK_UNLOAD, maid);
   }

   public static void markDimensionMove(EntityMaid maid) {
      stamp(DIMENSION_MOVE, maid);
   }

   public static boolean dimensionMoving(EntityMaid maid) {
      return fresh(DIMENSION_MOVE, maid);
   }

   public static void markHandover(EntityMaid maid) {
      if (maid != null && maid.level() != null && !maid.level().isClientSide) {
         HANDOVER.put(maid.getUUID(), maid.level().getGameTime() + 300L);
      }
   }

   public static boolean handover(UUID id, long now) {
      if (id == null) {
         return false;
      } else {
         Long until = HANDOVER.get(id);
         if (until == null) {
            return false;
         } else if (now > until) {
            HANDOVER.remove(id);
            return false;
         } else {
            return true;
         }
      }
   }

   public static void endHandover(UUID id) {
      if (id != null) {
         HANDOVER.remove(id);
      }
   }

   private static void stamp(Map<UUID, Long> book, EntityMaid maid) {
      if (maid != null && maid.level() != null && !maid.level().isClientSide) {
         book.put(maid.getUUID(), maid.level().getGameTime() + 20L);
      }
   }

   private static boolean fresh(Map<UUID, Long> book, EntityMaid maid) {
      if (maid != null && maid.level() != null) {
         UUID id = maid.getUUID();
         Long until = book.get(id);
         if (until == null) {
            return false;
         } else if (maid.level().getGameTime() > until) {
            book.remove(id);
            return false;
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   public static boolean consumePermit(EntityMaid maid) {
      UUID id = maid.getUUID();
      GuardRemoval.Permit permit = PERMITS.get(id);
      if (permit == null) {
         return false;
      }

      if (permit.owner != maid) {
         PERMITS.remove(id);
         ShenlinMod.LOGGER.info("[shenlin] stale permit dropped, uuid {} reused", id);
         return false;
      }

      long now = maid.level().getGameTime();
      if (now > permit.expireAt) {
         PERMITS.remove(id);
         return false;
      }

      permit.hops--;
      if (permit.hops <= 0) {
         PERMITS.remove(id);
      }

      return true;
   }

   public static boolean hasPermit(EntityMaid maid) {
      GuardRemoval.Permit permit = PERMITS.get(maid.getUUID());
      return permit != null && permit.owner == maid && maid.level().getGameTime() <= permit.expireAt;
   }

   public static void clear(UUID id) {
      if (id != null) {
         PERMITS.remove(id);
      }
   }

   public static void forceRemove(EntityMaid maid) {
      if (maid != null && maid.level() != null) {
         UUID id = maid.getUUID();
         permit(maid);

         try {
            maid.setRemoved(RemovalReason.DISCARDED);
         } catch (Throwable t) {
            ShenlinMod.LOGGER.warn("[shenlin] maid {} removal threw: {}", id, t.getMessage());
         }

         if (!maid.isRemoved()) {
            GuardKeeper.forget(id);
         }

         clear(id);
      }
   }

   public static void cleanup(long now) {
      PERMITS.entrySet().removeIf(e -> now > e.getValue().expireAt);
      CHUNK_UNLOAD.entrySet().removeIf(e -> now > e.getValue());
      DIMENSION_MOVE.entrySet().removeIf(e -> now > e.getValue());
      HANDOVER.entrySet().removeIf(e -> now > e.getValue());
   }

   private static final class Permit {
      private int hops;
      private final long expireAt;
      private final EntityMaid owner;

      private Permit(int hops, long expireAt, EntityMaid owner) {
         this.hops = hops;
         this.expireAt = expireAt;
         this.owner = owner;
      }
   }
}

