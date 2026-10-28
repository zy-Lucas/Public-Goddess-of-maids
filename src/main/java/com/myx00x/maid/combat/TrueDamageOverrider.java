package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.yimeng261.maidspell.utils.TrueDamageUtil;
import com.myx00x.maid.Config;
import com.myx00x.maid.combat.health.UltraHealth;
import com.myx00x.maid.compat.Upstream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.world.entity.LivingEntity;

public final class TrueDamageOverrider {
   private static final ConcurrentLinkedQueue<TrueDamageOverrider.Request> QUEUE = new ConcurrentLinkedQueue<>();

   private TrueDamageOverrider() {
   }

   public static void enqueue(LivingEntity target, float damage, EntityMaid maid, int hits) {
      QUEUE.offer(new TrueDamageOverrider.Request(new WeakReference<>(target), damage, new WeakReference<>(maid), hits));
   }

   public static void processQueued() {
      if (!QUEUE.isEmpty()) {
         List<TrueDamageOverrider.Request> drained = new ArrayList<>();

         TrueDamageOverrider.Request r;
         while ((r = QUEUE.poll()) != null) {
            drained.add(r);
         }

         for (TrueDamageOverrider.Request req : drained) {
            LivingEntity target = req.target().get();
            EntityMaid maid = req.maid().get();
            if (target != null && maid != null && target.isAlive() && !target.isRemoved()) {
               boolean killedByUs = false;
               if (req.damage() > 0.0F) {
                  float before = Executioner.readHealthRaw(target);
                  float newHealth = Math.max(0.0F, before - req.damage());
                  if (!applyDamage(target, newHealth, maid)) {
                     continue;
                  }

                  killedByUs = before > 0.0F && Executioner.readHealthRaw(target) <= 0.0F;
               }

               int hits = ExecutionCounter.recordTrueHit(maid, target, req.hits());
               if (killedByUs && !target.isRemoved()) {
                  Executioner.executeByOwner(maid, target, "truehit");
               } else if (hits >= (Integer)Config.TRUE_HITS_EXECUTION.get()) {
                  Executioner.executeByOwner(maid, target, "hits");
               }
            }
         }
      }
   }

   private static boolean applyDamage(LivingEntity target, float newHealth, EntityMaid maid) {
      if (Executioner.setHealthSilently(target, newHealth)) {
         return true;
      }

      float before = target.getHealth();
      float amount = before - newHealth;
      if (amount > 0.0F && Executioner.maySilenceHealth(target) && UltraHealth.hurtThrough(target, maid, amount)) {
         return true;
      }

      if (!Upstream.SPELL) {
         return false;
      }

      try {
         return TrueDamageUtil.setNewHealth(target, newHealth, maid);
      } catch (Throwable t) {
         return false;
      }
   }

   private record Request(WeakReference<LivingEntity> target, float damage, WeakReference<EntityMaid> maid, int hits) {
   }
}

