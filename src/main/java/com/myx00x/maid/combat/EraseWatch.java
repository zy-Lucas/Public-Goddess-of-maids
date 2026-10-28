package com.myx00x.maid.combat;

import com.myx00x.maid.ShenlinMod;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

public final class EraseWatch {
   private static final long BAN_CLASS_MS = 10000L;
   private static final long RECHECK_WINDOW_MS = 30000L;
   private static final long RECHECK_STEP_MS = 500L;
   private static final Map<String, Long> BANNED = new ConcurrentHashMap<>();
   private static final Map<String, EraseWatch.Watch> WATCHES = new ConcurrentHashMap<>();
   private static long lastSweep;

   private EraseWatch() {
   }

   public static void register(LivingEntity target) {
      if (target != null) {
         long now = System.currentTimeMillis();

         try {
            String cls = target.getClass().getName();
            BANNED.put(cls, now + 10000L);
         } catch (Throwable var5) {
         }

         try {
            String typeId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType()) == null
               ? "-"
               : ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).toString();
            WATCHES.put(target.getStringUUID(), new EraseWatch.Watch(target.getStringUUID(), target.getClass().getName(), typeId, now + 30000L));
         } catch (Throwable t) {
            ShenlinMod.LOGGER.warn("[shenlin] erase watch register failed: {}", t.toString());
         }
      }
   }

   public static boolean banned(String className) {
      if (className == null) {
         return false;
      } else {
         Long until = BANNED.get(className);
         if (until == null) {
            return false;
         } else if (System.currentTimeMillis() > until) {
            BANNED.remove(className);
            return false;
         } else {
            return true;
         }
      }
   }

   public static boolean onEntityJoined(Entity entity) {
      if (entity instanceof LivingEntity living) {
         String cls = living.getClass().getName();
         String id = living.getStringUUID();
         boolean byClass = banned(cls);
         boolean byUuid = WATCHES.containsKey(id);
         if (!byClass && !byUuid) {
            return false;
         }

         ShenlinMod.LOGGER.info("[shenlin] erase watch: {} rejoined (class={} uuid={}), erasing again", new Object[]{cls, byClass, byUuid});
         return Annihilator.erase(living);
      } else {
         return false;
      }
   }

   public static void tick(MinecraftServer server) {
      long now = System.currentTimeMillis();
      if (now - lastSweep >= 500L) {
         lastSweep = now;
         if (server != null) {
            BANNED.entrySet().removeIf(ex -> now > (Long)ex.getValue());
            if (!WATCHES.isEmpty()) {
               Iterator<Entry<String, EraseWatch.Watch>> it = WATCHES.entrySet().iterator();
               List<LivingEntity> again = new ArrayList<>();

               while (it.hasNext()) {
                  EraseWatch.Watch w = it.next().getValue();
                  if (now > w.until()) {
                     it.remove();
                  } else {
                     LivingEntity back = find(server, w.uuid());
                     if (back != null) {
                        again.add(back);
                     }
                  }
               }

               for (LivingEntity e : again) {
                  ShenlinMod.LOGGER.info("[shenlin] erase watch: {} came back, erasing again", e.getStringUUID());
                  Annihilator.erase(e);
               }
            }
         }
      }
   }

   private static LivingEntity find(MinecraftServer server, String uuid) {
      try {
         UUID id = UUID.fromString(uuid);
         Entity e = null;

         for (ServerLevel lv : server.getAllLevels()) {
            e = lv.getEntity(id);
            if (e != null) {
               break;
            }
         }

         return e instanceof LivingEntity living ? living : null;
      } catch (Throwable t) {
         return null;
      }
   }

   public static int watching() {
      return WATCHES.size();
   }

   public static int banned() {
      return BANNED.size();
   }

   private record Watch(String uuid, String className, String typeId, long until) {
   }
}

