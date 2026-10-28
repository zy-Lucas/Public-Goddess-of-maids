package com.myx00x.maid.boss;

public final class RuinedPhantasmMusic {
   public static final int TOTAL_TICKS = 4660;
   public static final String TRACK_ID = "goddess_of_maids:boss_theme";

   private RuinedPhantasmMusic() {
   }

   public static boolean hasTrack() {
      return "goddess_of_maids:boss_theme" != null && !"goddess_of_maids:boss_theme".isEmpty();
   }
}

