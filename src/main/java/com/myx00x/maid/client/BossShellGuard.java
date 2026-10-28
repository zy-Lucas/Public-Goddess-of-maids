package com.myx00x.maid.client;

import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.boss.ShellHold;
import com.myx00x.maid.boss.ShenlinBossEntity;
import com.myx00x.maid.network.ModPackets;
import com.myx00x.maid.registry.ModEntities;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.RenderTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT, bus = Bus.FORGE)
public final class BossShellGuard {
   private static final int SPARE_ID = -24081;
   private static volatile int current;
   private static volatile long currentUntil;
   private static final Map<Integer, double[]> SPOT = new ConcurrentHashMap<>();
   private static final Map<Integer, Long> UNTIL = new ConcurrentHashMap<>();
   private static final Map<Integer, Boolean> GUARDED = new ConcurrentHashMap<>();
   private static final Set<Integer> MADE = ConcurrentHashMap.newKeySet();
   private static final Map<Integer, Long> LAST_LOG = new ConcurrentHashMap<>();
   private static long lastAckAt;
   private static int missingTicks;
   private static ClientLevel boundLevel;
   private static boolean shadowPainting;
   private static final Set<Integer> PAINTED = ConcurrentHashMap.newKeySet();
   private static volatile boolean resolved;
   private static Field storageField;
   private static Method addEntityMethod;

   private BossShellGuard() {
   }

   public static void apply(int id, double x, double y, double z, float yRot, float xRot, int millis, boolean guarded) {
      if (id < 0) {
         SPOT.clear();
         UNTIL.clear();
         GUARDED.clear();
         current = 0;
         currentUntil = 0L;
      } else {
         SPOT.put(id, new double[]{x, y, z, yRot, xRot});
         UNTIL.put(id, System.currentTimeMillis() + Math.max(0, millis));
         GUARDED.put(id, guarded);
         current = id;
         currentUntil = UNTIL.get(id);
      }
   }

   public static List<Integer> liveIds() {
      List<Integer> out = new ArrayList<>();

      for (Integer id : SPOT.keySet()) {
         if (Boolean.TRUE.equals(GUARDED.get(id))) {
            out.add(id);
         }
      }

      return out;
   }

   public static boolean isGuarded(int id) {
      return Boolean.TRUE.equals(GUARDED.get(id));
   }

   static void shadowPass(boolean on) {
      shadowPainting = on;
   }

   static void markPainted(int id) {
      if (!shadowPainting) {
         PAINTED.add(id);
      }
   }

   static Set<Integer> takePainted() {
      Set<Integer> was = new HashSet<>(PAINTED);
      PAINTED.clear();
      return was;
   }

