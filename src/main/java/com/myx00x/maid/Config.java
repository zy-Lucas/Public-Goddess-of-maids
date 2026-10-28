package com.myx00x.maid;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

public class Config {
   public static final ForgeConfigSpec SPEC;
   public static final IntValue TRUE_HITS_EXECUTION;
   public static final IntValue FINISH_SWINGS;
   public static final DoubleValue FINAL_SLASH_HIT_RATIO;
   public static final IntValue FINAL_SLASH_HIT_COUNT;
   public static final BooleanValue SLASH_PULSE_ENABLED;
   public static final IntValue SLASH_PULSE_INTERVAL_TICKS;
   public static final DoubleValue SLASH_PULSE_CUT_RATIO;
   public static final IntValue SLASH_PULSE_EXEC_COUNT;
   public static final DoubleValue SLASH_PULSE_RANGE;
   public static final BooleanValue FINAL_SLASH_ERASE_ALLOWED;
   public static final IntValue FINAL_SLASH_SEAL_RADIUS;
   public static final IntValue FINAL_SLASH_SEAL_SECONDS;
   public static final BooleanValue FINAL_SLASH_HARD_RETRY;
   public static final BooleanValue FINAL_SLASH_REFLECT_LINK;
   public static final BooleanValue TARGET_KEEPER_ENABLED;
   public static final DoubleValue TARGET_KEEPER_RANGE;
   public static final IntValue TARGET_KEEPER_KEEP_TICKS;
   public static final BooleanValue SERVANT_BELL_KEEP_HOME;
   public static final BooleanValue SERVANT_BELL_MOVE_HOME;
   public static final BooleanValue SERVANT_BELL_SLAB_PRIORITY;
   public static final BooleanValue RETREAT_ENABLED;
   public static final IntValue RETREAT_KILLS;
   public static final IntValue RETREAT_WINDOW_TICKS;
   public static final IntValue RETREAT_AVOID_TICKS;
   public static final BooleanValue SHIELD_HOLD_CLEARS;
   public static final BooleanValue SHIELD_RESTORE_VANISH;
   public static final BooleanValue SHIELD_KEEP_ON_COLLECT;
   public static final BooleanValue BOSS_BOOM_ON_LOST;
   public static final BooleanValue BOSS_THEME_ENABLED;
   public static final BooleanValue SHIELD_TIERED;
   public static final BooleanValue SHIELD_LEDGER;
   public static final BooleanValue GUARD_REWRITE;
   public static final BooleanValue DEVOTED_HEART_ENABLED;
   public static final DoubleValue HEART_BIND_RANGE;
   public static final DoubleValue HEART_HATE_RANGE;
   public static final IntValue HEART_HATE_SWEEP_TICKS;
   public static final BooleanValue HEART_OWNER_SHIELDED;
   public static final DoubleValue HEART_FULL_RANGE;
   public static final DoubleValue HEART_NEAR_RANGE;
   public static final DoubleValue HEART_MID_RANGE;
   public static final DoubleValue HEART_NEAR_CUT;
   public static final DoubleValue HEART_MID_CUT;
   public static final DoubleValue HEART_FAR_CUT;
   public static final BooleanValue HATE_ENABLED;
   public static final IntValue HATE_SWEEP_TICKS;
   public static final IntValue HATE_MARK_TICKS;
   public static final IntValue HATE_STRIKE_TICKS;
   public static final IntValue HATE_HURT_TICKS;
   public static final DoubleValue HATE_BIND_RANGE;
   public static final DoubleValue HATE_RANGE;
   public static final DoubleValue HATE_TAKEOVER_RANGE;
   public static final BooleanValue HATE_FILL_LIST;
   public static final DoubleValue HATE_MARK_DAMAGE;
   public static final IntValue HATE_MOVE_HOME_TICKS;
   public static final BooleanValue BLACK_HOLE_ENABLED;
   public static final DoubleValue BLACK_HOLE_PULL_SPEED;
   public static final DoubleValue BLACK_HOLE_CRUSH_RATIO;
   public static final DoubleValue BLACK_HOLE_RADIUS;
   public static final IntValue BLACK_HOLE_TICKS;
   public static final DoubleValue BLACK_HOLE_SIZE;
   public static final DoubleValue BLACK_HOLE_HIT_RADIUS;
   public static final DoubleValue BLACK_HOLE_WARP_RADIUS;
   public static final DoubleValue BLACK_HOLE_EDGE_BAND;
   public static final DoubleValue BLACK_HOLE_EXEC_DRAIN_RATIO;
   public static final DoubleValue BLACK_HOLE_EXEC_GRIP_RADIUS;
   public static final DoubleValue BLACK_HOLE_SOUND_EXTRA;
   public static final BooleanValue BLACK_HOLE_SOUND_ENABLED;
   public static final BooleanValue HOVER_CANNON_ENABLED;
   public static final IntValue HOVER_CANNON_IDLE_SECONDS;
   public static final IntValue HOVER_CANNON_INTERVAL_TICKS;
   public static final DoubleValue HOVER_CANNON_INTERVAL_JITTER;
   public static final DoubleValue HOVER_CANNON_HEALTH_RATIO;
   public static final DoubleValue HOVER_CANNON_ATTACK_RATIO;
   public static final IntValue HOVER_CANNON_SLOW_LEVEL;
   public static final DoubleValue HOVER_CANNON_RAY_SOUND_RANGE;
   public static final DoubleValue HOVER_CANNON_SPIN_HIT_SOUND_RANGE;
   public static final DoubleValue HOVER_CANNON_SPIN_RAY_SOUND_RANGE;
   public static final DoubleValue HOVER_CANNON_SPIN_RAY_SOUND_FAR;
   public static final DoubleValue HOVER_CANNON_FORM_RAY_SOUND_RANGE;
   public static final DoubleValue HOVER_CANNON_FORM_RAY_SOUND_FAR;
   public static final DoubleValue HOVER_CANNON_SOLO_BEAM_SOUND_RANGE;
   public static final DoubleValue HOVER_CANNON_SOLO_BEAM_SOUND_FAR;
   public static final DoubleValue HOVER_CANNON_RANGE;
   public static final IntValue HOVER_CANNON_VOLLEYS;
   public static final IntValue HOVER_CANNON_CHARGE_TICKS;
   public static final DoubleValue HOVER_CANNON_SHOT_SPEED;
   public static final DoubleValue HOVER_CANNON_SHOT_HEALTH_RATIO;
   public static final DoubleValue HOVER_CANNON_SHOT_ATTACK_RATIO;
   public static final IntValue HOVER_CANNON_SPIN_REQUIRED;
   public static final IntValue HOVER_CANNON_SPIN_TICKS;
   public static final BooleanValue HOVER_CANNON_SPIN_BLOCK_BREAK;
   public static final DoubleValue HOVER_CANNON_SPIN_ATTACK_RATIO;
   public static final DoubleValue HOVER_CANNON_SPIN_HEALTH_RATIO;
   public static final DoubleValue HOVER_CANNON_JOINT_MAX_RATIO;
   public static final DoubleValue HOVER_CANNON_JOINT_ARMOR_IGNORE;
   public static final IntValue HOVER_CANNON_SOLO_CHARGE_TICKS;
   public static final IntValue HOVER_CANNON_SOLO_FIRE_TICKS;
   public static final IntValue HOVER_CANNON_SOLO_FADE_TICKS;
   public static final DoubleValue HOVER_CANNON_SOLO_ATTACK_RATIO;
   public static final DoubleValue HOVER_CANNON_SOLO_HEALTH_RATIO;
   public static final DoubleValue HOVER_CANNON_SOLO_LOCK_RANGE;
   public static final DoubleValue MOSSY_PUPPET_HEALTH;
   public static final DoubleValue MOSSY_PUPPET_ATTACK;
   public static final IntValue MOSSY_PUPPET_ATTACK_INTERVAL;
   public static final DoubleValue MOSSY_PUPPET_WAKE_RANGE;
   public static final BooleanValue MOSSY_PUPPET_THROW_PAUSE;
   public static final DoubleValue MOSSY_PUPPET_DEATH_RADIUS;
   public static final DoubleValue RELIC_WARDEN_WAKE_RANGE;
   public static final DoubleValue RELIC_WARDEN_BEAM_RATIO;
   public static final DoubleValue RELIC_WARDEN_BOLT_SPEED;
   public static final DoubleValue RELIC_MUSIC_GAP_MIN;
   public static final DoubleValue RELIC_MUSIC_GAP_MAX;
   public static final DoubleValue RELIC_WARDEN_MUSIC_RANGE;
   public static final DoubleValue RELIC_FOG_VISIBLE_RANGE;
   public static final DoubleValue RELIC_WARDEN_STUN_SECONDS;
   public static final DoubleValue RELIC_WARDEN_TRUE_DAMAGE_PERCENT;
   public static final DoubleValue RELIC_WARDEN_INVULN_SECONDS;
   public static final DoubleValue RELIC_WARDEN_DAMAGE_CAP;
   public static final IntValue RELIC_WARDEN_SWEEP_MILLIS;
   public static final DoubleValue RELIC_WARDEN_DEATH_KNOCKBACK;
   public static final DoubleValue RELIC_WARDEN_DEATH_BLAST_PLAYER_RADIUS;
   public static final DoubleValue RELIC_WARDEN_DEATH_BLAST_MOB_RADIUS;
   public static final DoubleValue RELIC_WARDEN_BASE_ATTACK;
   public static final DoubleValue RELIC_WARDEN_SLAM_TRUE_PERCENT;
   public static final DoubleValue RELIC_WARDEN_PULL_TRUE_PERCENT;
   public static final DoubleValue RELIC_WARDEN_CHARGE_MIN_DIST;
   public static final DoubleValue RELIC_WARDEN_BREAK_HEALTH;
   public static final DoubleValue RELIC_WARDEN_AIR_HEIGHT;
   public static final IntValue RELIC_EYE_SEARCH_RANGE;
   public static final DoubleValue RELIC_SHAFT_FACE_FALL;
   public static final DoubleValue RELIC_SHAFT_BACK_FALL;
   public static final BooleanValue RECIPES_ENABLED;
   public static final BooleanValue BUILD_LIMIT_OVERRIDE;
   public static volatile boolean buildLimitOverride;
   private static volatile boolean buildLimitReady;
   private static volatile double puppetHealth = 20.0;
   private static volatile double puppetAttack = 5.0;
   private static volatile int puppetAttackInterval = 22;
   private static volatile double puppetWakeRange = 4.0;
   private static volatile boolean puppetThrowPause = true;
   private static volatile double puppetDeathRadius = 2.5;
   private static volatile boolean puppetReady;
   private static volatile double relicWardenWakeRange = 4.0;
   private static volatile double relicMusicGapMin = 11.0;
   private static volatile double relicMusicGapMax = 33.0;
   private static volatile double relicWardenMusicRange = 66.0;
   private static volatile double relicFogVisibleRange = 33.0;
   private static volatile double relicWardenBeamRatio = 0.1;
   private static volatile double relicWardenBoltSpeed = 111.0;
   private static volatile double relicWardenStunSeconds = 3.0;
   private static volatile double relicWardenTrueDamagePercent = 0.22;
   private static volatile double relicWardenInvulnSeconds = 1.0;
   private static volatile double relicWardenDamageCap = 25.0;
   private static volatile int relicWardenSweepMillis = 10;
   private static volatile double relicWardenDeathKnockback = 11.0;
   private static volatile double relicWardenDeathBlastPlayerRadius = 5.0;
   private static volatile double relicWardenDeathBlastMobRadius = 11.0;
   private static volatile double relicWardenBaseAttack = 100.0;
   private static volatile double relicWardenSlamTruePercent = 0.33;
   private static volatile double relicWardenPullTruePercent = 0.66;
   private static volatile double relicWardenChargeMinDist = 5.0;
   private static volatile double relicWardenBreakHealth = 0.1;
   private static volatile double relicWardenAirHeight = 4.0;
   private static volatile int relicEyeSearchRange;
   private static volatile boolean relicReady;
   private static final BooleanValue DEATH_FOG_ENABLED;

