package com.myx00x.maid.client;

import com.myx00x.maid.mixin.ClientLevelPartAccessor;
import com.myx00x.maid.mob.RelicWardenEntity;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.entity.PartEntity;

public final class ClientWardenParts {
   private ClientWardenParts() {
   }

   public static void register(RelicWardenEntity warden) {
      if (warden.level() instanceof ClientLevel clientLevel) {
         Int2ObjectMap var7 = ((ClientLevelPartAccessor)clientLevel).shenlin.partEntities();

         for (PartEntity<?> part : warden.getParts()) {
            if (part != null) {
               var7.put(part.getId(), part);
            }
         }
      }
   }

   public static void unregister(RelicWardenEntity warden) {
      if (warden.level() instanceof ClientLevel clientLevel) {
         Int2ObjectMap var7 = ((ClientLevelPartAccessor)clientLevel).shenlin.partEntities();

         for (PartEntity<?> part : warden.getParts()) {
            if (part != null) {
               var7.remove(part.getId());
            }
         }
      }
   }
}

