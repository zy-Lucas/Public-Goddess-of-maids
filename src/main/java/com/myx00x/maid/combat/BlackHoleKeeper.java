package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.data.inner.AttackListData;
import com.github.tartaricacid.touhoulittlemaid.entity.misc.MonsterType;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitTaskData;
import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.network.ModPackets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

public final class BlackHoleKeeper {
   private static final double SCAN_MARGIN = 1.0;
   private static final int MAX_ALIVE = 32;
   private static final long GRIP_MS = 733L;
   private static final int DRAIN_EVERY_TICKS = 20;
   private static final int EXEC_LIFETIME_TICKS = 90;
   private static final float EXEC_HOLE_WARP = 10.0F;
   private static final float EXEC_HOLE_SIZE = 1.6666666F;
   private static final double EXEC_HOLE_RANGE = 64.0;
   private static final double SMALL_HOLE_RANGE = 64.0;
   private static final Map<ResourceLocation, List<BlackHoleKeeper.Hole>> HOLES = new ConcurrentHashMap<>();
   private static final ResourceKey<DamageType> HOLE_DAMAGE = ResourceKey.create(
      Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("goddess_of_maids", "black_hole")
   );
   private static long lastCrushLog;
   private static long lastDrainLog;

   private BlackHoleKeeper() {
   }

   public static int lifeTicks() {
      int configured = (Integer)Config.BLACK_HOLE_TICKS.get();
      return configured > 0 ? configured : (Integer)Config.SLASH_PULSE_INTERVAL_TICKS.get();
   }

   public static void spawn(ServerLevel level, EntityMaid maid, Vec3 at) {
      if ((Boolean)Config.BLACK_HOLE_ENABLED.get()) {
         int ticks = lifeTicks();
         List<BlackHoleKeeper.Hole> list = HOLES.computeIfAbsent(level.dimension().location(), key -> new ArrayList<>());

         for (BlackHoleKeeper.Hole hole : list) {
            if (!hole.execution && hole.owner.equals(maid.getUUID()) && hole.at.distanceToSqr(at) < 1.0) {
               hole.left = ticks;
               return;
            }
         }

         double reach = (Double)Config.BLACK_HOLE_RADIUS.get();
         double reachSqr = reach * reach;
         int before = list.size();
         list.removeIf(holex -> !holex.execution && holex.at.distanceToSqr(at) < reachSqr);
         int bumped = before - list.size();
         list.add(new BlackHoleKeeper.Hole(at, ticks, maid.getUUID()));

         while (list.size() > 32) {
            list.remove(0);
         }

         ShenlinMod.LOGGER
            .info(
               "[shenlin] hole born maid={} at={} ticks={} live={} bumped={}",
               new Object[]{maid.getId(), String.format("%.2f/%.2f/%.2f", at.x, at.y, at.z), ticks, list.size(), bumped}
            );
      }
   }

   public static void spawnSmallHole(ServerLevel level, EntityMaid maid, Vec3 at) {
      if ((Boolean)Config.BLACK_HOLE_ENABLED.get()) {
         spawn(level, maid, at);
         ModPackets.sendBlackHole(
            level,
            at,
            64.0,
            ((Double)Config.BLACK_HOLE_SIZE.get()).floatValue(),
            ((Double)Config.BLACK_HOLE_WARP_RADIUS.get()).floatValue(),
            ((Double)Config.BLACK_HOLE_RADIUS.get()).floatValue(),
            lifeTicks(),
            false
         );
      }
   }

   public static void spawnExecution(ServerLevel level, EntityMaid maid, Vec3 at, float victimMax) {
      if ((Boolean)Config.BLACK_HOLE_ENABLED.get()) {
         List<BlackHoleKeeper.Hole> list = HOLES.computeIfAbsent(level.dimension().location(), key -> new ArrayList<>());
         double reach = (Double)Config.BLACK_HOLE_RADIUS.get();
         double reachSqr = reach * reach;
         int before = list.size();
         list.removeIf(hole -> hole.at.distanceToSqr(at) < reachSqr);
         int bumped = before - list.size();
         list.add(new BlackHoleKeeper.Hole(at, 90, maid.getUUID(), true, victimMax));

         while (list.size() > 32) {
            list.remove(0);
         }

         ShenlinMod.LOGGER
            .info(
               "[shenlin] hole born exec=1 maid={} at={} ticks={} max={} live={} bumped={}",
               new Object[]{maid.getId(), String.format("%.2f/%.2f/%.2f", at.x, at.y, at.z), 90, victimMax, list.size(), bumped}
            );
      }
   }

