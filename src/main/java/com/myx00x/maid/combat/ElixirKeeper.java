package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.inventory.handler.BaubleItemHandler;
import com.myx00x.maid.item.ItemLookup;
import com.myx00x.maid.registry.ModItems;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ElixirKeeper {
   private static final Map<UUID, Int2ObjectMap<ItemStack>> KEPT = new ConcurrentHashMap<>();

   private ElixirKeeper() {
   }

   public static void keep(EntityMaid maid) {
      if (!maid.level().isClientSide && maid.getMaidBauble().containsItem((Item)ModItems.GUARDIAN_SHIELD.get())) {
         Item elixir = ItemLookup.of("touhou_little_maid", "ultramarine_orb_elixir");
         Int2ObjectMap<ItemStack> kept = new Int2ObjectOpenHashMap();
         BaubleItemHandler handler = maid.getMaidBauble();

         for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            boolean protect = elixir != null && stack.is(elixir) || stack.is(Items.TOTEM_OF_UNDYING);
            if (protect) {
               kept.put(slot, stack.copy());
            }
         }

         if (kept.isEmpty()) {
            KEPT.remove(maid.getUUID());
         } else {
            KEPT.put(maid.getUUID(), kept);
         }
      }
   }

   public static void restoreIfRevived(EntityMaid maid) {
      Int2ObjectMap<ItemStack> kept = KEPT.remove(maid.getUUID());
      if (kept != null && !maid.level().isClientSide && !maid.isDeadOrDying()) {
         BaubleItemHandler handler = maid.getMaidBauble();
         ObjectIterator var3 = kept.int2ObjectEntrySet().iterator();

         while (var3.hasNext()) {
            Entry<ItemStack> e = (Entry<ItemStack>)var3.next();
            int slot = e.getIntKey();
            if (slot >= 0 && slot < handler.getSlots()) {
               ItemStack now = handler.getStackInSlot(slot);
               if (!ItemStack.matches(now, (ItemStack)e.getValue())) {
                  handler.setStackInSlot(slot, ((ItemStack)e.getValue()).copy());
               }
            }
         }
      }
   }
}

