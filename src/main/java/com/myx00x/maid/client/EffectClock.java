package com.myx00x.maid.client;

public final class EffectClock {
   private EffectClock() {
   }

   public static long now() {
      return System.nanoTime() / 1000000L;
   }
}

