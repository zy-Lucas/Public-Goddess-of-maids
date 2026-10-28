package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.item.ItemLookup;
import com.myx00x.maid.registry.ModItems;
import net.minecraft.world.item.Item;

public final class GuardTier {
   public static final int SHIELD = 1;
   public static final int ANCHORED = 2;
   public static final int ABSOLUTE = 3;
   public static final int NONE = 0;
   public static final float LAST_DROP = 1.0F;
   private static final String ELIXIR_NAMESPACE = "touhou_little_maid";
   private static final String ELIXIR_PATH = "ultramarine_orb_elixir";
   private static final String ANCHOR_NAMESPACE = "touhou_little_maid_spell";
   private static final String ANCHOR_PATH = "anchor_core";
   private static final String PROTECTION_NAMESPACE = "goddess_of_maids";
   private static final String PROTECTION_PATH = "protection_core";

   private GuardTier() {
   }

   public static int of(EntityMaid maid) {
      if (!wearingShield(maid)) {
         return 0;
      } else if (!wears(maid, "touhou_little_maid_spell", "anchor_core") && !wears(maid, "goddess_of_maids", "protection_core")) {
         return 1;
      } else {
         return wears(maid, "touhou_little_maid", "ultramarine_orb_elixir") ? 3 : 2;
      }
   }

   public static boolean locksAtZero(int tier) {
      return tier >= 3;
   }

   public static boolean rejectsRemoval(int tier) {
      return tier >= 2;
   }

   private static boolean wears(EntityMaid maid, String namespace, String path) {
      try {
         Item item = ItemLookup.of(namespace, path);
         return item != null && maid.getMaidBauble().containsItem(item);
      } catch (Throwable t) {
         return false;
      }
   }

   public static boolean wearingShield(EntityMaid maid) {
      try {
         return maid.getMaidBauble().containsItem((Item)ModItems.GUARDIAN_SHIELD.get());
      } catch (Throwable t) {
         return false;
      }
   }
}

