package com.myx00x.maid.client;

import com.myx00x.maid.ShenlinMod;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT, bus = Bus.FORGE)
public final class GuardShield {
   private static final long PASS_COOLDOWN_MS = 5000L;
   private static final double FAR = 192.0;
   private static final double FAR_SQR = 36864.0;
   private static final Map<Integer, Long> HELD = new ConcurrentHashMap<>();
   private static final Map<Integer, Entity> KEPT = new ConcurrentHashMap<>();
   private static volatile long lastToldAt;
   private static final Map<Integer, Long> PASSED = new ConcurrentHashMap<>();
   private static final Map<Integer, Long> LAST_LOG = new ConcurrentHashMap<>();
   private static ClientLevel boundLevel;
   private static volatile boolean resolved;
   private static Field storageField;
   private static Method addEntityMethod;
   private static Field removalReasonField;
   private static Field tickListField;
   private static Method tickAddMethod;

   private GuardShield() {
   }

   public static void apply(int[] ids, int millis) {
      if (ids != null && ids.length != 0) {
         long now = System.currentTimeMillis();
         long until = now + Math.max(0, millis);

         for (int id : ids) {
            Long blocked = PASSED.get(id);
            if (blocked != null) {
               if (now < blocked) {
                  continue;
               }

               PASSED.remove(id);
            }

            HELD.put(id, until);
         }

         trim(HELD, now, 1024);
         if (now - lastToldAt > 2000L) {
            lastToldAt = now;
            ShenlinMod.LOGGER.info("[shenlin] client shield renewed, n={} hold={}ms", ids.length, millis);
         }
      }
   }

   public static void release(int id) {
      long now = System.currentTimeMillis();
      HELD.remove(id);
      KEPT.remove(id);
      PASSED.put(id, now + 5000L);
      trim(PASSED, now, 1024);
   }

   public static boolean holds(int id) {
      Long until = HELD.get(id);
      if (until == null) {
         return false;
      }

      long now = System.currentTimeMillis();
      if (now <= until && !tooFar(id)) {
         Long seen = LAST_LOG.get(id);
         if (seen == null || now - seen > 2000L) {
            LAST_LOG.put(id, now);
            ShenlinMod.LOGGER.info("[shenlin] client kept entity {} displayed, the removal was held off", id);
         }

         return true;
      } else {
         HELD.remove(id);
         return false;
      }
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.END) {
         sweep();
      }
   }

   private static void sweep() {
      Minecraft minecraft = Minecraft.getInstance();
      ClientLevel level = minecraft.level;
      LocalPlayer me = minecraft.player;
      if (level != null && me != null && !HELD.isEmpty()) {
         if (level != boundLevel) {
            boundLevel = level;
            HELD.clear();
            KEPT.clear();
            PASSED.clear();
            LAST_LOG.clear();
         } else {
            long now = System.currentTimeMillis();

            for (Entry<Integer, Long> entry : HELD.entrySet()) {
               if (now <= entry.getValue()) {
                  int id = entry.getKey();
                  Entity she = level.getEntity(id);
                  if (she != null) {
                     KEPT.put(id, she);
                     if (she.isRemoved()) {
                        clearRemoved(she);
                     }
                  } else {
                     Entity kept = KEPT.get(id);
                     if (kept != null) {
                        if (kept.level() != level) {
                           KEPT.remove(id);
                        } else if (!(kept.distanceToSqr(me) > 36864.0)) {
                           putBack(level, kept);
                        }
                     }
                  }
               }
            }

            if (KEPT.size() > 512) {
               KEPT.keySet().removeIf(key -> !HELD.containsKey(key));
            }
         }
      } else {
         KEPT.clear();
      }
   }

   private static void putBack(ClientLevel level, Entity entity) {
      resolve();

      try {
         clearRemoved(entity);
         Object storage = storageField.get(level);
         if (storage == null) {
            return;
         }

         addEntityMethod.invoke(storage, entity);
         backIntoTicks(level, entity);
         long now = System.currentTimeMillis();
         Long seen = LAST_LOG.get(entity.getId());
         if (seen == null || now - seen > 2000L) {
            LAST_LOG.put(entity.getId(), now);
            ShenlinMod.LOGGER.info("[shenlin] client put entity {} back on its own books", entity.getId());
         }
      } catch (Throwable var6) {
      }
   }

   private static void clearRemoved(Entity entity) {
      resolve();
      if (removalReasonField != null) {
         try {
            removalReasonField.set(entity, null);
         } catch (Throwable var2) {
         }
      }
   }

   private static boolean tooFar(int id) {
      Minecraft minecraft = Minecraft.getInstance();
      ClientLevel level = minecraft.level;
      LocalPlayer me = minecraft.player;
      if (level != null && me != null) {
         Entity she = level.getEntity(id);
         return she == null ? false : she.distanceToSqr(me) > 36864.0;
      } else {
         return true;
      }
   }

   private static void trim(Map<Integer, Long> table, long now, int cap) {
      if (table.size() > cap) {
         table.entrySet().removeIf(entry -> now > entry.getValue());
      }
   }

   private static void resolve() {
      if (!resolved) {
         resolved = true;
         removalReasonField = findField(Entity.class, "f_146795_", "removalReason");
         tickListField = findField(Level.class, "f_143243_", "entityTickList");
         if (tickListField != null) {
            tickAddMethod = findMethod(tickListField.getType(), "m_156910_", "add", Entity.class);
         }
      }
   }

   private static void backIntoTicks(ClientLevel level, Entity entity) {
      if (tickListField != null && tickAddMethod != null) {
         try {
            Object list = tickListField.get(level);
            if (list != null) {
               tickAddMethod.invoke(list, entity);
            }
         } catch (Throwable var3) {
         }
      }
   }

   private static Field findField(Class<?> owner, String... names) {
      for (String name : names) {
         try {
            Field found = owner.getDeclaredField(name);
            found.setAccessible(true);
            return found;
         } catch (Throwable var7) {
         }
      }

      return null;
   }

   private static Method findMethod(Class<?> owner, String srg, String official, Class<?>... args) {
      for (String name : new String[]{srg, official}) {
         try {
            Method found = owner.getDeclaredMethod(name, args);
            found.setAccessible(true);
            return found;
         } catch (Throwable var9) {
         }
      }

      return null;
   }
}

