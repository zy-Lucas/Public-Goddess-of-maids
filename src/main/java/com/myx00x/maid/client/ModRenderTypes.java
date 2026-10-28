package com.myx00x.maid.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard.ShaderStateShard;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.resources.ResourceLocation;

public final class ModRenderTypes extends RenderType {
   public static final RenderType BLACK_HOLE = RenderType.create(
      "shenlin_black_hole",
      DefaultVertexFormat.NEW_ENTITY,
      Mode.TRIANGLES,
      256,
      false,
      true,
      CompositeState.builder()
         .setShaderState(new ShaderStateShard(ModShaders::blackHole))
         .setTextureState(NO_TEXTURE)
         .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
         .setCullState(NO_CULL)
         .setWriteMaskState(COLOR_WRITE)
         .setDepthTestState(NO_DEPTH_TEST)
         .createCompositeState(false)
   );
   public static final RenderType SHOCK_WARP = RenderType.create(
      "shenlin_shock_warp",
      DefaultVertexFormat.NEW_ENTITY,
      Mode.TRIANGLES,
      256,
      false,
      true,
      CompositeState.builder()
         .setShaderState(new ShaderStateShard(ModShaders::shockWarp))
         .setTextureState(NO_TEXTURE)
         .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
         .setCullState(NO_CULL)
         .setWriteMaskState(COLOR_WRITE)
         .setDepthTestState(NO_DEPTH_TEST)
         .createCompositeState(false)
   );
   public static final RenderType SLASH_RAY = RenderType.create(
      "shenlin_slash_ray",
      DefaultVertexFormat.NEW_ENTITY,
      Mode.TRIANGLES,
      256,
      false,
      true,
      CompositeState.builder()
         .setShaderState(new ShaderStateShard(ModShaders::slashRay))
         .setTextureState(NO_TEXTURE)
         .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
         .setCullState(NO_CULL)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );
   public static final RenderType STAR_GLOW = RenderType.create(
      "shenlin_star_glow",
      DefaultVertexFormat.NEW_ENTITY,
      Mode.TRIANGLES,
      256,
      false,
      true,
      CompositeState.builder()
         .setShaderState(new ShaderStateShard(ModShaders::starCore))
         .setTextureState(NO_TEXTURE)
         .setTransparencyState(ADDITIVE_TRANSPARENCY)
         .setCullState(NO_CULL)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );
   public static final RenderType STAR_GLOW_THROUGH = RenderType.create(
      "shenlin_star_glow_through",
      DefaultVertexFormat.NEW_ENTITY,
      Mode.TRIANGLES,
      256,
      false,
      true,
      CompositeState.builder()
         .setShaderState(new ShaderStateShard(ModShaders::starCore))
         .setTextureState(NO_TEXTURE)
         .setTransparencyState(ADDITIVE_TRANSPARENCY)
         .setCullState(NO_CULL)
         .setWriteMaskState(COLOR_WRITE)
         .setDepthTestState(NO_DEPTH_TEST)
         .createCompositeState(false)
   );
   public static final RenderType GLOW_THROUGH = RenderType.create(
      "shenlin_glow_through",
      DefaultVertexFormat.POSITION_COLOR,
      Mode.TRIANGLES,
      256,
      false,
      true,
      CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setTransparencyState(LIGHTNING_TRANSPARENCY).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).setDepthTestState(NO_DEPTH_TEST).createCompositeState(false)
   );
   public static final RenderType GLOW = RenderType.create(
      "shenlin_glow",
      DefaultVertexFormat.POSITION_COLOR,
      Mode.TRIANGLES,
      256,
      false,
      true,
      CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setTransparencyState(LIGHTNING_TRANSPARENCY).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false)
   );
   public static final RenderType GLOW_ALPHA = RenderType.create(
      "shenlin_glow_alpha",
      DefaultVertexFormat.POSITION_COLOR,
      Mode.TRIANGLES,
      256,
      false,
      true,
      CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false)
   );
   public static final RenderType FOG_VOLUME = RenderType.create(
      "shenlin_fog_volume",
      DefaultVertexFormat.POSITION_COLOR,
      Mode.TRIANGLES,
      256,
      false,
      true,
      CompositeState.builder()
         .setShaderState(new ShaderStateShard(ModShaders::volumetricFog))
         .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
         .setCullState(NO_CULL)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );
   public static final RenderType GLOW_ALPHA_THROUGH = RenderType.create(
      "shenlin_glow_alpha_through",
      DefaultVertexFormat.POSITION_COLOR,
      Mode.TRIANGLES,
      256,
      false,
      true,
      CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).setDepthTestState(NO_DEPTH_TEST).createCompositeState(false)
   );
   public static final Function<ResourceLocation, RenderType> GLOWING = Util.memoize(
      tex -> RenderType.create(
         "shenlin_glowing",
         DefaultVertexFormat.NEW_ENTITY,
         Mode.QUADS,
         256,
         true,
         true,
         CompositeState.builder()
            .setShaderState(RENDERTYPE_BEACON_BEAM_SHADER)
            .setTextureState(new TextureStateShard(tex, false, false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setOverlayState(OVERLAY)
            .setWriteMaskState(COLOR_WRITE)
            .setCullState(NO_CULL)
            .createCompositeState(false)
      )
   );

   private ModRenderTypes(
      String name, VertexFormat format, Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setup, Runnable clear
   ) {
      super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
   }
}

