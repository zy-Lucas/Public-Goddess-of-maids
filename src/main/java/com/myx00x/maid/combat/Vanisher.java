package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.ShenlinMod;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.level.entity.EntityLookup;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;

public final class Vanisher {
   private static boolean resolved;
   private static Field removalReason;
   private static Field levelCallback;
   private static Field nullCallback;
   private static Field entityManager;
   private static Field managerSections;
   private static Field managerVisible;
   private static Field managerKnownUuids;
   private static Method removeSectionIfEmpty;
   private static Field lookupByUuid;
   private static Field lookupById;
   private static Field sectionEntities;
   private static Field multiMapByClass;
   private static Field multiMapAll;
   private static Field storageSections;
   private static Field tickListField;
   private static Field tickActive;
   private static Method tickEnsure;
   private static Field chunkEntityMap;
   private static Field trackedSeenBy;
   private static Field trackedEntity;
   private static Field navigatingMobs;
   private static Field dragonParts;
   private static Method scoreboardRemoved;
   private static final List<Vanisher.Ghost> PENDING_GHOSTS = new ArrayList<>();
   private static final int MAX_VANISH_DEPTH = 32;
   private static final long WARN_GAP_MS = 2000L;
   private static final Map<UUID, Long> LAST_WARN = new ConcurrentHashMap<>();

   private Vanisher() {
   }

   public static List<Vanisher.Ghost> vanishGhost(Entity entity, boolean callEvents) {
      PENDING_GHOSTS.clear();
      vanish(entity, callEvents, true);
      return new ArrayList<>(PENDING_GHOSTS);
   }

   public static void scanRoster(ServerLevel level, Consumer<Entity> action) {
      resolve();
      if (entityManager != null) {
         try {
            Object manager = entityManager.get(level);
            if (manager == null) {
               return;
            }

            Set<Entity> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            if (managerVisible != null && lookupByUuid != null) {
               Object lookup = managerVisible.get(manager);
               Map<?, ?> byUuid = lookup != null ? (Map)lookupByUuid.get(lookup) : null;
               if (byUuid != null) {
                  for (Object value : new ArrayList<>(byUuid.values())) {
                     if (value instanceof Entity entity && seen.add(entity)) {
                        feed(action, entity);
                     }
                  }
               }
            }

            if (managerSections != null && storageSections != null && sectionEntities != null && multiMapAll != null) {
               Object storage = managerSections.get(manager);
               Map<?, ?> sections = storage != null ? (Map)storageSections.get(storage) : null;
               if (sections != null) {
                  for (Object section : new ArrayList<>(sections.values())) {
                     Object multiMap = section != null ? sectionEntities.get(section) : null;
                     if (multiMap != null) {
                        List<?> all = (List<?>)multiMapAll.get(multiMap);
                        if (all != null && !all.isEmpty()) {
                           for (Object obj : new ArrayList(all)) {
                              if (obj instanceof Entity entity && seen.add(entity)) {
                                 feed(action, entity);
                              }
                           }
                        }
                     }
                  }
               }
            }
         } catch (Throwable t) {
            ShenlinMod.LOGGER.warn("[shenlin] roster scan failed: {}", t.toString());
         }
      }
   }

   private static void feed(Consumer<Entity> action, Entity entity) {
      try {
         action.accept(entity);
      } catch (Throwable t) {
         ShenlinMod.LOGGER.warn("[shenlin] roster action failed on {}: {}", entity.getType().toShortString(), t.toString());
      }
   }

   public static void scanAround(ServerLevel level, double x, double y, double z, double radius, Consumer<Entity> action) {
      resolve();
      if (entityManager != null && managerSections != null && storageSections != null && sectionEntities != null && multiMapAll != null) {
         try {
            Object manager = entityManager.get(level);
            Map<?, ?> sections = manager != null ? (Map)storageSections.get(managerSections.get(manager)) : null;
            if (sections == null) {
               return;
            }

            int minX = SectionPos.posToSectionCoord(x - radius);
            int maxX = SectionPos.posToSectionCoord(x + radius);
            int minY = SectionPos.posToSectionCoord(y - radius);
            int maxY = SectionPos.posToSectionCoord(y + radius);
            int minZ = SectionPos.posToSectionCoord(z - radius);
            int maxZ = SectionPos.posToSectionCoord(z + radius);

            for (int sx = minX; sx <= maxX; sx++) {
               for (int sz = minZ; sz <= maxZ; sz++) {
                  for (int sy = minY; sy <= maxY; sy++) {
                     Object section = sections.get(SectionPos.asLong(sx, sy, sz));
                     Object multiMap = section != null ? sectionEntities.get(section) : null;
                     List<?> all = multiMap != null ? (List)multiMapAll.get(multiMap) : null;
                     if (all != null && !all.isEmpty()) {
                        for (Object obj : new ArrayList(all)) {
                           if (obj instanceof Entity entity) {
                              feed(action, entity);
                           }
                        }
                     }
                  }
               }
            }
         } catch (Throwable t) {
            ShenlinMod.LOGGER.warn("[shenlin] near scan failed: {}", t.toString());
         }
      }
   }

