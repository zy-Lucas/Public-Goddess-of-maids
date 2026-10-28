package com.myx00x.maid.mixin;

import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DimensionType.class, remap = false)
public class BuildLimitMixin {
   private static final int SHENLIN_MIN_Y = -256;
   private static final int SHENLIN_HEIGHT = 768;
   private static final ResourceLocation SHENLIN_OVERWORLD = ResourceLocation.tryParse("minecraft:overworld");
   private static volatile boolean shenlin.announced;

   private static boolean shenlin.applies(DimensionType self) {
      if (!Config.buildLimit()) {
         return false;
      }

      if (!Objects.equals(SHENLIN_OVERWORLD, self.effectsLocation())) {
         return false;
      }

      if (!shenlin.announced) {
         shenlin.announced = true;
         ShenlinMod.LOGGER.info("建筑限制突破已生效：主世界可建造范围放宽到 {} 到 {}", -256, 511);
      }

      return true;
   }

   @Inject(method = "f_156647_()I", at = @At("HEAD"), remap = false, cancellable = true)
   private void shenlin.minY(CallbackInfoReturnable<Integer> cir) {
      if (shenlin.applies((DimensionType)this)) {
         cir.setReturnValue(-256);
      }
   }

   @Inject(method = "f_156648_()I", at = @At("HEAD"), remap = false, cancellable = true)
   private void shenlin.height(CallbackInfoReturnable<Integer> cir) {
      if (shenlin.applies((DimensionType)this)) {
         cir.setReturnValue(768);
      }
   }

   @Inject(method = "f_63865_()I", at = @At("HEAD"), remap = false, cancellable = true)
   private void shenlin.logicalHeight(CallbackInfoReturnable<Integer> cir) {
      if (shenlin.applies((DimensionType)this)) {
         cir.setReturnValue(768);
      }
   }
}

