package com.myx00x.maid.mixin;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractSoundInstance.class)
public interface SoundVolumeAccessor {
   @Accessor("f_119573_")
   void shenlin.setVolume(float var1);
}