   public static double relicWardenDeathBlastPlayerRadius() {
      if (!relicReady) {
         refresh();
      }

      return relicWardenDeathBlastPlayerRadius;
   }

   public static double relicWardenDeathBlastMobRadius() {
      if (!relicReady) {
         refresh();
      }

      return relicWardenDeathBlastMobRadius;
   }

   public static double relicWardenPullTruePercent() {
      if (!relicReady) {
         refresh();
      }

      return relicWardenPullTruePercent;
   }

   public static void refresh() {
      try {
         buildLimitOverride = (Boolean)BUILD_LIMIT_OVERRIDE.get();
         buildLimitReady = true;
      } catch (Throwable var3) {
      }

      try {
         puppetHealth = (Double)MOSSY_PUPPET_HEALTH.get();
         puppetAttack = (Double)MOSSY_PUPPET_ATTACK.get();
         puppetAttackInterval = (Integer)MOSSY_PUPPET_ATTACK_INTERVAL.get();
         puppetWakeRange = (Double)MOSSY_PUPPET_WAKE_RANGE.get();
         puppetThrowPause = (Boolean)MOSSY_PUPPET_THROW_PAUSE.get();
         puppetDeathRadius = (Double)MOSSY_PUPPET_DEATH_RADIUS.get();
         puppetReady = true;
      } catch (Throwable var2) {
      }

      try {
         relicWardenWakeRange = (Double)RELIC_WARDEN_WAKE_RANGE.get();
         relicMusicGapMin = (Double)RELIC_MUSIC_GAP_MIN.get();
         relicMusicGapMax = (Double)RELIC_MUSIC_GAP_MAX.get();
         relicWardenMusicRange = (Double)RELIC_WARDEN_MUSIC_RANGE.get();
         relicFogVisibleRange = (Double)RELIC_FOG_VISIBLE_RANGE.get();
         relicWardenBeamRatio = (Double)RELIC_WARDEN_BEAM_RATIO.get();
         relicWardenBoltSpeed = (Double)RELIC_WARDEN_BOLT_SPEED.get();
         relicWardenStunSeconds = (Double)RELIC_WARDEN_STUN_SECONDS.get();
         relicWardenTrueDamagePercent = (Double)RELIC_WARDEN_TRUE_DAMAGE_PERCENT.get();
         relicWardenInvulnSeconds = (Double)RELIC_WARDEN_INVULN_SECONDS.get();
         relicWardenDamageCap = (Double)RELIC_WARDEN_DAMAGE_CAP.get();
         relicWardenSweepMillis = (Integer)RELIC_WARDEN_SWEEP_MILLIS.get();
         relicWardenDeathKnockback = (Double)RELIC_WARDEN_DEATH_KNOCKBACK.get();
         relicWardenDeathBlastPlayerRadius = (Double)RELIC_WARDEN_DEATH_BLAST_PLAYER_RADIUS.get();
         relicWardenDeathBlastMobRadius = (Double)RELIC_WARDEN_DEATH_BLAST_MOB_RADIUS.get();
         relicWardenBaseAttack = (Double)RELIC_WARDEN_BASE_ATTACK.get();
         relicWardenSlamTruePercent = (Double)RELIC_WARDEN_SLAM_TRUE_PERCENT.get();
         relicWardenPullTruePercent = (Double)RELIC_WARDEN_PULL_TRUE_PERCENT.get();
         relicWardenChargeMinDist = (Double)RELIC_WARDEN_CHARGE_MIN_DIST.get();
         relicWardenBreakHealth = (Double)RELIC_WARDEN_BREAK_HEALTH.get();
         relicWardenAirHeight = (Double)RELIC_WARDEN_AIR_HEIGHT.get();
         relicEyeSearchRange = (Integer)RELIC_EYE_SEARCH_RANGE.get();
         relicReady = true;
      } catch (Throwable var1) {
      }
   }

