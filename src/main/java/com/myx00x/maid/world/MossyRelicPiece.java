package com.myx00x.maid.world;

import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.mob.MossyPuppetEntity;
import com.myx00x.maid.mob.RelicWardenEntity;
import com.myx00x.maid.registry.ModEntities;
import com.myx00x.maid.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class MossyRelicPiece extends StructurePiece {
   public static final ResourceLocation TEMPLATE = ResourceLocation.fromNamespaceAndPath("goddess_of_maids", "mossy_relic");
   private static final String TAG_X = "Ox";
   private static final String TAG_Y = "Oy";
   private static final String TAG_Z = "Oz";
   private static final String TAG_ROT = "Rot";
   private static final int CORE_X = 97;
   private static final int CORE_Y = 19;
   private static final int CORE_Z = 15;
   public static final int ENTRY_X0 = 121;
   public static final int ENTRY_X1 = 128;
   public static final int ENTRY_Y0 = 26;
   public static final int ENTRY_Y1 = 32;
   public static final int ENTRY_Z0 = 11;
   public static final int ENTRY_Z1 = 20;
   private static final int SHELL = 2;
   private static final int SKIRT_RIM = 2;
   private static final double SKIRT_SLOPE = 1.0;
   private static final int SKIRT_RUN = 32;
   private static final int SKIRT_DEPTH = 8;
   private static final int OPEN_FACES_FOR_GRAVEL = 3;
   private static final BlockState[] SHELL_ROCKS = new BlockState[]{
      Blocks.DEEPSLATE.defaultBlockState(), Blocks.DEEPSLATE.defaultBlockState(), Blocks.STONE.defaultBlockState(), Blocks.ANDESITE.defaultBlockState(), Blocks.TUFF.defaultBlockState()
   };
   private static final float FACE_NORTH = 180.0F;
   private static final float FACE_SOUTH = 0.0F;
   private static final float FACE_EAST = 270.0F;
   private static final MossyRelicPiece.Puppet[] PUPPETS = new MossyRelicPiece.Puppet[]{
      new MossyRelicPiece.Puppet(96, 20, 26, 180.0F),
      new MossyRelicPiece.Puppet(97, 20, 26, 180.0F),
      new MossyRelicPiece.Puppet(100, 20, 5, 0.0F),
      new MossyRelicPiece.Puppet(101, 20, 5, 0.0F),
      new MossyRelicPiece.Puppet(102, 20, 5, 0.0F),
      new MossyRelicPiece.Puppet(89, 20, 19, 270.0F),
      new MossyRelicPiece.Puppet(89, 20, 18, 270.0F),
      new MossyRelicPiece.Puppet(89, 20, 17, 270.0F),
      new MossyRelicPiece.Puppet(89, 20, 16, 270.0F),
      new MossyRelicPiece.Puppet(89, 20, 15, 270.0F),
      new MossyRelicPiece.Puppet(89, 20, 14, 270.0F),
      new MossyRelicPiece.Puppet(89, 20, 13, 270.0F)
   };
   private final StructureTemplateManager templateManager;
   private final BlockPos origin;
   private final Rotation rotation;
   private final BoundingBox relicBox;

   public static BlockPos coreColumn(ChunkPos chunk) {
      return new BlockPos(chunk.getMinBlockX() + MossyRelicStructure.originDx() + 97, 0, chunk.getMinBlockZ() + MossyRelicStructure.originDz() + 15);
   }

   public BlockPos corePos() {
      return this.origin.offset(97, 19, 15);
   }

   public MossyRelicPiece(StructureTemplateManager manager, BlockPos origin, Rotation rotation) {
      super((StructurePieceType)ModStructures.MOSSY_RELIC_PIECE.get(), 0, spreadForSkirt(measureRelic(manager, origin, rotation)));
      this.templateManager = manager;
      this.origin = origin;
      this.rotation = rotation;
      this.relicBox = measureRelic(manager, origin, rotation);
   }

   public MossyRelicPiece(StructureTemplateManager manager, CompoundTag tag) {
      super((StructurePieceType)ModStructures.MOSSY_RELIC_PIECE.get(), tag);
      this.templateManager = manager;
      this.origin = new BlockPos(tag.getInt("Ox"), tag.getInt("Oy"), tag.getInt("Oz"));
      this.rotation = Rotation.valueOf(tag.getString("Rot"));
      this.relicBox = measureRelic(manager, this.origin, this.rotation);
      this.boundingBox = spreadForSkirt(this.relicBox);
   }

   private static BoundingBox measureRelic(StructureTemplateManager manager, BlockPos origin, Rotation rotation) {
      StructureTemplate template = manager.getOrCreate(TEMPLATE);
      StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);
      return template.getBoundingBox(settings, origin);
   }

   private static BoundingBox spreadForSkirt(BoundingBox relic) {
      return new BoundingBox(
         relic.minX() - 32, relic.minY(), relic.minZ() - 32, relic.maxX() + 32, relic.maxY(), relic.maxZ() + 32
      );
   }

   protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
      tag.putInt("GD", this.genDepth);
      tag.putInt("Ox", this.origin.getX());
      tag.putInt("Oy", this.origin.getY());
      tag.putInt("Oz", this.origin.getZ());
      tag.putString("Rot", this.rotation.name());
   }

   public void postProcess(
      WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos
   ) {
      long began = System.nanoTime();
      this.wrapCavities(level, box);
      StructureTemplate template = this.templateManager.getOrCreate(TEMPLATE);
      StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(this.rotation).setMirror(Mirror.NONE).setIgnoreEntities(true).setBoundingBox(box);
      template.placeInWorld(level, this.origin, this.origin, settings, random, 2);
      this.drainEntry(level, box);
      this.erodeShaft(level, box, random);
      this.gravelizeShaft(level, box);
      this.raiseSkirt(level, box);
      this.placePuppets(level, box);
      this.placeWarden(level, box);
      long ms = (System.nanoTime() - began) / 1000000L;
      if (ms >= 30L) {
         ShenlinMod.LOGGER.info("[shenlin] relic piece {} chunk={} took {}ms", new Object[]{this.origin, chunkPos, ms});
      }
   }

   private void erodeShaft(WorldGenLevel level, BoundingBox box, RandomSource random) {
      double faceFall = Config.relicShaftFaceFall();
      double backFall = Config.relicShaftBackFall();
      int fx0 = this.origin.getX() + 121 - 2;
      int fx1 = this.origin.getX() + 128 + 2;
      int fz0 = this.origin.getZ() + 11 - 2;
      int fz1 = this.origin.getZ() + 20 + 2;
      int fy0 = this.origin.getY() + 26;
      int fy1 = this.relicBox.maxY() + 2;

      for (int y = fy0; y <= fy1; y++) {
         for (int x = fx0; x <= fx1; x++) {
            for (int z = fz0; z <= fz1; z++) {
               BlockPos p = new BlockPos(x, y, z);
               if (box.isInside(p)) {
                  int depth = this.shaftDepth(x, z);
                  if (depth == 1) {
                     if (random.nextDouble() < faceFall) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                     }
                  } else if (depth == 2 && random.nextDouble() < backFall) {
                     level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);

                     for (Direction dir : Plane.HORIZONTAL) {
                        BlockPos face = p.relative(dir);
                        if (this.shaftDepth(face.getX(), face.getZ()) == 1 && box.isInside(face)) {
                           level.setBlock(face, Blocks.AIR.defaultBlockState(), 2);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void gravelizeShaft(WorldGenLevel level, BoundingBox box) {
      int fx0 = this.origin.getX() + 121 - 2;
      int fx1 = this.origin.getX() + 128 + 2;
      int fz0 = this.origin.getZ() + 11 - 2;
      int fz1 = this.origin.getZ() + 20 + 2;
      int fy0 = this.origin.getY() + 26;
      int fy1 = this.relicBox.maxY() + 2;

      for (int y = fy0; y <= fy1; y++) {
         for (int x = fx0; x <= fx1; x++) {
            for (int z = fz0; z <= fz1; z++) {
               BlockPos p = new BlockPos(x, y, z);
               if (box.isInside(p)) {
                  int depth = this.shaftDepth(x, z);
                  if (depth == 1 || depth == 2) {
                     BlockState here = level.getBlockState(p);
                     if (!here.isAir() && !here.is(Blocks.GRAVEL)) {
                        int open = 0;

                        for (Direction dir : Direction.values()) {
                           BlockPos side = p.relative(dir);
                           if (box.isInside(side) && level.getBlockState(side).isAir()) {
                              open++;
                           }
                        }

                        if (open >= 3) {
                           level.setBlock(p, Blocks.GRAVEL.defaultBlockState(), 2);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private int shaftDepth(int x, int z) {
      int ex0 = this.origin.getX() + 121;
      int ex1 = this.origin.getX() + 128;
      int ez0 = this.origin.getZ() + 11;
      int ez1 = this.origin.getZ() + 20;
      int dx = x < ex0 ? ex0 - x : (x > ex1 ? x - ex1 : 0);
      int dz = z < ez0 ? ez0 - z : (z > ez1 ? z - ez1 : 0);
      return Math.max(dx, dz);
   }

   private void wrapCavities(WorldGenLevel level, BoundingBox box) {
      BoundingBox relic = this.relicBox;
      int x0 = Math.max(relic.minX() - 2, box.minX());
      int x1 = Math.min(relic.maxX() + 2, box.maxX());
      int y0 = Math.max(relic.minY() - 2, box.minY());
      int y1 = Math.min(relic.maxY() + 2, box.maxY());
      int z0 = Math.max(relic.minZ() - 2, box.minZ());
      int z1 = Math.min(relic.maxZ() + 2, box.maxZ());

      for (int x = x0; x <= x1; x++) {
         for (int y = y0; y <= y1; y++) {
            for (int z = z0; z <= z1; z++) {
               if (!this.inEntryColumn(x, z)) {
                  BlockPos p = new BlockPos(x, y, z);
                  BlockState here = level.getBlockState(p);
                  if (here.isAir() || !here.getFluidState().isEmpty()) {
                     level.setBlock(p, this.rockAt(x, y, z), 2);
                  }
               }
            }
         }
      }
   }

   private void drainEntry(WorldGenLevel level, BoundingBox box) {
      int x0 = this.origin.getX() + 121;
      int x1 = this.origin.getX() + 128;
      int z0 = this.origin.getZ() + 11;
      int z1 = this.origin.getZ() + 20;
      int y0 = this.origin.getY() + 26;
      int y1 = this.relicBox.maxY() + 2;

      for (int y = y0; y <= y1; y++) {
         for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
               BlockPos p = new BlockPos(x, y, z);
               if (box.isInside(p) && !level.getBlockState(p).getFluidState().isEmpty()) {
                  level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
               }
            }
         }
      }
   }

   private boolean inEntryColumn(int x, int z) {
      int sx0 = this.origin.getX() + 121;
      int sx1 = this.origin.getX() + 128;
      int sz0 = this.origin.getZ() + 11;
      int sz1 = this.origin.getZ() + 20;
      return x >= sx0 && x <= sx1 && z >= sz0 && z <= sz1;
   }

   private BlockState rockAt(int x, int y, int z) {
      int h = x * 31 + y * 17 + z & 2147483647;
      return SHELL_ROCKS[h % SHELL_ROCKS.length];
   }

   private void raiseSkirt(WorldGenLevel level, BoundingBox box) {
      int topY = this.relicBox.maxY() + 2;
      int cx = (this.relicBox.minX() + this.relicBox.maxX()) / 2;
      int cz = (this.relicBox.minZ() + this.relicBox.maxZ()) / 2;
      int halfX = (this.relicBox.maxX() - this.relicBox.minX()) / 2 + 2;
      int halfZ = (this.relicBox.maxZ() - this.relicBox.minZ()) / 2 + 2;

      for (int x = box.minX(); x <= box.maxX(); x++) {
         for (int z = box.minZ(); z <= box.maxZ(); z++) {
            if (!this.inEntryColumn(x, z)) {
               double dx = Math.max(0, Math.abs(x - cx) - halfX);
               double dz = Math.max(0, Math.abs(z - cz) - halfZ);
               double d = Math.sqrt(dx * dx + dz * dz);
               if (!(d < 1.0) && !(d > 32.0)) {
                  int want = topY - 2 - (int)Math.round((d - 1.0) * 1.0);
                  int floor = Math.max(box.minY(), want - 8);

                  for (int y = want; y >= floor; y--) {
                     BlockPos p = new BlockPos(x, y, z);
                     if (!box.isInside(p) || !level.getBlockState(p).isAir()) {
                        break;
                     }

                     level.setBlock(p, this.skirtAt(y, want), 2);
                  }
               }
            }
         }
      }
   }

   private BlockState skirtAt(int y, int top) {
      int depth = top - y;
      if (depth == 0) {
         return Blocks.GRASS_BLOCK.defaultBlockState();
      } else if (depth <= 3) {
         return Blocks.DIRT.defaultBlockState();
      } else {
         return y < 0 ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState();
      }
   }

   private void placePuppets(WorldGenLevel level, BoundingBox box) {
      for (MossyRelicPiece.Puppet spot : PUPPETS) {
         BlockPos at = this.origin.offset(spot.x(), spot.y(), spot.z());
         if (box.isInside(at)) {
            this.clearRoom(level, box, at);
            MossyPuppetEntity puppet = (MossyPuppetEntity)((EntityType)ModEntities.MOSSY_PUPPET.get()).create(level.getLevel());
            if (puppet != null) {
               puppet.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, spot.facing(), 0.0F);
               puppet.setYHeadRot(spot.facing());
               puppet.setPersistenceRequired();
               level.addFreshEntityWithPassengers(puppet);
            }
         }
      }
   }

   private void placeWarden(WorldGenLevel level, BoundingBox box) {
      BlockPos at = this.corePos().above();
      if (box.isInside(at)) {
         for (int dy = 0; dy <= 3; dy++) {
            BlockPos cell = at.above(dy);
            if (box.isInside(cell)) {
               level.setBlock(cell, Blocks.AIR.defaultBlockState(), 2);
            }
         }

         RelicSites.noteGenerated(level.getLevel(), this.corePos());
         RelicWardenEntity warden = (RelicWardenEntity)((EntityType)ModEntities.RELIC_WARDEN.get()).create(level.getLevel());
         if (warden != null) {
            warden.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
            warden.setYHeadRot(0.0F);
            warden.setPersistenceRequired();
            warden.setHomeCore(this.corePos());
            level.addFreshEntityWithPassengers(warden);
         }
      }
   }

   private void clearRoom(WorldGenLevel level, BoundingBox box, BlockPos at) {
      for (int dy = 0; dy <= 1; dy++) {
         BlockPos cell = at.above(dy);
         if (box.isInside(cell)) {
            level.setBlock(cell, Blocks.AIR.defaultBlockState(), 2);
         }
      }
   }

   private record Puppet(int x, int y, int z, float facing) {
   }
}

