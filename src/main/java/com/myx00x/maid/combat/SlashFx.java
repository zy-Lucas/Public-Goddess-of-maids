package com.myx00x.maid.combat;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public final class SlashFx {
   public static final int STAR_LEAD_MS = 100;

   private SlashFx() {
   }

   public static float starRadius(LivingEntity maid) {
      return (float)Mth.clamp(maid.getBbHeight() * 1.4, 1.6, 4.0);
   }
}

