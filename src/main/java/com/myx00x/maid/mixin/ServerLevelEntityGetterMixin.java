package com.myx00x.maid.mixin;

import com.myx00x.maid.combat.GuardUnionGetter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.LevelEntityGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelEntityGetterMixin {
   @Unique
   private LevelEntityGetter<Entity> shenlin.union;

   @Inject(method = "m_142646_()Lnet/minecraft/world/level/entity/LevelEntityGetter;", at = @At("RETURN"), cancellable = true)
   private void shenlin.wrap(CallbackInfoReturnable<LevelEntityGetter<Entity>> cir) {
      LevelEntityGetter<Entity> base = (LevelEntityGetter<Entity>)cir.getReturnValue();
      if (base != null) {
         LevelEntityGetter<Entity> wrapped = this.shenlin.union;
         if (wrapped == null) {
            wrapped = new GuardUnionGetter(base, (ServerLevel)this);
            this.shenlin.union = wrapped;
         }

         cir.setReturnValue(wrapped);
      }
   }
}

