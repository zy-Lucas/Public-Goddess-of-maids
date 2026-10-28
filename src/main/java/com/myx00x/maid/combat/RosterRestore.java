package com.myx00x.maid.combat;

import com.myx00x.maid.ShenlinMod;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.util.AbortableIterationConsumer.Continuation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.level.entity.EntityLookup;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.entity.Visibility;
import net.minecraft.world.phys.AABB;

public final class RosterRestore {
   private static volatile boolean resolved;
   private static Field managerField;
   private static Field tickListField;
   private static Field visibleField;
   private static Field knownUuidsField;
   private static Field lookupByUuid;
   private static Field lookupById;
   private static Field sectionStorageField;
   private static Method sectionAt;
   private static Field sectionEntities;
   private static Field multiMapAll;
   private static Field multiMapByClass;
   private static Method tickAdd;
   private static Field tickActive;
   private static Field chunkEntityMap;
   private static Field trackedLastSection;
   private static Field trackedOwnerField;
   private static Constructor<?> trackedCtor;
   private static Method updatePlayers;
   private static Method typeRange;
   private static Method typeInterval;
   private static Method typeDeltas;
   private static Method setCallback;
   private static Constructor<?> callbackCtor;
   private static Field removalReasonField;
   private static Field noPhysicsField;
   private static Field deadField;
   private static Method setInvisible;
   private static Method sectionStatus;
   private static Method managerMark;
   private static Field chunkVisibility;
   private static boolean visibilityProbed;
   private static long accessLoggedAt;
   private static final long ACCESS_LOG_MS = 5000L;
   private static String lastClean = "";

   private RosterRestore() {
   }

   public static boolean force(ServerLevel level, Entity entity) {
      return force(level, entity, false);
   }

   public static boolean force(ServerLevel level, Entity entity, boolean thorough) {
      resolve();
      if (managerField != null && visibleField != null) {
         try {
            Object manager = managerField.get(level);
            if (manager == null) {
               return false;
            }

            clean(level, manager, entity, thorough);
            if (knownUuidsField != null && knownUuidsField.get(manager) instanceof Set<?> uuids) {
               ((Set<UUID>)uuids).add(entity.getUUID());
            }

            Object lookup = visibleField.get(manager);
            if (lookup == null) {
               return false;
            }

            if (lookupByUuid != null && lookupByUuid.get(lookup) instanceof Map<?, ?> byUuid) {
               ((Map<?, Entity>)byUuid).put(entity.getUUID(), entity);
            }

            if (lookupById != null && lookupById.get(lookup) instanceof Int2ObjectMap<?> byId) {
               byId.put(entity.getId(), entity);
            }

            intoSection(manager, entity);
            intoTickList(level, entity);
            repairTracking(level, entity);
            Object backUuid = lookupByUuid != null && lookupByUuid.get(lookup) instanceof Map<?, ?> m ? m.get(entity.getUUID()) : null;
            Object backId = lookupById != null && lookupById.get(lookup) instanceof Int2ObjectMap<?> m2 ? m2.get(entity.getId()) : null;
            return backUuid == entity && backId == entity;
         } catch (Throwable t) {
            return false;
         }
      } else {
         return false;
      }
   }

   private static void clean(ServerLevel level, Object manager, Entity entity, boolean thorough) {
      List<String> hit = new ArrayList<>(4);

      try {
         if (removalReasonField != null && removalReasonField.get(entity) instanceof RemovalReason reason) {
            boolean moved = reason == RemovalReason.CHANGED_DIMENSION;
            boolean churn = reason == RemovalReason.UNLOADED_TO_CHUNK || reason == RemovalReason.UNLOADED_WITH_PLAYER;
            boolean fakeChurn = churn && loaded(manager, entity);
            boolean known = reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED;
            if (!moved && (thorough && (fakeChurn || !churn) || !thorough && known)) {
               removalReasonField.set(entity, null);
               hit.add("reason");
            }
         }
      } catch (Throwable var14) {
      }

      entity.onAddedToWorld();
      if (setInvisible != null) {
         try {
            if (entity.isInvisible()) {
               setInvisible.invoke(entity, false);
               hit.add("invisible");
            }
         } catch (Throwable var13) {
         }
      }

      if (entity instanceof LivingEntity living) {
         try {
            if (deadField != null && deadField.getBoolean(living)) {
               deadField.setBoolean(living, false);
               hit.add("dead");
            }
         } catch (Throwable var12) {
         }

         if (living.deathTime != 0) {
            living.deathTime = 0;
            hit.add("time");
         }
      }

      try {
         if (noPhysicsField != null && noPhysicsField.getBoolean(entity)) {
            noPhysicsField.setBoolean(entity, false);
            hit.add("physics");
         }
      } catch (Throwable var11) {
      }

      entity.canUpdate(true);
      if (!entity.isInvulnerable()) {
         entity.setInvulnerable(true);
         hit.add("invul");
      }

      if (entity.getBoundingBox().getXsize() < 0.01 || entity.getBoundingBox().getYsize() < 0.01) {
         entity.refreshDimensions();
         hit.add("box");
      }

      if (!hit.isEmpty()) {
         String now = String.join("+", hit);
         if (!now.equals(lastClean)) {
            lastClean = now;
            ShenlinMod.LOGGER.info("[shenlin] boss shell dirt cleaned: {}", now);
         }
      }
   }

