package com.myx00x.maid.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class SoundBroadcast {
   private static final int RANGE_CHUNKS = 32;
   public static final int SLASH_CHUNKS = 11;
   private static final int SETTLE_TICKS = 40;

   private SoundBroadcast() {
   }

   public static void play(ServerLevel level, Vec3 pos, SoundEvent sound) {
      play(level, pos, sound, 32);
   }

   public static void play(ServerLevel level, Vec3 pos, SoundEvent sound, int chunks) {
      double range = chunks * 16.0;
      double limit = range * range;

      for (Player p : level.players()) {
         if (p.tickCount >= 40 && p.distanceToSqr(pos.x, pos.y, pos.z) <= limit) {
            ((ServerPlayer)p).playNotifySound(sound, SoundSource.PLAYERS, 1.0F, 1.0F);
         }
      }
   }
}

