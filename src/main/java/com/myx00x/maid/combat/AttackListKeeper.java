package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.data.inner.AttackListData;
import com.github.tartaricacid.touhoulittlemaid.entity.misc.MonsterType;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitTaskData;
import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

public final class AttackListKeeper {
   private AttackListKeeper() {
   }

   public static void ensure(EntityMaid maid, LivingEntity target) {
      if ((Boolean)Config.HATE_FILL_LIST.get() && !maid.level().isClientSide) {
         if (!(target instanceof Player) && !(target instanceof EntityMaid)) {
            ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
            if (type != null) {
               AttackListData data = (AttackListData)maid.getData(InitTaskData.ATTACK_LIST);
               if (data == null || !data.attackGroups().containsKey(type)) {
                  Map<ResourceLocation, MonsterType> groups = new HashMap<>();
                  if (data != null) {
                     groups.putAll(data.attackGroups());
                  }

                  groups.put(type, MonsterType.NEUTRAL);
                  maid.setAndSyncData(InitTaskData.ATTACK_LIST, new AttackListData(groups));
                  ShenlinMod.LOGGER.info("[shenlin] hate list add maid={} type={}", maid.getId(), type);
               }
            }
         }
      }
   }
}

