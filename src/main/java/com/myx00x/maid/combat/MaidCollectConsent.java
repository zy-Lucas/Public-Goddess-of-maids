package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.ShenlinMod;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.player.Player;

public final class MaidCollectConsent {
   private static final long TTL_TICKS = 2L;
   private static final Map<UUID, MaidCollectConsent.Consent> CONSENTS = new ConcurrentHashMap<>();

   private MaidCollectConsent() {
   }

   public static void record(Player player, EntityMaid maid) {
      if (player != null && maid != null && !maid.level().isClientSide) {
         UUID owner = maid.getOwnerUUID();
         if (owner != null && owner.equals(player.getUUID())) {
            CONSENTS.put(maid.getUUID(), new MaidCollectConsent.Consent(player.getUUID(), maid.level().getGameTime() + 2L));
         } else {
            ShenlinMod.LOGGER.info("[shenlin] collect by non-owner {} on maid {}", player.getUUID(), maid.getUUID());
         }
      }
   }

   public static boolean consume(EntityMaid maid) {
      if (maid == null) {
         return false;
      }

      UUID maidId = maid.getUUID();
      MaidCollectConsent.Consent consent = CONSENTS.remove(maidId);
      return consent == null ? false : maid.level().getGameTime() <= consent.expireAt;
   }

   public static void cleanup(long now) {
      Iterator<Entry<UUID, MaidCollectConsent.Consent>> it = CONSENTS.entrySet().iterator();

      while (it.hasNext()) {
         if (now > it.next().getValue().expireAt) {
            it.remove();
         }
      }
   }

   private static final class Consent {
      private final UUID playerId;
      private final long expireAt;

      private Consent(UUID playerId, long expireAt) {
         this.playerId = playerId;
         this.expireAt = expireAt;
      }
   }
}

