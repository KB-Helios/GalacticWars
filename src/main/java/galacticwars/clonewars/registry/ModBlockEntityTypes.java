package galacticwars.clonewars.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.settlement.CommandCenterBlockEntity;
import galacticwars.clonewars.world.BlueprintSiteAnchorBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

public final class ModBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(GalacticWars.MODID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<BlockEntityType<CommandCenterBlockEntity>> COMMAND_CENTER =
            BLOCK_ENTITY_TYPES.register("command_center", () -> new BlockEntityType<>(
                    CommandCenterBlockEntity::new,
                    Set.of(ModBlocks.COMMAND_CENTER.get())));
    public static final RegistrySupplier<BlockEntityType<BlueprintSiteAnchorBlockEntity>> BLUEPRINT_SITE_ANCHOR =
            BLOCK_ENTITY_TYPES.register("blueprint_site_anchor", () -> new BlockEntityType<>(
                    BlueprintSiteAnchorBlockEntity::new,
                    Set.of(ModBlocks.BLUEPRINT_SITE_ANCHOR.get())));

    private ModBlockEntityTypes() {
    }

    public static void register() {
        BLOCK_ENTITY_TYPES.register();
    }
}
