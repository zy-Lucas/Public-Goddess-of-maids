package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.myx00x.maid.ShenlinMod;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public final class GuardBook {
   private static final long SAMPLE_EVERY = 2L;
   private static final long NBT_EVERY = 20L;
   private static final long GRACE_TICKS = 40L;
   private static final int MAX_TRIES = 8;
   private static final int CAPACITY = 4096;
   private static final Map<UUID, GuardBook.Page> BOOK = new ConcurrentHashMap<>();
   private static long lastSample;
   private static long lastFull;

   private GuardBook() {
   }

   public static void tick(MinecraftServer server) {
      if (server != null) {
         ServerLevel level = server.overworld();
         if (level != null) {
            long now = level.getGameTime();
            if (now < lastSample) {
               lastSample = now;
               lastFull = now;
            }

            if (now >= lastSample + 2L) {
               lastSample = now;
               boolean full = now >= lastFull + 20L;
               if (full) {
                  lastFull = now;
               }

               sample(server, now, full);
               sweep(server, now);
            }
         }
      }
   }

   private static void sample(MinecraftServer server, long now, boolean full) {
      for (UUID id : new ArrayList<>(GuardWatch.watched())) {
         EntityMaid maid = GuardWatch.held(id);
         if (maid != null && maid.level() != null && !maid.level().isClientSide && !maid.isRemoved()) {
            int tier = GuardWatch.tierOf(maid);
            if (tier == 0) {
               if (BOOK.remove(id) != null) {
                  ShenlinMod.LOGGER.info("[shenlin] book page dropped, maid {} no longer guarded", id);
               }
            } else {
               GuardBook.Page page = BOOK.computeIfAbsent(id, key -> new GuardBook.Page());
               page.tier = tier;
               page.ownerId = maid.getOwnerUUID();
               if (maid.level() instanceof ServerLevel home) {
                  page.dim = home.dimension();
               }

               page.x = maid.getX();
               page.y = maid.getY();
               page.z = maid.getZ();
               page.yRot = maid.getYRot();
               page.xRot = maid.getXRot();
               page.updatedAt = now;
               page.expectLive = true;
               page.missingSince = -1L;
               page.tries = 0;
               if (full) {
                  CompoundTag fresh = MaidDataKeeper.snapshot(maid);
                  if (fresh != null && !fresh.isEmpty()) {
                     page.nbt = fresh;
                  }
               }
            }
         }
      }

      if (BOOK.size() > 4096) {
         BOOK.entrySet()
            .stream()
            .sorted((a, b) -> Long.compare(a.getValue().updatedAt, b.getValue().updatedAt))
            .limit(BOOK.size() - 4096)
            .map(Entry::getKey)
            .toList()
            .forEach(BOOK::remove);
      }
   }

   private static void sweep(MinecraftServer server, long now) {
      if (!BOOK.isEmpty()) {
         for (Entry<UUID, GuardBook.Page> entry : new ArrayList<>(BOOK.entrySet())) {
            UUID id = entry.getKey();
            GuardBook.Page page = entry.getValue();
            if (page.expectLive && page.nbt != null && !page.nbt.isEmpty() && page.tries < 8) {
               if (GuardRemoval.handover(id, now)) {
                  page.missingSince = -1L;
               } else if (foundAnywhere(server, id)) {
                  page.missingSince = -1L;
                  page.tries = 0;
               } else {
                  ServerLevel home = page.dim == null ? null : server.getLevel(page.dim);
                  if (home == null) {
                     page.missingSince = -1L;
                  } else if (!home.hasChunk(
                     (new ChunkPos(BlockPos.containing(page.x, page.y, page.z))).x, (new ChunkPos(BlockPos.containing(page.x, page.y, page.z))).z
                  )) {
                     page.missingSince = -1L;
                  } else if (page.missingSince < 0L) {
                     page.missingSince = now;
                  } else if (now - page.missingSince >= 40L) {
                     rebuild(home, id, page, now);
                  }
               }
            }
         }
      }
   }

   private static boolean foundAnywhere(MinecraftServer server, UUID id) {
      for (ServerLevel level : server.getAllLevels()) {
         if (level.getEntity(id) != null) {
            return true;
         }
      }

      return false;
   }

   private static void rebuild(ServerLevel level, UUID id, GuardBook.Page page, long now) {
      if (foundAnywhere(level.getServer(), id)) {
         page.missingSince = -1L;
         page.tries = 0;
      } else {
         Entity created;
         try {
            created = ((EntityType)InitEntities.MAID.get()).create(level);
         } catch (Throwable t) {
            page.tries++;
            warn(page, now, "create threw: " + t);
            return;
         }

         if (created instanceof EntityMaid fresh) {
            try {
               fresh.load(page.nbt.copy());
            } catch (Throwable t) {
               page.tries++;
               warn(page, now, "load failed: " + t);
               return;
            }

            fresh.setUUID(id);
            fresh.moveTo(page.x, page.y, page.z, page.yRot, page.xRot);
            MaidRecovery.sanitize(fresh);
            GuardVitals.reconnect(fresh);

            boolean placed;
            try {
               placed = level.addFreshEntity(fresh);
            } catch (Throwable t) {
               placed = false;
               ShenlinMod.LOGGER.warn("[shenlin] book addFreshEntity threw for {}: {}", id, t.toString());
            }

            if (!placed || !RosterRestore.tracked(level, fresh)) {
               boolean registerSaidOk = placed;
               if (RosterRestore.force(level, fresh, true)) {
                  placed = true;
                  ShenlinMod.LOGGER.info("[shenlin] book put maid {} back by table write (register={})", id, registerSaidOk ? "ok-but-untracked" : "refused");
               }
            }

            if (placed && !RosterRestore.tracked(level, fresh)) {
               RosterRestore.forceTracking(level, fresh);
            }

            if (!placed) {
               page.tries++;
               warn(page, now, "add and table write both failed");
            } else {
               GuardWatch.keep(fresh);
               GuardVitals.reconnect(fresh);
               page.tries = 0;
               page.missingSince = -1L;
               page.expectLive = true;
               page.updatedAt = now;
               ShenlinMod.LOGGER
                  .warn(
                     "[shenlin] book rebuilt maid {} at {} {} {} (tier {})",
                     new Object[]{id, level.dimension().location(), (int)page.x, (int)page.z, page.tier}
                  );
            }
         } else {
            page.tries++;
            warn(page, now, "create returned nothing");
         }
      }
   }

   private static void warn(GuardBook.Page page, long now, String why) {
      if (now - page.lastWarn >= 20L) {
         page.lastWarn = now;
         ShenlinMod.LOGGER.warn("[shenlin] book rebuild failed ({}/{}): {}", new Object[]{page.tries, 8, why});
      }
   }

   public static void expectGone(UUID id) {
      if (id != null) {
         GuardBook.Page page = BOOK.get(id);
         if (page != null) {
            page.expectLive = false;
            page.missingSince = -1L;
            page.tries = 0;
         }
      }
   }

   public static void forgetAll() {
      BOOK.clear();
      lastSample = 0L;
      lastFull = 0L;
   }

   public static String describe(UUID id) {
      GuardBook.Page page = id == null ? null : BOOK.get(id);
      return page == null
         ? "no page"
         : "tier="
            + page.tier
            + " expectLive="
            + page.expectLive
            + " updatedAt="
            + page.updatedAt
            + " missingSince="
            + page.missingSince
            + " tries="
            + page.tries
            + " nbt="
            + (page.nbt == null ? 0 : page.nbt.size())
            + " at "
            + (page.dim == null ? "?" : page.dim.location())
            + " "
            + (int)page.x
            + ","
            + (int)page.y
            + ","
            + (int)page.z;
   }

   private static final class Page {
      private CompoundTag nbt;
      private UUID ownerId;
      private ResourceKey<Level> dim;
      private double x;
      private double y;
      private double z;
      private float yRot;
      private float xRot;
      private int tier;
      private boolean expectLive;
      private long updatedAt;
      private long missingSince = -1L;
      private int tries;
      private long lastWarn;
   }
}