   public static void spawnExecutionHole(ServerLevel level, EntityMaid maid, Vec3 at, float victimMax) {
      if ((Boolean)Config.BLACK_HOLE_ENABLED.get()) {
         spawnExecution(level, maid, at, victimMax);
         ModPackets.sendBlackHole(level, at, 64.0, 1.6666666F, 10.0F, ((Double)Config.BLACK_HOLE_RADIUS.get()).floatValue(), 0, true);
      }
   }

   public static void tick(MinecraftServer server) {
      for (ServerLevel level : server.getAllLevels()) {
         List<BlackHoleKeeper.Hole> list = HOLES.get(level.dimension().location());
         if (list != null && !list.isEmpty()) {
            try {
               step(level, list);
            } catch (Throwable t) {
               ShenlinMod.LOGGER.warn("[shenlin] black hole tick failed: {}", t.toString());
            }

            if (list.isEmpty()) {
               HOLES.remove(level.dimension().location());
            }
         }
      }
   }

   private static void step(ServerLevel level, List<BlackHoleKeeper.Hole> list) {
      double radius = (Double)Config.BLACK_HOLE_RADIUS.get();
      double speed = (Double)Config.BLACK_HOLE_PULL_SPEED.get();
      Iterator<BlackHoleKeeper.Hole> it = list.iterator();

      while (it.hasNext()) {
         BlackHoleKeeper.Hole hole = it.next();
         if (--hole.left <= 0) {
            it.remove();
         } else {
            try {
               stepOne(level, hole, radius, speed);
            } catch (Throwable t) {
               ShenlinMod.LOGGER.warn("[shenlin] hole step failed: {}", t.toString());
            }
         }
      }
   }

   private static void stepOne(ServerLevel level, BlackHoleKeeper.Hole hole, double radius, double speed) {
      EntityMaid owner = level.getEntity(hole.owner) instanceof EntityMaid maid ? maid : null;
      if (owner == null) {
         hole.left = 0;
      } else {
         UUID ownerId = ownerOf(owner);
         Set<ResourceLocation> friendly = friendlyTypes(owner);
         if (hole.execution) {
            stepExecution(level, hole, owner, ownerId, friendly);
         } else {
            AABB box = new AABB(hole.at, hole.at).inflate(radius + 1.0);

            for (Entity entity : level.getEntities(owner, box)) {
               if (!skip(owner, ownerId, friendly, entity)) {
                  pull(hole, owner, entity, speed);
               }
            }
         }
      }
   }

   private static void stepExecution(ServerLevel level, BlackHoleKeeper.Hole hole, EntityMaid owner, UUID ownerId, Set<ResourceLocation> friendly) {
      hole.age++;
      if (!hole.gripped && (hole.age - 1) * 50L >= 733L) {
         hole.gripped = true;
         hole.nextDrainAt = hole.age;
         double grip = (Double)Config.BLACK_HOLE_EXEC_GRIP_RADIUS.get();
         AABB box = new AABB(hole.at, hole.at).inflate(grip);

         for (Entity entity : level.getEntities(owner, box)) {
            if (!skip(owner, ownerId, friendly, entity)) {
               hole.held.add(entity.getUUID());
            }
         }

         ShenlinMod.LOGGER
            .info(
               "[shenlin] hole grip caught={} at={}", hole.held.size(), String.format("%.2f/%.2f/%.2f", hole.at.x, hole.at.y, hole.at.z)
            );
      }

      if (hole.gripped) {
         for (UUID id : hole.held) {
            Entity entity = level.getEntity(id);
            if (entity != null && !entity.isRemoved()) {
               try {
                  pin(entity, hole.at);
               } catch (Throwable t) {
                  ShenlinMod.LOGGER.warn("[shenlin] hole hold failed: {}", t.toString());
               }
            }
         }

         if (hole.age >= hole.nextDrainAt) {
            hole.nextDrainAt = hole.age + 20;
            double ratio = (Double)Config.BLACK_HOLE_EXEC_DRAIN_RATIO.get();
            if (!(ratio <= 0.0) && !(hole.victimMax <= 0.0F)) {
               float amount = hole.victimMax * (float)ratio;

               for (UUID id : hole.held) {
                  Entity entity = level.getEntity(id);
                  if (entity != null) {
                     try {
                        drain(owner, entity, amount);
                     } catch (Throwable t) {
                        ShenlinMod.LOGGER.warn("[shenlin] hole drain failed: {}", t.toString());
                     }
                  }
               }
            }
         }
      }
   }

