package middleearth.lotr.warmod.registry;

import middleearth.lotr.warmod.KingdomWarsMiddleEarth;
import middleearth.lotr.warmod.settlement.KingdomHallBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, KingdomWarsMiddleEarth.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KingdomHallBlockEntity>> KINGDOM_HALL =
            BLOCK_ENTITY_TYPES.register("kingdom_hall", () -> new BlockEntityType<>(
                    KingdomHallBlockEntity::new,
                    ModBlocks.KINGDOM_HALL.get()));

    private ModBlockEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
