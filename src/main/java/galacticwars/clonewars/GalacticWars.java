package galacticwars.clonewars;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.combat.BlasterCombatEvents;
import galacticwars.clonewars.gametest.ModGameTests;
import galacticwars.clonewars.registry.ModBlocks;
import galacticwars.clonewars.registry.ModBlockEntityTypes;
import galacticwars.clonewars.registry.ModCreativeTabs;
import galacticwars.clonewars.registry.ModDataComponents;
import galacticwars.clonewars.registry.ModEntityTypes;
import galacticwars.clonewars.registry.ModItems;
import galacticwars.clonewars.registry.ModMenuTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(GalacticWars.MODID)
public class GalacticWars {
    public static final String MODID = "galacticwars";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GalacticWars(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerEntityAttributes);
        modEventBus.addListener(this::registerSpawnPlacements);
        modEventBus.addListener(ModGameTests::registerTestFunctions);
        modEventBus.addListener(ModGameTests::registerGameTests);

        ModBlocks.register(modEventBus);
        ModBlockEntityTypes.register(modEventBus);
        ModEntityTypes.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModItems.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(BlasterCombatEvents::onProjectileImpact);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        if (Config.LOG_STARTUP.getAsBoolean()) {
            LOGGER.info("Galactic Wars: Clone Wars foundation loaded.");
        }
    }

    private void registerEntityAttributes(EntityAttributeCreationEvent event) {
        ModEntityTypes.recruits().forEach(holder -> registerRecruitAttributes(event, holder.get()));
    }

    private void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        ModEntityTypes.recruits().forEach(holder -> registerRecruitSpawnPlacement(event, holder.get()));
    }

    private static void registerRecruitAttributes(
            EntityAttributeCreationEvent event,
            EntityType<? extends GalacticRecruitEntity> entityType
    ) {
        event.put(entityType, GalacticRecruitEntity.createAttributes().build());
    }

    private static void registerRecruitSpawnPlacement(
            RegisterSpawnPlacementsEvent event,
            EntityType<GalacticRecruitEntity> entityType
    ) {
        event.register(
                entityType,
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
