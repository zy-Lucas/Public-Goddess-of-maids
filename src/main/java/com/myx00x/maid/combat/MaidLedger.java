package com.myx00x.maid.combat;

import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

public final class MaidLedger {
   private static final String FOLDER = "goddess_of_maids";
   private static final String FILENAME = "warded.nbt";
   private static final int FORMAT = 1;
   private static final long SAVE_INTERVAL_TICKS = 100L;
   private static final Map<UUID, MaidLedger.Mark> MARKS = new ConcurrentHashMap<>();
   private static MinecraftServer bound;
   private static File ledgerFile;
   private static boolean dirty;
   private static long lastSave;

   private MaidLedger() {
   }

   public static void tick(MinecraftServer server) {
      if (server != null && (Boolean)Config.SHIELD_LEDGER.get()) {
         if (bound != server) {
            bind(server);
         } else if (dirty) {
            long now = now(server);
            if (now >= 0L && now - lastSave >= 100L) {
               save(now);
            }
         }
      }
   }

   public static void dirty() {
      dirty = true;
   }

   public static void noteClear(UUID maidId, String how) {
      if ((Boolean)Config.SHIELD_LEDGER.get()) {
         MaidLedger.Mark mark = MARKS.computeIfAbsent(maidId, k -> new MaidLedger.Mark());
         mark.clears++;
         mark.how = how;
         dirty = true;
      }
   }

   public static void markMissing(UUID maidId) {
      if ((Boolean)Config.SHIELD_LEDGER.get()) {
         MARKS.computeIfAbsent(maidId, k -> new MaidLedger.Mark()).missing = true;
         dirty = true;
         flush();
      }
   }

   public static void markLive(UUID maidId) {
      if ((Boolean)Config.SHIELD_LEDGER.get()) {
         MaidLedger.Mark mark = MARKS.get(maidId);
         if (mark != null && mark.missing) {
            mark.missing = false;
            dirty = true;
            flush();
         }
      }
   }

   public static void forget(UUID maidId) {
      if ((Boolean)Config.SHIELD_LEDGER.get()) {
         if (MARKS.remove(maidId) != null || dirty) {
            dirty = true;
            flush();
         }
      }
   }

   private static void bind(MinecraftServer server) {
      bound = server;
      ledgerFile = null;

      try {
         File folder = new File(server.getWorldPath(LevelResource.ROOT).normalize().toFile(), "goddess_of_maids");
         if (!folder.isDirectory() && !folder.mkdirs()) {
            ShenlinMod.LOGGER.warn("[shenlin] ledger folder unwritable: {}", folder);
            return;
         }

         ledgerFile = new File(folder, "warded.nbt");
      } catch (Throwable t) {
         ShenlinMod.LOGGER.warn("[shenlin] ledger unavailable: {}", t.getMessage());
         return;
      }

      load();
   }

   private static void load() {
      if (ledgerFile != null && ledgerFile.isFile()) {
         CompoundTag root;
         try {
            root = NbtIo.readCompressed(ledgerFile);
         } catch (Throwable t) {
            ShenlinMod.LOGGER.warn("[shenlin] ledger unreadable, starting fresh: {}", t.getMessage());
            return;
         }

         if (root != null) {
            long now = now(bound);
            ListTag list = root.getList("maids", 10);
            int warded = 0;
            int pending = 0;

            for (int i = 0; i < list.size(); i++) {
               CompoundTag entry = list.getCompound(i);
               if (entry.hasUUID("uuid")) {
                  UUID maidId = entry.getUUID("uuid");
                  ShieldWatcher.Snapshot snapshot = ShieldWatcher.snapshotFromTag(entry);
                  if (snapshot != null) {
                     ShieldWatcher.adopt(snapshot);
                     MaidLedger.Mark mark = new MaidLedger.Mark();
                     mark.clears = entry.getInt("clears");
                     mark.how = entry.getString("how");
                     mark.missing = entry.getBoolean("missing");
                     MARKS.put(maidId, mark);
                     warded++;
                     if (mark.missing && !snapshot.stored() && now >= 0L) {
                        ShieldWatcher.markPendingRestore(maidId, now);
                        pending++;
                     }
                  }
               }
            }

            dirty = false;
            lastSave = now;
            ShenlinMod.LOGGER.info("[shenlin] ledger loaded: {} warded, {} pending", warded, pending);
         }
      }
   }

   private static void save(long now) {
      if (ledgerFile == null) {
         dirty = false;
      } else {
         ListTag list = ShieldWatcher.dumpAll();

         for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.hasUUID("uuid")) {
               MaidLedger.Mark mark = MARKS.get(entry.getUUID("uuid"));
               if (mark != null) {
                  entry.putInt("clears", mark.clears);
                  entry.putString("how", mark.how);
                  entry.putBoolean("missing", mark.missing);
               }
            }
         }

         CompoundTag root = new CompoundTag();
         root.putInt("format", 1);
         root.put("maids", list);
         File tmp = new File(ledgerFile.getParentFile(), "warded.nbt.tmp");

         try {
            NbtIo.writeCompressed(root, tmp);
            if (ledgerFile.exists() && !ledgerFile.delete()) {
               ShenlinMod.LOGGER.warn("[shenlin] ledger not replaceable");
            }

            if (!tmp.renameTo(ledgerFile)) {
               ShenlinMod.LOGGER.warn("[shenlin] ledger rename failed");
            }
         } catch (Throwable t) {
            ShenlinMod.LOGGER.warn("[shenlin] ledger write failed: {}", t.getMessage());
         }

         dirty = false;
         lastSave = now;
      }
   }

   public static void flushNow() {
      if ((Boolean)Config.SHIELD_LEDGER.get()) {
         flush();
      }
   }

   private static void flush() {
      if (bound != null && ledgerFile != null) {
         save(now(bound));
      }
   }

   private static long now(MinecraftServer server) {
      return server != null && server.overworld() != null ? server.overworld().getGameTime() : -1L;
   }

   private static final class Mark {
      private int clears;
      private String how = "";
      private boolean missing;
   }
}

