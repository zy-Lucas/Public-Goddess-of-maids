package com.myx00x.maid.client;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.myx00x.maid.compat.Upstream;
import com.myx00x.maid.item.ItemLookup;
import com.myx00x.maid.network.ModPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = "goddess_of_maids", value = Dist.CLIENT)
public final class MarkInput {
   private static final double REACH = 192.0;

   private MarkInput() {
   }

   @SubscribeEvent
   public static void onAttack(InteractionKeyMappingTriggered event) {
      if (Upstream.MAID) {
         if (event.isAttack()) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player != null && mc.level != null && holding(player)) {
               EntityHitResult hit = pick(player);
               if (hit != null) {
                  ModPackets.sendMark(hit.getEntity().getId());
               }
            }
         }
      }
   }

   private static boolean holding(Player player) {
      Item copier = ItemLookup.of("touhou_little_maid", "entity_id_copy");
      return copier != null && player.getMainHandItem().is(copier);
   }

   private static EntityHitResult pick(Player player) {
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getViewVector(1.0F);
      Vec3 end = eye.add(look.scale(192.0));
      AABB box = player.getBoundingBox().expandTowards(look.scale(192.0)).inflate(1.0);
      return ProjectileUtil.getEntityHitResult(player, eye, end, box, MarkInput::wanted, 36864.0);
   }

   private static boolean wanted(Entity entity) {
      return entity instanceof LivingEntity && !(entity instanceof Player) && !(entity instanceof EntityMaid) && entity.isPickable();
   }
}

