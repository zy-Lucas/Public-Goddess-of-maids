package com.myx00x.maid.compat;

import net.minecraftforge.fml.ModList;

public final class Upstream {
   public static final boolean MAID = loaded("touhou_little_maid");
   public static final boolean SPELL = MAID && loaded("touhou_little_maid_spell");

   private Upstream() {
   }

   private static boolean loaded(String modId) {
      try {
         ModList list = ModList.get();
         return list != null && list.isLoaded(modId);
      } catch (Throwable t) {
         return false;
      }
   }
}

