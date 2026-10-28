package com.myx00x.maid.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = GameRenderer.class, remap = false)
public interface GameRendererAccessor {
   @Invoker("m_109141_")
   double shenlin.getFov(Camera var1, float var2, boolean var3);

   @Invoker("m_109138_")
   void shenlin.bobView(PoseStack var1, float var2);

   @Invoker("m_109117_")
   void shenlin.bobHurt(PoseStack var1, float var2);
}

