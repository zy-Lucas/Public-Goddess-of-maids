package com.myx00x.maid.world;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class RelicOpeningState extends SavedData {
   private static final String NAME = "shenlin_relic_openings";
   private static final String TAG_OPENED = "Opened";
   private final Set<BlockPos> opened = new HashSet<>();

   public static RelicOpeningState get(ServerLevel level) {
      return (RelicOpeningState)level.getDataStorage().computeIfAbsent(RelicOpeningState::load, RelicOpeningState::new, "shenlin_relic_openings");
   }

   public static RelicOpeningState load(CompoundTag tag) {
      RelicOpeningState state = new RelicOpeningState();

      for (long packed : tag.getLongArray("Opened")) {
         state.opened.add(BlockPos.of(packed));
      }

      return state;
   }

   public CompoundTag save(CompoundTag tag) {
      long[] packed = new long[this.opened.size()];
      int i = 0;

      for (BlockPos pos : this.opened) {
         packed[i++] = pos.asLong();
      }

      tag.putLongArray("Opened", packed);
      return tag;
   }

   public boolean isOpened(BlockPos core) {
      return this.opened.contains(core);
   }

   public void markOpened(BlockPos core) {
      if (this.opened.add(core)) {
         this.setDirty();
      }
   }

   public void clear(BlockPos core) {
      if (this.opened.remove(core)) {
         this.setDirty();
      }
   }

   public Set<BlockPos> opened() {
      return this.opened;
   }
}

