package com.myx00x.maid.client;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public final class BrightBakedModel implements BakedModel {
   private static final int VERTEX_STRIDE = 8;
   private final BakedModel inner;
   private final Map<BakedQuad, BakedQuad> cache = new IdentityHashMap<>();

   public BrightBakedModel(BakedModel inner) {
      this.inner = inner;
   }

   public List<BakedQuad> getQuads(BlockState state, Direction direction, RandomSource random) {
      List<BakedQuad> source = this.inner.getQuads(state, direction, random);
      if (source.isEmpty()) {
         return source;
      }

      List<BakedQuad> result = new ArrayList<>(source.size());

      for (BakedQuad quad : source) {
         result.add(this.unshaded(quad));
      }

      return result;
   }

   private BakedQuad unshaded(BakedQuad quad) {
      return this.cache
         .computeIfAbsent(
            quad,
            key -> {
               int[] vertices = key.getVertices();
               return vertices.length != 0 && vertices.length % 8 == 0
                  ? new BakedQuad((int[])vertices.clone(), key.getTintIndex(), key.getDirection(), key.getSprite(), false)
                  : key;
            }
         );
   }

   public boolean useAmbientOcclusion() {
      return false;
   }

   public boolean isGui3d() {
      return false;
   }

   public boolean usesBlockLight() {
      return false;
   }

   public boolean isCustomRenderer() {
      return this.inner.isCustomRenderer();
   }

   public TextureAtlasSprite getParticleIcon() {
      return this.inner.getParticleIcon();
   }

   public ItemTransforms getTransforms() {
      return this.inner.getTransforms();
   }

   public ItemOverrides getOverrides() {
      return this.inner.getOverrides();
   }
}

