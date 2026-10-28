package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.mob.RelicWardenEntity;
import com.myx00x.maid.network.ModPackets;
import com.myx00x.maid.registry.ModItems;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class HoverCannon {
   public static final int PODS = 3;
   private static final double CHASE_GAP = 11.0;
   private static final double IDLE_RADIUS_BASE = 0.95;
   private static final double SPIN_PER_TICK = 0.019;
   private static final double BOB = 0.22;
   private static final double RING_HEIGHT_RATIO = 0.68;
   private static final double RING_HEIGHT_MIN = 1.1;
   private static final double RING_HEIGHT_MAX = 2.2;
   private static final double LIFT_SPREAD = 0.22;
   private static final double CHASE_TAU_MS = 340.0;
   private static final long HOLD_TICKS = 12L;
   private static final long RETURN_TICKS = 8L;
   private static final int BEAM_TICKS = 8;
   private static final float BEAM_WIDTH = 1.5F;
   private static final double BEAM_RANGE = 64.0;
   private static final float BEAM_R = 0.22F;
   private static final float BEAM_G = 0.5F;
   private static final float BEAM_B = 1.0F;
   private static final float RAY_SHOCK_RADIUS = 1.7F;
   private static final int RAY_SHOCK_TICKS = 12;
   private static final double FLASH_RISE_TICKS = 3.0;
   private static final double FLASH_GAIN = 1.0;
   private static final int SLOW_TICKS = 80;
   private static final ResourceKey<DamageType> CANNON_DAMAGE = ResourceKey.create(
      Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("goddess_of_maids", "hover_cannon")
   );
   private static final long PING_IDLE_MS = 400L;
   private static final long PING_MIN_MS = 100L;
   public static final long FORM_IN_TICKS = 15L;
   public static final long FORM_HOLD_TICKS = 8L;
   public static final long FORM_OUT_TICKS = 10L;
   private static final double FORM_ORBIT = 2.0;
   private static final double FORM_ABOVE = 2.5;
   private static final float FORM_CHARGE_WIDTH = 0.075F;
   private static final float FORM_BEAM_WIDTH = 2.4F;
   private static final int FORM_BEAM_TICKS = 14;
   private static final float FORM_SHOCK_RADIUS = 3.0F;
   private static final float FORM_SHOCK_FAST_RADIUS = 2.0F;
   private static final int FORM_SHOCK_FAST_MS = 150;
   private static final int FORM_SHOCK_SLOW_MS = 3000;
   private static final int FORM_SHOCK_TICKS = 63;
   private static final double FORM_SHOCK_RANGE = 64.0;
   private static final int FORM_SHAKE_TICKS = 12;
   private static final float FORM_SHAKE_POWER = 1.6F;
   private static final float FORM_SHAKE_DOWN = 1.8F;
   private static final double ARMOR_PIERCE_CAP = 8.0;
   private static final double SPIN_RADIUS = 1.35;
   private static final double SPIN_HEIGHT = 1.05;
   private static final double SPIN_POD_HEIGHT = 1.5;
   private static final double SPIN_TURN_PER_TICK = Math.PI * 3.0 / 5.0;
   private static final double SPIN_BOB = 0.09;
   private static final int SPIN_DIG_STEPS = 4;
   private static final double SPIN_RANGE = 24.0;
   private static final float SPIN_BEAM_WIDTH = 0.42F;
   private static final float SPIN_SHOCK_RADIUS = 1.3F;
   private static final int SPIN_SHOCK_TICKS = 10;
   private static final int SPIN_SHOCK_MAX = 3;
   private static final double NETHERITE_SPEED = 9.0;
   private static final double VANILLA_TOOL_DIVISOR = 30.0;
   public static final double SOLO_ABOVE = 1.5;
   public static final double SOLO_RING_RADIUS = 1.5;
   private static final double SOLO_TURN_PER_TICK = 0.01308996938995747;
   private static final double SOLO_RING_LIFT = 0.15;
   private static final long SOLO_SWING_EVERY = 6L;
   private static final long SOLO_SHAKE_FROM = 12L;
   private static final float SOLO_SHAKE_CHARGE = 0.35F;
   private static final long SOLO_SHAKE_EVERY = 3L;
   private static final int SOLO_SHAKE_TICKS = 6;
   private static final float SOLO_SHAKE_POWER = 0.33333334F;
   private static final float SOLO_SHAKE_ROUGH = 1.0F;
   private static final long SOLO_HIT_EVERY = 3L;
   private static final float SOLO_HIT_RADIUS = 1.6F;
   private static final int SOLO_HIT_TICKS = 12;
   private static final long SOLO_FADE_WIDEN = 3L;
   private static final float SOLO_FADE_QUIET = 0.35F;
   private static final Map<UUID, HoverCannon.Report> REPORTED = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> PINGED = new ConcurrentHashMap<>();
   private static final Map<UUID, HoverCannon.Pick> PICKS = new ConcurrentHashMap<>();
   private static final Map<UUID, Vec3> CENTERS = new ConcurrentHashMap<>();
   private static final Map<UUID, int[]> SHOTS = new ConcurrentHashMap<>();
   private static final Map<UUID, HoverCannon.Formation> FORMS = new ConcurrentHashMap<>();
   private static final Map<UUID, Integer> FORM_RUNS = new ConcurrentHashMap<>();
   private static final Map<UUID, HoverCannon.Spin> SPINS = new ConcurrentHashMap<>();
   private static final Map<UUID, HoverCannon.Solo> SOLOS = new ConcurrentHashMap<>();
   private static final Map<HoverCannon.ChipKey, HoverCannon.Chip> CHIPS = new ConcurrentHashMap<>();

   private static double soloLockRange() {
      return Math.max(1.0, (Double)Config.HOVER_CANNON_SOLO_LOCK_RANGE.get());
   }

   private HoverCannon() {
   }

   public static void forgetAll() {
      REPORTED.clear();
      PINGED.clear();
      PICKS.clear();
      CENTERS.clear();
      SHOTS.clear();
      FORMS.clear();
      FORM_RUNS.clear();
      SPINS.clear();
      SOLOS.clear();
      CHIPS.clear();
   }

   public static void tick(EntityMaid maid) {
      if (!maid.level().isClientSide) {
         try {
            if (maid.isRemoved()) {
               stop(maid);
               return;
            }

            boolean armed = armed(maid);
            announce(maid, armed);
            if (!armed) {
               UUID id = maid.getUUID();
               CENTERS.remove(id);
               PICKS.remove(id);
               SHOTS.remove(id);
               FORM_RUNS.remove(id);
               FORMS.remove(id);
               endSpin(maid, true);
               endSolo(maid, true);
               return;
            }

            long now = maid.level().getGameTime();
            nudgeCenter(maid);
            HoverCannon.Solo running = SOLOS.get(maid.getUUID());
            if (running != null) {
               SHOTS.remove(maid.getUUID());
               stepSolo(maid, running, now);
            } else {
               HoverCannon.Formation form = FORMS.get(maid.getUUID());
               if (form != null) {
                  stepFormation(maid, form, now);
               } else {
                  int volleys = volleys();
                  int[] shots = SHOTS.computeIfAbsent(maid.getUUID(), key -> new int[3]);

                  for (int pod = 0; pod < 3; pod++) {
                     if (dueNow(pod, now, maid.getUUID()) && fire(maid, pod, now) && shots[pod] < volleys) {
                        shots[pod]++;
                     }
                  }

                  maybeForm(maid, now, shots, volleys);
               }
            }

            HoverCannon.Spin spin = SPINS.get(maid.getUUID());
            if (spin != null) {
               stepSpin(maid, spin, now);
            }
         } catch (Throwable t) {
            ShenlinMod.LOGGER.error("[shenlin] hover cannon tick failed", t);
         }
      }
   }

   private static boolean armed(EntityMaid maid) {
      return !Config.HOVER_CANNON_ENABLED.get() ? false : maid.getMaidBauble().containsItem((Item)ModItems.ENERGY_CORE.get());
   }

   private static void announce(EntityMaid maid, boolean armed) {
      LivingEntity target = armed ? targetOf(maid) : null;
      boolean soloOn = armed && SOLOS.containsKey(maid.getUUID());
      int state = !armed ? 0 : (soloOn ? 3 : (engaged(maid, target) ? 2 : 1));
      int targetId = state == 2 && target != null ? target.getId() : 0;
      UUID id = maid.getUUID();
      HoverCannon.Report now = new HoverCannon.Report(state, targetId);
      HoverCannon.Report was = REPORTED.get(id);
      long nowMs = System.currentTimeMillis();
      Long last = PINGED.get(id);
      long since = last == null ? Long.MAX_VALUE : nowMs - last;
      boolean urgent = state == 3 || was != null && was.state == 3;
      if (!now.equals(was)) {
         if (since < 100L && !urgent) {
            return;
         }
      } else if (state == 0 || since < 400L) {
         return;
      }

      REPORTED.put(id, now);
      PINGED.put(id, nowMs);
      ModPackets.sendOrbit(maid, state, targetId, idleMs());
   }

   private static int idleMs() {
      return (Integer)Config.HOVER_CANNON_IDLE_SECONDS.get() * 1000;
   }

   private static boolean engaged(EntityMaid maid, LivingEntity target) {
      if (target == null) {
         return false;
      }

      double range = (Double)Config.HOVER_CANNON_RANGE.get();
      return maid.distanceToSqr(target) <= range * range;
   }

   private static void nudgeCenter(EntityMaid maid) {
      LivingEntity target = targetOf(maid);
      if (target != null && !engaged(maid, target)) {
         target = null;
      }

      if (SOLOS.containsKey(maid.getUUID())) {
         target = null;
      }

      Vec3 goal = target != null ? ringCenter(target.position(), target.getBbHeight()) : ringCenter(maid.position(), maid.getBbHeight());
      CENTERS.compute(maid.getUUID(), (key, was) -> was == null ? goal : approach(was, goal, 50.0));
   }

   private static Vec3 centerOf(EntityMaid maid) {
      Vec3 center = CENTERS.get(maid.getUUID());
      if (center != null) {
         return center;
      }

      LivingEntity target = targetOf(maid);
      return target != null ? ringCenter(target.position(), target.getBbHeight()) : ringCenter(maid.position(), maid.getBbHeight());
   }

   private static boolean dueNow(int pod, long now, UUID id) {
      long interval = intervalTicks();
      long round = Math.floorDiv(now - phaseOf(pod, id, interval), interval);

      for (long r = round - 1L; r <= round + 1L; r++) {
         if (fireTickAt(pod, r, id, interval) == now) {
            return true;
         }
      }

      return false;
   }

   private static long phaseOf(int pod, UUID id, long interval) {
      long spread = Math.floorMod(id.hashCode() & 2147483647L, interval);
      return (spread + pod * (interval / 3L)) % interval;
   }

   private static long intervalTicks() {
      return Math.max(1L, ((Integer)Config.HOVER_CANNON_INTERVAL_TICKS.get()).intValue());
   }

   private static int volleys() {
      return Math.max(1, (Integer)Config.HOVER_CANNON_VOLLEYS.get());
   }

   private static long fireTickAt(int pod, long round, UUID id, long interval) {
      long base = phaseOf(pod, id, interval) + round * interval;
      long amp = jitterAmplitude(interval);
      return amp <= 0L ? base : base + Math.floorMod(noise(pod, round, id), amp * 2L + 1L) - amp;
   }

   private static long jitterAmplitude(long interval) {
      double ratio = (Double)Config.HOVER_CANNON_INTERVAL_JITTER.get();
      return !(ratio > 0.0) ? 0L : (long)Math.min(interval * ratio, interval);
   }

   private static long noise(int pod, long round, UUID id) {
      long h = id.getMostSignificantBits() * -7046029254386353131L;
      h ^= id.getLeastSignificantBits();
      h ^= pod * -4658895280553007687L;
      h ^= round * -7723592293110705685L;
      h ^= h >>> 29;
      h *= -4658895280553007687L;
      return h ^ h >>> 32;
   }

   private static boolean fire(EntityMaid maid, int pod, long now) {
      LivingEntity target = targetOf(maid);
      if (target == null) {
         return false;
      }

      double range = (Double)Config.HOVER_CANNON_RANGE.get();
      if (maid.distanceToSqr(target) > range * range) {
         return false;
      }

      if (maid.level() instanceof ServerLevel level) {
         Vec3 var13 = centerOf(maid).add(podOffset(pod, now, maid.getUUID(), true, chaseRadius(target.getBbWidth())));
         Vec3 to = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
         if (var13.distanceTo(to) < 0.1) {
            return false;
         }

         ModPackets.sendRay(level, var13, 64.0, var13, to, 8, 1.5F, false, 0, 0.0F, 0.22F, 0.5F, 1.0F, raySoundRange());
         float damage = damageAgainst(maid, target);
         DamageSource source = cannonSource(maid);
         boolean landed = damage > 0.0F && target.hurt(source, damage);
         if (isCannonDamage(source) && ExecutionCounter.recordIssued(maid, target, damage)) {
            Executioner.executeByOwner(maid, target, "cannon");
         }

         if (landed) {
            slowDown(target);
            ModPackets.sendPodShockwave(level, to, 64.0, 1.7F, 12, maid, pod, facingFrom(var13, to));
         }

         ShenlinMod.LOGGER
            .info(
               "[shenlin] hover cannon pod {} -> {} dmg {} dist {} {}",
               new Object[]{pod, target.getType().getDescriptionId(), damage, (int)Math.sqrt(maid.distanceToSqr(target)), landed ? "" : " blocked"}
            );
         return true;
      } else {
         return false;
      }
   }

   private static float raySoundRange() {
      return ((Double)Config.HOVER_CANNON_RAY_SOUND_RANGE.get()).floatValue();
   }

   private static DamageSource cannonSource(EntityMaid maid) {
      try {
         Holder<DamageType> type = maid.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(CANNON_DAMAGE);
         return new DamageSource(type, maid);
      } catch (Throwable t) {
         return maid.damageSources().mobAttack(maid);
      }
   }

   public static boolean isCannonDamage(DamageSource source) {
      return source != null && source.is(CANNON_DAMAGE);
   }

   private static void slowDown(LivingEntity target) {
      int level = (Integer)Config.HOVER_CANNON_SLOW_LEVEL.get();
      if (level > 0) {
         target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, level - 1, false, true));
      }
   }

   public static double attackPower(EntityMaid maid, LivingEntity target) {
      double fromTable = maid.getAttributeValue(Attributes.ATTACK_DAMAGE);
      ItemStack stack = maid.getMainHandItem();
      if (stack.isEmpty()) {
         return fromTable + EnchantmentHelper.getDamageBonus(stack, MobType.UNDEFINED);
      }

      double fromItem = itemAttack(maid.getAttributeBaseValue(Attributes.ATTACK_DAMAGE), stack);
      return Math.max(fromTable, fromItem) + EnchantmentHelper.getDamageBonus(stack, target == null ? MobType.UNDEFINED : target.getMobType());
   }

   private static double itemAttack(double base, ItemStack stack) {
      double add = 0.0;
      double multiplyBase = 0.0;
      double multiplyTotal = 1.0;

      for (Entry<Attribute, AttributeModifier> entry : stack.getAttributeModifiers(EquipmentSlot.MAINHAND).entries()) {
         if (entry.getKey() == Attributes.ATTACK_DAMAGE) {
            AttributeModifier mod = entry.getValue();
            switch (mod.getOperation()) {
               case ADDITION:
                  add += mod.getAmount();
                  break;
               case MULTIPLY_BASE:
                  multiplyBase += mod.getAmount();
                  break;
               case MULTIPLY_TOTAL:
                  multiplyTotal *= 1.0 + mod.getAmount();
            }
         }
      }

      return (base + add) * (1.0 + multiplyBase) * multiplyTotal;
   }

   private static float damageAgainst(EntityMaid maid, LivingEntity target) {
      double healthRatio = (Double)Config.HOVER_CANNON_HEALTH_RATIO.get();
      double attackRatio = (Double)Config.HOVER_CANNON_ATTACK_RATIO.get();
      return (float)(currentHealth(target) * healthRatio + attackPower(maid, target) * attackRatio);
   }

   private static float currentHealth(LivingEntity target) {
      float shown = target.getHealth();
      float raw = Executioner.syncedHealth(target);
      if (shown <= 0.0F) {
         return raw;
      } else {
         return raw <= 0.0F ? shown : Math.max(shown, raw);
      }
   }

   private static LivingEntity targetOf(EntityMaid maid) {
      UUID id = maid.getUUID();
      long now = maid.level().getGameTime();
      HoverCannon.Pick pick = PICKS.get(id);
      if (pick != null && pick.tick() == now) {
         return pick.target();
      }

      LivingEntity prev = pick == null ? null : pick.target();
      LivingEntity found = pickTarget(maid, prev);
      PICKS.put(id, new HoverCannon.Pick(now, found));
      return found;
   }

   private static LivingEntity pickTarget(EntityMaid maid, LivingEntity prev) {
      double range = (Double)Config.HOVER_CANNON_RANGE.get();
      double r2 = range * range;
      LivingEntity ordered = HateBoard.declaredTarget(maid, range);
      if (ordered != null) {
         return ordered;
      }

      LivingEntity target = null;
      Brain<?> brain = maid.getBrain();
      if (brain != null) {
         target = (LivingEntity)brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
      }

      if (target == null) {
         target = maid.getTarget();
      }

      if (inReach(maid, target, r2)) {
         return target;
      } else {
         return inReach(maid, prev, r2) ? prev : nearestFoe(maid, range);
      }
   }

   private static boolean inReach(EntityMaid maid, LivingEntity target, double r2) {
      return target != null
         && target != maid
         && !(target instanceof Player)
         && !(target instanceof EntityMaid)
         && target.isAlive()
         && !target.isRemoved()
         && target.level() == maid.level()
         && !RetreatManager.isAvoided(maid.getUUID(), target.getUUID(), maid.level().getGameTime())
         && maid.distanceToSqr(target) <= r2;
   }

   private static LivingEntity nearestFoe(EntityMaid maid, double range) {
      if (maid.level() instanceof ServerLevel level) {
         UUID var16 = maid.getUUID();
         long now = maid.level().getGameTime();
         double r2 = range * range;
         LivingEntity best = null;
         double bestD = Double.MAX_VALUE;

         for (Entity entity : level.getEntities(
            maid,
            maid.getBoundingBox().inflate(range),
            e -> e instanceof LivingEntity && e instanceof Enemy && e.isAlive() && !e.isRemoved() && !RetreatManager.isAvoided(var16, e.getUUID(), now)
         )) {
            double d = maid.distanceToSqr(entity);
            if (!(d > r2) && d < bestD) {
               bestD = d;
               best = (LivingEntity)entity;
            }
         }

         return best;
      } else {
         return null;
      }
   }

   public static Vec3 podOffset(int index, double spin, UUID id, boolean firing, double radius) {
      if (!firing) {
         return orbit(index, spin, id, radius);
      } else {
         long interval = intervalTicks();
         long hold = holdTicks(interval);
         long back = returnTicks(interval);
         long last = lastFire(index, (long)Math.floor(spin), id, interval);
         double since = spin - last;
         if (since < hold) {
            return orbit(index, last, id, radius);
         } else if (since < hold + back) {
            double t = (since - hold) / back;
            return orbit(index, last, id, radius).lerp(orbit(index, spin, id, radius), t);
         } else {
            return orbit(index, spin, id, radius);
         }
      }
   }

   public static double firePulse(int index, double spin, UUID id, boolean firing) {
      if (!firing) {
         return 1.0;
      } else {
         long interval = intervalTicks();
         long hold = holdTicks(interval);
         double since = spin - lastFire(index, (long)Math.floor(spin), id, interval);
         if (!(since < 0.0) && !(since >= hold)) {
            double rise = Math.min(1.0, since / 3.0);
            return 1.0 + 1.0 * rise * (1.0 - since / hold);
         } else {
            return 1.0;
         }
      }
   }

   private static long holdTicks(long interval) {
      return Math.min(12L, Math.max(4L, interval / 3L));
   }

   private static long returnTicks(long interval) {
      return Math.min(8L, Math.max(2L, interval / 3L));
   }

   private static long lastFire(int index, long now, UUID id, long interval) {
      long round = Math.floorDiv(now - phaseOf(index, id, interval), interval);
      long best = Long.MIN_VALUE;

      for (long r = round - 1L; r <= round + 1L; r++) {
         long t = fireTickAt(index, r, id, interval);
         if (t <= now && t > best) {
            best = t;
         }
      }

      return best == Long.MIN_VALUE ? now : best;
   }

   private static double podSeed(int index, int slot, UUID id) {
      return (noise(index, 1592590336L + slot, id) >>> 11) * 1.110223E-16F;
   }

   private static Vec3 orbit(int index, double spin, UUID id, double radius) {
      double turn = podSeed(index, 0, id);
      double reach = podSeed(index, 1, id);
      double lift = podSeed(index, 2, id);
      double drift = podSeed(index, 3, id);
      double speed = 0.019 * (0.85 + 0.35 * turn);
      double base = spin * speed + (id.hashCode() & 0xFF) * 0.017;
      double angle = base + index * (Math.PI * 2.0 / 3.0) + turn * 1.3;
      double breath = 1.0 + 0.07 * Math.sin(spin * (0.019 + 0.026 * drift) + drift * 6.2831853);
      double r = radius * (0.94 + 0.12 * reach) * breath;
      double y = (lift * 2.0 - 1.0) * 0.22 + Math.sin(spin * (0.045 + 0.04 * turn) + index * 2.1 + drift * 6.2831853) * 0.22 * (0.65 + 0.7 * lift);
      return new Vec3(Math.cos(angle) * r, y, Math.sin(angle) * r);
   }

   public static Vec3 ringCenter(Vec3 hostPos, double hostHeight) {
      return hostPos.add(0.0, ringHeight(hostHeight), 0.0);
   }

   public static double ringHeight(double hostHeight) {
      return Math.max(1.1, Math.min(hostHeight * 0.68, 2.2));
   }

   public static double chaseRadius(double hostWidth) {
      return hostWidth * 0.5 + 11.0;
   }

   public static double idleRadius(double hostWidth) {
      return Math.max(0.95, hostWidth * 0.85);
   }

   public static Vec3 approach(Vec3 from, Vec3 to, double ms) {
      if (ms <= 0.0) {
         return from;
      }

      double k = 1.0 - Math.exp(-ms / 340.0);
      return new Vec3(
         from.x + (to.x - from.x) * k,
         from.y + (to.y - from.y) * k,
         from.z + (to.z - from.z) * k
      );
   }

   private static void maybeForm(EntityMaid maid, long now, int[] shots, int volleys) {
      UUID id = maid.getUUID();
      if (!SPINS.containsKey(id) && !SOLOS.containsKey(id)) {
         for (int done : shots) {
            if (done < volleys) {
               return;
            }
         }

         LivingEntity target = targetOf(maid);
         if (target != null) {
            if (maid.level() instanceof ServerLevel level) {
               Vec3 var13 = overhead(target);
               Vec3 var14 = flat(target.position().subtract(maid.position()));
               FORMS.put(id, new HoverCannon.Formation(now, var13, var14, target.getId()));
               SHOTS.remove(id);
               ModPackets.sendCannonForm(level, maid, target.getId(), var13, var14, now, chargeTicks(), totalTicks(), formSoundRange(), formSoundFar());
               ShenlinMod.LOGGER
                  .info(
                     "[shenlin] hover cannon formation start -> {} overhead {} {} {}",
                     new Object[]{target.getType().getDescriptionId(), (float)var13.x, (float)var13.y, (float)var13.f_82481_}
                  );
            }
         }
      }
   }

   private static void stepFormation(EntityMaid maid, HoverCannon.Formation form, long now) {
      long since = now - form.startTick;
      if (!form.soloChecked && since >= settleTicks()) {
         form.soloChecked = true;
         if (soloEarly(maid, now)) {
            return;
         }
      }

      if (since >= totalTicks()) {
         FORMS.remove(maid.getUUID());
         afterForm(maid, now);
      } else {
         if (maid.level().getEntity(form.targetId) instanceof LivingEntity living && living.isAlive()) {
            form.anchor = overhead(living);
         }

         if (!form.fired && since >= 15L + chargeTicks()) {
            form.fired = true;
            strike(maid, form, now);
            FORM_RUNS.merge(maid.getUUID(), 1, Integer::sum);
         }
      }
   }

   private static Vec3 overhead(LivingEntity target) {
      return target.position().add(0.0, target.getBbHeight() + 2.5, 0.0);
   }

   public static double formAbove() {
      return 2.5;
   }

   private static void afterForm(EntityMaid maid, long now) {
      UUID id = maid.getUUID();
      Integer done = FORM_RUNS.get(id);
      int need = Math.max(1, (Integer)Config.HOVER_CANNON_SPIN_REQUIRED.get());
      if (done != null && done >= need) {
         if (maid.level() instanceof ServerLevel level) {
            FORM_RUNS.remove(id);
            HoverCannon.Round round = decide(maid, level);
            if (round.lock() != null) {
               beginSolo(maid, round.lock(), now, done, round.marks());
            } else if (round.spin()) {
               SPINS.put(id, new HoverCannon.Spin(now));
               ModPackets.sendCannonSpin(maid, now, spinTicks(), spinRayRange(), spinRayFar());
               ShenlinMod.LOGGER
                  .info("[shenlin] spin laser start after {} forms, {} ticks (marks {}, foes {})", new Object[]{done, spinTicks(), round.marks(), round.foes()});
            }
         }
      }
   }

   private static boolean soloEarly(EntityMaid maid, long now) {
      UUID id = maid.getUUID();
      Integer done = FORM_RUNS.get(id);
      int need = Math.max(1, (Integer)Config.HOVER_CANNON_SPIN_REQUIRED.get());
      if (done != null && done >= need) {
         if (maid.level() instanceof ServerLevel level) {
            HoverCannon.Round round = decide(maid, level);
            if (round.lock() == null) {
               return false;
            }

            FORM_RUNS.remove(id);
            FORMS.remove(id);
            beginSolo(maid, round.lock(), now, done, round.marks());
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static void beginSolo(EntityMaid maid, LivingEntity only, long now, int done, int marks) {
      SOLOS.put(maid.getUUID(), new HoverCannon.Solo(now, only.getId()));
      ModPackets.sendSoloBeam(
         maid,
         only.getId(),
         now,
         (int)soloChargeTicks(),
         (int)soloFireTicks(),
         (int)soloFadeTicks(),
         only.position(),
         soloBeamSoundRange(),
         soloBeamSoundFar()
      );
      announce(maid, true);
      ShenlinMod.LOGGER
         .info(
            "[shenlin] solo beam start after {} forms, charge {} fire {}, target {} (marks {})",
            new Object[]{done, soloChargeTicks(), soloFireTicks(), only.getType().getDescriptionId(), marks}
         );
   }

   private static void strike(EntityMaid maid, HoverCannon.Formation form, long now) {
      if (maid.level() instanceof ServerLevel level) {
         Vec3 var9 = form.anchor;
         LivingEntity target = entityById(level, form.targetId);
         Vec3 at = target == null ? var9.subtract(0.0, 2.5, 0.0) : target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
         ModPackets.sendRay(level, var9, 64.0, var9, at, 14, 5.04F, false, 0, 0.0F, 0.1F, 0.26F, 0.62F);
         ModPackets.sendRay(level, var9, 64.0, var9, at, 14, 2.4F, false, 0, 0.0F, 0.22F, 0.5F, 1.0F);
         ModPackets.sendShake(level, at, 64.0, 12, 1.6F, 1.8F);
         boolean scroll = (Boolean)Config.BLACK_HOLE_ENABLED.get() && ItemsUtil.hasBaubleItemInMaid(maid, (Item)ModItems.BLACK_HOLE_SCROLL.get());
         if (scroll) {
            BlackHoleKeeper.spawnSmallHole(level, maid, at);
         } else {
            ModPackets.sendShockwave(level, at, 64.0, 3.0F, 63, 2.0F, 150, 3000, facingFrom(var9, at));
         }

         if (target != null) {
            landBolt(maid, target, at);
         }

         ShenlinMod.LOGGER
            .info(
               "[shenlin] hover cannon beam strike at {} {} {} target {}",
               new Object[]{(float)at.x, (float)at.y, (float)at.z, target == null ? "gone" : target.getType().getDescriptionId()}
            );
      }
   }

   private static void landBolt(EntityMaid maid, LivingEntity target, Vec3 at) {
      double power = attackPower(maid, target);
      double want = currentHealth(target) * (Double)Config.HOVER_CANNON_SHOT_HEALTH_RATIO.get() + power * (Double)Config.HOVER_CANNON_SHOT_ATTACK_RATIO.get();
      float damage = (float)pierceArmor(target, want);
      DamageSource source = cannonSource(maid);
      boolean landed = damage > 0.0F && target.hurt(source, damage);
      if (isCannonDamage(source) && ExecutionCounter.recordIssued(maid, target, damage)) {
         Executioner.executeByOwner(maid, target, "solo");
      }

      if (landed) {
         shaveMax(target);
      }

      ShenlinMod.LOGGER
         .info(
            "[shenlin] hover cannon joint strike -> {} want {} dmg {} pow {} attr {} max {} at {} {} {} {}",
            new Object[]{
               target.getType().getDescriptionId(),
               (float)want,
               damage,
               (float)power,
               (float)maid.getAttributeValue(Attributes.ATTACK_DAMAGE),
               target.getMaxHealth(),
               (float)at.x,
               (float)at.y,
               (float)at.z,
               landed ? "" : " blocked"
            }
         );
   }

   private static void shaveMax(LivingEntity target) {
      double ratio = (Double)Config.HOVER_CANNON_JOINT_MAX_RATIO.get();
      if (ratio > 0.0) {
         AttributeInstance attr = target.getAttribute(Attributes.MAX_HEALTH);
         if (attr != null) {
            double before = attr.getBaseValue();
            if (!(before <= 0.0)) {
               double after = Math.max(1.0, before * (1.0 - ratio));
               if (!(after >= before)) {
                  attr.setBaseValue(after);
                  if (Executioner.readHealthRaw(target) > target.getMaxHealth()) {
                     Executioner.setHealthSilently(target, target.getMaxHealth());
                  }

                  ShenlinMod.LOGGER
                     .info(
                        "[shenlin] hover cannon joint shave -> {} max {} to {} now {}",
                        new Object[]{target.getType().getDescriptionId(), (float)before, (float)after, target.getMaxHealth()}
                     );
               }
            }
         }
      }
   }

   private static Vec3 facingHome(EntityMaid maid, Vec3 at) {
      return flat(maid.position().subtract(at));
   }

   private static Vec3 facingFrom(Vec3 source, Vec3 at) {
      Vec3 v = source.subtract(at);
      return v.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 1.0, 0.0) : v.normalize();
   }

   private static double pierceArmor(LivingEntity target, double want) {
      double ignore = (Double)Config.HOVER_CANNON_JOINT_ARMOR_IGNORE.get();
      if (!(ignore <= 0.0) && !(want <= 0.0)) {
         double armor = target.getArmorValue();
         double tough = target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
         double reduction = Math.min(20.0, Math.max(armor / 5.0, armor - want / (2.0 + tough / 4.0))) / 25.0;
         if (reduction <= 0.0) {
            return want;
         }

         double pierced = want * (1.0 - reduction * (1.0 - ignore)) / (1.0 - reduction);
         return Math.min(pierced, want * 8.0);
      } else {
         return want;
      }
   }

   public static Vec3 spinPodPos(int index, double spin) {
      double angle = spin * (Math.PI * 3.0 / 5.0) + index * (Math.PI * 2.0 / 3.0);
      double x = Math.cos(angle) * 1.35;
      double z = Math.sin(angle) * 1.35;
      double y = 1.05 + Math.sin(spin * 0.06 + index * 2.1) * 0.09;
      return new Vec3(x, y, z);
   }

   public static int spinTicks() {
      return Math.max(1, (Integer)Config.HOVER_CANNON_SPIN_TICKS.get());
   }

   private static float spinRayRange() {
      return ((Double)Config.HOVER_CANNON_SPIN_RAY_SOUND_RANGE.get()).floatValue();
   }

   private static float spinRayFar() {
      return ((Double)Config.HOVER_CANNON_SPIN_RAY_SOUND_FAR.get()).floatValue();
   }

   public static double spinPodHeight() {
      return 1.5;
   }

   public static float spinBeamWidth() {
      return 0.42F;
   }

   public static double spinRange() {
      return 24.0;
   }

   private static void stepSpin(EntityMaid maid, HoverCannon.Spin spin, long now) {
      if (now - spin.startTick >= spinTicks()) {
         endSpin(maid, false);
      } else if (maid.level() instanceof ServerLevel level) {
         Vec3 var23 = maid.position();
         float attackPart = (float)(attackPower(maid, null) * (Double)Config.HOVER_CANNON_SPIN_ATTACK_RATIO.get());
         float healthRatio = ((Double)Config.HOVER_CANNON_SPIN_HEALTH_RATIO.get()).floatValue();
         boolean dig = (Boolean)Config.HOVER_CANNON_SPIN_BLOCK_BREAK.get();
         double sweep = Math.PI * 3.0 / 5.0;
         AABB ringBox = new AABB(var23, var23).inflate(24.419999986886978);
         List caught = level.getEntitiesOfClass(LivingEntity.class, ringBox);
         Vec3 heard = nearestEdgeOf(maid, var23, caught);
         if (heard != null) {
            playSpinHit(level, heard);
         }

         for (int index = 0; index < 3; index++) {
            Vec3 from = var23.add(spinPodPos(index, now));
            double start = spinAngle(index, now);
            sweepSear(level, maid, var23, from, start, sweep, attackPart, healthRatio, caught);
            if (dig) {
               for (int step = 0; step < 4; step++) {
                  double at = start + sweep * (step + 0.5) / 4.0;
                  Vec3 end = var23.add(Math.cos(at) * 24.0, 0.0, Math.sin(at) * 24.0);
                  BlockHitResult hit = level.clip(new ClipContext(from, end, Block.COLLIDER, Fluid.NONE, maid));
                  if (hit.getType() == Type.BLOCK) {
                     chip(level, maid, spin, hit.getBlockPos());
                  }
               }
            }
         }
      }
   }

   private static double spinAngle(int index, double spin) {
      return spin * (Math.PI * 3.0 / 5.0) + index * (Math.PI * 2.0 / 3.0);
   }

   private static void sweepSear(
      ServerLevel level, EntityMaid maid, Vec3 anchor, Vec3 from, double start, double sweep, float attackPart, float healthRatio, List<LivingEntity> caught
   ) {
      int rings = 0;

      for (LivingEntity target : caught) {
         if (fightable(maid, target)) {
            Vec3 mid = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
            Vec3 flat = mid.subtract(anchor);
            double dist = Math.sqrt(flat.x * flat.x + flat.z * flat.z);
            if (!(dist > 24.0 + target.getBbWidth() * 0.5) && !(wrap(Math.atan2(flat.z, flat.x) - start) > sweep)) {
               Vec3 end = (Vec3)target.getBoundingBox().inflate(0.42F).clip(from, mid).orElse(null);
               if (end != null) {
                  BlockHitResult hit = level.clip(new ClipContext(from, end, Block.COLLIDER, Fluid.NONE, maid));
                  if (hit.getType() == Type.MISS || !(hit.getLocation().distanceToSqr(from) < from.distanceToSqr(end) - 1.0E-4)) {
                     float damage = attackPart + currentHealth(target) * healthRatio;
                     if (damage > 0.0F) {
                        sear(maid, target, damage);
                        Vec3 edge = nearestEdge(maid, target, mid);
                        if (rings < 3) {
                           rings++;
                           ModPackets.sendShockwave(level, edge, 24.419999986886978, 1.3F, 10, facingHome(maid, edge));
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void sear(EntityMaid maid, LivingEntity target, float damage) {
      searOnce(maid, target, damage);
   }

   private static void searOnce(EntityMaid maid, LivingEntity target, float damage) {
      DamageSource source = BlackHoleKeeper.holeSource(maid);
      boolean hit = false;
      if (target instanceof RelicWardenEntity warden) {
         warden.ownDamage(damage);
         hit = true;
      } else {
         try {
            hit = target.hurt(source, damage);
         } catch (Throwable t) {
            ShenlinMod.LOGGER.warn("[shenlin] spin laser hit failed: {}", t.toString());
         }
      }

      if (BlackHoleKeeper.isHoleDamage(source) && ExecutionCounter.recordIssued(maid, target, damage)) {
         Executioner.executeByOwner(maid, target, "spin");
      }

      if (!hit) {
         if (SelfManagedHealth.is(target)) {
            if (!SelfManagedHealth.redirect(target, damage, maid)) {
               ShenlinMod.LOGGER.info("[shenlin] sear 放弃这一口：目标是自管血量的 boss，{} 打不进去", target.getType().getDescriptionId());
            }
         } else {
            float before = Executioner.readHealthRaw(target);
            if (!(before <= 0.0F)) {
               float after = Math.max(0.0F, before - damage);
               Executioner.setHealthSilently(target, after);
               if (Executioner.reached(target, 0.0F) && target.deathTime <= 0) {
                  try {
                     target.die(BlackHoleKeeper.holeSource(maid));
                  } catch (Throwable t) {
                     ShenlinMod.LOGGER.warn("[shenlin] spin laser finish failed: {}", t.toString());
                  }
               }
            }
         }
      }
   }

   private static Vec3 nearestEdge(EntityMaid maid, LivingEntity target, Vec3 mid) {
      Vec3 eye = maid.position().add(0.0, maid.getBbHeight() * 0.5, 0.0);
      return target.getBoundingBox().clip(eye, mid).orElse(mid);
   }

   private static Vec3 nearestEdgeFrom(Vec3 from, LivingEntity target, Vec3 mid) {
      return target.getBoundingBox().clip(from, mid).orElse(mid);
   }

   private static Vec3 nearestEdgeOf(EntityMaid maid, Vec3 anchor, List<LivingEntity> caught) {
      Vec3 best = null;
      double bestSqr = Double.MAX_VALUE;

      for (LivingEntity target : caught) {
         if (fightable(maid, target)) {
            Vec3 mid = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
            Vec3 flat = mid.subtract(anchor);
            double dist = Math.sqrt(flat.x * flat.x + flat.z * flat.z);
            if (!(dist > 24.0 + target.getBbWidth() * 0.5)) {
               double d = maid.distanceToSqr(target);
               if (d < bestSqr) {
                  bestSqr = d;
                  best = nearestEdge(maid, target, mid);
               }
            }
         }
      }

      return best;
   }

   private static void playSpinHit(ServerLevel level, Vec3 at) {
      float range = ((Double)Config.HOVER_CANNON_SPIN_HIT_SOUND_RANGE.get()).floatValue();
      if (!(range <= 0.0F)) {
         ModPackets.sendSpinHit(level, at, range);
      }
   }

   private static double wrap(double rad) {
      double t = rad % (Math.PI * 2);
      return t < 0.0 ? t + (Math.PI * 2) : t;
   }

   private static boolean burnable(EntityMaid maid, LivingEntity target) {
      return target != maid && !(target instanceof Player) && !(target instanceof EntityMaid) && target.isAlive() && !target.isRemoved();
   }

   private static void chip(ServerLevel level, EntityMaid maid, HoverCannon.Spin spin, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      if (!state.isAir()) {
         float hardness = state.getDestroySpeed(level, pos);
         if (!(hardness < 0.0F)) {
            HoverCannon.ChipKey key = new HoverCannon.ChipKey(level.dimension(), pos);
            spin.touched.add(key);
            if (hardness == 0.0F) {
               CHIPS.remove(key);
               level.destroyBlock(pos, true, maid);
            } else {
               HoverCannon.Chip chip = CHIPS.computeIfAbsent(key, k -> new HoverCannon.Chip());
               chip.progress += 9.0 / hardness / 30.0;
               if (chip.progress < 1.0) {
                  level.destroyBlockProgress(maid.getId(), pos, (int)(chip.progress * 10.0));
               } else {
                  CHIPS.remove(key);
                  level.destroyBlock(pos, true, maid);
               }
            }
         }
      }
   }

   private static void endSpin(EntityMaid maid, boolean tellClient) {
      HoverCannon.Spin spin = SPINS.remove(maid.getUUID());
      if (spin != null) {
         MinecraftServer server = maid.level().getServer();

         for (HoverCannon.ChipKey key : spin.touched) {
            ServerLevel in = server == null ? null : server.getLevel(key.dim());
            if (in != null) {
               in.destroyBlockProgress(maid.getId(), key.pos(), -1);
            }

            CHIPS.remove(key);
         }

         if (tellClient && !maid.level().isClientSide) {
            ModPackets.sendCannonSpin(maid, 0L, 0, 0.0F, 0.0F);
         }
      }
   }

   private static List<LivingEntity> foesWithin(EntityMaid maid, ServerLevel level, double range) {
      AABB box = new AABB(maid.position(), maid.position()).inflate(range);
      List<LivingEntity> found = level.getEntitiesOfClass(LivingEntity.class, box);
      List<LivingEntity> foes = new ArrayList<>();
      UUID id = maid.getUUID();
      long now = maid.level().getGameTime();
      double rangeSqr = range * range;

      for (LivingEntity target : found) {
         if (fightable(maid, target) && !RetreatManager.isAvoided(id, target.getUUID(), now) && maid.distanceToSqr(target) <= rangeSqr) {
            foes.add(target);
         }
      }

      return foes;
   }

   private static HoverCannon.Round decide(EntityMaid maid, ServerLevel level) {
      List<LivingEntity> marks = markedFoes(maid, soloLockRange());
      List<LivingEntity> near = foesWithin(maid, level, 24.0);
      if (!marks.isEmpty()) {
         return marks.size() == 1
            ? new HoverCannon.Round(marks.get(0), false, marks.size(), near.size())
            : new HoverCannon.Round(null, true, marks.size(), near.size());
      } else if (near.size() >= 2) {
         return new HoverCannon.Round(null, true, 0, near.size());
      } else {
         return near.size() == 1
            ? new HoverCannon.Round(near.get(0), false, 0, 1)
            : new HoverCannon.Round(nearestFoe(maid, foesWithin(maid, level, soloLockRange())), false, 0, 0);
      }
   }

   private static List<LivingEntity> markedFoes(EntityMaid maid, double range) {
      List<LivingEntity> out = new ArrayList<>();

      for (LivingEntity one : HateBoard.marksOf(maid, range)) {
         if (burnable(maid, one)) {
            out.add(one);
         }
      }

      return out;
   }

   private static LivingEntity nearestFoe(EntityMaid maid, List<LivingEntity> list) {
      if (list.isEmpty()) {
         return null;
      }

      LivingEntity hers = maid.getTarget();
      if (hers == null) {
         Brain<?> brain = maid.getBrain();
         if (brain != null) {
            hers = (LivingEntity)brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
         }
      }

      if (hers != null && list.contains(hers)) {
         return hers;
      }

      LivingEntity best = null;
      double bestDist = Double.MAX_VALUE;

      for (LivingEntity one : list) {
         double dist = maid.distanceToSqr(one);
         if (dist < bestDist) {
            bestDist = dist;
            best = one;
         }
      }

      return best;
   }

   private static boolean fightable(EntityMaid maid, LivingEntity target) {
      return burnable(maid, target) && (hostile(maid, target) || HateBoard.markedBy(maid, target));
   }

   private static boolean hostile(EntityMaid maid, LivingEntity target) {
      if (target instanceof Enemy) {
         return true;
      }

      Brain<?> brain = maid.getBrain();
      return brain != null && brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) == target || maid.getTarget() == target;
   }

   private static long soloChargeTicks() {
      return Math.max(1L, ((Integer)Config.HOVER_CANNON_SOLO_CHARGE_TICKS.get()).intValue());
   }

   private static long soloFireTicks() {
      return Math.max(1L, ((Integer)Config.HOVER_CANNON_SOLO_FIRE_TICKS.get()).intValue());
   }

   private static long soloFadeTicks() {
      return Math.max(0L, ((Integer)Config.HOVER_CANNON_SOLO_FADE_TICKS.get()).intValue());
   }

   private static long widenAt(long into, long base) {
      return into < 0L ? base : base + into / 3L;
   }

   private static Vec3 soloOrb(EntityMaid maid) {
      return maid.position().add(0.0, maid.getBbHeight() + 1.5, 0.0);
   }

   public static double soloRingRadius() {
      return 1.5;
   }

   public static Vec3 soloPodOffset(int pod, double spin, UUID id, double radius) {
      double angle = spin * 0.01308996938995747 + pod * (Math.PI * 2.0 / 3.0) + (id.hashCode() & 31) * 0.013;
      double y = 0.15 * Math.sin(spin * 0.03 + pod * 2.1);
      return new Vec3(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
   }

   private static void stepSolo(EntityMaid maid, HoverCannon.Solo solo, long now) {
      long since = now - solo.startTick;
      long charge = soloChargeTicks();
      long fire = soloFireTicks();
      long fade = soloFadeTicks();
      if (since >= charge + fire + fade) {
         endSolo(maid, false);
      } else if (maid.level() instanceof ServerLevel level) {
         maid.getNavigation().stop();
         LivingEntity target = entityById(level, solo.targetId);
         if (target == null && since >= charge) {
            endSolo(maid, true);
         } else {
            if (since >= charge && target != null) {
               long fired = since - charge;
               long fading = fired - fire;
               if (fired - solo.lastStrike >= widenAt(fading, 1L)) {
                  solo.lastStrike = fired;
                  float damage = (float)(
                     attackPower(maid, target) * (Double)Config.HOVER_CANNON_SOLO_ATTACK_RATIO.get()
                        + currentHealth(target) * (Double)Config.HOVER_CANNON_SOLO_HEALTH_RATIO.get()
                  );
                  if (damage > 0.0F) {
                     sear(maid, target, damage);
                  }
               }

               if (fired - solo.lastHit >= widenAt(fading, 3L)) {
                  solo.lastHit = fired;
                  Vec3 mid = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
                  Vec3 orb = soloOrb(maid);
                  Vec3 edge = nearestEdgeFrom(orb, target, mid);
                  ModPackets.sendShockwave(level, edge, soloLockRange() + target.getBbWidth(), 1.6F, 12, facingFrom(orb, edge), true);
               }
            }

            if (since >= 12L && since - solo.lastShake >= 3L) {
               solo.lastShake = since;
               float power = 0.33333334F * shakeScale(since, charge, fire, fade);
               Vec3 at = target == null ? soloOrb(maid) : maid.position().add(target.position()).scale(0.5);
               double reach = soloLockRange() + (target == null ? 0.0 : target.getBbWidth());
               ModPackets.sendShake(level, at, reach, 6, power, 0.0F, 1.0F, true);
            }
         }
      }
   }

   private static float shakeScale(long since, long charge, long fire, long fade) {
      if (since < charge) {
         return 0.35F;
      } else {
         long fired = since - charge;
         if (fired >= fire && fade > 0L) {
            float t = Math.min(1.0F, (float)(fired - fire) / (float)fade);
            return 1.0F - 0.65F * t;
         } else {
            return 1.0F;
         }
      }
   }

   private static void endSolo(EntityMaid maid, boolean tellClient) {
      if (SOLOS.remove(maid.getUUID()) != null) {
         if (tellClient && !maid.level().isClientSide) {
            ModPackets.sendSoloBeam(maid, 0, 0L, 0, 0, 0, maid.position(), soloBeamSoundRange(), soloBeamSoundFar());
         }
      }
   }

   public static Vec3 shotPos(Vec3 from, Vec3 to, double travelled) {
      Vec3 axis = to.subtract(from);
      double length = axis.length();
      return !(length < 1.0E-6) && !(travelled >= length) ? from.add(axis.scale(travelled / length)) : to;
   }

   public static float formChargeWidth() {
      return 0.075F;
   }

   public static Vec3 formationPos(int index, Vec3 anchor, Vec3 aim, Vec3 enter, Vec3 leave, double since, long chargeTicks) {
      Vec3 slot = formationSlot(index, anchor, aim);
      if (since < 15.0) {
         double t = smooth(clamp01(since / 15.0));
         return enter.lerp(slot, t);
      } else {
         long settle = 15L + Math.max(0L, chargeTicks) + 8L;
         if (since >= settle) {
            double t = smooth(clamp01((since - settle) / 10.0));
            return slot.lerp(leave, t);
         } else {
            return slot;
         }
      }
   }

   public static Vec3 formationSlot(int index, Vec3 anchor, Vec3 aim) {
      double base = Math.atan2(aim.z, aim.x);
      double angle = base + index * (Math.PI * 2.0 / 3.0);
      return anchor.add(Math.cos(angle) * 2.0, 0.0, Math.sin(angle) * 2.0);
   }

   private static long totalTicks() {
      return settleTicks() + 10L;
   }

   private static long settleTicks() {
      return 15L + chargeTicks() + 8L;
   }

   public static long chargeTicks() {
      return Math.max(1L, ((Integer)Config.HOVER_CANNON_CHARGE_TICKS.get()).intValue());
   }

   private static float formSoundRange() {
      return ((Double)Config.HOVER_CANNON_FORM_RAY_SOUND_RANGE.get()).floatValue();
   }

   private static float formSoundFar() {
      return ((Double)Config.HOVER_CANNON_FORM_RAY_SOUND_FAR.get()).floatValue();
   }

   private static double soloBeamSoundRange() {
      return (Double)Config.HOVER_CANNON_SOLO_BEAM_SOUND_RANGE.get();
   }

   private static double soloBeamSoundFar() {
      return (Double)Config.HOVER_CANNON_SOLO_BEAM_SOUND_FAR.get();
   }

   public static double shotSpeedPerTick() {
      return shotSpeedPerMs() * 50.0;
   }

   private static double shotSpeedPerMs() {
      return Math.max(0.1, (Double)Config.HOVER_CANNON_SHOT_SPEED.get()) / 1000.0;
   }

   private static Vec3 flat(Vec3 v) {
      Vec3 flat = new Vec3(v.x, 0.0, v.z);
      return flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
   }

   private static double clamp01(double v) {
      return v < 0.0 ? 0.0 : Math.min(v, 1.0);
   }

   private static double smooth(double t) {
      return t * t * (3.0 - 2.0 * t);
   }

   private static LivingEntity entityById(ServerLevel level, int id) {
      return level.getEntity(id) instanceof LivingEntity living && living.isAlive() ? living : null;
   }

   public static void stop(EntityMaid maid) {
      UUID id = maid.getUUID();
      REPORTED.remove(id);
      PINGED.remove(id);
      PICKS.remove(id);
      SHOTS.remove(id);
      FORMS.remove(id);
      CENTERS.remove(id);
      FORM_RUNS.remove(id);

      try {
         endSpin(maid, true);
      } catch (Throwable t) {
         ShenlinMod.LOGGER.warn("[shenlin] hover cannon stop: spin teardown failed: {}", t.toString());
      }

      try {
         endSolo(maid, true);
      } catch (Throwable t) {
         ShenlinMod.LOGGER.warn("[shenlin] hover cannon stop: solo teardown failed: {}", t.toString());
      }

      try {
         if (!maid.level().isClientSide) {
            ModPackets.sendOrbit(maid, 0, 0, 0);
         }
      } catch (Throwable t) {
         ShenlinMod.LOGGER.warn("[shenlin] hover cannon stop: orbit clear failed: {}", t.toString());
      }
   }

   private static final class Chip {
      private double progress;
   }

   private record ChipKey(ResourceKey<Level> dim, BlockPos pos) {
   }

   private static final class Formation {
      private final long startTick;
      private Vec3 anchor;
      private final Vec3 aim;
      private final int targetId;
      private boolean fired;
      private boolean soloChecked;

      private Formation(long startTick, Vec3 anchor, Vec3 aim, int targetId) {
         this.startTick = startTick;
         this.anchor = anchor;
         this.aim = aim;
         this.targetId = targetId;
      }
   }

   private record Pick(long tick, LivingEntity target) {
   }

   private record Report(int state, int target) {
   }

   private record Round(LivingEntity lock, boolean spin, int marks, int foes) {
   }

   private static final class Solo {
      private final long startTick;
      private final int targetId;
      private long lastStrike = -1L;
      private long lastHit = -3L;
      private long lastShake = 9L;

      private Solo(long startTick, int targetId) {
         this.startTick = startTick;
         this.targetId = targetId;
      }
   }

   private static final class Spin {
      private final long startTick;
      private final Set<HoverCannon.ChipKey> touched = new HashSet<>();

      private Spin(long startTick) {
         this.startTick = startTick;
      }
   }
}

