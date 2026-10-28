package com.myx00x.maid.world;

import com.mojang.serialization.Codec;
import com.myx00x.maid.registry.ModStructures;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationStub;
import net.minecraft.world.level.levelgen.structure.Structure.StructureSettings;

public class MossyRelicStructure extends Structure {
   public static final Codec<MossyRelicStructure> CODEC = simpleCodec(MossyRelicStructure::new);
   private static final int SIZE_X = 129;
   private static final int SIZE_Z = 57;
   private static final int ORIGIN_DX = -40;
   private static final int ORIGIN_DZ = -12;
   private static final int MIN_SURFACE = 62;
   private static final int MAX_SPREAD = 80;
   private static final int LIFT = 3;
   private static final int ENTRY_SPAN_X = 4;
   private static final int ENTRY_SPAN_Z = 5;

   public static int originDx() {
      return -40;
   }

   public static int originDz() {
      return -12;
   }

   public MossyRelicStructure(StructureSettings settings) {
      super(settings);
   }

   protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
      ChunkPos chunkPos = context.chunkPos();
      ChunkGenerator generator = context.chunkGenerator();
      int originX = chunkPos.getMinBlockX() + -40;
      int originZ = chunkPos.getMinBlockZ() + -12;
      int halfX = 64;
      int halfZ = 28;
      int cx = originX + halfX;
      int cz = originZ + halfZ;
      int lowest = Integer.MAX_VALUE;
      int highest = Integer.MIN_VALUE;

      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            int h = generator.getBaseHeight(cx + dx * halfX, cz + dz * halfZ, Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
            lowest = Math.min(lowest, h);
            highest = Math.max(highest, h);
         }
      }

      int ex0 = originX + 121;
      int ex1 = originX + 128;
      int ez0 = originZ + 11;
      int ez1 = originZ + 20;
      int entryTop = Integer.MIN_VALUE;

      for (int i = 0; i <= 4; i++) {
         int x = ex0 + (ex1 - ex0) * i / 4;

         for (int j = 0; j <= 5; j++) {
            int z = ez0 + (ez1 - ez0) * j / 5;
            int h = generator.getBaseHeight(x, z, Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
            entryTop = Math.max(entryTop, h);
         }
      }

      highest = Math.max(highest, entryTop);
      if (lowest >= 62 && highest - lowest <= 80) {
         int baseY = entryTop + 3 - 32;
         BlockPos origin = new BlockPos(originX, baseY, originZ);
         return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new MossyRelicPiece(context.structureTemplateManager(), origin, Rotation.NONE))));
      } else {
         return Optional.empty();
      }
   }

   public StructureType<?> type() {
      return (StructureType<?>)ModStructures.MOSSY_RELIC.get();
   }
}