   public static boolean buildLimit() {
      if (!buildLimitReady) {
         refresh();
      }

      return buildLimitOverride;
   }

   public static double puppetHealth() {
      if (!puppetReady) {
         refresh();
      }

      return puppetHealth;
   }

   public static double puppetAttack() {
      if (!puppetReady) {
         refresh();
      }

      return puppetAttack;
   }

   public static int puppetAttackInterval() {
      if (!puppetReady) {
         refresh();
      }

      return puppetAttackInterval;
   }

   public static double puppetWakeRange() {
      if (!puppetReady) {
         refresh();
      }

      return puppetWakeRange;
   }

   public static boolean puppetThrowPause() {
      if (!puppetReady) {
         refresh();
      }

      return puppetThrowPause;
   }

   public static double puppetDeathRadius() {
      if (!puppetReady) {
         refresh();
      }

      return puppetDeathRadius;
   }

   public static double relicWardenWakeRange() {
      if (!relicReady) {
         refresh();
      }

      return relicWardenWakeRange;
   }

   public static double relicWardenBeamRatio() {
      if (!relicReady) {
         refresh();
      }

      return relicWardenBeamRatio;
   }

   public static double relicWardenBoltSpeed() {
      if (!relicReady) {
         refresh();
      }

      return relicWardenBoltSpeed;
   }

   public static double relicMusicGapMin() {
      if (!relicReady) {
         refresh();
      }

      return relicMusicGapMin;
   }

   public static double relicMusicGapMax() {
      if (!relicReady) {
         refresh();
      }

      return Math.max(relicMusicGapMax, relicMusicGapMin);
   }

   public static double relicFogVisibleRange() {
      if (!relicReady) {
         refresh();
      }

      return relicFogVisibleRange;
   }

   public static boolean deathFogEnabled() {
      try {
         return (Boolean)DEATH_FOG_ENABLED.get();
      } catch (Throwable ignored) {
         return false;
      }
   }

   public static double relicWardenMusicRange() {
      if (!relicReady) {
         refresh();
      }

      return relicWardenMusicRange;
   }

   public static double relicWardenStunSeconds() {
      if (!relicReady) {
         refresh();
      }

      return relicWardenStunSeconds;
   }

   public static double relicWardenTrueDamagePercent() {
      if (!relicReady) {
         refresh();
      }

      return relicWardenTrueDamagePercent;
   }

   public static double relicWardenDamageCap() {
      if (!relicReady) {
         refresh();
      }

      return relicWardenDamageCap;
   }

   public static int relicWardenSweepMillis() {
      if (!relicReady) {
         refresh();
      }

      return relicWardenSweepMillis;
   }

   public static double relicWardenInvulnSeconds() {
      if (!relicReady) {
         refresh();
      }

      return relicWardenInvulnSeconds;
   }

