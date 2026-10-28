package com.myx00x.maid.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.combat.GuardRemoval;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PersistentEntitySectionManager.class, remap = false)
public class ChunkUnloadMarkMixin {
   @Inject(method = "m_157585_(Lnet/minecraft/world/level/entity/EntityAccess;)V", at = @At("HEAD"), remap = false)
   private void shenlin.markChunkUnload(EntityAccess entity, CallbackInfo ci) {
      if (entity instanceof EntityMaid maid) {
         GuardRemoval.markChunkUnload(maid);
      }
   }
}

