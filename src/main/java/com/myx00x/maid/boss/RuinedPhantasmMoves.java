package com.myx00x.maid.boss;

import com.myx00x.maid.ShenlinMod;

public final class RuinedPhantasmMoves {
   private RuinedPhantasmMoves() {
   }

   public static void play(RuinedPhantasmBoss boss, int musicTick) {
      if (boss != null) {
         if (musicTick % 100 == 0) {
            ShenlinMod.LOGGER
               .info(
                  "[shenlin] ruined phantasm music tick={} pos={}/{}/{}",
                  new Object[]{musicTick, String.format("%.1f", boss.x()), String.format("%.1f", boss.y()), String.format("%.1f", boss.z())}
               );
         }
      }
   }
}

