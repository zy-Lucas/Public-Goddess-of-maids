package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskIdle;
import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

public final class TargetKeeper {
   private static final long STALE_TICKS = 6000L;
   private static final long SWEEP_TICKS = 10L;
   private static final long FALLBACK_TICKS = 200L;
   private static final AABB EVERYWHERE = new AABB(-3.0E7, -2048.0, -3.0E7, 3.0E7, 2048.0, 3.0E7);
   private static final Map<UUID, TargetKeeper.Lock> LOCKS = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> IDLE_SINCE = new ConcurrentHashMap<>();
   private static final long SCAVENGE_AFTER = 40L;
   private static long lastLog;
   private static long ticks;
   private static final AtomicLong SELF_DRIVEN = new AtomicLong();
   private static final AtomicBoolean DRIVEN_ANNOUNCED = new AtomicBoolean();

   private TargetKeeper() {
   }

   public static void sweep(MinecraftServer server) {
      if ((Boolean)Config.TARGET_KEEPER_ENABLED.get() && server != null) {
         if (++ticks % 200L == 0L && SELF_DRIVEN.get() <= 0L) {
            for (ServerLevel level : server.getAllLevels()) {
               for (EntityMaid maid : level.getEntities(EntityTypeTest.forClass(EntityMaid.class), EVERYWHERE, m -> true)) {
                  try {
                     tick(maid);
                  } catch (Throwable t) {
                     ShenlinMod.LOGGER.error("[shenlin] keeper tick failed", t);
                  }
               }
            }
         }
      }
   }

   public static void onMaidTick(EntityMaid maid) {
      if ((Boolean)Config.TARGET_KEEPER_ENABLED.get()) {
         SELF_DRIVEN.incrementAndGet();
         if (DRIVEN_ANNOUNCED.compareAndSet(false, true)) {
            ShenlinMod.LOGGER.info("[shenlin] keeper self-driven");
         }

         if (maid.tickCount % 10L == 0L) {
            try {
               tick(maid);
            } catch (Throwable t) {
               ShenlinMod.LOGGER.error("[shenlin] keeper tick failed", t);
            }
         }
      }
   }

   public static void tick(EntityMaid maid) {
      if ((Boolean)Config.TARGET_KEEPER_ENABLED.get() && !maid.level().isClientSide && maid.getBrain() != null) {
         if (!onIdleTask(maid)) {
            boolean underOrders = HateBoard.underOrders(maid);
            long now = maid.level().getGameTime();
            LivingEntity current = (LivingEntity)maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
            if (current != null && RetreatManager.isAvoided(maid.getUUID(), current.getUUID(), now)) {
               maid.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
               maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
               maid.setTarget(null);
               LOCKS.remove(maid.getUUID());
            } else {
               TargetKeeper.Lock lock = LOCKS.get(maid.getUUID());
               if (current != null && (lock == null || !lock.targetId.equals(current.getUUID()))) {
                  lock = new TargetKeeper.Lock(current.getUUID(), now);
                  LOCKS.put(maid.getUUID(), lock);
               }

               if (lock != null) {
                  IDLE_SINCE.remove(maid.getUUID());
                  lock.lastTouch = now;
                  if (current != null) {
                     lock.chasingSince = 0L;
                     lock.gaveUp = false;
                  } else if (!lock.gaveUp) {
                     double reach = underOrders
                        ? Math.max((Double)Config.HATE_RANGE.get(), (Double)Config.TARGET_KEEPER_RANGE.get())
                        : (Double)Config.TARGET_KEEPER_RANGE.get();
                     LivingEntity target = find(maid, lock.targetId, reach);
                     if (target == null || RetreatManager.isAvoided(maid.getUUID(), target.getUUID(), now)) {
                        LOCKS.remove(maid.getUUID());
                     } else if (maid.distanceTo(target) > reach) {
                        LOCKS.remove(maid.getUUID());
                     } else {
                        if (lock.chasingSince == 0L) {
                           lock.chasingSince = now;
                        } else if (now - lock.chasingSince > ((Integer)Config.TARGET_KEEPER_KEEP_TICKS.get()).intValue()) {
                           lock.gaveUp = true;
                           return;
                        }

                        setTarget(maid, target);
                     }
                  }
               } else if (!underOrders) {
                  Long idle = IDLE_SINCE.get(maid.getUUID());
                  if (idle == null) {
                     IDLE_SINCE.put(maid.getUUID(), now);
                  } else if (now - idle >= 40L) {
                     scavenge(maid, now);
                  }
               }
            }
         }
      }
   }

