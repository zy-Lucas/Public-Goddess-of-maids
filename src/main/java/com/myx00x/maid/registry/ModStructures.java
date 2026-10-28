package com.myx00x.maid.registry;

import com.myx00x.maid.world.MossyRelicPiece;
import com.myx00x.maid.world.MossyRelicStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModStructures {
   public static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, "goddess_of_maids");
   public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, "goddess_of_maids");
   public static final RegistryObject<StructureType<MossyRelicStructure>> MOSSY_RELIC = TYPES.register("mossy_relic", () -> () -> MossyRelicStructure.CODEC);
   public static final RegistryObject<StructurePieceType> MOSSY_RELIC_PIECE = PIECES.register("mossy_relic_piece", () -> MossyRelicPiece::new);

   private ModStructures() {
   }

   public static void register(IEventBus bus) {
      TYPES.register(bus);
      PIECES.register(bus);
   }
}

