package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.boss.BossRegistry;
import com.myx00x.maid.boss.ShenlinBoss;
import com.myx00x.maid.boss.ShenlinBossKeeper;
import com.myx00x.maid.network.FlashPacket;
import com.myx00x.maid.network.ModPackets;
import com.myx00x.maid.registry.ModItems;
import com.myx00x.maid.registry.ModSounds;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class ExecutionSequencer {
   private static final int STRIKE_DELAY_TICKS = 3;
   private static final int ANIM_TICKS = 20;
   private static final int FOUNTAIN_TICKS = 22;
   private static final int JET_LIFE_TICKS = 14;
   private static final int JETS_PER_TICK = 6;
   private static final double JET_GRAVITY = 0.058;
   private static final int SHELL_TICKS = 12;
   private static final int FADE_TICKS = 10;
   private static final int DASH_LEAD_TICKS = 4;
   private static final int DASH_TICKS = 3;
   private static final double DASH_PAST = 2.0;
   private static final int DASH_SETTLE_TICKS = 2;
   private static final int FLASH_TICKS = 7;
   private static final int FLASH_LEAD_TICKS = 2;
   private static final float SHOCKWAVE_RADIUS = 10.0F;
   private static final double SHOCKWAVE_RANGE = 64.0;
   private static final int SHOCKWAVE_GROW_MS = 100;
   private static final int SHOCKWAVE_HOLD_MS = 2750;
   private static final int RAY_TICKS = 60;
   private static final int RAY_TICKS_HOLE = 9;
   private static final double RAY_RANGE = 64.0;
   private static final int SHAKE_TICKS = 8;
   private static final float SHAKE_POWER = 2.0F;
   private static final double SHAKE_RANGE = 64.0;
   private static final double RAY_REACH = 512.0;
   private static final float RAY_WIDTH = 2.4F;
   private static final Vector3f BEAM_RED = new Vector3f(1.0F, 0.08F, 0.08F);
   private static final Vector3f SHELL_RED = new Vector3f(1.0F, 0.05F, 0.05F);
   private static final Vector3f DASH_BLACK = new Vector3f(0.06F, 0.02F, 0.05F);
   private static final double HOLE_CLEAR_RADIUS = 4.0;
   private static final Map<UUID, ExecutionSequencer.Sequence> ACTIVE = new ConcurrentHashMap<>();

   private ExecutionSequencer() {
   }

   public static boolean begin(EntityMaid maid, LivingEntity target, String reason, boolean erase, int swings) {
      if (!target.isRemoved() && !(target instanceof Player) && !(target instanceof EntityMaid) && maid.level() instanceof ServerLevel level) {
         ShenlinBoss boss = ShenlinBossKeeper.of(target);
         if (boss != null) {
            retireBoss(maid, target, boss, level, reason);
            return true;
         }

         if (ACTIVE.containsKey(target.getUUID())) {
            return false;
         }

         LivingEntity owner = maid.getOwner();
         DamageSource source = owner instanceof Player player ? target.damageSources().playerAttack(player) : target.damageSources().mobAttack(maid);
         Player killer = owner instanceof Player player ? player : null;
         ExecutionSequencer.Sequence seq = new ExecutionSequencer.Sequence(level, target, maid, source, killer, reason, erase);
         seq.lastPos = target.position();
         if (swings >= (Integer)Config.FINISH_SWINGS.get()) {
            seq.dash = true;
            seq.dashFrom = maid.position();
            Vec3 aim = target.position();
            seq.dashTo = behind(seq.dashFrom, aim);
            float[] face = faceToward(seq.dashFrom, aim, maid.getYRot(), maid.getXRot());
            seq.dashYaw = face[0];
            seq.dashPitch = face[1];
            seq.hole = (Boolean)Config.BLACK_HOLE_ENABLED.get() && ItemsUtil.hasBaubleItemInMaid(maid, (Item)ModItems.BLACK_HOLE_SCROLL.get());
            openFire(maid, target, level, seq.hole);
         }

         ACTIVE.put(target.getUUID(), seq);
         clearHolesNear(level, seq.lastPos);
         SoundBroadcast.play(level, seq.lastPos, (SoundEvent)ModSounds.EXECUTE.get(), 11);
         if (seq.hole) {
            try {
               executionHole(level, seq);
            } catch (Throwable t) {
               seq.hole = false;
               ShenlinMod.LOGGER.warn("[shenlin] execution hole failed: {}", t.toString());
            }
         }

         ShenlinMod.LOGGER
            .info(
               "[shenlin] execute {} by owner ({}) armed erase={} swings={}{}",
               new Object[]{target.getType().getDescriptionId(), reason, erase, swings, seq.dash ? " dash" : ""}
            );
         return true;
      } else {
         return false;
      }
   }

   private static void retireBoss(EntityMaid maid, LivingEntity target, ShenlinBoss boss, ServerLevel level, String reason) {
      Vec3 at = target.position();
      float width = target.getBbWidth();
      float height = target.getBbHeight();
      clearHolesNear(level, at);
      SoundBroadcast.play(level, at, (SoundEvent)ModSounds.EXECUTE.get(), 11);
      openFire(maid, target, level, false);
      shockwave(level, at, height);
      burst(level, at, width, height);
      shatter(level, at, width, height);
      ShenlinBossKeeper.dismiss(boss);
      ShenlinMod.LOGGER.info("[shenlin] boss retired by owner ({}) age={}", reason, boss.age());
   }

   private static void clearHolesNear(ServerLevel level, Vec3 pos) {
      int cleared = BlackHoleKeeper.clearNear(level, pos, 4.0);
      ModPackets.sendHoleClear(level, pos, 64.0, 4.0F);
      if (cleared > 0) {
         ShenlinMod.LOGGER.info("[shenlin] execute cleared holes={} at {}", cleared, String.format("%.2f/%.2f/%.2f", pos.x, pos.y, pos.z));
      }
   }

   private static void executionHole(ServerLevel level, ExecutionSequencer.Sequence seq) {
      EntityMaid maid = seq.maid.get();
      if (maid != null) {
         LivingEntity target = seq.target.get();
         float victimMax = target != null ? target.getMaxHealth() : 0.0F;
         BlackHoleKeeper.spawnExecutionHole(level, maid, seq.lastPos, victimMax);
      }
   }

   public static boolean isExecuted(LivingEntity target) {
      return target != null && ACTIVE.containsKey(target.getUUID());
   }

   public static void dropAll() {
      ACTIVE.clear();
   }

   public static void tick() {
      if (!ACTIVE.isEmpty()) {
         for (ExecutionSequencer.Sequence seq : ACTIVE.values()) {
            try {
               tickOne(seq);
            } catch (Throwable t) {
               ShenlinMod.LOGGER.warn("[shenlin] execute sequence aborted: {}", t.toString());
               remove(seq);
            }
         }
      }
   }

   private static void tickOne(ExecutionSequencer.Sequence seq) {
      ServerLevel level = seq.level.get();
      LivingEntity target = seq.target.get();
      if (level != null && target != null) {
         if (seq.phase < 2) {
            seq.lastPos = target.position();
         }

         updateFountain(seq, level);
         switch (seq.phase) {
            case 0:
               if (seq.dash && seq.dashTo != null) {
                  seq.timer++;
                  if (seq.timer == 2) {
                     EntityMaid flashMaid = seq.maid.get();
                     if (flashMaid != null) {
                        ModPackets.sendToTrackers(flashMaid, new FlashPacket(7));
                     }
                  }

                  if (seq.timer == 5) {
                     blink(seq, level);
                     if (!seq.hole) {
                        shockwave(level, seq.lastPos, seq.bbHeight);
                     }
                  }

                  if (seq.timer >= 7) {
                     strikeSafely(seq, target, level);
                  }
               } else if (++seq.timer >= 3) {
                  strikeSafely(seq, target, level);
               }
               break;
            case 1:
               if (!target.isRemoved() && !Executioner.reached(target, 0.0F)) {
                  Executioner.setHealthSilently(target, 0.0F);
               }

               int anim = seq.dash ? 2 : 20;
               if (target.isRemoved() || ++seq.timer >= anim) {
                  if (seq.erase) {
                     clearTargetSafely(seq, target, level);
                  } else {
                     ShenlinMod.LOGGER.info("[shenlin] execute {} settled, erase off", target.getType().getDescriptionId());
                     remove(seq);
                  }
               }
               break;
            case 2:
               shellParticle(level, seq.lastPos, seq.bbWidth, seq.bbHeight);
               if (++seq.timer >= 12) {
                  seq.ghosts.forEach(Vanisher.Ghost::sendRemove);
                  seq.ghosts = List.of();
                  seq.timer = 0;
                  seq.phase = 3;
               }
               break;
            default:
               fadeParticle(level, seq.lastPos, seq.bbWidth, seq.bbHeight);
               if (++seq.timer >= 10) {
                  remove(seq);
               }
         }
      } else {
         remove(seq);
      }
   }

   private static void remove(ExecutionSequencer.Sequence seq) {
      try {
         restoreMax(seq);
         seq.ghosts.forEach(Vanisher.Ghost::sendRemove);
         seq.ghosts = List.of();
      } finally {
         ACTIVE.remove(seq.targetId);
      }
   }

   private static void restoreMax(ExecutionSequencer.Sequence seq) {
      Float value = seq.originalMax;
      if (value != null) {
         seq.originalMax = null;
         LivingEntity target = seq.target.get();
         if (target != null && !target.isRemoved()) {
            AttributeInstance max = target.getAttribute(Attributes.MAX_HEALTH);
            if (max != null && max.getBaseValue() == 0.0) {
               max.setBaseValue(value.floatValue());
            }
         }
      }
   }

   private static void strikeSafely(ExecutionSequencer.Sequence seq, LivingEntity target, ServerLevel level) {
      try {
         strike(seq, target, level);
      } catch (Throwable t) {
         ShenlinMod.LOGGER.warn("[shenlin] execute strike failed: {}", t.toString());
         if (seq.phase == 0) {
            seq.phase = 1;
            seq.timer = 0;
         }
      }
   }

   private static void clearTargetSafely(ExecutionSequencer.Sequence seq, LivingEntity target, ServerLevel level) {
      BossRegistry.release(target, "被终结之斩杀");

      try {
         clearTarget(seq, target, level);
      } catch (Throwable t) {
         ShenlinMod.LOGGER.warn("[shenlin] execute clear failed: {}", t.toString());
         pullOut(target);
      }
   }

   private static void hardClearSafely(LivingEntity target) {
      BossRegistry.release(target, "被终结之斩杀");

      try {
         hardClear(target);
      } catch (Throwable t) {
         ShenlinMod.LOGGER.warn("[shenlin] execute hard clear failed: {}", t.toString());
         pullOut(target);
      }
   }

   private static void pullOut(LivingEntity target) {
      try {
         if (!target.isRemoved()) {
            Executioner.setHealthSilently(target, 0.0F);
            target.discard();
         }

         if (!target.isRemoved()) {
            Vanisher.vanish(target, true);
         }
      } catch (Throwable var2) {
      }
   }

   private static void strike(ExecutionSequencer.Sequence seq, LivingEntity target, ServerLevel level) {
      if (!Vanisher.inRoster(level, target)) {
         clearTargetSafely(seq, target, level);
      } else {
         seq.timer = 0;
         seq.phase = 1;
         seq.struck = true;
         ModPackets.sendShake(level, seq.lastPos, 64.0, 8, 2.0F);
         float before = Executioner.readHealthRaw(target);
         AttributeInstance max = target.getAttribute(Attributes.MAX_HEALTH);
         if (max != null) {
            seq.originalMax = (float)max.getBaseValue();
            max.setBaseValue(0.0);
         }

         Executioner.setHealthSilently(target, 0.0F);
         LootSpoofer.spoofKiller(target, seq.owner.get(), seq.source);
         LootSpoofer.forceLoot(target, level, seq.source, seq.owner.get());
         if (target.deathTime <= 0) {
            target.die(seq.source);
         }

         LootSpoofer.settleDeferred(target, seq.source);
         target.setPose(Pose.DYING);
         target.deathTime = 0;
         if (seq.dash) {
            burst(level, seq.lastPos, seq.bbWidth, seq.bbHeight);
            if (seq.erase) {
               shatter(level, seq.lastPos, seq.bbWidth, seq.bbHeight);
               hardClearSafely(target);
               remove(seq);
               return;
            }
         }

         seq.fountain = 22;
         ShenlinMod.LOGGER
            .info("[shenlin] execute {} hp {} -> -inf visible {}, animation starts", new Object[]{target.getType().getDescriptionId(), before, target.getHealth()});
      }
   }

   private static void clearTarget(ExecutionSequencer.Sequence seq, LivingEntity target, ServerLevel level) {
      seq.timer = 0;
      seq.phase = 2;
      seq.lastPos = target.position();
      SealField.seal(target);
      boolean onRosterBefore = Vanisher.inRoster(level, target);

      try {
         RosterErase.erase(target);
      } catch (Throwable t) {
         ShenlinMod.LOGGER.warn("[shenlin] execute roster erase failed: {}", t.toString());
      }

      boolean onRosterAfter = Vanisher.inRoster(level, target);
      ShenlinMod.LOGGER.info("[shenlin] erase check {}: before={} after={}", new Object[]{target.getType().getDescriptionId(), onRosterBefore, onRosterAfter});
      if (!seq.struck) {
         LootSpoofer.settleDeferred(target, seq.source);
         LootSpoofer.forceLoot(target, level, seq.source, seq.owner.get());
      }

      if (!target.isRemoved()) {
         Executioner.setHealthSilently(target, 0.0F);
      }

      target.discard();
      Entity vehicle = target.getVehicle();
      seq.ghosts = new ArrayList<>(Vanisher.vanishGhost(target, true));
      if (Vanisher.inRoster(level, target)) {
         seq.ghosts.forEach(Vanisher.Ghost::sendRemove);
         seq.ghosts = new ArrayList<>();
         Vanisher.vanish(target, true);
      }

      boolean clean = !Vanisher.inRoster(level, target);
      if (!clean) {
         ShenlinMod.LOGGER.warn("[shenlin] execute {} clear left it on the roster", target.getType().getDescriptionId());
      }

      if (vehicle != null && vehicle != target) {
         if (vehicle.isRemoved()) {
            Vanisher.vanish(vehicle, true);
         } else {
            seq.ghosts.addAll(Vanisher.vanishGhost(vehicle, true));
         }
      }

      SoundBroadcast.play(level, seq.lastPos, (SoundEvent)ModSounds.CLEAR.get());
      ShenlinMod.LOGGER.info("[shenlin] execute {} cleared, {} {}", new Object[]{target.getType().getDescriptionId(), seq.reason, clean ? "removed" : "unlinked"});
   }

   private static void updateFountain(ExecutionSequencer.Sequence seq, ServerLevel level) {
      if (seq.fountain > 0) {
         seq.fountain--;
         Vec3 pos = seq.lastPos;

         for (int j = 0; j < 6; j++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            double spread = 0.08 + level.random.nextDouble() * 0.12;
            seq.jets
               .add(
                  new double[]{
                     pos.x,
                     pos.y + 0.15,
                     pos.z,
                     Math.cos(angle) * spread,
                     0.14 + level.random.nextDouble() * 0.16,
                     Math.sin(angle) * spread,
                     14.0
                  }
               );
         }
      }

      if (!seq.jets.isEmpty()) {
         DustParticleOptions dust = new DustParticleOptions(SHELL_RED, 0.7F);
         Iterator<double[]> it = seq.jets.iterator();

         while (it.hasNext()) {
            double[] jet = it.next();
            jet[4] -= 0.058;
            jet[0] += jet[3];
            jet[1] += jet[4];
            jet[2] += jet[5];
            level.sendParticles(dust, jet[0], jet[1], jet[2], 2, 0.0, 0.0, 0.0, 0.0);
            if (--jet[6] <= 0.0 || jet[1] < seq.lastPos.y - 1.5) {
               it.remove();
            }
         }
      }
   }

   private static Vec3 behind(Vec3 from, Vec3 to) {
      Vec3 line = to.subtract(from);
      double distance = line.length();
      return distance < 0.4 ? null : to.add(line.scale(2.0 / distance));
   }

   private static float[] faceToward(Vec3 from, Vec3 to, float yawFallback, float pitchFallback) {
      double dx = to.x - from.x;
      double dy = to.y - from.y;
      double dz = to.z - from.z;
      double flat = Math.sqrt(dx * dx + dz * dz);
      return flat < 0.05 && Math.abs(dy) < 0.05
         ? new float[]{yawFallback, pitchFallback}
         : new float[]{(float)(Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F, (float)(-Mth.atan2(dy, flat) * (180.0 / Math.PI))};
   }

   private static void blink(ExecutionSequencer.Sequence seq, ServerLevel level) {
      EntityMaid maid = seq.maid.get();
      if (maid != null && !maid.isRemoved()) {
         Vec3 to = seq.dashTo;
         maid.getNavigation().stop();
         maid.setDeltaMovement(Vec3.ZERO);
         maid.moveTo(to.x, to.y, to.z, seq.dashYaw, seq.dashPitch);
         dashStreak(level, seq.dashFrom, to, maid.getBbHeight());
      }
   }

   private static void dashStreak(ServerLevel level, Vec3 from, Vec3 to, float height) {
      Vec3 line = to.subtract(from);
      int steps = (int)Mth.clamp(line.length() * 3.0, 4.0, 30.0);

      for (int i = 0; i <= steps; i++) {
         Vec3 at = from.add(line.scale((double)i / steps));
         double midY = at.y + height * 0.5;
         level.sendParticles(new DustParticleOptions(BEAM_RED, 1.0F), at.x, midY, at.z, 2, 0.1, 0.18, 0.1, 0.03);
         level.sendParticles(new DustParticleOptions(DASH_BLACK, 1.2F), at.x, midY, at.z, 1, 0.1, 0.18, 0.1, 0.03);
      }
   }

   private static void shockwave(ServerLevel level, Vec3 pos, float height) {
      ModPackets.sendShockWarp(level, pos.add(0.0, height * 0.5, 0.0), 64.0, 10.0F, 10.0F, 100, 2750, 0);
   }

   private static void shatter(ServerLevel level, Vec3 pos, float width, float height) {
      double midY = pos.y + height * 0.5;
      level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, midY, pos.z, 3, 0.5, 0.4, 0.5, 0.0);
      level.sendParticles(new DustParticleOptions(SHELL_RED, 1.0F), pos.x, midY, pos.z, 130, width * 0.7, height * 0.5, width * 0.7, 0.6);
      level.sendParticles(new DustParticleOptions(DASH_BLACK, 1.1F), pos.x, midY, pos.z, 60, width * 0.6, height * 0.4, width * 0.6, 0.5);
      level.sendParticles(ParticleTypes.SMOKE, pos.x, midY, pos.z, 26, 0.4, 0.35, 0.4, 0.18);
   }

   private static void hardClear(LivingEntity target) {
      String who = target.getType().getDescriptionId();
      ShenlinMod.LOGGER.info("[shenlin] hard clear {} begin", who);
      SealField.seal(target);

      try {
         RosterErase.erase(target);
      } catch (Throwable t) {
         ShenlinMod.LOGGER.warn("[shenlin] hard clear roster erase failed: {}", t.toString());
      }

      Executioner.setHealthSilently(target, 0.0F);
      target.discard();
      Vanisher.vanish(target, true);
      if (target.level() instanceof ServerLevel server && Vanisher.inRoster(server, target)) {
         Vanisher.vanish(target, true);
         ShenlinMod.LOGGER.warn("[shenlin] hard clear left {} on the roster", who);
      }

      ShenlinMod.LOGGER.info("[shenlin] hard clear {} done", who);
   }

   private static void burst(ServerLevel level, Vec3 pos, float width, float height) {
      double midY = pos.y + height * 0.5;
      level.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, midY, pos.z, 2, 0.2, 0.2, 0.2, 0.0);
      level.sendParticles(new DustParticleOptions(SHELL_RED, 0.8F), pos.x, midY, pos.z, 95, width * 0.7, height * 0.45, width * 0.7, 0.55);
      level.sendParticles(ParticleTypes.CRIT, pos.x, midY, pos.z, 24, 0.3, 0.4, 0.3, 0.25);
      level.sendParticles(ParticleTypes.SMOKE, pos.x, midY, pos.z, 12, 0.25, 0.25, 0.25, 0.05);
   }

   private static void openFire(EntityMaid maid, LivingEntity target, ServerLevel level, boolean hole) {
      Vec3 from = maid.getEyePosition();
      Vec3 hit = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
      Vec3 line = hit.subtract(from);
      double length = line.length();
      if (!(length < 0.1)) {
         Vec3 unit = line.scale(1.0 / length);
         Vec3 end = from.add(unit.scale(512.0));
         int ticks = hole ? 9 : 60;
         ModPackets.sendRay(level, maid.position(), 64.0, from, end, ticks, 2.4F, true, 100, SlashFx.starRadius(maid));
         level.sendParticles(new DustParticleOptions(BEAM_RED, 1.4F), hit.x, hit.y, hit.z, 14, 0.25, 0.25, 0.25, 0.1);
      }
   }

   private static void shellParticle(ServerLevel level, Vec3 pos, float bbWidth, float bbHeight) {
      double half = bbWidth / 2.0;
      DustParticleOptions dust = new DustParticleOptions(SHELL_RED, 1.2F);

      for (int i = 0; i < 24; i++) {
         double x;
         double y;
         double z;
         if (level.random.nextFloat() < 0.22F) {
            double a = level.random.nextDouble() * Math.PI * 2.0;
            double r = level.random.nextDouble() * half;
            x = pos.x + Math.cos(a) * r;
            z = pos.z + Math.sin(a) * r;
            y = pos.y + bbHeight;
         } else {
            double a = level.random.nextDouble() * Math.PI * 2.0;
            x = pos.x + Math.cos(a) * half;
            z = pos.z + Math.sin(a) * half;
            y = pos.y + level.random.nextDouble() * bbHeight;
         }

         level.sendParticles(dust, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
      }
   }

   private static void fadeParticle(ServerLevel level, Vec3 pos, float bbWidth, float bbHeight) {
      level.sendParticles(
         new DustParticleOptions(SHELL_RED, 1.0F),
         pos.x,
         pos.y + bbHeight * 0.5,
         pos.z,
         8,
         bbWidth * 0.45,
         bbHeight * 0.45,
         bbWidth * 0.45,
         0.03
      );
   }

   private static final class Sequence {
      final UUID targetId;
      final WeakReference<ServerLevel> level;
      final WeakReference<LivingEntity> target;
      final WeakReference<EntityMaid> maid;
      final WeakReference<Player> owner;
      final DamageSource source;
      final String reason;
      final boolean erase;
      boolean struck;
      int phase;
      int timer;
      Vec3 lastPos;
      float bbWidth;
      float bbHeight;
      Float originalMax;
      int fountain;
      boolean dash;
      boolean hole;
      Vec3 dashFrom;
      Vec3 dashTo;
      float dashYaw;
      float dashPitch;
      final List<double[]> jets = new ArrayList<>();
      List<Vanisher.Ghost> ghosts = List.of();

      Sequence(ServerLevel level, LivingEntity target, EntityMaid maid, DamageSource source, Player owner, String reason, boolean erase) {
         this.targetId = target.getUUID();
         this.level = new WeakReference<>(level);
         this.target = new WeakReference<>(target);
         this.maid = new WeakReference<>(maid);
         this.owner = new WeakReference<>(owner);
         this.source = source;
         this.reason = reason;
         this.erase = erase;
         this.bbWidth = target.getBbWidth();
         this.bbHeight = target.getBbHeight();
      }
   }
}

