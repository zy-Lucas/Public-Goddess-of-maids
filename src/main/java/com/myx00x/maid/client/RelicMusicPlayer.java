package com.myx00x.maid.client;

import com.myx00x.maid.mob.RelicWardenEntity;
import com.myx00x.maid.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT, bus = Bus.FORGE)
public final class RelicMusicPlayer {
   private static final float VOLUME = 1.0F;
   private static final long RETRY_MS = 500L;
   private static final long RETRY_MAX_MS = 4000L;
   private static final int FADE_TICKS = 40;
   private static final int FADE_IN_TICKS = 16;
   private static final float FADE_IN_FLOOR = 0.05F;
   private static final int MISS_TOLERANCE = 40;
   private static double battleRange = 66.0;
   private static final RandomSource RANDOM = RandomSource.create();
   private static RelicMusicPlayer.Track ambient;
   private static boolean inside;
   private static double gapMin = 11.0;
   private static double gapMax = 33.0;
   private static long ambientReadyAt;
   private static long ambientRetriedAt;
   private static long ambientRetryGap = 500L;
   private static RelicMusicPlayer.Track battle;
   private static int battleMisses;
   private static long battleRetriedAt;
   private static long battleRetryGap = 500L;

   private RelicMusicPlayer() {
   }

   public static void setInside(boolean inside, double gapMin, double gapMax, double range) {
      boolean changed = inside != RelicMusicPlayer.inside;
      RelicMusicPlayer.inside = inside;
      RelicMusicPlayer.gapMin = gapMin;
      RelicMusicPlayer.gapMax = gapMax;
      battleRange = range;
      if (changed && !inside) {
         ambientReadyAt = 0L;
      }
   }

   public static boolean silencing() {
      return inside || battle != null;
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.END) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.getSoundManager() != null && mc.level != null && mc.player != null) {
            boolean fighting = wardenFighting(mc);
            tickBattle(mc, fighting);
            tickAmbient(mc, inside && !fighting && !BossMusicPlayer.bossNearby());
         } else {
            forget();
         }
      }
   }

   private static void tickBattle(Minecraft mc, boolean fighting) {
      if (!fighting) {
         if (battleMisses < 40) {
            battleMisses++;
         } else {
            if (battle != null) {
               battle.beginFade();
            }
         }
      } else {
         battleMisses = 0;
         if (battle != null && !battle.isStopped()) {
            battle.cancelFade();
            battleRetryGap = 500L;
         } else if (!(mc.options.getSoundSourceVolume(SoundSource.MUSIC) <= 0.0F)) {
            long now = System.currentTimeMillis();
            if (battle == null || now - battleRetriedAt >= battleRetryGap) {
               battleRetriedAt = now;
               battleRetryGap = Math.min(battleRetryGap * 2L, 4000L);
               RelicMusicPlayer.Track fresh = new RelicMusicPlayer.Track((SoundEvent)ModSounds.WARDEN_BATTLE.get(), true);
               battle = fresh;
               mc.getSoundManager().play(fresh);
            }
         }
      }
   }

   private static void tickAmbient(Minecraft mc, boolean may) {
      if (!may) {
         if (ambient != null) {
            ambient.beginFade();
         }
      } else {
         OriginalMusicFader.request(mc);
         if (ambient != null) {
            if (!ambient.isStopped()) {
               ambientRetryGap = 500L;
            } else {
               ambient = null;
               ambientReadyAt = System.currentTimeMillis() + gapMillis();
            }
         } else if (!(mc.options.getSoundSourceVolume(SoundSource.MUSIC) <= 0.0F)) {
            long now = System.currentTimeMillis();
            if (now >= ambientReadyAt) {
               if (now - ambientRetriedAt >= ambientRetryGap) {
                  ambientRetriedAt = now;
                  ambientRetryGap = Math.min(ambientRetryGap * 2L, 4000L);
                  RelicMusicPlayer.Track fresh = new RelicMusicPlayer.Track((SoundEvent)ModSounds.RELIC_MUSIC_1.get(), false);
                  ambient = fresh;
                  mc.getSoundManager().play(fresh);
               }
            }
         }
      }
   }

   private static long gapMillis() {
      double low = Math.max(0.0, gapMin);
      double high = Math.max(low, gapMax);
      return (long)(Mth.nextDouble(RANDOM, low, high) * 1000.0);
   }

   private static boolean wardenFighting(Minecraft mc) {
      ClientLevel level = mc.level;
      Player self = mc.player;
      if (level != null && self != null) {
         double reach = battleRange * battleRange;

         for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof RelicWardenEntity warden && !warden.isDormant() && warden.battleBegan() && warden.distanceToSqr(self) <= reach) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static void forget() {
      ambient = null;
      battle = null;
      inside = false;
      ambientReadyAt = 0L;
      battleMisses = 0;
   }

   private static void clear(RelicMusicPlayer.Track who) {
      if (ambient == who) {
         ambient = null;
      }

      if (battle == who) {
         battle = null;
      }
   }

   private static final class Track extends AbstractTickableSoundInstance {
      private int fade = -1;
      private int warmup;

      private Track(SoundEvent event, boolean loop) {
         super(event, SoundSource.MUSIC, RandomSource.create());
         this.attenuation = Attenuation.NONE;
         this.relative = false;
         this.looping = loop;
         this.volume = 0.05F;
         this.pitch = 1.0F;
         this.warmup = 16;
      }

      void beginFade() {
         if (this.fade < 0) {
            this.fade = 40;
         }
      }

      void cancelFade() {
         if (this.fade >= 0) {
            this.fade = -1;
            this.volume = 1.0F;
         }
      }

      public void tick() {
         if (this.warmup > 0) {
            this.warmup--;
            this.volume = 1.0F * (0.05F + 0.95F * (1.0F - this.warmup / 16.0F));
         }

         if (this.fade >= 0) {
            this.fade--;
            if (this.fade <= 0) {
               this.stop();
               RelicMusicPlayer.clear(this);
            } else {
               this.volume = 1.0F * (this.fade / 40.0F);
            }
         }
      }
   }
}