   public static void scanLive(ServerLevel level, Consumer<Entity> action) {
      resolve();
      if (entityManager != null && managerVisible != null && lookupByUuid != null) {
         try {
            Object manager = entityManager.get(level);
            Object lookup = manager != null ? managerVisible.get(manager) : null;
            Map<?, ?> byUuid = lookup != null ? (Map)lookupByUuid.get(lookup) : null;
            if (byUuid == null) {
               return;
            }

            for (Object value : new ArrayList<>(byUuid.values())) {
               if (value instanceof Entity entity) {
                  feed(action, entity);
               }
            }
         } catch (Throwable t) {
            ShenlinMod.LOGGER.warn("[shenlin] live scan failed: {}", t.toString());
         }
      }
   }

   public static boolean inRoster(ServerLevel level, Entity entity) {
      resolve();
      if (entityManager == null) {
         return true;
      }

      try {
         Object manager = entityManager.get(level);
         if (manager == null) {
            return true;
         }

         if (managerVisible != null && lookupByUuid != null) {
            Object lookup = managerVisible.get(manager);
            Map<?, ?> byUuid = lookup != null ? (Map)lookupByUuid.get(lookup) : null;
            if (byUuid != null && byUuid.containsKey(entity.getUUID())) {
               return true;
            }
         }

         if (managerSections != null && storageSections != null && sectionEntities != null && multiMapAll != null) {
            Object storage = managerSections.get(manager);
            Map<?, ?> sections = storage != null ? (Map)storageSections.get(storage) : null;
            if (sections != null) {
               Object section = sections.get(SectionPos.of(entity.blockPosition()).asLong());
               Object multiMap = section != null ? sectionEntities.get(section) : null;
               if (multiMap != null) {
                  List<?> all = (List<?>)multiMapAll.get(multiMap);
                  if (all != null) {
                     for (Object obj : new ArrayList(all)) {
                        if (obj == entity) {
                           return true;
                        }
                     }
                  }
               }
            }
         }

         return false;
      } catch (Throwable t) {
         return true;
      }
   }

   public static void scanTracked(ServerLevel level, Consumer<Entity> action) {
      resolve();
      if (chunkEntityMap != null && trackedEntity != null) {
         try {
            Object map = chunkEntityMap.get(level.getChunkSource().chunkMap);
            if (map == null) {
               return;
            }

            for (Object record : new ArrayList(((Int2ObjectMap)map).values())) {
               if (trackedEntity.get(record) instanceof Entity entity) {
                  action.accept(entity);
               }
            }
         } catch (Throwable t) {
            ShenlinMod.LOGGER.warn("[shenlin] tracked scan failed: {}", t.toString());
         }
      }
   }

   private static boolean inSection(ServerLevel level, Entity entity) {
      try {
         Object manager = entityManager != null ? entityManager.get(level) : null;
         if (manager != null && managerSections != null && storageSections != null && sectionEntities != null && multiMapAll != null) {
            Object storage = managerSections.get(manager);
            Map<?, ?> sections = storage != null ? (Map)storageSections.get(storage) : null;
            if (sections == null) {
               return false;
            }

            Object section = sections.get(SectionPos.of(entity.blockPosition()).asLong());
            Object multiMap = section != null ? sectionEntities.get(section) : null;
            if (multiMap == null) {
               return false;
            }

            List<?> all = (List<?>)multiMapAll.get(multiMap);
            if (all == null) {
               return false;
            }

            for (Object obj : all) {
               if (obj == entity) {
                  return true;
               }
            }

            return false;
         } else {
            return false;
         }
      } catch (Throwable t) {
         return false;
      }
   }

   private static boolean inVisibleIndex(ServerLevel level, Entity entity) {
      try {
         Object manager = entityManager != null ? entityManager.get(level) : null;
         if (manager != null && managerVisible != null && lookupByUuid != null) {
            Object lookup = managerVisible.get(manager);
            Map<?, ?> byUuid = lookup != null ? (Map)lookupByUuid.get(lookup) : null;
            return byUuid != null && byUuid.containsKey(entity.getUUID());
         } else {
            return false;
         }
      } catch (Throwable t) {
         return false;
      }
   }

