package com.myx00x.maid.client;

import com.google.common.collect.Multimap;
import com.mojang.blaze3d.audio.Channel;
import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.mixin.SoundManagerAccessor;
import com.myx00x.maid.registry.ModSounds;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT, bus = Bus.FORGE)
public final class BossMusicPlayer {
   private static final float VOLUME = 1.0F;
   private static final long RETRY_MS = 500L;
   private static final long RETRY_MAX_MS = 4000L;
   private static final int FADE_TICKS = 60;
   private static final int FADE_IN_TICKS = 16;
   private static final float FADE_IN_FLOOR = 0.05F;
   private static final double THEME_RANGE = 128.0;
   private static BossMusicPlayer.Track track;
   private static long retriedAt;
   private static long retryGap = 500L;
   private static boolean wasNearby;

   private BossMusicPlayer() {
   }

   public static boolean isTheme(SoundInstance sound) {
      return track != null && sound == track;
   }

   public static boolean bossNearby() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null || mc.player == null) {
         return false;
      }

      if (!(Boolean)Config.BOSS_THEME_ENABLED.get()) {
         return false;
      }

      boolean here = false;

      for (int id : BossShellGuard.liveIds()) {
         double[] spot = BossShellGuard.spotStale(id);
         if (spot != null && mc.player.distanceToSqr(spot[0], spot[1], spot[2]) <= 16384.0) {
            here = true;
            break;
         }
      }

      if (here != wasNearby) {
         wasNearby = here;
         ShenlinMod.LOGGER.info("[shenlin] boss theme {}", here ? "gained" : "lost");
      }

      return here;
   }

   public static boolean keepThroughStopAll() {
      Minecraft mc = Minecraft.getInstance();
      SoundInstance keep = track;
      if (keep != null && mc.getSoundManager() != null) {
         SoundEngine engine = ((SoundManagerAccessor)mc.getSoundManager()).shenlin.engine();
         if (engine == null) {
            return false;
         }

         if (read(engine, "f_120226_") instanceof Map<?, ?> live) {
            Object keepHandle = live.get(keep);
            if (keepHandle == null) {
               return false;
            }

            Object access = read(engine, "f_120224_");
            Object pool = read(access, "f_120121_");
            boolean lifted = false;
            if (pool instanceof Set<Object> raw) {
               lifted = raw.remove(keepHandle);
            }

            try {
               Iterator<? extends Entry<?, ?>> it = live.entrySet().iterator();

               while (it.hasNext()) {
                  Entry<?, ?> e = (Entry<?, ?>)it.next();
                  if (e.getKey() != keep) {
                     Object handle = e.getValue();
                     if (handle != null) {
                        try {
                           Method stop = handle.getClass().getMethod("m_120154_", Consumer.class);
                           stop.invoke(handle, Channel::m_83679_);
                        } catch (Throwable var17) {
                        }
                     }

                     it.remove();
                  }
               }

               if (access != null) {
                  Method releaseAll = access.getClass().getMethod("m_120139_");
                  releaseAll.invoke(access);
               }
            } catch (Throwable var18) {
            } finally {
               if (lifted && pool instanceof Set<Object> raw) {
                  raw.add(keepHandle);
               }
            }

            if (read(engine, "f_120228_") instanceof List<?> tickable) {
               removeOthers(tickable, keep);
            }

            if (read(engine, "f_120227_") instanceof Multimap<?, ?> bySound) {
               Multimap<Object, Object> map = (Multimap<Object, Object>)bySound;
               map.clear();
               map.put(keep.getSound(), keep);
            }

            clearIfPresent(engine, "f_120229_");
            clearIfPresent(engine, "f_120230_");
            clearIfPresent(engine, "f_120232_");
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static Object read(Object owner, String name) {
      if (owner == null) {
         return null;
      }

      try {
         Field f = owner.getClass().getDeclaredField(name);
         f.setAccessible(true);
         return f.get(owner);
      } catch (Throwable t) {
         return null;
      }
   }

   private static void removeOthers(List<?> list, Object keep) {
      try {
         List<Object> all = (List<Object>)list;
         all.removeIf(o -> o != keep);
      } catch (Throwable var3) {
      }
   }

   private static void clearIfPresent(Object owner, String name) {
      if (read(owner, name) instanceof Collection<?> c) {
         try {
            c.clear();
         } catch (Throwable var4) {
         }
      }
   }

   public static void stop() {
      if (track != null) {
         track.beginFade();
      }
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.END) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.getSoundManager() == null || mc.level == null) {
            track = null;
            wasNearby = false;
         } else if (!bossNearby()) {
            if (track != null) {
               track.beginFade();
            }
         } else {
            if (track != null) {
               track.cancelFade();
            }

            OriginalMusicFader.request(mc);
            if (track != null && mc.getSoundManager().isActive(track)) {
               retryGap = 500L;
            } else {
               long now = System.currentTimeMillis();
               if (track == null || now - retriedAt >= retryGap) {
                  retriedAt = now;
                  retryGap = track == null ? 500L : Math.min(retryGap * 2L, 4000L);
                  boolean wasSilent = track == null;
                  BossMusicPlayer.Track fresh = new BossMusicPlayer.Track((SoundEvent)ModSounds.BOSS_THEME.get());
                  track = fresh;
                  mc.getSoundManager().play(fresh);
                  if (wasSilent) {
                     ShenlinMod.LOGGER.info("[shenlin] boss theme started");
                  }
               }
            }
         }
      }
   }

   private static final class Track extends AbstractTickableSoundInstance {
      private int fade = -1;
      private int warmup;

      private Track(SoundEvent event) {
         super(event, SoundSource.MASTER, RandomSource.create());
         this.attenuation = Attenuation.NONE;
         this.relative = false;
         this.looping = false;
         this.volume = 0.05F;
         this.pitch = 1.0F;
         this.warmup = 16;
      }

      void beginFade() {
         if (this.fade < 0) {
            this.fade = 60;
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
               if (BossMusicPlayer.track == this) {
                  BossMusicPlayer.track = null;
               }
            } else {
               this.volume = 1.0F * (this.fade / 60.0F);
            }
         }
      }
   }
}

