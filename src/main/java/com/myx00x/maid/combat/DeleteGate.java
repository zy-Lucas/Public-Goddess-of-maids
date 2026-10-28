package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.compat.Upstream;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import java.lang.reflect.Field;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class DeleteGate {
   private static final String TARGET = "com.mega.uom.common.network.s2c.entity.DeleteEntityPacket";
   private static final String FIELD = "idOfUser";
   private static Field idField;
   private static boolean probed;
   private static boolean announced;

   private DeleteGate() {
   }

   private static boolean holdsForeignDelete(Object message) {
      if (!Upstream.MAID) {
         return false;
      }

      if (message != null && "com.mega.uom.common.network.s2c.entity.DeleteEntityPacket".equals(message.getClass().getName())) {
         int id = idOf(message);
         if (id < 0) {
            return false;
         }

         EntityMaid maid = find(id);
         if (maid == null) {
            return false;
         }

         boolean hold = GuardKeeper.wouldHold(maid, RemovalReason.UNLOADED_TO_CHUNK);
         if (hold && !announced) {
            announced = true;
            ShenlinMod.LOGGER.info("[shenlin] gate delete held {}", id);
         }

         return hold;
      } else {
         return false;
      }
   }

   private static EntityMaid find(int id) {
      MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
      if (server == null) {
         return null;
      }

      for (ServerLevel level : server.getAllLevels()) {
         if (level.getEntity(id) instanceof EntityMaid maid) {
            return maid;
         }
      }

      return null;
   }

   public static boolean holdsRemoval(Object message) {
      if (message instanceof ClientboundRemoveEntitiesPacket packet) {
         IntListIterator var2 = packet.getEntityIds().iterator();

         while (var2.hasNext()) {
            int id = (Integer)var2.next();
            EntityMaid maid = find(id);
            if (maid != null && !maid.isRemoved() && !GuardWatch.isVanishableFresh(maid.getUUID()) && GuardKeeper.wouldHold(maid, RemovalReason.DISCARDED)) {
               if (!announced) {
                  announced = true;
                  ShenlinMod.LOGGER.info("[shenlin] gate removal packet held {}", id);
               }

               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static boolean holdsAny(Object message) {
      return holdsRemoval(message) || holdsForeignDelete(message);
   }

   private static int idOf(Object message) {
      if (!probed) {
         probed = true;

         try {
            idField = message.getClass().getDeclaredField("idOfUser");
            idField.setAccessible(true);
         } catch (Throwable t) {
            idField = null;
            ShenlinMod.LOGGER.warn("[shenlin] delete packet field {} not found on {}, gate idle", "idOfUser", message.getClass().getName());
         }
      }

      if (idField == null) {
         return -1;
      }

      try {
         return (Integer)idField.get(message);
      } catch (Throwable t) {
         return -1;
      }
   }
}

