package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import com.myx00x.maid.ShenlinMod;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public final class MaidSlabber {
   private static final String MAID_INFO = "MaidInfo";

   private MaidSlabber() {
   }

   public static boolean pack(EntityMaid maid) {
      if (!maid.level().isClientSide && maid.level() instanceof ServerLevel level) {
         CompoundTag data = ShieldWatcher.liveData(maid);
         if (data != null && !data.isEmpty()) {
            ItemStack slab = ((Item)InitItems.SMART_SLAB_HAS_MAID.get()).getDefaultInstance();
            slab.getOrCreateTagElement("MaidInfo").merge(data);
            ShieldWatcher.stash(maid);
            MaidSlabber.Outcome outcome = handOver(level, maid.getOwnerUUID(), slab, maid.getX(), maid.getY(), maid.getZ());
            if (outcome != MaidSlabber.Outcome.DELIVERED) {
               ShenlinMod.LOGGER.warn("[shenlin] maid {} not packed away: slab was not delivered ({})", maid.getUUID(), outcome);
               return false;
            } else {
               vanish(level, maid);
               ShenlinMod.LOGGER.info("[shenlin] maid {} into slab", maid.getUUID());
               return true;
            }
         } else {
            ShenlinMod.LOGGER.warn("[shenlin] maid {} has no data to pack", maid.getUUID());
            return false;
         }
      } else {
         return false;
      }
   }

   public static MaidSlabber.Outcome packFromBackup(ServerLevel level, UUID maidId, UUID ownerId, CompoundTag backup, double x, double y, double z) {
      if (backup != null && !backup.isEmpty()) {
         ItemStack slab;
         try {
            slab = ((Item)InitItems.SMART_SLAB_HAS_MAID.get()).getDefaultInstance();
            slab.getOrCreateTagElement("MaidInfo").merge(backup.copy());
         } catch (Throwable t) {
            ShenlinMod.LOGGER.warn("[shenlin] maid {} into slab from backup failed: {}", maidId, t.getMessage());
            return MaidSlabber.Outcome.FAILED;
         }

         MaidSlabber.Outcome outcome = handOver(level, ownerId, slab, x, y, z);
         if (outcome != MaidSlabber.Outcome.FAILED) {
            ShenlinMod.LOGGER.info("[shenlin] maid {} into slab from backup", maidId);
         }

         return outcome;
      } else {
         return MaidSlabber.Outcome.FAILED;
      }
   }

   public static void dropSlabData(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         stack.removeTagKey("MaidInfo");
      }
   }

   static void voidSlab(ItemStack stack) {
      dropSlabData(stack);
   }

   static boolean tryDeliver(MinecraftServer server, ShieldWatcher.Snapshot snapshot) {
      if (server != null && snapshot != null && snapshot.ownerId() != null) {
         ServerPlayer owner = server.getPlayerList().getPlayer(snapshot.ownerId());
         if (owner == null) {
            return false;
         }

         ItemStack slab;
         try {
            slab = ((Item)InitItems.SMART_SLAB_HAS_MAID.get()).getDefaultInstance();
            slab.getOrCreateTagElement("MaidInfo").merge(snapshot.nbt().copy());
         } catch (Throwable t) {
            return false;
         }

         if (!intoInventory(owner, slab)) {
            return false;
         }

         ShenlinMod.LOGGER.info("[shenlin] maid {} slab delivered", snapshot.maidId());
         return true;
      } else {
         return false;
      }
   }

   private static MaidSlabber.Outcome handOver(ServerLevel level, UUID ownerId, ItemStack slab, double x, double y, double z) {
      ServerPlayer owner = ownerId == null ? null : level.getServer().getPlayerList().getPlayer(ownerId);
      if (owner == null) {
         Block.popResource(level, BlockPos.containing(x, y, z), slab);
         return MaidSlabber.Outcome.DELIVERED;
      }

      if (intoInventory(owner, slab)) {
         return MaidSlabber.Outcome.DELIVERED;
      }

      owner.drop(slab, false);
      ShenlinMod.LOGGER.warn("[shenlin] slab for maid could not enter the inventory, dropped at {} feet", owner.getGameProfile().getName());
      return MaidSlabber.Outcome.DELIVERED;
   }

   static boolean intoInventory(ServerPlayer owner, ItemStack slab) {
      Inventory inventory = owner.getInventory();
      if (owner.getOffhandItem().is((Item)InitItems.SMART_SLAB_EMPTY.get())) {
         owner.setItemInHand(InteractionHand.OFF_HAND, slab);
         ShenlinMod.LOGGER.info("[shenlin] slab delivered into the off hand");
         return true;
      }

      for (int i = 0; i < inventory.getContainerSize(); i++) {
         ItemStack slot = inventory.getItem(i);
         if (slot.is((Item)InitItems.SMART_SLAB_EMPTY.get())) {
            if (slot.getCount() <= 1) {
               inventory.setItem(i, slab);
               ShenlinMod.LOGGER.info("[shenlin] slab delivered into slot {}", i);
               return true;
            }

            ItemStack rest = slot.copy();
            rest.shrink(1);
            inventory.setItem(i, slab);
            if (!inventory.add(rest)) {
               owner.drop(rest, false);
            }

            ShenlinMod.LOGGER.info("[shenlin] slab delivered into slot {} (empty slabs left behind)", i);
            return true;
         }
      }

      int free = inventory.getFreeSlot();
      if (free < 0) {
         return false;
      }

      inventory.setItem(free, slab);
      ShenlinMod.LOGGER.info("[shenlin] slab delivered into free slot {}", free);
      return true;
   }

   private static void vanish(ServerLevel level, EntityMaid maid) {
      level.playSound(null, maid.getX(), maid.getY(), maid.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.0F, 0.8F);
   }

   public enum Outcome {
      FAILED,
      DELIVERED,
      PENDING;
   }
}

