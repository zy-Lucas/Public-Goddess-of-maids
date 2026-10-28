package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.phys.AABB;

public final class GuardUnionGetter implements LevelEntityGetter<Entity> {
   private final LevelEntityGetter<Entity> base;
   private final ServerLevel level;

   public GuardUnionGetter(LevelEntityGetter<Entity> base, ServerLevel level) {
      this.base = base;
      this.level = level;
   }

   public Entity get(int id) {
      Entity found = (Entity)this.base.get(id);
      return found != null ? found : this.byId(id);
   }

   public Entity get(UUID uuid) {
      Entity found = (Entity)this.base.get(uuid);
      return found != null ? found : this.byUuid(uuid);
   }

   public Iterable<Entity> getAll() {
      List<Entity> all = new ArrayList<>();

      for (Entity entity : this.base.getAll()) {
         all.add(entity);
      }

      for (UUID key : GuardWatch.watched()) {
         EntityMaid maid = this.missing(key);
         if (maid != null) {
            all.add(maid);
         }
      }

      return all;
   }

   public <U extends Entity> void get(EntityTypeTest<Entity, U> type, AbortableIterationConsumer<U> out) {
      this.base.get(type, out);

      for (UUID key : GuardWatch.watched()) {
         EntityMaid maid = this.missing(key);
         if (maid != null) {
            U cast = (U)type.tryCast(maid);
            if (cast != null && out.accept(cast).shouldAbort()) {
               return;
            }
         }
      }
   }

   public void get(AABB box, Consumer<Entity> out) {
      this.base.get(box, out);

      for (UUID key : GuardWatch.watched()) {
         EntityMaid maid = this.missing(key);
         if (maid != null && maid.getBoundingBox().intersects(box)) {
            out.accept(maid);
         }
      }
   }

   public <U extends Entity> void get(EntityTypeTest<Entity, U> type, AABB box, AbortableIterationConsumer<U> out) {
      this.base.get(type, box, out);

      for (UUID key : GuardWatch.watched()) {
         EntityMaid maid = this.missing(key);
         if (maid != null && maid.getBoundingBox().intersects(box)) {
            U cast = (U)type.tryCast(maid);
            if (cast != null && out.accept(cast).shouldAbort()) {
               return;
            }
         }
      }
   }

   private Entity byId(int id) {
      for (UUID key : GuardWatch.watched()) {
         EntityMaid maid = GuardWatch.held(key);
         if (maid != null && maid.getId() == id && maid.level() == this.level) {
            return maid;
         }
      }

      return null;
   }

   private Entity byUuid(UUID id) {
      EntityMaid maid = GuardWatch.held(id);
      return maid != null && maid.level() == this.level ? maid : null;
   }

   private EntityMaid missing(UUID key) {
      EntityMaid maid = GuardWatch.held(key);
      if (maid != null && maid.level() == this.level) {
         return this.base.get(maid.getId()) == null ? maid : null;
      } else {
         return null;
      }
   }
}

