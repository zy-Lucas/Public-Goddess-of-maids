package com.myx00x.maid.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.yimeng261.maidspell.item.bauble.silverCercis.SilverCercisBauble;
import com.github.yimeng261.maidspell.utils.DataItem;
import com.myx00x.maid.combat.ReflectLink;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SilverCercisBauble.class, remap = false)
public class SilverCercisLinkMixin {
   @Inject(method = "handleSilverCercis", at = @At("TAIL"), remap = false, require = 0)
   private static void shenlin.reflectLink(EntityMaid maid, LivingEntity attacker, DataItem item, CallbackInfo ci) {
      ReflectLink.onCercis(maid, attacker, item);
   }
}

