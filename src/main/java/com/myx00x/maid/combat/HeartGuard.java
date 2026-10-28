package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.registry.ModItems;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;

public final class HeartGuard {
   private static final long DUTY_GRACE = 4L;
   private static final long PROBE_INTERVAL = 10L;
   private static final Map<UUID, HeartGuard.Stamp> ON_DUTY = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> PROBED_AT = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> SWEPT_AT = new ConcurrentHashMap<>();
   private static final AtomicLong SEEN = new AtomicLong();
   private static final AtomicLong SWAPPED = new AtomicLong();
   private static final AtomicLong MISSED = new AtomicLong();
   private static final AtomicLong PROBED = new AtomicLong();
   private static final AtomicLong TURNED = new AtomicLong();
   private static final AtomicLong BLOCKED = new AtomicLong();
   private static long lastPullLog;
   private static long lastDiag;
   private static String lastPulled = "?";

   private HeartGuard() {
   }

   public static void noteEvent() {
      SEEN.incrementAndGet();
   }

   public static void noteSwap() {
      SWAPPED.incrementAndGet();
   }

   public static void noteMiss() {
      MISSED.incrementAndGet();
   }

   public static void ownerStruck(Player owner, LivingEntity target) {
      if (onDuty(owner)) {
         EntityMaid guard = findGuard(owner);
         if (guard != null && wearsHeart(guard)) {
            turn(target, guard, owner);
         } else {
            noteMiss();
         }
      }
   }

   public static void ownerHurt(Player owner, LivingEntity attacker) {
      if (onDuty(owner)) {
         EntityMaid guard = findGuard(owner);
         if (guard != null && wearsHeart(guard)) {
            turn(attacker, guard, owner);
         } else {
            noteMiss();
         }
      }
   }

   public static double damageCut(Player owner) {
      if ((Boolean)Config.DEVOTED_HEART_ENABLED.get() && (Boolean)Config.HEART_OWNER_SHIELDED.get() && !owner.level().isClientSide) {
         EntityMaid guard = findGuard(owner);
         if (guard != null && wearsShield(guard)) {
            double dist = Math.sqrt(owner.distanceToSqr(guard));
            if (dist <= (Double)Config.HEART_FULL_RANGE.get()) {
               return 1.0;
            } else if (dist <= (Double)Config.HEART_NEAR_RANGE.get()) {
               return (Double)Config.HEART_NEAR_CUT.get();
            } else {
               return dist <= Config.HEART_MID_RANGE.get() ? (Double)Config.HEART_MID_CUT.get() : (Double)Config.HEART_FAR_CUT.get();
            }
         } else {
            return 0.0;
         }
      } else {
         return 0.0;
      }
   }

   public static void tick(EntityMaid maid) {
      try {
         tickOnce(maid);
      } catch (Throwable t) {
         ShenlinMod.LOGGER.warn("[shenlin] devoted heart tick failed: {}", t.toString());
      }
   }

   private static void tickOnce(EntityMaid maid) {
      if ((Boolean)Config.DEVOTED_HEART_ENABLED.get() && !maid.level().isClientSide && !maid.isRemoved() && maid.isAlive()) {
         diag(maid.level().getGameTime());
         if (maid.getOwner() instanceof Player owner && !owner.isRemoved() && owner.isAlive() && owner.level() == maid.level()) {
            long now = maid.level().getGameTime();
            report(maid, owner, now);
            int interval = (Integer)Config.HEART_HATE_SWEEP_TICKS.get();
            if (interval > 0 && Math.floorMod(now, interval) == Math.floorMod(maid.getUUID().hashCode(), interval) && claimSweep(owner, now)) {
               pull(maid, owner, now);
            }
         }
      }
   }

   private static boolean claimSweep(Player owner, long now) {
      UUID ownerId = owner.getUUID();
      Long at = SWEPT_AT.get(ownerId);
      if (at != null && now - at < 10L) {
         return false;
      }

      SWEPT_AT.put(ownerId, now);
      return true;
   }

   private static void report(EntityMaid maid, Player owner, long now) {
      double dist = owner.distanceToSqr(maid);
      if (!(dist > sqr((Double)Config.HEART_BIND_RANGE.get()))) {
         HeartGuard.Stamp old = ON_DUTY.get(owner.getUUID());
         if (old == null || old.maid == maid || now - old.seen > 4L || dist <= old.dist) {
            ON_DUTY.put(owner.getUUID(), new HeartGuard.Stamp(maid, dist, now));
         }
      }
   }

   public static EntityMaid findGuard(Player owner) {
      if (owner.level().isClientSide) {
         return null;
      }

      long now = owner.level().getGameTime();
      HeartGuard.Stamp stamp = ON_DUTY.get(owner.getUUID());
      if (stamp != null && now - stamp.seen <= 4L) {
         EntityMaid maid = stamp.maid;
         if (usable(maid, owner) && maid.level() == owner.level()) {
            return maid;
         }
      }

      return probe(owner, now);
   }

