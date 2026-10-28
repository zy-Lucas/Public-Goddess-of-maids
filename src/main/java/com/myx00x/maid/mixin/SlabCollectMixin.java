package com.myx00x.maid.mixin;

import com.github.tartaricacid.touhoulittlemaid.api.event.InteractMaidEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.event.maid.SlabClickEvent;
import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import com.myx00x.maid.combat.GuardKeeper;
import com.myx00x.maid.combat.MaidCollectConsent;
import com.myx00x.maid.combat.MaidRemoval;
import com.myx00x.maid.combat.ShieldWatcher;
import com.myx00x.maid.combat.SlabAudit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SlabClickEvent.class, remap = false)
public class SlabCollectMixin {
   @Inject(method = "onInteract", at = @At("HEAD"), remap = false)
   private static void shenlin.markConsent(InteractMaidEvent event, CallbackInfo ci) {
      if ((Boolean)Config.GUARD_REWRITE.get()) {
         GuardKeeper.noZeroSkin(event.getMaid());
      }

      MaidCollectConsent.record(event.getPlayer(), event.getMaid());
   }

   @Inject(
      method = "onInteract",
      at = @At(
         value = "INVOKE",
         shift = Shift.BEFORE,
         target = "Lcom/github/tartaricacid/touhoulittlemaid/item/ItemSmartSlab;storeMaidData(Lnet/minecraft/world/item/ItemStack;Lcom/github/tartaricacid/touhoulittlemaid/entity/passive/EntityMaid;)V"
      ),
      remap = false,
      cancellable = true
   )
   private static void shenlin.fillBeforeStore(InteractMaidEvent event, CallbackInfo ci) {
      if ((Boolean)Config.GUARD_REWRITE.get()) {
         EntityMaid maid = event.getMaid();
         if (GuardKeeper.guards(maid)) {
            if (!MaidCollectConsent.consume(maid)) {
               ShenlinMod.LOGGER.info("[shenlin] maid {} held, collect without owner", maid.getUUID());
               ci.cancel();
            } else {
               GuardKeeper.refillForStore(maid);
            }
         }
      }
   }

   @Inject(
      method = "onInteract",
      at = @At(value = "INVOKE", shift = Shift.BEFORE, target = "Lcom/github/tartaricacid/touhoulittlemaid/entity/passive/EntityMaid;discard()V"),
      remap = false,
      cancellable = true
   )
   private static void shenlin.takeOverCollect(InteractMaidEvent event, CallbackInfo ci) {
      if ((Boolean)Config.GUARD_REWRITE.get()) {
         EntityMaid maid = event.getMaid();
         if (GuardKeeper.guards(maid)) {
            if (!GuardKeeper.takeByOwner(maid)) {
               ci.cancel();
            } else {
               ci.cancel();
               GuardKeeper.collectFeedback(event.getPlayer(), event.getStack(), maid);
            }
         }
      }
   }

   @Inject(
      method = "onInteract",
      at = @At(
         value = "INVOKE",
         shift = Shift.AFTER,
         target = "Lcom/github/tartaricacid/touhoulittlemaid/item/ItemSmartSlab;storeMaidData(Lnet/minecraft/world/item/ItemStack;Lcom/github/tartaricacid/touhoulittlemaid/entity/passive/EntityMaid;)V"
      ),
      remap = false
   )
   private static void shenlin.noteStored(InteractMaidEvent event, CallbackInfo ci) {
      SlabAudit.noteStored(event.getMaid());
   }

   @Inject(
      method = "onInteract",
      at = @At(
         value = "INVOKE",
         shift = Shift.AFTER,
         target = "Lcom/github/tartaricacid/touhoulittlemaid/item/ItemSmartSlab;storeMaidData(Lnet/minecraft/world/item/ItemStack;Lcom/github/tartaricacid/touhoulittlemaid/entity/passive/EntityMaid;)V"
      ),
      remap = false
   )
   private static void shenlin.purgeOnCollect(InteractMaidEvent event, CallbackInfo ci) {
      if (!(Boolean)Config.GUARD_REWRITE.get()) {
         EntityMaid maid = event.getMaid();
         if (ShieldWatcher.ensureGuard(maid)) {
            if (!MaidCollectConsent.consume(maid)) {
               ShieldWatcher.rejectFakeCollect(maid, event.getStack());
            } else {
               MaidRemoval.purge(maid);
            }
         }
      }
   }
}

