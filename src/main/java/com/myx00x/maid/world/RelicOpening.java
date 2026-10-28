package com.myx00x.maid.world;

import com.myx00x.maid.boss.RuinedPhantasmKeeper;
import com.myx00x.maid.network.ModPackets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;

public final class RelicOpening {
   private static final ResourceKey<Structure> RELIC = ResourceKey.create(
      Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath("goddess_of_maids", "mossy_relic")
   );
   private static final int SCAN_PERIOD = 10;
   private static final int LAMP_PERIOD = 20;
   private static final int TRIGGER_X0 = -97;
   private static final int TRIGGER_X1 = -41;
   private static final int TRIGGER_Y0 = -19;
   private static final int TRIGGER_Y1 = 17;
   private static final int TRIGGER_Z0 = -15;
   private static final int TRIGGER_Z1 = 41;
   private static final int FLOOR_Y = -19;
   private static final int CENTER_DX = -69;
   private static final int CENTER_DZ = 13;
   private static final int[][] SLASH_FROM = new int[][]{{-42, -14}, {-42, 40}, {-96, 40}, {-96, -14}};
   private static final int[][] SLASH_BY = new int[][]{{-1, 1}, {-1, -1}, {1, -1}, {1, 1}};
   private static final int SLASH_LAST = 26;
   private static final int[][] LINE_FROM = new int[][]{{-42, 13}, {-69, 40}, {-96, 13}, {-69, -14}};
   private static final int[][] LINE_BY = new int[][]{{-1, 0}, {0, -1}, {1, 0}, {0, 1}};
   private static final int LINE_LAST = 8;
   private static final int LINE_GAP = 3;
   private static final int LINE_PLACE_EVERY = 3;
   private static final int LINE_LIGHT_EVERY = 4;
   private static final int FINALE_AT = 32;
   private static final int FINALE_GAP = 3;
   private static final double CALL_RANGE = 11.0;
   private static final float WARP_RADIUS = 40.0F;
   private static final double WARP_RANGE = 64.0;
   private static final int WARP_GROW_MS = 1500;
   private static final int WARP_HOLD_MS = 400;
   private static final float WARP_FROM = 0.12F;
   private static final int WARP_TICKS = 38;
   private static final int WARP_FLICKER_TICKS = 120;
   private static final Map<String, RelicOpening.Show> SHOWS = new ConcurrentHashMap<>();

   private RelicOpening() {
   }

   public static void tick(MinecraftServer server) {
      if (server != null) {
         if (!SHOWS.isEmpty()) {
            for (RelicOpening.Show show : SHOWS.values()) {
               show.step(server);
            }

            SHOWS.values().removeIf(RelicOpening.Show::done);
         }

         long now = server.getTickCount();
         if (now % 10L == 0L) {
            scan(server);
         }

         if (now % 20L == 0L) {
            relight(server);
         }
      }
   }

   private static void scan(MinecraftServer server) {
      for (ServerPlayer player : server.getPlayerList().getPlayers()) {
         ServerLevel level = player.serverLevel();
         StructureStart start = level.structureManager().getStructureWithPieceAt(player.blockPosition(), RELIC);
         if (start != null && start.isValid()) {
            BlockPos core = coreOf(start);
            if (core != null) {
               RelicOpeningState state = RelicOpeningState.get(level);
               if (!state.isOpened(core) && inTrigger(core, player)) {
                  state.markOpened(core);
                  SHOWS.put(level.dimension().location() + "@" + core.asLong(), new RelicOpening.Show(level, core, player.getUUID()));
               }
            }
         }
      }
   }

   private static boolean inTrigger(BlockPos core, ServerPlayer player) {
      int dx = player.getBlockX() - core.getX();
      int dy = player.getBlockY() - core.getY();
      int dz = player.getBlockZ() - core.getZ();
      return dx >= -97 && dx <= -41 && dy >= -19 && dy <= 17 && dz >= -15 && dz <= 41;
   }

