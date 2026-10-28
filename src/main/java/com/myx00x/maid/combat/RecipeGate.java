package com.myx00x.maid.combat;

import com.myx00x.maid.Config;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

public final class RecipeGate {
   private RecipeGate() {
   }

   private static boolean ours(Recipe<?> recipe) {
      if (recipe != null && recipe.getId() != null) {
         ResourceLocation id = recipe.getId();
         return "goddess_of_maids".equals(id.getNamespace()) && id.getPath().startsWith("altar/");
      } else {
         return false;
      }
   }

   public static <C extends Container, T extends Recipe<C>> T hold(RecipeType<T> type, T found) {
      return !Config.RECIPES_ENABLED.get() && ours(found) ? null : found;
   }

   public static <C extends Container, T extends Recipe<C>> List<T> filtered(RecipeType<T> type, List<T> list) {
      if (list != null && !list.isEmpty() && !(Boolean)Config.RECIPES_ENABLED.get()) {
         List<T> kept = new ArrayList<>(list.size());

         for (T one : list) {
            if (!ours(one)) {
               kept.add(one);
            }
         }

         return kept;
      } else {
         return list;
      }
   }
}