   public static void vanish(Entity entity, boolean callEvents) {
      vanish(entity, callEvents, false, false);
   }

   public static void vanishMaid(EntityMaid maid, boolean keepGhost) {
      vanish(maid, false, keepGhost, true);
   }

   private static void vanish(Entity entity, boolean callEvents, boolean keepGhost) {
      vanish(entity, callEvents, keepGhost, false);
   }

   private static void vanish(Entity entity, boolean callEvents, boolean keepGhost, boolean allowMaid) {
      vanish(entity, callEvents, keepGhost, allowMaid, new IdentityHashMap<>(), 0);
   }

   private static void vanish(Entity entity, boolean callEvents, boolean keepGhost, boolean allowMaid, IdentityHashMap<Entity, Boolean> seen, int depth) {
      if (!(entity instanceof Player) && (allowMaid || !(entity instanceof EntityMaid))) {
         if (seen.put(entity, Boolean.TRUE) == null && depth <= 32) {
            resolve();

            try {
               if (entity.isVehicle()) {
                  for (Entity passenger : new ArrayList(entity.getPassengers())) {
                     vanish(passenger, callEvents, keepGhost, false, seen, depth + 1);
                  }
               }

               entity.stopRiding();
               if (removalReason != null && entity.getRemovalReason() == null) {
                  removalReason.set(entity, RemovalReason.DISCARDED);
               }

               if (entity.level() instanceof ServerLevel server) {
                  strip(server, entity, keepGhost);
                  List<String> left = leftovers(server, entity);
                  if (!left.isEmpty()) {
                     try {
                        strip(server, entity, false);
                     } catch (Throwable t) {
                        ShenlinMod.LOGGER.warn("[shenlin] vanish retry {} failed: {}", entity.getType().toShortString(), t.toString());
                     }

                     List<String> again = leftovers(server, entity);
                     if (!again.isEmpty()) {
                        noteLeftovers(entity, again);
                     }
                  }

                  if (callEvents) {
                     entity.onRemovedFromWorld();
                     MinecraftForge.EVENT_BUS.post(new EntityLeaveLevelEvent(entity, server));
                  }

                  detachFromWorld(entity);
               }
            } catch (Throwable t) {
               ShenlinMod.LOGGER.warn("[shenlin] vanish {} failed: {}", entity.getType().toShortString(), t.toString());
            }
         }
      }
   }

   private static void strip(ServerLevel server, Entity entity, boolean keepGhost) throws ReflectiveOperationException {
      long sectionPos = SectionPos.of(entity.blockPosition()).asLong();
      Object manager = entityManager != null ? entityManager.get(server) : null;
      EntitySection section = (manager != null && managerSections != null ? managerSections.get(manager) : null) instanceof EntitySectionStorage sections
         ? sections.getSection(sectionPos)
         : null;
      if (section != null) {
         removeFromSection(entity, section);
      }

      deactivateTicks(server, entity);
      untrack(server, entity, keepGhost);
      if (manager != null) {
         if (managerVisible != null) {
            Object lookup = managerVisible.get(manager);
            if (lookupByUuid != null) {
               ((Map)lookupByUuid.get(lookup)).remove(entity.getUUID());
            }

            if (lookupById != null) {
               ((Int2ObjectMap)lookupById.get(lookup)).remove(entity.getId());
            }
         }

         if (managerKnownUuids != null) {
            ((Set)managerKnownUuids.get(manager)).remove(entity.getUUID());
         }

         if (section != null && removeSectionIfEmpty != null) {
            removeSectionIfEmpty.invoke(manager, sectionPos, section);
         }
      }

      dropScoreboard(server, entity);
      detachCallback(entity);
   }

