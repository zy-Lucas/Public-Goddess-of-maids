package com.myx00x.maid.mixin;

import com.myx00x.maid.item.CoordinateDisplayItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class GhostNoPhysicsMixin {
   @Inject(method = "m_6478_", remap = false, at = @At("HEAD"))
   private void shenlin.keepGhost(MoverType type, Vec3 movement, CallbackInfo ci) {
      Entity self = (Entity)this;
      if (!self.noPhysics && CoordinateDisplayItem.isGhost(self)) {
         self.noPhysics = true;
      }
   }
}

