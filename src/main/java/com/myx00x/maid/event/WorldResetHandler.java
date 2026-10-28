package com.myx00x.maid.event;

import com.myx00x.maid.boss.RuinedPhantasmKeeper;
import com.myx00x.maid.boss.ShenlinBossKeeper;
import com.myx00x.maid.combat.BlackHoleKeeper;
import com.myx00x.maid.combat.ChunkAnchor;
import com.myx00x.maid.combat.ExecutionSequencer;
import com.myx00x.maid.combat.GuardBook;
import com.myx00x.maid.combat.GuardWatch;
import com.myx00x.maid.combat.HoverCannon;
import com.myx00x.maid.combat.ShieldWatcher;
import com.myx00x.maid.combat.SlabAudit;
import com.myx00x.maid.combat.SlashPulse;
import com.myx00x.maid.combat.TargetKeeper;
import com.myx00x.maid.combat.TimeKeeper;
import com.myx00x.maid.combat.WormholeRecall;
import com.myx00x.maid.compat.Upstream;
import com.myx00x.maid.world.RelicOpening;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.LevelEvent.Unload;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class WorldResetHandler {
   private WorldResetHandler() {
   }

   @SubscribeEvent
   public static void onLevelUnload(Unload event) {
      if (!event.getLevel().isClientSide()) {
         if (event.getLevel() instanceof ServerLevel level && level.dimension().equals(Level.OVERWORLD)) {
            forget();
         }
      }
   }

   @SubscribeEvent
   public static void onServerStopped(ServerStoppedEvent event) {
      forget();
   }

   private static void forget() {
      if (Upstream.MAID) {
         HoverCannon.forgetAll();
         BlackHoleKeeper.forgetAll();
         ExecutionSequencer.dropAll();
         GuardWatch.forgetAll();
         TimeKeeper.forgetAll();
         ShieldWatcher.forgetAll();
         TargetKeeper.forgetAll();
         SlashPulse.forgetAll();
         WormholeRecall.forgetAll();
         SlabAudit.forgetAll();
         ChunkAnchor.forgetAll();
         GuardBook.forgetAll();
         MaidAttackListener.forgetAll();
         RelicOpening.forgetAll();
      }

      ShenlinBossKeeper.clear();
      RuinedPhantasmKeeper.clear();
      ServerTickGuard.reset();
   }
}