   private static void pin(Entity entity, Vec3 center) {
      entity.setDeltaMovement(Vec3.ZERO);
      entity.fallDistance = 0.0F;
      if (entity instanceof ServerPlayer player) {
         player.connection.teleport(center.x, center.y, center.z, player.getYRot(), player.getXRot());
      } else {
         entity.teleportTo(center.x, center.y, center.z);
         if (entity instanceof Mob mob) {
            mob.getNavigation().stop();
         }
      }
   }

   private static void drain(EntityMaid owner, Entity entity, float amount) {
      if (entity instanceof LivingEntity living && !living.isRemoved() && !(amount <= 0.0F)) {
         if (!(living instanceof Player) && !(living instanceof EntityMaid)) {
            float before = Executioner.readHealthRaw(living);
            if (!(before <= 0.0F)) {
               float after = Math.max(0.0F, before - amount);
               Executioner.setHealthSilently(living, after);
               if (after > 0.0F && ExecutionCounter.recordIssued(owner, living, amount)) {
                  Executioner.executeByOwner(owner, living, "hole");
               }

               if (Executioner.reached(living, 0.0F) && living.deathTime <= 0) {
                  try {
                     living.die(holeSource(owner));
                  } catch (Throwable t) {
                     ShenlinMod.LOGGER.warn("[shenlin] hole drain finish failed: {}", t.toString());
                  }
               }

               long wall = System.currentTimeMillis();
               if (wall - lastDrainLog > 1000L) {
                  lastDrainLog = wall;
                  ShenlinMod.LOGGER.info("[shenlin] hole drain {} hp {} amount {}", new Object[]{living.getType().getDescriptionId(), before, amount});
               }
            }
         } else {
            try {
               living.hurt(holeSource(owner), amount);
            } catch (Throwable t) {
               ShenlinMod.LOGGER.warn("[shenlin] hole drain hit failed: {}", t.toString());
            }
         }
      }
   }

   private static void pull(BlackHoleKeeper.Hole hole, EntityMaid owner, Entity entity, double speed) {
      Vec3 offset = hole.at.subtract(entity.position());
      double distance = offset.length();
      if (gapTo(hole.at, entity.getBoundingBox()) <= (Double)Config.BLACK_HOLE_HIT_RADIUS.get()) {
         crush(hole, owner, entity);
      }

      if (!(distance < 1.0E-4)) {
         if (!(speed <= 0.0)) {
            Vec3 direction = offset.scale(1.0 / distance);
            entity.push(direction.x * speed / 20.0, direction.y * speed / 20.0, direction.z * speed / 20.0);
            if (entity instanceof ServerPlayer player) {
               player.connection.send(new ClientboundSetEntityMotionPacket(player));
            }
         }
      }
   }

