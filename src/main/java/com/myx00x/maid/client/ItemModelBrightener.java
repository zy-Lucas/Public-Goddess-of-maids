package com.myx00x.maid.client;

import com.myx00x.maid.ShenlinMod;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent.ModifyBakingResult;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT, bus = Bus.MOD)
public final class ItemModelBrightener {
   private static final Set<String> OURS = Set.of(
      "guardian_core",
      "protection_core",
      "coordinate_display",
      "black_hole_scroll",
      "guardian_shield",
      "devoted_heart",
      "energy_core",
      "hover_cannon",
      "teleport_wormhole"
   );
   private static boolean shenlin.logged;

   private ItemModelBrightener() {
   }

   @SubscribeEvent
   public static void onModifyBakingResult(ModifyBakingResult event) {
      Map<ResourceLocation, BakedModel> models = event.getModels();
      if (!shenlin.logged) {
         shenlin.logged = true;
         shenlin.dump(models, "ours(thin)", "devoted_heart");
         shenlin.dump(models, "vanilla(gen)", "energy_core");
      }

      for (Entry<ResourceLocation, BakedModel> entry : models.entrySet()) {
         ResourceLocation id = entry.getKey();
         if ("goddess_of_maids".equals(id.getNamespace()) && shenlin.ours(id.getPath())) {
            entry.setValue(new BrightBakedModel(entry.getValue()));
         }
      }
   }

   private static void shenlin.dump(Map<ResourceLocation, BakedModel> models, String label, String name) {
      BakedModel model = null;
      String keyText = "(none)";

      for (Entry<ResourceLocation, BakedModel> entry : models.entrySet()) {
         ResourceLocation id = entry.getKey();
         if ("goddess_of_maids".equals(id.getNamespace()) && shenlin.oursOne(id.getPath(), name)) {
            model = entry.getValue();
            keyText = id.toString();
            break;
         }
      }

      if (model == null) {
         ShenlinMod.LOGGER.info("[shenlin] model cmp {}: (not found {})", label, name);
      } else {
         ShenlinMod.LOGGER.info("[shenlin] model cmp {}: key={}", label, keyText);
         List<BakedQuad> quads = model.getQuads(null, null, RandomSource.create(42L));
         String quadInfo = "-";
         if (!quads.isEmpty()) {
            BakedQuad q = quads.get(0);
            int[] v = q.getVertices();
            quadInfo = "n="
               + quads.size()
               + " len="
               + v.length
               + " shade="
               + q.isShade()
               + " col0=0x"
               + Integer.toHexString(v.length > 3 ? v[3] : -1)
               + " dir="
               + q.getDirection();
         }

         ShenlinMod.LOGGER
            .info(
               "[shenlin] model cmp {}: gui3d={} ao={} blockLight={} custom={} quads[{}]",
               new Object[]{label, model.isGui3d(), model.useAmbientOcclusion(), model.usesBlockLight(), model.isCustomRenderer(), quadInfo}
            );
      }
   }

   private static boolean shenlin.ours(String path) {
      for (String name : OURS) {
         if (shenlin.oursOne(path, name)) {
            return true;
         }
      }

      return false;
   }

   private static boolean shenlin.oursOne(String path, String name) {
      return path.equals(name) || path.endsWith("/" + name);
   }
}

