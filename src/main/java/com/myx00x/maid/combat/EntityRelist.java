package com.myx00x.maid.combat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityLookup;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;

public final class EntityRelist {
   private static volatile boolean resolved;
   private static Field managerField;
   private static Field visibleField;
   private static Method addNoEvent;
   private static Method addByEvent;
   private static Method lookupGet;
   private static Method lookupGetById;
   private static Field removalReasonField;

   private EntityRelist() {
   }

   private static void resolve() {
      if (!resolved) {
         resolved = true;
         managerField = findField(ServerLevel.class, "f_143244_", "entityManager");
         visibleField = findField(PersistentEntitySectionManager.class, "f_157494_", "visibleEntityStorage");
         addNoEvent = findMethod(PersistentEntitySectionManager.class, "addNewEntityWithoutEvent", "addNewEntityWithoutEvent", EntityAccess.class);
         addByEvent = findMethod(PersistentEntitySectionManager.class, "m_157533_", "addEntity", EntityAccess.class);
         lookupGet = findMethod(EntityLookup.class, "m_156819_", "get", UUID.class);
         lookupGetById = findMethod(EntityLookup.class, "m_156812_", "getEntity", int.class);
         removalReasonField = findField(Entity.class, "f_146795_", "removalReason");
      }
   }

   public static void clearRemovalReason(Entity entity) {
      if (entity != null && entity.isRemoved()) {
         resolve();
         if (removalReasonField != null) {
            try {
               RemovalReason reason = entity.getRemovalReason();
               if (reason == null || reason == RemovalReason.CHANGED_DIMENSION) {
                  return;
               }

               removalReasonField.set(entity, null);
            } catch (Throwable var2) {
            }
         }
      }
   }

   public static boolean onLists(Entity entity, ServerLevel level) {
      if (entity != null && level != null) {
         resolve();
         if (visibleField != null && managerField != null) {
            try {
               Object manager = managerField.get(level);
               Object lookup = manager == null ? null : visibleField.get(manager);
               if (lookup == null) {
                  return true;
               }

               boolean byUuid = lookupGet != null && lookupGet.invoke(lookup, entity.getUUID()) != null;
               boolean byId = lookupGetById != null && lookupGetById.invoke(lookup, entity.getId()) != null;
               return byUuid && byId;
            } catch (Throwable t) {
               return true;
            }
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   public static boolean addNoEvent(Entity entity, ServerLevel level) {
      return invokeAdd(entity, level, addNoEvent, true);
   }

   public static boolean addByEvent(Entity entity, ServerLevel level) {
      return invokeAdd(entity, level, addByEvent, true);
   }

   private static boolean invokeAdd(Entity entity, ServerLevel level, Method m, boolean useResolve) {
      if (entity != null && level != null) {
         if (useResolve) {
            resolve();
         }

         Method target = m == addNoEvent ? addNoEvent : addByEvent;
         if (target != null && managerField != null) {
            try {
               Object manager = managerField.get(level);
               if (manager == null) {
                  return false;
               }

               target.invoke(manager, entity);
               return true;
            } catch (Throwable t) {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
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