   private static boolean loaded(Object manager, Entity entity) {
      probeVisibility(manager);
      if (chunkVisibility == null) {
         return true;
      }

      try {
         return chunkVisibility.get(manager) instanceof Long2ObjectMap<?> byChunk ? byChunk.containsKey(new ChunkPos(entity.blockPosition()).toLong()) : true;
      } catch (Throwable t) {
         return true;
      }
   }

   private static void probeVisibility(Object manager) {
      if (!visibilityProbed) {
         visibilityProbed = true;
         chunkVisibility = field(PersistentEntitySectionManager.class, "f_157492_", "chunkVisibility");
         if (chunkVisibility == null) {
            for (Field probe : PersistentEntitySectionManager.class.getDeclaredFields()) {
               if (Long2ObjectMap.class.isAssignableFrom(probe.getType())) {
                  probe.setAccessible(true);

                  try {
                     if (probe.get(manager) instanceof Long2ObjectMap<?> map && map.defaultReturnValue() == Visibility.HIDDEN) {
                        chunkVisibility = probe;
                        return;
                     }
                  } catch (Throwable var7) {
                  }
               }
            }
         }
      }
   }

   private static void restoreAccess(Object manager, Entity entity, Object section) {
      if (section != null && sectionStatus != null && managerMark != null) {
         try {
            if (sectionStatus.invoke(section) instanceof Visibility visible && visible.isAccessible()) {
               return;
            }

            if (!loaded(manager, entity)) {
               return;
            }

            managerMark.invoke(manager, new ChunkPos(entity.blockPosition()), Visibility.TICKING);
            long now = System.currentTimeMillis();
            if (now - accessLoggedAt >= 5000L) {
               accessLoggedAt = now;
               ShenlinMod.LOGGER.warn("[shenlin] entity section was hidden, opened again");
            }
         } catch (Throwable var6) {
         }
      }
   }

   private static void intoSection(Object manager, Entity entity) {
      if (sectionStorageField != null && sectionAt != null && sectionEntities != null && multiMapAll != null) {
         try {
            Object storage = sectionStorageField.get(manager);
            if (storage == null) {
               return;
            }

            long key = SectionPos.of(entity.blockPosition()).asLong();
            Object section = sectionAt.invoke(storage, key);
            restoreAccess(manager, entity, section);
            Object multiMap = section != null ? sectionEntities.get(section) : null;
            if (multiMap == null) {
               return;
            }

            if (multiMapAll.get(multiMap) instanceof List<?> all && !holdsIdentity(all, entity)) {
               ((List<Entity>)all).add(entity);
            }

            if (multiMapByClass != null && multiMapByClass.get(multiMap) instanceof Map<?, ?> map) {
               for (Entry<Class<?>, List<Object>> entry : map.entrySet()) {
                  if (entry.getKey().isInstance(entity) && !holdsIdentity(entry.getValue(), entity)) {
                     entry.getValue().add(entity);
                  }
               }
            }

            if (setCallback != null && callbackCtor != null) {
               setCallback.invoke(entity, callbackCtor.newInstance(manager, entity, key, section));
            }
         } catch (Throwable var12) {
         }
      }
   }

   private static void intoTickList(ServerLevel level, Entity entity) {
      if (tickListField != null) {
         try {
            Object list = tickListField.get(level);
            if (list == null) {
               return;
            }

            if (!((tickActive != null ? tickActive.get(list) : null) instanceof Int2ObjectMap<?> active && !active.containsKey(entity.getId()))) {
               return;
            }

            if (tickAdd != null) {
               try {
                  tickAdd.invoke(list, entity);
               } catch (Throwable var6) {
               }
            }

            if (!active.containsKey(entity.getId())) {
               active.put(entity.getId(), entity);
            }
         } catch (Throwable var7) {
         }
      }
   }

