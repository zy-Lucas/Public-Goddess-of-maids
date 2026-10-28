package com.myx00x.maid.mob;

import com.myx00x.maid.Config;
import com.myx00x.maid.FxTrace;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.block.RelicLocator;
import com.myx00x.maid.client.ClientWardenParts;
import com.myx00x.maid.client.StarEffect;
import com.myx00x.maid.client.WardenFlashScreen;
import com.myx00x.maid.client.WardenTailTrail;
import com.myx00x.maid.combat.Executioner;
import com.myx00x.maid.combat.HeartGuard;
import com.myx00x.maid.network.ModPackets;
import com.myx00x.maid.registry.ModEntities;
import com.myx00x.maid.registry.ModItems;
import com.myx00x.maid.registry.ModSounds;
import com.myx00x.maid.world.RelicSites;
import com.myx00x.maid.world.WardenVitalsData;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Entity.MoveFunction;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.entity.PartEntity;

public class RelicWardenEntity extends Monster {
   private static final int DORMANT_RISE_TICKS = 40;
   private static final double DORMANT_HEAD_DROP = 0.5625;
   private static final float DORMANT_HEAD_PITCH_DEG = 18.0F;
   private static final EntityDataAccessor<Boolean> DATA_DORMANT = SynchedEntityData.defineId(RelicWardenEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> DATA_BATTLE = SynchedEntityData.defineId(RelicWardenEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Float> DATA_CORE = SynchedEntityData.defineId(RelicWardenEntity.class, EntityDataSerializers.FLOAT);
   private static final float SHADOW_SCALE = 0.01F;
   private static final float SHADOW_OFFSET = 3.0F;
   private static final int CHASE_REPATH_TICKS = 6;
   private static final boolean VERBOSE_LOG = false;
   private static final float CHASE_TURN_PER_TICK = 10.0F;
   private static final int CHASE_STRAIGHT_MAX_STUCK = 12;
   private static final double CHARGE_HEAD_GUARD = 3.5;
   private static final int CHARGE_THRUST_LOCK = 25;
   private static final double CHARGE_JET_BACK = 1.6;
   private static final double CHARGE_JET_UP = 2.3;
   private static final double CHARGE_SHOVE_SPEED = 0.8;
   private static final double CHARGE_SHOVE_LIFT = 0.45;
   private static final double CHARGE_SHOVE_RANGE = 4.0;
   private static final int CHARGE_SHOVE_TICKS = 8;
   private static final double CHASE_SPEED = 3.225;
   private static final double WARDEN_MAX_HEALTH = 250.0;
   public static final float LEG_SWING = 0.9F;
   private static final double LEG_LENGTH = 1.5;
   private static final double GAIT_STRIDE = 9.0 * Math.sin(0.9F);
   private static final double TURN_ARM = 0.9;
   private static final float GAIT_GAIN = 6.0F;
   private static final float GAIT_STEP_CAP = 0.55F;
   private static final float GAIT_SMOOTH = 0.35F;
   private static final float TAU = (float) (Math.PI * 2);
   private static final float DEG = (float) (Math.PI / 180.0);
   private static final float HEAD_BASE_R = 40.0F;
   private static final float HEAD_YAW_PUSH = 9.0F;
   private static final float HEAD_PITCH_PUSH = 8.0F;
   private static final double HEAD_CENTER_Y = 2.125;
   private static final float HEAD_PART_W = 2.0F;
   private static final float HEAD_PART_H = 1.5F;
   private static final float HEAD_PART_D = 2.0F;
   private static final int SLOT_BODY = 1;
   private static final int SLOT_HEAD = 2;
   private static final int SLOT_SPIKE = 3;
   private final RelicWardenPart headPart;
   private final RelicWardenPart bodyPart;
   private final RelicWardenPart spikePart;
   private final PartEntity<?>[] parts;
   private final BodyRotationControl bodyControl;
   private static final Map<RelicWardenEntity, RelicWardenEntity.Vitals> VITALS = Collections.synchronizedMap(new WeakHashMap<>());
   private static volatile EntityDataAccessor<Float> healthSlot;
   private static volatile boolean healthSlotProbed;
   private boolean healthReady;
   private volatile boolean ownZero;
   private long lastDieRefused;
   private static final int SEAL_KEY = sealKey();
   private static Field itemsByIdField;
   private static Field itemValueField;
   private static Method accessorGetter;
   private long lastWardNote = -4000L;
   private long lastHitNote = -300L;
   private long lastOwnNote = -300L;
   private long lastFoldNote = -4000L;
   private long lastSettleNote = -200L;
   private int healthGate;
   private volatile int hurtCooldown;
   private volatile long windowUntilMs;
   private volatile long windowOpenedMs;
   private volatile long lastInvulnZeroNote;
   private long lastCoreNote;
   private volatile boolean releasing;
   private static final BlockPos[] AFTERMATH_CLEAR = new BlockPos[]{new BlockPos(-9, 2, -6), new BlockPos(-9, 2, -5)};
   private static final BlockPos AFTERMATH_SLAB = new BlockPos(-9, 1, -5);
   private static final int AFTERMATH_CORE_RADIUS = 50;
   private boolean aftermathDone;
   private boolean blasted;
   private static final int SKILL_RISE_END = 22;
   private static final int SKILL_EYE_FROM = 11;
   private static final int SKILL_LOOK_END = 36;
   private static final int SKILL_SMASH_TICK = 40;
   private static final int SKILL_FIRE_END = 48;
   private static final int SKILL_FLY_TICK = 40;
   private static final int SKILL_FLY_TICKS = 12;
   private static final int SKILL_FLY_END = 52;
   private static final int SKILL_FLASH_AT = 5;
   private static final float SKILL_HEAD_UP = -40.0F;
   private static final float SKILL_HEAD_DOWN = 54.0F;
   private static final double SKILL_SMASH_RADIUS = 1.5;
   private static final double SKILL_LAND_RADIUS = 2.5;
   private static final int SLAM_DUST_OUTER = 40;
   private static final float SLAM_DUST_ALPHA = 2.0F;
   private static final double SLAM_DUST_RING = 5.0;
   private static final float SLAM_DUST_SIZE = 0.42F;
   private static final int SLAM_DUST_TICKS = 70;
   private static final float SLAM_DUST_R = 0.44F;
   private static final float SLAM_DUST_G = 0.44F;
   private static final float SLAM_DUST_B = 0.45F;
   private static final double SLAM_DUST_LIFT = 0.1;
   private static final double SLAM_DUST_LIFT_VARY = 0.3;
   private static final int SLAM_DUST_MODE = 3;
   private static final int SLAM_BURST_LEAD = 0;
   private static final float SKILL_SMASH_SCALE = 0.1F;
   private static final float SKILL_LAND_SCALE = 0.3F;
   private static final double SKILL_LAUNCH_DIST = 9.0;
   private static final double SKILL_LAUNCH_HEIGHT = 4.4;
   private static final double SKILL_DEBRIS_RADIUS = 2.5;
   private static final int SKILL_DEBRIS_COUNT = 40;
   private static final double SKILL_BEAM_RANGE = 256.0;
   private static final int SKILL_BEAM_TICKS = 3;
   private static final int SKILL_AIM_TICK = 22;
   private static final double CORE_POINT_Y = 3.3125;
   private static final double CORE_POINT_FWD = 0.1875;
   private static final double CORE_POINT_SIDE = 0.125;
   private static final double AIR_EMIT_Y = 4.53125;
   private static final double AIR_EMIT_FWD = 0.25;
   private static final float LAMP_IDLE_GLOW = 0.35F;
   private static final float LAMP_WHITEN = 0.55F;
   private static final double SMASH_MOUTH_OUT = 1.7;
   private static final int AIR_CHARGE_TICKS = 8;
   private static final int AIR_MAX_TICKS = 90;
   private static final int AIR_REST_TICKS = 22;
   private static final float AIR_HIT_RING_RADIUS = 0.7F;
   private static final int AIR_HIT_RING_TICKS = 11;
   private static final int AIR_RING_EVERY = 2;
   private static final float AIR_HIT_ATK = 0.1F;
   private static final float AIR_HIT_HEALTH = 0.01F;
   private static final float AIR_DRAIN_KEEP = 0.99F;
   private static final int AIR_BEAM_KEEP = 200;
   private static final int AIR_BEAM_RENEW = 120;
   private static final int AIR_BEAM_FADE = 8;
   private static final int AIR_BEAM_RING_EVERY = 3;
   private static final float AIR_HIT_WAVE_RADIUS = 1.6F;
   private static final float AIR_HIT_FLARE_SCALE = 0.55F;
   private static final int AIR_HIT_WAVE_TICKS = 12;
   private static final long AIR_SHAKE_FROM = 12L;
   private static final float AIR_SHAKE_CHARGE = 0.35F;
   private static final long AIR_SHAKE_EVERY = 3L;
   private static final int AIR_SHAKE_TICKS = 6;
   private static final float AIR_SHAKE_POWER = 0.33333334F;
   private static final float AIR_SHAKE_ROUGH = 1.0F;
   private static final float AIR_SHAKE_FADE_QUIET = 0.15F;
   private static final double AIR_BEAM_NOTIFY = 40.0;
   private static final float AIR_GLOW_STEP = 0.16F;
   private static final double PULL_BEAM_LENGTH = 18.0;
   private static final int PULL_BEAM_TICKS = 23;
   private static final int PULL_BEAM_FADE_TICKS = 2;
   private static final float PULL_BEAM_SCALE = 2.2F;
   private static final float LAND_WAVE_RADIUS = 1.7F;
   private static final int LAND_WAVE_TICKS = 9;
   private static final double SNAP_DOWN_MAX = 12.0;
   private static final float PULL_BEAM_MIN_LIFT_PITCH = -30.0F;
   private static final float PULL_BEAM_R = 1.0F;
   private static final float PULL_BEAM_G = 0.45F;
   private static final float PULL_BEAM_B = 0.12F;
   private static final int GRIP_SHAKE_TICKS = 24;
   private static final float GRIP_SHAKE_POWER = 1.0F;
   private static final float GRIP_SHAKE_PULL = 9.0F;
   private static final float GRIP_SHAKE_BOUNCE = 1.6F;
   private static final int HEAD_BASE = 52;
   private static final int HEAD_JUDGE_AT = 0;
   private static final int HEAD_JUDGE = 20;
   private static final int HEAD_THRUST_END = 23;
   private static final int HEAD_RED_END = 40;
   private static final int HEAD_PULL_END = 50;
   private static final int HEAD_LAUNCH = 55;
   private static final int HEAD_RANGED_WINDUP = 5;
   private static final int HEAD_FLY_TICKS = 6;
   private static final int HEAD_RECALL_HOLD = 10;
   private static final int HEAD_RECALL_TICKS = 33;
   private static final float HEAD_PIN_FADE = 0.08F;
   private static final int HURT_GLOW_RISE = 1;
   private static final int HURT_GLOW_FALL = 3;
   private static final int HEAD_SETTLE = 104;
   private static final int CHARGE_DUCK_TICKS = 20;
   private static final float CHARGE_DUCK_PITCH = 32.0F;
   private static final double CHARGE_STEP = 1.35;
   private static final float CHARGE_TURN_PER_TICK = 8.0F;
   private static final double CHARGE_STEP_UP = 1.02;
   private static final int CHARGE_RUSH_MAX = 26;
   private static final double CHARGE_BLOCK_FRAC = 0.35;
   private static final double CHARGE_HIT_REACH = 2.8;
   private static final int CHARGE_HOLD_TICKS = 3;
   private static final int CHARGE_LIFT_TICKS = 12;
   private static final int CHARGE_LIFT_HOLD_TICKS = 4;
   private static final int CHARGE_SLAM_TICKS = 6;
   private static final float CHARGE_LIFT_PITCH = -30.0F;
   private static final float CHARGE_SLAM_PITCH = 55.0F;
   private static final float CHARGE_HIT_SCALE = 0.25F;
   private static final float CHARGE_HIT_TRUE_SCALE = 0.05F;
   private static final float CHARGE_SLAM_SCALE = 0.66F;
   private static final int CHARGE_COOLDOWN = 80;
   private static final int CHARGE_COOLDOWN_CLOSE = 20;
   private static final float CHARGE_HORN_FADE = 0.12F;
   private static final int CHARGE_RIPPLE_TICKS = 16;
   private static final int CHARGE_STUCK_TICKS = 55;
   private static final int CHARGE_PULL_TICKS = 25;
   private static final int CHARGE_PULL_RISE_TICKS = 6;
   private static final float CHARGE_REAR_PITCH = -45.0F;
   private static final int[] CHARGE_TRY_AT = new int[]{8, 28};
   private static final int CHARGE_TRY_TICKS = 12;
   private static final int CHARGE_PULL_SETTLE = 2;
   private static final int CHARGE_TRY_FALL = 2;
   private static final double CHARGE_PULL_REACH = 4.0;
   private static final float CHARGE_PULL_SCALE = 0.1F;
   private static final double CHARGE_PULL_SPEED = 1.3;
   private static final double CHARGE_PULL_LIFT = 0.7;
   private static final float CHARGE_HEAD_BIAS = 100.0F;
   public static final float BODY_RISE_PITCH = 14.0F;
   private static final float CHARGE_SPIKE_REACH = 1.2F;
   private static final int CHARGE_CLOSE_DUCK_TICKS = 12;
   private static final double CHARGE_THRUST_REACH = 3.0;
   private static final float CHARGE_THRUST_SCALE = 0.2F;
   private static final double CHARGE_THRUST_KNOCK = 2.4;
   private static final double CHARGE_THRUST_LIFT = 0.95;
   private static final float CHARGE_THRUST_FADE = 0.35F;
   private static final double CHARGE_WALL_AHEAD = 0.8;
   private static final double CHARGE_SPIKE_ROOT = 3.1;
   private static final double CHARGE_SPIKE_TIP = 5.6;
   private static final int CHARGE_SPIKE_SAMPLES = 12;
   private static final float CHARGE_SPIKE_PART_W = 0.7F;
   private static final double CHARGE_SPIKE_ROOT_Y = 2.1;
   private static final double CHARGE_SPIKE_TIP_Y = 0.8;
   private static final double CHARGE_BREAK_SHARE = 0.3;
   private static final int CHARGE_BREAK_CHIPS = 18;
   private static final float CHARGE_CRASH_WAVE_RADIUS = 6.0F;
   private static final int CHARGE_CRASH_WAVE_TICKS = 14;
   private static final int CHARGE_CRASH_TICKS = 24;
   private static final int CHARGE_CRASH_FLASH_AT = 12;
   private static final int CHARGE_CRASH_HOLD = 2;
   private static final double CHARGE_MAD_KNOCK = 2.2;
   private static final double CHARGE_MAD_LIFT = 0.45;
   private static final int CHARGE_MAD_GAP = 12;
   private static final float HEAD_THRUST_DIST = 1.5F;
   private static final float HEAD_GLOW_FADE = 3.0F;
   private static final int SWING_WIND = 10;
   private static final int SWING_PASS = 3;
   private static final int SWING_HOLD = 6;
   private static final int SWING_PASS1_AT = 10;
   private static final int SWING_HOLD1_AT = 13;
   private static final int SWING_PASS2_AT = 19;
   private static final int SWING_HOLD2_AT = 22;
   private static final int SWING_RAISE_AT = 28;
   private static final int SWING_RAISE = 27;
   private static final int SWING_STAR_AT = 55;
   private static final int SWING_SLAM_AT = 55;
   private static final int SWING_SETTLE = 16;
   private static final int SWING_END = 74;
   private static final int SWING_COOLDOWN = 45;
   private static final int SWING_COOLDOWN_NEAR = 6;
   private static final float SWING_YAW = 66.0F;
   private static final double SWING_REACH = 6.2;
   private static final float SWING_ARC_DEG = 80.0F;
   private static final float SWING_HIT_SCALE = 0.15F;
   private static final double SWING_SLAM_RANGE = 6.0;
   private static final float SWING_SLAM_SCALE = 1.0F;
   private static final float SWING_TRUE_PCT = 0.55F;
   private static final float SWING_HEAD_LIFT = 3.4F;
   private static final int SWING_STAR_TICKS = 2;
   private static final float SWING_KICK_DEG = 3.4F;
   private static final double SWING_KEEP_DIST = 4.2;
   private static final double SWING_BLINK_BEHIND = 2.8;
   private static final int SWING_BLINK_COVER = 3;
   private static final double SWING_BACK_STEP = 0.32;
   private static final double SWING_SLAM_LIFT = 0.6;
   private static final float SWING_REACH_OUT = 2.35F;
   private static final float SWING_RAISE_PITCH = -46.0F;
   private static final float SWING_SLAM_PITCH = 36.0F;
   private static final int SWING_DUST_PUFFS = 48;
   private static final double SWING_DUST_RING = 6.0;
   private static final float SWING_ACCEL = 0.72F;
   private static final int SWING_STRIKE_OFF = 5;
   private static final float SWING_HEAD_HALF = 1.575F;
   private static final float SWING_BODY_HALF = 1.15F;
   private static final double SWING_KNOCK_SPEED = 5.0;
   private static final double SWING_KNOCK_LIFT = 0.42;
   private static final float SWING_STAR_RADIUS = 5.2F;
   private static final float SWING_STAR_WIDTH_SCALE = 2.0F;
   private static final int SWING_GATHER_ARMS = 6;
   private static final int SWING_GATHER_PER_ARM = 7;
   private static final int SWING_GATHER_PARTICLES = 9;
   private static final double SWING_GATHER_RADIUS = 3.6;
   private static final double HEAD_REACH = 2.5;
   private static final float HEAD_STRIKE_SCALE = 0.2F;
   private static final double HEAD_KNOCK_SPEED = 1.2;
   private static final double HEAD_KNOCK_LIFT = 0.6;
   private static final double HEAD_FLY_MIN = 8.0;
   private static final double HEAD_FLY_ARC = 1.4;
   private static final double HEAD_LAUNCH_Y = 2.1;
   private static final double HEAD_BLAST_RADIUS = 2.5;
   private static final float HEAD_BLAST_SCALE = 1.0F;
   private static final float HEAD_BURST = 3.0F;
   private static final double HEAD_RECALL_RADIUS = 1.5;
   private static final float HEAD_RECALL_SCALE = 0.88F;
   private static final int DEATH_SHOW = 55;
   private static final int DEATH_HEAD_FALL = 6;
   private static final float DEATH_HEAD_FWD = 1.3F;
   private static final float DEATH_HEAD_DROP = 1.5F;
   private static final int DEATH_WARP_GROW_MS = 300;
   private static final int DEATH_WARP_HOLD_MS = 2050;
   private static final float DEATH_WARP_FAST_RADIUS = 66.0F;
   private static final float DEATH_WARP_RADIUS = 72.0F;
   private static final int DEATH_WARP_KIND = 1;
   private static final double DEATH_WARP_RANGE = 150.0;
   private static final double DEATH_PUSH_RANGE = 16.0;
   private static final double DEATH_PUSH_PER_SPEED = 10.0;
   private static final double DEATH_PUSH_LIFT = 0.42;
   private static final double DEATH_BURST_UP = 1.4;
   private static final int DEATH_DUST_PUFFS = 96;
   private static final double DEATH_DUST_SPREAD = 19.5;
   private static final double DEATH_DUST_LIFT = 0.6;
   private static final double DEATH_DUST_UP = 10.2;
   private static final float DEATH_DUST_SIZE = 2.15F;
   private static final int DEATH_DUST_TICKS = 200;
   private static final float DEATH_DUST_R = 0.47F;
   private static final float DEATH_DUST_G = 0.45F;
   private static final float DEATH_DUST_B = 0.43F;
   private static final float DEATH_DUST_ALPHA = 1.9F;
   private static final int DEATH_DUST_MODE = 4;
   private static final double DEATH_BURST_SPREAD = 1.1;
   private static final int DEATH_SOUL_COUNT = 60;
   private static final double DEATH_SOUL_SPEED = 0.16;
   private static final int DEATH_SOULFIRE_COUNT = 50;
   private static final double DEATH_SOULFIRE_SPEED = 0.42;
   private static final int DEATH_FLAME_COUNT = 90;
   private static final double DEATH_FLAME_SPEED = 0.55;
   private static final float HEAD_FACE_DEG = 30.0F;
   private static final int HEAD_WAIT_MAX = 60;
   private static final double BEAM_KNOCK_SPEED = 0.6;
   private static final double BEAM_KNOCK_LIFT = 0.3;
   private static final int SKILL_TAIL_END = 240;
   private static final EntityDataAccessor<Integer> DATA_SKILL_TICK = SynchedEntityData.defineId(RelicWardenEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_RISE_TICK = SynchedEntityData.defineId(RelicWardenEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_HEAD_MODE = SynchedEntityData.defineId(RelicWardenEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_HEAD_START = SynchedEntityData.defineId(RelicWardenEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_HEAD_STAGE = SynchedEntityData.defineId(RelicWardenEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_DEATH_SHOW = SynchedEntityData.defineId(RelicWardenEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> DATA_HEAD_RANGE = SynchedEntityData.defineId(RelicWardenEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DATA_HEAD_ANGLE = SynchedEntityData.defineId(RelicWardenEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DATA_FACE_ANGLE = SynchedEntityData.defineId(RelicWardenEntity.class, EntityDataSerializers.FLOAT);
   private static final float FACE_ANGLE_BIAS = 180.0F;
   private static final EntityDataAccessor<Integer> DATA_FLASH_SEQ = SynchedEntityData.defineId(RelicWardenEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_CHARGE_TICK = SynchedEntityData.defineId(RelicWardenEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> DATA_CHARGE_HEAD = SynchedEntityData.defineId(RelicWardenEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DATA_CHARGE_HORN = SynchedEntityData.defineId(RelicWardenEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> DATA_CHARGE_VICTIM = SynchedEntityData.defineId(RelicWardenEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> DATA_CHARGE_SPIKE = SynchedEntityData.defineId(RelicWardenEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DATA_CHARGE_THRUST = SynchedEntityData.defineId(RelicWardenEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> DATA_SWING_TICK = SynchedEntityData.defineId(RelicWardenEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> DATA_SWING_TAIL = SynchedEntityData.defineId(RelicWardenEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> DATA_SWING_GLIDE = SynchedEntityData.defineId(RelicWardenEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Float> DATA_CHARGE_CRASH = SynchedEntityData.defineId(RelicWardenEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DATA_CHARGE_RISE = SynchedEntityData.defineId(RelicWardenEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DATA_CHARGE_LEGS = SynchedEntityData.defineId(RelicWardenEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DATA_CHARGE_KICK = SynchedEntityData.defineId(RelicWardenEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> DATA_AIR_TICK = SynchedEntityData.defineId(RelicWardenEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> DATA_HOLD_ON = SynchedEntityData.defineId(RelicWardenEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Float> DATA_HOLD_YAW = SynchedEntityData.defineId(RelicWardenEntity.class, EntityDataSerializers.FLOAT);
   private static final ResourceKey<DamageType> BEAM_DAMAGE = ResourceKey.create(
      Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("goddess_of_maids", "warden_beam")
   );
   private boolean skillDone;
   private int skillTick = -1;
   @Nullable
   private Vec3 skillAim;
   @Nullable
   private Vec3 skillOrigin;
   private boolean skillLaunched;
   private int skillHang;
   private Vec3 skillAway = Vec3.ZERO;
   private int flashSeq;
   private int flashSeqSeen = -1;
   private Vec3 headDir = Vec3.ZERO;
   private double headRange;
   private int headStart;
   private float headFlyLocked;
   private boolean headFlyHeld;
   private float headPin = 1.0F;
   private final Map<Mob, int[]> stunned = new WeakHashMap<>();
   private int airTick = -1;
   private int airCooldown;
   private float airGlow;
   private Vec3 airAnchor = Vec3.ZERO;
   private int airBeamTarget;
   private int chargeTick = -1;
   private int chargeCooldown = 80;
   @Nullable
   private LivingEntity chargeTarget;
   private Vec3 chargeDir = Vec3.ZERO;
   @Nullable
   private LivingEntity chargeVictim;
   private Vec3 pulledSpot;
   @Nullable
   private boolean pullEntered;
   private boolean pullLandDone;
   private WardenLaserBeamEntity gripBeam;
   private static final int STATIC_BEAM_FRAME = 5;
   private static final float SKILL_BEAM_SCALE = 0.62F;
   @Nullable
   private WardenLaserBeamEntity pewBeam;
   @Nullable
   private WardenLaserBeamEntity airBeam;
   private int chargeHitTick = -1;
   private float chargeStuckYaw;
   private boolean chargeClose;
   private boolean spikeBroken;
   @Nullable
   private LivingEntity chargeMadLast;
   private int chargeMadTick = -100;
   private double chargePeakOverlap;
   private int chargeCrashTick = -1;
   private boolean slamSpawned;
   private int slamSoundTick = -99;
   private int thrustLock;
   private int swingTick = -1;
   private boolean swingBlinked;
   private int swingBlinkWait;
   private Vec3 swingBlinkTo;
   private float swingHoldYaw;
   private boolean holdAnchorOn;
   private double holdX;
   private double holdY;
   private double holdZ;
   private float holdYaw;
   private boolean blinkRelocating;
   private int swingCooldown;
   private final Set<Integer> swingHitIds = new HashSet<>();
   private int swingHitMask;
   private int swingAnimPrev = -1;
   private int swingAnimCur = -1;
   private boolean deathBlastDone;
   private boolean retiredByOrder;
   private static final double LAST_POS_LIMIT = 3.0E7;
   public static final double POS_JUMP_LIMIT = 66.0;
   private final Map<LivingEntity, int[]> thrustPushed = new WeakHashMap<>();
   private float chargeAimYaw;
   private int clientSkillTick = -1;
   private int clientSkillPrev = -1;
   private int headActLocal = -1;
   private float headHeld = -1.0F;
   private int hurtGlowTick;
   private int hurtTimePrev;
   private boolean clientSkillDone;
   private float gaitPhase;
   private float prevGaitPhase;
   private float gaitPower;
   private float prevGaitYaw;
   private float chargeLegsO;
   private float chargeLegsLag;
   private float chargeRiseO;
   private float chargeKickO;
   private float chargeCrashO;
   private float chargeHeadPitchO;
   private float chargeSpikeO;
   private float chargeThrustO;
   private float skillHeadPitchO;
   private float headPinO = 1.0F;
   private float gaitPowerO;
   private float chargeRiseLag;
   private float chargeKickLag;
   private float chargeCrashLag;
   private float chargeHeadPitchLag;
   private float chargeSpikeLag;
   private float chargeThrustLag;
   private float skillHeadPitchLag;
   private float headPinLag;
   private float gaitPowerLag;
   private float dormantLerp;
   private float dormantLerpO;
   private boolean dormantLerpSettled;
   private boolean born;
   private BlockPos homeCore;
   private int coreRetry;
   private static final Set<EntityDataAccessor<Float>> OWN_FLOATS = Set.of(
      DATA_CORE,
      DATA_HEAD_RANGE,
      DATA_HEAD_ANGLE,
      DATA_FACE_ANGLE,
      DATA_CHARGE_HEAD,
      DATA_CHARGE_HORN,
      DATA_CHARGE_SPIKE,
      DATA_CHARGE_THRUST,
      DATA_CHARGE_CRASH,
      DATA_CHARGE_RISE,
      DATA_CHARGE_LEGS,
      DATA_CHARGE_KICK
   );

   private static float toShadow(float health) {
      return health * 0.01F + 3.0F;
   }

   private static float fromShadow(float shadow) {
      return (shadow - 3.0F) / 0.01F;
   }

   private RelicWardenEntity.Vitals vitals() {
      return VITALS.computeIfAbsent(this, k -> new RelicWardenEntity.Vitals());
   }

   private EntityDataAccessor<Float> healthSlot() {
      if (healthSlotProbed) {
         return healthSlot;
      }

      synchronized (RelicWardenEntity.class) {
         if (healthSlotProbed) {
            return healthSlot;
         }

         healthSlotProbed = true;
         EntityDataAccessor<Float> smallest = null;
         EntityDataAccessor<Float> confirmed = null;
         float now = this.getMaxHealth();

         try {
            for (Field f : LivingEntity.class.getDeclaredFields()) {
               if (Modifier.isStatic(f.getModifiers()) && EntityDataAccessor.class.isAssignableFrom(f.getType())) {
                  f.setAccessible(true);
                  if (f.get(null) instanceof EntityDataAccessor<?> acc && acc.getSerializer() == EntityDataSerializers.FLOAT) {
                     EntityDataAccessor<Float> slot = (EntityDataAccessor<Float>)acc;
                     if (smallest == null || slot.getId() < smallest.getId()) {
                        smallest = slot;
                     }

                     try {
                        Float got = (Float)this.entityData.get(slot);
                        if (got != null && Math.abs(got - now) < 0.001F) {
                           confirmed = slot;
                           break;
                        }
                     } catch (Throwable var14) {
                     }
                  }
               }
            }
         } catch (Throwable t) {
            ShenlinMod.LOGGER.warn("[shenlin] health slot probe failed", t);
         }

         EntityDataAccessor<Float> best = confirmed != null ? confirmed : smallest;
         healthSlot = best;
         if (best != null) {
            ShenlinMod.LOGGER.info("[shenlin] health slot bound id={} confirmed={}", best.getId(), confirmed != null);
         }

         return best;
      }
   }

   private float rawHealth() {
      EntityDataAccessor<Float> slot = this.healthSlot();
      if (slot != null) {
         try {
            Float v = (Float)this.entityData.get(slot);
            if (v != null && !Float.isNaN(v)) {
               return v;
            }
         } catch (Throwable var3) {
         }
      }

      return this.coreOf();
   }

   private void writeRawHealth(float value) {
      float v = Mth.clamp(value, 0.0F, 250.0F);
      if (this.born) {
         EntityDataAccessor<Float> slot = this.healthSlot();
         if (slot != null) {
            try {
               this.entityData.set(slot, v);
               return;
            } catch (Throwable var5) {
            }
         }
      }
   }

   private float coreOf() {
      return Float.intBitsToFloat(this.vitals().core ^ SEAL_KEY);
   }

   private void setCore(float value) {
      float old = this.coreOf();
      this.vitals().core = Float.floatToRawIntBits(value) ^ SEAL_KEY;
      if (value < old - 0.01F && this.lastCoreNote + 200L < System.currentTimeMillis()) {
         this.lastCoreNote = System.currentTimeMillis();
         ShenlinMod.LOGGER.info("[shenlin] core down {} -> {} by={}", new Object[]{String.format("%.2f", old), String.format("%.2f", value), caller()});
      }
   }

   private float savedOf() {
      return Float.intBitsToFloat(this.vitals().saved ^ SEAL_KEY);
   }

   private void setSaved(float value) {
      this.vitals().saved = Float.floatToRawIntBits(value) ^ SEAL_KEY;
   }

   private float goodOf() {
      return Float.intBitsToFloat(this.vitals().good ^ SEAL_KEY);
   }

   private void setGood(float value) {
      this.vitals().good = Float.floatToRawIntBits(value) ^ SEAL_KEY;
   }

   static List<RelicWardenEntity> watched() {
      synchronized (VITALS) {
         return new ArrayList<>(VITALS.keySet());
      }
   }

   private static int sealKey() {
      int key = new Random().nextInt();
      return key == 0 ? 1515897795 : key;
   }

   private static int rgbOf(float r, float g, float b) {
      return (int)(r * 255.0F) << 16 | (int)(g * 255.0F) << 8 | (int)(b * 255.0F);
   }

   public RelicWardenEntity(EntityType<? extends RelicWardenEntity> type, Level level) {
      super(type, level);
      this.xpReward = 30;
      this.setNoAi(true);
      this.setMaxUpStep(1.0F);
      this.headPart = new RelicWardenPart(this, 2, 2.0F, 1.5F, 2.0F);
      this.bodyPart = new RelicWardenPart(this, 1, 2.7F, 3.0F, 3.5F);
      this.spikePart = new RelicWardenPart(this, 3, 0.7F, 0.7F, 2.5F);
      this.parts = new PartEntity[]{this.bodyPart, this.headPart, this.spikePart};
      this.bodyControl = new BodyRotationControl(this);
      this.prevGaitYaw = this.yBodyRot;
      this.born = true;
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(DATA_CORE, toShadow(250.0F));
      this.entityData.define(DATA_DORMANT, Boolean.TRUE);
      this.entityData.define(DATA_BATTLE, Boolean.FALSE);
      this.entityData.define(DATA_SKILL_TICK, -1);
      this.entityData.define(DATA_RISE_TICK, -1);
      this.entityData.define(DATA_HEAD_MODE, 0);
      this.entityData.define(DATA_HEAD_RANGE, 0.0F);
      this.entityData.define(DATA_HEAD_ANGLE, 0.0F);
      this.entityData.define(DATA_FACE_ANGLE, 180.0F);
      this.entityData.define(DATA_FLASH_SEQ, 0);
      this.entityData.define(DATA_HEAD_START, 0);
      this.entityData.define(DATA_HEAD_STAGE, -1);
      this.entityData.define(DATA_DEATH_SHOW, 0);
      this.entityData.define(DATA_CHARGE_TICK, -1);
      this.entityData.define(DATA_CHARGE_HEAD, 100.0F);
      this.entityData.define(DATA_CHARGE_HORN, 0.0F);
      this.entityData.define(DATA_CHARGE_VICTIM, -1);
      this.entityData.define(DATA_CHARGE_SPIKE, 0.0F);
      this.entityData.define(DATA_CHARGE_CRASH, -1.0F);
      this.entityData.define(DATA_CHARGE_RISE, 0.0F);
      this.entityData.define(DATA_CHARGE_LEGS, 0.0F);
      this.entityData.define(DATA_CHARGE_KICK, 0.0F);
      this.entityData.define(DATA_CHARGE_THRUST, 0.0F);
      this.entityData.define(DATA_SWING_TICK, -1);
      this.entityData.define(DATA_SWING_TAIL, false);
      this.entityData.define(DATA_SWING_GLIDE, false);
      this.entityData.define(DATA_AIR_TICK, -1);
      this.entityData.define(DATA_HOLD_ON, false);
      this.entityData.define(DATA_HOLD_YAW, 0.0F);
      this.chargeRiseLag = this.chargeRise();
      this.chargeLegsLag = this.chargeLegs();
      this.chargeKickLag = this.chargeKick();
      this.chargeCrashLag = this.chargeCrash();
      this.chargeHeadPitchLag = this.chargeHeadPitch();
      this.chargeSpikeLag = this.chargeSpike();
      this.chargeThrustLag = this.chargeThrust();
      this.skillHeadPitchLag = this.skillHeadPitch();
      this.headPinLag = this.headPin();
      this.gaitPowerLag = this.gaitPower();
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 250.0)
         .add(Attributes.ATTACK_DAMAGE, 12.0)
         .add(Attributes.MOVEMENT_SPEED, 0.22)
         .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
         .add(Attributes.ARMOR, 8.0)
         .add(Attributes.FOLLOW_RANGE, 32.0);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(2, new RelicWardenEntity.ChaseGoal());
      this.goalSelector.addGoal(7, new RelicWardenEntity.StrollGoal());
      this.goalSelector.addGoal(8, new RelicWardenEntity.IdleLookGoal());
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[0]));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Player.class, true));
   }

   public void tick() {
      if (!this.level().isClientSide) {
         this.wardTick();
      }

      super.tick();
      if (this.level().isClientSide && (Boolean)this.entityData.get(DATA_HOLD_ON)) {
         float hold = (Float)this.entityData.get(DATA_HOLD_YAW);
         this.setYRot(hold);
         this.yRotO = hold;
         this.yBodyRot = hold;
         this.yBodyRotO = hold;
         this.yHeadRot = hold;
         this.yHeadRotO = hold;
      }

      if (this.level().isClientSide && (Integer)this.entityData.get(DATA_CHARGE_TICK) >= 0) {
         float face = this.getYRot();
         this.yBodyRot = face;
         this.yHeadRot = face;
         this.yBodyRotO = this.yRotO;
         this.yHeadRotO = this.yRotO;
      }

      int rawTick = (Integer)this.entityData.get(DATA_SKILL_TICK);
      if (rawTick >= 45 && this.headActTick() < this.headLaunchTick()) {
         float face = this.faceAngle();
         this.yBodyRot = face;
         this.yHeadRot = face;
         this.setYRot(face);
         if (rawTick == 45) {
            this.yBodyRotO = face;
            this.yHeadRotO = face;
            this.yRotO = face;
         }
      }

      if (this.level().isClientSide) {
         int seq = (Integer)this.entityData.get(DATA_FLASH_SEQ);
         if (this.flashSeqSeen < 0) {
            this.flashSeqSeen = seq;
         } else if (seq != this.flashSeqSeen) {
            this.flashSeqSeen = seq;
            WardenFlashScreen.triggerFor(this);
         }
      }

      this.gaitTick();
      this.hurtGlowStep();
      if (this.level().isClientSide) {
         this.clientSkillStep();
         this.clientHeadStep();
         this.clientSwingStep();
      }

      if (this.level().isClientSide) {
         this.tailTrailTick();
      }

      this.dormantTick();
      this.reviveSkillIfNeeded();
      if (this.headFlying()) {
         if (!this.headFlyHeld) {
            this.headFlyLocked = this.getYRot();
            this.headFlyHeld = true;
         }
      } else {
         this.headFlyHeld = false;
      }

      if (this.headActTick() >= 0) {
         this.headPin = 1.0F;
      } else if (this.headPin > 0.0F) {
         this.headPin = Math.max(0.0F, this.headPin - 0.08F);
      }

      if (!this.level().isClientSide) {
         float thrust = (Float)this.entityData.get(DATA_CHARGE_THRUST);
         if (thrust > 0.0F) {
            this.entityData.set(DATA_CHARGE_THRUST, Math.max(0.0F, thrust - 0.35F));
         }

         this.skillStep();
         this.chargeStep();
         this.swingStep();
         if (this.isRemoved()) {
            return;
         }

         this.airStep();
         this.stunTick();
         this.thrustPushTick();
         this.holdStillTick();
      }

      this.chargeLegsO = this.chargeLegsLag;
      this.chargeLegsLag = this.chargeLegs();
      this.chargeRiseO = this.chargeRiseLag;
      this.chargeRiseLag = this.chargeRise();
      this.chargeKickO = this.chargeKickLag;
      this.chargeKickLag = this.chargeKick();
      this.chargeSpikeO = this.chargeSpikeLag;
      this.chargeSpikeLag = this.chargeSpike();
      this.chargeThrustO = this.chargeThrustLag;
      this.chargeThrustLag = this.chargeThrust();
      this.airGlow = Mth.clamp(this.airGlow + (this.airActive() ? 0.16F : -0.16F), 0.0F, 1.0F);
      this.chargeCrashO = this.chargeCrashLag;
      this.chargeCrashLag = this.chargeCrash();
      this.chargeHeadPitchO = this.chargeHeadPitchLag;
      this.chargeHeadPitchLag = this.chargeHeadPitch();
      this.skillHeadPitchO = this.skillHeadPitchLag;
      this.skillHeadPitchLag = this.skillHeadPitch();
      this.headPinO = this.headPinLag;
      this.headPinLag = this.headPin();
      this.gaitPowerO = this.gaitPowerLag;
      this.gaitPowerLag = this.gaitPower();
      this.updateParts();
      if (this.level().isClientSide) {
         ClientWardenParts.register(this);
      }
   }

   private void wardTick() {
      this.groundingGuard();
      if (this.hurtCooldown > 0) {
         this.hurtCooldown--;
      }

      this.invulnerableTime = 0;
      this.realHealth();
      this.flattenOffsets();
      this.settleVitals();
      this.persistVitals();
      this.cureStuckFall();
      if (this.homeCore == null && this.coreRetry < 8 && (this.tickCount & 63) == 0) {
         this.coreRetry++;
         if (this.level() instanceof ServerLevel server) {
            BlockPos found = RelicLocator.nearestCore(server, this.blockPosition(), 24).orElse(null);
            if (found != null) {
               this.homeCore = found;
               RelicSites.get(server).markKnown(found);
            }
         }
      }

      if (this.tickCount % 5 == 0) {
         this.separateOverlapping();
      }
   }

   private void separateOverlapping() {
      AABB box = this.getBoundingBox().inflate(0.1);

      for (Entity other : this.level().getEntities(this, box, e -> e != this && e.isAlive())) {
         Vec3 away = other.position().subtract(this.position());
         Vec3 flat = new Vec3(away.x, 0.0, away.z);
         if (flat.lengthSqr() < 1.0E-4) {
            flat = new Vec3(this.getRandom().nextDouble() - 0.5, 0.0, this.getRandom().nextDouble() - 0.5);
         }

         if (!(flat.lengthSqr() < 1.0E-6)) {
            double force = other instanceof Player ? 0.35 : 0.8;
            flat = flat.normalize().scale(force);
            other.push(flat.x, 0.06, flat.z);
         }
      }
   }

   private void cureStuckFall() {
      if ((Integer)this.entityData.get(DATA_DEATH_SHOW) > 0 && !(this.coreOf() <= 0.0F)) {
         this.entityData.set(DATA_DEATH_SHOW, 0);
         this.endSkill();
         ShenlinMod.LOGGER.info("[shenlin] warden stuck fall cleared");
      }
   }

   public boolean settleVitals() {
      return this.settleVitals(true);
   }

   public boolean settleVitals(boolean mayWrite) {
      RelicWardenEntity.Vitals v = this.vitals();
      if (!v.lock.tryLock()) {
         return false;
      }

      try {
         this.settleLocked(mayWrite);
         return true;
      } finally {
         v.lock.unlock();
      }
   }

   private void settleLocked(boolean mayWrite) {
      float full = 250.0F;
      float table = this.coreOf();
      if (Float.isNaN(table) || table > full) {
         table = full;
         this.setCore(table);
         this.setGood(table);
         this.noteWard("table reset", 0.0F, table);
      } else if (table < 0.0F) {
         table = 0.0F;
         this.setCore(table);
      }

      if (table > 0.0F) {
         this.ownZero = false;
      }

      if (table <= 0.0F && !this.ownZero && !this.releasing) {
         table = this.goodOf() > 0.0F ? this.goodOf() : full;
         this.setCore(table);
         this.setGood(table);
         this.noteWard("table revived", 0.0F, table);
      }

      float game = this.rawHealth();
      if (Float.isNaN(game)) {
         this.writeTable(table);
      } else {
         float diff = game - table;
         if (Math.abs(diff) <= 1.0E-4F) {
            if (mayWrite) {
               this.writeTable(table);
            }
         } else if (diff > 0.0F) {
            if (mayWrite) {
               this.writeTable(table);
            }
         } else if (this.windowOpen()) {
            if (mayWrite) {
               this.writeTable(table);
            }
         } else {
            float loss = -diff;
            float cap = this.damageCap();
            float take = Math.min(loss, cap);
            float next = Math.max(0.0F, table - take);
            this.setGood(table);
            this.setCore(next);
            if (next <= 0.0F) {
               this.ownZero = true;
            }

            this.markHurtWindow();
            if (mayWrite) {
               this.writeTable(next);
            }

            this.noteSettle(loss, take, next);
         }
      }
   }

   private void writeTable(float value) {
      float shadow = toShadow(value);
      if (Math.abs((Float)this.entityData.get(DATA_CORE) - shadow) > 1.0E-4F) {
         this.entityData.set(DATA_CORE, shadow);
      }

      if (Math.abs(this.rawHealth() - value) > 1.0E-4F) {
         this.writeRawHealth(value);
      }
   }

   private void noteSettle(float loss, float take, float left) {
      long now = System.currentTimeMillis();
      if (now - this.lastSettleNote >= 20L) {
         this.lastSettleNote = now;
         ShenlinMod.LOGGER
            .info(
               "[shenlin] warden settle loss={} take={} left={} game={} cool={} win={}ms",
               new Object[]{
                  String.format("%.2f", loss),
                  String.format("%.2f", take),
                  String.format("%.2f", left),
                  String.format("%.2f", this.rawHealth()),
                  this.hurtCooldown,
                  Math.max(0L, this.windowUntilMs - nowMs())
               }
            );
      }
   }

   private float damageCap() {
      return (float)Math.max(0.0, Config.relicWardenDamageCap());
   }

   private void persistVitals() {
      if (this.level() instanceof ServerLevel server) {
         RelicWardenEntity.Vitals var4 = this.vitals();
         float core = this.coreOf();
         if (!(Math.abs(core - this.savedOf()) <= 1.0E-4F)) {
            WardenVitalsData.get(server).put(this.getUUID(), core);
            this.setSaved(core);
         }
      }
   }

   private void markBattleBegan() {
      if (this.level() instanceof ServerLevel server) {
         WardenVitalsData.get(server).putBattle(this.getUUID(), true);
      }
   }

   private void restoreVitals() {
      if (this.level() instanceof ServerLevel server) {
         WardenVitalsData data = WardenVitalsData.get(server);
         WardenVitalsData.Row row = data.find(this.getUUID());
         Float saved = row == null ? null : row.core();
         if (saved == null) {
            float full = this.realHealth();
            data.put(this.getUUID(), full);
            this.setSaved(full);
            this.writeRawHealth(full);
            ShenlinMod.LOGGER.info("[shenlin] warden vitals born core={} uuid={}", String.format("%.1f", full), this.getUUID());
         } else {
            float value = Mth.clamp(saved, 0.0F, this.getMaxHealth());
            this.setCore(value);
            this.setGood(value);
            this.healthReady = true;
            this.setSaved(value);
            this.syncCore(value);
            this.writeRawHealth(value);
            if (row.battle()) {
               this.entityData.set(DATA_BATTLE, Boolean.TRUE);
               this.skillDone = true;
            }
         }
      }
   }

   public void setHomeCore(BlockPos core) {
      this.homeCore = core;
   }

   private void markSiteExplored() {
      if (this.level() instanceof ServerLevel server) {
         BlockPos core = this.homeCore != null ? this.homeCore : this.blockPosition().below();
         RelicSites.get(server).markExplored(core);
      }
   }

   private void markSiteCleared() {
      if (this.level() instanceof ServerLevel server) {
         BlockPos core = this.homeCore != null ? this.homeCore : this.blockPosition().below();
         RelicSites sites = RelicSites.get(server);
         sites.markExplored(core);
         sites.markClaimed(core);
      }
   }

   private void flattenOffsets() {
      Map<Integer, ?> items;
      try {
         items = itemsById(this.entityData);
      } catch (Throwable t) {
         return;
      }

      if (items != null) {
         try {
            for (Object item : items.values()) {
               if (item != null) {
                  EntityDataAccessor<Float> accessor = floatAccessorOf(item);
                  Field value = itemValueField(item.getClass());
                  if (accessor != null && value != null && !OWN_FLOATS.contains(accessor) && value.get(item) instanceof Float f && f < 0.0F) {
                     this.entityData.set(accessor, 0.0F);
                     this.noteWard("offset", f, 0.0F);
                  }
               }
            }
         } catch (Throwable var9) {
         }
      }
   }

   private static Map<Integer, ?> itemsById(SynchedEntityData data) throws IllegalAccessException {
      Field field = itemsByIdField;
      if (field == null) {
         for (Field f : SynchedEntityData.class.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) && Int2ObjectMap.class.isAssignableFrom(f.getType())) {
               f.setAccessible(true);
               itemsByIdField = f;
               field = f;
               break;
            }
         }

         if (field == null) {
            return null;
         }
      }

      return (Map<Integer, ?>)field.get(data);
   }

   private static Field itemValueField(Class<?> itemClass) {
      Field field = itemValueField;
      if (field != null && field.getDeclaringClass() == itemClass) {
         return field;
      }

      for (Field f : itemClass.getDeclaredFields()) {
         if (!Modifier.isStatic(f.getModifiers()) && f.getType() == Object.class) {
            f.setAccessible(true);
            itemValueField = f;
            return f;
         }
      }

      return null;
   }

   private static EntityDataAccessor<Float> floatAccessorOf(Object item) {
      try {
         if (accessorGetter == null) {
            for (Method m : item.getClass().getMethods()) {
               if (m.getParameterCount() == 0 && EntityDataAccessor.class.isAssignableFrom(m.getReturnType())) {
                  accessorGetter = m;
                  break;
               }
            }
         }

         if (accessorGetter == null) {
            return null;
         }

         Object key = accessorGetter.invoke(item);
         return key instanceof EntityDataAccessor ? (EntityDataAccessor)key : null;
      } catch (Throwable t) {
         return null;
      }
   }

   private void markHurtWindow() {
      long now = nowMs();
      if (this.windowUntilMs > now && this.windowOpenedMs > 0L && now - this.windowOpenedMs < this.invulnMillis() / 2L) {
         ShenlinMod.LOGGER.info("[shenlin] warden window re-cut after {}ms (want {}ms)", now - this.windowOpenedMs, this.invulnMillis());
      }

      long span = this.invulnMillis();
      this.windowOpenedMs = now;
      this.windowUntilMs = now + span;
      this.hurtCooldown = this.invulnTicks();
      this.sealCore(this.coreOf());
   }

   private static long nowMs() {
      return System.nanoTime() / 1000000L;
   }

   private boolean windowOpen() {
      return this.windowUntilMs > nowMs();
   }

   private long invulnMillis() {
      long ms = Math.round(Config.relicWardenInvulnSeconds() * 1000.0);
      if (ms <= 0L) {
         this.noteInvulnZero();
         ms = 1000L;
      }

      return ms;
   }

   private void noteInvulnZero() {
      long now = nowMs();
      if (now - this.lastInvulnZeroNote >= 4000L) {
         this.lastInvulnZeroNote = now;
         ShenlinMod.LOGGER.warn("[shenlin] warden invuln read as zero on {} — fallback 1000ms", Thread.currentThread().getName());
      }
   }

   private int invulnTicks() {
      return Math.max(0, (int)Math.round(Config.relicWardenInvulnSeconds() * 20.0));
   }

   private float wardenAttack() {
      return (float)Config.relicWardenBaseAttack();
   }

   private void dormantTick() {
      this.dormantLerpO = this.dormantLerp;
      int skill = this.skillTick();
      int rise = (Integer)this.entityData.get(DATA_RISE_TICK);
      if (!this.dormantLerpSettled) {
         this.dormantLerp = this.isDormant() ? 1.0F : 0.0F;
         this.dormantLerpSettled = true;
      } else if (skill >= 0) {
         if (skill >= 40) {
            float t = Mth.clamp((skill - 40) / 12.0F, 0.0F, 1.0F);
            this.dormantLerp = 1.0F - riseCurve(t);
         } else {
            this.dormantLerp = 1.0F;
         }
      } else if (rise >= 0) {
         float t = Mth.clamp(rise / 40.0F, 0.0F, 1.0F);
         this.dormantLerp = 1.0F - riseCurve(t);
      } else {
         float target = this.isDormant() ? 1.0F : 0.0F;
         this.dormantLerp = Mth.approach(this.dormantLerp, target, 0.025F);
      }

      if (!this.level().isClientSide) {
         if (rise >= 0) {
            this.entityData.set(DATA_RISE_TICK, rise >= 40 ? -1 : rise + 1);
         }

         boolean hurtAlready = this.coreOf() < 249.999F;
         if (this.isDormant() && (hurtAlready || this.nearestPrey(Config.relicWardenWakeRange()) != null)) {
            this.wake();
         }
      }
   }

   private static float riseCurve(float t) {
      if (t <= 0.0F) {
         return 0.0F;
      } else if (t >= 1.0F) {
         return 1.0F;
      } else if (t < 0.3F) {
         float u = t / 0.3F;
         return 0.06F * (u * u);
      } else if (t < 0.8F) {
         float u = (t - 0.3F) / 0.5F;
         return 0.06F + 0.94F * (u * u * (3.0F - 2.0F * u));
      } else {
         return 1.0F;
      }
   }

   public float dormantLerp(float partialTicks) {
      return Mth.lerp(partialTicks, this.dormantLerpO, this.dormantLerp);
   }

   public boolean isDormant() {
      return (Boolean)this.entityData.get(DATA_DORMANT);
   }

   public boolean battleBegan() {
      return (Boolean)this.entityData.get(DATA_BATTLE);
   }

   public void wake() {
      if (this.isDormant()) {
         this.setDormant(false);
         this.markSiteExplored();
         this.beginSkill();
      }
   }

   private void reviveSkillIfNeeded() {
      if (!this.level().isClientSide && !this.skillDone && !this.isDormant() && !this.isDeadOrDying()) {
         this.beginSkill();
      }
   }

   private void beginSkill() {
      if (!this.level().isClientSide && !this.skillDone) {
         this.skillDone = true;
         this.skillTick = 0;
         this.entityData.set(DATA_SKILL_TICK, 0);
         this.entityData.set(DATA_FACE_ANGLE, packFace(this.yBodyRot));
         this.setNoAi(true);
      }
   }

   private void setDormant(boolean dormant) {
      this.entityData.set(DATA_DORMANT, dormant);
      if (dormant) {
         this.entityData.set(DATA_BATTLE, Boolean.FALSE);
      }

      if (!this.level().isClientSide) {
         this.setNoAi(dormant);
      }
   }

   @Nullable
   private Player nearestPrey(double range) {
      return this.level().getNearestPlayer(this.getX(), this.getY(), this.getZ(), range, e -> e instanceof Player p && !p.isCreative() && !p.isSpectator());
   }

   public void setTarget(@Nullable LivingEntity target) {
      super.setTarget(target);
   }

   private void gaitTick() {
      this.prevGaitPhase = this.gaitPhase;
      double dx = this.getX() - this.xo;
      double dz = this.getZ() - this.zo;
      float moved = (float)Math.sqrt(dx * dx + dz * dz);
      float turned = (float)(Math.abs(Mth.wrapDegrees(this.yBodyRot - this.prevGaitYaw)) * (float) (Math.PI / 180.0) * 0.9);
      this.prevGaitYaw = this.yBodyRot;
      float walk = this.walkAnimation.speed();
      float step = Math.max(Math.max(moved, turned), walk);
      step = Math.min(step, 0.55F);
      this.gaitPhase = this.gaitPhase + step / (float)GAIT_STRIDE * (float) Math.PI;
      if (this.gaitPhase > (float) (Math.PI * 2)) {
         this.gaitPhase -= (float) (Math.PI * 2);
         this.prevGaitPhase -= (float) (Math.PI * 2);
      }

      float want = Mth.clamp(step * 6.0F, 0.0F, 1.0F);
      this.gaitPower = this.gaitPower + (want - this.gaitPower) * 0.35F;
   }

   public float gaitPhase() {
      return this.gaitPhase;
   }

   public float prevGaitPhase() {
      return this.prevGaitPhase;
   }

   public float gaitPower() {
      return this.gaitPower;
   }

   public float gaitPower(float partialTicks) {
      return Mth.lerp(partialTicks, this.gaitPowerO, this.gaitPower);
   }

   public void remove(RemovalReason reason) {
      if (this.level().isClientSide) {
         ClientWardenParts.unregister(this);
         super.remove(reason);
      } else {
         this.clearStun();
         this.releaseChargeVictim();
         if (this.blinkRelocating) {
            super.remove(reason);
         } else if (this.releasing
            || reason.shouldDestroy()
            || reason == RemovalReason.CHANGED_DIMENSION
            || reason == RemovalReason.UNLOADED_TO_CHUNK
            || reason == RemovalReason.UNLOADED_WITH_PLAYER) {
            if (reason.shouldDestroy() && !this.deathBlastDone && !this.retiredByOrder) {
               this.deathBlast();
            }

            this.persistVitals();
            if (reason.shouldDestroy()) {
               if (reason == RemovalReason.KILLED) {
                  this.markSiteCleared();
               } else {
                  this.markSiteExplored();
               }
            }

            if (this.releasing && reason.shouldDestroy() && this.level() instanceof ServerLevel server) {
               WardenVitalsData.get(server).forget(this.getUUID());
            }

            super.remove(reason);
         }
      }
   }

   private void updateParts() {
      this.bodyPart.setOldPosAndRot();
      this.headPart.setOldPosAndRot();
      this.spikePart.setOldPosAndRot();
      this.bodyPart.setPos(this.getX(), this.getY(), this.getZ());
      this.bodyPart.setYRot(this.yBodyRot);
      this.bodyPart.syncBox();
      float yaw = Mth.clamp(Mth.wrapDegrees(this.yHeadRot - this.yBodyRot), -90.0F, 90.0F);
      boolean skill = this.skillTick() >= 0;
      float pitch = skill ? this.skillHeadPitch() : Mth.lerp(this.dormantLerp, Mth.clamp(this.getXRot(), -12.0F, 18.0F), 18.0F);
      float a = yaw * (float) (Math.PI / 180.0);
      float r = 40.0F + 9.0F * Math.abs(Mth.sin(a * 2.0F)) + 8.0F * Math.abs(Mth.sin(pitch * (float) (Math.PI / 180.0)));
      double y = this.getY() + 2.125 - 0.75 - 40.0 * Math.sin(pitch * (float) (Math.PI / 180.0)) / 16.0 - 0.5625 * this.dormantLerp;
      double rho = (r - 40.0F + 40.0 * Math.cos(pitch * (float) (Math.PI / 180.0))) / 16.0;
      double ang = this.headFlying() ? Math.toRadians(this.headFlyAngle()) : Math.toRadians((double)yaw + this.yBodyRot);
      double reach = rho + this.headForward(0.0F);
      this.headPart.setPos(this.getX() - Math.sin(ang) * reach, y + this.headLift(0.0F), this.getZ() + Math.cos(ang) * reach);
      this.headPart.setYRot((float)Math.toDegrees(ang));
      this.headPart.syncBox();
      boolean spikeOut = (Float)this.entityData.get(DATA_CHARGE_SPIKE) > 0.01F;
      double spikeReach = spikeOut ? reach + 4.35 - 2.5 : 0.0;
      this.spikePart
         .setPos(
            this.getX() - Math.sin(ang) * spikeReach,
            spikeOut ? this.getY() + 1.4500000000000002 : this.getY(),
            this.getZ() + Math.cos(ang) * spikeReach
         );
      this.spikePart.setYRot((float)Math.toDegrees(ang));
      this.spikePart.syncBox();
   }

   protected float tickHeadTurn(float pTargetYaw, float pPrevYaw) {
      int rt = this.skillTick();
      if (rt >= 40 && rt <= 60) {
         float away = Mth.wrapDegrees(this.getYRot() - this.yBodyRot);
         ShenlinMod.LOGGER
            .info(
               "[shenlin] 覆苔守护兽 转身 tick={} 沉睡={} 身子角={} 实体朝向={} 头角={} 差角={} 余拍={}",
               new Object[]{rt, this.isDormant(), this.yBodyRot, this.getYRot(), this.entityData.get(DATA_HEAD_ANGLE), away, 52 - rt}
            );
      }

      if ((Integer)this.entityData.get(DATA_DEATH_SHOW) > 0) {
         return pPrevYaw;
      }

      if (this.isDormant()) {
         return pPrevYaw;
      }

      if (rt >= 40) {
         return pPrevYaw;
      }

      if (this.getTarget() == null && !this.chargeActive()) {
         this.bodyControl.clientTick();
      }

      return pPrevYaw;
   }

   public int getMaxHeadYRot() {
      return 75;
   }

   public boolean isMultipartEntity() {
      return true;
   }

   public PartEntity<?>[] getParts() {
      return this.parts == null ? new PartEntity[0] : this.parts;
   }

   public boolean removeWhenFarAway(double distanceToClosestPlayer) {
      return false;
   }

   public boolean fireImmune() {
      return true;
   }

   protected float getSoundVolume() {
      return 1.0F;
   }

   public float getVoicePitch() {
      return 0.7F;
   }

   private void clientSkillStep() {
      int synced = (Integer)this.entityData.get(DATA_SKILL_TICK);
      if (synced < 0) {
         this.clientSkillTick = -1;
         this.clientSkillPrev = -1;
         this.clientSkillDone = false;
      } else if (!this.clientSkillDone) {
         this.clientSkillTick = synced != this.clientSkillPrev ? synced : this.clientSkillTick + 1;
         this.clientSkillPrev = synced;
         if (this.clientSkillTick > 240) {
            this.clientSkillTick = 240;
            this.clientSkillDone = true;
         }
      }
   }

   private void clientHeadStep() {
      int stage = (Integer)this.entityData.get(DATA_HEAD_STAGE);
      int synced = stage > 0 ? (stage & 65535) - 1 : -1;
      if (synced < 0) {
         this.headActLocal = -1;
      } else {
         if (this.headActLocal >= 0 && Math.abs(synced - this.headActLocal) <= 2) {
            this.headActLocal++;
         } else {
            this.headActLocal = synced;
         }
      }
   }

   private void skillStep() {
      if (this.skillTick >= 0) {
         if (this.isDeadOrDying()) {
            this.endSkill();
         } else {
            int tick = this.skillTick;
            if (tick == 11) {
               this.playSoundLoud((SoundEvent)ModSounds.WARDEN_CHARGE.get(), 1.0F);
            }

            if (tick == 22) {
               Vec3 lock = this.aimPoint();
               if (lock == null) {
                  Vec3 fwd = new Vec3(-Math.sin(Math.toRadians(this.yBodyRot)), 0.0, Math.cos(Math.toRadians(this.yBodyRot)));
                  lock = this.corePoint().add(fwd.scale(12.0));
               }

               this.skillAim = lock;
               this.skillOrigin = this.corePoint();
            }

            if (tick == 40) {
               this.smashGround();
               this.fireBeam();
               this.entityData.set(DATA_BATTLE, Boolean.TRUE);
               this.markBattleBegan();
            }

            if (tick >= 40) {
               this.faceAim();
            }

            if (tick == 40) {
               this.launch();
            }

            if (this.skillLaunched) {
               this.skillHang++;
               if (this.skillHang == 6) {
                  this.faceAim();
                  this.flashNow();
               }

               if (this.skillHang > 12) {
                  this.landImpact();
                  this.skillLaunched = false;
                  this.skillAway = Vec3.ZERO;
                  this.setDeltaMovement(Vec3.ZERO);
               } else {
                  this.flightStep();
                  this.hasImpulse = true;
               }
            }

            if (this.headStart <= 0) {
               if (tick >= 52 && this.headAligned(tick)) {
                  this.headStart = tick;
                  this.entityData.set(DATA_HEAD_START, this.headStart);
                  this.headStep(tick);
                  this.logAim("起手", tick);
               }
            } else {
               this.headStep(tick);
               if (tick >= this.headStart + this.headEnd()) {
                  this.endSkill();
                  return;
               }
            }

            this.entityData
               .set(DATA_HEAD_STAGE, this.headStart <= 0 ? -1 : (Integer)this.entityData.get(DATA_HEAD_MODE) << 16 | tick - this.headStart + 1);
            this.skillTick++;
            this.entityData.set(DATA_SKILL_TICK, this.skillTick);
         }
      }
   }

   private float skillHeadAngle(int tick) {
      if (tick <= 22) {
         return 18.0F;
      }

      if (tick <= 36) {
         float u = (tick - 22) / 14.0F;
         return Mth.lerp(Mth.sin(u * (float) Math.PI * 0.5F), 18.0F, -40.0F);
      }

      if (tick <= 40) {
         float u = (tick - 36) / 4.0F;
         return Mth.lerp(u * u, -40.0F, 54.0F);
      }

      if (tick <= 48) {
         return 54.0F;
      }

      float u = Mth.clamp((tick - 48) / 12.0F, 0.0F, 1.0F);
      return Mth.lerp(u, 54.0F, 0.0F);
   }

   public float skillCoreCharge() {
      int tick = this.skillTick();
      if (tick < 0) {
         return 0.0F;
      }

      float u = Mth.clamp((tick - 11) / 29.0F, 0.0F, 1.0F);
      float grow = u * u * (3.0F - 2.0F * u);
      if (tick > 48) {
         grow *= 1.0F - Mth.clamp((tick - 48) / 8.0F, 0.0F, 1.0F);
      }

      return grow;
   }

   @Nullable
   private Vec3 aimPoint() {
      LivingEntity target = this.getTarget();
      if (target == null || !target.isAlive()) {
         target = this.getLastHurtByMob();
      }

      if (target == null || !target.isAlive()) {
         target = this.nearestPrey(32.0);
      }

      return target == null ? null : target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
   }

   public Vec3 corePoint() {
      float ang = (float)Math.toRadians(this.yBodyRot);
      return new Vec3(
         this.getX() - Math.sin(ang) * 0.1875 + Math.cos(ang) * 0.125,
         this.getY() + 3.3125,
         this.getZ() + Math.cos(ang) * 0.1875 + Math.sin(ang) * 0.125
      );
   }

   public Vec3 airEmitPointAt(float partialTick) {
      float ang = (float)Math.toRadians(Mth.rotLerp(partialTick, this.yBodyRotO, this.yBodyRot));
      double x = Mth.lerp(partialTick, this.xo, this.getX());
      double y = Mth.lerp(partialTick, this.yo, this.getY());
      double z = Mth.lerp(partialTick, this.zo, this.getZ());
      return new Vec3(x - Math.sin(ang) * 0.25, y + 4.53125, z + Math.cos(ang) * 0.25);
   }

   public Vec3 airEmitPoint() {
      float ang = (float)Math.toRadians(this.yBodyRot);
      return new Vec3(this.getX() - Math.sin(ang) * 0.25, this.getY() + 4.53125, this.getZ() + Math.cos(ang) * 0.25);
   }

   public Vec3 corePointAt(float partialTick) {
      float ang = (float)Math.toRadians(Mth.rotLerp(partialTick, this.yBodyRotO, this.yBodyRot));
      double x = Mth.lerp(partialTick, this.xo, this.getX());
      double y = Mth.lerp(partialTick, this.yo, this.getY());
      double z = Mth.lerp(partialTick, this.zo, this.getZ());
      return new Vec3(x - Math.sin(ang) * 0.1875 + Math.cos(ang) * 0.125, y + 3.3125, z + Math.cos(ang) * 0.1875 + Math.sin(ang) * 0.125);
   }

   public Vec3 beamOrigin() {
      return this.skillOrigin == null ? this.corePoint() : this.skillOrigin;
   }

   private static float wardenVoice() {
      return (float)(Config.relicWardenMusicRange() / 16.0);
   }

   private void playSoundLoud(SoundEvent sound, float pitch) {
      this.playSoundLoud(sound, pitch, 1.0F);
   }

   private void playSoundLoud(SoundEvent sound, float pitch, float volume) {
      if (sound != null && !this.level().isClientSide) {
         if (this.level() instanceof ServerLevel server) {
            double range = Config.relicWardenMusicRange();
            double rangeSqr = range * range;

            for (ServerPlayer player : server.getPlayers(p -> p.distanceToSqr(this) <= rangeSqr)) {
               player.connection
                  .send(
                     new ClientboundSoundPacket(
                        BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
                        SoundSource.HOSTILE,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        volume,
                        pitch,
                        server.getRandom().nextLong()
                     )
                  );
            }
         }
      }
   }

   private Vec3 headGroundPoint() {
      float ang = (float)Math.toRadians(this.yBodyRot);
      return new Vec3(this.getX() - Math.sin(ang) * 1.7, this.getY(), this.getZ() + Math.cos(ang) * 1.7);
   }

   private void smashGround() {
      if (this.level() instanceof ServerLevel server) {
         this.playSoundLoud((SoundEvent)ModSounds.WARDEN_SMASH.get(), 0.7F);
         Vec3 var4 = this.headGroundPoint();
         this.groundChips(server, var4, 1.5);
         ModPackets.sendShakeSeen(server, this, 12, 2.2F, 1.4F, 0.0F, 0.0F);
         float hit = this.wardenAttack() * 0.1F;
         this.sweepAround(var4, 1.5, hit);
      }
   }

   private void fireBeam() {
      if (this.level() instanceof ServerLevel server && this.skillAim != null) {
         this.playSoundLoud((SoundEvent)ModSounds.WARDEN_SHOOT.get(), 0.75F);
         Vec3 from = this.airEmitPoint();
         Vec3 to = this.beamEndpoint(server, from);
         Vec3 dir = to.subtract(from);
         Vec3 flat = new Vec3(dir.x, 0.0, dir.z);
         if (flat.lengthSqr() > 1.0E-6) {
            flat = flat.normalize();
         } else {
            flat = this.flatForward();
         }

         for (LivingEntity victim : this.beamVictims(from, to)) {
            float amount = (float)(victim.getMaxHealth() * Config.relicWardenBeamRatio());
            if (victim.hurt(this.beamSource(server), amount)) {
               victim.push(flat.x * 0.6, 0.3, flat.z * 0.6);
            }
         }

         this.drawBeam(server, from, to);
      }
   }

   private Vec3 beamEndpoint(ServerLevel server, Vec3 from) {
      Vec3 aim = this.skillAim == null ? from : this.skillAim;
      Vec3 d = aim.subtract(from);
      if (d.lengthSqr() < 1.0E-8) {
         return from;
      }

      Vec3 far = from.add(d.normalize().scale(256.0));
      BlockHitResult hit = server.clip(new ClipContext(from, far, Block.COLLIDER, Fluid.NONE, this));
      return hit.getType() == Type.MISS ? far : hit.getLocation();
   }

   private List<LivingEntity> beamVictims(Vec3 from, Vec3 to) {
      Vec3 dir = to.subtract(from);
      double len2 = dir.lengthSqr();
      if (len2 < 1.0E-4) {
         return List.of();
      }

      AABB box = new AABB(from, to).inflate(1.2);
      List<LivingEntity> hits = new ArrayList<>();

      for (Entity e : this.level().getEntities(this, box, this::isBeamTarget)) {
         if (e instanceof LivingEntity le) {
            Vec3 mid = le.position().add(0.0, le.getBbHeight() * 0.5, 0.0);
            Vec3 rel = mid.subtract(from);
            double along = rel.dot(dir) / len2;
            if (!(along < 0.0) && !(along > 1.0)) {
               Vec3 nearest = from.add(dir.scale(along));
               if (!(nearest.distanceTo(mid) > le.getBbWidth() + 0.7)) {
                  hits.add(le);
               }
            }
         }
      }

      hits.sort((a, b) -> Double.compare(a.distanceToSqr(from), b.distanceToSqr(from)));
      return hits;
   }

   private boolean isBeamTarget(Entity e) {
      return e instanceof LivingEntity le && le != this && le.isAlive() && !le.isSpectator();
   }

   private void drawBeam(ServerLevel server, Vec3 from, Vec3 to) {
      if (!(from.distanceToSqr(to) < 1.0E-6)) {
         double span = from.distanceTo(to);
         Vec3 mid = from.add(to).scale(0.5);
         if (this.pewBeam != null && !this.pewBeam.isRemoved()) {
            this.pewBeam.setBlueStar(true);
            this.pewBeam.aim(from, to, 3, 5, false, 0.62F);
         } else {
            this.pewBeam = new WardenLaserBeamEntity((EntityType<? extends WardenLaserBeamEntity>)ModEntities.WARDEN_LASER.get(), server);
            this.pewBeam.setBlueStar(true);
            this.pewBeam.aim(from, to, 3, 5, false, 0.62F);
            server.addFreshEntity(this.pewBeam);
         }
      }
   }

   private DamageSource beamSource(ServerLevel level) {
      try {
         Holder<DamageType> type = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(BEAM_DAMAGE);
         return new DamageSource(type, this);
      } catch (Throwable t) {
         return this.damageSources().mobAttack(this);
      }
   }

   private void launch() {
      this.skillLaunched = true;
      this.skillHang = 0;
      this.skillAway = this.recoilDirection();
   }

   private void flightStep() {
      int i = Mth.clamp(this.skillHang, 1, 12);
      double speed = 18.0 * (1.0 - (i - 1) / 12.0) / 13.0;
      double uNow = i / 12.0;
      double uPrev = (i - 1) / 12.0;
      double rise = 17.6 * (uNow * (1.0 - uNow) - uPrev * (1.0 - uPrev));
      this.setDeltaMovement(Vec3.ZERO);
      this.move(MoverType.SELF, new Vec3(this.skillAway.x * speed, rise, this.skillAway.z * speed));
   }

   private Vec3 recoilDirection() {
      Vec3 flat;
      if (this.skillAim == null) {
         flat = new Vec3(-Math.sin(Math.toRadians(this.yBodyRot)), 0.0, Math.cos(Math.toRadians(this.yBodyRot)));
      } else {
         Vec3 d = this.skillAim.subtract(this.position());
         flat = new Vec3(d.x, 0.0, d.z);
         if (flat.lengthSqr() < 1.0E-4) {
            flat = new Vec3(0.0, 0.0, 1.0);
         }
      }

      return flat.normalize().scale(-1.0);
   }

   private void groundingGuard() {
      boolean performing = this.chargeTick >= 0 || this.swingTick >= 0 || this.skillTick() >= 0;
      if (performing && !this.skillLaunched && !this.isPassenger() && !this.onGround()) {
         if (!(this.getDeltaMovement().y > 0.05)) {
            this.snapToGround();
         }
      }
   }

   private void snapToGround() {
      Vec3 from = this.position();
      Vec3 to = from.add(0.0, -12.0, 0.0);
      BlockHitResult hit = this.level().clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, this));
      if (hit.getType() != Type.MISS) {
         double groundY = hit.getLocation().y;
         if (this.getY() - groundY > 0.02) {
            this.setPos(this.getX(), groundY, this.getZ());
            this.setDeltaMovement(0.0, 0.0, 0.0);
            this.fallDistance = 0.0F;
         }
      }
   }

   private void landImpact() {
      this.setNoGravity(false);
      this.snapToGround();
      if (this.level() instanceof ServerLevel server) {
         Vec3 var4 = this.position();
         this.playSoundLoud((SoundEvent)ModSounds.WARDEN_SMASH.get(), 0.6F);
         ModPackets.sendShakeSeen(server, this, 14, 2.6F, 1.6F, 0.0F, 0.0F);
         this.landChips(server, var4);
         float hit = this.wardenAttack() * 0.3F;
         this.sweepAround(var4, 2.5, hit);
      }
   }

   private void landChips(ServerLevel server, Vec3 center) {
      RandomSource rnd = this.getRandom();

      for (int i = 0; i < 40; i++) {
         double ang = rnd.nextDouble() * Math.PI * 2.0;
         double r = Math.sqrt(rnd.nextDouble()) * 2.5;
         double x = center.x + Math.cos(ang) * r;
         double z = center.z + Math.sin(ang) * r;
         BlockState state = this.groundStateAt(new Vec3(x, center.y, z));
         if (!state.isAir()) {
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), x, center.y + 0.12, z, 7, 0.5, 0.12, 0.5, 0.16);
         }
      }

      this.groundChips(server, center, 2.5);
   }

   private void sweepAround(Vec3 center, double radius, float amount) {
      if (!(amount <= 0.0F)) {
         AABB box = new AABB(center, center).inflate(radius);

         for (Entity e : this.level().getEntities(this, box, this::isBeamTarget)) {
            if (e instanceof LivingEntity le && le.hurt(this.damageSources().mobAttack(this), amount)) {
               Vec3 away = le.position().subtract(center);
               Vec3 flat = new Vec3(away.x, 0.0, away.z);
               if (flat.lengthSqr() > 1.0E-5) {
                  flat = flat.normalize().scale(0.65);
               }

               le.push(flat.x, 0.34, flat.z);
            }
         }
      }
   }

   private void trueHit(LivingEntity target) {
      double percent = Config.relicWardenTrueDamagePercent();
      if (!(percent <= 0.0)) {
         float before = Executioner.readHealthForRatio(target);
         if (!(before <= 0.0F)) {
            float after = Math.max(0.0F, before - (float)(target.getMaxHealth() * percent));
            Executioner.setHealthSilently(target, after);
            if (!Executioner.reached(target, after <= 0.0F ? 0.0F : after)) {
               ShenlinMod.LOGGER
                  .info(
                     "[shenlin] warden trueHit missed type={} before={} after={} max={} raw={} shown={}",
                     new Object[]{target.getType(), before, after, target.getMaxHealth(), Executioner.syncedHealth(target), target.getHealth()}
                  );
            }

            if (Executioner.reached(target, 0.0F) && target.deathTime <= 0) {
               target.die(this.damageSources().mobAttack(this));
            }
         }
      }
   }

   private void sweepTrue(Vec3 center, double radius) {
      AABB box = new AABB(center, center).inflate(radius);

      for (Entity e : this.level().getEntities(this, box, this::isBeamTarget)) {
         if (e instanceof LivingEntity le) {
            this.trueHit(le);
         }
      }
   }

   private void stun(LivingEntity target) {
      if (target instanceof Mob mob && !this.stunned.containsKey(mob)) {
         int ticks = this.stunTicks();
         if (ticks > 0) {
            this.stunned.put(mob, new int[]{mob.isNoAi() ? 1 : 0, this.tickCount + ticks});
            mob.setNoAi(true);
         }
      }
   }

   private int stunTicks() {
      return (int)Math.round(Config.relicWardenStunSeconds() * 20.0);
   }

   private void stunTick() {
      if (!this.stunned.isEmpty()) {
         Iterator<Entry<Mob, int[]>> it = this.stunned.entrySet().iterator();

         while (it.hasNext()) {
            Entry<Mob, int[]> entry = it.next();
            Mob mob = entry.getKey();
            if (mob != null && !mob.isRemoved()) {
               if (this.tickCount >= entry.getValue()[1]) {
                  mob.setNoAi(entry.getValue()[0] == 1);
                  it.remove();
               }
            } else {
               it.remove();
            }
         }
      }
   }

   private void clearStun() {
      for (Entry<Mob, int[]> entry : this.stunned.entrySet()) {
         Mob mob = entry.getKey();
         if (mob != null && !mob.isRemoved()) {
            mob.setNoAi(entry.getValue()[0] == 1);
         }
      }

      this.stunned.clear();
   }

   private void groundChips(ServerLevel server, Vec3 at, double radius) {
      BlockState state = this.groundStateAt(at);
      double spread = radius * 0.35;
      if (!state.isAir()) {
         server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), at.x, at.y + 0.15, at.z, 36, spread, 0.25, spread, 0.12);
      }

      int puffs = Mth.clamp((int)Math.round(radius * 8.0), 10, 28);
      double ring = Math.max(1.2, radius * 0.95);
      RandomSource smokeRnd = this.getRandom();

      for (int i = 0; i < puffs; i++) {
         double ang = (Math.PI * 2) * i / puffs + (smokeRnd.nextDouble() - 0.5) * 0.14;
         double r = ring * (0.88 + smokeRnd.nextDouble() * 0.24);
         double x = at.x + Math.cos(ang) * r;
         double z = at.z + Math.sin(ang) * r;
         double y = at.y + 0.15 + smokeRnd.nextDouble() * 0.22;
         server.sendParticles(ParticleTypes.SMOKE, x, y, z, 1, 0.0, 0.0, 0.0, 0.012);
      }

      server.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.15, at.z, 1, 0.0, 0.0, 0.0, 0.0);
   }

   private void slamDust(ServerLevel server) {
      Vec3 center = this.slamContactPoint();
      double ringR = 5.0;
      float bigSize = 1.05F;
      int outer = 16;
      int upper = 8;
      int puffs = outer + upper;
      double[] xs = new double[puffs];
      double[] ys = new double[puffs];
      double[] zs = new double[puffs];
      float[] sizes = new float[puffs];

      for (int i = 0; i < puffs; i++) {
         boolean isUpper = i >= outer;
         double angle;
         double ring;
         if (isUpper) {
            angle = (Math.PI * 2) * (i - outer) / upper + Math.PI / upper;
            ring = 5.0 * (1.0 + this.getRandom().nextDouble() * 0.16);
         } else {
            angle = (Math.PI * 2) * i / outer + (this.getRandom().nextDouble() - 0.5) * 0.12;
            ring = 5.0 * (0.94 + this.getRandom().nextDouble() * 0.12);
         }

         xs[i] = center.x + Math.cos(angle) * ring;
         zs[i] = center.z + Math.sin(angle) * ring;
         if (isUpper) {
            ys[i] = center.y + 0.22 + this.getRandom().nextDouble() * 0.16;
         } else {
            ys[i] = center.y + 0.05 + this.getRandom().nextDouble() * 0.18;
         }

         sizes[i] = 1.05F * (0.82F + this.getRandom().nextFloat() * 0.36F) * (isUpper ? 0.72F : 1.0F);
      }

      ModPackets.sendFogBurst(server, xs, ys, zs, sizes, 0.44F, 0.44F, 0.45F, 70, 3, 2.0F);
      FxTrace.fx(
         "slamDust",
         "chargeSlam",
         center,
         System.currentTimeMillis(),
         "center=hornTip-contact ring="
            + FxTrace.f(5.0)
            + " puffs="
            + puffs
            + " (outer/upper "
            + outer
            + "/"
            + upper
            + ") size="
            + FxTrace.f(1.05F)
            + " alpha="
            + FxTrace.f(2.0)
            + " ticks=70 mode=3 rgb="
            + FxTrace.f(0.44F)
            + "/"
            + FxTrace.f(0.44F)
            + "/"
            + FxTrace.f(0.45F)
      );
   }

   private void logSlamGeometry(int h, int slamAt) {
      if (!this.level().isClientSide) {
         Vec3 tip = this.spikePointAt(2.4999999999999996);
         double groundY = Double.NaN;
         boolean hit = false;
         if (this.level() instanceof ServerLevel server) {
            BlockHitResult probe = server.clip(
               new ClipContext(
                  new Vec3(tip.x, tip.y + 0.2, tip.z),
                  new Vec3(tip.x, tip.y - 0.35, tip.z),
                  Block.COLLIDER,
                  Fluid.NONE,
                  this
               )
            );
            if (probe.getType() != Type.MISS) {
               hit = true;
               groundY = probe.getLocation().y;
            }
         }

         ShenlinMod.LOGGER
            .info(
               "[shenlin] 砸地几何 h={} slamAt={} 音效后={}拍 tipY={} 地面Y={} 触地={} ms={}",
               new Object[]{
                  h, slamAt, h - this.slamSoundTick, FxTrace.f(tip.y), Double.isNaN(groundY) ? "-" : FxTrace.f(groundY), hit, System.currentTimeMillis()
               }
            );
      }
   }

   private void slamBurst() {
      if (!this.slamSpawned && !this.level().isClientSide) {
         if (this.level() instanceof ServerLevel server) {
            this.slamSpawned = true;
            this.slamDust(server);
            FxTrace.fx(
               "slamDustBurst",
               "sound-cue+21tick",
               this.position(),
               System.currentTimeMillis(),
               "chargeTick=" + this.chargeTick + " 音效后=" + (this.chargeTick - this.slamSoundTick) + "拍"
            );
         }
      }
   }

   private Vec3 slamContactPoint() {
      Vec3 tip = this.spikePointAt(2.4999999999999996);
      Vec3 from = new Vec3(tip.x, tip.y + 1.0, tip.z);
      Vec3 to = new Vec3(tip.x, tip.y - 6.0, tip.z);
      BlockHitResult hit = this.level().clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, this));
      if (hit.getType() == Type.MISS) {
         return new Vec3(tip.x, this.getY(), tip.z);
      }

      Vec3 p = hit.getLocation();
      return new Vec3(p.x, p.y, p.z);
   }

   private BlockState groundStateAt(Vec3 at) {
      BlockPos below = BlockPos.containing(at.x, at.y - 0.2, at.z);
      BlockState state = this.level().getBlockState(below);
      if (state.isAir()) {
         state = this.level().getBlockState(below.below());
      }

      return state;
   }

   private void headStep(int tick) {
      int h = tick - this.headStart;
      if (h == 0) {
         this.headJudge();
      }

      if (h == 23) {
         this.headStrike();
      }

      if (h == this.headLaunchTick()) {
         this.launchHead();
      }

      if (h == this.headLaunchTick() + 6) {
         this.headBlast();
      }

      if (h == this.headEnd()) {
         this.headRecallStrike();
      }
   }

   private int headLaunchTick() {
      return this.headMode() == 2 ? 5 : 55;
   }

   private int headEnd() {
      return this.headLaunchTick() + 6 + 10 + 33;
   }

   private void faceAim() {
      Vec3 aim = this.aimPoint();
      if (aim != null) {
         Vec3 d = aim.subtract(this.position());
         if (!(d.x * d.x + d.z * d.z < 1.0E-4)) {
            float want = (float)Math.toDegrees(Math.atan2(-d.x, d.z));
            this.entityData.set(DATA_FACE_ANGLE, packFace(want));
            if (this.headFlying()) {
               this.logFace(aim, want);
            } else {
               this.setYRot(want);
               this.logFace(aim, want);
            }
         }
      }
   }

   private static float packFace(float deg) {
      return Mth.wrapDegrees(deg) + 180.0F;
   }

   private float faceAngle() {
      return (Float)this.entityData.get(DATA_FACE_ANGLE) - 180.0F;
   }

   private void logFace(Vec3 aim, float want) {
      int tick = this.skillTick;
      if (tick >= 0 && tick <= 58) {
         ShenlinMod.LOGGER
            .info(
               "[shenlin] 覆苔守护兽 定角 tick={} 想要={} 实体朝向={} 身子角={} 目标={}",
               new Object[]{tick, want, this.getYRot(), this.yBodyRot, String.format("%.1f,%.1f,%.1f", aim.x, aim.y, aim.z)}
            );
      }
   }

   private boolean headAligned(int tick) {
      if (tick >= 112) {
         return true;
      }

      Vec3 aim = this.aimPoint();
      if (aim == null) {
         return true;
      }

      Vec3 d = aim.subtract(this.position());
      if (d.x * d.x + d.z * d.z < 1.0E-4) {
         return true;
      }

      float want = this.flatAngleOf(new Vec3(d.x, 0.0, d.z));
      return Math.abs(Mth.wrapDegrees(want - this.yBodyRot)) <= 30.0F;
   }

   private void headJudge() {
      Vec3 aim = this.aimPoint();
      boolean close = aim != null && this.headDistanceTo(aim) <= 2.5;
      this.entityData.set(DATA_HEAD_MODE, close ? 1 : 2);
      this.logAim("判定", this.skillTick);
   }

   private Vec3 headPoint() {
      Vec3 f = this.flatForward();
      double r = 2.5;
      return new Vec3(this.getX() + f.x * r, this.getY(), this.getZ() + f.z * r);
   }

   private double headDistanceTo(Vec3 at) {
      Vec3 p = this.headPoint();
      double dx = at.x - p.x;
      double dz = at.z - p.z;
      return Math.sqrt(dx * dx + dz * dz);
   }

   private void headStrike() {
      if ((Integer)this.entityData.get(DATA_HEAD_MODE) == 1) {
         Vec3 center = this.headPoint();
         float hit = this.wardenAttack() * 0.2F;
         boolean landed = false;
         AABB box = new AABB(center, center).inflate(2.5);

         for (Entity e : this.level().getEntities(this, box, this::isBeamTarget)) {
            if (e instanceof LivingEntity le && le.hurt(this.damageSources().mobAttack(this), hit)) {
               Vec3 away = le.position().subtract(center);
               Vec3 flat = new Vec3(away.x, 0.0, away.z);
               if (flat.lengthSqr() > 1.0E-5) {
                  flat = flat.normalize();
               } else {
                  flat = this.flatForward();
               }

               le.push(flat.x * 1.2, 0.6, flat.z * 1.2);
               this.stun(le);
               landed = true;
            }
         }

         if (landed) {
            this.playSoundLoud(SoundEvents.ANVIL_LAND, 0.7F);
         }
      }
   }

   private void launchHead() {
      Vec3 forward = this.flatForward();
      double range = 8.0;
      Vec3 aim = this.aimPoint();
      if (aim != null) {
         Vec3 d = aim.subtract(this.position());
         Vec3 flat = new Vec3(d.x, 0.0, d.z);
         if (flat.lengthSqr() > 1.0E-6) {
            forward = flat.normalize();
            range = Math.max(flat.length(), 8.0);
         }
      }

      this.headDir = forward;
      this.headRange = range;
      this.entityData.set(DATA_HEAD_RANGE, (float)range);
      this.entityData.set(DATA_HEAD_ANGLE, this.flatAngleOf(forward));
      this.logAim("放头", this.skillTick);
   }

   private void headBlast() {
      if (this.level() instanceof ServerLevel server) {
         Vec3 var4 = this.headLandPoint();
         this.playSoundLoud(SoundEvents.ANVIL_LAND, 0.6F);
         this.headLandDust(server, var4);
         this.groundChips(server, var4, 2.5);
         float hit = this.wardenAttack() * 1.0F;
         this.sweepAround(var4, 2.5, hit);
         this.sweepTrue(var4, 2.5);
      }
   }

   private void headLandDust(ServerLevel server, Vec3 at) {
      int outer = 14;
      int upper = 7;
      int puffs = outer + upper;
      double ring = Math.max(2.4, 2.875);
      float bigSize = (float)(ring * 0.26);
      double[] xs = new double[puffs];
      double[] ys = new double[puffs];
      double[] zs = new double[puffs];
      float[] radii = new float[puffs];

      for (int i = 0; i < puffs; i++) {
         boolean isUpper = i >= outer;
         double angle;
         double r;
         if (isUpper) {
            angle = (Math.PI * 2) * (i - outer) / upper + Math.PI / upper;
            r = ring * (1.0 + this.getRandom().nextDouble() * 0.16);
         } else {
            angle = (Math.PI * 2) * i / outer + (this.getRandom().nextDouble() - 0.5) * 0.12;
            r = ring * (0.94 + this.getRandom().nextDouble() * 0.12);
         }

         xs[i] = at.x + Math.cos(angle) * r;
         zs[i] = at.z + Math.sin(angle) * r;
         if (isUpper) {
            ys[i] = at.y + 0.2 + this.getRandom().nextDouble() * 0.14;
         } else {
            ys[i] = at.y + 0.04 + this.getRandom().nextDouble() * 0.16;
         }

         radii[i] = bigSize * (0.82F + this.getRandom().nextFloat() * 0.36F) * (isUpper ? 0.72F : 1.0F);
      }

      ModPackets.sendFogBurst(server, xs, ys, zs, radii, 0.45F, 0.45F, 0.44F, 55, 3, 1.55F);
      FxTrace.fx("headLandDust", "head-land", at, System.currentTimeMillis(), "ring=" + FxTrace.f(ring) + " puffs=" + puffs);
   }

   private void headRecallStrike() {
      if (!this.level().isClientSide) {
         Vec3 center = this.position();
         Vec3 fwd = this.flatForward();
         this.playSoundLoud(SoundEvents.ANVIL_LAND, 0.6F);
         float hit = this.wardenAttack() * 0.88F;
         AABB box = new AABB(center, center).inflate(1.5);

         for (Entity e : this.level().getEntities(this, box, this::isBeamTarget)) {
            if (e instanceof LivingEntity le) {
               Vec3 flat = new Vec3(le.getX() - center.x, 0.0, le.getZ() - center.z);
               if (!(flat.lengthSqr() > 1.0E-5) || !(flat.normalize().dot(fwd) <= 0.0)) {
                  this.trueHit(le);
                  if (le.hurt(this.damageSources().mobAttack(this), hit)) {
                     this.stun(le);
                  }
               }
            }
         }
      }
   }

   private Vec3 headLandPoint() {
      Vec3 base = this.position();
      Vec3 dir = this.headDir.lengthSqr() < 1.0E-6 ? this.flatForward() : this.headDir;
      return new Vec3(base.x + dir.x * this.headRange, base.y, base.z + dir.z * this.headRange);
   }

   private void tailTrailTick() {
      if (!this.chargeActive() && !(Boolean)this.entityData.get(DATA_SWING_TAIL)) {
         WardenTailTrail.stop(this.getId());
      } else {
         WardenTailTrail.record(this.getId(), this.tailJetPoint());
      }
   }

   public Vec3 tailJetPoint() {
      Vec3 fwd = this.flatForward();
      return new Vec3(this.getX() - fwd.x * 1.6, this.getY() + 2.3, this.getZ() - fwd.z * 1.6);
   }

   private Vec3 flatForward() {
      float ang = (float)Math.toRadians(this.yBodyRot);
      return new Vec3(-Math.sin(ang), 0.0, Math.cos(ang));
   }

   private float flatAngleOf(Vec3 flat) {
      return (float)Math.toDegrees(Math.atan2(-flat.x, flat.z));
   }

   private void logAim(String stage, int tick) {
      Vec3 aim = this.aimPoint();
      String aimText = aim == null ? "无" : String.format("%.1f,%.1f,%.1f", aim.x, aim.y, aim.z);
      String wantText = "无";
      if (aim != null) {
         Vec3 d = aim.subtract(this.position());
         if (d.x * d.x + d.z * d.z >= 1.0E-4) {
            wantText = String.format("%.1f", this.flatAngleOf(new Vec3(d.x, 0.0, d.z)));
         }
      }

      ShenlinMod.LOGGER
         .info(
            "[shenlin] 覆苔守护兽 {} tick={} 身子角={} 目标角={} 面朝角={} 头角={} 头程={} 分支={} 自身={} 目标={}",
            new Object[]{
               stage,
               tick,
               this.yBodyRot,
               wantText,
               this.faceAngle(),
               this.entityData.get(DATA_HEAD_ANGLE),
               this.entityData.get(DATA_HEAD_RANGE),
               this.headMode(),
               String.format("%.1f,%.1f,%.1f", this.getX(), this.getY(), this.getZ()),
               aimText
            }
         );
   }

   private float headH(float partialTicks) {
      int act = this.headActTick();
      if (act < 0) {
         this.headHeld = -1.0F;
         return -1.0F;
      }

      float h = act + partialTicks;
      if (this.headHeld >= 0.0F && h < this.headHeld && this.headHeld - h < 1.0F) {
         h = this.headHeld;
      }

      this.headHeld = h;
      return h;
   }

   public boolean headRecalling() {
      int act = this.headActTick();
      return act >= 0 && act > this.headLaunchTick() + 6;
   }

   public float hurtGlow(float partialTicks) {
      int span = this.hurtGlowTicks();
      if (span > 0 && this.hurtGlowTick > 0) {
         float total = span;
         float t = total - this.hurtGlowTick + partialTicks;
         float up = Mth.clamp(t / Math.min(1.0F, total), 0.0F, 1.0F);
         float down = Mth.clamp((total - t) / Math.min(3.0F, total), 0.0F, 1.0F);
         return Math.min(up, down);
      } else {
         return 0.0F;
      }
   }

   private int hurtGlowTicks() {
      return this.invulnTicks();
   }

   private void hurtGlowStep() {
      if (this.hurtTime > this.hurtTimePrev) {
         this.hurtGlowTick = this.hurtGlowTicks();
      }

      this.hurtTimePrev = this.hurtTime;
      if (this.hurtGlowTick > 0) {
         this.hurtGlowTick--;
      }
   }

   public float headForward(float partialTicks) {
      float fallen = this.deathShow(partialTicks);
      if (fallen >= 0.0F) {
         return 1.3F * Mth.clamp(fallen / 6.0F, 0.0F, 1.0F);
      } else {
         float h = this.headH(partialTicks);
         if (h < 0.0F) {
            return 0.0F;
         } else {
            int launch = this.headLaunchTick();
            if (h >= launch) {
               float u = Mth.clamp((h - launch) / 6.0F, 0.0F, 1.0F);
               return this.headFlyRange() * u * (1.0F - this.headRecall(h));
            } else if (this.headMode() != 1) {
               return 0.0F;
            } else if (h <= 20.0F) {
               return 0.0F;
            } else if (h <= 23.0F) {
               float u = (h - 20.0F) / 3.0F;
               return 1.5F * (u * u * (3.0F - 2.0F * u));
            } else if (h <= 40.0F) {
               return 1.5F;
            } else if (h <= 50.0F) {
               float u = (h - 40.0F) / 10.0F;
               return 1.5F * (1.0F - u * u * (3.0F - 2.0F * u));
            } else {
               return 0.0F;
            }
         }
      }
   }

   public float headLift(float partialTicks) {
      float fallen = this.deathShow(partialTicks);
      if (fallen >= 0.0F) {
         float u = Mth.clamp(fallen / 6.0F, 0.0F, 1.0F);
         return -1.5F * (u * u);
      } else {
         float h = this.headH(partialTicks);
         if (h < 0.0F) {
            return 0.0F;
         } else {
            int launch = this.headLaunchTick();
            if (h <= launch) {
               return 0.0F;
            } else if (h <= launch + 6) {
               double u = (h - launch) / 6.0;
               return (float)(-2.1 * u + 5.6 * u * (1.0 - u));
            } else {
               return (float)(-2.1 * (1.0 - this.headRecall(h)));
            }
         }
      }
   }

   public boolean headFlying() {
      return this.headActTick() >= this.headLaunchTick();
   }

   public int headActTick() {
      if (this.level().isClientSide) {
         return this.headActLocal;
      }

      int stage = (Integer)this.entityData.get(DATA_HEAD_STAGE);
      if (stage > 0) {
         return (stage & 65535) - 1;
      }

      int from = this.headStartTick();
      if (from <= 0) {
         return -1;
      }

      int tick = this.skillTick();
      return tick < from ? -1 : tick - from;
   }

   public int headStartTick() {
      return (Integer)this.entityData.get(DATA_HEAD_START);
   }

   public float headFlyAngle() {
      return this.headFlyHeld ? this.headFlyLocked : this.getYRot();
   }

   public float headPin() {
      return this.headPin;
   }

   public float headPin(float partialTicks) {
      return Mth.lerp(partialTicks, this.headPinO, this.headPin);
   }

   private float headRecall(float h) {
      int from = this.headLaunchTick() + 6 + 10;
      if (h <= from) {
         return 0.0F;
      }

      float u = Mth.clamp((h - from) / 33.0F, 0.0F, 1.0F);
      return u * u * u;
   }

   private float recallGlow(int h) {
      int from = this.headLaunchTick() + 6;
      return h >= from && h < this.headEnd() ? Mth.clamp((h - from) / 3.0F, 0.0F, 1.0F) : 0.0F;
   }

   public float jointHeadGlow() {
      float swing = this.swingGlow();
      if (swing > 0.0F) {
         return swing;
      } else {
         int h = this.headActTick();
         if (h < 0) {
            return 0.0F;
         } else {
            float burst = this.jointBurst(h);
            if (burst > 0.0F) {
               return burst;
            } else {
               float recall = this.recallGlow(h);
               if (recall > 0.0F) {
                  return recall;
               } else {
                  return this.headMode() != 1 ? 0.0F : this.jointGlow(h);
               }
            }
         }
      }
   }

   public float jointHeadRed() {
      return this.swingGlow() > 0.0F ? 1.0F : 0.0F;
   }

   public float jointBodyGlow() {
      float swing = this.swingGlow();
      if (swing > 0.0F) {
         return swing;
      }

      float h = this.headActTick();
      if (h < 0.0F) {
         return 0.0F;
      }

      float burst = this.jointBurst((int)h);
      if (burst > 0.0F) {
         return burst;
      }

      float recall = this.recallGlow((int)h);
      if (recall > 0.0F) {
         return recall;
      }

      if (this.headMode() != 1) {
         return 0.0F;
      }

      if (h <= 23.0F) {
         return this.jointGlow(h);
      }

      float from = 37.0F;
      return h <= from ? 0.0F : Mth.clamp((h - from) / 3.0F, 0.0F, 1.0F);
   }

   private float jointBurst(int h) {
      int d = h - this.headLaunchTick();
      if (d == -1) {
         return 1.5F;
      } else if (d == 0 || d == 1) {
         return 3.0F;
      } else {
         return d == 2 ? 1.2F : 0.0F;
      }
   }

   public float jointBodyRed() {
      if (this.swingGlow() > 0.0F) {
         return 0.0F;
      } else {
         float h = this.headActTick();
         if (h < 0.0F) {
            return 0.0F;
         } else if (this.recallGlow((int)h) > 0.0F) {
            return 1.0F;
         } else {
            float from = 37.0F;
            if (h <= from) {
               return 0.0F;
            } else if (h <= 40.0F) {
               return Mth.clamp((h - from) / 3.0F, 0.0F, 1.0F);
            } else if (h <= 50.0F) {
               return 1.0F;
            } else {
               return h <= 55.0F ? 1.0F - Mth.clamp((h - 50.0F) / 5.0F, 0.0F, 1.0F) : Mth.clamp((h - 55.0F) / 6.0F, 0.0F, 1.0F);
            }
         }
      }
   }

   private float jointGlow(float h) {
      if (h < 0.0F) {
         return 0.0F;
      } else if (h < 3.0F) {
         return h / 3.0F;
      } else if (h <= 20.0F) {
         return 1.0F;
      } else {
         return h <= 23.0F ? 1.0F - (h - 20.0F) / 3.0F : 0.0F;
      }
   }

   private int headMode() {
      int stage = (Integer)this.entityData.get(DATA_HEAD_STAGE);
      return stage > 0 ? stage >>> 16 : (Integer)this.entityData.get(DATA_HEAD_MODE);
   }

   private float headFlyRange() {
      return (Float)this.entityData.get(DATA_HEAD_RANGE);
   }

   private void endSkill() {
      this.logAim("收场", this.skillTick);
      this.skillTick = -1;
      this.skillAim = null;
      this.skillOrigin = null;
      this.skillLaunched = false;
      this.skillHang = 0;
      this.skillAway = Vec3.ZERO;
      this.headDir = Vec3.ZERO;
      this.headRange = 0.0;
      this.headStart = 0;
      this.entityData.set(DATA_HEAD_MODE, 0);
      this.entityData.set(DATA_HEAD_RANGE, 0.0F);
      this.entityData.set(DATA_HEAD_ANGLE, 0.0F);
      this.entityData.set(DATA_FACE_ANGLE, 180.0F);
      this.entityData.set(DATA_HEAD_START, 0);
      this.entityData.set(DATA_HEAD_STAGE, -1);
      this.clearStun();
      this.entityData.set(DATA_SKILL_TICK, -1);
      this.entityData.set(DATA_RISE_TICK, this.dormantLerp > 0.02F ? 0 : -1);
      if (!this.level().isClientSide) {
         this.setNoAi(false);
      }
   }

   private void airStep() {
      if (!this.isDeadOrDying() && !this.isDormant() && this.skillTick() < 0) {
         if (this.airTick >= 0) {
            LivingEntity target = this.airTarget();
            if (target == null) {
               this.endAir();
            } else {
               this.airTick++;
               this.entityData.set(DATA_AIR_TICK, this.airTick);
               if (this.airTick >= 90) {
                  this.endAir();
               } else {
                  this.getNavigation().stop();
                  this.setXxa(0.0F);
                  this.setZza(0.0F);
                  Vec3 move = this.getDeltaMovement();
                  this.setDeltaMovement(0.0, move.y, 0.0);
                  this.setPos(this.airAnchor.x, this.getY(), this.airAnchor.z);
                  if (this.airTick >= 8) {
                     this.airFire(target);
                  }
               }
            }
         } else if (this.airCooldown > 0) {
            this.airCooldown--;
         } else {
            LivingEntity target = this.airTarget();
            if (target != null && this.chargeTick < 0 && !this.chargeActive()) {
               this.startAir(target);
            }
         }
      } else {
         if (this.airTick >= 0) {
            this.endAir();
         }
      }
   }

   public boolean airActive() {
      return (Integer)this.entityData.get(DATA_AIR_TICK) >= 0;
   }

   public float airGlow() {
      return this.airGlow;
   }

   private double airGap(LivingEntity target) {
      return target.getY() - this.getY();
   }

   @Nullable
   private LivingEntity airTarget() {
      LivingEntity target = this.getTarget();
      if (target == null || !target.isAlive()) {
         target = this.getLastHurtByMob();
      }

      if (target != null && target.isAlive()) {
         return this.airGap(target) <= Config.relicWardenAirHeight() ? null : target;
      } else {
         return null;
      }
   }

   private void startAir(LivingEntity target) {
      this.airTick = 0;
      this.entityData.set(DATA_AIR_TICK, 0);
      this.airAnchor = this.position();
      this.airBeamTarget = target.getId();
      if (this.level() instanceof ServerLevel server) {
         this.playSoundLoud((SoundEvent)ModSounds.WARDEN_AIR_LASER.get(), 1.0F);
         ModPackets.sendSoloBeam(this, this.airBeamTarget, this.level().getGameTime(), 8, 200, 8, target.position(), 0.0, 0.0);
         ShenlinMod.LOGGER.info("[shenlin] 守护兽对空激光 起手 target={} charge={} keep={}", new Object[]{this.airBeamTarget, 8, 200});
      }
   }

   private void endAir() {
      this.airCooldown = 22;
      this.airTick = -1;
      this.entityData.set(DATA_AIR_TICK, -1);
      if (this.airBeamTarget > 0) {
         ModPackets.sendSoloBeam(this, this.airBeamTarget, this.level().getGameTime(), 0, 1, 8, this.position(), 0.0, 0.0);
         this.airBeamTarget = 0;
      }
   }

   private static float airSoundRange() {
      return ((Double)Config.HOVER_CANNON_RAY_SOUND_RANGE.get()).floatValue();
   }

   private static double AIR_BEAM_SOUND_RANGE() {
      return (Double)Config.HOVER_CANNON_SPIN_RAY_SOUND_RANGE.get();
   }

   private static double AIR_BEAM_SOUND_FAR() {
      return (Double)Config.HOVER_CANNON_SPIN_RAY_SOUND_FAR.get();
   }

   private void airFire(LivingEntity target) {
      if (this.level() instanceof ServerLevel server) {
         Vec3 var15 = this.airEmitPoint();
         Vec3 mid = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
         Vec3 dir = mid.subtract(var15);
         if (!(dir.lengthSqr() <= 1.0E-6)) {
            long now = this.level().getGameTime();
            int who = target.getId();
            if (who != this.airBeamTarget) {
               this.airBeamTarget = who;
               ModPackets.sendSoloBeam(this, who, now, 1, 200, 8, target.position(), 0.0, 0.0);
            } else if (this.airTick % 120 == 0) {
               ModPackets.sendSoloBeam(this, who, now, 0, 200, 8, target.position(), 0.0, 0.0);
            }

            float hit = this.wardenAttack() * 0.1F + target.getMaxHealth() * 0.01F;
            if (hit > 0.0F) {
               target.hurt(this.beamSource(server), hit);
            }

            float before = Executioner.readHealthRaw(target);
            if (before > 0.0F) {
               this.trueHitAmount(target, before * 0.00999999F);
            }

            double reach = ModPackets.seenRange() + target.getBbWidth();
            if (this.airTick % 3 == 0) {
               Vec3 edge = this.airHitEdge(target, var15, mid);
               ModPackets.sendShockwave(server, edge, reach, 1.6F, 12, this.facingFromPoint(var15, edge), true, false, 0.55F);
            }

            if (this.airTick >= 12L && this.airTick % 3L == 0L) {
               float power = 0.33333334F * this.airShakeScale();
               Vec3 shakeAt = this.position().add(target.position()).scale(0.5);
               ModPackets.sendShake(server, shakeAt, reach, 6, power, 0.0F, 1.0F, true);
            }
         }
      }
   }

   private Vec3 airHitEdge(LivingEntity target, Vec3 from, Vec3 mid) {
      return target.getBoundingBox().clip(from, mid).orElse(mid);
   }

   private Vec3 facingFromPoint(Vec3 source, Vec3 at) {
      Vec3 v = source.subtract(at);
      return v.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 1.0, 0.0) : v.normalize();
   }

   private float airShakeScale() {
      if (this.airTick < 8) {
         return 0.35F;
      }

      int tail = 82;
      if (this.airTick <= tail) {
         return 1.0F;
      }

      float k = Math.min(1.0F, (float)(this.airTick - tail) / Math.max(1, 8));
      return 1.0F + -0.85F * k;
   }

   public void boltHit(LivingEntity target, Vec3 at, Vec3 dir) {
      if (this.level() instanceof ServerLevel server && target != null && target.isAlive()) {
         float hit = this.wardenAttack() * 0.1F + target.getMaxHealth() * 0.01F;
         if (hit > 0.0F) {
            target.hurt(this.beamSource(server), hit);
         }

         float before = Executioner.readHealthRaw(target);
         if (before > 0.0F) {
            this.trueHitAmount(target, before * 0.00999999F);
         }

         if (dir.lengthSqr() > 1.0E-6) {
            ModPackets.sendShockwave(server, at, Math.max(4.0, 40.0), 0.7F, 11, dir.normalize());
         }
      }
   }

   private void chargeStep() {
      if (this.chargeTick < 0) {
         float horn = (Float)this.entityData.get(DATA_CHARGE_HORN);
         if (horn > 0.0F) {
            this.entityData.set(DATA_CHARGE_HORN, Math.max(0.0F, horn - 0.12F));
         }
      }

      if (this.isNoAi() && this.chargeTick < 0 && !this.isDormant() && this.skillTick < 0 && !this.isDeadOrDying()) {
         this.setNoAi(false);
      }

      if (!this.isDormant() && this.skillTick < 0 && !this.airActive()) {
         if (this.chargeTick < 0 && this.thrustLock > 0) {
            this.thrustLock--;
         }

         if (this.chargeTick < 0 && this.thrustLock <= 0 && !this.isDeadOrDying()) {
            LivingEntity close = this.getTarget();
            if (close == null || !close.isAlive()) {
               close = this.getLastHurtByMob();
            }

            if (close != null
               && close.isAlive()
               && this.inHeadGuard(close)
               && this.airGap(close) <= Config.relicWardenAirHeight()
               && this.swingTick < 0
               && this.swingCooldown <= 0) {
               this.beginHeadSwing(close);
               return;
            }
         }

         if (this.chargeTick < 0) {
            if (this.chargeCooldown > 0) {
               this.chargeCooldown--;
            }

            if (!this.isDeadOrDying()) {
               this.tryCharge();
            }
         } else if (this.isDeadOrDying()) {
            this.endCharge();
         } else {
            int t = this.chargeTick;
            int duck = this.chargeClose ? 12 : 20;
            int out = t - duck;
            if (this.chargeCrashTick >= 0) {
               this.chargeCrashTick++;
               if (this.chargeCrashTick > 14) {
                  this.endCharge();
                  this.chargeCooldown = 0;
               } else {
                  if (this.chargeCrashTick == 13) {
                     this.flashNow();
                     ShenlinMod.LOGGER.info("[shenlin] 守护兽撞墙 收场闪一下 拍={} 断角={}", this.chargeCrashTick, this.spikeBroken);
                  }

                  this.entityData.set(DATA_CHARGE_CRASH, this.chargeCrashTick / 24.0F);
                  Vec3 crashMove = this.getDeltaMovement();
                  if (crashMove.x != 0.0 || crashMove.z != 0.0) {
                     this.setDeltaMovement(0.0, crashMove.y, 0.0);
                  }
               }
            } else if (out <= 0) {
               if (!this.level().isClientSide) {
                  ShenlinMod.LOGGER
                     .info(
                        "[shenlin] 守护兽瞄准 进 拍={} 身子角={} 头角={} 朝向={} 方向角={}",
                        new Object[]{
                           t,
                           this.yBodyRot,
                           this.yHeadRot,
                           this.getYRot(),
                           String.format("%.1f", this.flatAngleOf(this.chargeDir.lengthSqr() < 1.0E-6 ? this.flatForward() : this.chargeDir))
                        }
                     );
               }

               float u = Mth.clamp((float)t / duck, 0.0F, 1.0F);
               this.setChargePose(32.0F * u, u);
               this.entityData.set(DATA_CHARGE_SPIKE, this.spikeBroken ? 0.0F : u);
               this.aimChargeDir(true);
               if (out == 0) {
                  this.lockChargeDir();
                  if (this.chargeClose) {
                     this.chargeThrustStrike();
                  }
               }

               this.advanceCharge(t);
            } else if (this.chargeHitTick >= 0) {
               int h = t - this.chargeHitTick;
               int liftEnd = 15;
               int holdEnd = liftEnd + 4;
               int slamAt = holdEnd + 6;
               int pullAt = slamAt + 55;
               if (h > pullAt + 25) {
                  this.endCharge();
               } else {
                  if (h > slamAt && h <= pullAt) {
                     this.yBodyRotO = this.chargeStuckYaw;
                     this.yBodyRot = this.chargeStuckYaw;
                     this.yHeadRotO = this.chargeStuckYaw;
                     this.yHeadRot = this.chargeStuckYaw;
                     this.setYRot(this.chargeStuckYaw);
                  }

                  this.pinCharge(h > slamAt);
                  if (h <= 3) {
                     this.setChargePose(32.0F, 1.0F);
                     this.setChargeRise(0.0F, 0.0F);
                     this.setChargeLegs(0.0F);
                  } else if (h <= liftEnd) {
                     if (h == 4) {
                        this.playSoundLoud((SoundEvent)ModSounds.WARDEN_GRAB_SLAM.get(), 1.0F);
                        this.slamSoundTick = this.chargeTick;
                        ShenlinMod.LOGGER.info("[shenlin] 砸地音效起点 tick={} ms={}", this.chargeTick, System.currentTimeMillis());
                     }

                     float u = (h - 3) / 12.0F;
                     float s = 1.0F - (1.0F - u) * (1.0F - u) * (1.0F - u);
                     this.setChargePose(Mth.lerp(s, 32.0F, -30.0F), 1.0F);
                     this.setChargeLegs(s);
                     this.setChargeRise(s, 0.0F);
                  } else if (h <= holdEnd) {
                     this.setChargePose(-30.0F, 1.0F);
                     this.setChargeRise(1.0F, 0.0F);
                     this.setChargeLegs(1.0F);
                  } else if (h <= slamAt) {
                     float u = (h - holdEnd) / 6.0F;
                     float drop = u * u * u;
                     this.setChargePose(Mth.lerp(drop, -30.0F, 55.0F), 1.0F);
                     this.setChargeRise(1.0F - drop, 0.0F);
                     this.setChargeLegs(1.0F - drop);
                     this.logSlamGeometry(h, slamAt);
                     if (h == slamAt - 0) {
                        this.slamBurst();
                     }

                     if (h == slamAt) {
                        this.chargeSlam();
                     }
                  } else if (h <= pullAt) {
                     this.setChargePose(55.0F, 1.0F);
                     this.setChargeRise(this.pullTry(h - slamAt), 0.0F);
                     this.setChargeLegs(0.0F);
                  } else {
                     int into = h - pullAt;
                     float u = Mth.clamp(into / 6.0F, 0.0F, 1.0F);
                     float s = Mth.sin(u * (float) Math.PI * 0.5F);
                     int settleFrom = 23;
                     float backRaw = into <= settleFrom ? 0.0F : Mth.clamp((into - settleFrom) / 2.0F, 0.0F, 1.0F);
                     float back = 1.0F - (1.0F - backRaw) * (1.0F - backRaw);
                     float keep = 1.0F - back;
                     this.setChargePose(Mth.lerp(s, 55.0F, -45.0F), keep);
                     this.setChargeRise(s * keep, u * keep);
                     this.setChargeLegs(s * keep);
                     if (into == 1) {
                        this.pullEntered = true;
                        this.chargePullStrike();
                     }

                     if (into == 25) {
                        if (this.level() instanceof ServerLevel landLevel) {
                           this.pullLandFx(landLevel);
                        }

                        this.chargePullRelease();
                     }

                     if (into >= 1 && into <= 23 && this.level() instanceof ServerLevel beamLevel) {
                        this.pullBeam(beamLevel, into == 1);
                     }
                  }

                  this.advanceCharge(t);
               }
            } else if (this.chargeClose) {
               this.endCharge();
            } else if (!this.spikeBroken && out > 26) {
               this.endCharge();
            } else {
               this.limbSmoke();
               boolean blocked = this.rushStep();
               if (!this.spikeBroken) {
                  double overlap = this.spikeOverlap();
                  if (overlap > this.chargePeakOverlap) {
                     this.chargePeakOverlap = overlap;
                  }

                  if (overlap >= 0.3) {
                     this.breakSpike();
                  }
               }

               if (blocked) {
                  this.beginCrash();
               } else {
                  LivingEntity got = this.chargeCatch();
                  if (got != null) {
                     this.chargeOnHit(got, t);
                  }

                  this.advanceCharge(t);
               }
            }
         }
      } else {
         if (this.chargeTick >= 0) {
            this.clearCharge();
         }
      }
   }

   private float pullTry(int stuck) {
      for (int at : CHARGE_TRY_AT) {
         int d = stuck - at;
         if (d >= 0 && d < 12) {
            int rise = 10;
            if (d < rise) {
               return Mth.sin((float)d / rise * (float) Math.PI * 0.5F);
            }

            float u = (d - rise + 1) / 2.0F;
            return Mth.clamp(1.0F - u, 0.0F, 1.0F);
         }
      }

      return 0.0F;
   }

   private void setChargeRise(float rise, float kick) {
      this.entityData.set(DATA_CHARGE_RISE, Mth.clamp(rise, 0.0F, 1.0F));
      this.entityData.set(DATA_CHARGE_KICK, Mth.clamp(kick, 0.0F, 1.0F));
   }

   private void setChargeLegs(float legs) {
      this.entityData.set(DATA_CHARGE_LEGS, Mth.clamp(legs, 0.0F, 1.0F));
   }

   private void advanceCharge(int tick) {
      this.chargeTick = tick + 1;
      this.entityData.set(DATA_CHARGE_TICK, this.chargeTick);
   }

   private void tryCharge() {
      if (this.swingTick < 0) {
         LivingEntity target = this.getTarget();
         if (target == null || !target.isAlive()) {
            target = this.getLastHurtByMob();
         }

         if (target != null && target.isAlive()) {
            if (!(this.airGap(target) > Config.relicWardenAirHeight())) {
               Vec3 d = target.position().subtract(this.position());
               double flat = Math.sqrt(d.x * d.x + d.z * d.z);
               double gap = flat - (this.getBbWidth() + target.getBbWidth()) * 0.5;
               if (!(gap < Config.relicWardenChargeMinDist())) {
                  this.beginCharge(target, false);
               }
            }
         }
      }
   }

   private boolean inHeadGuard(LivingEntity target) {
      Vec3 fwd = this.flatForward();
      Vec3 to = target.position().subtract(this.position());
      double front = to.x * fwd.x + to.z * fwd.z;
      double side = Math.abs(to.x * fwd.z - to.z * fwd.x);
      return front > 0.0 && front <= 3.5 && side <= 3.5;
   }

   private void beginHeadSwing(LivingEntity target) {
      if (this.swingTick < 0 && this.chargeTick < 0 && this.headActTick() < 0) {
         Vec3 d = new Vec3(target.getX() - this.getX(), 0.0, target.getZ() - this.getZ());
         if (d.lengthSqr() > 1.0E-6) {
            float aim = this.flatAngleOf(d.normalize());
            this.setYRot(aim);
            this.yBodyRot = aim;
            this.yHeadRot = aim;
         }

         this.swingTick = 0;
         this.swingBlinked = false;
         this.swingHitMask = 0;
         this.getNavigation().stop();
         this.entityData.set(DATA_SWING_TICK, 0);
         this.playSoundLoud((SoundEvent)ModSounds.WARDEN_CHARGE.get(), 1.0F);
         FxTrace.fx(
            "swingStart",
            "close-meteor",
            this.position(),
            System.currentTimeMillis(),
            "target=" + target.getName().getString() + " reachOut=" + FxTrace.f(2.35F) + " yaw=" + FxTrace.f(66.0) + " slamR=" + FxTrace.f(6.0) + " end=74"
         );
         ShenlinMod.LOGGER.info("[shenlin] 守护兽流星锤 起手 目标={} 总拍={}", target.getName().getString(), 74);
      }
   }

   private boolean swingNearAgain() {
      LivingEntity close = this.getTarget();
      if (close == null || !close.isAlive()) {
         close = this.getLastHurtByMob();
      }

      return close != null && close.isAlive() && !close.isRemoved() && this.inHeadGuard(close) && this.airGap(close) <= Config.relicWardenAirHeight();
   }

   private void holdStillTick() {
      boolean holding = this.swingTick >= 0 || this.chargeTick >= 0 && this.chargeVictim != null;
      if (!holding) {
         this.holdAnchorOn = false;
         if ((Boolean)this.entityData.get(DATA_HOLD_ON)) {
            this.entityData.set(DATA_HOLD_ON, false);
         }
      } else {
         if (!this.holdAnchorOn) {
            this.holdAnchorOn = true;
            this.holdX = this.getX();
            this.holdY = this.getY();
            this.holdZ = this.getZ();
            this.holdYaw = this.getYRot();
            this.entityData.set(DATA_HOLD_ON, true);
            this.entityData.set(DATA_HOLD_YAW, this.holdYaw);
         } else if ((Float)this.entityData.get(DATA_HOLD_YAW) != this.holdYaw) {
            this.entityData.set(DATA_HOLD_YAW, this.holdYaw);
         }

         Vec3 move = this.getDeltaMovement();
         if (move.x != 0.0 || move.z != 0.0) {
            this.setDeltaMovement(0.0, move.y, 0.0);
         }

         this.getNavigation().stop();
         this.getMoveControl().setWantedPosition(this.holdX, this.getY(), this.holdZ, 0.0);
         if (this.getX() != this.holdX || this.getZ() != this.holdZ) {
            this.setPos(this.holdX, this.getY(), this.holdZ);
            Vec3 now = this.getDeltaMovement();
            if (now.x != 0.0 || now.z != 0.0) {
               this.setDeltaMovement(0.0, now.y, 0.0);
            }
         }

         if (this.getYRot() != this.holdYaw) {
            this.setYRot(this.holdYaw);
         }

         if (this.yBodyRot != this.holdYaw) {
            this.yBodyRot = this.holdYaw;
         }

         if (this.yHeadRot != this.holdYaw) {
            this.yHeadRot = this.holdYaw;
         }

         if (this.swingTick >= 0 && this.swingHoldYaw != this.holdYaw) {
            this.swingHoldYaw = this.holdYaw;
         }
      }
   }

   private void swingStep() {
      if (this.swingTick < 0) {
         if (this.swingCooldown > 0) {
            this.swingCooldown--;
         }
      } else if (this.isDeadOrDying()) {
         this.swingTick = -1;
         this.swingBlinked = false;
         this.entityData.set(DATA_SWING_TICK, -1);
         this.entityData.set(DATA_SWING_TAIL, false);
         this.entityData.set(DATA_SWING_GLIDE, false);
      } else {
         int t = this.swingTick;
         if (t == 28) {
            this.playSoundLoud((SoundEvent)ModSounds.WARDEN_SWING_RAISE.get(), 1.0F);
         }

         boolean wantTail = t >= 28;
         if ((Boolean)this.entityData.get(DATA_SWING_TAIL) != wantTail) {
            this.entityData.set(DATA_SWING_TAIL, wantTail);
         }

         if ((Boolean)this.entityData.get(DATA_SWING_GLIDE)) {
            this.entityData.set(DATA_SWING_GLIDE, false);
         }

         this.getNavigation().stop();
         Vec3 v = this.getDeltaMovement();
         if (v.x != 0.0 || v.z != 0.0) {
            this.setDeltaMovement(0.0, v.y, 0.0);
         }

         if (t >= 28) {
            if (t == 28) {
               this.swingHoldYaw = this.getYRot();
            }

            if (this.getYRot() != this.swingHoldYaw) {
               this.setYRot(this.swingHoldYaw);
            }

            if (this.yBodyRot != this.swingHoldYaw) {
               this.yBodyRot = this.swingHoldYaw;
            }

            if (this.yHeadRot != this.swingHoldYaw) {
               this.yHeadRot = this.swingHoldYaw;
            }
         }

         if (t >= 28) {
            this.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 0.0);
         }

         int blinkPhase = this.swingBlinkWait;
         if (blinkPhase != 0) {
            if (blinkPhase == 1) {
               if (this.level() instanceof ServerLevel sv) {
                  this.swingBlinkMove(sv);
               } else {
                  this.swingBlinkWait = 0;
               }

               return;
            }

            this.swingBlinkWait = 0;
            this.swingBlinkTo = null;
         }

         if (t == 54 && !this.swingBlinked) {
            if (!(this.level() instanceof ServerLevel sv)) {
               return;
            }

            Vec3 spot = this.swingBlinkSpot();
            if (spot != null) {
               this.swingBlinked = true;
               this.swingBlinkBegin(sv, spot);
               this.entityData.set(DATA_SWING_TICK, t);
               return;
            }

            this.swingBlinked = true;
         }

         if (t == 10) {
            this.swingHitIds.clear();
         }

         if (t == 19) {
            this.swingHitIds.clear();
         }

         if (t >= 10 && t < 19) {
            this.swingContact(1.0F);
         } else if (t >= 19 && t < 28) {
            this.swingContact(-1.0F);
         }

         this.swingGatherTick();
         if (t == 57) {
            this.swingSlam();
         }

         int next = t + 1;
         if (next >= 74) {
            next = -1;
            this.swingBlinked = false;
            this.entityData.set(DATA_SWING_TAIL, false);
            this.entityData.set(DATA_SWING_GLIDE, false);
            this.swingCooldown = this.swingNearAgain() ? 6 : 45;
            ShenlinMod.LOGGER.info("[shenlin] 守护兽流星锤 收工 共 {} 拍", 74);
         }

         this.swingTick = next;
         this.entityData.set(DATA_SWING_TICK, next);
      }
   }

   private void limbSmoke() {
      if (this.level() instanceof ServerLevel server) {
         Vec3 var23 = this.flatForward();
         Vec3 side = new Vec3(-var23.z, 0.0, var23.x);
         double half = this.getBbWidth() * 0.5;
         double front = half * 0.72;
         double back = -half * 0.78;
         double lateral = half * 0.62;
         double y = this.getY() + 0.1;

         for (int i = 0; i < 4; i++) {
            double f = i < 2 ? front : back;
            double s = (i & 1) == 0 ? lateral : -lateral;
            double x = this.getX() + var23.x * f + side.x * s;
            double z = this.getZ() + var23.z * f + side.z * s;
            server.sendParticles(ParticleTypes.SMOKE, x, y, z, 2, 0.14, 0.06, 0.14, 0.008);
            server.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.05, z, 1, 0.1, 0.03, 0.1, 0.006);
         }
      }
   }

   private Vec3 swingBlinkSpot() {
      LivingEntity target = this.getTarget();
      if (target != null && target.isAlive()) {
         Vec3 away = new Vec3(this.getX() - target.getX(), 0.0, this.getZ() - target.getZ());
         if (away.lengthSqr() < 1.0E-6) {
            float ang = (float)Math.toRadians(target.getYRot());
            away = new Vec3(-Math.sin(ang), 0.0, Math.cos(ang));
         }

         away = away.normalize();
         double tx = target.getX() - away.x * 2.8;
         double tz = target.getZ() - away.z * 2.8;
         return new Vec3(tx, this.settleBlinkY(target.getY(), tx, tz), tz);
      } else {
         return null;
      }
   }

   private void swingBlinkBegin(ServerLevel server, Vec3 to) {
      this.playSoundLoud(SoundEvents.ENDERMAN_TELEPORT, 1.0F);
      this.swingBlinkWait = 1;
      this.swingBlinkTo = to;
      Vec3 here = new Vec3(this.getX(), this.getY() + this.getBbHeight() * 0.55, this.getZ());
      Vec3 there = new Vec3(to.x, to.y + this.getBbHeight() * 0.55, to.z);
      ModPackets.sendBlink(server, here, there, 3);
      ShenlinMod.LOGGER
         .info(
            "[shenlin] 守护兽流星锤 点遮盖 {} 拍：原处={},{},{} 目标身后={},{},{}",
            new Object[]{
               3,
               FxTrace.f((float)this.getX()),
               FxTrace.f((float)this.getY()),
               FxTrace.f((float)this.getZ()),
               FxTrace.f((float)to.x),
               FxTrace.f((float)to.y),
               FxTrace.f((float)to.z)
            }
         );
   }

   private void swingBlinkMove(ServerLevel server) {
      Vec3 to = this.swingBlinkTo;
      if (to == null) {
         this.swingBlinkWait = 0;
      } else {
         LivingEntity target = this.getTarget();
         UUID id = this.getUUID();
         WardenVitalsData data = WardenVitalsData.get(server);
         data.putBoom(id, false);
         data.markPos(id, to.x, to.y, to.z);
         CompoundTag nbt = new CompoundTag();
         this.saveWithoutId(nbt);
         RelicWardenEntity fresh = (RelicWardenEntity)((EntityType)ModEntities.RELIC_WARDEN.get()).create(server);
         if (fresh == null) {
            data.putBoom(id, true);
            data.markPos(id, this.getX(), this.getY(), this.getZ());
            this.swingBlinkWait = 0;
            ShenlinMod.LOGGER.warn("[shenlin] 流星锤闪现 造不出新的，放弃这一次闪现");
         } else {
            fresh.load(nbt);
            fresh.setUUID(id);
            float yaw = target != null ? this.flatAngleOf(new Vec3(target.getX() - to.x, 0.0, target.getZ() - to.z)) : this.getYRot();
            fresh.moveTo(to.x, to.y, to.z, yaw, 0.0F);
            fresh.setYHeadRot(yaw);
            fresh.setDormant(false);
            fresh.setPersistenceRequired();
            fresh.swingTick = 53;
            fresh.entityData.set(DATA_SWING_TICK, 53);
            fresh.entityData.set(DATA_SWING_TAIL, true);
            fresh.swingBlinked = true;
            fresh.swingBlinkWait = 2;
            fresh.swingHoldYaw = yaw;
            if (target != null) {
               fresh.setTarget(target);
            }

            this.blinkRelocating = true;
            this.discard();
            server.addFreshEntityWithPassengers(fresh);
            data.putBoom(id, true);
            data.markPos(id, fresh.getX(), fresh.getY(), fresh.getZ());
            this.blinkArriveFx(server, fresh, target, yaw);
            ShenlinMod.LOGGER
               .info(
                  "[shenlin] 守护兽流星锤 移除并重造完成 落点={},{},{} 朝向={}",
                  new Object[]{FxTrace.f((float)fresh.getX()), FxTrace.f((float)fresh.getY()), FxTrace.f((float)fresh.getZ()), FxTrace.f(yaw)}
               );
         }
      }
   }

   private void blinkArriveFx(ServerLevel server, RelicWardenEntity fresh, LivingEntity target, float yaw) {
      double x = fresh.getX();
      double y = fresh.getY();
      double z = fresh.getZ();
      float startR = (float)(fresh.getBbWidth() * 0.5);
      ModPackets.sendShockwaveBlue(server, new Vec3(x, y + 0.25, z), ModPackets.seenRange(), startR, startR * 3.0F, 14, new Vec3(0.0, 1.0, 0.0));
      double half = fresh.getBbWidth() * 0.5;
      double height = fresh.getBbHeight();

      for (int i = 0; i < 36; i++) {
         double ang = (Math.PI * 2) * i / 36.0;
         double r = half * (0.75 + fresh.getRandom().nextDouble() * 0.55);
         double px = x + Math.cos(ang) * r;
         double pz = z + Math.sin(ang) * r;
         double py = y + 0.15 + fresh.getRandom().nextDouble() * height * 0.85;
         server.sendParticles(ParticleTypes.PORTAL, px, py, pz, 1, 0.06, 0.22, 0.06, 0.12);
      }

      server.sendParticles(ParticleTypes.PORTAL, x, y + height * 0.45, z, 30, half * 0.5, height * 0.35, half * 0.5, 0.1);
      server.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y + height * 0.35, z, 14, half * 0.4, height * 0.25, half * 0.4, 0.05);
      FxTrace.fx("blinkArrive", "close-meteor-blink", new Vec3(x, y, z), System.currentTimeMillis(), "ring=3.4 portal=36+30 vel=0.12");
   }

   private double settleBlinkY(double y, double x, double z) {
      BlockPos base = BlockPos.containing(x, y, z);

      for (int down = 0; down < 5; down++) {
         BlockPos at = base.below(down);
         if (!this.level().getBlockState(at).getCollisionShape(this.level(), at).isEmpty()
            || !this.level().getBlockState(at.above()).getCollisionShape(this.level(), at.above()).isEmpty()) {
            return at.getY() + 1.0;
         }
      }

      return y;
   }

   private void swingContact(float dir) {
      if (this.level() instanceof ServerLevel server) {
         Vec3 var14 = this.headWorldPoint();
         Vec3 fwd = this.flatForward();
         Vec3 bodyAt = new Vec3(this.getX(), var14.y, this.getZ());
         AABB headBox = new AABB(var14, var14).inflate(1.575F).minmax(new AABB(bodyAt, bodyAt).inflate(1.15F));
         Vec3 swing = new Vec3(-fwd.z * dir, 0.0, fwd.x * dir);
         float atk = this.wardenAttack();
         int hits = 0;

         for (LivingEntity le : server.getEntitiesOfClass(LivingEntity.class, headBox.inflate(1.0), e -> e != this && e.isAlive() && this.isBeamTarget(e))) {
            if (le.getBoundingBox().intersects(headBox) && this.swingHitIds.add(le.getId()) && le.hurt(this.damageSources().mobAttack(this), atk * 0.15F)) {
               Vec3 away = new Vec3(le.getX() - var14.x, 0.0, le.getZ() - var14.z);
               away = away.lengthSqr() < 1.0E-6 ? fwd : away.normalize();
               Vec3 push = away.scale(0.55).add(swing.scale(0.45));
               le.setDeltaMovement(push.x, 0.25, push.z);
               le.hurtMarked = true;
               hits++;
            }
         }

         if (hits > 0) {
            ModPackets.sendShakeSeenKick(server, this, 8, 1.1F, 0.0F, 0.1F, 0.0F, (float)swing.x, (float)swing.z, 3.4F);
            this.playSoundLoud(SoundEvents.IRON_GOLEM_HURT, 0.92F);
            ShenlinMod.LOGGER.info("[shenlin] 守护兽流星锤 头扫到 {} 个（方向 {}）", hits, dir > 0.0F ? "右" : "左");
         }
      }
   }

   private void swingGatherTick() {
   }

   private void swingSlam() {
      if (this.level() instanceof ServerLevel server) {
         this.playSoundLoud((SoundEvent)ModSounds.WARDEN_SWING_SLAM.get(), 1.0F);
         Vec3 var12 = this.headWorldPoint();
         Vec3 fwd = this.flatForward();
         float atk = this.wardenAttack();
         int hits = 0;

         for (LivingEntity le : server.getEntitiesOfClass(
            LivingEntity.class, new AABB(this.position(), this.position()).inflate(6.0, 4.0, 6.0), e -> e != this && e.isAlive() && this.isBeamTarget(e)
         )) {
            double flat = Math.sqrt(le.distanceToSqr(var12.x, le.getY(), var12.z));
            if (!(flat > 6.0) && !this.swingBreakShield(le)) {
               boolean hit = le.hurt(this.damageSources().mobAttack(this), atk * 1.0F);
               this.trueHitAmount(le, le.getMaxHealth() * 0.55F);
               if (hit) {
                  Vec3 away = new Vec3(le.getX() - var12.x, 0.0, le.getZ() - var12.z);
                  if (away.lengthSqr() < 1.0E-6) {
                     this.flatForward();
                  } else {
                     away.normalize();
                  }

                  le.setDeltaMovement(fwd.x * 5.0, 0.42, fwd.z * 5.0);
                  le.hurtMarked = true;
               }

               hits++;
            }
         }

         ModPackets.sendShake(server, this.position(), ModPackets.seenRange(), 24, 1.0F, 9.0F, 0.0F, 1.6F);
         this.swingSlamDust(server, var12);
         ShenlinMod.LOGGER.info("[shenlin] 守护兽流星锤 砸地 命中 {} 个（半径 {} 格）", hits, FxTrace.f(6.0));
      }
   }

   private boolean swingBreakShield(LivingEntity target) {
      if (!target.isBlocking()) {
         return false;
      }

      ItemStack held = target.getUseItem();
      if (!(held.getItem() instanceof ShieldItem)) {
         return false;
      }

      target.stopUsingItem();
      if (target instanceof Player player) {
         if (!player.getAbilities().instabuild) {
            held.shrink(1);
         }
      } else {
         held.setCount(0);
      }

      this.playSoundLoud(SoundEvents.SHIELD_BREAK, 1.0F);
      ShenlinMod.LOGGER.info("[shenlin] 守护兽流星锤 砸碎 {} 的盾，这一下不结算伤害", target.getName().getString());
      return true;
   }

   public Vec3 swingStarPoint(float partialTicks) {
      Vec3 f = this.flatForward();
      if (this.headSwinging()) {
         double yaw = Math.toRadians(this.swingHeadYaw(partialTicks));
         f = new Vec3(f.x * Math.cos(yaw) + f.z * Math.sin(yaw), 0.0, f.z * Math.cos(yaw) - f.x * Math.sin(yaw));
      }

      double along = 1.2499999999999998;
      double fw = 3.1 + along;
      double up0 = 0.375;
      double p = Math.toRadians(this.swingHeadPitch(partialTicks));
      double f1 = fw * Math.cos(p) + up0 * Math.sin(p);
      double u1 = up0 * Math.cos(p) - fw * Math.sin(p);
      double up = 2.125 + this.swingHeadLift(partialTicks);
      return new Vec3(this.getX() + f.x * f1, this.getY() + up + u1, this.getZ() + f.z * f1);
   }

   private void swingSlamDust(ServerLevel server, Vec3 at) {
      double groundY = this.getY();
      double ringR = 6.0;
      float bigSize = 1.2F;
      int outer = 18;
      int upper = 9;
      int puffs = outer + upper;
      double[] xs = new double[puffs];
      double[] ys = new double[puffs];
      double[] zs = new double[puffs];
      float[] radii = new float[puffs];

      for (int i = 0; i < puffs; i++) {
         boolean isUpper = i >= outer;
         double angle;
         double ring;
         if (isUpper) {
            angle = (Math.PI * 2) * (i - outer) / upper + Math.PI / upper;
            ring = 6.0 * (1.0 + this.getRandom().nextDouble() * 0.16);
         } else {
            angle = (Math.PI * 2) * i / outer + (this.getRandom().nextDouble() - 0.5) * 0.12;
            ring = 6.0 * (0.94 + this.getRandom().nextDouble() * 0.12);
         }

         xs[i] = at.x + Math.cos(angle) * ring;
         zs[i] = at.z + Math.sin(angle) * ring;
         if (isUpper) {
            ys[i] = groundY + 0.18 + this.getRandom().nextDouble() * 0.14;
         } else {
            ys[i] = groundY + 0.02 + this.getRandom().nextDouble() * 0.14;
         }

         radii[i] = 1.2F * (0.82F + this.getRandom().nextFloat() * 0.36F) * (isUpper ? 0.72F : 1.0F);
      }

      ModPackets.sendFogBurst(server, xs, ys, zs, radii, 0.43F, 0.43F, 0.43F, 70, 3, 1.9F);
      FxTrace.fx(
         "swingSlamDust",
         "close-meteor-slam",
         at,
         System.currentTimeMillis(),
         "ring=" + FxTrace.f(6.0) + " puffs=" + puffs + " (outer/upper " + outer + "/" + upper + ") size=" + FxTrace.f(1.2F)
      );
   }

   public Vec3 headWorldPoint() {
      Vec3 f = this.flatForward();
      double r = 2.5;
      if (this.headSwinging()) {
         double yaw = Math.toRadians(this.swingHeadYaw(1.0F));
         f = new Vec3(f.x * Math.cos(yaw) + f.z * Math.sin(yaw), 0.0, f.z * Math.cos(yaw) - f.x * Math.sin(yaw));
         r += this.swingHeadReach(1.0F);
      }

      return new Vec3(this.getX() + f.x * r, this.getY() + 2.1, this.getZ() + f.z * r);
   }

   private float swingGlow() {
      int t = this.swingTick >= 0 ? this.swingTick : (Integer)this.entityData.get(DATA_SWING_TICK);
      if (t < 0) {
         return 0.0F;
      } else if (t < 10) {
         return t / 10.0F;
      } else {
         int tail = 61;
         if (t <= tail) {
            return t >= 54 && t <= 57 ? 1.0F : 0.9F;
         } else {
            return Mth.clamp(1.0F - (float)(t - tail) / Math.max(1, 74 - tail), 0.0F, 1.0F);
         }
      }
   }

   private float swingAnimTick(float partialTicks) {
      return this.swingAnimCur < 0
         ? ((Integer)this.entityData.get(DATA_SWING_TICK)).intValue()
         : Mth.lerp(partialTicks, this.swingAnimPrev, this.swingAnimCur);
   }

   private static float passCurve(float v) {
      float x = Mth.clamp(v, 0.0F, 1.0F);
      if (x <= 0.72F) {
         float a = x / 0.72F;
         return (float)Math.pow(a, 2.4) * 0.9F;
      } else {
         float b = (x - 0.72F) / 0.27999997F;
         return 0.9F + 0.1F * (float)(1.0 - Math.pow(1.0 - b, 3.0));
      }
   }

   public float swingHeadYaw(float partialTicks) {
      float t = this.swingAnimTick(partialTicks);
      if (t < 0.0F) {
         return 0.0F;
      }

      if (t < 10.0F) {
         float u = t / 10.0F;
         return -66.0F * (u * u * (3.0F - 2.0F * u));
      }

      if (t < 13.0F) {
         float u = (t - 10.0F) / 3.0F;
         return -66.0F + 132.0F * passCurve(u);
      }

      if (t < 19.0F) {
         return 66.0F;
      }

      if (t < 22.0F) {
         float u = (t - 19.0F) / 3.0F;
         return 66.0F - 132.0F * passCurve(u);
      }

      if (t < 28.0F) {
         return -66.0F;
      }

      float u = Mth.clamp((t - 28.0F) / 10.0F, 0.0F, 1.0F);
      return Mth.lerp(u, -66.0F, 0.0F);
   }

   public float swingHeadPitch(float partialTicks) {
      float t = this.swingAnimTick(partialTicks);
      if (t < 0.0F) {
         return 0.0F;
      }

      if (t < 28.0F) {
         return 0.0F;
      }

      if (t < 55.0F) {
         float u = (t - 28.0F) / Math.max(1, 27);
         return Mth.lerp(u * u, 0.0F, -46.0F);
      }

      if (t < 55.0F) {
         return -46.0F;
      }

      float d = t - 55.0F;
      if (d < 3.0F) {
         return Mth.lerp(d / 3.0F, -46.0F, 36.0F);
      }

      float u = Mth.clamp((d - 3.0F) / Math.max(1, 16), 0.0F, 1.0F);
      return Mth.lerp(u, 36.0F, 0.0F);
   }

   public float swingHeadReach(float partialTicks) {
      float t = this.swingAnimTick(partialTicks);
      if (t < 0.0F) {
         return 0.0F;
      }

      if (t < 10.0F) {
         float u = t / 10.0F;
         return 2.35F * (u * u * (3.0F - 2.0F * u));
      }

      if (t <= 57.0F) {
         return 2.35F;
      }

      float u = Mth.clamp((t - 55.0F - 2.0F) / Math.max(1, 16), 0.0F, 1.0F);
      return 2.35F * (1.0F - u);
   }

   public float swingHeadLift(float partialTicks) {
      float t = this.swingAnimTick(partialTicks);
      if (t < 0.0F) {
         return 0.0F;
      } else if (t < 28.0F) {
         float u = Mth.clamp(t / Math.max(1, 28), 0.0F, 1.0F);
         return 0.952F * u;
      } else if (t < 55.0F) {
         float u = (t - 28.0F) / Math.max(1, 27);
         return Mth.lerp(0.28F + 0.72F * u * u, 0.0F, 3.4F);
      } else {
         float d = t - 55.0F;
         return d < 2.0F ? 3.4F * (1.0F - d / 2.0F) : 0.0F;
      }
   }

   private void clientSwingStep() {
      int synced = (Integer)this.entityData.get(DATA_SWING_TICK);
      if (synced == this.swingAnimCur) {
         this.swingAnimPrev = this.swingAnimCur;
      } else {
         this.swingAnimPrev = this.swingAnimCur;
         this.swingAnimCur = synced;
         if (this.swingAnimPrev < 0) {
            this.swingAnimPrev = synced;
         } else {
            if (this.swingAnimPrev < 55 && this.swingAnimCur >= 55) {
               Vec3 at = this.swingStarPoint(1.0F);
               StarEffect.spawn(at.x, at.y, at.z, 5.2F, 2, false, -1, 2.0F);
               ShenlinMod.LOGGER
                  .info(
                     "[shenlin] 守护兽流星锤 头上一颗星 拍={} 处={}",
                     this.swingAnimCur,
                     FxTrace.f(at.x) + "/" + FxTrace.f(at.y) + "/" + FxTrace.f(at.z)
                  );
            }
         }
      }
   }

   public boolean headSwinging() {
      return this.swingAnimCur >= 0 || this.swingTick >= 0;
   }

   public boolean swingGliding() {
      return (Boolean)this.entityData.get(DATA_SWING_GLIDE);
   }

   private void thrustAlone(LivingEntity target) {
      Vec3 d = new Vec3(target.getX() - this.getX(), 0.0, target.getZ() - this.getZ());
      if (d.lengthSqr() > 1.0E-6) {
         this.chargeAimYaw = this.yBodyRot + Mth.wrapDegrees(this.flatAngleOf(d.normalize()) - this.yBodyRot);
         double rad = Math.toRadians(this.chargeAimYaw);
         this.chargeDir = new Vec3(-Math.sin(rad), 0.0, Math.cos(rad));
         this.setYRot(this.chargeAimYaw);
         this.yBodyRot = this.chargeAimYaw;
         this.yHeadRot = this.chargeAimYaw;
      }

      this.chargeThrustStrike();
      if (this.level() instanceof ServerLevel server) {
         for (LivingEntity le : server.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(4.0), e -> e != this && e.isAlive() && this.isBeamTarget(e))) {
            this.thrustPushed.put(le, new int[]{8});
         }
      }

      ShenlinMod.LOGGER.info("[shenlin] 守护兽贴脸那一手 顶飞 {}（范围 {} 格，{} 个）", new Object[]{target.getName().getString(), 4.0, this.thrustPushed.size()});
      this.thrustLock = 25;
      this.beginCharge(target, false);
   }

   private void thrustPushTick() {
      if (!this.thrustPushed.isEmpty()) {
         if (!this.isAlive()) {
            this.thrustPushed.clear();
         } else {
            Iterator<Entry<LivingEntity, int[]>> it = this.thrustPushed.entrySet().iterator();

            while (it.hasNext()) {
               Entry<LivingEntity, int[]> entry = it.next();
               LivingEntity le = entry.getKey();
               int[] left = entry.getValue();
               if (le != null && !le.isRemoved() && le.isAlive()) {
                  Vec3 away = new Vec3(le.getX() - this.getX(), 0.0, le.getZ() - this.getZ());
                  away = away.lengthSqr() < 1.0E-6 ? this.flatForward() : away.normalize();
                  double vy = left[0] == 8 ? 0.45 : le.getDeltaMovement().y;
                  le.setDeltaMovement(away.x * 0.8, vy, away.z * 0.8);
                  le.hurtMarked = true;
                  if (--left[0] <= 0) {
                     it.remove();
                  }
               } else {
                  it.remove();
               }
            }
         }
      }
   }

   private void beginCharge(LivingEntity target, boolean close) {
      this.chargeClose = close;
      this.chargeMadLast = null;
      this.chargeMadTick = -100;
      this.chargeTarget = target;
      this.chargeVictim = null;
      this.chargeHitTick = -1;
      this.chargeDir = Vec3.ZERO;
      this.chargeTick = 0;
      this.pullEntered = false;
      this.pullLandDone = false;
      this.chargeCrashTick = -1;
      this.chargeAimYaw = this.yBodyRot;
      this.entityData.set(DATA_FACE_ANGLE, packFace(this.yBodyRot));
      this.entityData.set(DATA_CHARGE_TICK, 0);
      this.entityData.set(DATA_CHARGE_HEAD, 100.0F);
      this.entityData.set(DATA_CHARGE_HORN, 0.0F);
      this.entityData.set(DATA_CHARGE_VICTIM, -1);
      this.entityData.set(DATA_CHARGE_CRASH, -1.0F);
      this.setNoAi(true);
   }

   private void lockChargeDir() {
      if (this.chargeDir.lengthSqr() < 1.0E-6) {
         float fwdDeg = this.flatAngleOf(this.flatForward());
         this.chargeAimYaw = this.chargeAimYaw + Mth.wrapDegrees(fwdDeg - this.chargeAimYaw);
         double rad = Math.toRadians(this.chargeAimYaw);
         this.chargeDir = new Vec3(-Math.sin(rad), 0.0, Math.cos(rad));
         this.setYRot(this.chargeAimYaw);
         this.yBodyRot = this.chargeAimYaw;
      }

      this.playSoundLoud((SoundEvent)ModSounds.WARDEN_CHARGE.get(), 1.0F);
   }

   private void chargeThrustStrike() {
      this.entityData.set(DATA_CHARGE_THRUST, 1.0F);
      if (this.level() instanceof ServerLevel) {
         Vec3 center = this.headPoint();
         Vec3 fwd = this.flatForward();
         float hit = this.wardenAttack() * 0.2F;
         boolean landed = false;
         AABB box = new AABB(center, center).inflate(3.0);

         for (Entity e : this.level().getEntities(this, box, this::isBeamTarget)) {
            if (e instanceof LivingEntity le) {
               Vec3 away = new Vec3(le.getX() - center.x, 0.0, le.getZ() - center.z);
               boolean onAxis = away.lengthSqr() > 1.0E-5;
               if ((!onAxis || !(away.normalize().dot(fwd) <= 0.0)) && le.hurt(this.damageSources().mobAttack(this), hit)) {
                  Vec3 dir = onAxis ? away : fwd;
                  le.push(dir.x * 2.4, 0.95, dir.z * 2.4);
                  this.stun(le);
                  landed = true;
               }
            }
         }

         if (landed) {
            this.playSoundLoud(SoundEvents.ANVIL_LAND, 0.7F);
         }
      }
   }

   private boolean rushStep() {
      this.aimChargeDir(false);
      if (this.spikeWalled()) {
         if (!this.spikeBroken) {
            this.breakSpike();
         }

         return true;
      } else {
         double fromX = this.getX();
         double fromZ = this.getZ();
         this.setDeltaMovement(Vec3.ZERO);
         this.move(MoverType.SELF, new Vec3(this.chargeDir.x * 1.35, 0.0, this.chargeDir.z * 1.35));
         this.hasImpulse = true;
         double dx = this.getX() - fromX;
         double dz = this.getZ() - fromZ;
         return Math.sqrt(dx * dx + dz * dz) >= 0.4725 ? false : !this.stepUpAhead();
      }
   }

   private void aimChargeDir(boolean align) {
      LivingEntity target = this.chargeTarget;
      if (target != null && target.isAlive()) {
         Vec3 d = new Vec3(target.getX() - this.getX(), 0.0, target.getZ() - this.getZ());
         if (!(d.lengthSqr() < 1.0E-6)) {
            float cur = this.chargeAimYaw;
            float aim = this.flatAngleOf(d.normalize());
            float turn;
            if (align) {
               turn = Mth.wrapDegrees(aim - cur) / this.chargeDuckLeft();
            } else {
               turn = Mth.clamp(Mth.wrapDegrees(aim - cur), -8.0F, 8.0F);
            }

            this.chargeAimYaw = cur + turn;
            float next = this.chargeAimYaw;
            double rad = Math.toRadians(next);
            this.chargeDir = new Vec3(-Math.sin(rad), 0.0, Math.cos(rad));
            this.yRotO = this.yBodyRot;
            this.yBodyRotO = this.yBodyRot;
            this.yHeadRotO = this.yHeadRot;
            this.setYRot(next);
            this.yBodyRot = next;
            this.entityData.set(DATA_FACE_ANGLE, packFace(next));
            this.yHeadRot = next;
            if (!this.level().isClientSide) {
               ShenlinMod.LOGGER
                  .info(
                     "[shenlin] 守护兽瞄准 设 拍={} 目标角={} 原角={} 转={} 设成={} 剩余拍={} {}",
                     new Object[]{
                        this.chargeTick,
                        String.format("%.1f", aim),
                        String.format("%.1f", cur),
                        String.format("%.1f", turn),
                        String.format("%.1f", next),
                        this.chargeDuckLeft(),
                        align ? "蓄势" : "冲"
                     }
                  );
            }
         }
      }
   }

   private int chargeDuckLeft() {
      int duck = this.chargeClose ? 12 : 20;
      return Math.max(1, duck - this.chargeTick + 1);
   }

   private boolean stepUpAhead() {
      double nx = this.getX() + this.chargeDir.x * 1.35;
      double nz = this.getZ() + this.chargeDir.z * 1.35;
      AABB lifted = this.getBoundingBox().move(nx - this.getX(), 1.02, nz - this.getZ());
      if (!this.level().noCollision(this, lifted)) {
         return false;
      }

      BlockPos foot = BlockPos.containing(nx, this.getY() + 0.1, nz);
      if (this.level().getBlockState(foot).getCollisionShape(this.level(), foot).isEmpty()) {
         return false;
      }

      this.setPos(nx, this.getY() + 1.02, nz);
      this.hasImpulse = true;
      return true;
   }

   private boolean spikeWalled() {
      Vec3 ahead = this.spikePoint().add(this.chargeDir.scale(0.8));
      BlockPos base = BlockPos.containing(ahead.x, this.getY() + 0.8, ahead.z);
      BlockPos up = base.above();
      return !this.level().getBlockState(base).getCollisionShape(this.level(), base).isEmpty() && !this.level().getBlockState(up).getCollisionShape(this.level(), up).isEmpty();
   }

   private double spikeOverlap() {
      Vec3 f = this.chargeDir.lengthSqr() < 1.0E-6 ? this.flatForward() : this.chargeDir;
      int total = 0;
      int solid = 0;

      for (int i = 0; i <= 12; i++) {
         double u = i / 12.0;
         double r = 3.1 + 2.4999999999999996 * u;
         double y = this.getY() + Mth.lerp(u, 2.1, 0.8);
         if (this.spikePointSolid(this.getX() + f.x * r, y, this.getZ() + f.z * r)) {
            solid++;
         }

         total++;
      }

      return total <= 0 ? 0.0 : (double)solid / total;
   }

   private boolean spikePointSolid(double x, double y, double z) {
      if (Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)) {
         BlockPos pos = BlockPos.containing(x, y, z);
         VoxelShape shape = this.level().getBlockState(pos).getCollisionShape(this.level(), pos);
         return shape.isEmpty() ? false : shape.bounds().move(pos).contains(x, y, z);
      } else {
         return false;
      }
   }

   private void breakSpike() {
      if (!this.spikeBroken) {
         this.spikeBroken = true;
         this.entityData.set(DATA_CHARGE_SPIKE, 0.0F);
         this.playSoundLoud((SoundEvent)ModSounds.WARDEN_BREAK.get(), 1.0F);
         this.breakHealthCut();
         if (this.level() instanceof ServerLevel server) {
            Vec3 var5 = this.spikePoint();
            RandomSource rnd = this.getRandom();

            for (int i = 0; i < 18; i++) {
               server.sendParticles(
                  ParticleTypes.CRIT,
                  var5.x,
                  var5.y + 1.2,
                  var5.z,
                  1,
                  rnd.nextGaussian() * 0.25,
                  rnd.nextGaussian() * 0.25,
                  rnd.nextGaussian() * 0.25,
                  0.35
               );
            }
         }
      }
   }

   private void breakHealthCut() {
      if (!this.level().isClientSide) {
         double percent = Config.relicWardenBreakHealth();
         if (!(percent <= 0.0)) {
            float current = this.realHealth();
            float cut = (float)(this.getMaxHealth() * percent);
            if (!(cut <= 0.0F) && !(current <= 0.0F)) {
               this.cutVitals(cut);
               ShenlinMod.LOGGER
                  .info(
                     "[shenlin] 守护兽冲撞 断角削血 {} → {}（按 {}%）",
                     new Object[]{String.format("%.1f", current), String.format("%.1f", this.realHealth()), Math.round(percent * 100.0)}
                  );
            }
         }
      }
   }

   private void beginCrash() {
      this.chargeCrashTick = 0;
      this.entityData.set(DATA_CHARGE_CRASH, 0.0F);
      this.crashWall();
   }

   private void flashNow() {
      this.flashSeq++;
      this.entityData.set(DATA_FLASH_SEQ, this.flashSeq);
   }

   private void crashWall() {
      if (this.level() instanceof ServerLevel server) {
         BlockPos hit = this.wallBlockAhead();
         if (hit != null && this.canCrashBreak(server, hit, server.getBlockState(hit))) {
            server.destroyBlock(hit, true, this);
         }

         Vec3 at = hit != null ? Vec3.atCenterOf(hit) : this.position();
         Vec3 axis = this.chargeDir.lengthSqr() < 1.0E-6 ? this.flatForward() : this.chargeDir;
         this.playSoundLoud((SoundEvent)ModSounds.WARDEN_SMASH.get(), 1.0F);
         ModPackets.sendShakeSeen(server, this, 14, 2.6F, 1.6F, 0.0F, 0.0F);
         ModPackets.sendShockwave(server, at, ModPackets.seenRange(), 6.0F, 14, axis, false, true);
      }
   }

   @Nullable
   private BlockPos wallBlockAhead() {
      Vec3 f = this.chargeDir.lengthSqr() < 1.0E-6 ? this.flatForward() : this.chargeDir;

      for (double r = 0.81; r <= 2.16; r += 0.5) {
         double x = this.getX() + f.x * r;
         double z = this.getZ() + f.z * r;

         for (double y = 0.0; y <= 3.0; y++) {
            BlockPos pos = BlockPos.containing(x, this.getY() + y + 0.1, z);
            if (!this.level().getBlockState(pos).getCollisionShape(this.level(), pos).isEmpty()) {
               return pos;
            }
         }
      }

      return null;
   }

   private boolean canCrashBreak(ServerLevel level, BlockPos pos, BlockState state) {
      return state.getDestroySpeed(level, pos) < 0.0F ? false : !state.requiresCorrectToolForDrops() || Items.STONE_PICKAXE.getDefaultInstance().isCorrectToolForDrops(state);
   }

   public float chargeCrash() {
      return (Float)this.entityData.get(DATA_CHARGE_CRASH);
   }

   public float chargeCrash(float partialTicks) {
      return Mth.lerp(partialTicks, this.chargeCrashO, this.chargeCrashLag);
   }

   public float chargeRise() {
      return (Float)this.entityData.get(DATA_CHARGE_RISE);
   }

   public float chargeLegs() {
      return (Float)this.entityData.get(DATA_CHARGE_LEGS);
   }

   public float chargeLegs(float partialTicks) {
      return Mth.lerp(partialTicks, this.chargeLegsO, this.chargeLegsLag);
   }

   public float chargeRise(float partialTicks) {
      return Mth.lerp(partialTicks, this.chargeRiseO, this.chargeRiseLag);
   }

   public float chargeKick() {
      return (Float)this.entityData.get(DATA_CHARGE_KICK);
   }

   public float chargeKick(float partialTicks) {
      return Mth.lerp(partialTicks, this.chargeKickO, this.chargeKickLag);
   }

   @Nullable
   private double chargeFarSqr() {
      double d = Config.relicWardenChargeMinDist();
      return d * d;
   }

   private LivingEntity chargeCatch() {
      Vec3 at = this.headPoint();
      AABB head = new AABB(at, at).inflate(2.8);
      AABB body = this.getBoundingBox().inflate(1.4);
      AABB box = head.minmax(body);
      LivingEntity best = null;
      double bestD = Double.MAX_VALUE;

      for (Entity e : this.level().getEntities(this, box, this::isBeamTarget)) {
         if (e instanceof LivingEntity le) {
            double d = le.position().distanceToSqr(at);
            if (d < bestD) {
               bestD = d;
               best = le;
            }
         }
      }

      ShenlinMod.LOGGER
         .info(
            "[shenlin] 守护兽冲撞 冲 t={} 头壳={} 最近={} 距离={}",
            new Object[]{
               this.chargeTick,
               String.format("%.2f,%.2f,%.2f", at.x, at.y, at.z),
               best == null ? "无" : best.getName().getString(),
               best == null ? -1.0 : Math.sqrt(bestD)
            }
         );
      return best;
   }

   private void chargeOnHit(LivingEntity target, int tick) {
      if (!this.spikeBroken) {
         float atk = this.wardenAttack();
         boolean solid = target.hurt(this.damageSources().mobAttack(this), atk * 0.25F);
         this.trueHitAmount(target, atk * 0.05F);
         if (!target.isAlive()
            && target != this.chargeTarget
            && this.chargeTarget != null
            && this.chargeTarget.isAlive()
            && this.chargeTarget.distanceToSqr(this) > this.chargeFarSqr()) {
            ShenlinMod.LOGGER.info("[shenlin] 守护兽冲撞 撞死拦路的 {} t={}，目标还在远处，不停", target.getName().getString(), tick);
         } else {
            this.chargeVictim = target;
            this.chargeHitTick = tick;
            target.setNoGravity(true);
            this.entityData.set(DATA_CHARGE_VICTIM, target.getId());
            ShenlinMod.LOGGER
               .info("[shenlin] 守护兽冲撞 撞上 {} t={} 物理={} 落账={} 真伤={}", new Object[]{target.getName().getString(), tick, atk * 0.25F, solid, atk * 0.05F});
            this.playSoundLoud((SoundEvent)ModSounds.WARDEN_SMASH.get(), 0.7F);
         }
      } else {
         float atk = this.wardenAttack();
         target.hurt(this.damageSources().mobAttack(this), atk * 0.25F);
         this.playSoundLoud((SoundEvent)ModSounds.WARDEN_SMASH.get(), 0.7F);
         if (target != this.chargeMadLast || tick - this.chargeMadTick >= 12) {
            Vec3 dir = new Vec3(target.getX() - this.getX(), 0.0, target.getZ() - this.getZ());
            dir = dir.lengthSqr() < 1.0E-5 ? this.chargeDir : dir.normalize();
            target.push(dir.x * 2.2, 0.45, dir.z * 2.2);
            this.chargeMadLast = target;
            this.chargeMadTick = tick;
            ShenlinMod.LOGGER.info("[shenlin] 守护兽冲撞 角已断，撞飞 {} t={}", target.getName().getString(), tick);
         }
      }
   }

   private void pinCharge(boolean allowFree) {
      LivingEntity victim = this.chargeVictim;
      if (victim != null && victim.isAlive() && !victim.isRemoved()) {
         if (allowFree && victim.getVehicle() != this) {
            this.releaseChargeVictim();
            this.entityData.set(DATA_CHARGE_VICTIM, -1);
            ShenlinMod.LOGGER.info("[shenlin] 守护兽冲撞 角上那位挣脱下车 {} t={}", victim.getName().getString(), this.chargeTick);
         } else {
            Vec3 at = this.spikeWorldPoint();
            if (victim.getVehicle() != this) {
               victim.setNoGravity(true);
               victim.startRiding(this, true);
            }

            victim.setDeltaMovement(Vec3.ZERO);
            victim.fallDistance = 0.0F;
         }
      } else {
         if (victim != null) {
            this.releaseChargeVictim();
         }
      }
   }

   private Vec3 spikeWorldPoint() {
      return this.spikePointAt(1.2499999999999998);
   }

   public Vec3 spikePointAt(double along) {
      return this.spikePointAt(along, this.chargeHeadPitch(), this.chargeRise());
   }

   public Vec3 spikePointAt(double along, float partialTicks) {
      return this.spikePointAt(along, this.chargeHeadPitch(partialTicks), this.chargeRise(partialTicks));
   }

   private Vec3 spikePointAt(double along, float pitch, float rise) {
      double f = 3.1 + along;
      double u = 0.375;
      double p = Math.toRadians(pitch);
      double f1 = f * Math.cos(p) + u * Math.sin(p);
      double u1 = u * Math.cos(p) - f * Math.sin(p);
      double up = 2.125 + 0.25 * rise;
      Vec3 fwd = this.flatForward();
      return new Vec3(this.getX() + fwd.x * f1, this.getY() + up + u1, this.getZ() + fwd.z * f1);
   }

   public void positionRider(Entity passenger, MoveFunction callback) {
      if (passenger.getId() != (Integer)this.entityData.get(DATA_CHARGE_VICTIM)) {
         super.positionRider(passenger, callback);
      } else {
         Vec3 at = this.spikeWorldPoint();
         callback.accept(passenger, at.x, at.y, at.z);
      }
   }

   public boolean shouldRiderSit() {
      return false;
   }

   private Vec3 spikePoint() {
      Vec3 f = this.flatForward();
      double r = 3.7F;
      return new Vec3(this.getX() + f.x * r, this.getY(), this.getZ() + f.z * r);
   }

   private void releaseChargeVictim() {
      LivingEntity victim = this.chargeVictim;
      if (victim != null) {
         if (victim.getVehicle() == this) {
            victim.stopRiding();
         }

         victim.setNoGravity(false);
         victim.fallDistance = 0.0F;
      }

      this.chargeVictim = null;
   }

   private void chargeSlam() {
      this.chargeStuckYaw = this.yBodyRot;
      if (this.level() instanceof ServerLevel server) {
         Vec3 var8 = this.position();
         this.playSoundLoud((SoundEvent)ModSounds.WARDEN_SMASH.get(), 0.8F);
         ModPackets.sendShake(server, this.position(), ModPackets.seenRange(), 24, 1.0F, 9.0F, 0.0F, 1.6F);
         LivingEntity bound = this.chargeVictim;
         Vec3 rippleFrom = bound != null && bound.isAlive() ? this.spikeWorldPoint().add(0.0, bound.getBbHeight() * 0.25, 0.0) : var8;
         ModPackets.sendScreenRipple(server, this, rippleFrom, 16);
         this.groundChips(server, var8, 2.5);
         LivingEntity victim = this.chargeVictim;
         if (victim != null && victim.isAlive()) {
            victim.hurt(this.damageSources().cramming(), this.wardenAttack() * 0.66F);
            double pct = Config.relicWardenSlamTruePercent();
            if (pct > 0.0) {
               this.trueHitAmount(victim, (float)(victim.getMaxHealth() * pct));
            }
         }
      }
   }

   private void chargePullStrike() {
      if (this.level() instanceof ServerLevel server) {
         LivingEntity var6 = this.chargeVictim;
         this.pulledSpot = var6 != null ? var6.position() : null;
         double heldPct = Config.relicWardenPullTruePercent();
         if (var6 != null && var6.isAlive() && heldPct > 0.0) {
            float amount = (float)(var6.getMaxHealth() * heldPct);
            this.trueHitAmount(var6, amount);
            ShenlinMod.LOGGER.info("[shenlin] 守护兽冲撞 拔出这一记 {} 改血={}（人还挑在角上）", var6.getName().getString(), amount);
         }

         this.playSoundLoud((SoundEvent)ModSounds.WARDEN_EXECUTE_BEAM.get(), 1.0F);
         ModPackets.sendShake(server, this.position(), ModPackets.seenRange(), 24, 1.0F, -9.0F, 0.0F, 1.6F);
      }
   }

   private void chargePullRelease() {
      if (this.level() instanceof ServerLevel server) {
         this.releaseChargeVictim();
         Vec3 var12 = this.headPoint();
         Vec3 fwd = this.flatForward();
         this.landOnFrontLegs(server);
         float hit = this.wardenAttack() * 0.1F;
         AABB box = new AABB(var12, var12).inflate(4.0);

         for (Entity e : server.getEntities(this, box, this::isBeamTarget)) {
            if (e instanceof LivingEntity le) {
               Vec3 away = new Vec3(le.getX() - var12.x, 0.0, le.getZ() - var12.z);
               boolean onAxis = away.lengthSqr() > 1.0E-5;
               if ((!onAxis || !(away.normalize().dot(fwd) <= 0.0)) && le.hurt(this.damageSources().mobAttack(this), hit)) {
                  Vec3 dir = onAxis ? away : fwd;
                  le.push(dir.x * 1.3, 0.7, dir.z * 1.3);
               }
            }
         }
      }
   }

   private void pullLandFx(ServerLevel server) {
      if (this.pullEntered && !this.pullLandDone) {
         this.pullLandDone = true;
         this.pullEntered = false;
         this.landOnFrontLegs(server);
      }
   }

   private void landOnFrontLegs(ServerLevel server) {
      Vec3 fwd = this.flatForward();
      Vec3 side = new Vec3(fwd.z, 0.0, -fwd.x);
      double half = this.getBbWidth() * 0.5;
      double out = half * 0.62;
      double ahead = half * 0.55;

      for (int s = -1; s <= 1; s += 2) {
         Vec3 at = new Vec3(
            this.getX() + fwd.x * ahead + side.x * out * s,
            this.getY() + 0.06,
            this.getZ() + fwd.z * ahead + side.z * out * s
         );
         ModPackets.sendShockwave(server, at, ModPackets.seenRange(), 1.7F, 9, new Vec3(0.0, 1.0, 0.0), false, true);
      }
   }

   private void pullBeam(ServerLevel server, boolean first) {
      Vec3 from = this.spikePointAt(0.0);
      Vec3 dir = this.spikeAimDir();
      Vec3 far = from.add(dir.scale(18.0));
      BlockHitResult hit = server.clip(new ClipContext(from, far, Block.COLLIDER, Fluid.NONE, this));
      Vec3 to = hit.getType() == Type.MISS ? far : hit.getLocation();
      FxTrace.fx(
         "pullBeam",
         first ? "into-first-only-crack" : "into-aim",
         from,
         server.getGameTime(),
         "to="
            + FxTrace.fmt(to)
            + " len="
            + FxTrace.f(from.distanceTo(to))
            + " pitch="
            + FxTrace.f(this.chargeHeadPitch())
            + " beamId="
            + (this.gripBeam == null ? -1 : this.gripBeam.getId())
      );
      if (!(from.distanceToSqr(to) <= 1.0E-4)) {
         if (first) {
            BlockPos ground = BlockPos.containing(from);

            while (ground.getY() > server.getMinBuildHeight() && server.getBlockState(ground).isAir()) {
               ground = ground.below();
            }

            double gx = from.x;
            double gy = ground.getY() + 1.0;
            double gz = from.z;
            Vec3 look = this.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0.0, look.z);
            if (flat.lengthSqr() < 1.0E-6) {
               flat = new Vec3(0.0, 0.0, 1.0);
            }

            flat = flat.normalize();
            double tilt = Math.toRadians(60.0);
            Vec3 push = flat.scale(Math.cos(tilt)).add(new Vec3(0.0, Math.sin(tilt), 0.0));

            for (ServerPlayer watcher : server.players()) {
               if (watcher.distanceToSqr(gx, gy, gz) < 9216.0) {
                  ModPackets.sendRelicDoor(watcher, 2, gx, gy, gz, push.x, push.y, push.z, 60);
               }
            }
         } else {
            int rgb = 16740894;
            if (this.chargeHeadPitch() > -30.0F) {
               FxTrace.fx("pullBeam", "skip-too-low", from, server.getGameTime(), "pitch=" + FxTrace.f(this.chargeHeadPitch()) + " 门槛=" + FxTrace.f(-30.0));
            } else {
               if (!first && this.gripBeam != null && !this.gripBeam.isRemoved()) {
                  this.gripBeam.aim(from, to, 1, -1, true, 2, 2.2F);
               } else {
                  if (this.gripBeam != null) {
                     this.gripBeam.discard();
                  }

                  this.gripBeam = new WardenLaserBeamEntity((EntityType<? extends WardenLaserBeamEntity>)ModEntities.WARDEN_LASER.get(), server);
                  this.gripBeam.aim(from, to, 1, -1, true, 2, 2.2F);
                  this.gripBeam.setSpray(true);
                  server.addFreshEntity(this.gripBeam);
                  FxTrace.fx(
                     "gripBeamCreate",
                     "spawn",
                     from,
                     server.getGameTime(),
                     "id="
                        + this.gripBeam.getId()
                        + " to="
                        + FxTrace.fmt(to)
                        + " len="
                        + FxTrace.f(from.distanceTo(to))
                        + " width="
                        + FxTrace.f(2.2F)
                        + " spray=true"
                  );
               }
            }
         }
      }
   }

   public Vec3 spikeAimDir() {
      return this.aimDirOfPitch(this.chargeHeadPitch());
   }

   public Vec3 spikeAimDir(float partialTicks) {
      return this.aimDirOfPitch(this.chargeHeadPitch(partialTicks));
   }

   private Vec3 aimDirOfPitch(float pitch) {
      double a = Math.toRadians(pitch);
      Vec3 fwd = this.flatForward();
      double flat = Math.cos(a);
      Vec3 dir = new Vec3(fwd.x * flat, -Math.sin(a), fwd.z * flat);
      return dir.lengthSqr() < 1.0E-6 ? fwd : dir.normalize();
   }

   private void trueHitAmount(LivingEntity target, float amount) {
      if (!(amount <= 0.0F)) {
         if (target instanceof Player player) {
            if (!player.isCreative() && !player.isSpectator()) {
               float keep = (float)Mth.clamp(HeartGuard.damageCut(player), 0.0, 1.0);
               float cut = amount * (1.0F - keep);
               if (!(cut <= 0.0F)) {
                  float health = trueHealthOf(player);
                  if (!(health <= 0.0F)) {
                     player.setHealth(Math.max(0.0F, health - cut));
                     if (player.getHealth() <= 0.0F && !player.isDeadOrDying()) {
                        player.die(this.damageSources().mobAttack(this));
                     }
                  }
               }
            }
         } else {
            float before = Executioner.readHealthRaw(target);
            if (!(before <= 0.0F)) {
               Executioner.setHealthSilently(target, Math.max(0.0F, before - amount));
               if (Executioner.reached(target, 0.0F) && target.deathTime <= 0) {
                  target.die(this.damageSources().mobAttack(this));
               }
            }
         }
      }
   }

   private void setChargePose(float pitch, float horn) {
      this.entityData.set(DATA_CHARGE_HEAD, pitch + 100.0F);
      this.entityData.set(DATA_CHARGE_HORN, Mth.clamp(horn, 0.0F, 1.0F));
   }

   private void endCharge() {
      this.snapToGround();
      this.chargeCooldown = this.chargeClose ? 20 : 80;
      if (!this.spikeBroken && this.chargePeakOverlap > 0.05) {
         ShenlinMod.LOGGER
            .info("[shenlin] 守护兽冲撞 角没断，这一趟最高扎进去 {}（门槛 {}）", String.format("%.0f%%", this.chargePeakOverlap * 100.0), String.format("%.0f%%", 30.0));
      }

      this.clearCharge();
      if (!this.level().isClientSide && !this.isDeadOrDying()) {
         this.setNoAi(false);
      }
   }

   private void clearCharge() {
      this.snapToGround();
      if (this.level() instanceof ServerLevel server) {
         this.pullLandFx(server);
      }

      this.chargeTick = -1;
      this.chargeTarget = null;
      this.releaseChargeVictim();
      this.chargeHitTick = -1;
      this.chargeDir = Vec3.ZERO;
      this.chargeClose = false;
      this.chargeMadLast = null;
      this.chargeMadTick = -100;
      this.chargePeakOverlap = 0.0;
      this.chargeCrashTick = -1;
      this.slamSpawned = false;
      this.entityData.set(DATA_CHARGE_TICK, -1);
      this.entityData.set(DATA_CHARGE_HEAD, 100.0F);
      this.entityData.set(DATA_CHARGE_VICTIM, -1);
      this.entityData.set(DATA_CHARGE_SPIKE, 0.0F);
      this.entityData.set(DATA_CHARGE_THRUST, 0.0F);
      this.entityData.set(DATA_CHARGE_CRASH, -1.0F);
      this.entityData.set(DATA_CHARGE_RISE, 0.0F);
      this.entityData.set(DATA_CHARGE_LEGS, 0.0F);
      this.entityData.set(DATA_CHARGE_KICK, 0.0F);
   }

   public boolean chargeActive() {
      return (Integer)this.entityData.get(DATA_CHARGE_TICK) >= 0;
   }

   public float chargeHeadPitch() {
      return (Float)this.entityData.get(DATA_CHARGE_HEAD) - 100.0F;
   }

   public float chargeHeadPitch(float partialTicks) {
      return Mth.lerp(partialTicks, this.chargeHeadPitchO, this.chargeHeadPitchLag);
   }

   public float hornGlow() {
      return (Float)this.entityData.get(DATA_CHARGE_HORN);
   }

   public int chargeVictimId() {
      return (Integer)this.entityData.get(DATA_CHARGE_VICTIM);
   }

   public float chargeSpike() {
      return (Float)this.entityData.get(DATA_CHARGE_SPIKE);
   }

   public float chargeSpike(float partialTicks) {
      return Mth.lerp(partialTicks, this.chargeSpikeO, this.chargeSpikeLag);
   }

   public float chargeThrust() {
      return (Float)this.entityData.get(DATA_CHARGE_THRUST);
   }

   public float chargeThrust(float partialTicks) {
      return Mth.lerp(partialTicks, this.chargeThrustO, this.chargeThrustLag);
   }

   public int skillTick() {
      return this.level().isClientSide ? this.clientSkillTick : (Integer)this.entityData.get(DATA_SKILL_TICK);
   }

   public float skillHeadPitch() {
      int tick = this.skillTick();
      return tick < 0 ? 0.0F : this.skillHeadAngle(tick);
   }

   public float skillHeadPitch(float partialTicks) {
      return Mth.lerp(partialTicks, this.skillHeadPitchO, this.skillHeadPitchLag);
   }

   public float eyeGlow() {
      int tick = this.skillTick();
      if (tick < 0) {
         tick = (Integer)this.entityData.get(DATA_SKILL_TICK);
      }

      if (tick >= 0) {
         if (tick >= 22) {
            return 1.0F;
         } else {
            return tick < 11 ? 0.0F : (tick - 11) / 11.0F;
         }
      } else {
         return this.isDormant() ? 0.0F : 1.0F;
      }
   }

   public float muzzleGlow() {
      float charge = Math.max(this.skillCoreCharge(), this.airGlow());
      return 0.35F + 0.65F * charge;
   }

   public float lampWhite() {
      return 0.55F * this.skillCoreCharge();
   }

   private float realHealth() {
      if (!this.healthReady) {
         this.healthReady = true;
         float full = this.getMaxHealth();
         this.setCore(full);
         this.setGood(full);
         this.syncCore(full);
      }

      float core = this.coreOf();
      if (Float.isNaN(core) || core < 0.0F) {
         core = 0.0F;
      }

      return Mth.clamp(core, 0.0F, this.getMaxHealth());
   }

   private void syncCore(float value) {
      float shadow = toShadow(value);
      if (Math.abs((Float)this.entityData.get(DATA_CORE) - shadow) > 1.0E-4F) {
         this.entityData.set(DATA_CORE, shadow);
      }
   }

   private void sealCore(float value) {
      this.vitals().seal = Float.floatToRawIntBits(value) ^ SEAL_KEY;
   }

   private float sealedCore() {
      int sealed = this.vitals().seal;
      return sealed == 0 ? 0.0F : Float.intBitsToFloat(sealed ^ SEAL_KEY);
   }

   private void noteWard(String where, float was, float now) {
      long stamp = System.currentTimeMillis();
      if (stamp - this.lastWardNote >= 4000L) {
         this.lastWardNote = stamp;
         ShenlinMod.LOGGER
            .warn("[shenlin] warden vitals touched: {} {} -> {} (core {} cool {})", new Object[]{where, was, now, this.coreOf(), this.hurtCooldown});
      }
   }

   public float getHealth() {
      if (!this.born) {
         return 250.0F;
      } else {
         return this.level().isClientSide ? fromShadow((Float)this.entityData.get(DATA_CORE)) : this.realHealth();
      }
   }

   public float displayHealth() {
      return this.level().isClientSide ? fromShadow((Float)this.entityData.get(DATA_CORE)) : this.coreOf();
   }

   public void ownDamage(float amount) {
      if (!this.level().isClientSide && amount > 0.0F) {
         this.writeRawHealth(Math.max(0.0F, this.rawHealth() - amount));
         if (this.settleVitals()) {
            this.writeRawHealth(this.coreOf());
         }
      }
   }

   public void ownCutPercent(float cutRatio) {
      if (!this.level().isClientSide) {
         float r = Mth.clamp(cutRatio, 0.0F, 1.0F);
         float base = this.realHealth();
         ShenlinMod.LOGGER
            .info(
               "[shenlin] warden cut-percent base={} ratio={} cut={}",
               new Object[]{String.format("%.2f", base), String.format("%.3f", r), String.format("%.2f", base * r)}
            );
         this.cutVitals(base * r);
      }
   }

   private void cutVitals(float amount) {
      if (!this.level().isClientSide && amount > 0.0F) {
         if (!this.windowOpen()) {
            float from = this.coreOf();
            ShenlinMod.LOGGER
               .info(
                  "[shenlin] warden own-cut in amount={} from={} win={}ms",
                  new Object[]{String.format("%.2f", amount), String.format("%.2f", from), Math.max(0L, this.windowUntilMs - nowMs())}
               );
            float next = Math.max(0.0F, from - amount);
            this.setGood(from);
            this.setCore(next);
            if (next <= 0.0F) {
               this.ownZero = true;
            }

            this.markHurtWindow();
            this.writeTable(next);
            this.noteOwn(amount, next);
         }
      }
   }

   public void setHealth(float health) {
      if (this.born) {
         if (this.level().isClientSide) {
            this.writeRawHealth(health);
         } else {
            float want = Float.isNaN(health) ? this.coreOf() : Mth.clamp(health, 0.0F, 250.0F);
            this.writeRawHealth(want);
            boolean done = this.settleVitals();
            if (done) {
               float table = this.coreOf();
               if (Math.abs(this.rawHealth() - table) > 1.0E-4F) {
                  this.writeRawHealth(table);
               }
            }
         }
      }
   }

   private void noteFoldBounce(float amount) {
      long stamp = System.currentTimeMillis();
      if (stamp - this.lastFoldNote >= 4000L) {
         this.lastFoldNote = stamp;
         ShenlinMod.LOGGER
            .warn(
               "[shenlin] warden fold bounced: 想抹 {} 现在 {} (自窗 {} 原版无敌 {} 上一刀 {})",
               new Object[]{amount, this.coreOf(), this.hurtCooldown, this.invulnerableTime, this.f_20898_}
            );
      }
   }

   public void heal(float amount) {
   }

   public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
      super.onSyncedDataUpdated(key);
      if (!this.level().isClientSide && DATA_CORE.equals(key)) {
         if (this.windowOpen()) {
            float want = toShadow(this.coreOf());
            if (Math.abs((Float)this.entityData.get(DATA_CORE) - want) > 1.0E-4F) {
               this.entityData.set(DATA_CORE, want);
            }
         }
      }
   }

   private void noteHit(DamageSource source, float amount, String verdict) {
      long now = System.currentTimeMillis();
      if (now - this.lastHitNote >= 300L) {
         this.lastHitNote = now;
         ShenlinMod.LOGGER
            .info(
               "[shenlin] warden hit src={} amt={} cool={} core={} {}",
               new Object[]{source.getMsgId(), String.format("%.2f", amount), this.hurtCooldown, String.format("%.1f", this.coreOf()), verdict}
            );
      }
   }

   private void noteOwn(float cut, float left) {
      long now = System.currentTimeMillis();
      if (now - this.lastOwnNote >= 50L) {
         this.lastOwnNote = now;
         ShenlinMod.LOGGER.info("[shenlin] warden own cut={} left={}", String.format("%.1f", cut), String.format("%.1f", left));
      }
   }

   public boolean hurt(DamageSource source, float amount) {
      if (this.level().isClientSide) {
         return false;
      } else {
         this.wake();
         if (this.windowOpen()) {
            this.noteHit(source, amount, "blocked");
            return false;
         } else {
            this.invulnerableTime = 0;
            boolean hit = super.hurt(source, amount);
            this.invulnerableTime = 0;
            this.settleVitals();
            return hit;
         }
      }
   }

   protected void actuallyHurt(DamageSource source, float amount) {
      if (this.level().isClientSide) {
         super.actuallyHurt(source, amount);
      } else if (this.windowOpen()) {
         this.noteHit(source, amount, "blocked-direct");
      } else {
         float capped = Math.min(amount, this.damageCap());
         this.noteHit(source, capped, "pass");
         super.actuallyHurt(source, capped);
      }
   }

   public void die(DamageSource source) {
      float table = this.coreOf();
      float cap = this.damageCap();
      boolean ownDeath = this.releasing || this.ownZero && table <= 0.0F;
      if (!ownDeath) {
         if (table > cap) {
            this.refuseDeath(source, "core above cap");
         } else if (table > 0.0F) {
            this.refuseDeath(source, "core above zero");
         } else {
            this.refuseDeath(source, "core zero without own mark");
         }
      } else {
         this.releasing = true;
         this.ownZero = true;
         this.markSiteCleared();
         ShenlinMod.LOGGER
            .info(
               "[shenlin] warden died core={} good={} own={} rel={} win={}ms src={} by={}",
               new Object[]{
                  String.format("%.2f", this.coreOf()),
                  String.format("%.2f", this.goodOf()),
                  this.ownZero,
                  this.releasing,
                  Math.max(0L, this.windowUntilMs - nowMs()),
                  source.getMsgId(),
                  source.getEntity() == null ? "-" : source.getEntity().getClass().getSimpleName()
               }
            );
         this.endSkill();
         this.setNoAi(true);
         this.setTarget(null);
         this.setCore(0.0F);
         this.setGood(0.0F);
         this.sealCore(0.0F);
         this.healthReady = true;
         this.syncCore(0.0F);
         this.healthGate++;
         if (!this.level().isClientSide) {
            this.spawnAtLocation(new ItemStack((ItemLike)ModItems.GUARDIAN_CORE.get()));
         }

         try {
            super.die(source);
         } finally {
            this.healthGate--;
         }
      }
   }

   private static String caller() {
      StackTraceElement[] st = Thread.currentThread().getStackTrace();
      StringBuilder sb = new StringBuilder();

      for (int i = 3; i < Math.min(st.length, 6); i++) {
         if (sb.length() > 0) {
            sb.append(" <- ");
         }

         String cls = st[i].getClassName();
         sb.append(cls.substring(cls.lastIndexOf(46) + 1)).append('#').append(st[i].getMethodName());
      }

      return sb.toString();
   }

   private void refuseDeath(DamageSource source, String why) {
      long now = System.currentTimeMillis();
      if (now - this.lastDieRefused >= 4000L) {
         this.lastDieRefused = now;
         ShenlinMod.LOGGER
            .info(
               "[shenlin] warden die refused core={} good={} own={} rel={} win={}ms {} src={} by={}",
               new Object[]{
                  String.format("%.2f", this.coreOf()),
                  String.format("%.2f", this.goodOf()),
                  this.ownZero,
                  this.releasing,
                  Math.max(0L, this.windowUntilMs - nowMs()),
                  why,
                  source.getMsgId(),
                  source.getEntity() == null ? "-" : source.getEntity().getClass().getSimpleName()
               }
            );
      }
   }

   private void relicAftermath() {
      if (!this.aftermathDone && this.level() instanceof ServerLevel server) {
         this.aftermathDone = true;
         aftermath(server, this.position());
      }
   }

   private static void aftermath(ServerLevel server, Vec3 at) {
      BlockPos core = RelicLocator.nearestCore(server, BlockPos.containing(at), 50).orElse(null);
      if (core != null) {
         for (BlockPos rel : AFTERMATH_CLEAR) {
            BlockPos cell = RelicLocator.relToWorld(core, rel);
            if (server.getChunkSource().getChunkNow(cell.getX() >> 4, cell.getZ() >> 4) != null) {
               server.destroyBlock(cell, false);
            }
         }

         BlockPos slab = RelicLocator.relToWorld(core, AFTERMATH_SLAB);
         if (server.getChunkSource().getChunkNow(slab.getX() >> 4, slab.getZ() >> 4) != null) {
            server.setBlock(slab, Blocks.MOSSY_STONE_BRICK_SLAB.defaultBlockState(), 3);
         }
      }
   }

   public void kill() {
      if (!this.level().isClientSide) {
         if (!(this.coreOf() > 0.0F)) {
            this.beginDeathShow();
         }
      }
   }

   public void retire() {
      if (!this.level().isClientSide && !this.isRemoved()) {
         this.retiredByOrder = true;
         this.releasing = true;
         if (this.level() instanceof ServerLevel server) {
            WardenVitalsData data = WardenVitalsData.get(server);
            data.putBoom(this.getUUID(), false);
            data.forget(this.getUUID());
         }

         this.discard();
      }
   }

   public void beginDeathShow() {
      if (!this.level().isClientSide && !this.isRemoved() && !this.releasing) {
         this.releasing = true;
         this.ownZero = true;
         this.setCore(0.0F);
         this.setGood(0.0F);
         this.writeTable(0.0F);
         this.die(this.damageSources().genericKill());
      }
   }

   protected void tickDeath() {
      if (!this.level().isClientSide) {
         if (!this.releasing && !this.ownZero) {
            this.realHealth();
            if (this.coreOf() > 0.0F) {
               return;
            }
         }

         int t = (Integer)this.entityData.get(DATA_DEATH_SHOW) + 1;
         this.entityData.set(DATA_DEATH_SHOW, t);
         if (t == 1) {
            this.releasing = true;
            this.deathPush();
         }

         if (t >= 55) {
            if (!this.blasted) {
               this.blasted = true;
               this.deathBlast();
            }

            this.releasing = true;
            this.remove(RemovalReason.KILLED);
         }
      }
   }

   public float deathShow(float partialTicks) {
      int t = (Integer)this.entityData.get(DATA_DEATH_SHOW);
      return t <= 0 ? -1.0F : t - 1 + partialTicks;
   }

   private void deathPush() {
      if (this.level() instanceof ServerLevel server) {
         double want = Config.relicWardenDeathKnockback();
         if (!(want <= 0.0)) {
            double speed = want / 10.0;

            for (Player player : server.getPlayers(p -> !p.isSpectator() && p.isAlive() && p.distanceToSqr(this) <= 256.0)) {
               Vec3 away = player.position().subtract(this.position());
               double dx = away.x;
               double dz = away.z;
               double flat = Math.sqrt(dx * dx + dz * dz);
               if (flat < 1.0E-4) {
                  dx = 1.0;
                  dz = 0.0;
                  flat = 1.0;
               }

               player.push(dx / flat * speed, 0.42, dz / flat * speed);
               player.hurtMarked = true;
            }
         }
      }
   }

   private void deathBlast() {
      this.deathBlast(null);
   }

   private void deathBlast(Vec3 at) {
      if (this.level() instanceof ServerLevel server) {
         if (blast(server, this, at != null ? at : this.position(), true)) {
            this.deathBlastDone = true;
         }

         WardenVitalsData.get(server).forget(this.getUUID());
      }
   }

   public static void blastAt(ServerLevel server, Vec3 at, boolean withAftermath) {
      blast(server, null, at, withAftermath);
   }

   public void blastHereAndLeave(ServerLevel server, Vec3 at) {
      if (blast(server, this, at, true)) {
         this.deathBlastDone = true;
      }

      WardenVitalsData.get(server).forget(this.getUUID());
      this.retiredByOrder = true;
      this.releasing = true;
      this.discard();
   }

   private static boolean blast(ServerLevel server, RelicWardenEntity self, Vec3 at, boolean withAftermath) {
      Vec3 spot = at;
      if (!(Math.abs(spot.x) > 3.0E7) && !(Math.abs(spot.y) > 3.0E7) && !(Math.abs(spot.z) > 3.0E7)) {
         server.playSound(null, spot.x, spot.y, spot.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 7.0F, 1.0F);
         server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, spot.x, spot.y + 0.6, spot.z, 1, 0.0, 0.0, 0.0, 0.0);
         deathBurst(server, spot);
         deathDust(server, spot);
         double mobRadius = Config.relicWardenDeathBlastMobRadius();

         for (LivingEntity le : server.getEntitiesOfClass(
            LivingEntity.class,
            new AABB(spot, spot).inflate(mobRadius),
            e -> e != self && e.isAlive() && e.distanceToSqr(spot.x, spot.y, spot.z) <= mobRadius * mobRadius && !(e instanceof Player)
         )) {
            deathTake(server, le);
         }

         double playerRadius = Config.relicWardenDeathBlastPlayerRadius();

         for (Player player : server.getEntitiesOfClass(
            Player.class,
            new AABB(spot, spot).inflate(playerRadius),
            p -> !p.isCreative() && !p.isSpectator() && !p.isDeadOrDying() && p.distanceToSqr(spot.x, spot.y, spot.z) <= playerRadius * playerRadius
         )) {
            deathTakePlayer(server, player);
         }

         ModPackets.sendShockWarp(server, spot, 150.0, 72.0F, 66.0F, 300, 2050, 1);
         if (withAftermath) {
            try {
               aftermath(server, spot);
            } catch (Throwable t) {
               ShenlinMod.LOGGER.warn("[shenlin] relic warden aftermath failed: {}", t.toString());
            }
         }

         return true;
      } else {
         ShenlinMod.LOGGER
            .warn("[shenlin] death blast skipped: position already void at={}", String.format("%.1f,%.1f,%.1f", spot.x, spot.y, spot.z));
         return false;
      }
   }

   private static void deathDust(ServerLevel server, Vec3 at) {
      int n = 96;
      double[] xs = new double[n];
      double[] ys = new double[n];
      double[] zs = new double[n];
      float[] sizes = new float[n];

      for (int i = 0; i < n; i++) {
         double angle = (Math.PI * 2) * i / n + (server.random.nextDouble() - 0.5) * 0.7;
         double ring = 19.5 * Math.sqrt(server.random.nextDouble());
         xs[i] = at.x + Math.cos(angle) * ring;
         zs[i] = at.z + Math.sin(angle) * ring;
         double up = 1.0 - 0.55 * (ring / 19.5);
         ys[i] = at.y + 0.6 + server.random.nextDouble() * 10.2 * up;
         sizes[i] = 2.15F * (0.65F + server.random.nextFloat() * 0.85F);
      }

      ModPackets.sendFogBurst(server, xs, ys, zs, sizes, 0.47F, 0.45F, 0.43F, 200, 4, 1.9F);
      FxTrace.fx(
         "deathDust",
         "deathBlast",
         at,
         System.currentTimeMillis(),
         "puffs="
            + n
            + " spread="
            + FxTrace.f(19.5)
            + " size="
            + FxTrace.f(2.15F)
            + " alpha="
            + FxTrace.f(1.9F)
            + " ticks=200 mode=4 rgb="
            + FxTrace.f(0.47F)
            + "/"
            + FxTrace.f(0.45F)
            + "/"
            + FxTrace.f(0.43F)
      );
   }

   private static void deathBurst(ServerLevel server, Vec3 at) {
      double cx = at.x;
      double cy = at.y + 1.4;
      double cz = at.z;
      server.sendParticles(ParticleTypes.SOUL, cx, cy, cz, 60, 1.1, 1.1, 1.1, 0.16);
      server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, cx, cy, cz, 50, 1.1, 1.1, 1.1, 0.42);
      server.sendParticles(ParticleTypes.FLAME, cx, cy, cz, 90, 1.1, 1.1, 1.1, 0.55);
   }

