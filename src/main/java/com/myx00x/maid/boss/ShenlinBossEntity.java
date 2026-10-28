package com.myx00x.maid.boss;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityLookup;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.phys.Vec3;

public class ShenlinBossEntity extends Entity {
   private boolean shadow;
   public ShenlinBoss boss;
   private boolean releasing;
   private static volatile boolean listsResolved;
   private static Field managerField;
   private static Field visibleField;
   private static Method addNoEvent;
   private static Method addByEvent;
   private static Method lookupGet;
   private static Method lookupGetById;
   private static Field removalReasonField;

   public ShenlinBossEntity(EntityType<? extends Entity> type, Level level) {
      super(type, level);
   }

   public boolean isInvulnerable() {
      return true;
   }

   public void markShadow(boolean value) {
      this.shadow = value;
   }

   public boolean isPickable() {
      return !this.shadow && super.isPickable();
   }

   public boolean isPushable() {
      return false;
   }

   public void setSecondsOnFire(int seconds) {
   }

   public void setRemainingFireTicks(int ticks) {
   }

   public void remove(RemovalReason reason) {
      if (this.level().isClientSide) {
         super.remove(reason);
      } else if (this.releasing || reason != RemovalReason.KILLED && reason != RemovalReason.DISCARDED) {
         super.remove(reason);
      }
   }

   protected void defineSynchedData() {
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
   }

   void release() {
      this.releasing = true;
   }

   void resolveLists() {
      if (!listsResolved) {
         listsResolved = true;
         managerField = findField(ServerLevel.class, "f_143244_", "entityManager");
         visibleField = findField(PersistentEntitySectionManager.class, "f_157494_", "visibleEntityStorage");
         addNoEvent = findMethod(PersistentEntitySectionManager.class, "addNewEntityWithoutEvent", "addNewEntityWithoutEvent", EntityAccess.class);
         addByEvent = findMethod(PersistentEntitySectionManager.class, "m_157533_", "addEntity", EntityAccess.class);
         lookupGet = findMethod(EntityLookup.class, "m_156819_", "get", UUID.class);
         lookupGetById = findMethod(EntityLookup.class, "m_156812_", "getEntity", int.class);
         removalReasonField = findField(Entity.class, "f_146795_", "removalReason");
      }
   }

   void clearRemovalReason() {
      if (this.isRemoved()) {
         RemovalReason reason = this.getRemovalReason();
         if (reason != null && reason != RemovalReason.CHANGED_DIMENSION) {
            if (removalReasonField != null) {
               try {
                  removalReasonField.set(this, null);
               } catch (Throwable var3) {
               }
            }
         }
      }
   }

   Object listManager(ServerLevel level) {
      if (managerField == null) {
         return null;
      }

      try {
         return managerField.get(level);
      } catch (Throwable t) {
         return null;
      }
   }

   boolean addNoEvent(Object manager) {
      if (addNoEvent == null) {
         return false;
      }

      try {
         addNoEvent.invoke(manager, this);
         return true;
      } catch (Throwable t) {
         return false;
      }
   }

   boolean addByEvent(Object manager) {
      if (addByEvent == null) {
         return false;
      }

      try {
         addByEvent.invoke(manager, this);
         return true;
      } catch (Throwable t) {
         return false;
      }
   }

   boolean onLists(ServerLevel level) {
      if (visibleField == null) {
         return true;
      }

      try {
         Object manager = this.listManager(level);
         Object lookup = manager == null ? null : visibleField.get(manager);
         if (lookup == null) {
            return true;
         }

         boolean byUuid = lookupGet != null && lookupGet.invoke(lookup, this.getUUID()) != null;
         boolean byId = lookupGetById != null && lookupGetById.invoke(lookup, this.getId()) != null;
         return byUuid && byId;
      } catch (Throwable t) {
         return true;
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

   public void tick() {
      super.tick();
      if (!this.level().isClientSide) {
         ShenlinBoss self = this.boss;
         if (self == null) {
            self = ShenlinBossKeeper.adopt(this);
            if (self == null) {
               ShenlinBossKeeper.dismissShell(this);
               return;
            }
         }

         if (!self.alive()) {
            ShenlinBossKeeper.dismissShell(this);
         } else {
            this.setDeltaMovement(Vec3.ZERO);
            this.setPos(self.x(), self.y(), self.z());
            this.setYRot(self.yRot());
            this.setXRot(self.xRot());
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();
         }
      }
   }
}

