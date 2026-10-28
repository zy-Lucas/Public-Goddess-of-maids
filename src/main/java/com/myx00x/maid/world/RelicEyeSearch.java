package com.myx00x.maid.world;

import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.item.RelicEyeItem;
import com.myx00x.maid.mob.RelicWardenEntity;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.WorldWorkerManager;
import net.minecraftforge.common.WorldWorkerManager.IWorker;

public final class RelicEyeSearch {
   private static final ResourceLocation RELIC_ID = ResourceLocation.fromNamespaceAndPath("goddess_of_maids", "mossy_relic");
   private static final Map<UUID, RelicEyeSearch.Pending> VERIFY = new HashMap<>();

   private RelicEyeSearch() {
   }

   public static void start(ServerPlayer player, ItemStack stack, InteractionHand hand, ServerLevel level) {
      WorldWorkerManager.addWorker(new RelicEyeSearch.Job(level, player, stack, hand, Config.relicEyeSearchRange()));
   }

   public static void verifyNextTick(ServerPlayer player, ItemStack stack, ServerLevel level, BlockPos site) {
      VERIFY.put(player.getUUID(), new RelicEyeSearch.Pending(stack, level, site));
   }

   public static void tick(MinecraftServer server) {
      if (!VERIFY.isEmpty()) {
         Iterator<Entry<UUID, RelicEyeSearch.Pending>> vi = VERIFY.entrySet().iterator();

         while (vi.hasNext()) {
            Entry<UUID, RelicEyeSearch.Pending> entry = vi.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            RelicEyeSearch.Pending pending = entry.getValue();
            if (player != null && !player.isRemoved()) {
               if (--pending.wait <= 0) {
                  vi.remove();
                  boolean loaded = pending.level.getChunkSource().getChunkNow(pending.site.getX() >> 4, pending.site.getZ() >> 4) != null;
                  boolean guarded = !loaded || !pending.level.getEntitiesOfClass(RelicWardenEntity.class, new AABB(pending.site).inflate(48.0)).isEmpty();
                  if (guarded) {
                     RelicEyeItem.bind(pending.stack, pending.level, pending.site);
                     player.displayClientMessage(Component.translatable("item.goddess_of_maids.relic_eye.linked"), true);
                  } else {
                     RelicSites.get(pending.level).markExplored(pending.site);
                     RelicSites.get(pending.level).markClaimed(pending.site);
                  }
               }
            } else {
               vi.remove();
            }
         }
      }
   }

   private static final class Job implements IWorker {
      private final ServerLevel level;
      private final ServerPlayer player;
      private final ItemStack stack;
      private final InteractionHand hand;
      private final int limit;
      private final int spacing;
      private final int span;
      private final long seed;
      private final int originRegionX;
      private final int originRegionZ;
      private final ArrayDeque<int[]> queue = new ArrayDeque<>();
      private final Structure structure;
      private final RandomSpreadStructurePlacement spread;
      private int length;
      private int x;
      private int z;
      private int scanned;
      private int present;
      private int filtered;
      private int skipped;
      private boolean done;

      Job(ServerLevel level, ServerPlayer player, ItemStack stack, InteractionHand hand, int limit) {
         this.level = level;
         this.player = player;
         this.stack = stack;
         this.hand = hand;
         this.limit = limit;
         this.seed = level.getSeed();
         this.structure = (Structure)level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(RelicEyeSearch.RELIC_ID);
         StructureSet set = (StructureSet)level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(RelicEyeSearch.RELIC_ID);
         this.spread = set != null && set.placement() instanceof RandomSpreadStructurePlacement s ? s : null;
         this.spacing = this.spread == null ? 60 : Math.max(1, this.spread.spacing());
         this.span = this.spacing * 16;
         BlockPos from = player.blockPosition();
         this.originRegionX = Math.floorDiv(from.getX(), this.span);
         this.originRegionZ = Math.floorDiv(from.getZ(), this.span);
         ShenlinMod.LOGGER
            .info(
               "[shenlin] relic eye job from={} region=({},{}) spacing={} structure={} spread={}",
               new Object[]{
                  from, this.originRegionX, this.originRegionZ, this.spacing, this.structure == null ? "null" : "ok", this.spread == null ? "null" : "ok"
               }
            );
      }

