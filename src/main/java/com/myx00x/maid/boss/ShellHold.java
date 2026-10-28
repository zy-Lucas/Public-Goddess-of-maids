package com.myx00x.maid.boss;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ShellHold {
   private static final Set<Integer> HELD = ConcurrentHashMap.newKeySet();

   private ShellHold() {
   }

   public static void hold(int id) {
      if (id != 0) {
         HELD.add(id);
      }
   }

   public static void release(int id) {
      HELD.remove(id);
   }

   public static boolean held(int id) {
      return id != 0 && HELD.contains(id);
   }

   public static void clear() {
      HELD.clear();
   }
}

