package com.myx00x.maid.event;

import com.github.tartaricacid.touhoulittlemaid.api.task.IAttackTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import com.github.yimeng261.maidspell.damage.InfoDamageSource;
import com.github.yimeng261.maidspell.item.MaidSpellItems;
import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.combat.BlackHoleKeeper;
import com.myx00x.maid.combat.ExecutionCounter;
import com.myx00x.maid.combat.Executioner;
import com.myx00x.maid.combat.HoverCannon;
import com.myx00x.maid.combat.TargetKeeper;
import com.myx00x.maid.combat.TrueDamageOverrider;
import com.myx00x.maid.compat.Upstream;
import com.myx00x.maid.registry.ModItems;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class MaidAttackListener {
   private static long lastLog;
   private static final Map<UUID, Map<UUID, Long>> LINK_TICK = new ConcurrentHashMap<>();

   @SubscribeEvent(priority = EventPriority.HIGH)
   public static void onLivingHurt(LivingHurtEvent event) {
      if (event.getSource().getEntity() instanceof EntityMaid maid) {
         if (!maid.level().isClientSide) {
            LivingEntity target = event.getEntity();
            if (target != maid && !(target instanceof Player) && !(target instanceof EntityMaid)) {
               if (!Upstream.SPELL || !(event.getSource() instanceof InfoDamageSource)) {
                  if (!(event.getAmount() <= 0.0F)) {
                     if (!target.isDeadOrDying() && !target.isRemoved()) {
                        if (ItemsUtil.hasBaubleItemInMaid(maid, (Item)ModItems.FINAL_SLASH.get())) {
                           addLinkTrueDamage(maid, target);
                           if (!HoverCannon.isCannonDamage(event.getSource()) && !BlackHoleKeeper.isHoleDamage(event.getSource())) {
                              if (ExecutionCounter.recordDamage(maid, target, event.getAmount())) {
                                 event.setCanceled(true);
                                 Executioner.executeByOwner(maid, target, "damage");
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

   @SubscribeEvent(priority = EventPriority.LOW)
   public static void onMaidHurt(LivingHurtEvent event) {
      if (event.getEntity() instanceof EntityMaid maid && !maid.level().isClientSide) {
         if (event.getSource().getEntity() instanceof LivingEntity attacker && attacker != maid) {
            if (!(attacker instanceof Player)) {
               if (maid.getTask() instanceof IAttackTask task && task.canAttack(maid, attacker)) {
                  TargetKeeper.retaliate(maid, attacker);
               }
            }
         }
      }
   }

   private static void addLinkTrueDamage(EntityMaid maid, LivingEntity target) {
      if (Upstream.SPELL) {
         if (ItemsUtil.hasBaubleItemInMaid(maid, (Item)MaidSpellItems.CHAOS_BOOK.get())) {
            long now = target.level().getGameTime();
            if (firstLinkThisTick(maid, target, now)) {
               float extra = (float)(target.getMaxHealth() * (Double)Config.FINAL_SLASH_HIT_RATIO.get());
               int hits = (Integer)Config.FINAL_SLASH_HIT_COUNT.get();
               if (!(extra <= 0.0F) || hits > 0) {
                  TrueDamageOverrider.enqueue(target, extra, maid, hits);
                  ExecutionCounter.recordIssued(maid, target, extra);
                  if (now - lastLog >= 100L) {
                     lastLog = now;
                     ShenlinMod.LOGGER.info("[shenlin] maid hit {} link true damage {} count {}", new Object[]{target.getType().getDescriptionId(), extra, hits});
                  }
               }
            }
         }
      }
   }

   private static boolean firstLinkThisTick(EntityMaid maid, LivingEntity target, long now) {
      Map<UUID, Long> inner = LINK_TICK.computeIfAbsent(maid.getUUID(), k -> new ConcurrentHashMap<>());
      Long last = inner.get(target.getUUID());
      if (last != null && last == now) {
         return false;
      }

      inner.put(target.getUUID(), now);
      if (inner.size() > 64) {
         inner.entrySet().removeIf(e -> now - e.getValue() > 40L);
      }

      if (LINK_TICK.size() > 64) {
         LINK_TICK.entrySet().removeIf(e -> {
            Map<UUID, Long> one = e.getValue();
            one.entrySet().removeIf(t -> now - t.getValue() > 40L);
            return one.isEmpty();
         });
      }

      return true;
   }

   public static void forgetAll() {
      LINK_TICK.clear();
   }
}

