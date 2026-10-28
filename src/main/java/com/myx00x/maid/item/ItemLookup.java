package com.myx00x.maid.item;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

public final class ItemLookup {
   private static final Map<String, Item> KNOWN = new ConcurrentHashMap<>();

   private ItemLookup() {
   }

   public static Item of(String namespace, String path) {
      String key = namespace + ":" + path;
      Item cached = KNOWN.get(key);
      if (cached != null) {
         return cached;
      }

      Item found = (Item)ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(namespace, path));
      if (found != null) {
         KNOWN.put(key, found);
      }

      return found;
   }
}

