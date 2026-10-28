package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import com.github.yimeng261.maidspell.item.MaidSpellItems;
import com.github.yimeng261.maidspell.item.bauble.silverCercis.SilverCercisBauble;
import com.github.yimeng261.maidspell.spell.manager.BaubleStateManager;
import com.github.yimeng261.maidspell.utils.DataItem;
import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.compat.Upstream;
import com.myx00x.maid.registry.ModItems;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

public final class ReflectLink {
   private static Field mapField;
   private static boolean mapResolved;
   private static Method getA;
   private static Method getB;
   private static boolean methodsResolved;
   private static long lastLog;

   private ReflectLink() {
   }

   public static void onCercis(EntityMaid maid, LivingEntity attacker, DataItem item) {
      try {
         if (!Upstream.SPELL) {
            return;
         }

         if (!(Boolean)Config.FINAL_SLASH_REFLECT_LINK.get() || maid.level().isClientSide) {
            return;
         }

         if (attacker == null || item == null || attacker.isRemoved() || !attacker.isAlive()) {
            return;
         }

         if (attacker instanceof Player || attacker instanceof EntityMaid) {
            return;
         }

         if (!justTriggered(maid)) {
            return;
         }

         if (!ItemsUtil.hasBaubleItemInMaid(maid, (Item)ModItems.FINAL_SLASH.get())) {
            return;
         }

         double factor = BaubleStateManager.hasBauble(maid, MaidSpellItems.DREAM_CAT_CRYSTAL) ? 2.0 : 1.0;
         float extra = (float)(item.getAmount() * com.github.yimeng261.maidspell.Config.silverCercisTrueDamageMultiplier * factor);
         if (extra <= 0.0F) {
            return;
         }

         TrueDamageOverrider.enqueue(attacker, extra, maid, 0);
         if (ExecutionCounter.recordReflect(maid, attacker, extra)) {
            Executioner.executeByOwner(maid, attacker, "reflect");
         }

         long now = maid.level().getGameTime();
         if (now - lastLog >= 200L) {
            lastLog = now;
            ShenlinMod.LOGGER.info("[shenlin] reflect link {} extra {}", attacker.getType().getDescriptionId(), extra);
         }
      } catch (Throwable var8) {
      }
   }

   private static boolean justTriggered(EntityMaid maid) {
      Object pair = stampOf(maid);
      if (pair != null && resolveMethods(pair.getClass())) {
         try {
            Object count = getA.invoke(pair);
            Object tick = getB.invoke(pair);
            return count instanceof Integer c && tick instanceof Integer t && c == 0 && t == maid.tickCount;
         } catch (Throwable t) {
            return false;
         }
      } else {
         return false;
      }
   }

   private static Object stampOf(EntityMaid maid) {
      try {
         if (!resolveMap()) {
            return null;
         }

         Map<?, ?> map = (Map<?, ?>)mapField.get(null);
         return map == null ? null : map.get(maid.getUUID());
      } catch (Throwable t) {
         return null;
      }
   }

   private static boolean resolveMap() {
      if (mapResolved) {
         return mapField != null;
      }

      mapResolved = true;

      try {
         Field field = SilverCercisBauble.class.getDeclaredField("maidCercisMap");
         field.setAccessible(true);
         mapField = field;
      } catch (Throwable var1) {
      }

      return mapField != null;
   }

   private static boolean resolveMethods(Class<?> type) {
      if (methodsResolved) {
         return getA != null && getB != null;
      }

      methodsResolved = true;

      try {
         getA = type.getMethod("getA");
         getB = type.getMethod("getB");
      } catch (Throwable var2) {
      }

      return getA != null && getB != null;
   }
}

