package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.registry.ModItems;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

public final class ExecutionCounter {
   private static final Map<UUID, Map<UUID, ExecutionCounter.DamageRecord>> DAMAGE = new ConcurrentHashMap<>();
   private static final Map<UUID, Map<UUID, ExecutionCounter.TrueHitRecord>> TRUE_HITS = new ConcurrentHashMap<>();

   private ExecutionCounter() {
   }

   public static boolean recordDamage(EntityMaid maid, LivingEntity target, float amount) {
      Map<UUID, ExecutionCounter.DamageRecord> inner = DAMAGE.computeIfAbsent(maid.getUUID(), k -> new ConcurrentHashMap<>());
      ExecutionCounter.DamageRecord rec = inner.computeIfAbsent(target.getUUID(), k -> new ExecutionCounter.DamageRecord(target));
      countSwing(rec, target.level().getGameTime());
      rec.sum += amount;
      return rec.sum >= strikeLine(rec, target);
   }

   public static boolean recordReflect(EntityMaid maid, LivingEntity target, float amount) {
      Map<UUID, ExecutionCounter.DamageRecord> inner = DAMAGE.computeIfAbsent(maid.getUUID(), k -> new ConcurrentHashMap<>());
      ExecutionCounter.DamageRecord rec = inner.computeIfAbsent(target.getUUID(), k -> new ExecutionCounter.DamageRecord(target));
      rec.sum += amount;
      return rec.sum >= strikeLine(rec, target);
   }

   public static boolean recordIssued(EntityMaid maid, LivingEntity target, float amount) {
      if (!(amount <= 0.0F) && ItemsUtil.hasBaubleItemInMaid(maid, (Item)ModItems.FINAL_SLASH.get())) {
         Map<UUID, ExecutionCounter.DamageRecord> inner = DAMAGE.computeIfAbsent(maid.getUUID(), k -> new ConcurrentHashMap<>());
         ExecutionCounter.DamageRecord rec = inner.computeIfAbsent(target.getUUID(), k -> new ExecutionCounter.DamageRecord(target));
         rec.sum += amount;
         float line = strikeLine(rec, target);
         if (rec.sum >= line) {
            ShenlinMod.LOGGER.info("[shenlin] issued filled {} sum {} / {}", new Object[]{target.getType().getDescriptionId(), rec.sum, line});
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static float strikeLine(ExecutionCounter.DamageRecord rec, LivingEntity target) {
      float now = target.getMaxHealth();
      if (rec.ceiling <= 0.0F) {
         rec.ceiling = now;
      } else if (now > rec.ceiling) {
         rec.ceiling = now;
      }

      return Math.max(1.0F, Math.max(now, rec.ceiling));
   }

   public static void noteSwing(EntityMaid maid, LivingEntity target) {
      Map<UUID, ExecutionCounter.DamageRecord> inner = DAMAGE.computeIfAbsent(maid.getUUID(), k -> new ConcurrentHashMap<>());
      ExecutionCounter.DamageRecord rec = inner.computeIfAbsent(target.getUUID(), k -> new ExecutionCounter.DamageRecord(target));
      countSwing(rec, target.level().getGameTime());
   }

   private static void countSwing(ExecutionCounter.DamageRecord rec, long now) {
      if (rec.lastSwingTick != now) {
         rec.lastSwingTick = now;
         rec.swings++;
      }
   }

   public static int recordTrueHit(EntityMaid maid, LivingEntity target, int hits) {
      Map<UUID, ExecutionCounter.TrueHitRecord> inner = TRUE_HITS.computeIfAbsent(maid.getUUID(), k -> new ConcurrentHashMap<>());
      ExecutionCounter.TrueHitRecord rec = inner.computeIfAbsent(target.getUUID(), k -> new ExecutionCounter.TrueHitRecord(target));
      rec.count += hits;
      return rec.count;
   }

   public static int swings(EntityMaid maid, LivingEntity target) {
      Map<UUID, ExecutionCounter.DamageRecord> inner = DAMAGE.get(maid.getUUID());
      ExecutionCounter.DamageRecord rec = inner == null ? null : inner.get(target.getUUID());
      return rec == null ? 0 : rec.swings;
   }

   public static void clearTarget(UUID maidId, UUID targetId) {
      Map<UUID, ExecutionCounter.DamageRecord> d = DAMAGE.get(maidId);
      if (d != null) {
         d.remove(targetId);
      }

      Map<UUID, ExecutionCounter.TrueHitRecord> t = TRUE_HITS.get(maidId);
      if (t != null) {
         t.remove(targetId);
      }
   }

   public static void clearMaid(UUID maidId) {
      DAMAGE.remove(maidId);
      TRUE_HITS.remove(maidId);
   }

   public static void cleanup() {
      DAMAGE.entrySet().removeIf(e -> {
         e.getValue().entrySet().removeIf(t -> isGone(t.getValue().target));
         return e.getValue().isEmpty();
      });
      TRUE_HITS.entrySet().removeIf(e -> {
         e.getValue().entrySet().removeIf(t -> isGone(t.getValue().target));
         return e.getValue().isEmpty();
      });
   }

   private static boolean isGone(WeakReference<LivingEntity> ref) {
      LivingEntity entity = ref.get();
      return entity == null || entity.isRemoved() || entity.isDeadOrDying();
   }

   private static final class DamageRecord {
      float sum;
      int swings;
      long lastSwingTick = -1L;
      float ceiling = -1.0F;
      final WeakReference<LivingEntity> target;

      DamageRecord(LivingEntity target) {
         this.sum = 0.0F;
         this.target = new WeakReference<>(target);
      }
   }

   private static final class TrueHitRecord {
      int count = 0;
      final WeakReference<LivingEntity> target;

      TrueHitRecord(LivingEntity target) {
         this.target = new WeakReference<>(target);
      }
   }
}

