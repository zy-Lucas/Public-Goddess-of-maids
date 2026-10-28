package com.myx00x.maid.boss;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class DamnedLedger extends SavedData {
   private static final String DATA_NAME = "shenlin_damned";
   private static final String TAG_LIST = "Players";
   private static final String TAG_ID = "Id";
   private final Set<UUID> players = new HashSet<>();

   private DamnedLedger() {
   }

   public static DamnedLedger get(MinecraftServer server) {
      return (DamnedLedger)server.overworld().getDataStorage().computeIfAbsent(DamnedLedger::load, DamnedLedger::new, "shenlin_damned");
   }

   public static boolean contains(MinecraftServer server, UUID id) {
      return id != null && get(server).players.contains(id);
   }

   public static void mark(MinecraftServer server, UUID id) {
      if (id != null) {
         DamnedLedger data = get(server);
         if (data.players.add(id)) {
            data.setDirty();
         }
      }
   }

   public static void clear(MinecraftServer server, UUID id) {
      if (id != null) {
         DamnedLedger data = get(server);
         if (data.players.remove(id)) {
            data.setDirty();
         }
      }
   }

   public static Set<UUID> all(MinecraftServer server) {
      return new HashSet<>(get(server).players);
   }

   private static DamnedLedger load(CompoundTag tag) {
      DamnedLedger data = new DamnedLedger();
      ListTag list = tag.getList("Players", 10);

      for (int i = 0; i < list.size(); i++) {
         CompoundTag entry = list.getCompound(i);

         try {
            data.players.add(UUID.fromString(entry.getString("Id")));
         } catch (Throwable var6) {
         }
      }

      return data;
   }

   public CompoundTag save(CompoundTag tag) {
      ListTag list = new ListTag();

      for (UUID id : this.players) {
         CompoundTag entry = new CompoundTag();
         entry.putString("Id", id.toString());
         list.add(entry);
      }

      tag.put("Players", list);
      return tag;
   }
}