   private static void deathTake(ServerLevel server, LivingEntity target) {
      boolean wrote = Executioner.setHealthSilently(target, 0.0F);
      boolean down = Executioner.reached(target, 0.0F);
      if (down && target.deathTime <= 0) {
         target.die(server.damageSources().generic());
      } else if (!down) {
         ShenlinMod.LOGGER
            .info("[shenlin] death blast left {} standing (hp {}, wrote {})", new Object[]{target.getName().getString(), target.getHealth(), wrote});
      }
   }

   private static void deathTakePlayer(ServerLevel server, Player player) {
      float keep = (float)Mth.clamp(HeartGuard.damageCut(player), 0.0, 1.0);
      if (!(keep >= 1.0F)) {
         float health = trueHealthOf(player);
         if (!(health <= 0.0F)) {
            player.setHealth(health * keep);
            ShenlinMod.LOGGER
               .info("[shenlin] death blast on player {} keep={} hp={} left={}", new Object[]{player.getName().getString(), keep, health, trueHealthOf(player)});
            if (keep <= 0.0F && !player.isDeadOrDying()) {
               player.die(server.damageSources().generic());
            }
         }
      }
   }

   private static float trueHealthOf(LivingEntity living) {
      return Executioner.rawHealth(living);
   }

   public boolean isDeadOrDying() {
      if (this.level().isClientSide) {
         return super.isDeadOrDying();
      } else {
         return !this.releasing && this.realHealth() > 0.0F ? false : this.realHealth() <= 0.0F;
      }
   }

