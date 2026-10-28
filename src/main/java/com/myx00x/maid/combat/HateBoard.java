package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public final class HateBoard {
   private static final int MARK = 0;
   private static final int STRIKE = 1;
   private static final int HURT = 2;
   private static final int SLOTS = 3;
   private static final double MARK_LIMIT = 256.0;
   private static final double MARK_REPORT_LIMIT = 192.0;
   private static final long ORDER_TICKS = 40L;
   private static final double REACH_MARGIN = 6.0;
   private static final double MIN_TAKEOVER = 16.0;
   private static final Map<UUID, HateBoard.Slot[]> BOARDS = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> ORDERS = new ConcurrentHashMap<>();
   private static final Map<UUID, Float> WIDE = new ConcurrentHashMap<>();
   private static final Set<UUID> ANNOUNCED = ConcurrentHashMap.newKeySet();
   private static final AtomicBoolean RADIUS_ANNOUNCED = new AtomicBoolean();
   private static long ticks;

   private HateBoard() {
   }

   public static void mark(Player owner, LivingEntity target) {
      if (!(target.distanceToSqr(owner) > 65536.0)) {
         write(owner, 0, target, (Integer)Config.HATE_MARK_TICKS.get());
         Nudge.strike(owner, target);
         ShenlinMod.LOGGER.info("[shenlin] hate mark {}", target.getEncodeId());
      }
   }

   public static void strike(Player owner, LivingEntity target) {
      write(owner, 1, target, (Integer)Config.HATE_STRIKE_TICKS.get());
   }

   public static void hurt(Player owner, LivingEntity attacker) {
      write(owner, 2, attacker, (Integer)Config.HATE_HURT_TICKS.get());
   }

   public static void accept(Player owner, int entityId) {
      if (owner.level().getEntity(entityId) instanceof LivingEntity living && living != owner) {
         if (!(living.distanceToSqr(owner) > 36864.0)) {
            mark(owner, living);
         }
      }
   }

   public static boolean markedBy(EntityMaid maid, LivingEntity target) {
      if (!BOARDS.isEmpty() && (Boolean)Config.HATE_ENABLED.get() && !maid.level().isClientSide) {
         UUID ownerId = maid.getOwnerUUID();
         if (ownerId == null) {
            return false;
         }

         HateBoard.Slot[] slots = BOARDS.get(ownerId);
         return slots == null ? false : fresh(slots[0], target.getUUID(), maid.level().getGameTime(), (Integer)Config.HATE_MARK_TICKS.get());
      } else {
         return false;
      }
   }

   public static List<LivingEntity> marksOf(EntityMaid maid, double range) {
      List<LivingEntity> out = new ArrayList<>();
      if ((Boolean)Config.HATE_ENABLED.get() && !maid.level().isClientSide) {
         UUID ownerId = maid.getOwnerUUID();
         if (ownerId == null) {
            return out;
         }

         HateBoard.Slot[] slots = BOARDS.get(ownerId);
         if (slots == null) {
            return out;
         }

         long now = maid.level().getGameTime();
         int ttl = (Integer)Config.HATE_MARK_TICKS.get();
         List<Entry<UUID, Long>> list = new ArrayList<>(slots[0].stamps.entrySet());
         list.removeIf(e -> now - e.getValue() > ttl);

         for (Entry<UUID, Long> entry : list) {
            LivingEntity one = find(maid, entry.getKey(), range);
            if (one != null) {
               out.add(one);
            }
         }

         return out;
      } else {
         return out;
      }
   }

   public static LivingEntity declaredTarget(EntityMaid maid, double range) {
      if ((Boolean)Config.HATE_ENABLED.get() && !maid.level().isClientSide) {
         UUID ownerId = maid.getOwnerUUID();
         if (ownerId == null) {
            return null;
         }

         HateBoard.Slot[] slots = BOARDS.get(ownerId);
         if (slots == null) {
            return null;
         }

         long now = maid.level().getGameTime();

         for (int i = 0; i < 3; i++) {
            LivingEntity found = freshest(slots[i], now, ttlOf(i), id -> find(maid, id, range));
            if (found != null) {
               return found;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public static void sweep(MinecraftServer server) {
      if ((Boolean)Config.HATE_ENABLED.get() && server != null && server.overworld() != null) {
         HomeKeeper.tick(server.overworld().getGameTime());
         if (!BOARDS.isEmpty()) {
            int interval = (Integer)Config.HATE_SWEEP_TICKS.get();
            if (++ticks % interval == 0L) {
               for (Player owner : server.getPlayerList().getPlayers()) {
                  if (!owner.level().isClientSide) {
                     HateBoard.Slot[] slots = BOARDS.get(owner.getUUID());
                     if (slots != null) {
                        List<EntityMaid> maids = maidsAround(owner);
                        if (!maids.isEmpty()) {
                           long now = owner.level().getGameTime();
                           LivingEntity target = pick(owner, slots, now);
                           if (target != null) {
                              double reach = (Double)Config.HATE_RANGE.get();

                              for (EntityMaid maid : maids) {
                                 if (!(maid.distanceToSqr(target) > reach * reach)) {
                                    AttackListKeeper.ensure(maid, target);
                                    if (TargetKeeper.reassert(maid, target)) {
                                       ORDERS.put(maid.getUUID(), now + 40L);
                                       HomeKeeper.follow(maid, target);
                                       renewRadius(maid);
                                       if (ANNOUNCED.add(maid.getUUID())) {
                                          ShenlinMod.LOGGER.info("[shenlin] hate order {} -> {}", maid.getId(), target.getEncodeId());
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void write(Player owner, int slot, LivingEntity target, int ttl) {
      if ((Boolean)Config.HATE_ENABLED.get() && !owner.level().isClientSide) {
         if (target != owner && !(target instanceof Player) && !(target instanceof EntityMaid) && target.isAlive() && !target.isRemoved()) {
            HateBoard.Slot entry = boards(owner)[slot];
            long now = owner.level().getGameTime();
            entry.stamps.put(target.getUUID(), now);
            entry.stamps.entrySet().removeIf(e -> now - e.getValue() > ttl);
         }
      }
   }

   public static boolean underOrders(EntityMaid maid) {
      if (ORDERS.isEmpty()) {
         return false;
      } else {
         Long until = ORDERS.get(maid.getUUID());
         if (until == null) {
            return false;
         } else {
            return !maid.level().isClientSide && Config.HATE_ENABLED.get() ? maid.level().getGameTime() <= until : false;
         }
      }
   }

   public static float takeoverRadius(EntityMaid maid) {
      if (!underOrders(maid)) {
         return 0.0F;
      }

      Float wide = WIDE.get(maid.getUUID());
      return wide == null ? 0.0F : wide;
   }

   private static void renewRadius(EntityMaid maid) {
      double cap = Math.max((Double)Config.HATE_TAKEOVER_RANGE.get(), (Double)Config.HATE_RANGE.get());
      float wide = (float)takeoverNeeded(cap, maid);
      Float prev = WIDE.get(maid.getUUID());
      if (prev != null && prev > wide) {
         wide = prev;
      }

      WIDE.put(maid.getUUID(), wide);
      if (RADIUS_ANNOUNCED.compareAndSet(false, true)) {
         ShenlinMod.LOGGER.info("[shenlin] hate takeover radius={}", wide);
      }
   }

   private static double takeoverNeeded(double cap, EntityMaid maid) {
      LivingEntity target = currentTarget(maid);
      if (target == null) {
         return cap;
      }

      double needed = Math.sqrt(maid.distanceToSqr(target)) + 6.0;
      return Math.min(cap, Math.max(needed, 16.0));
   }

   private static LivingEntity currentTarget(EntityMaid maid) {
      Brain<?> brain = maid.getBrain();
      return brain == null ? null : (LivingEntity)brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
   }

   private static HateBoard.Slot[] boards(Player owner) {
      return BOARDS.computeIfAbsent(owner.getUUID(), key -> {
         HateBoard.Slot[] slots = new HateBoard.Slot[3];

         for (int i = 0; i < 3; i++) {
            slots[i] = new HateBoard.Slot();
         }

         return slots;
      });
   }

   private static int ttlOf(int slot) {
      return switch (slot) {
         case 0 -> Config.HATE_MARK_TICKS.get();
         case 1 -> Config.HATE_STRIKE_TICKS.get();
         default -> Config.HATE_HURT_TICKS.get();
      };
   }

   private static boolean fresh(HateBoard.Slot slot, UUID targetId, long now, int ttl) {
      Long stamp = slot.stamps.get(targetId);
      return stamp != null && now - stamp <= ttl;
   }

   private static LivingEntity freshest(HateBoard.Slot slot, long now, int ttl, Function<UUID, LivingEntity> resolver) {
      List<Entry<UUID, Long>> list = new ArrayList<>(slot.stamps.entrySet());
      list.removeIf(e -> now - e.getValue() > ttl);
      if (list.isEmpty()) {
         return null;
      }

      list.sort(Comparator.<Entry<UUID, Long>>comparingLong(e -> e.getValue()).reversed());

      for (Entry<UUID, Long> entry : list) {
         LivingEntity living = resolver.apply(entry.getKey());
         if (living != null) {
            return living;
         }
      }

      return null;
   }

   private static LivingEntity find(EntityMaid maid, UUID targetId, double range) {
      if (maid.level() instanceof ServerLevel level) {
         return level.getEntity(targetId) instanceof LivingEntity living
               && living != maid
               && !(living instanceof Player)
               && !(living instanceof EntityMaid)
               && living.isAlive()
               && !living.isRemoved()
               && maid.distanceToSqr(living) <= range * range
               && !RetreatManager.isAvoided(maid.getUUID(), targetId, maid.level().getGameTime())
            ? living
            : null;
      } else {
         return null;
      }
   }

   private static LivingEntity pick(Player owner, HateBoard.Slot[] slots, long now) {
      Map<UUID, LivingEntity> around = null;

      for (int i = 0; i < 3; i++) {
         HateBoard.Slot slot = slots[i];
         int ttl = ttlOf(i);
         slot.stamps.entrySet().removeIf(e -> now - e.getValue() > ttl);
         if (!slot.stamps.isEmpty()) {
            if (around == null) {
               around = livingAround(owner, (Double)Config.HATE_RANGE.get());
            }

            LivingEntity found = freshest(slot, now, ttl, around::get);
            if (found != null) {
               return found;
            }
         }
      }

      return null;
   }

   private static Map<UUID, LivingEntity> livingAround(Player owner, double range) {
      AABB box = owner.getBoundingBox().inflate(range);
      Map<UUID, LivingEntity> found = new HashMap<>();

      for (Entity entity : owner.level().getEntities(owner, box, e -> e instanceof LivingEntity)) {
         if (entity instanceof LivingEntity living && living != owner && living.isAlive() && !living.isRemoved()) {
            found.put(living.getUUID(), living);
         }
      }

      return found;
   }

   private static List<EntityMaid> maidsAround(Player owner) {
      AABB box = owner.getBoundingBox().inflate((Double)Config.HATE_BIND_RANGE.get());
      List<EntityMaid> found = new ArrayList<>();

      for (Entity entity : owner.level().getEntities(owner, box, e -> e instanceof EntityMaid)) {
         if (entity instanceof EntityMaid maid && maid.isAlive() && !maid.isRemoved() && owner.getUUID().equals(maid.getOwnerUUID())) {
            found.add(maid);
         }
      }

      return found;
   }

   public static void cleanup(long now) {
      BOARDS.entrySet().removeIf(entry -> {
         HateBoard.Slot[] slots = entry.getValue();

         for (int i = 0; i < 3; i++) {
            int ttl = ttlOf(i);
            slots[i].stamps.entrySet().removeIf(e -> now - e.getValue() > ttl);
            if (!slots[i].stamps.isEmpty()) {
               return false;
            }
         }

         return true;
      });
      ORDERS.entrySet().removeIf(entry -> now > entry.getValue());
      WIDE.keySet().removeIf(id -> !ORDERS.containsKey(id));
      ANNOUNCED.removeIf(id -> !ORDERS.containsKey(id));
   }

   private static final class Slot {
      private final Map<UUID, Long> stamps = new HashMap<>();
   }
}

