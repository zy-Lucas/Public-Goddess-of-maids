package com.myx00x.maid.world;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class WardenVitalsData extends SavedData {
   public static final String NAME = "goddess_of_maids_warden_vitals";
   private static final String TAG_KEY = "Key";
   private int coreKey = coreKeyDefault();
   private final Map<UUID, WardenVitalsData.Row> table = new ConcurrentHashMap<>();

   private static int coreKeyDefault() {
      int key = new Random().nextInt();
      return key == 0 ? 1010603715 : key;
   }

   private int seal(float value) {
      return Float.floatToRawIntBits(value) ^ this.coreKey;
   }

   private float unseal(int sealed) {
      return Float.intBitsToFloat(sealed ^ this.coreKey);
   }

   private static int keyOf(WardenVitalsData data) {
      return data.coreKey;
   }

   public static WardenVitalsData get(ServerLevel level) {
      return (WardenVitalsData)level.getDataStorage().computeIfAbsent(WardenVitalsData::load, WardenVitalsData::new, "goddess_of_maids_warden_vitals");
   }

   public static WardenVitalsData load(CompoundTag tag) {
      WardenVitalsData data = new WardenVitalsData();
      if (tag.contains("Key")) {
         int stored = tag.getInt("Key");
         if (stored != 0) {
            data.coreKey = stored;
         }
      }

      CompoundTag all = tag.getCompound("Vitals");
      int sealKey = data.coreKey;

      for (String key : all.getAllKeys()) {
         try {
            UUID id = UUID.fromString(key);
            if (all.get(key) instanceof CompoundTag row) {
               WardenVitalsData.Row one = row.contains("CoreMasked")
                  ? new WardenVitalsData.Row(row.getInt("CoreMasked"), sealKey, row.getBoolean("Battle"))
                  : new WardenVitalsData.Row(row.getFloat("Core"), sealKey, row.getBoolean("Battle"));
               if (row.contains("Boom")) {
                  one.boom = row.getBoolean("Boom");
               }

               if (row.contains("X") && row.contains("Y") && row.contains("Z")) {
                  one.hasPos = true;
                  one.x = row.getDouble("X");
                  one.y = row.getDouble("Y");
                  one.z = row.getDouble("Z");
               }

               data.table.put(id, one);
            } else {
               data.table.put(id, new WardenVitalsData.Row(all.getFloat(key), sealKey, false));
            }
         } catch (IllegalArgumentException var9) {
         }
      }

      return data;
   }

   public CompoundTag save(CompoundTag tag) {
      CompoundTag all = new CompoundTag();
      this.table.forEach((id, row) -> {
         CompoundTag one = new CompoundTag();
         one.putInt("CoreMasked", row.core);
         one.putBoolean("Battle", row.battle);
         one.putBoolean("Boom", row.boom);
         if (row.hasPos) {
            one.putDouble("X", row.x);
            one.putDouble("Y", row.y);
            one.putDouble("Z", row.z);
         }

         all.put(id.toString(), one);
      });
      tag.put("Vitals", all);
      tag.putInt("Key", this.coreKey);
      return tag;
   }

   public WardenVitalsData.Row find(UUID id) {
      return this.table.get(id);
   }

   public void put(UUID id, float value) {
      WardenVitalsData.Row row = this.table.get(id);
      if (row == null) {
         if (!(value <= 0.0F)) {
            this.table.put(id, new WardenVitalsData.Row(value, this.coreKey, false));
            this.setDirty();
         }
      } else {
         if (Math.abs(row.core() - value) > 1.0E-4F) {
            row.setCore(value);
            this.setDirty();
         }
      }
   }

   public void putBattle(UUID id, boolean fought) {
      WardenVitalsData.Row row = this.table.get(id);
      if (row != null) {
         if (row.battle != fought) {
            row.battle = fought;
            this.setDirty();
         }
      }
   }

   public WardenVitalsData.Row markPos(UUID id, double x, double y, double z) {
      WardenVitalsData.Row row = this.table.get(id);
      if (row == null) {
         return null;
      }

      boolean first = !row.hasPos;
      double dx = row.x - x;
      double dy = row.y - y;
      double dz = row.z - z;
      if (first || dx * dx + dy * dy + dz * dz > 1.0E-4) {
         row.hasPos = true;
         row.x = x;
         row.y = y;
         row.z = z;
         this.setDirty();
      }

      return row;
   }

   public void voidPos(UUID id) {
      WardenVitalsData.Row row = this.table.get(id);
      if (row != null && row.hasPos) {
         row.hasPos = false;
         this.setDirty();
      }
   }

   public void putPos(UUID id, double x, double y, double z) {
      WardenVitalsData.Row row = this.table.get(id);
      if (row != null) {
         row.hasPos = true;
         row.x = x;
         row.y = y;
         row.z = z;
         this.setDirty();
      }
   }

   public void putBoom(UUID id, boolean boom) {
      WardenVitalsData.Row row = this.table.get(id);
      if (row != null && row.boom != boom) {
         row.boom = boom;
         this.setDirty();
      }
   }

   public Map<UUID, WardenVitalsData.Row> rows() {
      return new HashMap<>(this.table);
   }

   public void forget(UUID id) {
      if (this.table.remove(id) != null) {
         this.setDirty();
      }
   }

   public static final class Row {
      private int core;
      private boolean battle;
      private boolean hasPos;
      private double x;
      private double y;
      private double z;
      private boolean boom = true;
      private final int key;

      public Row(float core, int key, boolean battle) {
         this.key = key;
         this.core = Float.floatToRawIntBits(core) ^ key;
         this.battle = battle;
      }

      private Row(int sealedCore, int key, boolean battle) {
         this.key = key;
         this.core = sealedCore;
         this.battle = battle;
      }

      private void setCore(float value) {
         this.core = Float.floatToRawIntBits(value) ^ this.key;
      }

      public boolean boom() {
         return this.boom;
      }

      public float core() {
         return Float.intBitsToFloat(this.core ^ this.key);
      }

      public boolean battle() {
         return this.battle;
      }

      public boolean hasPos() {
         return this.hasPos;
      }

      public double x() {
         return this.x;
      }

      public double y() {
         return this.y;
      }

      public double z() {
         return this.z;
      }
   }
}