   private static void repairTracking(ServerLevel level, Entity entity) {
      if (chunkEntityMap != null) {
         try {
            Object chunkMap = level.getChunkSource().chunkMap;
            if (!(chunkEntityMap.get(chunkMap) instanceof Int2ObjectMap<?> entities)) {
               return;
            }

            Object existing = entities.get(entity.getId());
            if (existing != null && trackedOwnerField != null && trackedOwnerField.get(existing) == entity) {
               if (trackedLastSection != null && trackedLastSection.get(existing) != null) {
                  trackedLastSection.set(existing, null);
               }

               return;
            }

            if (trackedCtor == null || typeRange == null || typeInterval == null || typeDeltas == null) {
               return;
            }

            Object type = entity.getType();
            int range = (Integer)typeRange.invoke(type) * 16;
            if (range == 0) {
               return;
            }

            Object tracked = trackedCtor.newInstance(chunkMap, entity, range, (Integer)typeInterval.invoke(type), (Boolean)typeDeltas.invoke(type));
            entities.put(entity.getId(), tracked);
            if (updatePlayers != null && level.getServer() != null) {
               List<ServerPlayer> players = level.getServer().getPlayerList().getPlayers();
               if (!players.isEmpty()) {
                  updatePlayers.invoke(tracked, players);
               }
            }
         } catch (Throwable var10) {
         }
      }
   }

   public static boolean collidable(ServerLevel level, Entity entity) {
      resolve();
      if (entity == null) {
         return false;
      }

      AABB box = entity.getBoundingBox();
      if (!(box.getXsize() < 0.01) && !(box.getYsize() < 0.01)) {
         try {
            boolean[] hit = new boolean[]{false};
            level.getEntities().get(EntityTypeTest.forClass(Entity.class), box.inflate(0.5), found -> {
               if (found == entity) {
                  hit[0] = true;
                  return Continuation.ABORT;
               } else {
                  return Continuation.CONTINUE;
               }
            });
            return hit[0];
         } catch (Throwable t) {
            return true;
         }
      } else {
         return false;
      }
   }

   public static boolean intact(ServerLevel level, Entity entity) {
      resolve();
      if (entity != null && !entity.isRemoved()) {
         try {
            if (level.getEntity(entity.getId()) != entity) {
               return false;
            }

            if (level.getEntities().get(entity.getUUID()) != entity) {
               return false;
            }
         } catch (Throwable t) {
            return false;
         }

         return inTickTable(level, entity) && collidable(level, entity);
      } else {
         return false;
      }
   }

   private static boolean inTickTable(ServerLevel level, Entity entity) {
      if (tickListField != null && tickActive != null) {
         try {
            Object list = tickListField.get(level);
            return !((list == null ? null : tickActive.get(list)) instanceof Int2ObjectMap<?> map && !map.containsKey(entity.getId()));
         } catch (Throwable t) {
            return true;
         }
      } else {
         return true;
      }
   }

   public static boolean tracked(ServerLevel level, Entity entity) {
      resolve();
      if (chunkEntityMap != null && trackedOwnerField != null) {
         try {
            Object chunkMap = level.getChunkSource().chunkMap;
            if (chunkEntityMap.get(chunkMap) instanceof Int2ObjectMap<?> entities) {
               Object tracked = entities.get(entity.getId());
               return tracked != null && trackedOwnerField.get(tracked) == entity;
            } else {
               return true;
            }
         } catch (Throwable t) {
            return true;
         }
      } else {
         return true;
      }
   }

   public static boolean forceTracking(ServerLevel level, Entity entity) {
      resolve();
      repairTracking(level, entity);
      return tracked(level, entity);
   }

   private static boolean holdsIdentity(List<?> list, Object target) {
      for (Object item : list) {
         if (item == target) {
            return true;
         }
      }

      return false;
   }

