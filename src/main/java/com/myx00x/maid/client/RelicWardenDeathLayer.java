package com.myx00x.maid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.myx00x.maid.mob.RelicWardenEntity;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelPart.Cube;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class RelicWardenDeathLayer extends RenderLayer<RelicWardenEntity, RelicWardenModel> {
   private static final RenderType DEATH_BODY = RenderType.eyes(
      ResourceLocation.fromNamespaceAndPath("goddess_of_maids", "textures/entity/relic_warden_death_body.png")
   );
   private static final float RED_R = 1.0F;
   private static final float RED_G = 0.16F;
   private static final float RED_B = 0.14F;
   private static final float BLUE_R = 0.25F;
   private static final float BLUE_G = 0.55F;
   private static final float BLUE_B = 1.0F;
   private static final int FULL_LIGHT = 15728640;
   private static final int SEED = 99537358;
   private static final int BRIGHT_SALT = -1640531527;
   private static Field cubesField;
   private static Field polygonsField;
   private static Field verticesField;
   private static Field normalField;
   private static Field posField;
   private static Field uField;
   private static Field vField;
   private static boolean looked;

   public RelicWardenDeathLayer(RenderLayerParent<RelicWardenEntity, RelicWardenModel> parent) {
      super(parent);
   }

   public void render(
      PoseStack pose,
      MultiBufferSource buffer,
      int packedLight,
      RelicWardenEntity entity,
      float limbSwing,
      float limbSwingAmount,
      float partialTicks,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
   ) {
      float show = entity.deathShow(partialTicks);
      if (!(show < 0.0F) && !entity.isInvisible()) {
         look();
         if (cubesField != null
            && polygonsField != null
            && verticesField != null
            && normalField != null
            && posField != null
            && uField != null
            && vField != null) {
            int tick = (int)show;
            VertexConsumer vertex = buffer.getBuffer(DEATH_BODY);
            int cell = 0;

            for (ModelPart part : ((RelicWardenModel)this.getParentModel()).root().getAllParts().toList()) {
               if (part.visible) {
                  pose.pushPose();
                  part.translateAndRotate(pose);

                  for (Object cube : cubesOf(part)) {
                     Object polygons = read(polygonsField, cube);
                     if (polygons != null) {
                        int faces = Array.getLength(polygons);

                        for (int face = 0; face < faces; face++) {
                           this.shade(Array.get(polygons, face), pose, vertex, mix(tick, cell * 7 + face));
                           cell++;
                        }
                     }
                  }

                  pose.popPose();
               }
            }
         }
      }
   }

   private void shade(Object polygon, PoseStack pose, VertexConsumer vertex, int roll) {
      Object corners = read(verticesField, polygon);
      if (corners != null && read(normalField, polygon) instanceof Vector3f normal) {
         boolean red = (roll & 1) == 0;
         float bright = 0.5F + 0.5F * (Math.floorMod(mix(roll, -1640531527) >>> 8, 101) / 100.0F);
         float r = (red ? 1.0F : 0.25F) * bright;
         float g = (red ? 0.16F : 0.55F) * bright;
         float b = (red ? 0.14F : 1.0F) * bright;
         Pose last = pose.last();
         Matrix4f m = last.pose();
         Matrix3f n = last.normal();
         int i = 0;

         for (int count = Array.getLength(corners); i < count; i++) {
            Object corner = Array.get(corners, i);
            if (read(posField, corner) instanceof Vector3f pos) {
               float v;
               float u;
               try {
                  u = uField.getFloat(corner);
                  v = vField.getFloat(corner);
               } catch (Throwable t) {
                  continue;
               }

               vertex.vertex(m, pos.x() / 16.0F, pos.y() / 16.0F, pos.z() / 16.0F)
                  .color(r, g, b, 1.0F)
                  .uv(u, v)
                  .overlayCoords(OverlayTexture.NO_OVERLAY)
                  .uv2(15728640)
                  .normal(n, normal.x(), normal.y(), normal.z())
                  .endVertex();
            }
         }
      }
   }

   private static List<?> cubesOf(ModelPart part) {
      return read(cubesField, part) instanceof List<?> list ? list : List.of();
   }

   private static Object read(Field field, Object owner) {
      try {
         return field == null ? null : field.get(owner);
      } catch (Throwable t) {
         return null;
      }
   }

   private static void look() {
      if (!looked) {
         looked = true;
         cubesField = find(ModelPart.class, "cubes", "f_104212_");
         polygonsField = find(Cube.class, "polygons", "f_104341_");
         if (polygonsField != null) {
            Class<?> polygonType = polygonsField.getType().getComponentType();
            verticesField = find(polygonType, "vertices", "f_104359_");
            normalField = find(polygonType, "normal", "f_104360_");
            if (verticesField != null) {
               Class<?> vertexType = verticesField.getType().getComponentType();
               posField = find(vertexType, "pos", "f_104371_");
               uField = find(vertexType, "u", "f_104372_");
               vField = find(vertexType, "v", "f_104373_");
            }
         }
      }
   }

   private static Field find(Class<?> owner, String... names) {
      for (String name : names) {
         try {
            Field f = owner.getDeclaredField(name);
            f.setAccessible(true);
            return f;
         } catch (Throwable var7) {
         }
      }

      return null;
   }

   private static int mix(int tick, int salt) {
      int h = tick * 374761393 + salt * 668265263 + 99537358;
      h = (h ^ h >>> 13) * 1274126177;
      return h ^ h >>> 16;
   }
}

