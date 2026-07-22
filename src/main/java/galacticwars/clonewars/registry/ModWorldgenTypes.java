package galacticwars.clonewars.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.world.BlueprintStructure;
import galacticwars.clonewars.world.BlueprintStructurePiece;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

public final class ModWorldgenTypes {
    private static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(GalacticWars.MODID, Registries.STRUCTURE_TYPE);
    private static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
            DeferredRegister.create(GalacticWars.MODID, Registries.STRUCTURE_PIECE);

    public static final RegistrySupplier<StructureType<BlueprintStructure>> BLUEPRINT_STRUCTURE =
            STRUCTURE_TYPES.register("blueprint_structure", () -> () -> BlueprintStructure.CODEC);
    public static final RegistrySupplier<StructurePieceType> BLUEPRINT_STRUCTURE_PIECE =
            STRUCTURE_PIECES.register("blueprint_structure", () -> BlueprintStructurePiece::new);

    private ModWorldgenTypes() {
    }

    public static void register() {
        STRUCTURE_TYPES.register();
        STRUCTURE_PIECES.register();
    }
}
