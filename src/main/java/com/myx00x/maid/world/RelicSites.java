package com.myx00x.maid.world;

import com.myx00x.maid.ShenlinMod;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;

public class RelicSites extends SavedData {
   private static final String NAME = "shenlin_relic_sites";
   private static final String TAG_KNOWN = "Known";
   private static final String TAG_EXPLORED = "Explored";
   private static final String TAG_CLAIMED = "Claimed";
   private static final String TAG_CLAIMED_XZ = "ClaimedXZ";
   private final Set<BlockPos> known = new HashSet<>();
   private final Set<BlockPos> explored = new HashSet<>();
   private final Set<BlockPos> claimed = new HashSet<>();
   private final Set<Long> claimedXZ = new HashSet<>();
   private static final Queue<RelicSites.Pending> PENDING = new ConcurrentLinkedQueue<>();

   public static RelicSites get(ServerLevel level) {
      return (RelicSites)level.getDataStorage().computeIfAbsent(RelicSites::load, RelicSites::new, "shenlin_relic_sites");
   }

   public static RelicSites load(CompoundTag tag) {
      RelicSites sites = new RelicSites();

      for (long packed : tag.getLongArray("Known")) {
         sites.known.add(BlockPos.of(packed));
      }

      for (long packed : tag.getLongArray("Explored")) {
         sites.explored.add(BlockPos.of(packed));
      }

      for (long packed : tag.getLongArray("Claimed")) {
         sites.claimed.add(BlockPos.of(packed));
      }

      for (long packed : tag.getLongArray("ClaimedXZ")) {
         sites.claimedXZ.add(packed);
      }

      for (BlockPos core : sites.claimed) {
         sites.claimedXZ.add(packXZ(core.getX(), core.getZ()));
      }

      return sites;
   }

   public CompoundTag save(CompoundTag tag) {
      tag.putLongArray("Known", pack(this.known));
      tag.putLongArray("Explored", pack(this.explored));
      tag.putLongArray("Claimed", pack(this.claimed));
      long[] xz = new long[this.claimedXZ.size()];
      int at = 0;

      for (long packed : this.claimedXZ) {
         xz[at++] = packed;
      }

      tag.putLongArray("ClaimedXZ", xz);
      return tag;
   }

   private static long[] pack(Set<BlockPos> set) {
      long[] out = new long[set.size()];
      int i = 0;

      for (BlockPos pos : set) {
         out[i++] = pos.asLong();
      }

      return out;
   }

   public static void noteGenerated(ServerLevel level, BlockPos core) {
      PENDING.add(new RelicSites.Pending(level.getServer(), level.dimension(), core.immutable()));
   }

   public static void drain(MinecraftServer server) {
      RelicSites.Pending pending;
      while ((pending = PENDING.poll()) != null) {
         if (pending.server() != server) {
            ShenlinMod.LOGGER.debug("[shenlin] relic site from another server dropped: {}", pending.core());
         } else {
            ServerLevel level = server.getLevel(pending.dimension());
            if (level != null) {
               get(level).markKnown(pending.core());
            }
         }
      }
   }

   public void markKnown(BlockPos core) {
      if (this.known.add(core)) {
         this.setDirty();
      }
   }

   public void markExplored(BlockPos core) {
      if (this.explored.add(core)) {
         this.setDirty();
      }
   }

   public static int resetClaimed(ServerLevel level) {
      RelicSites sites = get(level);
      int freed = sites.claimed.size();
      if (freed > 0) {
         sites.claimed.clear();
         sites.claimedXZ.clear();
         sites.setDirty();
      }

      return freed;
   }

   public void markClaimed(BlockPos core) {
      boolean hit = this.claimed.add(core);
      hit |= this.claimedXZ.add(packXZ(core.getX(), core.getZ()));
      if (hit) {
         this.setDirty();
      }
   }

   public boolean isClaimedColumn(int x, int z) {
      return this.claimedXZ.contains(packXZ(x, z));
   }

   private static long packXZ(int x, int z) {
      return (long)x << 32 | z & 4294967295L;
   }

   public void forget(BlockPos core) {
      boolean hit = this.known.remove(core);
      hit |= this.explored.remove(core);
      hit |= this.claimed.remove(core);
      hit |= this.claimedXZ.remove(packXZ(core.getX(), core.getZ()));
      if (hit) {
         this.setDirty();
      }
   }

   public boolean isExplored(BlockPos core) {
      return this.explored.contains(core);
   }

   public boolean isClaimed(BlockPos core) {
      return this.claimed.contains(core);
   }

   public Set<BlockPos> unexplored() {
      Set<BlockPos> out = new HashSet<>(this.known);
      out.removeAll(this.claimed);
      return out;
   }

   static BlockPos coreAt(ServerLevel level, ChunkPos chunk, Structure structure) {
      ChunkAccess access = level.getChunk(chunk.x, chunk.z, ChunkStatus.STRUCTURE_STARTS);
      StructureStart start = level.structureManager().getStartForStructure(SectionPos.of(new BlockPos(chunk.getMinBlockX(), 0, chunk.getMinBlockZ())), structure, access);
      if (start != null && start.isValid()) {
         for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof MossyRelicPiece relic) {
               return relic.corePos();
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private record Pending(MinecraftServer server, ResourceKey<Level> dimension, BlockPos core) {
   }
}