   private static void resolve() {
      if (!resolved) {
         resolved = true;
         managerField = field(ServerLevel.class, "f_143244_", "entityManager");
         tickListField = field(ServerLevel.class, "f_143243_", "entityTickList");
         visibleField = field(PersistentEntitySectionManager.class, "f_157494_", "visibleEntityStorage");
         knownUuidsField = field(PersistentEntitySectionManager.class, "f_157491_", "knownUuids");
         lookupByUuid = field(EntityLookup.class, "f_156808_", "byUuid");
         lookupById = field(EntityLookup.class, "f_156807_", "byId");
         sectionStorageField = field(PersistentEntitySectionManager.class, "f_157495_", "sectionStorage");
         sectionAt = method(EntitySectionStorage.class, "m_156893_", "getOrCreateSection", long.class);
         sectionEntities = field(EntitySection.class, "f_156827_", "storage");
         multiMapAll = field(ClassInstanceMultiMap.class, "f_13529_", "all");
         multiMapByClass = field(ClassInstanceMultiMap.class, "f_13527_", "byClass");
         tickAdd = method(EntityTickList.class, "m_156908_", "add", Entity.class);
         tickActive = field(EntityTickList.class, "f_156903_", "active");
         chunkEntityMap = field(ChunkMap.class, "f_140150_", "entityMap");
         setCallback = method(Entity.class, "m_141960_", "setLevelCallback", EntityInLevelCallback.class);
         trackedLastSection = fieldByName("net.minecraft.server.level.ChunkMap.TrackedEntity", "f_140474_", "lastSectionPos");
         trackedOwnerField = fieldByName("net.minecraft.server.level.ChunkMap.TrackedEntity", "f_140472_", "entity");
         trackedCtor = ctor("net.minecraft.server.level.ChunkMap.TrackedEntity", ChunkMap.class, Entity.class, int.class, int.class, boolean.class);
         updatePlayers = methodByName("net.minecraft.server.level.ChunkMap.TrackedEntity", "m_140487_", "updatePlayers", List.class);
         typeRange = methodByName("net.minecraft.world.entity.EntityType", "m_20681_", "clientTrackingRange");
         typeInterval = methodByName("net.minecraft.world.entity.EntityType", "m_20682_", "updateInterval");
         typeDeltas = methodByName("net.minecraft.world.entity.EntityType", "m_20683_", "trackDeltas");
         callbackCtor = ctor(
            "net.minecraft.world.level.entity.PersistentEntitySectionManager.Callback",
            PersistentEntitySectionManager.class,
            EntityAccess.class,
            long.class,
            EntitySection.class
         );
         removalReasonField = field(Entity.class, "f_146795_", "removalReason");
         deadField = field(LivingEntity.class, "f_20890_", "dead");
         noPhysicsField = field(Entity.class, "f_19794_", "noPhysics");
         setInvisible = method(Entity.class, "m_6842_", "setInvisible", boolean.class);
         sectionStatus = methodReturning(EntitySection.class, Visibility.class);
         managerMark = methodOf(PersistentEntitySectionManager.class, ChunkPos.class, Visibility.class);
         if (managerField == null || visibleField == null || lookupByUuid == null || lookupById == null) {
            ShenlinMod.LOGGER
               .warn(
                  "[shenlin] roster restore could not resolve the world roster (manager={} visible={} byUuid={} byId={})",
                  new Object[]{managerField != null, visibleField != null, lookupByUuid != null, lookupById != null}
               );
         }
      }
   }

   private static Constructor<?> ctor(String name, Class<?>... args) {
      try {
         Constructor<?> found = Class.forName(name).getDeclaredConstructor(args);
         found.setAccessible(true);
         return found;
      } catch (Throwable t) {
         return null;
      }
   }

   private static Field field(Class<?> owner, String... names) {
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

   private static Field fieldByName(String owner, String... names) {
      try {
         return field(Class.forName(owner), names);
      } catch (Throwable t) {
         return null;
      }
   }

   private static Method methodByName(String owner, String srg, String official, Class<?>... args) {
      try {
         return method(Class.forName(owner), srg, official, args);
      } catch (Throwable t) {
         return null;
      }
   }

   private static Method method(Class<?> owner, String srg, String official, Class<?>... args) {
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

   private static Method methodReturning(Class<?> owner, Class<?> returns) {
      for (Method found : owner.getDeclaredMethods()) {
         if (found.getParameterCount() == 0 && found.getReturnType() == returns) {
            found.setAccessible(true);
            return found;
         }
      }

      return null;
   }

   private static Method methodOf(Class<?> owner, Class<?>... args) {
      for (Method found : owner.getDeclaredMethods()) {
         if (Arrays.equals(found.getParameterTypes(), args)) {
            found.setAccessible(true);
            return found;
         }
      }

      return null;
   }
}