   private static BlockPos coreOf(StructureStart start) {
      for (StructurePiece piece : start.getPieces()) {
         if (piece instanceof MossyRelicPiece relic) {
            return relic.corePos();
         }
      }

      return null;
   }

   private static void relight(MinecraftServer server) {
      for (ServerLevel level : server.getAllLevels()) {
         for (BlockPos core : RelicOpeningState.get(level).opened()) {
            eachLamp(core, pos -> {
               if (loaded(level, pos)) {
                  BlockState state = level.getBlockState(pos);
                  if (state.is(Blocks.REDSTONE_LAMP) && !(Boolean)state.getValue(RedstoneLampBlock.LIT)) {
                     level.setBlock(pos, (BlockState)state.setValue(RedstoneLampBlock.LIT, true), 2);
                  }
               }
            });
         }
      }
   }

   public static BlockPos restoreNear(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      StructureStart start = level.structureManager().getStructureWithPieceAt(player.blockPosition(), RELIC);
      if (start != null && start.isValid()) {
         BlockPos core = coreOf(start);
         if (core == null) {
            return null;
         }

         BlockState brick = Blocks.STONE_BRICKS.defaultBlockState();
         eachLamp(core, pos -> reset(level, pos, brick));
         BlockPos center = floor(core, -69, 13);
         reset(level, center, brick);
         eachAltarCell(center, pos -> reset(level, pos, Blocks.AIR.defaultBlockState()));
         SHOWS.remove(level.dimension().location() + "@" + core.asLong());
         RelicOpeningState.get(level).clear(core);
         return core;
      } else {
         return null;
      }
   }

   private static boolean loaded(ServerLevel level, BlockPos pos) {
      return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
   }

   private static void reset(ServerLevel level, BlockPos pos, BlockState state) {
      if (loaded(level, pos)) {
         level.setBlock(pos, state, 2);
      }
   }

