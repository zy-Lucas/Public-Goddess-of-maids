package com.myx00x.maid.network;

import com.myx00x.maid.Config;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkRegistry.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor.TargetPoint;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModPackets {
   private static final double FORM_RANGE = 64.0;
   private static final double SHAKE_SEEN_RANGE = 192.0;
   private static final int SHELL_HOLD_MS = 2000;
   private static SimpleChannel channel;
   private static final double BLINK_SPREAD = 64.0;

   public static double seenRange() {
      return 192.0;
   }

   private static double seenAtLeast(double range) {
      return Math.max(range, 192.0);
   }

   private static double fogReach(double range) {
      double want = seenAtLeast(range);
      double cap = Config.relicFogVisibleRange();
      return cap > 0.5 ? Math.min(want, cap) : want;
   }

   private ModPackets() {
   }

   public static void register() {
      if (channel == null) {
         channel = ChannelBuilder.named(ResourceLocation.fromNamespaceAndPath("goddess_of_maids", "main"))
            .networkProtocolVersion(() -> "1")
            .clientAcceptedVersions(v -> true)
            .serverAcceptedVersions(v -> true)
            .simpleChannel();
         channel.messageBuilder(FlashPacket.class, 1).encoder(FlashPacket::encode).decoder(FlashPacket::decode).consumerMainThread(FlashPacket::handle).add();
         channel.messageBuilder(ShockwavePacket.class, 2)
            .encoder(ShockwavePacket::encode)
            .decoder(ShockwavePacket::decode)
            .consumerMainThread(ShockwavePacket::handle)
            .add();
         channel.messageBuilder(RayPacket.class, 3).encoder(RayPacket::encode).decoder(RayPacket::decode).consumerMainThread(RayPacket::handle).add();
         channel.messageBuilder(ShakePacket.class, 4).encoder(ShakePacket::encode).decoder(ShakePacket::decode).consumerMainThread(ShakePacket::handle).add();
         channel.messageBuilder(MarkPacket.class, 5).encoder(MarkPacket::encode).decoder(MarkPacket::decode).consumerMainThread(MarkPacket::handle).add();
         channel.messageBuilder(BlackHolePacket.class, 6)
            .encoder(BlackHolePacket::encode)
            .decoder(BlackHolePacket::decode)
            .consumerMainThread(BlackHolePacket::handle)
            .add();
         channel.messageBuilder(HoleClearPacket.class, 7)
            .encoder(HoleClearPacket::encode)
            .decoder(HoleClearPacket::decode)
            .consumerMainThread(HoleClearPacket::handle)
            .add();
         channel.messageBuilder(OrbitPacket.class, 8).encoder(OrbitPacket::encode).decoder(OrbitPacket::decode).consumerMainThread(OrbitPacket::handle).add();
         channel.messageBuilder(CannonFormPacket.class, 9)
            .encoder(CannonFormPacket::encode)
            .decoder(CannonFormPacket::decode)
            .consumerMainThread(CannonFormPacket::handle)
            .add();
         channel.messageBuilder(CannonShotPacket.class, 10)
            .encoder(CannonShotPacket::encode)
            .decoder(CannonShotPacket::decode)
            .consumerMainThread(CannonShotPacket::handle)
            .add();
         channel.messageBuilder(CannonSpinPacket.class, 11)
            .encoder(CannonSpinPacket::encode)
            .decoder(CannonSpinPacket::decode)
            .consumerMainThread(CannonSpinPacket::handle)
            .add();
         channel.messageBuilder(SpinHitPacket.class, 12)
            .encoder(SpinHitPacket::encode)
            .decoder(SpinHitPacket::decode)
            .consumerMainThread(SpinHitPacket::handle)
            .add();
         channel.messageBuilder(SoloBeamPacket.class, 13)
            .encoder(SoloBeamPacket::encode)
            .decoder(SoloBeamPacket::decode)
            .consumerMainThread(SoloBeamPacket::handle)
            .add();
         channel.messageBuilder(GuardShieldPacket.class, 14)
            .encoder(GuardShieldPacket::encode)
            .decoder(GuardShieldPacket::decode)
            .consumerMainThread(GuardShieldPacket::handle)
            .add();
         channel.messageBuilder(GuardPassPacket.class, 15)
            .encoder(GuardPassPacket::encode)
            .decoder(GuardPassPacket::decode)
            .consumerMainThread(GuardPassPacket::handle)
            .add();
         channel.messageBuilder(RuinedPhantasmSpotPacket.class, 29)
            .encoder(RuinedPhantasmSpotPacket::encode)
            .decoder(RuinedPhantasmSpotPacket::decode)
            .consumerMainThread(RuinedPhantasmSpotPacket::handle)
            .add();
         channel.messageBuilder(BossShellPacket.class, 16)
            .encoder(BossShellPacket::encode)
            .decoder(BossShellPacket::decode)
            .consumerMainThread(BossShellPacket::handle)
            .add();
         channel.messageBuilder(RelicMusicPacket.class, 17)
            .encoder(RelicMusicPacket::encode)
            .decoder(RelicMusicPacket::decode)
            .consumerMainThread(RelicMusicPacket::handle)
            .add();
         channel.messageBuilder(ShockWarpPacket.class, 18)
            .encoder(ShockWarpPacket::encode)
            .decoder(ShockWarpPacket::decode)
            .consumerMainThread(ShockWarpPacket::handle)
            .add();
         channel.messageBuilder(GhostPacket.class, 19).encoder(GhostPacket::encode).decoder(GhostPacket::decode).consumerMainThread(GhostPacket::handle).add();
         channel.messageBuilder(ScreenRipplePacket.class, 21)
            .encoder(ScreenRipplePacket::encode)
            .decoder(ScreenRipplePacket::decode)
            .consumerMainThread(ScreenRipplePacket::handle)
            .add();
         channel.messageBuilder(RelicDoorPacket.class, 22)
            .encoder(RelicDoorPacket::encode)
            .decoder(RelicDoorPacket::decode)
            .consumerMainThread(RelicDoorPacket::handle)
            .add();
         channel.messageBuilder(DamnationPacket.class, 23)
            .encoder(DamnationPacket::encode)
            .decoder(DamnationPacket::decode)
            .consumerMainThread(DamnationPacket::handle)
            .add();
         channel.messageBuilder(BossShellAckPacket.class, 24)
            .encoder(BossShellAckPacket::encode)
            .decoder(BossShellAckPacket::decode)
            .consumerMainThread(BossShellAckPacket::handle)
            .add();
         channel.messageBuilder(BossShellLostPacket.class, 25)
            .encoder(BossShellLostPacket::encode)
            .decoder(BossShellLostPacket::decode)
            .consumerMainThread(BossShellLostPacket::handle)
            .add();
         channel.messageBuilder(FogBurstPacket.class, 20)
            .encoder(FogBurstPacket::encode)
            .decoder(FogBurstPacket::decode)
            .consumerMainThread(FogBurstPacket::handle)
            .add();
         channel.messageBuilder(FogPacket.class, 26).encoder(FogPacket::encode).decoder(FogPacket::decode).consumerMainThread(FogPacket::handle).add();
         channel.messageBuilder(WardenBlinkPacket.class, 27)
            .encoder(WardenBlinkPacket::encode)
            .decoder(WardenBlinkPacket::decode)
            .consumerMainThread(WardenBlinkPacket::handle)
            .add();
         channel.messageBuilder(VolumetricFogPacket.class, 28)
            .encoder(VolumetricFogPacket::encode)
            .decoder(VolumetricFogPacket::decode)
            .consumerMainThread(VolumetricFogPacket::handle)
            .add();
      }
   }

   public static void reportShellLost(int id) {
      if (channel != null) {
         channel.sendToServer(new BossShellLostPacket(id));
      }
   }

   public static void sendBossShellAck(int id, boolean ok, boolean area, boolean visible, double width, double height) {
      if (channel != null) {
         channel.sendToServer(new BossShellAckPacket(id, ok, area, visible, (float)width, (float)height));
      }
   }

   public static void sendDamnation(ServerPlayer player, String who) {
      if (channel != null && player != null) {
         channel.send(PacketDistributor.PLAYER.with(() -> player), new DamnationPacket(1, who));
      }
   }

   public static void sendDamnationClear(ServerPlayer player) {
      if (channel != null && player != null) {
         channel.send(PacketDistributor.PLAYER.with(() -> player), new DamnationPacket(0, ""));
      }
   }

   public static void confessDamnation() {
      if (channel != null) {
         channel.sendToServer(new DamnationPacket(0, ""));
      }
   }

   public static void sendRelicDoor(ServerPlayer player, int action, double x, double y, double z, int ticks) {
      if (player != null) {
         channel.send(PacketDistributor.PLAYER.with(() -> player), new RelicDoorPacket(action, x, y, z, ticks));
      }
   }

   public static void sendRelicDoor(ServerPlayer player, int action, double x, double y, double z, double dx, double dy, double dz, int ticks) {
      if (player != null) {
         channel.send(PacketDistributor.PLAYER.with(() -> player), new RelicDoorPacket(action, x, y, z, dx, dy, dz, ticks));
      }
   }

   public static void sendFog(ServerPlayer player, double x, double y, double z, float radius, float r, float g, float b, int ticks) {
      if (player != null) {
         channel.send(PacketDistributor.PLAYER.with(() -> player), new FogPacket(x, y, z, radius, r, g, b, ticks));
      }
   }

   public static void sendFog(ServerLevel level, Vec3 pos, double range, float radius, float r, float g, float b, int ticks) {
      if (level != null && pos != null) {
         channel.send(
            PacketDistributor.NEAR.with(() -> new TargetPoint(pos.x, pos.y, pos.z, fogReach(range), level.dimension())),
            new FogPacket(pos.x, pos.y, pos.z, radius, r, g, b, ticks)
         );
      }
   }

   public static void sendFog(
      ServerPlayer player,
      double x,
      double y,
      double z,
      float radius,
      float r,
      float g,
      float b,
      int ticks,
      int mode,
      double dx,
      double dy,
      double dz,
      double speed
   ) {
      if (player != null) {
         channel.send(PacketDistributor.PLAYER.with(() -> player), new FogPacket(x, y, z, radius, r, g, b, ticks, mode, dx, dy, dz, speed));
      }
   }

   public static void sendFog(
      ServerLevel level, Vec3 pos, double range, float radius, float r, float g, float b, int ticks, int mode, double dx, double dy, double dz, double speed
   ) {
      if (level != null && pos != null) {
         channel.send(
            PacketDistributor.NEAR.with(() -> new TargetPoint(pos.x, pos.y, pos.z, fogReach(range), level.dimension())),
            new FogPacket(pos.x, pos.y, pos.z, radius, r, g, b, ticks, mode, dx, dy, dz, speed)
         );
      }
   }

   public static void sendFog(
      ServerLevel level,
      Vec3 pos,
      double range,
      float radius,
      float r,
      float g,
      float b,
      int ticks,
      int mode,
      double dx,
      double dy,
      double dz,
      double speed,
      float alphaScale
   ) {
      if (level != null && pos != null) {
         channel.send(
            PacketDistributor.NEAR.with(() -> new TargetPoint(pos.x, pos.y, pos.z, fogReach(range), level.dimension())),
            new FogPacket(pos.x, pos.y, pos.z, radius, r, g, b, ticks, mode, dx, dy, dz, speed, alphaScale)
         );
      }
   }

   public static void sendVolumetricFog(ServerLevel level, Vec3 pos, float radius, float density, int ticks) {
      if (level != null && pos != null) {
         sendNear(level, pos, 192.0, new VolumetricFogPacket(pos.x, pos.y, pos.z, radius, density, ticks));
      }
   }

   public static void sendFogBurst(
      ServerLevel level, double[] xs, double[] ys, double[] zs, float[] radii, float r, float g, float b, int ticks, int mode, float alphaScale
   ) {
      if (level != null && xs != null && xs.length != 0) {
         int n = xs.length;
         if (ys != null && zs != null && radii != null && ys.length >= n && zs.length >= n && radii.length >= n) {
            double cx = 0.0;
            double cy = 0.0;
            double cz = 0.0;

            for (int i = 0; i < n; i++) {
               cx += xs[i];
               cy += ys[i];
               cz += zs[i];
            }

            double far = 0.0;
            double sumX = cx / n;
            double sumY = cy / n;
            double sumZ = cz / n;

            for (int i = 0; i < n; i++) {
               double ddx = xs[i] - sumX;
               double ddy = ys[i] - sumY;
               double ddz = zs[i] - sumZ;
               far = Math.max(far, Math.sqrt(ddx * ddx + ddy * ddy + ddz * ddz));
            }

            double atX = sumX;
            double atY = sumY;
            double atZ = sumZ;
            double reach = fogReach(Math.max(far + 8.0, 0.0));
            channel.send(
               PacketDistributor.NEAR.with(() -> new TargetPoint(atX, atY, atZ, reach, level.dimension())),
               new FogBurstPacket(xs, ys, zs, radii, r, g, b, ticks, mode, alphaScale, 0.0, 0.0, 0.0, 0.0)
            );
         }
      }
   }

   public static void sendBlink(ServerLevel level, Vec3 a, Vec3 b, int ticks) {
      if (level != null && a != null && b != null && ticks > 0) {
         Vec3 mid = a.add(b).scale(0.5);
         double range = Math.max(a.distanceTo(mid), b.distanceTo(mid)) + 64.0;
         sendNear(level, mid, range, new WardenBlinkPacket(a.x, a.y, a.z, b.x, b.y, b.z, ticks));
      }
   }

   public static void sendSlamFlash(ServerLevel level, Vec3 pos, double range, int ticks) {
      if (level != null && pos != null && ticks > 0) {
         sendNear(level, pos, range, new FlashPacket(ticks, true));
      }
   }

   public static void sendGhost(boolean on) {
      if (channel != null) {
         channel.sendToServer(new GhostPacket(on));
      }
   }

   public static void sendGuardShield(ServerLevel level, int[] ids, int millis) {
      if (channel != null && level != null && ids != null && ids.length != 0) {
         channel.send(PacketDistributor.DIMENSION.with(level::m_46472_), new GuardShieldPacket(ids, millis));
      }
   }

   public static void sendGuardPass(int id) {
      if (channel != null) {
         channel.send(PacketDistributor.ALL.noArg(), new GuardPassPacket(id));
      }
   }

   public static void sendBossShell(ServerLevel level, int id, double x, double y, double z, float yRot, float xRot, boolean guarded) {
      if (channel != null && level != null) {
         channel.send(PacketDistributor.DIMENSION.with(level::m_46472_), new BossShellPacket(id, x, y, z, yRot, xRot, 2000, guarded));
      }
   }

   public static void sendBossShellGone(ServerLevel level, int id) {
      if (channel != null && level != null) {
         channel.send(PacketDistributor.DIMENSION.with(level::m_46472_), new BossShellPacket(id, 0.0, 0.0, 0.0, 0.0F, 0.0F, 0, false));
      }
   }

   public static void sendRelicMusic(ServerPlayer player, boolean inside, double gapMin, double gapMax, double range) {
      if (channel != null && player != null) {
         channel.send(PacketDistributor.PLAYER.with(() -> player), new RelicMusicPacket(inside, gapMin, gapMax, range));
      }
   }

   public static <T> void sendToTrackers(Entity entity, T packet) {
      if (channel != null) {
         channel.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), packet);
      }
   }

   public static <T> void sendNear(ServerLevel level, Vec3 pos, double range, T packet) {
      if (channel != null) {
         channel.send(PacketDistributor.NEAR.with(() -> new TargetPoint(pos.x, pos.y, pos.z, range, level.dimension())), packet);
      }
   }

   public static void sendRay(
      ServerLevel level, Vec3 center, double range, Vec3 from, Vec3 to, int ticks, float width, boolean through, int leadMs, float starRadius
   ) {
      sendRay(level, center, range, from, to, ticks, width, through, leadMs, starRadius, 1.0F, 0.1F, 0.06F);
   }

   public static void sendRay(
      ServerLevel level,
      Vec3 center,
      double range,
      Vec3 from,
      Vec3 to,
      int ticks,
      float width,
      boolean through,
      int leadMs,
      float starRadius,
      float cr,
      float cg,
      float cb
   ) {
      sendRay(level, center, range, from, to, ticks, width, through, leadMs, starRadius, cr, cg, cb, 0.0F);
   }

   public static void sendRay(
      ServerLevel level,
      Vec3 center,
      double range,
      Vec3 from,
      Vec3 to,
      int ticks,
      float width,
      boolean through,
      int leadMs,
      float starRadius,
      float cr,
      float cg,
      float cb,
      float soundRange
   ) {
      sendRay(level, center, range, from, to, ticks, width, through, leadMs, starRadius, cr, cg, cb, soundRange, 0.0F, 0.0F);
   }

   public static void sendRay(
      ServerLevel level,
      Vec3 center,
      double range,
      Vec3 from,
      Vec3 to,
      int ticks,
      float width,
      boolean through,
      int leadMs,
      float starRadius,
      float cr,
      float cg,
      float cb,
      float soundRange,
      float coreWidth,
      float burst
   ) {
      sendRay(level, center, range, from, to, ticks, width, through, leadMs, starRadius, cr, cg, cb, soundRange, coreWidth, burst, -1, false);
   }

   public static void sendRay(
      ServerLevel level,
      Vec3 center,
      double range,
      Vec3 from,
      Vec3 to,
      int ticks,
      float width,
      boolean through,
      int leadMs,
      float starRadius,
      float cr,
      float cg,
      float cb,
      float soundRange,
      float coreWidth,
      float burst,
      int followId,
      boolean column
   ) {
      sendRay(level, center, range, from, to, ticks, width, through, leadMs, starRadius, cr, cg, cb, soundRange, coreWidth, burst, followId, column, -1);
   }

   public static void sendRay(
      ServerLevel level,
      Vec3 center,
      double range,
      Vec3 from,
      Vec3 to,
      int ticks,
      float width,
      boolean through,
      int leadMs,
      float starRadius,
      float cr,
      float cg,
      float cb,
      float soundRange,
      float coreWidth,
      float burst,
      int followId,
      boolean column,
      int hangOn
   ) {
      sendNear(
         level,
         center,
         range,
         new RayPacket(
            from.x,
            from.y,
            from.z,
            to.x,
            to.y,
            to.z,
            ticks,
            width,
            through,
            leadMs,
            starRadius,
            cr,
            cg,
            cb,
            soundRange,
            coreWidth,
            burst,
            followId,
            column,
            hangOn
         )
      );
   }

   public static void sendOrbit(Entity entity, int state, int targetId, int idleMs) {
      sendToTrackers(entity, new OrbitPacket(entity.getId(), state, targetId, idleMs));
   }

   public static void sendCannonForm(
      ServerLevel level, Entity maid, int targetId, Vec3 anchor, Vec3 aim, long startTick, long chargeTicks, long totalTicks, float rayRange, float rayFar
   ) {
      sendNear(
         level,
         anchor,
         64.0,
         new CannonFormPacket(
            maid.getId(),
            anchor.x,
            anchor.y,
            anchor.z,
            aim.x,
            aim.z,
            startTick,
            (int)chargeTicks,
            (int)totalTicks,
            targetId,
            rayRange,
            rayFar
         )
      );
   }

   public static void sendCannonShot(ServerLevel level, Vec3 from, Vec3 to, long startTick) {
      sendNear(level, from, 64.0, new CannonShotPacket(from.x, from.y, from.z, to.x, to.y, to.z, startTick));
   }

   public static void sendCannonSpin(Entity maid, long startTick, int totalTicks, float rayRange, float rayFar) {
      Vec3 pos = maid.position();
      sendToTrackers(maid, new CannonSpinPacket(maid.getId(), startTick, totalTicks, pos.x, pos.y, pos.z, rayRange, rayFar));
   }

   public static void sendSoloBeam(
      Entity maid, int targetId, long startTick, int chargeTicks, int fireTicks, int fadeTicks, Vec3 at, double soundRange, double soundFar
   ) {
      sendToTrackers(
         maid,
         new SoloBeamPacket(
            maid.getId(), targetId, startTick, chargeTicks, fireTicks, fadeTicks, at.x, at.y, at.z, soundRange, soundFar
         )
      );
   }

   public static void sendSpinHit(ServerLevel level, Vec3 pos, float range) {
      if (range > 0.0F) {
         sendNear(level, pos, range, new SpinHitPacket(pos.x, pos.y, pos.z, range));
      }
   }

   public static void sendShake(ServerLevel level, Vec3 pos, double range, int ticks, float power) {
      sendShake(level, pos, range, ticks, power, 0.0F);
   }

   public static void sendShake(ServerLevel level, Vec3 pos, double range, int ticks, float power, float downBias) {
      sendShake(level, pos, range, ticks, power, downBias, 0.0F);
   }

   public static void sendShake(ServerLevel level, Vec3 pos, double range, int ticks, float power, float downBias, float rough) {
      sendShake(level, pos, range, ticks, power, downBias, rough, false);
   }

   public static void sendShake(ServerLevel level, Vec3 pos, double range, int ticks, float power, float downBias, float rough, boolean steady) {
      sendNear(level, pos, range, new ShakePacket(ticks, power, downBias, rough, steady));
   }

   public static void sendShake(ServerLevel level, Vec3 pos, double range, int ticks, float power, float downBias, float rough, float bounce) {
      sendNear(level, pos, range, new ShakePacket(ticks, power, downBias, rough, false, bounce));
   }

   public static void sendShakeSeen(ServerLevel level, Entity source, int ticks, float power, float downBias, float rough, float bounce) {
      sendNear(level, source.position(), 192.0, new ShakePacket(ticks, power, downBias, rough, false, bounce, source.getId()));
   }

   public static void sendShakeSeenKick(
      ServerLevel level, Entity source, int ticks, float power, float downBias, float rough, float bounce, float kx, float kz, float kp
   ) {
      sendNear(level, source.position(), 192.0, new ShakePacket(ticks, power, downBias, rough, false, bounce, source.getId(), kx, kz, kp));
   }

   public static void sendShockwave(ServerLevel level, Vec3 pos, double range, float radius, int ticks, Vec3 axis) {
      sendNear(
         level,
         pos,
         seenAtLeast(range),
         new ShockwavePacket(pos.x, pos.y, pos.z, radius, ticks, (float)axis.x, (float)axis.y, (float)axis.z)
      );
   }

   public static void sendScreenRipple(ServerLevel level, Entity source, Vec3 spot, int ticks) {
      sendNear(level, spot, 192.0, new ScreenRipplePacket(source.getId(), spot.x, spot.y, spot.z, ticks));
   }

   public static void sendShockwave(ServerLevel level, Vec3 pos, double range, float radius, int ticks, Vec3 axis, boolean wild) {
      sendNear(
         level,
         pos,
         seenAtLeast(range),
         new ShockwavePacket(pos.x, pos.y, pos.z, radius, ticks, (float)axis.x, (float)axis.y, (float)axis.z, wild)
      );
   }

   public static void sendShockwave(ServerLevel level, Vec3 pos, double range, float radius, int ticks, Vec3 axis, boolean wild, boolean warm, float flareScale) {
      sendNear(
         level,
         pos,
         seenAtLeast(range),
         new ShockwavePacket(
            pos.x, pos.y, pos.z, radius, ticks, (float)axis.x, (float)axis.y, (float)axis.z, wild, warm, flareScale
         )
      );
   }

   public static void sendShockwave(ServerLevel level, Vec3 pos, double range, float radius, int ticks, Vec3 axis, boolean wild, boolean warm) {
      sendNear(
         level,
         pos,
         seenAtLeast(range),
         new ShockwavePacket(
            pos.x, pos.y, pos.z, radius, ticks, (float)axis.x, (float)axis.y, (float)axis.z, wild, warm
         )
      );
   }

   public static void sendShockwaveBlue(ServerLevel level, Vec3 pos, double range, float startRadius, float endRadius, int ticks, Vec3 axis) {
      sendNear(
         level,
         pos,
         seenAtLeast(range),
         new ShockwavePacket(
            pos.x,
            pos.y,
            pos.z,
            endRadius,
            ticks,
            startRadius,
            40,
            500,
            (float)axis.x,
            (float)axis.y,
            (float)axis.z,
            0,
            -1,
            false,
            false,
            true
         )
      );
   }

   public static void sendShockwaveViolet(ServerLevel level, Vec3 pos, double range, float radius, int ticks, Vec3 axis) {
      sendNear(
         level,
         pos,
         seenAtLeast(range),
         new ShockwavePacket(
            pos.x,
            pos.y,
            pos.z,
            radius,
            ticks,
            radius * 0.5F,
            150,
            500,
            (float)axis.x,
            (float)axis.y,
            (float)axis.z,
            0,
            -1,
            false,
            false,
            true
         )
      );
   }

   public static void sendShockWarp(ServerLevel level, Vec3 pos, double range, float radius, float fastRadius, int fastMs, int slowMs, int kind) {
      sendShockWarp(level, pos, range, radius, fastRadius, fastMs, slowMs, kind, false);
   }

   public static void sendShockWarp(ServerLevel level, Vec3 pos, double range, float radius, float fastRadius, int fastMs, int slowMs, int kind, boolean flat) {
      sendShockWarp(level, pos, range, radius, fastRadius, fastMs, slowMs, kind, flat, false);
   }

   public static void sendShockWarp(
      ServerLevel level, Vec3 pos, double range, float radius, float fastRadius, int fastMs, int slowMs, int kind, boolean flat, boolean hollow
   ) {
      sendNear(
         level, pos, seenAtLeast(range), new ShockWarpPacket(pos.x, pos.y, pos.z, radius, fastRadius, fastMs, slowMs, kind, flat, hollow)
      );
   }

   public static void sendShockwave(ServerLevel level, Vec3 pos, double range, float radius, int ticks, float fastRadius, int fastMs, int slowMs, Vec3 axis) {
      sendNear(
         level,
         pos,
         seenAtLeast(range),
         new ShockwavePacket(
            pos.x,
            pos.y,
            pos.z,
            radius,
            ticks,
            fastRadius,
            fastMs,
            slowMs,
            (float)axis.x,
            (float)axis.y,
            (float)axis.z
         )
      );
   }

   public static void sendPodShockwave(ServerLevel level, Vec3 pos, double range, float radius, int ticks, Entity maid, int pod, Vec3 axis) {
      sendNear(
         level,
         pos,
         seenAtLeast(range),
         new ShockwavePacket(
            pos.x, pos.y, pos.z, radius, ticks, (float)axis.x, (float)axis.y, (float)axis.z, maid.getId(), pod
         )
      );
   }

   public static void sendBlackHole(ServerLevel level, Vec3 pos, double range, float size, float warp, float reach, int ticks, boolean execution) {
      sendNear(level, pos, range, new BlackHolePacket(pos.x, pos.y, pos.z, size, warp, reach, ticks, execution));
   }

   public static void sendHoleClear(ServerLevel level, Vec3 pos, double range, float radius) {
      sendNear(level, pos, range, new HoleClearPacket(pos.x, pos.y, pos.z, radius));
   }

   public static void sendRuinedPhantasmSpot(ServerLevel level, int id, double x, double y, double z, float yRot, float xRot, int millis, boolean alive) {
      if (level != null) {
         double range = alive ? 256.0 : 512.0;
         sendNear(level, new Vec3(x, y, z), range, new RuinedPhantasmSpotPacket(id, x, y, z, yRot, xRot, millis, alive));
      }
   }

   public static void sendMark(int entityId) {
      if (channel != null) {
         channel.sendToServer(new MarkPacket(entityId));
      }
   }
}