   private static void scavenge(EntityMaid maid, long now) {
      double range = (Double)Config.TARGET_KEEPER_RANGE.get();
      AABB box = maid.getBoundingBox().inflate(range);
      LivingEntity best = null;
      double bestDist = Double.MAX_VALUE;

      for (Entity entity : maid.level().getEntities(maid, box, e -> e instanceof LivingEntity)) {
         if (entity instanceof LivingEntity living
            && living != maid
            && living.isAlive()
            && !living.isRemoved()
            && !(living instanceof EntityMaid)
            && !(living instanceof TamableAnimal tamable && tamable.isTame())
            && !RetreatManager.isAvoided(maid.getUUID(), living.getUUID(), now)) {
            double dist = maid.distanceToSqr(living);
            if (dist < bestDist) {
               bestDist = dist;
               best = living;
            }
         }
      }

      if (best != null) {
         LOCKS.put(maid.getUUID(), new TargetKeeper.Lock(best.getUUID(), now));
         setTarget(maid, best);
      }
   }

   public static void retaliate(EntityMaid maid, LivingEntity attacker) {
      if ((Boolean)Config.TARGET_KEEPER_ENABLED.get() && !maid.level().isClientSide && maid.getBrain() != null) {
         if (!onIdleTask(maid)) {
            if (attacker.isAlive() && !attacker.isRemoved()) {
               if (!RetreatManager.isAvoided(maid.getUUID(), attacker.getUUID(), maid.level().getGameTime())) {
                  LivingEntity current = (LivingEntity)maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
                  if (current == null || !current.isAlive() || current.isRemoved()) {
                     long now = maid.level().getGameTime();
                     setTarget(maid, attacker);
                     LOCKS.put(maid.getUUID(), new TargetKeeper.Lock(attacker.getUUID(), now));
                     if (now - lastLog >= 100L) {
                        lastLog = now;
                        ShenlinMod.LOGGER.info("[shenlin] maid retaliate {}", attacker.getType().getDescriptionId());
                     }
                  }
               }
            }
         }
      }
   }

   public static boolean reassert(EntityMaid maid, LivingEntity target) {
      if (maid.level().isClientSide || maid.getBrain() == null) {
         return false;
      }

      if (onIdleTask(maid)) {
         return false;
      }

      if (RetreatManager.isAvoided(maid.getUUID(), target.getUUID(), maid.level().getGameTime())) {
         return false;
      }

      setTarget(maid, target);
      hold(maid, target);
      return true;
   }

   public static void hold(EntityMaid maid, LivingEntity target) {
      UUID id = maid.getUUID();
      TargetKeeper.Lock lock = LOCKS.get(id);
      if (lock != null && lock.targetId.equals(target.getUUID())) {
         lock.lastTouch = maid.level().getGameTime();
      } else {
         LOCKS.put(id, new TargetKeeper.Lock(target.getUUID(), maid.level().getGameTime()));
      }
   }

   private static void setTarget(EntityMaid maid, LivingEntity target) {
      Brain<?> brain = maid.getBrain();
      LivingEntity aiming = (LivingEntity)brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
      if (aiming == target) {
         brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
      } else {
         brain.setMemory(MemoryModuleType.ATTACK_TARGET, target);
         brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
         WalkTarget walking = (WalkTarget)brain.getMemory(MemoryModuleType.WALK_TARGET).orElse(null);
         if (walking == null || !(walking.getTarget() instanceof EntityTracker tracker && tracker.getEntity() == target)) {
            brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new EntityTracker(target, false), 1.0F, 0));
         }
      }
   }

   private static boolean onIdleTask(EntityMaid maid) {
      IMaidTask task = maid.getTask();
      return task != null && TaskIdle.UID.equals(task.getUid());
   }

   public static void clear(UUID maidId) {
      LOCKS.remove(maidId);
   }

   public static void cleanup(long now) {
      LOCKS.entrySet().removeIf(e -> now - e.getValue().lastTouch > 6000L);
      IDLE_SINCE.entrySet().removeIf(e -> now - e.getValue() > 6000L);
   }

   private static LivingEntity find(EntityMaid maid, UUID targetId, double range) {
      return maid.level() instanceof ServerLevel level
            && level.getEntity(targetId) instanceof LivingEntity living
            && living.isAlive()
            && !living.isRemoved()
            && living.distanceToSqr(maid) <= range * range
         ? living
         : null;
   }

   public static void forgetAll() {
      LOCKS.clear();
      IDLE_SINCE.clear();
   }

   private static final class Lock {
      private final UUID targetId;
      private long chasingSince;
      private long lastTouch;
      private boolean gaveUp;

      private Lock(UUID targetId, long now) {
         this.targetId = targetId;
         this.lastTouch = now;
      }
   }
}