   public double getAttributeValue(Attribute attribute) {
      return attribute == Attributes.MAX_HEALTH ? 250.0 : super.getAttributeValue(attribute);
   }

   public void onAddedToWorld() {
      super.onAddedToWorld();
      if (!this.level().isClientSide && this.level() instanceof ServerLevel server) {
         this.restoreVitals();
         boolean adopted = false;
         if (this.homeCore == null) {
            this.homeCore = RelicLocator.nearestCore(server, this.blockPosition(), 24).orElse(null);
            adopted = this.homeCore != null;
         }

         if (adopted) {
            RelicSites.get(server).markKnown(this.homeCore);
         }

         if (this.homeCore != null && !this.homeCore.equals(this.blockPosition().below())) {
            RelicSites.get(server).forget(this.blockPosition().below());
         }
      }
   }

   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.contains("Health")) {
         float saved = Mth.clamp(tag.getFloat("Health"), 0.0F, this.getMaxHealth());
         this.setCore(saved);
         this.setGood(saved);
         this.sealCore(saved);
         this.healthReady = true;
         this.syncCore(saved);
      }

      this.setDormant(!tag.contains("Dormant") || tag.getBoolean("Dormant"));
      this.spikeBroken = tag.getBoolean("SpikeBroken");
      if (tag.contains("SiteCore")) {
         this.homeCore = BlockPos.of(tag.getLong("SiteCore"));
      }
   }

   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putFloat("Health", this.realHealth());
      tag.putBoolean("Dormant", this.isDormant());
      tag.putBoolean("SpikeBroken", this.spikeBroken);
      if (this.homeCore != null) {
         tag.putLong("SiteCore", this.homeCore.asLong());
      }
   }

   static {
      WardenVitalsKeeper.start();
   }

   private class ChaseGoal extends Goal {
      private int repathAt;
      private double lastX;
      private double lastZ;
      private int stuck;

      private ChaseGoal() {
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean canUse() {
         LivingEntity target = RelicWardenEntity.this.getTarget();
         return RelicWardenEntity.this.swingTick >= 0 ? false : target != null && target.isAlive() && !RelicWardenEntity.this.isDormant();
      }

      public boolean canContinueToUse() {
         return this.canUse();
      }

      public void tick() {
         LivingEntity target = RelicWardenEntity.this.getTarget();
         if (target != null) {
            RelicWardenEntity mob = RelicWardenEntity.this;
            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            Vec3 to = new Vec3(target.getX() - mob.getX(), 0.0, target.getZ() - mob.getZ());
            if (to.lengthSqr() > 1.0E-6) {
               float want = mob.flatAngleOf(to.normalize());
               float headAway = Mth.wrapDegrees(want - mob.yHeadRot);
               mob.yHeadRot = mob.yHeadRot + Mth.clamp(headAway, -10.0F, 10.0F);
               if (!mob.getNavigation().isInProgress() && mob.getX() == mob.xo && mob.getZ() == mob.zo) {
                  float bodyAway = Mth.wrapDegrees(want - mob.yBodyRot);
                  mob.yBodyRot = mob.yBodyRot + Mth.clamp(bodyAway, -10.0F, 10.0F);
               } else {
                  mob.yBodyRot = mob.getYRot();
               }
            }

            double moved = Math.abs(mob.getX() - this.lastX) + Math.abs(mob.getZ() - this.lastZ);
            this.lastX = mob.getX();
            this.lastZ = mob.getZ();
            if (moved < 0.01) {
               this.stuck++;
            } else {
               this.stuck = 0;
            }

            boolean straight = mob.getSensing().hasLineOfSight(target) && !mob.horizontalCollision && this.stuck < 12;
            if (straight) {
               mob.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 3.225);
            } else if (mob.tickCount >= this.repathAt) {
               this.repathAt = mob.tickCount + 6;
               mob.getNavigation().moveTo(target, 3.225);
            }
         }
      }

      public void stop() {
         RelicWardenEntity.this.getNavigation().stop();
      }
   }

   private class IdleLookGoal extends LookAtPlayerGoal {
      private IdleLookGoal() {
         super(RelicWardenEntity.this, Player.class, 32.0F);
      }

      public boolean canUse() {
         return RelicWardenEntity.this.getTarget() == null && super.canUse();
      }

      public boolean canContinueToUse() {
         return RelicWardenEntity.this.getTarget() == null && super.canContinueToUse();
      }
   }

   private class StrollGoal extends WaterAvoidingRandomStrollGoal {
      private StrollGoal() {
         super(RelicWardenEntity.this, 0.6);
      }

      private boolean performing() {
         return RelicWardenEntity.this.swingTick >= 0 || RelicWardenEntity.this.chargeTick >= 0 || RelicWardenEntity.this.skillTick >= 0;
      }

      public boolean canUse() {
         return !this.performing() && super.canUse();
      }

      public boolean canContinueToUse() {
         return !this.performing() && super.canContinueToUse();
      }
   }

   private static final class Vitals {
      volatile int core = Float.floatToRawIntBits(250.0F) ^ RelicWardenEntity.SEAL_KEY;
      volatile int good = Float.floatToRawIntBits(-1.0F) ^ RelicWardenEntity.SEAL_KEY;
      volatile int seal;
      volatile int saved = Float.floatToRawIntBits(-1.0F) ^ RelicWardenEntity.SEAL_KEY;
      final ReentrantLock lock = new ReentrantLock();
   }
}

