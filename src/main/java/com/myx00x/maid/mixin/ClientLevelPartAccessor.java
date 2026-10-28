package com.myx00x.maid.mixin;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ClientLevel.class, remap = false)
public interface ClientLevelPartAccessor {
   @Accessor("partEntities")
   Int2ObjectMap<PartEntity<?>> shenlin.partEntities();
}

