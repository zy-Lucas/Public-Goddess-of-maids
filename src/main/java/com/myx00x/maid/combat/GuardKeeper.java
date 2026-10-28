package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.network.ModPackets;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class GuardKeeper {
   private static final Map<UUID, Long> LAST_HANDLE = new ConcurrentHashMap<>();
   private static final Map<String, Long> LAST_LOG = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> TROUBLED = new ConcurrentHashMap<>();
   private static final Field REMOVAL_MARK;
   private static final Field DEAD_FLAG;

   private GuardKeeper() {
   }

   public static boolean guards(EntityMaid maid) {
      if (!(Boolean)Config.GUARD_REWRITE.get()) {
         return false;
      } else {
         return maid != null && maid.level() != null && !maid.level().isClientSide ? GuardWatch.tierOf(maid) != 0 : false;
      }
   }

   public static void probe(EntityMaid maid, String what, Object detail) {
      if (note(maid, "probe-" + what)) {
         ShenlinMod.LOGGER.info("[shenlin] probe {} maid={} detail={}", new Object[]{what, maid.getUUID().toString().substring(0, 8), detail});
      }
   }

   public static void noteTrouble(EntityMaid maid) {
      if (maid != null && !maid.level().isClientSide) {
         TROUBLED.put(maid.getUUID(), maid.level().getGameTime());
      }
   }

   public static boolean troubledRecently(UUID maidId, long now, long window) {
      if (maidId == null) {
         return false;
      }

      Long at = TROUBLED.get(maidId);
      return at != null && now - at <= window;
   }

   public static long troubleStamp(UUID maidId) {
      if (maidId == null) {
         return Long.MIN_VALUE;
      }

      Long at = TROUBLED.get(maidId);
      return at == null ? Long.MIN_VALUE : at;
   }

   public static boolean onLethal(EntityMaid maid) {
      if (!guards(maid)) {
         return false;
      } else {
         noteTrouble(maid);
         int tier = GuardWatch.tierOf(maid);
         long now = maid.level().getGameTime();
         Long last = LAST_HANDLE.get(maid.getUUID());
         if (last != null && last == now) {
            settle(maid, tier);
            return true;
         } else {
            LAST_HANDLE.put(maid.getUUID(), now);
            if (!GuardTier.locksAtZero(tier)) {
               store(maid, "zero");
               return true;
            } else {
               pin(maid, tier);
               return true;
            }
         }
      }
   }

   public static boolean onForeignRemove(EntityMaid maid, RemovalReason reason) {
      if (!guards(maid)) {
         return false;
      } else {
         probe(maid, "remove-foreign", reason);
         noteTrouble(maid);
         int tier = GuardWatch.tierOf(maid);
         if (GuardTier.rejectsRemoval(tier)) {
            settle(maid, tier);
            note(maid, "removal-held");
            return true;
         } else {
            store(maid, "removal");
            return true;
         }
      }
   }

   public static boolean wouldHold(EntityMaid maid, RemovalReason reason) {
      if (!guards(maid)) {
         return false;
      } else if (GuardWatch.isVanishableFresh(maid.getUUID())) {
         return false;
      } else if (isLoadingChurn(maid, reason)) {
         return false;
      } else {
         return isChurnReason(reason) && beyondSight(maid) ? false : GuardTier.rejectsRemoval(GuardWatch.tierOf(maid));
      }
   }

   public static boolean isLoadingChurn(EntityMaid maid, RemovalReason reason) {
      return !isChurnReason(reason) ? false : !GuardTier.rejectsRemoval(GuardWatch.tierOf(maid));
   }

   public static boolean letGo(EntityMaid maid, RemovalReason reason) {
      if (!(Boolean)Config.GUARD_REWRITE.get()) {
         return false;
      }

      if (maid != null && maid.level() != null) {
         if ((reason == null || !reason.shouldDestroy()) && maid.level() instanceof ServerLevel level && !level.hasChunkAt(maid.blockPosition())) {
            if (ChunkAnchor.holds(maid)) {
               probe(maid, "anchor-lost-ground", reason);
            }

            return true;
         } else if (GuardRemoval.chunkUnloading(maid)) {
            if (ChunkAnchor.holds(maid)) {
               probe(maid, "anchor-chunk-unload", reason);
            }

            return true;
         } else if (reason == RemovalReason.CHANGED_DIMENSION && GuardRemoval.dimensionMoving(maid)) {
            return true;
         } else {
            return GuardWatch.isVanishableFresh(maid.getUUID()) ? true : GuardRemoval.consumePermit(maid);
         }
      } else {
         return true;
      }
   }

   public static boolean isChurnReason(RemovalReason reason) {
      return reason == RemovalReason.UNLOADED_TO_CHUNK || reason == RemovalReason.UNLOADED_WITH_PLAYER || reason == RemovalReason.CHANGED_DIMENSION;
   }

   public static boolean beyondSight(EntityMaid maid) {
      if (maid.level() instanceof ServerLevel level) {
         double var6 = 25600.0;

         for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() == maid.level() && player.distanceToSqr(maid) < var6) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean godremove(EntityMaid maid, String why) {
      if (maid != null && maid.level() != null && !maid.level().isClientSide) {
         GuardVitals.clear(maid);
         return vanish(maid, why);
      } else {
         return false;
      }
   }

   public static boolean packInto(EntityMaid maid) {
      if (maid != null && maid.level() != null && !maid.level().isClientSide) {
         refillForStore(maid);

         try {
            return MaidSlabber.pack(maid);
         } catch (Throwable t) {
            ShenlinMod.LOGGER.warn("[shenlin] maid {} pack failed: {}", maid.getUUID(), t.getMessage());
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean takeByOwner(EntityMaid maid) {
      if (maid == null || maid.level() == null || maid.level().isClientSide) {
         return false;
      } else if (!packInto(maid)) {
         refillSkin(maid);
         GuardVitals.restore(maid);
         ShenlinMod.LOGGER.warn("[shenlin] maid {} could not be packed, staying", maid.getUUID());
         return false;
      } else {
         return godremove(maid, "owner");
      }
   }

   public static void collectFeedback(Player player, ItemStack hand, EntityMaid maid) {
      if (maid.level() instanceof ServerLevel level && player != null) {
         level.playSound(
            null,
            maid.getX(),
            maid.getY(),
            maid.getZ(),
            SoundEvents.ENDERMAN_TELEPORT,
            SoundSource.NEUTRAL,
            1.0F,
            level.getRandom().nextFloat() * 0.1F + 0.9F
         );
         if (hand != null && !hand.isEmpty()) {
            hand.shrink(1);
         }
      }
   }

   private static void pin(EntityMaid maid, int tier) {
      setSkin(maid, 1.0F);
      GuardClean.skin(maid);
      if (note(maid, "pinned-" + tier)) {
         ShenlinMod.LOGGER.info("[shenlin] maid {} pinned at last drop, tier {}", maid.getUUID(), tier);
      }
   }

   private static void settle(EntityMaid maid, int tier) {
      if (GuardTier.locksAtZero(tier)) {
         keepPinned(maid);
      }

      GuardClean.skin(maid);
   }

   public static void onSkinSettled(EntityMaid maid) {
      if (guards(maid)) {
         if (GuardTier.locksAtZero(GuardWatch.tierOf(maid))) {
            keepPinned(maid);
         }
      }
   }

   private static void setSkin(EntityMaid maid, float value) {
      try {
         Executioner.putSkin(maid, value);
         maid.deathTime = 0;
         maid.hurtTime = 0;
         maid.fallDistance = 0.0F;
         if (note(maid, "skin-write")) {
            AttributeInstance attribute = maid.getAttribute(Attributes.MAX_HEALTH);
            ShenlinMod.LOGGER
               .info(
                  "[shenlin] maid {} skin write {} now={} synced={} max={} base={}",
                  new Object[]{
                     maid.getUUID(), value, maid.getHealth(), Executioner.syncedHealth(maid), maid.getMaxHealth(), attribute == null ? -1.0 : attribute.getBaseValue()
                  }
               );
         }
      } catch (Throwable t) {
         ShenlinMod.LOGGER.warn("[shenlin] maid {} skin write failed: {}", maid.getUUID(), t.getMessage());
      }
   }

   private static void refillSkin(EntityMaid maid) {
      setSkin(maid, GuardVitals.ceilingOf(maid));
   }

   public static void noZeroSkin(EntityMaid maid) {
      if (!maid.level().isClientSide) {
         if (!(maid.getHealth() > 0.0F)) {
            int tier = GuardWatch.tierOf(maid);
            if (GuardTier.locksAtZero(tier)) {
               setSkin(maid, 1.0F);
            } else {
               setSkin(maid, GuardVitals.ceilingOf(maid));
            }
         }
      }
   }

   public static void onZeroSkin(EntityMaid maid) {
      if (guards(maid)) {
         if (GuardTier.locksAtZero(GuardWatch.tierOf(maid))) {
            setSkin(maid, 1.0F);
         } else {
            store(maid, "zero");
         }
      }
   }

   public static void onUnpacked(EntityMaid maid) {
      if (!maid.level().isClientSide) {
         if (!GuardWatch.onRoster(maid)) {
            ShenlinMod.LOGGER.warn("[shenlin] maid {} unpacked but not on roster, brand kept", maid.getUUID());
         } else {
            GuardWatch.clearVanishable(maid.getUUID());
            GuardRemoval.clear(maid.getUUID());
            clearRemovalMark(maid);
            if (maid.getHealth() <= 0.0F || maid.isDeadOrDying()) {
               setSkin(maid, GuardVitals.ceilingOf(maid));
            }

            GuardVitals.enroll(maid);
            ShenlinMod.LOGGER
               .info(
                  "[shenlin] maid {} out of slab, health={} max={} invisible={} removed={}",
                  new Object[]{maid.getUUID(), maid.getHealth(), maid.getMaxHealth(), maid.isInvisible(), maid.isRemoved()}
               );
         }
      }
   }

   public static void scrub(EntityMaid maid) {
      if (maid != null && maid.level() != null && !maid.level().isClientSide) {
         if (guards(maid)) {
            clearRemovalMark(maid);
            if (maid.deathTime != 0) {
               maid.deathTime = 0;
            }

            if (maid.hurtTime != 0) {
               maid.hurtTime = 0;
            }

            if (DEAD_FLAG != null) {
               try {
                  if (DEAD_FLAG.getBoolean(maid)) {
                     DEAD_FLAG.setBoolean(maid, false);
                  }
               } catch (Throwable var2) {
               }
            }
         }
      }
   }

   public static void clearRemovalMark(EntityMaid maid) {
      try {
         if (!maid.isRemoved()) {
            return;
         }

         RemovalReason reason = maid.getRemovalReason();
         if (reason != null && isChurnReason(reason)) {
            return;
         }

         Object mark = REMOVAL_MARK.get(maid);
         if (mark == null) {
            return;
         }

         REMOVAL_MARK.set(maid, null);
         if (note(maid, "mark-cleared")) {
            ShenlinMod.LOGGER.info("[shenlin] maid {} removal mark cleared", maid.getUUID());
         }
      } catch (Throwable t) {
         note(maid, "mark-clear-failed");
         ShenlinMod.LOGGER.warn("[shenlin] maid {} mark clear failed: {}", maid.getUUID(), t.getMessage());
      }
   }

   private static Field findDeadFlag() {
      try {
         Class<?> living = Class.forName("net.minecraft.world.entity.LivingEntity");

         for (String name : new String[]{"f_20890_", "dead"}) {
            try {
               Field found = living.getDeclaredField(name);
               found.setAccessible(true);
               return found;
            } catch (Throwable var6) {
            }
         }
      } catch (Throwable var7) {
      }

      return null;
   }

   public static void refillForStore(EntityMaid maid) {
      if (!maid.level().isClientSide) {
         if (maid.getHealth() <= 0.0F || maid.isDeadOrDying()) {
            setSkin(maid, GuardVitals.ceilingOf(maid));
         }
      }
   }

   public static boolean purge(EntityMaid maid, String why) {
      if (maid != null && maid.level() != null && !maid.level().isClientSide) {
         boolean gone = vanish(maid, why);
         ShenlinMod.LOGGER.warn("[shenlin] maid {} purged by {} removed={}", new Object[]{maid.getUUID(), why, gone});
         return gone;
      } else {
         return false;
      }
   }

   private static boolean vanish(EntityMaid maid, String why) {
      UUID id = maid.getUUID();
      ModPackets.sendGuardPass(maid.getId());
      GuardWatch.markVanishable(id);
      GuardBook.expectGone(id);
      GuardWatch.drop(id);
      GuardRemoval.forceRemove(maid);
      GuardVitals.forget(maid);
      if (note(maid, "gone-" + why)) {
         ShenlinMod.LOGGER.info("[shenlin] maid {} into slab by {}", id, why);
      }

      return true;
   }

   private static boolean store(EntityMaid maid, String why) {
      if (!packInto(maid)) {
         refillSkin(maid);
         GuardVitals.restore(maid);
         ShenlinMod.LOGGER.warn("[shenlin] maid {} could not be packed, staying", maid.getUUID());
         return false;
      } else {
         return vanish(maid, why);
      }
   }

   public static void onGuardedTick(EntityMaid maid) {
      if (guards(maid)) {
         GuardVitals.reconnect(maid);
         GuardWatch.keep(maid);
         scrub(maid);
         ChunkAnchor.ensure(maid);
         if (maid.isInvisible()) {
            maid.setInvisible(false);
         }

         int tier = GuardWatch.tierOf(maid);
         if (GuardVitals.skinOf(maid) <= 0.0F) {
            onZeroSkin(maid);
         } else if (GuardVitals.overCeiling(maid)) {
            noteTrouble(maid);
            setSkin(maid, GuardTier.locksAtZero(tier) ? 1.0F : GuardVitals.ceilingOf(maid));
            if (note(maid, "skin-over-fixed")) {
               ShenlinMod.LOGGER.info("[shenlin] maid {} skin over ceiling, pulled back (tier {})", maid.getUUID(), tier);
            }
         } else if (GuardTier.locksAtZero(tier)) {
            keepPinned(maid);
         } else {
            if (maid.deathTime != 0 || maid.hurtTime != 0) {
               settle(maid, tier);
            }
         }
      }
   }

   private static void keepPinned(EntityMaid maid) {
      float hp = GuardVitals.skinOf(maid);
      if (!(hp > 1.0F)) {
         if (hp <= 0.0F || maid.isDeadOrDying()) {
            setSkin(maid, 1.0F);
         }
      }
   }

   public static void forget(UUID maidId) {
      LAST_HANDLE.remove(maidId);
      String prefix = maidId + "|";
      LAST_LOG.keySet().removeIf(key -> key.startsWith(prefix));
      TROUBLED.remove(maidId);
      GuardWatch.drop(maidId);
      GuardWatch.clearVanishable(maidId);
   }

   public static void sweep(long now) {
      TROUBLED.entrySet().removeIf(e -> now - e.getValue() > 1200L);
      LAST_HANDLE.entrySet().removeIf(e -> now - e.getValue() > 1200L);
   }

   private static boolean note(EntityMaid maid, String what) {
      long now = maid.level().getGameTime();
      String key = maid.getUUID() + "|" + what;
      Long last = LAST_LOG.get(key);
      if (last != null && now - last < 100L) {
         return false;
      }

      if (LAST_LOG.size() > 64) {
         LAST_LOG.entrySet().removeIf(e -> now - e.getValue() > 1200L);
      }

      LAST_LOG.put(key, now);
      return true;
   }

   static {
      Field found = null;

      try {
         Class<?> entity = Class.forName("net.minecraft.world.entity.Entity");

         for (Field field : entity.getDeclaredFields()) {
            Class<?> type = field.getType();
            if (type.isEnum() && type.getEnclosingClass() == entity) {
               field.setAccessible(true);
               found = field;
               break;
            }
         }
      } catch (Throwable t) {
         found = null;
      }

      REMOVAL_MARK = found;
      DEAD_FLAG = findDeadFlag();
   }
}

