package com.myx00x.maid.block;

import com.myx00x.maid.registry.ModBlocks;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;

public final class RelicLocator {
   private RelicLocator() {
   }

   public static Optional<BlockPos> findCore(BlockGetter level, BlockPos center, int hRadius, int vRadius) {
      return level instanceof ServerLevel server
         ? findCoreLoaded(server, center, hRadius, vRadius)
         : BlockPos.findClosestMatch(center, hRadius, vRadius, pos -> level.getBlockState(pos).is((Block)ModBlocks.RELIC_CONTROL_CORE.get()));
   }

   private static Optional<BlockPos> findCoreLoaded(ServerLevel level, BlockPos center, int hRadius, int vRadius) {
      int cx0 = center.getX() - hRadius >> 4;
      int cx1 = center.getX() + hRadius >> 4;
      int cz0 = center.getZ() - hRadius >> 4;
      int cz1 = center.getZ() + hRadius >> 4;
      ServerChunkCache cache = level.getChunkSource();
      Set<Long> live = new HashSet<>();

      for (int cx = cx0; cx <= cx1; cx++) {
         for (int cz = cz0; cz <= cz1; cz++) {
            if (cache.getChunkNow(cx, cz) != null) {
               live.add(ChunkPos.asLong(cx, cz));
            }
         }
      }

      return live.isEmpty()
         ? Optional.empty()
         : BlockPos.findClosestMatch(
            center,
            hRadius,
            vRadius,
            pos -> live.contains(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4))
               && level.getBlockState(pos).is((Block)ModBlocks.RELIC_CONTROL_CORE.get())
         );
   }

   public static Optional<BlockPos> nearestCore(BlockGetter level, BlockPos center, int radius) {
      return findCore(level, center, radius, radius);
   }

   public static BlockPos relToWorld(BlockPos core, BlockPos rel) {
      return core.offset(rel.getX(), rel.getY(), rel.getZ());
   }

   public static BlockPos worldToRel(BlockPos core, BlockPos pos) {
      return new BlockPos(pos.getX() - core.getX(), pos.getY() - core.getY(), pos.getZ() - core.getZ());
   }
}