   private static void eachAltarCell(BlockPos center, Consumer<BlockPos> sink) {
      for (int dx = -2; dx <= 2; dx++) {
         for (int dz = -2; dz <= 2; dz++) {
            sink.accept(center.offset(dx, 1, dz));
         }
      }

      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            sink.accept(center.offset(dx, 2, dz));
         }
      }

      sink.accept(center.offset(0, 3, 0));
   }

   private static void eachLamp(BlockPos core, Consumer<BlockPos> sink) {
      for (int i = 0; i < 4; i++) {
         for (int t = 0; t <= 26; t++) {
            sink.accept(floor(core, SLASH_FROM[i][0] + SLASH_BY[i][0] * t, SLASH_FROM[i][1] + SLASH_BY[i][1] * t));
         }

         for (int k = 0; k <= 8; k++) {
            sink.accept(lineAt(core, i, k));
         }
      }

      for (int sx = -1; sx <= 1; sx += 2) {
         for (int sz = -1; sz <= 1; sz += 2) {
            sink.accept(core.offset(-69 + 2 * sx, -18, 13 + 2 * sz));
         }
      }
   }

   private static BlockPos floor(BlockPos core, int dx, int dz) {
      return core.offset(dx, -19, dz);
   }

   private static BlockPos lineAt(BlockPos core, int line, int k) {
      int far = 3 * k;
      return floor(core, LINE_FROM[line][0] + LINE_BY[line][0] * far, LINE_FROM[line][1] + LINE_BY[line][1] * far);
   }

   private static BlockState lamp(boolean lit) {
      return (BlockState)Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, lit);
   }

   private static void swap(ServerLevel level, BlockPos pos, BlockState state) {
      if (loaded(level, pos)) {
         BlockState old = level.getBlockState(pos);
         level.setBlock(pos, state, 2);
         if (!old.isAir()) {
            level.levelEvent(2001, pos, Block.getId(old));
         }
      }
   }

   private static void light(ServerLevel level, BlockPos pos) {
      if (loaded(level, pos)) {
         BlockState state = level.getBlockState(pos);
         if (state.is(Blocks.REDSTONE_LAMP) && !(Boolean)state.getValue(RedstoneLampBlock.LIT)) {
            level.setBlock(pos, (BlockState)state.setValue(RedstoneLampBlock.LIT, true), 2);
         }
      }
   }

   private static void buildAltar(ServerLevel level, BlockPos base) {
      for (int dx = -2; dx <= 2; dx++) {
         for (int dz = -2; dz <= 2; dz++) {
            boolean edge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
            boolean corner = Math.abs(dx) == 2 && Math.abs(dz) == 2;
            BlockState state;
            if (corner) {
               state = lamp(true);
            } else if (edge) {
               state = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
            } else {
               state = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
            }

            swap(level, base.offset(dx, 0, dz), state);
         }
      }

      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            boolean corner = Math.abs(dx) == 1 && Math.abs(dz) == 1;
            swap(level, base.offset(dx, 1, dz), corner ? Blocks.STONE_BRICK_WALL.defaultBlockState() : Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
         }
      }

      swap(level, base.offset(0, 2, 0), Blocks.SOUL_CAMPFIRE.defaultBlockState());
   }

   public static void forgetAll() {
      SHOWS.clear();
   }

   private static final class Show {
      private final ServerLevel level;
      private final BlockPos core;
      private final UUID trigger;
      private int age;
      private int warp = -1;

      private Show(ServerLevel level, BlockPos core, UUID trigger) {
         this.level = level;
         this.core = core;
         this.trigger = trigger;
      }

      private void step(MinecraftServer server) {
         if (this.age <= 26) {
            for (int i = 0; i < 4; i++) {
               RelicOpening.swap(
                  this.level,
                  RelicOpening.floor(
                     this.core,
                     RelicOpening.SLASH_FROM[i][0] + RelicOpening.SLASH_BY[i][0] * this.age,
                     RelicOpening.SLASH_FROM[i][1] + RelicOpening.SLASH_BY[i][1] * this.age
                  ),
                  RelicOpening.lamp(true)
               );
            }
         }

         if (this.age % 3 == 0) {
            int k = this.age / 3;
            if (k <= 8) {
               for (int i = 0; i < 4; i++) {
                  RelicOpening.swap(this.level, RelicOpening.lineAt(this.core, i, k), RelicOpening.lamp(false));
               }
            }
         }

         if (this.age % 4 == 0) {
            int k = this.age / 4;
            if (k <= 8) {
               for (int i = 0; i < 4; i++) {
                  RelicOpening.light(this.level, RelicOpening.lineAt(this.core, i, k));
               }
            }
         }

         if (this.age == 35) {
            RelicOpening.swap(this.level, RelicOpening.floor(this.core, -69, 13), Blocks.NETHERITE_BLOCK.defaultBlockState());
         }

         if (this.age == 36) {
            BlockPos center = RelicOpening.floor(this.core, -69, 13);
            RuinedPhantasmKeeper.summon(this.level, center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5);
            this.call(server);
            ModPackets.sendShockWarp(
               this.level, new Vec3(center.getX() + 0.5, center.getY() + 1.5, center.getZ() + 0.5), 64.0, 40.0F, 40.0F, 1500, 400, 0, true
            );
            this.warp = 0;
         }

         if (this.warp >= 0) {
            this.warp++;
         }

         this.age++;
      }

      private void call(MinecraftServer server) {
         ServerPlayer source = server.getPlayerList().getPlayer(this.trigger);

         for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.serverLevel() == this.level) {
               boolean self = source != null && player == source;
               boolean near = source != null && player.distanceToSqr(source) <= 121.0;
               if (self || near || RelicOpening.inTrigger(this.core, player)) {
                  player.playNotifySound(SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F);
               }
            }
         }
      }

      private static float warpRatio(int tick) {
         float total = 1900.0F;
         float t = Math.min(1.0F, tick * 50.0F / total);
         return 0.12F + 0.88F * (1.0F - (1.0F - t) * (1.0F - t));
      }

      private boolean done() {
         return this.warp > 38;
      }
   }
}