   private static EntityMaid probe(Player owner, long now) {
      UUID ownerId = owner.getUUID();
      Long at = PROBED_AT.get(ownerId);
      if (at != null && now - at < 10L) {
         return null;
      }

      PROBED_AT.put(ownerId, now);
      AABB box = owner.getBoundingBox().inflate((Double)Config.HEART_BIND_RANGE.get());
      EntityMaid best = null;
      double bestDist = Double.MAX_VALUE;

      for (Entity entity : owner.level().getEntities(owner, box, e -> e instanceof EntityMaid)) {
         if (entity instanceof EntityMaid maid && usable(maid, owner) && wearsSomething(maid)) {
            double dist = owner.distanceToSqr(maid);
            if (dist < bestDist) {
               bestDist = dist;
               best = maid;
            }
         }
      }

      if (best != null) {
         ON_DUTY.put(ownerId, new HeartGuard.Stamp(best, bestDist, now));
         PROBED.incrementAndGet();
      }

      return best;
   }

   private static void pull(EntityMaid maid, LivingEntity owner, long now) {
      AABB box = owner.getBoundingBox().inflate((Double)Config.HEART_HATE_RANGE.get());
      List<Entity> around = owner.level().getEntities(owner, box, e -> e instanceof Mob);
      int moved = 0;

      for (Entity entity : around) {
         if (entity instanceof Mob mob && !(mob instanceof EntityMaid) && !mob.isRemoved() && mob.isAlive()) {
            LivingEntity target = currentTarget(mob);
            if (target != null && owner.getUUID().equals(target.getUUID()) && turn(mob, maid, owner)) {
               lastPulled = String.valueOf(mob.getEncodeId());
               moved++;
            }
         }
      }

      if (moved > 0 && now - lastPullLog >= 200L) {
         lastPullLog = now;
         ShenlinMod.LOGGER.info("[shenlin] heart pulled {} onto maid last={}", moved, lastPulled);
      }
   }

   private static boolean turn(LivingEntity enemy, EntityMaid maid, LivingEntity owner) {
      if (TurnTarget.turn(enemy, maid, owner)) {
         TURNED.incrementAndGet();
         return true;
      } else {
         BLOCKED.incrementAndGet();
         return false;
      }
   }

   private static LivingEntity currentTarget(Mob mob) {
      LivingEntity direct = mob.getTarget();
      if (direct != null) {
         return direct;
      }

      try {
         Brain brain = mob.getBrain();
         if (brain != null && brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) instanceof LivingEntity living) {
            return living;
         }
      } catch (Throwable var5) {
      }

      return null;
   }

   private static boolean onDuty(Player owner) {
      return (Boolean)Config.DEVOTED_HEART_ENABLED.get() && !owner.level().isClientSide;
   }

   private static boolean usable(EntityMaid maid, Player owner) {
      return !maid.isRemoved() && !owner.isRemoved() && owner.getUUID().equals(maid.getOwnerUUID());
   }

   private static double sqr(double value) {
      return value * value;
   }

   private static boolean wearsHeart(EntityMaid maid) {
      return wears(maid, (Item)ModItems.DEVOTED_HEART.get());
   }

   private static boolean wearsShield(EntityMaid maid) {
      return wears(maid, (Item)ModItems.GUARDIAN_SHIELD.get());
   }

   private static boolean wearsSomething(EntityMaid maid) {
      return wearsHeart(maid) || wearsShield(maid);
   }

   private static boolean wears(EntityMaid maid, Item item) {
      try {
         return maid.getMaidBauble().containsItem(item);
      } catch (Throwable t) {
         return false;
      }
   }

   private static void diag(long now) {
      if (now - lastDiag >= 200L) {
         long seen = SEEN.getAndSet(0L);
         long swapped = SWAPPED.getAndSet(0L);
         long missed = MISSED.getAndSet(0L);
         long probed = PROBED.getAndSet(0L);
         long turned = TURNED.getAndSet(0L);
         long blocked = BLOCKED.getAndSet(0L);
         lastDiag = now;
         if (seen > 0L || swapped > 0L || missed > 0L || probed > 0L || turned > 0L || blocked > 0L) {
            ShenlinMod.LOGGER
               .info(
                  "[shenlin] heart event seen={} swapped={} miss={} probed={} turned={} blocked={}",
                  new Object[]{seen, swapped, missed, probed, turned, blocked}
               );
         }
      }
   }

   public static void cleanup(long now) {
      ON_DUTY.entrySet().removeIf(e -> now - e.getValue().seen > 160L);
      PROBED_AT.entrySet().removeIf(e -> now - e.getValue() > 400L);
      SWEPT_AT.entrySet().removeIf(e -> now - e.getValue() > 400L);
   }

   private record Stamp(EntityMaid maid, double dist, long seen) {
   }
}

