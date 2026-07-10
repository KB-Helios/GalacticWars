package middleearth.lotr.warmod;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import middleearth.lotr.warmod.entity.MiddleEarthRecruitEntity;
import middleearth.lotr.warmod.gametest.ModGameTests;
import middleearth.lotr.warmod.registry.ModBlocks;
import middleearth.lotr.warmod.registry.ModBlockEntityTypes;
import middleearth.lotr.warmod.registry.ModCreativeTabs;
import middleearth.lotr.warmod.registry.ModEntityTypes;
import middleearth.lotr.warmod.registry.ModItems;
import middleearth.lotr.warmod.registry.ModMenuTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(KingdomWarsMiddleEarth.MODID)
public class KingdomWarsMiddleEarth {
    public static final String MODID = "kingdomwarsmiddleearth";
    public static final Logger LOGGER = LogUtils.getLogger();

    public KingdomWarsMiddleEarth(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerEntityAttributes);
        modEventBus.addListener(this::registerSpawnPlacements);
        modEventBus.addListener(ModGameTests::registerTestFunctions);
        modEventBus.addListener(ModGameTests::registerGameTests);

        ModBlocks.register(modEventBus);
        ModBlockEntityTypes.register(modEventBus);
        ModEntityTypes.register(modEventBus);
        ModItems.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        if (Config.LOG_STARTUP.getAsBoolean()) {
            LOGGER.info("KingdomWars-Middle-Earth foundation loaded.");
        }
    }

    private void registerEntityAttributes(EntityAttributeCreationEvent event) {
        registerRecruitAttributes(event, ModEntityTypes.GONDOR_RECRUIT.get());
        registerRecruitAttributes(event, ModEntityTypes.ROHAN_RECRUIT.get());
        registerRecruitAttributes(event, ModEntityTypes.MORDOR_ORC_RECRUIT.get());
        registerRecruitAttributes(event, ModEntityTypes.DWARF_RECRUIT.get());
        registerRecruitAttributes(event, ModEntityTypes.ELF_RECRUIT.get());
    }

    private void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        registerRecruitSpawnPlacement(event, ModEntityTypes.GONDOR_RECRUIT.get());
        registerRecruitSpawnPlacement(event, ModEntityTypes.ROHAN_RECRUIT.get());
        registerRecruitSpawnPlacement(event, ModEntityTypes.MORDOR_ORC_RECRUIT.get());
        registerRecruitSpawnPlacement(event, ModEntityTypes.DWARF_RECRUIT.get());
        registerRecruitSpawnPlacement(event, ModEntityTypes.ELF_RECRUIT.get());
    }

    private static void registerRecruitAttributes(
            EntityAttributeCreationEvent event,
            EntityType<? extends MiddleEarthRecruitEntity> entityType
    ) {
        event.put(entityType, MiddleEarthRecruitEntity.createAttributes().build());
    }

    private static void registerRecruitSpawnPlacement(
            RegisterSpawnPlacementsEvent event,
            EntityType<MiddleEarthRecruitEntity> entityType
    ) {
        event.register(
                entityType,
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
