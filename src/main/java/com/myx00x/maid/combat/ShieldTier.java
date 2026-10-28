package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.Config;
import com.myx00x.maid.item.ItemLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class ShieldTier {
   private static final String ELIXIR_NAMESPACE = "touhou_little_maid";
   private static final String ELIXIR_PATH = "ultramarine_orb_elixir";
   private static final String ANCHOR_NAMESPACE = "touhou_little_maid_spell";
   private static final String ANCHOR_PATH = "anchor_core";

   private ShieldTier() {
   }

   public static boolean canRevive(EntityMaid maid) {
      if (!(Boolean)Config.SHIELD_TIERED.get()) {
         return true;
      }

      Item elixir = ItemLookup.of("touhou_little_maid", "ultramarine_orb_elixir");
      return elixir != null && wears(maid, elixir) || wears(maid, Items.TOTEM_OF_UNDYING);
   }

   public static boolean canHoldClear(EntityMaid maid) {
      if (!(Boolean)Config.SHIELD_TIERED.get()) {
         return true;
      }

      Item anchor = ItemLookup.of("touhou_little_maid_spell", "anchor_core");
      return anchor != null && wears(maid, anchor);
   }

   private static boolean wears(EntityMaid maid, Item item) {
      try {
         return maid.getMaidBauble().containsItem(item);
      } catch (Throwable t) {
         return false;
      }
   }
}