   private static List<String> leftovers(ServerLevel server, Entity entity) {
      List<String> left = new ArrayList<>();
      if (entityManager == null) {
         return left;
      }

      try {
         Object manager = entityManager.get(server);
         if (manager != null) {
            if (inVisibleIndex(server, entity)) {
               left.add("visible");
            }

            if (managerKnownUuids != null && ((Set)managerKnownUuids.get(manager)).contains(entity.getUUID())) {
               left.add("knownUuids");
            }

            if (managerVisible != null && lookupById != null) {
               Object lookup = managerVisible.get(manager);
               if (lookup != null && ((Int2ObjectMap)lookupById.get(lookup)).containsKey(entity.getId())) {
                  left.add("byId");
               }
            }
         }

         if (inSection(server, entity)) {
            left.add("section");
         }

         if (tickListField != null && tickActive != null) {
            Object ticks = tickListField.get(server);
            if (((Int2ObjectMap)tickActive.get(ticks)).containsKey(entity.getId())) {
               left.add("tick");
            }
         }

         if (chunkEntityMap != null && ((Int2ObjectMap)chunkEntityMap.get(server.getChunkSource().chunkMap)).containsKey(entity.getId())) {
            left.add("tracked");
         }

         if (navigatingMobs != null && entity instanceof Mob mob && ((Set)navigatingMobs.get(server)).contains(mob)) {
            left.add("navigating");
         }

         if (levelCallback != null && nullCallback != null && levelCallback.get(entity) != nullCallback.get(null)) {
            left.add("callback");
         }

         if (removalReason != null && entity.getRemovalReason() == null) {
            left.add("reason");
         }
      } catch (Throwable var5) {
      }

      return left;
   }

   private static void noteLeftovers(Entity entity, List<String> left) {
      UUID id = entity.getUUID();
      long wall = System.currentTimeMillis();
      Long last = LAST_WARN.get(id);
      if (last == null || wall - last >= 2000L) {
         if (LAST_WARN.size() > 512) {
            LAST_WARN.entrySet().removeIf(e -> wall - e.getValue() > 60000L);
         }

         LAST_WARN.put(id, wall);
         ShenlinMod.LOGGER.warn("[shenlin] vanish {} still on {} after retry", entity.getType().toShortString(), left);
      }
   }

   private static void detachFromWorld(Entity entity) {
      try {
         entity.setPos(-999999.0, -999999.0, -999999.0);
      } catch (Throwable var3) {
      }

      try {
         entity.invalidateCaps();
      } catch (Throwable var2) {
      }
   }

   private static void deactivateTicks(ServerLevel server, Entity entity) throws ReflectiveOperationException {
      if (tickListField != null && tickEnsure != null && tickActive != null) {
         Object ticks = tickListField.get(server);
         tickEnsure.invoke(ticks);
         ((Int2ObjectMap)tickActive.get(ticks)).remove(entity.getId());
      }
   }

   private static void untrack(ServerLevel server, Entity entity, boolean keepGhost) throws ReflectiveOperationException {
      ChunkMap chunks = server.getChunkSource().chunkMap;
      Set<ServerPlayer> viewers = new LinkedHashSet<>();
      boolean recorded = false;
      if (chunkEntityMap != null) {
         Object record = ((Int2ObjectMap)chunkEntityMap.get(chunks)).remove(entity.getId());
         if (record != null && trackedSeenBy != null) {
            recorded = true;

            for (Object conn : new ArrayList((Set)trackedSeenBy.get(record))) {
               viewers.add(((ServerPlayerConnection)conn).getPlayer());
            }
         }
      }

      if (!recorded) {
         viewers.addAll(server.players());
      }

      if (keepGhost) {
         Vanisher.Ghost ghost = new Vanisher.Ghost(entity.getId());
         ghost.viewers.addAll(viewers);
         PENDING_GHOSTS.add(ghost);
      } else {
         ClientboundRemoveEntitiesPacket packet = new ClientboundRemoveEntitiesPacket(new int[]{entity.getId()});

         for (ServerPlayer viewer : viewers) {
            viewer.connection.send(packet);
         }
      }

      clearBossbars(server, entity);
      if (navigatingMobs != null && entity instanceof Mob mob) {
         ((Set)navigatingMobs.get(server)).remove(mob);
      }

      if (dragonParts != null && entity.isMultipartEntity()) {
         Int2ObjectMap<?> parts = (Int2ObjectMap<?>)dragonParts.get(server);

         for (PartEntity<?> part : entity.getParts()) {
            parts.remove(part.getId());
         }
      }
   }

   private static void removeFromSection(Entity entity, EntitySection section) throws ReflectiveOperationException {
      if (sectionEntities != null && multiMapByClass != null) {
         Object multiMap = sectionEntities.get(section);
         Map<?, List<?>> byClass = (Map<?, List<?>>)multiMapByClass.get(multiMap);

         for (Entry<?, List<?>> entry : byClass.entrySet()) {
            if (((Class)entry.getKey()).isInstance(entity)) {
               entry.getValue().remove(entity);
            }
         }
      }
   }

   private static void dropScoreboard(ServerLevel server, Entity entity) throws ReflectiveOperationException {
      if (scoreboardRemoved != null) {
         scoreboardRemoved.invoke(server.getScoreboard(), entity);
      }
   }