   private static double gapTo(Vec3 center, AABB box) {
      double dx = center.x - Mth.clamp(center.x, box.minX, box.maxX);
      double dy = center.y - Mth.clamp(center.y, box.minY, box.maxY);
      double dz = center.z - Mth.clamp(center.z, box.minZ, box.maxZ);
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   private static void crush(BlackHoleKeeper.Hole hole, EntityMaid owner, Entity entity) {
      if (entity instanceof LivingEntity living && !living.isDeadOrDying() && !living.isRemoved()) {
         double ratio = (Double)Config.BLACK_HOLE_CRUSH_RATIO.get();
         if (!(ratio <= 0.0)) {
            float before = Executioner.readHealthForRatio(living);
            if (!(before <= 0.0F)) {
               float amount = Math.max(0.0F, before * (float)ratio);
               if (!(amount <= 0.0F)) {
                  boolean hit = false;
                  DamageSource source = holeSource(owner);

                  try {
                     hit = living.hurt(source, amount);
                  } catch (Throwable t) {
                     ShenlinMod.LOGGER.warn("[shenlin] hole crush failed: {}", t.toString());
                  }

                  if (isHoleDamage(source) && before - amount > 0.0F && ExecutionCounter.recordIssued(owner, living, amount)) {
                     Executioner.executeByOwner(owner, living, "hole");
                  }

                  if (!hit) {
                     Executioner.setHealthSilently(living, Math.max(0.0F, before - amount));
                     if (Executioner.reached(living, 0.0F) && living.deathTime <= 0) {
                        try {
                           living.die(holeSource(owner));
                        } catch (Throwable t) {
                           ShenlinMod.LOGGER.warn("[shenlin] hole crush finish failed: {}", t.toString());
                        }
                     }
                  }

                  long wall = System.currentTimeMillis();
                  if (wall - lastCrushLog > 1000L) {
                     lastCrushLog = wall;
                     ShenlinMod.LOGGER.info("[shenlin] hole crush {} hp {} amount {} hit={}", new Object[]{living.getType().getDescriptionId(), before, amount, hit});
                  }
               }
            }
         }
      }
   }

   public static DamageSource holeSource(EntityMaid owner) {
      try {
         Holder<DamageType> type = owner.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(HOLE_DAMAGE);
         return new DamageSource(type, owner);
      } catch (Throwable t) {
         return owner.damageSources().drown();
      }
   }

   public static boolean isHoleDamage(DamageSource source) {
      return source != null && source.is(HOLE_DAMAGE);
   }

   private static boolean skip(EntityMaid maid, UUID ownerId, Set<ResourceLocation> friendly, Entity entity) {
      if (entity == maid || !entity.isAlive() || entity.isRemoved()) {
         return true;
      } else if (entity instanceof Player) {
         return entity.getUUID().equals(ownerId);
      } else if (entity instanceof EntityMaid other) {
         return ownerId != null && ownerId.equals(ownerOf(other));
      } else {
         ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
         return type != null && friendly.contains(type);
      }
   }

   private static UUID ownerOf(EntityMaid maid) {
      try {
         return maid.getOwnerUUID();
      } catch (Throwable t) {
         return null;
      }
   }

   private static Set<ResourceLocation> friendlyTypes(EntityMaid maid) {
      try {
         AttackListData data = (AttackListData)maid.getData(InitTaskData.ATTACK_LIST);
         if (data != null && !data.attackGroups().isEmpty()) {
            Set<ResourceLocation> friendly = new HashSet<>();

            for (Entry<ResourceLocation, MonsterType> entry : data.attackGroups().entrySet()) {
               if (entry.getValue() == MonsterType.FRIENDLY) {
                  friendly.add(entry.getKey());
               }
            }

            return friendly;
         } else {
            return Set.of();
         }
      } catch (Throwable t) {
         return Set.of();
      }
   }

   public static void forgetAll() {
      HOLES.clear();
   }

   public static void clear(UUID maidId) {
      for (List<BlackHoleKeeper.Hole> list : HOLES.values()) {
         list.removeIf(hole -> hole.owner.equals(maidId));
      }
   }

   public static int clearNear(ServerLevel level, Vec3 at, double radius) {
      List<BlackHoleKeeper.Hole> list = HOLES.get(level.dimension().location());
      if (list != null && !list.isEmpty()) {
         double sq = radius * radius;
         int before = list.size();
         list.removeIf(hole -> hole.at.distanceToSqr(at) < sq);
         return before - list.size();
      } else {
         return 0;
      }
   }

   private static final class Hole {
      private final Vec3 at;
      private final UUID owner;
      private int left;
      private final boolean execution;
      private final float victimMax;
      private int age;
      private boolean gripped;
      private final Set<UUID> held = new HashSet<>();
      private int nextDrainAt;

      private Hole(Vec3 at, int ticks, UUID owner) {
         this(at, ticks, owner, false, 0.0F);
      }

      private Hole(Vec3 at, int ticks, UUID owner, boolean execution, float victimMax) {
         this.at = at;
         this.left = ticks;
         this.owner = owner;
         this.execution = execution;
         this.victimMax = victimMax;
      }
   }
}