      public boolean hasWork() {
         return !this.done && !this.player.isRemoved() && this.player.level() == this.level && this.player.getItemInHand(this.hand) == this.stack;
      }

      public boolean doWork() {
         if (this.structure != null && this.spread != null) {
            int i = this.originRegionX * this.spacing + this.spacing * this.x;
            int j = this.originRegionZ * this.spacing + this.spacing * this.z;
            ChunkPos candidate = this.spread.getPotentialStructureChunk(this.seed, i, j);
            this.scanned++;
            StructureCheckResult check = this.level.structureManager().checkStructurePresence(candidate, this.structure, false);
            if (this.scanned % 100 == 0) {
               this.player.displayClientMessage(Component.literal("§7正在找遗迹…… 已看 " + this.scanned + " 处（第 " + this.length + " 圈）"), true);
            }

            if (this.scanned % 2000 == 0) {
               ShenlinMod.LOGGER
                  .info(
                     "[shenlin] relic eye scan n={} length={} present={} filtered={} nostart={} last={}",
                     new Object[]{this.scanned, this.length, this.present, this.filtered, this.skipped, check}
                  );
            }

            if (check == StructureCheckResult.START_PRESENT || check == StructureCheckResult.CHUNK_LOAD_NEEDED) {
               if (check == StructureCheckResult.START_PRESENT) {
                  this.present++;
               }

               RelicSites sites = RelicSites.get(this.level);
               BlockPos guess = MossyRelicPiece.coreColumn(candidate);
               if (sites.isClaimedColumn(guess.getX(), guess.getZ())) {
                  this.filtered++;
                  return this.advance();
               }

               BlockPos core = RelicSites.coreAt(this.level, candidate, this.structure);
               if (core != null) {
                  sites.markKnown(core);
                  boolean allowVisited = this.length > 8;
                  if (!sites.isClaimed(core) && (allowVisited || !sites.isExplored(core))) {
                     this.finish(null);
                     this.land(core);
                     return false;
                  }

                  this.filtered++;
               } else {
                  this.skipped++;
               }
            }

            return this.advance();
         } else {
            this.finish(Component.translatable("item.goddess_of_maids.relic_eye.none"));
            return false;
         }
      }

      private boolean advance() {
         if (this.length <= 4096 && (this.limit <= 0 || this.length * this.span <= this.limit)) {
            if (this.z < this.length) {
               this.z++;
            } else if (this.x < this.length) {
               this.x++;
               this.z = -this.length;
            } else {
               this.length++;
               this.x = -this.length;
               this.z = -this.length;
            }

            return true;
         } else {
            this.finish(Component.translatable("item.goddess_of_maids.relic_eye.none"));
            return false;
         }
      }

      private void finish(Component message) {
         this.done = true;
         if (message != null) {
            this.player.displayClientMessage(message, true);
         }
      }

      private void land(BlockPos site) {
         RelicEyeItem.bind(this.stack, this.level, site);
         BlockPos land = site.offset(28, 8, 0);
         ShenlinMod.LOGGER
            .info("[shenlin] relic eye site={} land={} dim={} scanned={}", new Object[]{site, land, this.level.dimension().location(), this.scanned});
         RelicDoor.open(this.player, site, this.stack);
      }
   }

   private static final class Pending {
      private final ItemStack stack;
      private final ServerLevel level;
      private final BlockPos site;
      private int wait = 2;

      Pending(ItemStack stack, ServerLevel level, BlockPos site) {
         this.stack = stack;
         this.level = level;
         this.site = site;
      }
   }
}

