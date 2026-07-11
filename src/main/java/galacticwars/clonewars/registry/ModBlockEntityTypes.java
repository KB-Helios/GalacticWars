package galacticwars.clonewars.registry;

import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.settlement.CommandCenterBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, GalacticWars.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CommandCenterBlockEntity>> COMMAND_CENTER =
            BLOCK_ENTITY_TYPES.register("command_center", () -> new BlockEntityType<>(
                    CommandCenterBlockEntity::new,
                    ModBlocks.COMMAND_CENTER.get()));

    private ModBlockEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
