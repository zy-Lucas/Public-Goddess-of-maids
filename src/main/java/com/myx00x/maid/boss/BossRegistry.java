package com.myx00x.maid.boss;

import com.myx00x.maid.ShenlinMod;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

public final class BossRegistry extends SavedData {
   private static final String DATA_NAME = "shenlin_boss_guards";
   private static final String TAG_LIST = "Bosses";
   private static final String TAG_DIM = "Dim";
   private static final String TAG_POS = "Pos";
   private static final String TAG_GUARDED = "Guarded";
   private static final String TAG_REASON = "Reason";
   private final Map<String, BossRegistry.Entry> entries = new LinkedHashMap<>();

   private BossRegistry() {
   }

   public static BossRegistry get(MinecraftServer server) {
      return (BossRegistry)server.overworld().getDataStorage().computeIfAbsent(BossRegistry::load, BossRegistry::new, "shenlin_boss_guards");
   }

   public static void enroll(ServerLevel level, BlockPos pos) {
      MinecraftServer server = level.getServer();
      if (server != null) {
         get(server).put(new BossRegistry.Entry(level.dimension().location().toString(), pos.asLong(), true, ""));
      }
   }

   public static void release(MinecraftServer server, ResourceKey<Level> dimension, BlockPos pos, String reason) {
      if (server != null) {
         BossRegistry data = get(server);
         String key = key(dimension.location().toString(), pos);
         BossRegistry.Entry old = data.entries.get(key);
         if (old == null || old.guarded()) {
            data.put(new BossRegistry.Entry(dimension.location().toString(), pos.asLong(), false, reason));
         }
      }
   }

   public static void release(Entity entity, String reason) {
      if (entity instanceof ShenlinBossEntity && entity.level() instanceof ServerLevel level) {
         MinecraftServer server = level.getServer();
         if (server != null) {
            ShenlinBoss boss = ShenlinBossKeeper.of(entity);
            BlockPos at = boss != null ? BlockPos.containing(boss.x(), boss.y(), boss.z()) : entity.blockPosition();
            release(server, level.dimension(), at, reason);
         }
      }
   }

   public static boolean guarded(MinecraftServer server, ResourceKey<Level> dimension, BlockPos pos) {
      if (server == null) {
         return true;
      }

      BossRegistry.Entry entry = get(server).entries.get(key(dimension.location().toString(), pos));
      return entry == null || entry.guarded();
   }

   public static List<BossRegistry.Entry> list(MinecraftServer server) {
      return new ArrayList<>(get(server).entries.values());
   }

   public static void forgetAll(MinecraftServer server) {
      if (server != null) {
         BossRegistry data = get(server);
         if (!data.entries.isEmpty()) {
            data.entries.clear();
            data.setDirty();
         }
      }
   }

   public static void forget(MinecraftServer server, ResourceKey<Level> dimension, BlockPos pos) {
      if (server != null) {
         BossRegistry data = get(server);
         if (data.entries.remove(key(dimension.location().toString(), pos)) != null) {
            data.setDirty();
         }
      }
   }

   private void put(BossRegistry.Entry entry) {
      this.entries.put(key(entry.dimension(), entry.blockPos()), entry);
      this.setDirty();
   }

   private static String key(String dimension, BlockPos pos) {
      return key(dimension, pos.asLong());
   }

   private static String key(String dimension, long packed) {
      return dimension + "@" + packed;
   }

   public static BossRegistry load(CompoundTag tag) {
      BossRegistry data = new BossRegistry();
      ListTag list = tag.getList("Bosses", 10);

      for (int i = 0; i < list.size(); i++) {
         CompoundTag one = list.getCompound(i);
         String dim = one.getString("Dim");
         long pos = one.getLong("Pos");
         if (!dim.isEmpty()) {
            data.entries.put(key(dim, pos), new BossRegistry.Entry(dim, pos, one.getBoolean("Guarded"), one.getString("Reason")));
         }
      }

      if (!data.entries.isEmpty()) {
         ShenlinMod.LOGGER.info("[shenlin] boss guards loaded: {}", data.entries.size());
      }

      return data;
   }

   public CompoundTag save(CompoundTag tag) {
      ListTag list = new ListTag();

      for (BossRegistry.Entry entry : this.entries.values()) {
         CompoundTag one = new CompoundTag();
         one.putString("Dim", entry.dimension());
         one.putLong("Pos", entry.pos());
         one.putBoolean("Guarded", entry.guarded());
         one.putString("Reason", entry.reason());
         list.add(one);
      }

      tag.put("Bosses", list);
      return tag;
   }

   public record Entry(String dimension, long pos, boolean guarded, String reason) {
      public BlockPos blockPos() {
         return BlockPos.of(this.pos);
      }
   }
}

