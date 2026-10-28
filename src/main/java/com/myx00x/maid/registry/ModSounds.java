package com.myx00x.maid.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
   public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "goddess_of_maids");
   public static final RegistryObject<SoundEvent> EXECUTE = register("execute");
   public static final RegistryObject<SoundEvent> SLASH = register("slash");
   public static final RegistryObject<SoundEvent> SLASH_HOLE = register("slash_hole");
   public static final RegistryObject<SoundEvent> CLEAR = register("clear");
   public static final RegistryObject<SoundEvent> CANNON_RAY = register("cannon_ray");
   public static final RegistryObject<SoundEvent> SPIN_HIT = register("spin_hit");
   public static final RegistryObject<SoundEvent> SPIN_RAY = register("spin_ray");
   public static final RegistryObject<SoundEvent> FORM_RAY = register("form_ray");
   public static final RegistryObject<SoundEvent> SOLO_BEAM = register("solo_beam");
   public static final RegistryObject<SoundEvent> HOLE_START = register("hole_start");
   public static final RegistryObject<SoundEvent> HOLE_LOOP = register("hole_loop");
   public static final RegistryObject<SoundEvent> BOSS_THEME = register("boss_theme");
   public static final RegistryObject<SoundEvent> MOSSY_WAKE = register("mossy_wake");
   public static final RegistryObject<SoundEvent> MOSSY_SLEEP = register("mossy_sleep");
   public static final RegistryObject<SoundEvent> MOSSY_SWING = register("mossy_swing");
   public static final RegistryObject<SoundEvent> MOSSY_THROW = register("mossy_throw");
   public static final RegistryObject<SoundEvent> MOSSY_HURT = register("mossy_hurt");
   public static final RegistryObject<SoundEvent> MOSSY_DEATH = register("mossy_death");
   public static final RegistryObject<SoundEvent> WARDEN_SHOOT = register("warden_shoot");
   public static final RegistryObject<SoundEvent> WARDEN_SMASH = register("warden_smash");
   public static final RegistryObject<SoundEvent> WARDEN_CHARGE = register("warden_charge");
   public static final RegistryObject<SoundEvent> WARDEN_BREAK = register("warden_break");
   public static final RegistryObject<SoundEvent> WARDEN_GRAB_SLAM = register("warden_grab_slam");
   public static final RegistryObject<SoundEvent> RELIC_MUSIC_1 = register("relic_music_1");
   public static final RegistryObject<SoundEvent> WARDEN_BATTLE = register("warden_battle");
   public static final RegistryObject<SoundEvent> WARDEN_EXECUTE_BEAM = register("warden_execute_beam");
   public static final RegistryObject<SoundEvent> WARDEN_SWING_RAISE = register("warden_swing_raise");
   public static final RegistryObject<SoundEvent> WARDEN_SWING_SLAM = register("warden_swing_slam");
   public static final RegistryObject<SoundEvent> WARDEN_AIR_LASER = register("warden_air_laser");

   private static RegistryObject<SoundEvent> register(String name) {
      return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("goddess_of_maids", name)));
   }

   public static void register(IEventBus bus) {
      SOUNDS.register(bus);
   }
}