   public static double relicWardenDeathKnockback() {
      if (!relicReady) {
         refresh();
      }

      return relicWardenDeathKnockback;
   }

   public static double relicWardenBaseAttack() {
      if (!relicReady) {
         refresh();
      }

      return relicWardenBaseAttack;
   }

   public static double relicWardenSlamTruePercent() {
      if (!relicReady) {
         refresh();
      }

      return relicWardenSlamTruePercent;
   }

   public static double relicWardenChargeMinDist() {
      if (!relicReady) {
         refresh();
      }

      return relicWardenChargeMinDist;
   }

   public static double relicWardenBreakHealth() {
      if (!relicReady) {
         refresh();
      }

      return relicWardenBreakHealth;
   }

   public static double relicWardenAirHeight() {
      if (!relicReady) {
         refresh();
      }

      return relicWardenAirHeight;
   }

   public static int relicEyeSearchRange() {
      if (!relicReady) {
         refresh();
      }

      return relicEyeSearchRange;
   }

   public static double relicShaftFaceFall() {
      try {
         return (Double)RELIC_SHAFT_FACE_FALL.get();
      } catch (Throwable ignored) {
         return 0.33;
      }
   }

   public static double relicShaftBackFall() {
      try {
         return (Double)RELIC_SHAFT_BACK_FALL.get();
      } catch (Throwable ignored) {
         return 0.09;
      }
   }

