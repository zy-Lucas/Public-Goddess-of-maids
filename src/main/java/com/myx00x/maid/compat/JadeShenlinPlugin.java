package com.myx00x.maid.compat;

import com.myx00x.maid.client.BossShellGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;
import snownee.jade.api.Accessor;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin("goddess_of_maids")
public final class JadeShenlinPlugin implements IWailaPlugin {
   public void registerClient(IWailaClientRegistration registration) {
      registration.addRayTraceCallback(
         (hitResult, accessor, original) -> {
            EntityHitResult aimed = BossShellGuard.aimed();
            if (aimed == null) {
               return accessor;
            }

            Minecraft minecraft = Minecraft.getInstance();
            return (Accessor)(minecraft.level != null && minecraft.player != null
               ? registration.entityAccessor().level(minecraft.level).player(minecraft.player).hit(aimed).entity(aimed.getEntity()).build()
               : accessor);
         }
      );
   }
}

