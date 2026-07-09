package middleearth.lotr.warmod.registry;

import middleearth.lotr.warmod.KingdomWarsMiddleEarth;
import middleearth.lotr.warmod.entity.MiddleEarthRecruitEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntityTypes {
    public static final DeferredRegister.Entities ENTITY_TYPES =
            DeferredRegister.createEntities(KingdomWarsMiddleEarth.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<MiddleEarthRecruitEntity>> GONDOR_RECRUIT =
            registerRecruit("gondor_recruit");
    public static final DeferredHolder<EntityType<?>, EntityType<MiddleEarthRecruitEntity>> ROHAN_RECRUIT =
            registerRecruit("rohan_recruit");
    public static final DeferredHolder<EntityType<?>, EntityType<MiddleEarthRecruitEntity>> MORDOR_ORC_RECRUIT =
            registerRecruit("mordor_orc_recruit");
    public static final DeferredHolder<EntityType<?>, EntityType<MiddleEarthRecruitEntity>> DWARF_RECRUIT =
            registerRecruit("dwarf_recruit");
    public static final DeferredHolder<EntityType<?>, EntityType<MiddleEarthRecruitEntity>> ELF_RECRUIT =
            registerRecruit("elf_recruit");

    private ModEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }

    private static DeferredHolder<EntityType<?>, EntityType<MiddleEarthRecruitEntity>> registerRecruit(String name) {
        return ENTITY_TYPES.registerEntityType(
                name,
                MiddleEarthRecruitEntity::new,
                MobCategory.CREATURE,
                builder -> builder.sized(0.6F, 1.95F).clientTrackingRange(8).updateInterval(3));
    }
}
