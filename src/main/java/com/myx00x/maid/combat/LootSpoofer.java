package com.myx00x.maid.combat;

import com.myx00x.maid.ShenlinMod;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootParams.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

public final class LootSpoofer {
   private static Field lastHurtByPlayer;
   private static Field lastHurtByPlayerTime;
   private static Field lastDamageSource;
   private static Field xpReward;
   private static Field deadFlag;
   private static Method lootTableGetter;
   private static Method xpGetter;
   private static Method lootTableDropper;
   private static Method customLootDropper;

   private LootSpoofer() {
   }

   public static void spoofKiller(LivingEntity target, @Nullable Player player, @Nullable DamageSource source) {
      if (player != null) {
         try {
            if (lastHurtByPlayer == null) {
               lastHurtByPlayer = field(LivingEntity.class, "lastHurtByPlayer", "f_20888_");
               lastHurtByPlayerTime = field(LivingEntity.class, "lastHurtByPlayerTime", "f_20889_");
               lastDamageSource = field(LivingEntity.class, "lastDamageSource", "f_20958_");
            }

            if (lastHurtByPlayer != null) {
               lastHurtByPlayer.set(target, player);
            }

            if (lastHurtByPlayerTime != null) {
               lastHurtByPlayerTime.setInt(target, 100);
            }

            if (lastDamageSource != null && source != null) {
               lastDamageSource.set(target, source);
            }
         } catch (Throwable t) {
            ShenlinMod.LOGGER.warn("[shenlin] execute loot context spoof failed: {}", t.toString());
         }
      }
   }

   public static void settleDeferred(LivingEntity target, DamageSource source) {
      try {
         if (deadFlag == null) {
            deadFlag = field(LivingEntity.class, "dead", "f_20890_");
         }

         if (deadFlag == null || deadFlag.getBoolean(target)) {
            return;
         }

         LivingDeathEvent event = new LivingDeathEvent(target, source);
         MinecraftForge.EVENT_BUS.post(event);
         if (event.isCanceled()) {
            return;
         }

         deadFlag.setBoolean(target, true);
         if (lootTableDropper == null) {
            lootTableDropper = method(LivingEntity.class, "dropFromLootTable", "m_7625_", DamageSource.class, boolean.class);
         }

         if (lootTableDropper != null) {
            lootTableDropper.setAccessible(true);
            lootTableDropper.invoke(target, source, true);
         }

         if (customLootDropper == null) {
            customLootDropper = method(LivingEntity.class, "dropCustomDeathLoot", "m_7472_", DamageSource.class, int.class, boolean.class);
         }

         if (customLootDropper != null) {
            customLootDropper.setAccessible(true);
            customLootDropper.invoke(target, source, lootingOf(killerOf(source)), true);
         }

         ShenlinMod.LOGGER.info("[shenlin] execute {} deferred settlement done (forced)", target.getType().getDescriptionId());
      } catch (Throwable t) {
         ShenlinMod.LOGGER.warn("[shenlin] execute deferred settlement failed: {}", t.toString());
      }
   }

   public static void forceLoot(LivingEntity target, ServerLevel level, DamageSource source, @Nullable Player killer) {
      int items = 0;
      int xp = 0;

      try {
         if (lootTableGetter == null) {
            lootTableGetter = method(LivingEntity.class, "getLootTable", "m_5743_");
         }

         if (lootTableGetter != null) {
            lootTableGetter.setAccessible(true);
            Object tableId = lootTableGetter.invoke(target);
            if (tableId != null) {
               Builder builder = new Builder(level)
                  .withParameter(LootContextParams.THIS_ENTITY, target)
                  .withParameter(LootContextParams.ORIGIN, target.position())
                  .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                  .withOptionalParameter(LootContextParams.KILLER_ENTITY, killer)
                  .withOptionalParameter(LootContextParams.DIRECT_KILLER_ENTITY, killer)
                  .withOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer);
               if (killer != null) {
                  builder = builder.withParameter(LootContextParams.TOOL, killer.getMainHandItem());
               }

               int looting = lootingOf(killer);
               if (looting > 0) {
                  builder = builder.withLuck(looting);
               }

               LootTable table = level.getServer().getLootData().getLootTable((ResourceLocation)tableId);

               for (ObjectListIterator var10 = table.getRandomItems(builder.create(LootContextParamSets.ENTITY)).iterator(); var10.hasNext(); items++) {
                  ItemStack stack = (ItemStack)var10.next();
                  target.spawnAtLocation(stack);
               }
            }
         }
      } catch (Throwable t) {
         ShenlinMod.LOGGER.warn("[shenlin] execute loot table pull failed: {}", t.toString());
      }

      items += wornDrops(level, target);
      if (target instanceof Monster) {
         ExperienceOrb.award(level, target.position(), 5 + level.random.nextInt(10));
      }

      if (target instanceof Animal) {
         target.spawnAtLocation(new ItemStack(Items.LEATHER));
      }

      try {
         if (xpGetter == null) {
            xpGetter = method(LivingEntity.class, "getExperienceReward", "m_213860_");
         }

         if (xpReward == null) {
            xpReward = field(Mob.class, "xpReward", "f_21364_");
         }

         int reward = 0;
         if (xpGetter != null) {
            xpGetter.setAccessible(true);
            if (xpGetter.invoke(target) instanceof Integer value) {
               reward = value;
            }
         }

         if (reward <= 0 && xpReward != null) {
            reward = xpReward.getInt(target);
         }

         if (xpReward != null) {
            xpReward.setInt(target, 0);
         }

         if (reward > 0) {
            ExperienceOrb.award(level, target.position(), reward);
            xp = reward;
         }
      } catch (Throwable var12) {
      }

      ShenlinMod.LOGGER.info("[shenlin] execute {} loot {} items {} xp (forced)", new Object[]{target.getType().getDescriptionId(), items, xp});
   }

   private static int wornDrops(ServerLevel level, LivingEntity target) {
      List<ItemStack> worn = new ArrayList<>();
      worn.add(target.getMainHandItem());
      worn.add(target.getOffhandItem());

      for (ItemStack armor : target.getArmorSlots()) {
         worn.add(armor);
      }

      int count = 0;

      for (ItemStack stack : worn) {
         if (!stack.isEmpty()) {
            target.spawnAtLocation(stack.copy());
            count++;
         }
      }

      for (EquipmentSlot slot : EquipmentSlot.values()) {
         try {
            target.setItemSlot(slot, ItemStack.EMPTY);
         } catch (Throwable var9) {
         }
      }

      return count;
   }

   private static int lootingOf(@Nullable Player killer) {
      return killer == null ? 0 : EnchantmentHelper.getMobLooting(killer);
   }

   @Nullable
   private static Player killerOf(DamageSource source) {
      return source.getEntity() instanceof Player player ? player : null;
   }

   private static Field field(Class<?> owner, String official, String srg) {
      for (String name : new String[]{official, srg}) {
         try {
            Field found = owner.getDeclaredField(name);
            found.setAccessible(true);
            return found;
         } catch (ReflectiveOperationException var8) {
         }
      }

      return null;
   }

   private static Method method(Class<?> owner, String official, String srg, Class<?>... params) {
      for (String name : new String[]{official, srg}) {
         try {
            Method found = owner.getDeclaredMethod(name, params);
            found.setAccessible(true);
            return found;
         } catch (ReflectiveOperationException var9) {
         }
      }

      return null;
   }
}