   static {
      Builder builder = new Builder();
      builder.push("final_slash");
      TRUE_HITS_EXECUTION = builder.comment("真伤斩杀线：同一目标承受混沌之书真伤达到该次数仍未死亡，则进行斩杀 (default: 33)").defineInRange("trueHitsExecution", 33, 1, 10000);
      FINISH_SWINGS = builder.comment("对同一个目标攻击次数过多，女仆将生气地直接将其斩杀；填 0 则每次斩杀都带 (default: 2)").defineInRange("finishSwings", 2, 0, 10000);
      FINAL_SLASH_HIT_RATIO = builder.comment("与混沌之书联动：女仆每次命中额外削去目标最大生命的比例 (default: 0.05)").defineInRange("hitTrueDamageRatio", 0.05, 0.0, 1.0);
      FINAL_SLASH_HIT_COUNT = builder.comment("与混沌之书联动：女仆每次命中累加的斩杀计数 (default: 1)").defineInRange("hitExecutionCount", 1, 0, 1000);
      SLASH_PULSE_ENABLED = builder.comment("有的敌人把受击部分整段重写了，事件根本发不出来，与混沌之书的真伤接不上；是否不看事件，只按战斗时间替她补刀 (default: true)").define("slashPulse", true);
      SLASH_PULSE_INTERVAL_TICKS = builder.comment("从进入战斗算起，隔多久补一刀，tick (default: 220)").defineInRange("slashPulseIntervalTicks", 220, 20, 24000);
      SLASH_PULSE_CUT_RATIO = builder.comment("普通斩每刀斩去目标当前血量的比例 (default: 0.5，砍半)").defineInRange("slashPulseCutRatio", 0.5, 0.0, 0.99);
      SLASH_PULSE_EXEC_COUNT = builder.comment("普通斩每刀累加的斩杀计数 (default: 6)").defineInRange("slashPulseExecCount", 6, 0, 1000);
      SLASH_PULSE_RANGE = builder.comment("隔着这么远就不再补刀，单位是格 (default: 48)").defineInRange("slashPulseRange", 48.0, 4.0, 128.0);
      FINAL_SLASH_ERASE_ALLOWED = builder.comment("终结之斩的抹除模式是否允许被打开 (default: true)").define("allowEraseMode", true);
      FINAL_SLASH_SEAL_RADIUS = builder.comment("阻止实体回归的半径，格 (default: 3)").defineInRange("sealRadius", 3, 1, 33);
      FINAL_SLASH_SEAL_SECONDS = builder.comment("阻止实体回归的持续时间，秒 (default: 6)").defineInRange("sealSeconds", 6, 1, 999);
      FINAL_SLASH_HARD_RETRY = builder.comment("是否和复活怪强行硬刚，有卡死风险 (default: true)").define("hardRetry", true);
      FINAL_SLASH_REFLECT_LINK = builder.comment("与紫荆银冠联动：她戴着这两件被打时，反伤之外再补一份同等真伤，将会计入斩杀累计值 (default: true)").define("reflectLink", true);
      builder.pop();
      builder.push("target_keeper");
      TARGET_KEEPER_ENABLED = builder.comment("女仆锁定的目标跑远了本体就会放弃，是否额外增加锁定距离 (default: true)").define("enabled", true);
      TARGET_KEEPER_RANGE = builder.comment("具体距离，格 (default: 48)").defineInRange("range", 48.0, 8.0, 128.0);
      TARGET_KEEPER_KEEP_TICKS = builder.comment("多长时间任然不行才放手，tick (default: 200)").defineInRange("keepTicks", 200, 20, 6000);
      builder.pop();
      builder.push("servant_bell");
      SERVANT_BELL_KEEP_HOME = builder.comment("仆人铃召回女仆时是否保留 home 模式(default: true)").define("keepHome", true);
      SERVANT_BELL_MOVE_HOME = builder.comment("召回后把 home 点改为召回落点 (default: true)").define("moveHome", true);
      SERVANT_BELL_SLAB_PRIORITY = builder.comment("是否使得魂符的放出女仆在交互方块时最优先使用").define("slabPriority", true);
      builder.pop();
      builder.push("retreat");
      RETREAT_ENABLED = builder.comment("同一只敌人反复打死她就让她先躲开，默认关 (default: false)").define("enabled", false);
      RETREAT_KILLS = builder.comment("同一个目标打死她多少次就让她躲开 (default: 3)").defineInRange("killsToRetreat", 3, 1, 100);
      RETREAT_WINDOW_TICKS = builder.comment("计数的窗口，tick (default: 600)").defineInRange("windowTicks", 600, 20, 24000);
      RETREAT_AVOID_TICKS = builder.comment("躲开多久，tick (default: 1200)").defineInRange("avoidTicks", 1200, 20, 24000);
      builder.pop();
      builder.comment("本组除 rewrite 之外都是旧版守护用的开关：rewrite 打开（默认）时它们不生效").push("shield");
      SHIELD_HOLD_CLEARS = builder.comment("戴盾的女仆会受到保护：除了换维度传送，被打死、被清除、区块卸载一律阻止。魂符收人走本模组的清除方法 (default: true)").define("holdClears", true);
      SHIELD_RESTORE_VANISH = builder.comment("最高档保护是否无条件复活，此选项为防止女仆无法清除导致其他模组玩法失效而生 (default: true)").define("restoreVanish", true);
      SHIELD_KEEP_ON_COLLECT = builder.comment("收入魂符时是否断开保护检测，关闭会导致可能放出就死 (default: true)").define("keepOnCollect", true);
      BOSS_BOOM_ON_LOST = builder.comment("测试boss自爆开关 (default: false)").define("bossBoomOnLost", false);
      BOSS_THEME_ENABLED = builder.comment("测试 boss 在场时放主题曲 (default: true)").define("bossTheme", true);
      SHIELD_TIERED = builder.comment("存护之盾是否根据联动分档保护？关闭此选项则单存护之盾就可无限复活(default: true)").define("tiered", true);
      SHIELD_LEDGER = builder.comment("守护留底另存一份在存档里：谁动过她、拦没拦住都记一笔，开服时照这份把人补回来。关掉的话只有本次开服期间守得住 (default: true)").define("ledger", true);
      GUARD_REWRITE = builder.comment("是否为新版的假血外露真血隐藏模式 (default: true)").define("rewrite", true);
      builder.pop();
      builder.push("devoted_heart");
      DEVOTED_HEART_ENABLED = builder.comment("赴爱捣敌之心：把盯着主人的生物引到她身上，主人亲手打的那只她优先锁定 (default: true)").define("enabled", true);
      HEART_BIND_RANGE = builder.comment("心转移仇恨最远距离 (default: 128)").defineInRange("bindRange", 128.0, 8.0, 512.0);
      HEART_HATE_RANGE = builder.comment("将这么远之内盯着主人的生物都拉过来，格 (default: 64)").defineInRange("sweepRange", 64.0, 4.0, 256.0);
      HEART_HATE_SWEEP_TICKS = builder.comment("检查主人仇恨间隔 (default: 20)").defineInRange("sweepIntervalTicks", 20, 1, 1200);
      HEART_OWNER_SHIELDED = builder.comment("与存护之盾联动：根据距离为主人提供减伤 (default: true)").define("ownerShielded", true);
      HEART_FULL_RANGE = builder.comment("减伤档位：她在这个距离以内，主人完全免伤，格 (default: 6)").defineInRange("fullRange", 6.0, 1.0, 64.0);
      HEART_NEAR_RANGE = builder.comment("减伤档位：她在这个距离以内按近档减伤，格 (default: 22)").defineInRange("nearRange", 22.0, 1.0, 256.0);
      HEART_MID_RANGE = builder.comment("减伤档位：她在这个距离以内按中档减伤，再远就是最后一档，格 (default: 66)").defineInRange("midRange", 66.0, 1.0, 512.0);
      HEART_NEAR_CUT = builder.comment("近档减伤比例，1.0 等于全免 (default: 0.99)").defineInRange("nearCut", 0.99, 0.0, 1.0);
      HEART_MID_CUT = builder.comment("中档减伤比例 (default: 0.90)").defineInRange("midCut", 0.9, 0.0, 1.0);
      HEART_FAR_CUT = builder.comment("最远那一档的减伤比例 (default: 0.50)").defineInRange("farCut", 0.5, 0.0, 1.0);
      builder.pop();
      builder.push("hate");
      HATE_ENABLED = builder.comment("主人结的仇按档位传给她：亲手使用实体id获取器左键标的最优先，其次是主人打过的那只，再其次是谁打了主人；她只认存在的目标中最靠前的那一档。与赴爱捣敌之心无关，装上就生效 (default: true)")
         .define("enabled", true);
      HATE_SWEEP_TICKS = builder.comment("多久把仇重新说给她听一次，tick (default: 20)。每跑一次都要把主人这一圈里的生物查一遍，调太密会跟着世界的 tick 一起抖")
         .defineInRange("sweepIntervalTicks", 20, 1, 1200);
      HATE_MARK_TICKS = builder.comment("手持实体ID复制器左键标下的那只，她记着打多久，tick (default: 2400)").defineInRange("markLockTicks", 2400, 20, 72000);
      HATE_STRIKE_TICKS = builder.comment("主人亲手打过的那只，她记着打多久，tick (default: 200)").defineInRange("strikeLockTicks", 200, 20, 24000);
      HATE_HURT_TICKS = builder.comment("主人挨了谁的打，她记着咬多久，tick (default: 100)").defineInRange("hurtLockTicks", 100, 20, 24000);
      HATE_BIND_RANGE = builder.comment("她在主人这么远之内才接这份仇，格 (default: 64)").defineInRange("bindRange", 64.0, 8.0, 512.0);
      HATE_RANGE = builder.comment("目标离她比这还远就先放着，格 (default: 64)").defineInRange("range", 64.0, 4.0, 256.0);
      HATE_TAKEOVER_RANGE = builder.comment(
            "接下仇之后把她的活动半径最多撑到这么大，格。本体拿作息半径当战斗半径用：闲时 6 格、工作 12 格、非家模式 8 格，目标一出这个圈她就转火。实际给的是她到目标那点距离加几格余量，到不了这个上限。这个数她还要当寻路口径用，撑太满会让她每 tick 都搜一大片，走路一步一顿。比 range 小时按 range 算 (default: 64)"
         )
         .defineInRange("takeoverRange", 64.0, 8.0, 256.0);
      HATE_FILL_LIST = builder.comment("是否将标记目标记入女仆中立名单 (default: true)").define("fillAttackList", true);
      HATE_MARK_DAMAGE = builder.comment("已主人为伤害源造成的伤害数额 (default: 2.0，一颗心)").defineInRange("markDamage", 2.0, 0.0, 40.0);
      HATE_MOVE_HOME_TICKS = builder.comment("将home挪到锁定的目标位置的间隔时长(default: 11)").defineInRange("moveHomeTicks", 11, 1, 1200);
      builder.pop();
      builder.push("black_hole");
      BLACK_HOLE_ENABLED = builder.comment("黑洞卷轴：女仆打出普通斩时，在射线的落点留一颗小黑洞，将周围实体向着此黑洞拖拽 (default: true)").define("enabled", true);
      BLACK_HOLE_PULL_SPEED = builder.comment("把周围实体往中心拖拽的速度，格每秒 (default: 5.0)。越靠近中心拽得越快这件事不额外做，各方向一律这个速度").defineInRange("pullSpeed", 5.0, 0.0, 64.0);
      BLACK_HOLE_CRUSH_RATIO = builder.comment("实体碰到黑洞边缘时按它当前血量的比例挤压，使用自定义的不吃冷却的伤害类型，每 tick 一次 (default: 0.01，即百分之一)")
         .defineInRange("crushRatio", 0.01, 0.0, 1.0);
      BLACK_HOLE_RADIUS = builder.comment("黑洞拽得到多远的实体，格 (default: 11)。注：这个半径里同时只留一颗黑洞，新的一颗落地会把范围内更早的那颗清掉，斩杀留下的那颗不在此列").defineInRange("radius", 11.0, 0.5, 32.0);
      BLACK_HOLE_TICKS = builder.comment("黑洞持续时间是多久，tick。填 0 就跟普通斩的间隔一样长，也就是这一刀留下的黑洞撑到下一刀 (default: 0)").defineInRange("durationTicks", 0, 0, 24000);
      BLACK_HOLE_SIZE = builder.comment("事件视界的半径，格，跟伤害判定、扭曲范围各算各的 (default: 0.25，直径半格)").defineInRange("visualSize", 0.25, 0.05, 8.0);
      BLACK_HOLE_HIT_RADIUS = builder.comment("伤害判定半径，格。实体的碰撞箱接触到这个球就持续受伤 (default: 0.25，直径半格)。").defineInRange("hitRadius", 0.25, 0.05, 8.0);
      BLACK_HOLE_WARP_RADIUS = builder.comment("光被拽弯的半径，格 (default: 2.5，直径五格)").defineInRange("warpRadius", 2.5, 0.1, 16.0);
      BLACK_HOLE_EDGE_BAND = builder.comment("扭曲缓冲占比 (default: 0.22，即两成二)").defineInRange("edgeBand", 0.22, 0.0, 1.0);
      BLACK_HOLE_EXEC_DRAIN_RATIO = builder.comment("斩杀后留下的黑洞每秒对其中的目标基于被斩杀目标最大生命值造成伤害的百分比 (default: 0.1，即一成)")
         .defineInRange("executionDrainRatio", 0.1, 0.0, 1.0);
      BLACK_HOLE_EXEC_GRIP_RADIUS = builder.comment("斩杀留下的黑洞对实体的吸附距离，格 (default: 22)").defineInRange("executionGripRadius", 22.0, 1.0, 64.0);
      BLACK_HOLE_SOUND_EXTRA = builder.comment("黑洞音效传播范围比黑洞半径大多少格 (default: 11)").defineInRange("soundRangeExtra", 11.0, 0.0, 64.0);
      BLACK_HOLE_SOUND_ENABLED = builder.comment("黑洞在场上时响不响那一串音效 (default: true)").define("soundEnabled", true);
      builder.pop();
      builder.push("hover_cannon");
      HOVER_CANNON_ENABLED = builder.comment("小光炮：女仆饰品，由能源反应核心供能。两件同时戴在她饰品栏里才转得起来，身边浮出三个炮台，三台各射各的，轮着朝她锁着的那只射一道蓝光；能源核心一摘当场停火，炮台也收掉 (default: true)")
         .define("enabled", true);
      HOVER_CANNON_IDLE_SECONDS = builder.comment("浮游炮无仇恨时待机时间 (default: 8，秒)").defineInRange("idleSeconds", 8, 1, 600);
      HOVER_CANNON_INTERVAL_TICKS = builder.comment("单台浮游炮普攻间隔，tick (default: 6，即 0.3 秒)").defineInRange("intervalTicks", 6, 3, 24000);
      HOVER_CANNON_INTERVAL_JITTER = builder.comment("浮游炮普通攻击间隔随机波动范围 (default: 0.11，即上下各 11%)").defineInRange("intervalJitter", 0.11, 0.0, Double.MAX_VALUE);
      HOVER_CANNON_HEALTH_RATIO = builder.comment("浮游炮造成伤害时基于目标当前生命值的百分比 (default: 0.11)").defineInRange("healthRatio", 0.11, 0.0, 1.0);
      HOVER_CANNON_ATTACK_RATIO = builder.comment("浮游炮造成伤害时基于女仆攻击力的百分比 (default: 0.88)").defineInRange("attackRatio", 0.88, 0.0, 16.0);
      HOVER_CANNON_SLOW_LEVEL = builder.comment("浮游炮射线命中为敌人增加的缓慢效果等级 (default: 1，即缓慢 I)。0则不添加，可选范围 0 到 255").defineInRange("slowLevel", 1, 0, 255);
      HOVER_CANNON_RAY_SOUND_RANGE = builder.comment("浮游炮射线音效传播距离，格 (default: 64)").defineInRange("raySoundRange", 64.0, 0.0, 128.0);
      HOVER_CANNON_SPIN_HIT_SOUND_RANGE = builder.comment("旋转激光命中音效最远传播距离，格 (default: 33)").defineInRange("spinHitSoundRange", 33.0, 0.0, 128.0);
      HOVER_CANNON_SPIN_RAY_SOUND_RANGE = builder.comment("大激光喷射音效无衰减传播距离，格 (default: 55)").defineInRange("spinRaySoundRange", 55.0, 0.0, 256.0);
      HOVER_CANNON_SPIN_RAY_SOUND_FAR = builder.comment("大激光喷射音效最远传播距离，格 (default: 66)").defineInRange("spinRaySoundFar", 66.0, 0.0, 256.0);
      HOVER_CANNON_FORM_RAY_SOUND_RANGE = builder.comment("三合法阵音效无衰减传播距离，格 (default: 55)").defineInRange("formRaySoundRange", 55.0, 0.0, 256.0);
      HOVER_CANNON_FORM_RAY_SOUND_FAR = builder.comment("三合法阵音效最远传播距离，格 (default: 66)").defineInRange("formRaySoundFar", 66.0, 0.0, 256.0);
      HOVER_CANNON_SOLO_BEAM_SOUND_RANGE = builder.comment("对单大激光音效无衰减传播距离，格 (default: 66)").defineInRange("soloBeamSoundRange", 66.0, 0.0, 256.0);
      HOVER_CANNON_SOLO_BEAM_SOUND_FAR = builder.comment("对单大激光音效最远传播距离，格 (default: 99)").defineInRange("soloBeamSoundFar", 99.0, 0.0, 256.0);
      HOVER_CANNON_RANGE = builder.comment("浮游炮攻击目标最远距离，格 (default: 55)").defineInRange("range", 55.0, 2.0, 128.0);
      HOVER_CANNON_VOLLEYS = builder.comment("浮游炮释放三合一需要的普通攻击累计轮数，各攻击一次记一轮 (default: 11)").defineInRange("volleys", 11, 1, 64);
      HOVER_CANNON_CHARGE_TICKS = builder.comment("三合一蓄力时间，tick (default: 10)").defineInRange("chargeTicks", 10, 1, 400);
      HOVER_CANNON_SHOT_SPEED = builder.comment("旧版三合一飞行弹的速度，格每秒，已弃用 (default: 11)").defineInRange("shotSpeed", 11.0, 0.5, 200.0);
      HOVER_CANNON_SHOT_HEALTH_RATIO = builder.comment("三合一造成的伤害基于目标当前生命值的百分比 (default: 0.33)").defineInRange("shotHealthRatio", 0.33, 0.0, 4.0);
      HOVER_CANNON_SHOT_ATTACK_RATIO = builder.comment("三合一造成的伤害基于女仆攻击力的百分比 (default: 9.99)").defineInRange("shotAttackRatio", 9.99, 0.0, 64.0);
      HOVER_CANNON_SPIN_REQUIRED = builder.comment("释放两种大激光需要的三合一攻击次数 (default: 3)").defineInRange("spinRequired", 3, 1, 64);
      HOVER_CANNON_SPIN_TICKS = builder.comment("清屏大招持续时长， tick (default: 67，即 3.35 秒)").defineInRange("spinTicks", 67, 5, 12000);
      HOVER_CANNON_SPIN_BLOCK_BREAK = builder.comment("旋转激光方块破坏 (default: true)").define("spinBlockBreak", true);
      HOVER_CANNON_SPIN_ATTACK_RATIO = builder.comment("旋转激光每 tick 造成伤害时基于女仆当前攻击力的百分比 (default: 1.11)").defineInRange("spinAttackRatio", 1.11, 0.0, 64.0);
      HOVER_CANNON_SPIN_HEALTH_RATIO = builder.comment("旋转激光每 tick 造成伤害时基于目标当前生命值的百分比 (default: 0.01)").defineInRange("spinHealthRatio", 0.01, 0.0, 1.0);
      HOVER_CANNON_JOINT_MAX_RATIO = builder.comment("三枚浮游炮的联合冲击将削减目标最大生命值的百分比 (default: 0.11)").defineInRange("jointMaxRatio", 0.11, 0.0, 1.0);
      HOVER_CANNON_JOINT_ARMOR_IGNORE = builder.comment("三枚浮游炮的联合冲击波无视目标护甲百分比 (default: 0.66)").defineInRange("jointArmorIgnore", 0.66, 0.0, 1.0);
      HOVER_CANNON_SOLO_CHARGE_TICKS = builder.comment("对单大激光蓄力时长，tick(default: 25，即 1.25 秒)").defineInRange("soloChargeTicks", 25, 1, 12000);
      HOVER_CANNON_SOLO_FIRE_TICKS = builder.comment("对单大激光持续攻击时长，tick (default: 74，即 3.7 秒)").defineInRange("soloFireTicks", 74, 1, 12000);
      HOVER_CANNON_SOLO_FADE_TICKS = builder.comment("对单大激光收尾时长，tick (default: 24，即 1.2 秒)").defineInRange("soloFadeTicks", 24, 0, 12000);
      HOVER_CANNON_SOLO_ATTACK_RATIO = builder.comment("小光炮发射的对单大激光造成伤害基于女仆攻击力的百分比 (default: 3.33，即 333%)").defineInRange("soloAttackRatio", 3.33, 0.0, 64.0);
      HOVER_CANNON_SOLO_HEALTH_RATIO = builder.comment("小光炮发射的对单大激光造成伤害基于目标当前生命值的百分比 (default: 0.33，即 33%)").defineInRange("soloHealthRatio", 0.33, 0.0, 1.0);
      HOVER_CANNON_SOLO_LOCK_RANGE = builder.comment("小光炮的对单大激光最远锁定距离 (default: 66)").defineInRange("soloLockRange", 66.0, 1.0, 512.0);
      builder.pop();
      builder.push("mossy_puppet");
      MOSSY_PUPPET_HEALTH = builder.comment("覆苔小傀儡血量上限 (default: 20)").defineInRange("health", 20.0, 1.0, 1024.0);
      MOSSY_PUPPET_ATTACK = builder.comment("覆苔小傀儡攻击力 (default: 5)").defineInRange("attack", 5.0, 0.0, 1024.0);
      MOSSY_PUPPET_ATTACK_INTERVAL = builder.comment("覆苔小傀儡普通攻击间隔时间，单位为tick (default: 22)").defineInRange("attackInterval", 22, 1, 512);
      MOSSY_PUPPET_WAKE_RANGE = builder.comment("覆苔小傀儡激活距离，格 (default: 4)").defineInRange("wakeRange", 4.0, 1.0, 64.0);
      MOSSY_PUPPET_THROW_PAUSE = builder.comment("覆苔小傀儡投石时是否暂时停止前进 (default: true)").define("throwPause", true);
      MOSSY_PUPPET_DEATH_RADIUS = builder.comment("覆苔小傀儡亡语波及范围，格 (default: 2.5)").defineInRange("deathRadius", 2.5, 0.0, 16.0);
      builder.pop();
      builder.push("mossy_relic");
      RELIC_SHAFT_FACE_FALL = builder.comment("遗迹外壳通道第1层风化概率，这个选项是入口竖井贴着井腔那一圈，每格按这个概率消失 (default: 0.33)").defineInRange("shaftFaceFall", 0.33, 0.0, 1.0);
      RELIC_SHAFT_BACK_FALL = builder.comment("遗迹外壳通道第2层风化概率，这个选项是第 1 层再往外一圈，每格按照这个概率连带着更表面的一层一同消失(default: 0.09)")
         .defineInRange("shaftBackFall", 0.09, 0.0, 1.0);
      RELIC_WARDEN_WAKE_RANGE = builder.comment("覆苔守卫兽起身感应范围，格 (default: 4)").defineInRange("wardenWakeRange", 4.0, 1.0, 64.0);
      RELIC_MUSIC_GAP_MIN = builder.comment("覆苔地下遗迹音乐播放完成后的等待时间最小值，秒 (default: 11)").defineInRange("musicGapMin", 11.0, 0.0, 3600.0);
      RELIC_MUSIC_GAP_MAX = builder.comment("覆苔地下遗迹音乐播放完成后的等待时间最大值，秒 (default: 33)").defineInRange("musicGapMax", 33.0, 0.0, 3600.0);
      RELIC_WARDEN_MUSIC_RANGE = builder.comment("覆苔守护兽战斗音乐与其音效的传播半径，格 (default: 66)").defineInRange("wardenMusicRange", 66.0, 1.0, 256.0);
      RELIC_FOG_VISIBLE_RANGE = builder.comment("各种技能产生的烟雾最远出现距离，格 (default: 33)").defineInRange("fogVisibleRange", 33.0, 4.0, 256.0);
      RELIC_WARDEN_BEAM_RATIO = builder.comment("覆苔守护兽起身时那一炮造成的伤害基于目标最大生命值的比例，无视护甲 (default: 0.1)").defineInRange("wardenBeamRatio", 0.1, 0.0, 10.0);
      RELIC_WARDEN_BOLT_SPEED = builder.comment("覆苔守护兽对空时发射的能量弹飞行速度（格每秒） (default: 111)").defineInRange("wardenBoltSpeed", 111.0, 1.0, 500.0);
      RELIC_WARDEN_STUN_SECONDS = builder.comment("覆苔守护兽发射头命中目标造成的眩晕时间，秒 (default: 3)").defineInRange("wardenStunSeconds", 3.0, 0.0, 60.0);
      RELIC_WARDEN_TRUE_DAMAGE_PERCENT = builder.comment("覆苔守护兽部分攻击命中造成的真实伤害基于目标的最大生命值百分比 (default: 0.22)")
         .defineInRange("wardenTrueDamagePercent", 0.22, 0.0, 10.0);
      RELIC_WARDEN_INVULN_SECONDS = builder.comment("覆苔守护兽受到攻击后的无敌时间，秒 (default: 1)").defineInRange("wardenInvulnSeconds", 1.0, 0.0, 60.0);
      RELIC_WARDEN_DAMAGE_CAP = builder.comment("覆苔守护兽单次最多承受的伤害 (default: 25)").defineInRange("wardenDamageCap", 25.0, 0.0, 100000.0);
      RELIC_WARDEN_SWEEP_MILLIS = builder.comment("覆苔守护兽血量与记录的比对间隔，毫秒 (default: 10)").defineInRange("wardenSweepMillis", 10, 1, 1000);
      RELIC_WARDEN_DEATH_BLAST_PLAYER_RADIUS = builder.comment("覆苔守护兽自爆归零玩家血量的范围半径，对创造和旁观者不生效 (default: 5)")
         .defineInRange("wardenDeathBlastPlayerRadius", 5.0, 0.0, 64.0);
      RELIC_WARDEN_DEATH_BLAST_MOB_RADIUS = builder.comment("覆苔守护兽自爆清空生物血量半径 (default: 11)").defineInRange("wardenDeathBlastMobRadius", 11.0, 0.0, 64.0);
      RELIC_WARDEN_DEATH_KNOCKBACK = builder.comment("死亡后震退玩家距离 (default: 11)").defineInRange("wardenDeathKnockback", 11.0, 0.0, 64.0);
      RELIC_WARDEN_BASE_ATTACK = builder.comment("覆苔守护兽基础攻击力数值，大部分技能都以此为基准计算伤害 (default: 100)").defineInRange("wardenBaseAttack", 100.0, 0.0, 100000.0);
      RELIC_WARDEN_SLAM_TRUE_PERCENT = builder.comment("守护兽冲撞下砸造成的真实伤害基于目标的最大生命值百分比 (default: 0.33)").defineInRange("wardenSlamTruePercent", 0.33, 0.0, 10.0);
      RELIC_WARDEN_PULL_TRUE_PERCENT = builder.comment("拔出头部时对未能挣脱的目标造成的真实伤害基于其最大生命值的百分比 (default: 0.66)")
         .defineInRange("wardenPullTruePercent", 0.66, 0.0, 10.0);
      RELIC_WARDEN_CHARGE_MIN_DIST = builder.comment("覆苔守护兽冲撞触发条件：与目标的最小距离，格 (default: 5)").defineInRange("wardenChargeMinDist", 5.0, 0.0, 64.0);
      RELIC_WARDEN_BREAK_HEALTH = builder.comment("断角扣除自身血量占总血量几成，不占用无敌帧 (default: 0.1，即一成)").defineInRange("wardenBreakHealthPercent", 0.1, 0.0, 1.0);
      RELIC_WARDEN_AIR_HEIGHT = builder.comment("覆苔守护兽对空高度门槛 (default: 4)").defineInRange("wardenAirHeight", 4.0, 0.0, 64.0);
      RELIC_EYE_SEARCH_RANGE = builder.comment("遗迹之眼搜索范围").defineInRange("eyeSearchRange", 0, 0, 1000000);
      builder.pop();
      builder.push("recipes");
      RECIPES_ENABLED = builder.comment("这个模组的女仆祭坛合成配方是否生效 (default: true)").define("enabled", true);
      builder.pop();
      builder.push("build_limit");
      BUILD_LIMIT_OVERRIDE = builder.comment("是否开启建筑限制高低突破 (default: true)").define("buildLimitOverride", true);
      builder.pop();
      builder.push("client");
      DEATH_FOG_ENABLED = builder.comment("守护兽自爆时那一片大烟幕是否绘制。默认关闭：它很大很浓，对显卡压力很重 (default: false)").define("deathFogEnabled", false);
      builder.pop();
      SPEC = builder.build();
   }
}

