package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public final class GuardClean {
   private static final String INJECTOR = "com.mega.uom.util.entity.EntityDataInjector";
   private static final String ASM = "com.mega.uom.util.entity.EntityASMUtil";
   private static boolean resolved = false;
   private static Method mebTime;
   private static Method mebLevel;
   private static Method mebDamage;
   private static Method mebSource;
   private static Method healthDelta;

   private GuardClean() {
   }

   public static void skin(EntityMaid maid) {
      harmful(maid);
      lingering(maid);
      maid.deathTime = 0;
      maid.hurtTime = 0;
      maid.fallDistance = 0.0F;
   }

   public static int harmful(EntityMaid maid) {
      List<MobEffect> found = new ArrayList<>();

      for (MobEffectInstance instance : maid.getActiveEffects()) {
         if (instance.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
            found.add(instance.getEffect());
         }
      }

      for (MobEffect effect : found) {
         maid.removeEffect(effect);
      }

      return found.size();
   }

   public static boolean lingering(EntityMaid maid) {
      if (!resolved) {
         resolve();
      }

      boolean touched = false;
      touched |= call(mebTime, maid, 0);
      touched |= call(mebLevel, maid, -1);
      touched |= call(mebDamage, maid, 0.0F);
      touched |= call(mebSource, maid, Optional.empty());
      return touched | call(healthDelta, maid, 0.0F);
   }

   private static void resolve() {
      resolved = true;
      ClassLoader loader = GuardClean.class.getClassLoader();

      try {
         Class<?> injector = Class.forName("com.mega.uom.util.entity.EntityDataInjector", true, loader);
         mebTime = injector.getMethod("setMebTime", LivingEntity.class, int.class);
         mebLevel = injector.getMethod("setMebLevel", LivingEntity.class, int.class);
         mebDamage = injector.getMethod("setMebDamage", LivingEntity.class, float.class);
         mebSource = injector.getMethod("setMebSource", LivingEntity.class, Optional.class);
      } catch (Throwable var3) {
      }

      try {
         Class<?> asm = Class.forName("com.mega.uom.util.entity.EntityASMUtil", true, loader);
         healthDelta = asm.getMethod("setHealthDelta", LivingEntity.class, float.class);
      } catch (Throwable var2) {
      }
   }

   private static boolean call(Method method, EntityMaid maid, Object value) {
      if (method == null) {
         return false;
      }

      try {
         method.invoke(null, maid, value);
         return true;
      } catch (Throwable ignored) {
         return false;
      }
   }
}

