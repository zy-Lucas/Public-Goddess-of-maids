package com.myx00x.maid.compat;

import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class ShenlinMixinPlugin implements IMixinConfigPlugin {
   private static final Set<String> MAID_ONLY = Set.of(
      "ChunkUnloadMarkMixin",
      "DimensionMoveMarkMixin",
      "DropLootHoldMixin",
      "EntityHardRemovalMixin",
      "EntityMaidDieMixin",
      "EntityTickListMixin",
      "LevelCallbackMixin",
      "MaidAttackMarkMixin",
      "MaidContainerCloseMixin",
      "MaidContainerValidMixin",
      "MaidInteractMixin",
      "MaidRestrictRadiusMixin",
      "MaidSkinHurtMixin",
      "MaidTickMixin",
      "MeleePickTargetMixin",
      "PacketSendRemovalGateMixin",
      "PlayerListRemovalGateMixin",
      "ServantBellMixin",
      "ServerLevelEntityGetterMixin",
      "SlabCollectMixin",
      "SlabPlaceRelaxMixin",
      "SlabPriorityMixin",
      "SlabSpawnMixin"
   );
   private static final Set<String> SPELL_ONLY = Set.of("SilverCercisLinkMixin", "SpellRemoveGate", "SpellHardRemovalGate");
   private static final boolean MAID = loaded("touhou_little_maid");
   private static final boolean SPELL = MAID && loaded("touhou_little_maid_spell");

   private static boolean loaded(String modId) {
      try {
         LoadingModList list = FMLLoader.getLoadingModList();
         return list != null && list.getModFileById(modId) != null;
      } catch (Throwable t) {
         return false;
      }
   }

   public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
      String simple = mixinClassName.substring(mixinClassName.lastIndexOf(46) + 1);
      if (MAID_ONLY.contains(simple) && !MAID) {
         return false;
      } else if (SPELL_ONLY.contains(simple) && !SPELL) {
         return false;
      } else {
         return targetClassName.startsWith("com.github.tartaricacid") && !MAID ? false : !targetClassName.startsWith("com.github.yimeng261") || SPELL;
      }
   }

   public void onLoad(String mixinPackage) {
   }

   public String getRefMapperConfig() {
      return null;
   }

   public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
   }

   public List<String> getMixins() {
      return null;
   }

   public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }

   public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }
}