   private static void detachCallback(Entity entity) throws ReflectiveOperationException {
      if (levelCallback != null && nullCallback != null) {
         levelCallback.set(entity, nullCallback.get(null));
      }
   }

   private static void clearBossbars(ServerLevel server, Entity entity) {
      for (Class<?> c = entity.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
         for (Field f : c.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())) {
               try {
                  f.setAccessible(true);
                  if (f.get(entity) instanceof ServerBossEvent bar) {
                     for (ServerPlayer viewer : server.players()) {
                        bar.removePlayer(viewer);
                     }

                     bar.removeAllPlayers();
                  }
               } catch (ReflectiveOperationException var10) {
               }
            }
         }
      }
   }

   private static void resolve() {
      if (!resolved) {
         resolved = true;
         removalReason = field(Entity.class, "removalReason", "f_146795_");
         levelCallback = field(Entity.class, "levelCallback", "f_146801_");
         nullCallback = field(EntityInLevelCallback.class, "NULL", "f_156799_");
         entityManager = field(ServerLevel.class, "entityManager", "f_143244_");
         managerSections = field(PersistentEntitySectionManager.class, "sectionStorage", "f_157495_");
         managerVisible = field(PersistentEntitySectionManager.class, "visibleEntityStorage", "f_157494_");
         managerKnownUuids = field(PersistentEntitySectionManager.class, "knownUuids", "f_157491_");
         removeSectionIfEmpty = method(PersistentEntitySectionManager.class, "removeSectionIfEmpty", "m_157509_", long.class, EntitySection.class);
         lookupByUuid = field(EntityLookup.class, "byUuid", "f_156808_");
         lookupById = field(EntityLookup.class, "byId", "f_156807_");
         sectionEntities = field(EntitySection.class, "storage", "f_156827_");
         multiMapByClass = field(ClassInstanceMultiMap.class, "byClass", "f_13527_");
         multiMapAll = field(ClassInstanceMultiMap.class, "all", "f_13529_");
         storageSections = field(EntitySectionStorage.class, "sectionById", "f_156852_");
         tickListField = field(ServerLevel.class, "entityTickList", "f_143243_");
         tickActive = field(EntityTickList.class, "active", "f_156903_");
         tickEnsure = method(EntityTickList.class, "ensureActiveIsNotIterated", "m_156907_");
         chunkEntityMap = field(ChunkMap.class, "entityMap", "f_140150_");
         Class<?> trackedClass = forName("net.minecraft.server.level.ChunkMap.TrackedEntity");
         trackedSeenBy = trackedClass != null ? field(trackedClass, "seenBy", "f_140475_") : null;
         trackedEntity = trackedClass != null ? field(trackedClass, "entity", "f_140472_") : null;
         navigatingMobs = field(ServerLevel.class, "navigatingMobs", "f_143246_");
         dragonParts = field(ServerLevel.class, "dragonParts", "f_143247_");
         scoreboardRemoved = method(ServerScoreboard.class, "entityRemoved", "m_83420_", Entity.class);
         if (entityManager == null || managerSections == null || chunkEntityMap == null) {
            ShenlinMod.LOGGER
               .warn(
                  "[shenlin] vanish resolve incomplete: manager={} sections={} entityMap={}",
                  new Object[]{entityManager != null, managerSections != null, chunkEntityMap != null}
               );
         }
      }
   }

   private static Field field(Class<?> owner, String official, String srg) {
      for (String name : new String[]{official, srg}) {
         try {
            Field found = owner.getDeclaredField(name);
            found.setAccessible(true);
            return found;
         } catch (ReflectiveOperationException var8) {
         }
      }

      return null;
   }

   private static Class<?> forName(String name) {
      try {
         return Class.forName(name);
      } catch (ClassNotFoundException e) {
         return null;
      }
   }

   private static Method method(Class<?> owner, String official, String srg, Class<?>... params) {
      for (String name : new String[]{official, srg}) {
         try {
            Method found = owner.getDeclaredMethod(name, params);
            found.setAccessible(true);
            return found;
         } catch (ReflectiveOperationException var9) {
         }
      }

      return null;
   }

   public static final class Ghost {
      final int id;
      final List<ServerPlayer> viewers = new ArrayList<>();

      Ghost(int id) {
         this.id = id;
      }

      public void sendRemove() {
         ClientboundRemoveEntitiesPacket packet = new ClientboundRemoveEntitiesPacket(new int[]{this.id});

         for (ServerPlayer viewer : this.viewers) {
            viewer.connection.send(packet);
         }

         this.viewers.clear();
      }
   }
}