   static boolean isVisible(int id) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level == null) {
         return false;
      } else if (shadowPainting) {
         return true;
      } else {
         return id == -24081 ? isGuarded(current) && !healthy(level.getEntity(current)) : isGuarded(id) && healthy(level.getEntity(id));
      }
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.END) {
         sweep();
      }
   }

   @SubscribeEvent
   public static void onRenderTick(RenderTickEvent event) {
      if (event.phase == Phase.START) {
         Minecraft minecraft = Minecraft.getInstance();
         ClientLevel level = minecraft.level;
         if (level != null && level == boundLevel && current != 0) {
            long now = System.currentTimeMillis();

            for (int id : liveIds()) {
               ensure(level, id, now);
            }

            ensure(level, -24081, now);
            aimHit(minecraft);
         }
      }
   }

   public static double[] spotStale() {
      return spotStale(current);
   }

   public static double[] spotStale(int id) {
      return Boolean.TRUE.equals(GUARDED.get(id)) ? SPOT.get(id) : null;
   }

   public static EntityHitResult aimed() {
      try {
         return aimed0();
      } catch (Throwable t) {
         return null;
      }
   }

   private static EntityHitResult aimed0() {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player != null && minecraft.level != null) {
         Player player = minecraft.player;
         if (player.isSpectator()) {
            return null;
         }

         List<Integer> live = liveIds();
         if (live.isEmpty()) {
            return null;
         }

         EntityDimensions size = ((EntityType)ModEntities.SHENLIN_BOSS.get()).getDimensions();
         double half = size.width * 0.5;
         Vec3 eye = player.getEyePosition();
         Vec3 end = eye.add(player.getViewVector(1.0F).scale(player.getEntityReach()));
         EntityHitResult best = null;
         double bestDist = Double.MAX_VALUE;

         for (int id : live) {
            double[] spot = spotStale(id);
            if (spot != null) {
               AABB crate = new AABB(spot[0] - half, spot[1], spot[2] - half, spot[0] + half, spot[1] + size.height, spot[2] + half);
               Optional<Vec3> cut = crate.clip(eye, end);
               if (!cut.isEmpty()) {
                  double dist = cut.get().distanceToSqr(eye);
                  if (!(dist >= bestDist)) {
                     Entity target = minecraft.level.getEntity(id);
                     if (target != null && !target.isRemoved()) {
                        bestDist = dist;
                        best = new EntityHitResult(target, cut.get());
                     }
                  }
               }
            }
         }

         return best;
      } else {
         return null;
      }
   }

   private static void aimHit(Minecraft minecraft) {
      EntityHitResult aimed = aimed();
      if (aimed != null) {
         if (!(minecraft.hitResult instanceof EntityHitResult already && already.getEntity() == aimed.getEntity())) {
            if (minecraft.hitResult == null
               || minecraft.hitResult.getType() == Type.MISS
               || !(minecraft.hitResult.getLocation().distanceToSqr(minecraft.player.getEyePosition()) < aimed.getLocation().distanceToSqr(minecraft.player.getEyePosition()))) {
               minecraft.hitResult = aimed;
            }
         }
      }
   }

   private static void sweep() {
      Minecraft minecraft = Minecraft.getInstance();
      ClientLevel level = minecraft.level;
      if (level == null) {
         forgetEverything();
      } else if (level != boundLevel) {
         boundLevel = level;
         forgetEverything();
      } else if (current != 0 || !MADE.isEmpty()) {
         long now = System.currentTimeMillis();
         if (current != 0 && now > currentUntil) {
            ShellHold.release(-24081);
            level.removeEntity(-24081, RemovalReason.DISCARDED);
            MADE.remove(-24081);
            SPOT.remove(-24081);
         } else {
            judgeLost(level, minecraft);
            pushOut(minecraft);
            alignSpare(level);

            for (int id : liveIds()) {
               ensure(level, id, now);
            }

            ensure(level, -24081, now);
            ack(level, now);
            List<Integer> keepIds = liveIds();

            for (Integer id : MADE) {
               if (!keepIds.contains(id) && id != -24081) {
                  ShellHold.release(id);
                  level.removeEntity(id, RemovalReason.DISCARDED);
                  MADE.remove(id);
                  SPOT.remove(id);
                  LAST_LOG.remove(id);
               }
            }
         }
      }
   }

   private static void alignSpare(ClientLevel level) {
      double[] stand = SPOT.get(current);
      if (stand != null) {
         double[] was = SPOT.get(-24081);
         if (was == null || was[0] != stand[0] || was[1] != stand[1] || was[2] != stand[2]) {
            SPOT.put(-24081, new double[]{stand[0], stand[1], stand[2], stand[3], stand[4]});
         }

         Entity mate = level.getEntity(-24081);
         if (mate != null && (mate.getX() != stand[0] || mate.getY() != stand[1] || mate.getZ() != stand[2])) {
            mate.setPos(stand[0], stand[1], stand[2]);
            face(mate, stand[3], stand[4]);
         }
      }
   }

   public static boolean trackedNow() {
      return current != 0 && System.currentTimeMillis() <= currentUntil;
   }

   private static void pushOut(Minecraft minecraft) {
      if (minecraft.player != null) {
         Player player = minecraft.player;
         if (!player.isSpectator() && !player.isPassenger() && !player.isDeadOrDying()) {
            EntityDimensions size = ((EntityType)ModEntities.SHENLIN_BOSS.get()).getDimensions();

            for (int id : liveIds()) {
               double[] spot = spotStale(id);
               if (spot != null) {
                  pushFrom(player, spot, size);
               }
            }
         }
      }
   }

   private static void pushFrom(Player player, double[] spot, EntityDimensions size) {
      double half = size.width * 0.5;
      AABB crate = new AABB(spot[0] - half, spot[1], spot[2] - half, spot[0] + half, spot[1] + size.height, spot[2] + half);
      AABB body = player.getBoundingBox();
      if (body.intersects(crate)) {
         double overlapX = Math.min(body.maxX - crate.minX, crate.maxX - body.minX);
         double overlapZ = Math.min(body.maxZ - crate.minZ, crate.maxZ - body.minZ);
         if (!(overlapX <= 0.0) && !(overlapZ <= 0.0)) {
            boolean alongX = overlapX < overlapZ;
            double depth = alongX ? overlapX : overlapZ;
            double sign = alongX
               ? ((body.minX + body.maxX) * 0.5 < spot[0] ? -1.0 : 1.0)
               : ((body.minZ + body.maxZ) * 0.5 < spot[2] ? -1.0 : 1.0);
            double shove = 0.05 + Math.min(0.04, depth * 0.15);
            if (alongX) {
               player.push(sign * shove, 0.0, 0.0);
            } else {
               player.push(0.0, 0.0, sign * shove);
            }

            if (depth > 0.5) {
               double spare = depth - 0.35;
               if (alongX) {
                  player.setPos(player.getX() + sign * spare, player.getY(), player.getZ());
               } else {
                  player.setPos(player.getX(), player.getY(), player.getZ() + sign * spare);
               }
            }
         }
      }
   }

   private static void judgeLost(ClientLevel level, Minecraft minecraft) {
      if (current == 0 || minecraft.player == null || System.currentTimeMillis() > currentUntil) {
         missingTicks = 0;
      } else if (areaHas(level, current)) {
         missingTicks = 0;
      } else {
         if (++missingTicks >= 2) {
            missingTicks = 0;
            ShenlinMod.LOGGER.info("[shenlin] client lost shell id={}, reported", current);
            ModPackets.reportShellLost(current);
         }
      }
   }

   private static boolean areaHas(ClientLevel level, int id) {
      Entity main = level.getEntity(id);
      if (main == null) {
         return false;
      } else {
         AABB box = main.getBoundingBox();
         if (box.getXsize() > 0.0 && box.getYsize() > 0.0) {
            boolean[] hit = new boolean[]{false};
            level.getEntities(EntityTypeTest.forClass(Entity.class), box, found -> {
               if (found.getId() == id) {
                  hit[0] = true;
               }

               return false;
            });
            return hit[0];
         } else {
            return false;
         }
      }
   }

   private static void ack(ClientLevel level, long now) {
      if (current != 0 && now - lastAckAt >= 1000L) {
         lastAckAt = now;
         Entity main = level.getEntity(current);
         boolean ok = healthy(main);
         double width = main != null ? main.getBoundingBox().getXsize() : 0.0;
         double height = main != null ? main.getBoundingBox().getYsize() : 0.0;
         ModPackets.sendBossShellAck(current, ok, areaHas(level, current), isVisible(current), width, height);
      }
   }

   private static boolean onClient(ClientLevel level, int id) {
      return id != 0 && level.getEntity(id) != null;
   }

   private static void ensure(ClientLevel level, int id, long now) {
      if (id != 0) {
         Entity here = level.getEntity(id);
         if (here == null || !healthy(here)) {
            double[] spot = SPOT.get(id);
            if (spot != null) {
               if (!mend(here, spot)) {
                  if (here != null) {
                     ShellHold.release(id);
                     level.removeEntity(id, RemovalReason.DISCARDED);
                     MADE.remove(id);
                  }

                  putBack(level, id, spot, now);
               }
            }
         }
      }
   }

   private static boolean mend(Entity entity, double[] spot) {
      if (entity instanceof ShenlinBossEntity boss && !boss.isRemoved()) {
         try {
            if (boss.isInvisible()) {
               boss.setInvisible(false);
            }

            boolean moved = boss.getX() != spot[0] || boss.getY() != spot[1] || boss.getZ() != spot[2];
            if (moved) {
               boss.setPos(spot[0], spot[1], spot[2]);
            }

            if (boss.getYRot() != spot[3] || boss.getXRot() != spot[4]) {
               face(boss, spot[3], spot[4]);
               moved = true;
            }

            AABB box = boss.getBoundingBox();
            if (box.getXsize() <= 0.01 || box.getYsize() <= 0.01) {
               boss.refreshDimensions();
            }

            return healthy(boss);
         } catch (Throwable ignored) {
            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean healthy(Entity entity) {
      return entity instanceof ShenlinBossEntity
         && !entity.isRemoved()
         && !entity.isInvisible()
         && entity.getBoundingBox().getXsize() > 0.01
         && entity.getBoundingBox().getYsize() > 0.01;
   }

   private static void releaseAll(ClientLevel level) {
      for (Integer id : MADE) {
         ShellHold.release(id);
         level.removeEntity(id, RemovalReason.DISCARDED);
      }
   }

   private static void putBack(ClientLevel level, int id, double[] spot, long now) {
      resolve();
      if (storageField != null && addEntityMethod != null) {
         try {
            ShenlinBossEntity fresh = new ShenlinBossEntity((EntityType<? extends Entity>)ModEntities.SHENLIN_BOSS.get(), level);
            fresh.setId(id);
            fresh.markShadow(id != current);
            fresh.setPos(spot[0], spot[1], spot[2]);
            fresh.setInvisible(false);
            face(fresh, spot[3], spot[4]);
            Object storage = storageField.get(level);
            if (storage == null) {
               return;
            }

            addEntityMethod.invoke(storage, fresh);
            MADE.add(id);
            ShellHold.hold(id);
            Long seen = LAST_LOG.get(id);
            if (seen == null || now - seen > 2000L) {
               LAST_LOG.put(id, now);
               ShenlinMod.LOGGER.info("[shenlin] boss shell put back on the client, id={}", id);
            }
         } catch (Throwable var8) {
         }
      }
   }

   private static void face(Entity entity, double yRot, double xRot) {
      float yaw = (float)yRot;
      float pitch = (float)xRot;
      entity.setYRot(yaw);
      entity.setXRot(pitch);
      entity.yRotO = yaw;
      entity.xRotO = pitch;
      if (entity instanceof LivingEntity living) {
         living.yHeadRot = yaw;
         living.yHeadRotO = yaw;
         living.yBodyRot = yaw;
         living.yBodyRotO = yaw;
      }
   }

   private static void forgetEverything() {
      for (Integer id : MADE) {
         ShellHold.release(id);
      }

      ShellHold.release(current);
      ShellHold.release(-24081);
      current = 0;
      currentUntil = 0L;
      SPOT.clear();
      UNTIL.clear();
      GUARDED.clear();
      PAINTED.clear();
      MADE.clear();
      LAST_LOG.clear();
   }

   private static void resolve() {
      if (!resolved) {
         resolved = true;
         storageField = findField(ClientLevel.class, "f_171631_", "entityStorage");

         try {
            Class<?> manager = Class.forName("net.minecraft.world.level.entity.TransientEntitySectionManager");
            addEntityMethod = findMethod(manager, "m_157653_", "addEntity", EntityAccess.class);
         } catch (Throwable ignored) {
            addEntityMethod = null;
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

