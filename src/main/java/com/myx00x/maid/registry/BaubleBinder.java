package com.myx00x.maid.registry;

import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.LittleMaidExtension;
import com.github.tartaricacid.touhoulittlemaid.item.bauble.BaubleManager;
import com.myx00x.maid.bauble.BlackHoleScrollBauble;
import com.myx00x.maid.bauble.DevotedHeartBauble;
import com.myx00x.maid.bauble.EnergyCoreBauble;
import com.myx00x.maid.bauble.FinalSlashBauble;
import com.myx00x.maid.bauble.GuardianShieldBauble;
import com.myx00x.maid.bauble.HoverCannonBauble;
import com.myx00x.maid.bauble.ProtectionCoreBauble;
import com.myx00x.maid.bauble.TeleportWormholeBauble;

@LittleMaidExtension
public class BaubleBinder implements ILittleMaid {
   public void bindMaidBauble(BaubleManager manager) {
      manager.bind(ModItems.FINAL_SLASH, new FinalSlashBauble());
      manager.bind(ModItems.GUARDIAN_SHIELD, new GuardianShieldBauble());
      manager.bind(ModItems.PROTECTION_CORE, new ProtectionCoreBauble());
      manager.bind(ModItems.DEVOTED_HEART, new DevotedHeartBauble());
      manager.bind(ModItems.BLACK_HOLE_SCROLL, new BlackHoleScrollBauble());
      manager.bind(ModItems.ENERGY_CORE, new EnergyCoreBauble());
      manager.bind(ModItems.HOVER_CANNON, new HoverCannonBauble());
      manager.bind(ModItems.TELEPORT_WORMHOLE, new TeleportWormholeBauble());
   }
}

