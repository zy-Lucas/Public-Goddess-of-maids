package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.combat.health.UltraHealth;
import com.myx00x.maid.item.FinalSlashItem;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.DataItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class Executioner {
   private static final float HEALTH_TOLERANCE = 0.01F;
   private static final float LETHAL = Float.NEGATIVE_INFINITY;
   private static final String[] HEALTH_FIELD_NAMES = new String[]{"DATA_HEALTH_ID", "f_20961_"};
   private static final String[] ITEM_VALUE_NAMES = new String[]{"value", "f_135391_"};
   private static final String[] ITEM_DIRTY_NAMES = new String[]{"dirty", "f_135392_"};
   private static final String[] DATA_DIRTY_NAMES = new String[]{"isDirty", "f_135348_"};
   private static final String[] NOTIFY_NAMES = new String[]{"onSyncedDataUpdated", "m_7350_"};
   private static final Map<Class<?>, Method> NOTIFY_CACHE = new ConcurrentHashMap<>();
   private static Field itemsByIdField;
   private static Field itemValueField;
   private static Field itemDirtyField;
   private static Field dataDirtyField;
   private static boolean itemFieldsResolved;
   private static EntityDataAccessor<Float> healthAccessor;
   private static boolean healthResolved;
   private static long lastMissedNote;

   private Executioner() {
   }

   public static void executeByOwner(EntityMaid maid, LivingEntity target, String reason) {
      if (!target.isRemoved() && !(target instanceof Player) && !(target instanceof EntityMaid)) {
         if (SelfManagedHealth.is(target)) {
            ShenlinMod.LOGGER.info("[shenlin] 斩杀跳过：{} 是自管血量的 boss", target.getType().getDescriptionId());
         } else {
            int swings = ExecutionCounter.swings(maid, target);
            ExecutionCounter.clearTarget(maid.getUUID(), target.getUUID());
            boolean erase = (Boolean)Config.FINAL_SLASH_ERASE_ALLOWED.get() && FinalSlashItem.eraseOn(maid);
            ExecutionSequencer.begin(maid, target, reason, erase, swings);
         }
      }
   }

   public static boolean maySilenceHealth(LivingEntity target) {
      if (target instanceof EntityMaid) {
         return false;
      } else {
         return !(target instanceof Player player) ? !target.isSpectator() : !player.isCreative() && !player.isSpectator();
      }
   }

   public static boolean setHealthSilently(LivingEntity target, float newHealth) {
      if (!maySilenceHealth(target)) {
         return false;
      }

      float value = newHealth <= 0.0F ? Float.NEGATIVE_INFINITY : newHealth;
      boolean lethal = value <= 0.0F;
      if (writeSynced(target, value) && reached(target, value)) {
         return true;
      }

      if (!byVanillaSetHealth(target, value) || lethal && !reached(target, value)) {
         if (UltraHealth.setHealth(target, value, 2)) {
            return true;
         }

         if (BloodLedger.write(target, value)) {
            return true;
         }

         if (tryNbt(target, value)) {
            return true;
         }

         DeepHealthScan.request(target, value);
         noteMissed(target, value);
         return false;
      } else {
         return true;
      }
   }

   private static void noteMissed(LivingEntity target, float value) {
      long now = System.currentTimeMillis();
      if (now - lastMissedNote >= 4000L) {
         lastMissedNote = now;
         ShenlinMod.LOGGER.warn("[shenlin] silent health write missed {}: want {} now {}", new Object[]{target.getType(), value, target.getHealth()});
      }
   }

   public static boolean putSkin(LivingEntity target, float value) {
      if (same(target.getHealth(), value)) {
         return true;
      } else {
         return byVanillaSetHealth(target, value) ? true : writeSynced(target, value) && reached(target, value);
      }
   }

   public static boolean reached(LivingEntity target, float expected) {
      float now = target.getHealth();
      return expected <= 0.0F ? now <= 0.0F : same(now, expected);
   }

   private static boolean byVanillaSetHealth(LivingEntity target, float value) {
      float before = target.getHealth();

      try {
         target.setHealth(value);
      } catch (Throwable t) {
         return false;
      }

      return !same(target.getHealth(), before);
   }

   public static float syncedHealth(LivingEntity target) {
      try {
         Map<Integer, ?> items = getItemsById(target.getEntityData());
         EntityDataAccessor<Float> health = resolveHealthAccessor();
         if (items != null && health != null) {
            for (Object obj : items.values()) {
               if (obj instanceof DataItem item && item.getAccessor() == health && item.getValue() instanceof Float f) {
                  return f;
               }
            }
         }
      } catch (Throwable var8) {
      }

      return Float.NEGATIVE_INFINITY;
   }

   public static float readHealthRaw(LivingEntity target) {
      float shown = target.getHealth();
      float raw = syncedHealth(target);
      if (raw <= 0.0F) {
         return shown;
      } else {
         return shown > 0.0F ? Math.min(raw, shown) : raw;
      }
   }

   public static float readHealthForRatio(LivingEntity target) {
      float shown = target.getHealth();
      float raw = syncedHealth(target);
      return shown <= 0.0F ? raw : (raw <= 0.0F ? shown : Math.max(shown, raw));
   }

   public static boolean writeSyncedRaw(LivingEntity target, float newHealth) {
      return writeSynced(target, newHealth);
   }

   private static boolean writeSynced(LivingEntity target, float newHealth) {
      try {
         SynchedEntityData data = target.getEntityData();
         Map<Integer, ?> items = getItemsById(data);
         if (items != null && !items.isEmpty()) {
            float probe = target.getHealth();
            boolean byValue = probe != 0.0F;
            EntityDataAccessor<Float> health = resolveHealthAccessor();
            boolean wrote = false;
            if (health != null
               && items.get(health.getId()) instanceof DataItem item
               && item.getValue() instanceof Float f
               && writeItem(data, item, newHealth)) {
               if (f != newHealth) {
                  notifyChanged(target, item.getAccessor());
               }

               wrote = true;
            }

            for (Object obj : items.values()) {
               if (obj instanceof DataItem item && item.getValue() instanceof Float f) {
                  boolean hit = health != null && item.getAccessor() == health || byValue && same(f, probe);
                  if (hit && writeItem(data, item, newHealth)) {
                     wrote = true;
                     if (f != newHealth) {
                        notifyChanged(target, item.getAccessor());
                     }
                  }
               }
            }

            return wrote;
         } else {
            return false;
         }
      } catch (Throwable t) {
         return false;
      }
   }

   private static void notifyChanged(LivingEntity target, Object accessor) {
      try {
         Method m = notifyMethod(target.getClass());
         if (m != null) {
            m.invoke(target, accessor);
         }
      } catch (Throwable var3) {
      }
   }

   private static Method notifyMethod(Class<?> type) {
      Method cached = NOTIFY_CACHE.get(type);
      if (cached != null) {
         return cached;
      }

      for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
         for (String name : NOTIFY_NAMES) {
            try {
               Method m = c.getDeclaredMethod(name, EntityDataAccessor.class);
               m.setAccessible(true);
               NOTIFY_CACHE.put(type, m);
               return m;
            } catch (Throwable var8) {
            }
         }
      }

      return null;
   }

   private static boolean writeItem(SynchedEntityData data, DataItem<?> item, float value) {
      if (!resolveItemFields()) {
         return false;
      }

      try {
         if (itemValueField.get(item) instanceof Float f && f == value) {
            return true;
         } else {
            itemValueField.set(item, value);
            itemDirtyField.setBoolean(item, true);
            dataDirtyField.setBoolean(data, true);
            return true;
         }
      } catch (Throwable t) {
         return false;
      }
   }

   private static boolean resolveItemFields() {
      if (itemFieldsResolved) {
         return itemValueField != null && itemDirtyField != null && dataDirtyField != null;
      }

      itemFieldsResolved = true;
      itemValueField = resolveValueField();
      itemDirtyField = resolveField(DataItem.class, ITEM_DIRTY_NAMES);
      dataDirtyField = resolveField(SynchedEntityData.class, DATA_DIRTY_NAMES);
      return itemValueField != null && itemDirtyField != null && dataDirtyField != null;
   }

   private static Field resolveValueField() {
      Class<?> type = DataItem.class;

      for (String name : ITEM_VALUE_NAMES) {
         try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field;
         } catch (Throwable var6) {
         }
      }

      for (Field field : type.getDeclaredFields()) {
         int mod = field.getModifiers();
         if (!Modifier.isStatic(mod) && !Modifier.isFinal(mod) && field.getType() == Object.class) {
            field.setAccessible(true);
            return field;
         }
      }

      return null;
   }

   private static Field resolveField(Class<?> owner, String[] names) {
      for (String name : names) {
         try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
         } catch (Throwable var7) {
         }
      }

      return null;
   }

   private static EntityDataAccessor<Float> resolveHealthAccessor() {
      if (healthResolved) {
         return healthAccessor;
      }

      healthResolved = true;

      for (Class<?> c = LivingEntity.class; c != null && c != Object.class; c = c.getSuperclass()) {
         for (Field field : c.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && EntityDataAccessor.class.isAssignableFrom(field.getType())) {
               String key = field.getName().toUpperCase().replace("_", "");
               if (key.contains("HEALTH") && !key.contains("MAXHEALTH")) {
                  try {
                     field.setAccessible(true);
                     healthAccessor = (EntityDataAccessor<Float>)field.get(null);
                  } catch (Throwable var7) {
                  }

                  if (healthAccessor != null) {
                     return healthAccessor;
                  }
               }
            }
         }
      }

      for (String name : HEALTH_FIELD_NAMES) {
         try {
            Field field = LivingEntity.class.getDeclaredField(name);
            field.setAccessible(true);
            healthAccessor = (EntityDataAccessor<Float>)field.get(null);
            break;
         } catch (Throwable var8) {
         }
      }

      return healthAccessor;
   }

   public static float rawHealth(LivingEntity target) {
      EntityDataAccessor<Float> accessor = resolveHealthAccessor();
      if (accessor == null) {
         return target.getHealth();
      }

      try {
         return (Float)target.getEntityData().get(accessor);
      } catch (Throwable t) {
         return target.getHealth();
      }
   }

   private static Map<Integer, ?> getItemsById(SynchedEntityData data) throws IllegalAccessException {
      Field field = itemsByIdField;
      if (field == null) {
         for (Field f : SynchedEntityData.class.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) && Int2ObjectMap.class.isAssignableFrom(f.getType())) {
               f.setAccessible(true);
               itemsByIdField = f;
               field = f;
               break;
            }
         }

         if (field == null) {
            return null;
         }
      }

      return (Map<Integer, ?>)field.get(data);
   }

   private static boolean same(float a, float b) {
      return a == b || Math.abs(a - b) <= 0.01F;
   }

   private static boolean tryNbt(LivingEntity target, float newHealth) {
      try {
         CompoundTag nbt = target.saveWithoutId(new CompoundTag());
         boolean modified = false;
         if (nbt.contains("Health")) {
            nbt.putFloat("Health", newHealth);
            modified = true;
         } else if (replaceMatchingFloat(nbt, target.getHealth(), newHealth)) {
            modified = true;
         }

         if (!modified) {
            return false;
         }

         target.load(nbt);
         return same(readHealthRaw(target), newHealth);
      } catch (Throwable t) {
         return false;
      }
   }

   private static boolean replaceMatchingFloat(CompoundTag nbt, float expected, float newHealth) {
      boolean modified = false;

      for (String key : nbt.getAllKeys()) {
         if (nbt.get(key) instanceof FloatTag tag && same(tag.getAsFloat(), expected)) {
            nbt.putFloat(key, newHealth);
            modified = true;
         } else if (nbt.get(key) instanceof CompoundTag inner && replaceMatchingFloat(inner, expected, newHealth)) {
            modified = true;
         }
      }

      return modified;
   }
}

