package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.ShenlinMod;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class SlabAudit {
   private static final long GRACE_TICKS = 10L;
   private static final long TROUBLE_TICKS = 60L;
   private static final int EVERY = 10;
   private static final double DROP_RANGE = 48.0;
   private static final Map<UUID, Long> COLLECTED = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> RESCUED = new ConcurrentHashMap<>();
   private static final long RESCUED_TICKS = 1200L;
   private static long last;

   private SlabAudit() {
   }

   public static void noteStored(EntityMaid maid) {
      if (!maid.level().isClientSide) {
         COLLECTED.put(maid.getUUID(), maid.level().getGameTime());
      }
   }

   public static void forget(UUID maidId) {
      COLLECTED.remove(maidId);
      RESCUED.remove(maidId);
   }

   public static void tick(MinecraftServer server) {
      if (server != null) {
         ServerLevel level = server.overworld();
         if (level != null) {
            long now = level.getGameTime();
            if (now < last) {
               last = now;
            }

            if (now >= last + 10L) {
               last = now;
               if (COLLECTED.size() > 256) {
                  COLLECTED.entrySet().removeIf(e -> now - e.getValue() > 2400L);
               }

               if (RESCUED.size() > 256) {
                  RESCUED.entrySet().removeIf(e -> now - e.getValue() > 1200L);
               }

               Set<UUID> watched = GuardWatch.watched();
               if (!watched.isEmpty()) {
                  Set<UUID> owners = new HashSet<>();
                  int nuked = sweep(level, watched, owners, now);
                  if (nuked > 0) {
                     ShenlinMod.LOGGER.info("[shenlin] forged slabs removed, maids={} nuked={}", owners.size(), nuked);
                  }
               }
            }
         }
      }
   }

   private static int sweep(ServerLevel level, Set<UUID> watched, Set<UUID> owners, long now) {
      int nuked = 0;

      for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
         Inventory inventory = player.getInventory();

         for (int i = 0; i < inventory.getContainerSize(); i++) {
            UUID owner = reject(inventory.getItem(i), watched, now);
            if (owner != null) {
               inventory.setItem(i, ItemStack.EMPTY);
               inventory.setChanged();
               owners.add(owner);
               nuked++;
            }
         }

         if (player.level() instanceof ServerLevel around) {
            for (ItemEntity drop : around.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(48.0))) {
               if (!drop.isRemoved()) {
                  UUID owner = reject(drop.getItem(), watched, now);
                  if (owner != null) {
                     drop.discard();
                     owners.add(owner);
                     nuked++;
                  }
               }
            }
         }
      }

      return nuked;
   }

   private static UUID reject(ItemStack stack, Set<UUID> watched, long now) {
      UUID owner = storedMaid(stack);
      if (owner != null && watched.contains(owner)) {
         return keep(owner, now) ? null : owner;
      } else {
         return null;
      }
   }

   private static boolean keep(UUID maidId, long now) {
      Long storedAt = COLLECTED.get(maidId);
      if (storedAt != null && now - storedAt <= 10L) {
         return true;
      } else if (GuardWatch.isVanishable(maidId)) {
         return true;
      } else if (find(maidId) == null) {
         return true;
      } else if (GuardKeeper.troubledRecently(maidId, now, 60L)) {
         RESCUED.put(maidId, now);
         return true;
      } else {
         Long rescuedAt = RESCUED.get(maidId);
         return rescuedAt != null && now - rescuedAt <= 1200L;
      }
   }

   private static UUID storedMaid(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         CompoundTag root = stack.getTag();
         if (root == null) {
            return null;
         }

         CompoundTag info = root.getCompound("MaidInfo");
         return !info.isEmpty() && info.hasUUID("UUID") ? info.getUUID("UUID") : null;
      } else {
         return null;
      }
   }

   private static EntityMaid find(UUID maidId) {
      EntityMaid tracked = GuardWatch.held(maidId);
      if (tracked != null && !tracked.isRemoved()) {
         return GuardVitals.alive(tracked) ? tracked : null;
      } else {
         return null;
      }
   }

   public static void forgetAll() {
      COLLECTED.clear();
      RESCUED.clear();
   }
}

