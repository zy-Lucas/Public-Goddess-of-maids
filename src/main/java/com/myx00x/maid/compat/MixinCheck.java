package com.myx00x.maid.compat;

import com.myx00x.maid.ShenlinMod;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class MixinCheck {
   private static final List<MixinCheck.Entry> ENTRIES = List.of(
      new MixinCheck.Entry("entity-hard-removal", "net.minecraft.world.entity.Entity", "shenlin.holdGuardedMaid", false),
      new MixinCheck.Entry("lookup-gate", "net.minecraft.world.level.entity.EntityLookup", "shenlin.holdShell", false),
      new MixinCheck.Entry("ticklist-gate", "net.minecraft.world.level.entity.EntityTickList", "shenlin.holdShell", false),
      new MixinCheck.Entry("callback-gate", "net.minecraft.world.level.entity.PersistentEntitySectionManager.Callback", "shenlin.holdShell", false),
      new MixinCheck.Entry("chunkmap-gate", "net.minecraft.server.level.ChunkMap", "shenlin.holdShell", false),
      new MixinCheck.Entry("playerlist-gate", "net.minecraft.server.players.PlayerList", "shenlin.holdRemoval", false),
      new MixinCheck.Entry("level-getter", "net.minecraft.server.level.ServerLevel", "shenlin.wrap", false),
      new MixinCheck.Entry("client-removal-gate", "net.minecraft.client.multiplayer.ClientPacketListener", "shenlin.keepGuarded", true),
      new MixinCheck.Entry("damned-screen", "net.minecraft.client.renderer.GameRenderer", "shenlin.damnedScreen", true),
      new MixinCheck.Entry("damned-input", "net.minecraft.client.player.LocalPlayer", "shenlin.holdDamned", true)
   );

   private MixinCheck() {
   }

   public static void report() {
      run(false);
   }

   public static void reportClient() {
      run(true);
   }

   private static void run(boolean clientPass) {
      List<String> missing = new ArrayList<>();
      int total = 0;

      for (MixinCheck.Entry entry : ENTRIES) {
         if (entry.clientOnly == clientPass) {
            total++;
            if (!present(entry)) {
               missing.add(entry.label);
            }
         }
      }

      if (total != 0) {
         String side = clientPass ? " (client)" : "";
         if (missing.isEmpty()) {
            ShenlinMod.LOGGER.info("[shenlin] mixin self-check{}: {}/{} in place", new Object[]{side, total, total});
         } else {
            ShenlinMod.LOGGER.warn("[shenlin] mixin self-check{}: {}/{} in place, missing={}", new Object[]{side, total - missing.size(), total, missing});
         }
      }
   }

   private static boolean present(MixinCheck.Entry entry) {
      try {
         Class<?> owner = Class.forName(entry.owner);

         for (Method method : owner.getDeclaredMethods()) {
            if (method.getName().equals(entry.method)) {
               return true;
            }
         }

         return false;
      } catch (Throwable t) {
         return false;
      }
   }

   private static final class Entry {
      private final String label;
      private final String owner;
      private final String method;
      private final boolean clientOnly;

      private Entry(String label, String owner, String method, boolean clientOnly) {
         this.label = label;
         this.owner = owner;
         this.method = method;
         this.clientOnly = clientOnly;
      }
   }
}

