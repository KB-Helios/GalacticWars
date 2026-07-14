package galacticwars.clonewars.gametest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import galacticwars.clonewars.Config;
import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.army.ArmyFormation;
import galacticwars.clonewars.army.ArmyGroupLifecycleState;
import galacticwars.clonewars.army.ArmyLocation;
import galacticwars.clonewars.army.ArmyTravelService;
import galacticwars.clonewars.combat.BlasterCombatEvents;
import galacticwars.clonewars.combat.BlasterBoltEntity;
import galacticwars.clonewars.combat.FactionRangedWeaponService;
import galacticwars.clonewars.combat.BlasterHeatPolicy;
import galacticwars.clonewars.combat.BlasterItem;
import galacticwars.clonewars.combat.LightsaberDeflectionService;
import galacticwars.clonewars.economy.CreditTransactionService;
import galacticwars.clonewars.economy.PhysicalTradeService;
import galacticwars.clonewars.data.GameplayDataManager;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.vehicle.GalacticVehicleEntity;
import galacticwars.clonewars.conquest.ConquestCaptureService;
import galacticwars.clonewars.conquest.ConquestControlState;
import galacticwars.clonewars.conquest.ConquestSavedData;
import galacticwars.clonewars.entity.RecruitSpawnEggItem;
import galacticwars.clonewars.entity.RecruitLifecycleService;
import galacticwars.clonewars.faction.FactionAlignmentSavedData;
import galacticwars.clonewars.faction.FactionId;
import galacticwars.clonewars.faction.FactionRelation;
import galacticwars.clonewars.force.ForceWorldEffectService;
import galacticwars.clonewars.kingdom.BuildProject;
import galacticwars.clonewars.kingdom.CommandCenterDashboardState;
import galacticwars.clonewars.kingdom.KingdomRecord;
import galacticwars.clonewars.kingdom.KingdomClaim;
import galacticwars.clonewars.kingdom.KingdomActionId;
import galacticwars.clonewars.kingdom.KingdomGameplayAction;
import galacticwars.clonewars.kingdom.KingdomGameplayResult;
import galacticwars.clonewars.kingdom.KingdomGameplayRuntimeService;
import galacticwars.clonewars.kingdom.KingdomFactionRelations;
import galacticwars.clonewars.kingdom.KingdomMemberRole;
import galacticwars.clonewars.kingdom.KingdomPermission;
import galacticwars.clonewars.kingdom.KingdomRelation;
import galacticwars.clonewars.kingdom.KingdomSiege;
import galacticwars.clonewars.kingdom.SiegeState;
import galacticwars.clonewars.kingdom.SettlementRecord;
import galacticwars.clonewars.kingdom.KingdomSavedData;
import galacticwars.clonewars.kingdom.RecruitmentCampaign;
import galacticwars.clonewars.kingdom.RecruitmentCampaignDecision;
import galacticwars.clonewars.kingdom.RecruitmentCampaignState;
import galacticwars.clonewars.kingdom.StorageEndpoint;
import galacticwars.clonewars.kingdom.WorkOrder;
import galacticwars.clonewars.kingdom.WorkOrderState;
import galacticwars.clonewars.kingdom.WorkOrderType;
import galacticwars.clonewars.kingdom.WorksiteRecord;
import galacticwars.clonewars.item.CommandTargetSelection;
import galacticwars.clonewars.menu.RecruitCommandMenu;
import galacticwars.clonewars.menu.CommandCenterOperationsMenu;
import galacticwars.clonewars.menu.CommandCenterOperationsMenuProvider;
import galacticwars.clonewars.menu.CommandCenterNavigationMenu;
import galacticwars.clonewars.menu.FactionSelectionMenu;
import galacticwars.clonewars.menu.MerchantTradeMenu;
import galacticwars.clonewars.menu.MerchantTradeMenuProvider;
import galacticwars.clonewars.progression.LaunchContentCatalog;
import galacticwars.clonewars.progression.ProgressionEvent;
import galacticwars.clonewars.progression.ProgressionDecision;
import galacticwars.clonewars.progression.ProgressionEventType;
import galacticwars.clonewars.progression.ProgressionSavedData;
import galacticwars.clonewars.progression.ProgressionState;
import galacticwars.clonewars.progression.ForceSavedData;
import galacticwars.clonewars.recruitment.RecruitmentAction;
import galacticwars.clonewars.recruitment.RecruitDuty;
import galacticwars.clonewars.recruitment.RecruitmentPaymentService;
import galacticwars.clonewars.recruitment.NpcServiceBranch;
import galacticwars.clonewars.registry.ModBlockTags;
import galacticwars.clonewars.registry.ModBlocks;
import galacticwars.clonewars.registry.ModEntityTypes;
import galacticwars.clonewars.registry.ModItems;
import galacticwars.clonewars.registry.ModDataComponents;
import galacticwars.clonewars.settlement.CommandCenterBlockEntity;
import galacticwars.clonewars.settlement.ConstructionPlan;
import galacticwars.clonewars.settlement.ConstructionProjectService;
import galacticwars.clonewars.settlement.KingdomBaseBlueprint;
import galacticwars.clonewars.workforce.WorkerPhase;
import galacticwars.clonewars.workforce.WorkerProfession;
import galacticwars.clonewars.world.PlanetTravelService;
import galacticwars.clonewars.world.PlanetArrivalService;
import galacticwars.clonewars.world.FactionOutpostRecord;
import galacticwars.clonewars.world.FactionOutpostSavedData;
import galacticwars.clonewars.world.FactionNaturalSpawnEvents;
import galacticwars.clonewars.world.FactionNaturalSpawnRules;
import galacticwars.clonewars.world.OverworldFactionSpawnProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.SpawnEggItemBehavior;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.tslat.smartbrainlib.util.BrainUtil;

public final class ModGameTests {
    private static final Identifier ENVIRONMENT = id("gameplay");
    private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");
    private static final int SMART_BRAIN_TEST_SETUP_INTERVAL = 300;
    private static final Map<Identifier, Consumer<GameTestHelper>> TESTS = createTests();

    private ModGameTests() {
    }

    public static void registerTestFunctions(RegisterEvent event) {
        if (!GameTestHooks.isGametestEnabled()) {
            return;
        }
        event.register(Registries.TEST_FUNCTION, helper -> TESTS.forEach(helper::register));
    }

    public static void registerGameTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                ENVIRONMENT,
                new TestEnvironmentDefinition.AllOf(List.of()));
        Map<Identifier, Holder<TestEnvironmentDefinition<?>>> isolatedEnvironments =
                new LinkedHashMap<>(Map.of(
                id("ungrouped_recruit_ranged_brain"), event.registerEnvironment(
                        id("ungrouped_recruit_ranged_brain_environment"),
                        new TestEnvironmentDefinition.AllOf(List.of())),
                id("ungrouped_recruit_melee_brain"), event.registerEnvironment(
                        id("ungrouped_recruit_melee_brain_environment"),
                        new TestEnvironmentDefinition.AllOf(List.of())),
                id("smart_brain_follow_and_sit"), event.registerEnvironment(
                        id("smart_brain_follow_and_sit_environment"),
                        new TestEnvironmentDefinition.AllOf(List.of())),
                id("smart_brain_move_command"), event.registerEnvironment(
                        id("smart_brain_move_command_environment"),
                        new TestEnvironmentDefinition.AllOf(List.of())),
                id("smart_brain_move_stall_recovery"), event.registerEnvironment(
                        id("smart_brain_move_stall_recovery_environment"),
                        new TestEnvironmentDefinition.AllOf(List.of())),
                id("natural_civilian_brain_runtime"), event.registerEnvironment(
                        id("natural_civilian_brain_runtime_environment"),
                        new TestEnvironmentDefinition.AllOf(List.of())),
                id("grouped_brain_authority"), event.registerEnvironment(
                        id("grouped_brain_authority_environment"),
                        new TestEnvironmentDefinition.AllOf(List.of())),
                id("local_recruit_protect_owner"), event.registerEnvironment(
                        id("local_recruit_protect_owner_environment"),
                        new TestEnvironmentDefinition.AllOf(List.of())),
                id("command_marker_runtime"), event.registerEnvironment(
                        id("command_marker_runtime_environment"),
                        new TestEnvironmentDefinition.AllOf(List.of())),
                id("vehicle_embodied_runtime"), event.registerEnvironment(
                        id("vehicle_embodied_runtime_environment"),
                        new TestEnvironmentDefinition.AllOf(List.of()))));
        isolatedEnvironments.put(id("force_embodied_runtime"), event.registerEnvironment(
                        id("force_embodied_runtime_environment"),
                        new TestEnvironmentDefinition.AllOf(List.of())));
        List<Identifier> smartBrainRuntimeTests = List.of(
                id("ungrouped_recruit_ranged_brain"),
                id("ungrouped_recruit_melee_brain"),
                id("smart_brain_follow_and_sit"),
                id("smart_brain_move_command"),
                id("smart_brain_move_stall_recovery"),
                id("natural_civilian_brain_runtime"),
                id("grouped_brain_authority"),
                id("local_recruit_protect_owner"),
                id("command_marker_runtime"));
        for (Identifier testId : TESTS.keySet()) {
            int timeoutTicks = testId.equals(id("smart_brain_move_stall_recovery"))
                    ? 520
                    : Set.of(
                    id("smart_brain_move_command"),
                    id("grouped_brain_authority")).contains(testId)
                    ? 260
                    : testId.equals(id("smart_brain_follow_and_sit"))
                            ? 360
                            : testId.equals(id("command_marker_runtime"))
                                    ? 180
                                    : testId.equals(id("vehicle_embodied_runtime"))
                                            ? 260
                                    : testId.equals(id("chapter_three_campaign_runtime"))
                                            ? 260
                                    : Set.of(
                                            id("faction_selection_transaction"),
                                            id("natural_civilian_brain_runtime")).contains(testId)
                                            ? 160
                                            : 100;
            int runtimeTestIndex = smartBrainRuntimeTests.indexOf(testId);
            TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                    isolatedEnvironments.getOrDefault(testId, environment),
                    EMPTY_STRUCTURE,
                    timeoutTicks,
                    runtimeTestIndex < 0
                            ? 0
                            : runtimeTestIndex * SMART_BRAIN_TEST_SETUP_INTERVAL,
                    true,
                    Rotation.NONE,
                    false,
                    1,
                    1,
                    false,
                    0);
            event.registerTest(testId, new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, testId),
                    data));
        }
    }

    private static Map<Identifier, Consumer<GameTestHelper>> createTests() {
        LinkedHashMap<Identifier, Consumer<GameTestHelper>> tests = new LinkedHashMap<>();
        tests.put(id("command_center_player_interaction"),
                ModGameTests::commandCenterPlayerInteraction);
        tests.put(id("command_center_authority"), ModGameTests::kingdomHallAuthority);
        tests.put(id("command_center_multiplayer_invite"),
                ModGameTests::commandCenterMultiplayerInvite);
        tests.put(id("command_center_squad_orders"), ModGameTests::commandCenterSquadOrders);
        tests.put(id("command_center_workforce_control"), ModGameTests::commandCenterWorkforceControl);
        tests.put(id("command_marker_runtime"), ModGameTests::commandMarkerRuntime);
        tests.put(id("blueprint_projector_runtime"), ModGameTests::blueprintProjectorRuntime);
        tests.put(id("kingdom_governance_persistence"), ModGameTests::kingdomGovernancePersistence);
        tests.put(id("kingdom_multiplayer_runtime"), ModGameTests::kingdomMultiplayerRuntime);
        tests.put(id("overworld_faction_outpost_runtime"), ModGameTests::overworldFactionOutpostRuntime);
        tests.put(id("physical_trade_transaction"), ModGameTests::physicalTradeTransaction);
        tests.put(id("faction_selection_transaction"), ModGameTests::factionSelectionTransaction);
        tests.put(id("progression_runtime_adapter"), ModGameTests::progressionRuntimeAdapter);
        tests.put(id("class_ability_runtime_data"), ModGameTests::classAbilityRuntimeData);
        tests.put(id("vehicle_runtime_contract"), ModGameTests::vehicleRuntimeContract);
        tests.put(id("vehicle_embodied_runtime"), ModGameTests::vehicleEmbodiedRuntime);
        tests.put(id("force_embodied_runtime"), ModGameTests::forceEmbodiedRuntime);
        tests.put(id("chapter_three_campaign_runtime"), ModGameTests::chapterThreeCampaignRuntime);
        tests.put(id("all_faction_campaign_paths"), ModGameTests::allFactionCampaignPaths);
        tests.put(id("conquest_control_authority"), ModGameTests::conquestControlAuthority);
        tests.put(id("recruit_entity_contract"), ModGameTests::recruitEntityContract);
        tests.put(id("worker_tags_and_loot"), ModGameTests::workerTagsAndLoot);
        tests.put(id("recruit_contract_lifecycle"), ModGameTests::recruitContractLifecycle);
        tests.put(id("local_recruit_protect_owner"), ModGameTests::localRecruitProtectOwner);
        tests.put(id("worker_resource_conservation"), ModGameTests::workerResourceConservation);
        tests.put(id("enabled_worker_loops"), ModGameTests::enabledWorkerLoops);
        tests.put(id("specialist_worker_loops"), ModGameTests::specialistWorkerLoops);
        tests.put(id("animal_farmer_species_pairing"), ModGameTests::animalFarmerSpeciesPairing);
        tests.put(id("workforce_saved_data_authority"), ModGameTests::workforceSavedDataAuthority);
        tests.put(id("recruit_spawn_eggs"), ModGameTests::recruitSpawnEggs);
        tests.put(id("blaster_friendly_fire"), ModGameTests::blasterFriendlyFire);
        tests.put(id("lightsaber_bolt_deflection"), ModGameTests::lightsaberBoltDeflection);
        tests.put(id("recruit_blaster_projectile"), ModGameTests::recruitBlasterProjectile);
        tests.put(id("player_blaster_heat"), ModGameTests::playerBlasterHeat);
        tests.put(id("ungrouped_recruit_ranged_brain"), ModGameTests::ungroupedRecruitRangedBrain);
        tests.put(id("ungrouped_recruit_melee_brain"), ModGameTests::ungroupedRecruitMeleeBrain);
        tests.put(id("smart_brain_follow_and_sit"), ModGameTests::smartBrainFollowAndSit);
        tests.put(id("smart_brain_move_command"), ModGameTests::smartBrainMoveCommand);
        tests.put(id("smart_brain_move_stall_recovery"), ModGameTests::smartBrainMoveStallRecovery);
        tests.put(id("natural_civilian_brain_runtime"), ModGameTests::naturalCivilianBrainRuntime);
        tests.put(id("grouped_brain_authority"), ModGameTests::groupedBrainAuthority);
        tests.put(id("external_library_runtime"), ModGameTests::externalLibraryRuntime);
        tests.put(id("planet_travel_failure_atomicity"), ModGameTests::planetTravelFailureAtomicity);
        tests.put(id("planet_arrival_runtime"), ModGameTests::planetArrivalRuntime);
        tests.put(id("army_planet_transfer_transaction"), ModGameTests::armyPlanetTransferTransaction);
        return Map.copyOf(tests);
    }

    private static void classAbilityRuntimeData(GameTestHelper helper) {
        var snapshot = GameplayDataManager.snapshot();
        if (snapshot.unitClasses().size() != 15
                || snapshot.abilities().size() != 30
                || snapshot.factionPolicies().size() != 5
                || snapshot.launchContent().planets().size() != 4
                || snapshot.launchContent().vehicles().size() != 5
                || snapshot.launchContent().forceAbilities().size() != 6
                || snapshot.launchContent().quests().size() != 15
                || snapshot.launchContent().trades().size() != 5
                || snapshot.launchContent().conquestRegions().size() != 4
                || snapshot.launchContent().forceAbilities().values().stream()
                        .anyMatch(ability -> !ability.enabled())) {
            helper.fail("Atomic gameplay snapshot did not load the launch class catalogs");
            return;
        }
        GalacticRecruitEntity recruit = helper.spawn(
                ModEntityTypes.CLONE_TROOPER.get(), new BlockPos(2, 1, 2));
        recruit.initializeFromSpawnEgg();
        if (!recruit.classProgressState().classId().equals("galacticwars:clone_trooper")) {
            helper.fail("Recruit did not derive its shared class state from the unit catalog");
            return;
        }
        var accepted = recruit.activateClassAbility(
                "galacticwars:suppressive_fire", helper.getLevel().getGameTime(),
                true, 8.0D, false);
        var replay = recruit.activateClassAbility(
                "galacticwars:suppressive_fire", helper.getLevel().getGameTime(),
                true, 8.0D, false);
        if (!accepted.accepted()
                || accepted.state().resource() != 85
                || replay.accepted()
                || !replay.reason().equals("ability_cooldown")) {
            helper.fail("Recruit class activation was not server-authoritative and cooldown-safe");
            return;
        }
        helper.succeed();
    }

    private static void vehicleRuntimeContract(GameTestHelper helper) {
        UUID owner = UUID.randomUUID();
        int lane = 1;
        for (var holder : ModEntityTypes.vehicles()) {
            GalacticVehicleEntity vehicle = helper.spawn(holder.get(), new BlockPos(lane++, 1, 2));
            vehicle.deploy(owner, "galacticwars:republic");
            var definition = GameplayDataManager.snapshot().launchContent().vehicles()
                    .get(vehicle.vehicleId());
            if (definition == null || vehicle.ownerId().filter(owner::equals).isEmpty()
                    || vehicle.fuel() != definition.fuelCapacity()
                    || vehicle.health() != definition.maxHealth()
                    || definition.seatRoles().size() != definition.seats()
                    || definition.fabricationInputs().isEmpty()) {
                helper.fail("Vehicle did not load its authoritative deployment contract: "
                        + vehicle.vehicleId());
                return;
            }
        }
        helper.succeed();
    }

    private static void conquestControlAuthority(GameTestHelper helper) {
        String region = "gametest_" + UUID.randomUUID();
        ConquestControlState state = new ConquestControlState(region,
                helper.getLevel().dimension().identifier().toString(), 4, 2, 4,
                "galacticwars:republic", UUID.randomUUID().toString(), "", 120, 3L);
        ConquestSavedData data = ConquestSavedData.get(helper.getLevel());
        data.put(state);
        if (!data.state(region).filter(state::equals).isPresent()) {
            helper.fail("Conquest control was not retained by authoritative saved data");
            return;
        }
        helper.succeed();
    }

    private static void progressionRuntimeAdapter(GameTestHelper helper) {
        UUID playerId = UUID.randomUUID();
        ProgressionSavedData progression = ProgressionSavedData.get(helper.getLevel());
        KingdomGameplayAction pledge = new KingdomGameplayAction(
                KingdomActionId.of("pledge", playerId),
                playerId,
                ProgressionEventType.FACTION_PLEDGED,
                "galacticwars:republic",
                1);
        KingdomGameplayResult first = KingdomGameplayRuntimeService.applyProgression(progression, pledge);
        KingdomGameplayResult retry = KingdomGameplayRuntimeService.applyProgression(progression, pledge);
        if (!first.accepted() || !first.changed()
                || !retry.accepted() || retry.changed()
                || !retry.reason().equals("duplicate_action")
                || !progression.state(playerId).factionId().equals("galacticwars:republic")) {
            helper.fail("Progression runtime adapter did not commit exactly once");
            return;
        }
        helper.succeed();
    }

    private static void factionSelectionTransaction(GameTestHelper helper) {
        BlockPos hallPos = isolatedCapital(helper, 6);
        CommandCenterBlockEntity hall = placeCommandCenter(helper, hallPos);
        ServerPlayer owner = makeConnectedMockPlayer(helper, GameType.SURVIVAL);
        owner.setPos(hall.getBlockPos().getX() + 0.5D, hall.getBlockPos().getY(), hall.getBlockPos().getZ() + 0.5D);
        if (!hall.claim(owner)) {
            helper.fail("Faction selection setup could not claim the Command Center");
            return;
        }
        FactionSelectionMenu menu = new FactionSelectionMenu(0, owner.getInventory(), hall.getBlockPos());
        int separatistButton = menu.factionIds().indexOf("galacticwars:separatist");
        if (separatistButton < 0 || !menu.clickMenuButton(owner, separatistButton)) {
            helper.fail("Server rejected the Separatist faction selection");
            return;
        }
        KingdomRecord kingdom = KingdomSavedData.get(helper.getLevel())
                .kingdomForOwner(owner.getUUID()).orElse(null);
        String pledged = ProgressionSavedData.get(helper.getLevel()).state(owner.getUUID()).factionId();
        int alignment = FactionAlignmentSavedData.get(helper.getLevel()).alignment(owner.getUUID())
                .score(FactionId.of("galacticwars:separatist"));
        if (kingdom == null
                || !kingdom.factionId().equals("galacticwars:separatist")
                || !hall.factionId().equals("galacticwars:separatist")
                || !pledged.equals("galacticwars:separatist")
                || alignment <= 0) {
            helper.fail("Faction selection did not atomically bind kingdom, Hall, progression, and alignment");
            return;
        }
        FactionSelectionMenu replay = new FactionSelectionMenu(1, owner.getInventory(), hall.getBlockPos());
        int differentFactionButton = separatistButton == 0 ? 1 : 0;
        if (differentFactionButton >= replay.factionIds().size()
                || replay.clickMenuButton(owner, differentFactionButton)
                || !KingdomSavedData.get(helper.getLevel()).kingdomForOwner(owner.getUUID())
                        .orElseThrow().factionId().equals("galacticwars:separatist")) {
            helper.fail("A second faction selection changed the committed kingdom faction");
            return;
        }
        ProgressionSavedData progression = ProgressionSavedData.get(helper.getLevel());
        if (!progression.state(owner.getUUID()).hasSubjectPath(
                        ProgressionEventType.BUILDING_COMPLETED, "command_center")
                || progression.state(owner.getUUID()).hasSubject(
                        ProgressionEventType.QUEST_ADVANCED, "separatist_chapter_1")) {
            helper.fail("Faction pledge did not preserve the Command Center objective or advanced too early");
            return;
        }
        owner.getInventory().add(new ItemStack(ModItems.CREDIT_CHIP.get(), 64));
        GalacticRecruitEntity firstRecruit = spawnRecruitAt(
                helper, ModEntityTypes.B1_BATTLE_DROID.get(), hallPos.offset(2, 0, 0));
        firstRecruit.setNoAi(true);
        boolean[] scenarioRun = {false};
        helper.onEachTick(() -> {
            if (scenarioRun[0]) {
                return;
            }
            if (helper.getLevel().getEntity(firstRecruit.getUUID()) != firstRecruit) {
                if (helper.getTick() >= 120L) {
                    scenarioRun[0] = true;
                    helper.fail("Chapter-one recruit never entered the server index within 120 ticks");
                }
                return;
            }
            scenarioRun[0] = true;
            owner.setPos(firstRecruit.getX(), firstRecruit.getY(), firstRecruit.getZ());
            boolean hired = firstRecruit.handleMenuButton(owner, RecruitCommandMenu.BUTTON_HIRE);
            ProgressionState completed = progression.state(owner.getUUID());
            owner.setPos(hallPos.getX() + 0.5D, hallPos.getY(), hallPos.getZ() + 0.5D);
            CommandCenterOperationsMenu operations = (CommandCenterOperationsMenu)
                    new CommandCenterOperationsMenuProvider(hallPos)
                            .createMenu(2, owner.getInventory(), owner);
            boolean chapterVisible = operations.dashboardState().activeQuest()
                    .filter(quest -> quest.questId().equals("separatist_chapter_2"))
                    .isPresent();
            int creditsBeforeClaim = RecruitmentPaymentService.creditCount(owner);
            boolean rewardClaimed = operations.handleReplayAction(
                    owner, UUID.randomUUID(), CommandCenterOperationsMenu.CLAIM_REWARDS);
            int creditsAfterClaim = RecruitmentPaymentService.creditCount(owner);
            boolean duplicateRejected = !operations.handleReplayAction(
                    owner, UUID.randomUUID(), CommandCenterOperationsMenu.CLAIM_REWARDS);
            if (!hired
                    || !completed.hasSubject(
                    ProgressionEventType.QUEST_ADVANCED, "separatist_chapter_1")
                    || completed.pendingCreditRewards() != 40
                    || !chapterVisible
                    || !rewardClaimed
                    || creditsAfterClaim != creditsBeforeClaim + 40
                    || progression.pendingCreditRewards(owner.getUUID()) != 0
                    || !duplicateRejected) {
                helper.fail("Fresh survival chapter one did not complete and pay exactly once: hired="
                        + hired + ", completed=" + completed.hasSubject(
                        ProgressionEventType.QUEST_ADVANCED, "separatist_chapter_1")
                        + ", pending=" + completed.pendingCreditRewards()
                        + ", chapterVisible=" + chapterVisible
                        + ", rewardClaimed=" + rewardClaimed
                        + ", credits=" + creditsBeforeClaim + "->" + creditsAfterClaim
                        + ", remaining=" + progression.pendingCreditRewards(owner.getUUID())
                        + ", duplicateRejected=" + duplicateRejected);
                return;
            }
            helper.succeed();
        });
    }

    private static void planetArrivalRuntime(GameTestHelper helper) {
        BlockPos first = PlanetArrivalService.findOrCreate(helper.getLevel()).orElse(null);
        BlockPos repeated = PlanetArrivalService.findOrCreate(helper.getLevel()).orElse(null);
        if (first == null || !first.equals(repeated)) {
            helper.fail("Planet arrival platform was unavailable or not stably reused");
            return;
        }
        if (!helper.getLevel().getBlockState(first.below()).is(ModBlocks.DURACRETE.get())
                || !helper.getLevel().getBlockState(first).canBeReplaced()
                || !helper.getLevel().getBlockState(first.above()).canBeReplaced()) {
            helper.fail("Planet arrival platform did not provide a solid floor and clear player volume");
            return;
        }
        helper.succeed();
    }

    private static void planetTravelFailureAtomicity(GameTestHelper helper) {
        BlockPos hallPos = isolatedCapital(helper, 7);
        CommandCenterBlockEntity hall = placeCommandCenter(helper, hallPos);
        ServerPlayer owner = makeConnectedMockPlayer(helper, GameType.CREATIVE);
        hall.claim(owner);
        hall.setFaction("galacticwars:republic");
        KingdomSavedData.get(helper.getLevel()).foundKingdom(
                owner.getUUID(), hall.factionId(),
                helper.getLevel().dimension().identifier().toString(), hallPos);
        ProgressionSavedData progression = ProgressionSavedData.get(helper.getLevel());
        progression.apply(new ProgressionEvent(
                UUID.randomUUID(), owner.getUUID(), ProgressionEventType.FACTION_PLEDGED,
                "galacticwars:republic", 1));
        progression.apply(new ProgressionEvent(
                UUID.randomUUID(), owner.getUUID(), ProgressionEventType.BUILDING_COMPLETED,
                "forward_base", 1));

        CommandCenterNavigationMenu navigation = new CommandCenterNavigationMenu(
                0, owner.getInventory());
        if (!navigation.planetIds().equals(GameplayDataManager.snapshot().launchContent().planetIds())) {
            helper.fail("Navigation menu did not capture the authoritative planet snapshot");
            return;
        }

        PlanetTravelService.TravelResult result = PlanetTravelService.travel(owner, "tatooine");
        if (result.accepted() || !result.reason().equals("destination_unavailable")) {
            helper.fail("Missing GameTest destination was not rejected atomically: " + result.reason());
            return;
        }
        if (owner.level() != helper.getLevel()) {
            helper.fail("Failed planet travel moved the player");
            return;
        }
        if (progression.state(owner.getUUID()).hasSubject(ProgressionEventType.PLANET_VISITED, "tatooine")) {
            helper.fail("Failed planet travel emitted a visited progression event");
            return;
        }
        helper.succeed();
    }

    private static void recruitBlasterProjectile(GameTestHelper helper) {
        ServerPlayer owner = makeConnectedMockPlayer(helper, GameType.CREATIVE);
        GalacticRecruitEntity shooter = helper.spawn(
                ModEntityTypes.CLONE_TROOPER.get(), new BlockPos(2, 1, 2));
        GalacticRecruitEntity target = helper.spawn(
                ModEntityTypes.B1_BATTLE_DROID.get(), new BlockPos(12, 1, 2));
        shooter.tame(owner);
        ItemStack weapon = new ItemStack(ModItems.DC15_BLASTER.get());
        shooter.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        ModItems.DC15_BLASTER.get().fireAt(helper.getLevel(), shooter, target, weapon);

        GalacticRecruitEntity archer = helper.spawn(
                ModEntityTypes.NIGHTSISTER_ARCHER.get(), new BlockPos(2, 1, 4));
        GalacticRecruitEntity bowTarget = helper.spawn(
                ModEntityTypes.CLONE_TROOPER.get(), new BlockPos(12, 1, 4));
        archer.tame(owner);
        ItemStack bow = new ItemStack(ModItems.NIGHTSISTER_BOW.get());
        archer.setItemInHand(InteractionHand.MAIN_HAND, bow);
        FactionRangedWeaponService.fireNightsisterBow(helper.getLevel(), archer, bowTarget, bow);
        AABB blasterShotArea = shooter.getBoundingBox().inflate(20.0D);
        AABB bowShotArea = archer.getBoundingBox().inflate(20.0D);
        helper.succeedWhen(() -> {
            List<BlasterBoltEntity> bolts = helper.getLevel().getEntitiesOfClass(
                    BlasterBoltEntity.class, blasterShotArea, bolt -> bolt.getOwner() == shooter);
            if (bolts.size() != 1
                    || bolts.getFirst().pickup
                            != net.minecraft.world.entity.projectile.arrow.AbstractArrow.Pickup.DISALLOWED
                    || !bolts.getFirst().getWeaponItem().is(ModItems.DC15_BLASTER.get())) {
                helper.fail("Recruit blaster did not spawn one owned, weapon-tagged bolt");
            }
            List<Arrow> arrows = helper.getLevel().getEntitiesOfClass(
                    Arrow.class, bowShotArea, arrow -> arrow.getOwner() == archer);
            if (arrows.size() != 1 || !arrows.getFirst().getWeaponItem().is(ModItems.NIGHTSISTER_BOW.get())) {
                helper.fail("Nightsister Archer did not spawn one owned, bow-tagged ranged projectile");
            }
        });
    }

    private static void armyPlanetTransferTransaction(GameTestHelper helper) {
        BlockPos hallPos = isolatedCapital(helper, 8);
        CommandCenterBlockEntity hall = placeCommandCenter(helper, hallPos);
        ServerPlayer owner = makeConnectedMockPlayer(helper, GameType.CREATIVE);
        hall.claim(owner);
        hall.setFaction("galacticwars:republic");
        KingdomSavedData data = KingdomSavedData.get(helper.getLevel());
        data.foundKingdom(owner.getUUID(), hall.factionId(),
                helper.getLevel().dimension().identifier().toString(), hallPos);
        KingdomBaseBlueprint forwardBase = GameplayDataManager.snapshot()
                .blueprint("galacticwars:forward_base").orElseThrow();
        BuildProject completedBase = fullyProgressProject(
                data, owner.getUUID(), forwardBase,
                helper.getLevel().dimension().identifier().toString(), hallPos.offset(0, 16, 0));
        if (!data.completeBuildProject(owner.getUUID(), completedBase, forwardBase)) {
            helper.fail("Army travel setup could not unlock a commander slot");
            return;
        }

        FactionAlignmentSavedData.get(helper.getLevel()).setScore(
                owner.getUUID(), FactionId.of("republic"), 100);
        GalacticRecruitEntity commander = helper.spawn(
                ModEntityTypes.CLONE_TROOPER.get(), new BlockPos(2, 1, 2));
        GalacticRecruitEntity member = helper.spawn(
                ModEntityTypes.ARC_TROOPER.get(), new BlockPos(3, 1, 2));
        commander.tick();
        member.tick();
        owner.setPos(commander.getX(), commander.getY(), commander.getZ());
        if (!commander.handleMenuButton(owner, RecruitCommandMenu.BUTTON_HIRE)
                || !member.handleMenuButton(owner, RecruitCommandMenu.BUTTON_HIRE)
                || !data.promoteCommander(owner.getUUID(), commander.getUUID())) {
            helper.fail("Army travel setup could not hire and promote its squad");
            return;
        }
        var group = data.createOrReclaimArmyGroup(
                owner.getUUID(), commander.getUUID(), ArmyFormation.LINE,
                new ArmyLocation(helper.getLevel().dimension().identifier().toString(),
                        commander.getX(), commander.getY(), commander.getZ()),
                helper.getLevel().getGameTime()).orElseThrow();
        BlockPos arrival = hallPos.offset(0, 32, 0);

        ArmyTravelService.TravelPlan rollbackPlan = ArmyTravelService.prepare(
                data, owner, helper.getLevel(), arrival);
        boolean reserved = rollbackPlan.reserve();
        boolean rolledBack = reserved && rollbackPlan.rollback(helper.getLevel().getGameTime());
        if (!rollbackPlan.accepted() || !rollbackPlan.transfersSquad() || !reserved || !rolledBack) {
            helper.fail("Army travel reservation could not be rolled back atomically; accepted="
                    + rollbackPlan.accepted() + ", reason=" + rollbackPlan.reason()
                    + ", transfers=" + rollbackPlan.transfersSquad()
                    + ", reserved=" + reserved + ", rolledBack=" + rolledBack);
            return;
        }
        var restored = data.armyGroup(group.id()).orElseThrow();
        if (restored.simulation().lifecycleState() != ArmyGroupLifecycleState.LIVE
                || commander.isRemoved() || member.isRemoved()) {
            helper.fail("Army travel rollback changed live squad entities");
            return;
        }

        ArmyTravelService.TravelPlan committedPlan = ArmyTravelService.prepare(
                data, owner, helper.getLevel(), arrival);
        if (!committedPlan.reserve()) {
            helper.fail("Army travel transaction could not reserve its second attempt");
            return;
        }
        committedPlan.commit();
        var transferred = data.armyGroup(group.id()).orElseThrow();
        if (transferred.simulation().lifecycleState() != ArmyGroupLifecycleState.VIRTUAL
                || transferred.snapshots().size() != 2
                || transferred.simulation().anchor().blockPosition().x() != arrival.getX()
                || !commander.isRemoved()
                || !member.isRemoved()) {
            helper.fail("Army travel commit did not virtualize, relocate, and remove the source squad");
            return;
        }
        UUID lateRecruit = UUID.randomUUID();
        if (!data.registerRecruit(owner.getUUID(), lateRecruit)
                || data.addRecruitToArmy(owner.getUUID(), group.id(), lateRecruit)
                || data.addRecruitToArmy(owner.getUUID(), lateRecruit)
                || data.armyGroup(group.id()).orElseThrow().snapshots().size() != 2) {
            helper.fail("Virtual squad accepted a member without an authoritative materialization snapshot");
            return;
        }
        helper.succeed();
    }

    private static void playerBlasterHeat(GameTestHelper helper) {
        ServerPlayer player = makeConnectedMockPlayer(helper, GameType.SURVIVAL);
        ItemStack weapon = new ItemStack(ModItems.DC15_BLASTER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        player.getInventory().add(new ItemStack(ModItems.ENERGY_CELL.get(), 8));

        for (int shot = 0; shot < BlasterHeatPolicy.SHOTS_BEFORE_OVERHEAT; shot++) {
            InteractionResult result = ModItems.DC15_BLASTER.get().use(
                    helper.getLevel(), player, InteractionHand.MAIN_HAND);
            if (result != InteractionResult.SUCCESS) {
                helper.fail("Player blaster rejected legal shot " + shot);
                return;
            }
            for (int tick = 0; tick < BlasterHeatPolicy.SHOT_COOLDOWN_TICKS; tick++) {
                ModItems.DC15_BLASTER.get().inventoryTick(
                        weapon, helper.getLevel(), player, EquipmentSlot.MAINHAND);
            }
        }

        BlasterHeatPolicy.BlasterHeatState overheated = weapon.get(ModDataComponents.BLASTER_HEAT.get());
        if (overheated == null || overheated.overheatTicks() <= 0
                || ModItems.DC15_BLASTER.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND)
                        != InteractionResult.FAIL) {
            helper.fail("Player blaster did not persist or enforce its overheat state");
            return;
        }
        for (int tick = 0; tick < BlasterHeatPolicy.OVERHEAT_TICKS; tick++) {
            ModItems.DC15_BLASTER.get().inventoryTick(
                    weapon, helper.getLevel(), player, EquipmentSlot.MAINHAND);
        }
        if (!BlasterItem.heat(weapon).isReady()) {
            helper.fail("Player blaster did not return to ready after venting");
            return;
        }
        helper.succeed();
    }

    private static void ungroupedRecruitRangedBrain(GameTestHelper helper) {
        SmartBrainTestArea area = prepareSmartBrainTestArea(
                helper, GameType.CREATIVE, -2, 6, -2, 6);
        GalacticRecruitEntity shooter = spawnRecruitAt(
                helper, ModEntityTypes.CLONE_TROOPER.get(), area.at(1, 1, 1));
        GalacticRecruitEntity target = spawnRecruitAt(
                helper, ModEntityTypes.B1_BATTLE_DROID.get(), area.at(4, 1, 1));
        target.setNoAi(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024.0D);
        target.setHealth(target.getMaxHealth());
        shooter.initializeNaturalFactionNpc(UUID.randomUUID(), NpcServiceBranch.MILITARY);
        shooter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DC15_BLASTER.get()));
        if (!(shooter.getBrain() instanceof net.tslat.smartbrainlib.api.internal.SmartBrain<?>)) {
            helper.fail("Recruit did not receive a SmartBrainLib brain");
            return;
        }
        AABB shotArea = shooter.getBoundingBox().inflate(20.0D);
        helper.succeedWhen(() -> {
            List<BlasterBoltEntity> bolts = helper.getLevel().getEntitiesOfClass(
                    BlasterBoltEntity.class, shotArea, bolt -> bolt.getOwner() == shooter);
            if (bolts.isEmpty()) {
                helper.fail("Ungrouped ranged recruit brain did not acquire and attack its enemy: nearest="
                        + BrainUtil.getMemory(shooter,
                        net.minecraft.world.entity.ai.memory.MemoryModuleType.NEAREST_ATTACKABLE)
                        + ", attack=" + BrainUtil.getMemory(shooter,
                        net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET)
                        + ", target=" + shooter.getTarget()
                        + ", tickCount=" + shooter.tickCount
                        + ", running=" + shooter.getBrain().getRunningBehaviors());
                return;
            }
            if (!bolts.isEmpty()
                    && !bolts.getFirst().getWeaponItem().is(ModItems.DC15_BLASTER.get())) {
                helper.fail("Ungrouped ranged recruit brain fired a bolt without its weapon identity");
                return;
            }
            shooter.discard();
            target.discard();
            bolts.forEach(BlasterBoltEntity::discard);
        });
    }

    private static void ungroupedRecruitMeleeBrain(GameTestHelper helper) {
        SmartBrainTestArea area = prepareSmartBrainTestArea(
                helper, GameType.CREATIVE, 0, 6, 0, 4);
        GalacticRecruitEntity attacker = spawnRecruitAt(
                helper, ModEntityTypes.CLONE_TROOPER.get(), area.at(1, 1, 1));
        GalacticRecruitEntity target = spawnRecruitAt(
                helper, ModEntityTypes.B1_BATTLE_DROID.get(), area.at(3, 1, 1));
        target.setNoAi(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024.0D);
        target.setHealth(target.getMaxHealth());
        attacker.initializeNaturalFactionNpc(UUID.randomUUID(), NpcServiceBranch.MILITARY);
        attacker.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        helper.succeedWhen(() -> {
            if (attacker.getLastHurtMob() == null) {
                helper.fail("Ungrouped melee recruit brain did not acquire and strike its enemy");
                return;
            }
            attacker.discard();
            target.discard();
        });
    }

    private static void smartBrainFollowAndSit(GameTestHelper helper) {
        SmartBrainTestArea area = prepareSmartBrainTestArea(
                helper, GameType.CREATIVE, -4, 12, -6, 10);
        ServerPlayer owner = area.player();
        GalacticRecruitEntity recruit = spawnRecruitAt(
                helper, ModEntityTypes.CLONE_TROOPER.get(), area.at(1, 1, 2));
        recruit.setInvulnerable(true);
        recruit.tame(owner);
        BlockPos ownerPos = area.at(8, 1, 2);
        owner.setNoGravity(true);
        owner.setYRot(-90.0F);
        long indexWaitStartedTick = helper.getTick();
        int[] phase = {0};
        int[] phaseStartedRecruitTick = {0};
        Vec3[] startingPosition = {Vec3.ZERO};
        Vec3[] heldPosition = {Vec3.ZERO};
        double[] startingDistance = {0.0D};
        helper.onEachTick(() -> {
            if (phase[0] == 0) {
                if (helper.getLevel().getEntity(recruit.getUUID()) != recruit) {
                    if (helper.getTick() - indexWaitStartedTick >= 120L) {
                        phase[0] = 4;
                        helper.fail("SmartBrain companion never entered the server entity index");
                    }
                    return;
                }
                owner.setPos(ownerPos.getX() + 0.5D, ownerPos.getY(), ownerPos.getZ() + 0.5D);
                invokeRecruitCommand(recruit, RecruitmentAction.FOLLOW_OWNER);
                startingPosition[0] = recruit.position();
                startingDistance[0] = recruit.distanceToSqr(owner);
                phaseStartedRecruitTick[0] = recruit.tickCount;
                phase[0] = 1;
                return;
            }
            if (phase[0] == 1 && recruit.tickCount - phaseStartedRecruitTick[0] >= 140) {
                boolean approachedOwner = recruit.distanceToSqr(owner) < startingDistance[0] - 4.0D;
                boolean followedAnchor = recruit.position().distanceToSqr(startingPosition[0]) > 4.0D
                        && BrainUtil.hasMemory(recruit,
                        net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET);
                if (!approachedOwner && !followedAnchor) {
                    phase[0] = 4;
                    helper.fail("SmartBrain companion behaviour did not follow its owner: position="
                            + recruit.position() + ", owner=" + owner.position()
                            + ", start=" + startingPosition[0]
                            + ", command=" + recruit.getRecruitCommand()
                            + ", shouldFollow=" + recruit.shouldUseCompanionAi()
                            + ", walkMemory=" + BrainUtil.hasMemory(recruit,
                            net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET)
                            + ", pathMemory=" + BrainUtil.hasMemory(recruit,
                            net.minecraft.world.entity.ai.memory.MemoryModuleType.PATH)
                            + ", navigationDone=" + recruit.getNavigation().isDone()
                            + ", recruitTicks=" + (recruit.tickCount - phaseStartedRecruitTick[0])
                            + ", path=" + pathState(recruit, owner.blockPosition()));
                    return;
                }
                recruit.setWorkerProfession(WorkerProfession.FARMER);
                invokeRecruitCommand(recruit, RecruitmentAction.HOLD_POSITION);
                heldPosition[0] = recruit.position();
                phaseStartedRecruitTick[0] = recruit.tickCount;
                phase[0] = 2;
                return;
            }
            if (phase[0] == 2 && recruit.tickCount - phaseStartedRecruitTick[0] >= 20) {
                double heldDeltaX = recruit.getX() - heldPosition[0].x();
                double heldDeltaZ = recruit.getZ() - heldPosition[0].z();
                if (!recruit.isOrderedToSit()
                        || heldDeltaX * heldDeltaX + heldDeltaZ * heldDeltaZ > 0.25D) {
                    phase[0] = 4;
                    helper.fail("SmartBrain sit behaviour did not hold the recruit in place: position="
                            + recruit.position() + ", held=" + heldPosition[0]
                            + ", command=" + recruit.getRecruitCommand()
                            + ", orderedSit=" + recruit.isOrderedToSit()
                            + ", group=" + recruit.getArmyGroupId()
                            + ", velocity=" + recruit.getDeltaMovement()
                            + ", running=" + recruit.getBrain().getRunningBehaviors());
                    return;
                }
                invokeRecruitCommand(recruit, RecruitmentAction.CLEAR_TARGET);
                phaseStartedRecruitTick[0] = recruit.tickCount;
                phase[0] = 3;
                return;
            }
            if (phase[0] == 3 && recruit.tickCount - phaseStartedRecruitTick[0] >= 5) {
                phase[0] = 4;
                if (recruit.isOrderedToSit()) {
                    helper.fail("Clear-target command incorrectly kept the recruit sitting");
                    return;
                }
                recruit.discard();
                helper.succeed();
            }
        });
    }

    private static void smartBrainMoveCommand(GameTestHelper helper) {
        SmartBrainTestArea area = prepareSmartBrainTestArea(
                helper, GameType.CREATIVE, -2, 6, -2, 6);
        ServerPlayer owner = area.player();
        GalacticRecruitEntity recruit = spawnRecruitAt(
                helper, ModEntityTypes.CLONE_TROOPER.get(), area.at(1, 1, 2));
        recruit.setInvulnerable(true);
        recruit.tame(owner);
        BlockPos target = area.at(3, 1, 4);
        setRecruitField(recruit, "moveTarget", target);
        invokeRecruitCommand(recruit, RecruitmentAction.MOVE_TO_POSITION);
        helper.succeedWhen(() -> {
            if (recruit.distanceToMoveTargetSqr() > 4.0D) {
                helper.fail("SmartBrain move command did not reach its explicit target: position="
                        + recruit.position() + ", target=" + target
                        + ", shouldMove=" + recruit.shouldMoveToCommandTarget()
                        + ", walkMemory=" + BrainUtil.hasMemory(recruit,
                        net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET)
                        + ", navigationDone=" + recruit.getNavigation().isDone()
                        + ", pathMemory=" + BrainUtil.hasMemory(recruit,
                        net.minecraft.world.entity.ai.memory.MemoryModuleType.PATH)
                        + ", pathRegistered=" + recruit.getBrain().checkMemory(
                        net.minecraft.world.entity.ai.memory.MemoryModuleType.PATH,
                        net.minecraft.world.entity.ai.memory.MemoryStatus.REGISTERED)
                        + ", running=" + recruit.getBrain().getRunningBehaviors()
                        + ", path=" + pathState(recruit, target));
                return;
            }
            recruit.discard();
        });
    }

    private static void smartBrainMoveStallRecovery(GameTestHelper helper) {
        SmartBrainTestArea area = prepareSmartBrainTestArea(
                helper, GameType.CREATIVE, -2, 4, -2, 4);
        ServerPlayer owner = area.player();
        GalacticRecruitEntity recruit = spawnRecruitAt(
                helper, ModEntityTypes.CLONE_TROOPER.get(), area.at(1, 1, 1));
        recruit.setInvulnerable(true);
        recruit.tame(owner);
        BlockPos unreachable = area.at(1, 20, 1);
        setRecruitField(recruit, "moveTarget", unreachable);
        invokeRecruitCommand(recruit, RecruitmentAction.MOVE_TO_POSITION);
        boolean[] finished = {false};
        helper.onEachTick(() -> {
            if (finished[0]) {
                return;
            }
            if (recruit.tickCount >= 200
                    && recruit.isAlive()
                    && recruit.distanceToMoveTargetSqr() > 4.0D
                    && recruit.getNavigation().isDone()
                    && !BrainUtil.hasMemory(recruit,
                    net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET)) {
                finished[0] = true;
                recruit.discard();
                helper.succeed();
            } else if (recruit.tickCount >= 240) {
                finished[0] = true;
                helper.fail("Unreachable move command did not enter bounded retry backoff: tickCount="
                        + recruit.tickCount + ", walkMemory="
                        + BrainUtil.hasMemory(recruit,
                        net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET)
                        + ", navigationDone=" + recruit.getNavigation().isDone()
                        + ", running=" + recruit.getBrain().getRunningBehaviors());
            }
        });
    }

    private static void naturalCivilianBrainRuntime(GameTestHelper helper) {
        SmartBrainTestArea area = prepareSmartBrainTestArea(
                helper, GameType.CREATIVE, -4, 8, -4, 8);
        NaturalCivilianTestScenario scenario = new NaturalCivilianTestScenario(helper, area);
        helper.onEachTick(scenario::tick);
    }

    private static void verifyNaturalCivilianWork(
            GameTestHelper helper,
            GalacticRecruitEntity sheltering,
            GalacticRecruitEntity workingCivilian,
            BlockPos storage
    ) {
        if (!(helper.getLevel().getBlockEntity(storage) instanceof Container container)
                || countContainerItems(container) == 0) {
            helper.fail("Natural civilian work behaviour did not deliver settlement supplies: position="
                    + workingCivilian.position() + ", workstation="
                    + workingCivilian.naturalWorkstationPosition() + ", profession="
                    + workingCivilian.getWorkerProfession() + ", natural="
                    + workingCivilian.isNaturalFactionCivilian() + ", home="
                    + workingCivilian.hasHome() + ", orderedSit="
                    + workingCivilian.isOrderedToSit() + ", bright="
                    + workingCivilian.level().isBrightOutside() + ", tickCount="
                    + workingCivilian.tickCount + ", walkMemory="
                    + BrainUtil.hasMemory(workingCivilian,
                    net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET)
                    + ", navigationDone=" + workingCivilian.getNavigation().isDone()
                    + ", productionAt="
                    + getRecruitField(workingCivilian, "nextNaturalProductionGameTime")
                    + ", gameTime=" + workingCivilian.level().getGameTime()
                    + ", running=" + workingCivilian.getBrain().getRunningBehaviors());
            return;
        }
        sheltering.discard();
        workingCivilian.discard();
        helper.succeed();
    }

    private static void groupedBrainAuthority(GameTestHelper helper) {
        SmartBrainTestArea area = prepareSmartBrainTestArea(
                helper, GameType.CREATIVE, 0, 4, 0, 4);
        GalacticRecruitEntity recruit = spawnRecruitAt(
                helper, ModEntityTypes.CLONE_TROOPER.get(), area.at(1, 1, 1));
        GalacticRecruitEntity target = spawnRecruitAt(
                helper, ModEntityTypes.B1_BATTLE_DROID.get(), area.at(2, 1, 1));
        long indexWaitStartedTick = helper.getTick();
        int[] phase = {0};
        int[] scenarioStartedRecruitTick = {0};
        helper.onEachTick(() -> {
            if (phase[0] == 0) {
                boolean recruitIndexed = helper.getLevel().getEntity(recruit.getUUID()) == recruit;
                boolean targetIndexed = helper.getLevel().getEntity(target.getUUID()) == target;
                if (!recruitIndexed || !targetIndexed) {
                    if (helper.getTick() - indexWaitStartedTick >= 120L) {
                        phase[0] = 2;
                        helper.fail("Grouped SmartBrain entities never entered the server entity index: recruit="
                                + recruitIndexed + ", target=" + targetIndexed);
                    }
                    return;
                }
                setRecruitField(recruit, "armyGroupId", UUID.randomUUID());
                BrainUtil.setTargetOfEntity(recruit, target);
                BrainUtil.setMemory(recruit,
                        net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET,
                        new net.minecraft.world.entity.ai.memory.WalkTarget(target, 1.0F, 1));
                scenarioStartedRecruitTick[0] = recruit.tickCount;
                phase[0] = 1;
                return;
            }
            if (phase[0] == 1 && recruit.tickCount - scenarioStartedRecruitTick[0] >= 80) {
                phase[0] = 2;
                if (BrainUtil.hasMemory(recruit,
                        net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET)
                        || BrainUtil.hasMemory(recruit,
                        net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET)) {
                    helper.fail("Grouped recruit retained stale local SmartBrain targets: tickCount="
                            + recruit.tickCount + ", running="
                            + recruit.getBrain().getRunningBehaviors());
                    return;
                }
                recruit.discard();
                target.discard();
                helper.succeed();
            }
        });
    }

    private static void externalLibraryRuntime(GameTestHelper helper) {
        GalacticRecruitEntity recruit = helper.spawn(
                ModEntityTypes.CLONE_TROOPER.get(), new BlockPos(2, 1, 2));
        if (!net.neoforged.fml.ModList.get().isLoaded("smartbrainlib")
                || !net.neoforged.fml.ModList.get().isLoaded("framework")) {
            helper.fail("Required SmartBrainLib or Framework runtime was not discovered");
            return;
        }
        if (!(recruit.getBrain() instanceof net.tslat.smartbrainlib.api.internal.SmartBrain<?>)) {
            helper.fail("Recruit did not receive SmartBrainLib's runtime brain");
            return;
        }
        if (!recruit.getBrain().checkMemory(
                net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET,
                net.minecraft.world.entity.ai.memory.MemoryStatus.REGISTERED)
                || !recruit.getBrain().checkMemory(
                net.minecraft.world.entity.ai.memory.MemoryModuleType.PATH,
                net.minecraft.world.entity.ai.memory.MemoryStatus.REGISTERED)) {
            helper.fail("SmartBrain movement memories were not registered");
            return;
        }
        Identifier expectedChannel = Identifier.fromNamespaceAndPath("galacticwars", "main");
        if (!galacticwars.clonewars.network.GalacticNetwork.CHANNEL.id().equals(expectedChannel)) {
            helper.fail("Framework channel was not built with the galacticwars:main identifier");
            return;
        }
        if (!codecRoundTrips(
                        new galacticwars.clonewars.network.ForceActivatePayload(UUID.randomUUID(), 2),
                        galacticwars.clonewars.network.ForceActivatePayload.STREAM_CODEC)
                || !codecRoundTrips(
                        new galacticwars.clonewars.network.VehicleInputPayload(
                                UUID.randomUUID(), 42, 0.75F, -0.5F, true, false, true),
                        galacticwars.clonewars.network.VehicleInputPayload.STREAM_CODEC)
                || !codecRoundTrips(
                        new galacticwars.clonewars.network.MenuActionPayload(
                                UUID.randomUUID(), 7, 255,
                                Optional.of(UUID.randomUUID()), Optional.of(UUID.randomUUID())),
                        galacticwars.clonewars.network.MenuActionPayload.STREAM_CODEC)
                || !codecRoundTrips(
                        new galacticwars.clonewars.network.ForceHudPayload(81, 4, 17, 0),
                        galacticwars.clonewars.network.ForceHudPayload.STREAM_CODEC)
                || !codecRoundTrips(
                        new galacticwars.clonewars.network.CommandCenterStatePayload(
                                7,
                                populatedDashboard(helper.getLevel().getGameTime())),
                        galacticwars.clonewars.network.CommandCenterStatePayload.STREAM_CODEC)) {
            helper.fail("A Framework message codec did not round-trip cleanly");
            return;
        }
        helper.succeed();
    }

    private static <T> boolean codecRoundTrips(
            T expected,
            net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, T> codec
    ) {
        net.minecraft.network.RegistryFriendlyByteBuf buffer =
                new net.minecraft.network.RegistryFriendlyByteBuf(
                        io.netty.buffer.Unpooled.buffer(), net.minecraft.core.RegistryAccess.EMPTY);
        try {
            codec.encode(buffer, expected);
            return expected.equals(codec.decode(buffer)) && !buffer.isReadable();
        } finally {
            buffer.release();
        }
    }

    private static CommandCenterDashboardState populatedDashboard(long gameTime) {
        UUID actorId = UUID.randomUUID();
        UUID kingdomId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID foreignKingdomId = UUID.randomUUID();
        return new CommandCenterDashboardState(
                Math.max(0L, gameTime), true, actorId, kingdomId, "galacticwars:republic", "owner",
                120, 40, true, 3, 8, 1, 1, true, true, 2, 3, 1,
                List.of(candidateId),
                List.of(new CommandCenterDashboardState.ClaimSummary(
                        UUID.randomUUID(), "minecraft:overworld", 2, 3, 9, true)),
                List.of(new CommandCenterDashboardState.SquadSummary(
                        UUID.randomUUID(), "Aurek Squad", Optional.of(actorId), List.of(memberId),
                        2, "follow", "line", "active", 16)),
                List.of(new CommandCenterDashboardState.CombatTargetSummary(
                        UUID.randomUUID(), "B1 Battle Droid", "galacticwars:separatist", 14)),
                List.of(new CommandCenterDashboardState.BlueprintSummary(
                        CommandCenterDashboardState.BlueprintSummary.targetId("galacticwars:forward_base"),
                        "galacticwars:forward_base", "Forward Base", List.of(0, 1, 2, 3), 12,
                        List.of(new CommandCenterDashboardState.MaterialSummary(
                                "minecraft:stone_bricks", 12)), 4, 9, 1)),
                List.of(candidateId),
                List.of(new CommandCenterDashboardState.BuildSummary(
                        UUID.randomUUID(), "galacticwars:forward_base", "active", 4, 12, "")),
                List.of(new CommandCenterDashboardState.WorkOrderSummary(
                        UUID.randomUUID(), "build", "assigned", Optional.of(candidateId), 4, 12, "")),
                List.of(new CommandCenterDashboardState.WorkerSummary(
                        candidateId, "CT-6116", "builder", "work_at_site", "interact",
                        "build_place", Optional.of(new CommandCenterDashboardState.PositionSummary(
                        "minecraft:overworld", 2, 64, 3)), 8,
                        Optional.of(new CommandCenterDashboardState.PositionSummary(
                                "minecraft:overworld", 1, 64, 1)),
                        Optional.of(new CommandCenterDashboardState.PositionSummary(
                                "minecraft:overworld", 3, 64, 3)), Optional.empty(), 2, 24, 5)),
                List.of(new CommandCenterDashboardState.QuestSummary(
                        "galacticwars:republic_chapter_1", false, 40, List.of("workforce"),
                        List.of(new CommandCenterDashboardState.ObjectiveSummary(
                                "command_center", true)))),
                List.of(new CommandCenterDashboardState.NearbyPlayerSummary(
                        UUID.randomUUID(), "Nearby Player", 6)),
                List.of(new CommandCenterDashboardState.MemberSummary(actorId, "owner")),
                List.of(new CommandCenterDashboardState.ForeignKingdomSummary(
                        foreignKingdomId, UUID.randomUUID(), "galacticwars:separatist")),
                List.of(new CommandCenterDashboardState.InviteSummary(
                        UUID.randomUUID(), kingdomId, actorId, memberId, "member", gameTime + 1200L)),
                List.of(new CommandCenterDashboardState.DiplomacyProposalSummary(
                        UUID.randomUUID(), foreignKingdomId, kingdomId, "ally", gameTime + 1200L)));
    }

    private static void blasterFriendlyFire(GameTestHelper helper) {
        ServerPlayer owner = makeConnectedMockPlayer(helper, GameType.CREATIVE);
        GalacticRecruitEntity recruit = helper.spawn(
                ModEntityTypes.CLONE_TROOPER.get(), new BlockPos(2, 1, 2));
        recruit.tame(owner);
        Arrow bolt = new Arrow(
                helper.getLevel(), owner,
                new ItemStack(ModItems.ENERGY_CELL.get()),
                new ItemStack(ModItems.DC15_BLASTER.get()));
        ProjectileImpactEvent impact = new ProjectileImpactEvent(bolt, new EntityHitResult(recruit));
        BlasterCombatEvents.onProjectileImpact(impact);
        if (!impact.isCanceled() || !bolt.isRemoved()) {
            helper.fail("Owned recruit was not protected from its owner's blaster bolt");
            return;
        }

        GalacticRecruitEntity squadmate = helper.spawn(
                ModEntityTypes.CLONE_TROOPER.get(), new BlockPos(3, 1, 2));
        squadmate.tame(owner);
        Arrow recruitBolt = new Arrow(
                helper.getLevel(), squadmate,
                new ItemStack(ModItems.ENERGY_CELL.get()),
                new ItemStack(ModItems.DC15_BLASTER.get()));
        ProjectileImpactEvent recruitImpact = new ProjectileImpactEvent(recruitBolt, new EntityHitResult(recruit));
        BlasterCombatEvents.onProjectileImpact(recruitImpact);
        if (!recruitImpact.isCanceled() || !recruitBolt.isRemoved()) {
            helper.fail("Same-owner squadmate was not protected from a recruit blaster bolt");
            return;
        }
        Arrow bowArrow = new Arrow(
                helper.getLevel(), squadmate,
                new ItemStack(Items.ARROW),
                new ItemStack(ModItems.NIGHTSISTER_BOW.get()));
        ProjectileImpactEvent bowImpact = new ProjectileImpactEvent(bowArrow, new EntityHitResult(recruit));
        BlasterCombatEvents.onProjectileImpact(bowImpact);
        if (!bowImpact.isCanceled() || !bowArrow.isRemoved()) {
            helper.fail("Same-owner squadmate was not protected from a Nightsister bow projectile");
            return;
        }
        Arrow vanillaArrow = new Arrow(EntityTypes.ARROW, helper.getLevel());
        ProjectileImpactEvent vanillaImpact = new ProjectileImpactEvent(
                vanillaArrow, new EntityHitResult(recruit));
        BlasterCombatEvents.onProjectileImpact(vanillaImpact);
        if (vanillaImpact.isCanceled() || vanillaArrow.isRemoved()) {
            helper.fail("Untagged vanilla arrow was incorrectly handled as a faction projectile");
            return;
        }

        ServerPlayer republicTarget = makeConnectedMockPlayer(helper, GameType.SURVIVAL);
        KingdomSavedData.get(helper.getLevel()).foundKingdom(
                republicTarget.getUUID(),
                "galacticwars:republic",
                helper.getLevel().dimension().identifier().toString(),
                helper.absolutePos(new BlockPos(1, 1, 1)).offset(0, 0, 50_000));
        GalacticRecruitEntity separatistGuard = helper.spawn(
                ModEntityTypes.B1_BATTLE_DROID.get(), new BlockPos(4, 1, 2));
        separatistGuard.initializeNaturalFactionNpc(UUID.randomUUID(), NpcServiceBranch.MILITARY);
        Arrow hostileBolt = new Arrow(
                helper.getLevel(), separatistGuard,
                new ItemStack(ModItems.ENERGY_CELL.get()),
                new ItemStack(ModItems.DC15_BLASTER.get()));
        ProjectileImpactEvent hostileImpact = new ProjectileImpactEvent(
                hostileBolt, new EntityHitResult(republicTarget));
        BlasterCombatEvents.onProjectileImpact(hostileImpact);
        boolean hostileHitBlocked = !Config.ALLOW_BLASTER_PVP.getAsBoolean();
        if (hostileImpact.isCanceled() != hostileHitBlocked || hostileBolt.isRemoved() != hostileHitBlocked) {
            helper.fail("Hostile faction recruit projectile did not honor the PvP setting");
            return;
        }

        ServerPlayer neutralTarget = makeConnectedMockPlayer(helper, GameType.SURVIVAL);
        Arrow neutralBolt = new Arrow(
                helper.getLevel(), separatistGuard,
                new ItemStack(ModItems.ENERGY_CELL.get()),
                new ItemStack(ModItems.DC15_BLASTER.get()));
        ProjectileImpactEvent neutralImpact = new ProjectileImpactEvent(
                neutralBolt, new EntityHitResult(neutralTarget));
        BlasterCombatEvents.onProjectileImpact(neutralImpact);
        if (!neutralImpact.isCanceled() || !neutralBolt.isRemoved()) {
            helper.fail("Faction recruit projectile damaged a neutral player");
            return;
        }
        helper.succeed();
    }

    private static void recruitSpawnEggs(GameTestHelper helper) {
        ServerPlayer player = makeConnectedMockPlayer(helper, GameType.CREATIVE);
        List<SpawnEggCase> cases = List.of(
                new SpawnEggCase(ModItems.CLONE_TROOPER_SPAWN_EGG.get(), ModEntityTypes.CLONE_TROOPER.get()),
                new SpawnEggCase(ModItems.ARC_TROOPER_SPAWN_EGG.get(), ModEntityTypes.ARC_TROOPER.get()),
                new SpawnEggCase(ModItems.JEDI_KNIGHT_SPAWN_EGG.get(), ModEntityTypes.JEDI_KNIGHT.get()),
                new SpawnEggCase(ModItems.MANDALORIAN_WARRIOR_SPAWN_EGG.get(), ModEntityTypes.MANDALORIAN_WARRIOR.get()),
                new SpawnEggCase(ModItems.MANDALORIAN_MARKSMAN_SPAWN_EGG.get(), ModEntityTypes.MANDALORIAN_MARKSMAN.get()),
                new SpawnEggCase(ModItems.MANDALORIAN_HEAVY_SPAWN_EGG.get(), ModEntityTypes.MANDALORIAN_HEAVY.get()),
                new SpawnEggCase(ModItems.B1_BATTLE_DROID_SPAWN_EGG.get(), ModEntityTypes.B1_BATTLE_DROID.get()),
                new SpawnEggCase(ModItems.B2_SUPER_BATTLE_DROID_SPAWN_EGG.get(), ModEntityTypes.B2_SUPER_BATTLE_DROID.get()),
                new SpawnEggCase(ModItems.COMMANDO_DROID_SPAWN_EGG.get(), ModEntityTypes.COMMANDO_DROID.get()),
                new SpawnEggCase(ModItems.HUTT_ENFORCER_SPAWN_EGG.get(), ModEntityTypes.HUTT_ENFORCER.get()),
                new SpawnEggCase(ModItems.BOUNTY_HUNTER_SPAWN_EGG.get(), ModEntityTypes.BOUNTY_HUNTER.get()),
                new SpawnEggCase(ModItems.SMUGGLER_SPAWN_EGG.get(), ModEntityTypes.SMUGGLER.get()),
                new SpawnEggCase(ModItems.NIGHTSISTER_ACOLYTE_SPAWN_EGG.get(), ModEntityTypes.NIGHTSISTER_ACOLYTE.get(), false),
                new SpawnEggCase(ModItems.NIGHTSISTER_ARCHER_SPAWN_EGG.get(), ModEntityTypes.NIGHTSISTER_ARCHER.get(), false),
                new SpawnEggCase(ModItems.NIGHTBROTHER_BRUTE_SPAWN_EGG.get(), ModEntityTypes.NIGHTBROTHER_BRUTE.get(), false),
                new SpawnEggCase(ModItems.REPUBLIC_CIVILIAN_SPAWN_EGG.get(), ModEntityTypes.REPUBLIC_CIVILIAN.get(), true),
                new SpawnEggCase(ModItems.SEPARATIST_TECHNICIAN_SPAWN_EGG.get(), ModEntityTypes.SEPARATIST_TECHNICIAN.get(), true),
                new SpawnEggCase(ModItems.MANDALORIAN_CLANSPERSON_SPAWN_EGG.get(), ModEntityTypes.MANDALORIAN_CLANSPERSON.get(), true),
                new SpawnEggCase(ModItems.HUTT_CIVILIAN_SPAWN_EGG.get(), ModEntityTypes.HUTT_CIVILIAN.get(), true),
                new SpawnEggCase(ModItems.NIGHTSISTER_CIVILIAN_SPAWN_EGG.get(), ModEntityTypes.NIGHTSISTER_CIVILIAN.get(), true));
        List<UUID> spawnedRecruitIds = new java.util.ArrayList<>();

        for (int index = 0; index < cases.size(); index++) {
            SpawnEggCase testCase = cases.get(index);
            BlockPos relativeClicked = new BlockPos(1 + index % 10 * 2, 1, 4 + index / 10 * 3);
            helper.setBlock(relativeClicked, Blocks.STONE);
            BlockPos clicked = helper.absolutePos(relativeClicked);
            BlockPos expectedSpawn = clicked.above();
            ItemStack eggStack = new ItemStack(testCase.item());
            if (!SpawnEggItem.spawnsEntity(eggStack, testCase.type())) {
                helper.fail("Spawn egg item component does not identify " + testCase.type());
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, eggStack);
            InteractionResult result;
            try {
                result = player.gameMode.useItemOn(
                        player,
                        helper.getLevel(),
                        eggStack,
                        InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(clicked), Direction.UP, clicked, false));
            } catch (Throwable exception) {
                GalacticWars.LOGGER.error(
                        "Spawn egg threw while creating {}", testCase.type(), exception);
                helper.fail("Spawn egg threw while creating " + testCase.type() + ": " + exception);
                return;
            }
            if (!result.consumesAction()) {
                helper.fail("Spawn egg rejected " + testCase.type());
            }
            List<GalacticRecruitEntity> spawned = helper.getLevel().getEntitiesOfClass(
                    GalacticRecruitEntity.class,
                    new AABB(expectedSpawn).inflate(1.0D),
                    recruit -> recruit.getType() == testCase.type());
            if (spawned.size() != 1) {
                helper.fail("Spawn egg did not create " + testCase.type());
            }
            GalacticRecruitEntity recruit = spawned.getFirst();
            spawnedRecruitIds.add(recruit.getUUID());
            // GameTests share a server batch; isolate this lifecycle assertion from nearby combat tests.
            recruit.setInvulnerable(true);
            if (!recruit.isPersistenceRequired()) {
                helper.fail("Spawn egg recruit was not marked persistent: " + testCase.type());
            }
            if (testCase.civilian()) {
                if (recruit.getServiceBranch() != NpcServiceBranch.CIVILIAN
                        || !recruit.getMainHandItem().isEmpty()) {
                    helper.fail("Civilian spawn egg did not apply its archetype: " + testCase.type());
                }
            } else if (recruit.getServiceBranch() != NpcServiceBranch.MILITARY
                    || recruit.getMainHandItem().isEmpty()) {
                helper.fail("Military spawn egg did not apply its unit loadout: " + testCase.type());
            }
        }

        ServerPlayer survivalPlayer = makeConnectedMockPlayer(helper, GameType.SURVIVAL);
        BlockPos replaceableFloor = helper.absolutePos(new BlockPos(15, 1, 11));
        BlockPos replaceableTarget = replaceableFloor.above();
        helper.getLevel().setBlockAndUpdate(replaceableFloor, Blocks.DIRT.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(replaceableTarget, Blocks.SHORT_GRASS.defaultBlockState());
        ItemStack namedEgg = new ItemStack(ModItems.CLONE_TROOPER_SPAWN_EGG.get(), 2);
        namedEgg.set(DataComponents.CUSTOM_NAME, Component.literal("QA Clone"));
        survivalPlayer.setItemInHand(InteractionHand.MAIN_HAND, namedEgg);
        InteractionResult replaceableResult = survivalPlayer.gameMode.useItemOn(
                survivalPlayer,
                helper.getLevel(),
                namedEgg,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        Vec3.atCenterOf(replaceableTarget),
                        Direction.UP,
                        replaceableTarget,
                        false));
        List<GalacticRecruitEntity> namedRecruits = helper.getLevel().getEntitiesOfClass(
                GalacticRecruitEntity.class,
                new AABB(replaceableTarget).inflate(1.0D),
                recruit -> recruit.getType() == ModEntityTypes.CLONE_TROOPER.get()
                        && Component.literal("QA Clone").equals(recruit.getCustomName()));
        if (!replaceableResult.consumesAction() || namedEgg.getCount() != 1 || namedRecruits.size() != 1) {
            helper.fail("Survival spawn egg did not preserve replaceable-block, naming, or consumption behavior");
        }

        BlockPos relativeSpawner = new BlockPos(1, 1, 11);
        helper.setBlock(relativeSpawner, Blocks.SPAWNER);
        BlockPos spawnerPos = helper.absolutePos(relativeSpawner);
        SpawnerBlockEntity spawner = helper.getBlockEntity(
                relativeSpawner, SpawnerBlockEntity.class);
        ItemStack spawnerEgg = new ItemStack(ModItems.CLONE_TROOPER_SPAWN_EGG.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, spawnerEgg);
        InteractionResult spawnerResult = player.gameMode.useItemOn(
                player,
                helper.getLevel(),
                spawnerEgg,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(spawnerPos), Direction.UP, spawnerPos, false));
        var spawnerDisplay = spawner.getSpawner().getOrCreateDisplayEntity(
                helper.getLevel(), spawnerPos);
        if (!spawnerResult.consumesAction()
                || spawnerDisplay == null
                || spawnerDisplay.getType() != ModEntityTypes.CLONE_TROOPER.get()) {
            helper.fail("Recruit spawn egg did not preserve vanilla spawner configuration behavior");
        }

        BlockPos relativeDispenser = new BlockPos(10, 1, 15);
        BlockPos dispenserPos = helper.absolutePos(relativeDispenser);
        var dispenserState = Blocks.DISPENSER.defaultBlockState()
                .setValue(DispenserBlock.FACING, Direction.EAST);
        helper.getLevel().setBlockAndUpdate(dispenserPos, dispenserState);
        DispenserBlockEntity dispenser = helper.getBlockEntity(
                relativeDispenser, DispenserBlockEntity.class);
        ItemStack dispenserEgg = new ItemStack(ModItems.B1_BATTLE_DROID_SPAWN_EGG.get());
        dispenser.setItem(0, dispenserEgg);
        dispenser.setItem(0, SpawnEggItemBehavior.INSTANCE.dispense(
                new BlockSource(helper.getLevel(), dispenserPos, dispenserState, dispenser),
                dispenserEgg));
        BlockPos dispenserSpawnPos = dispenserPos.relative(Direction.EAST);
        List<GalacticRecruitEntity> dispenserRecruits = helper.getLevel().getEntitiesOfClass(
                GalacticRecruitEntity.class,
                new AABB(dispenserSpawnPos).inflate(1.0D),
                recruit -> recruit.getType() == ModEntityTypes.B1_BATTLE_DROID.get());
        if (dispenserRecruits.size() != 1) {
            helper.fail("Dispenser spawn egg did not create its recruit");
        }
        GalacticRecruitEntity dispenserRecruit = dispenserRecruits.getFirst();
        spawnedRecruitIds.add(dispenserRecruit.getUUID());
        // Keep the dispenser case subject to normal AI ticking without allowing cross-test damage.
        dispenserRecruit.setInvulnerable(true);
        if (!dispenserRecruit.isPersistenceRequired()
                || dispenserRecruit.getServiceBranch() != NpcServiceBranch.MILITARY
                || dispenserRecruit.getMainHandItem().isEmpty()) {
            helper.fail("Dispenser spawn egg did not initialize recruit persistence and loadout");
        }

        BlockPos relativeFurnace = new BlockPos(15, 1, 15);
        helper.setBlock(relativeFurnace, Blocks.FURNACE);
        BlockPos furnacePos = helper.absolutePos(relativeFurnace);
        long spawnGameTime = helper.getLevel().getGameTime();
        helper.runAfterDelay(40, () -> {
            for (UUID recruitId : spawnedRecruitIds) {
                if (!(helper.getLevel().getEntity(recruitId) instanceof GalacticRecruitEntity recruit)
                        || !recruit.isAlive()) {
                    helper.fail("Spawned recruit stopped surviving server ticks: " + recruitId);
                    return;
                }
            }
            if (helper.getLevel().getGameTime() < spawnGameTime + 40L) {
                helper.fail("Server stopped ticking after recruit spawn-egg use");
                return;
            }
            if (!(helper.getLevel().getBlockEntity(furnacePos) instanceof Container furnace)) {
                helper.fail("Furnace block entity became unavailable after recruit spawn-egg use");
                return;
            }
            ItemStack furnaceInput = new ItemStack(Items.RAW_IRON);
            furnace.setItem(0, furnaceInput);
            if (!furnace.getItem(0).is(Items.RAW_IRON)) {
                helper.fail("Server could not process a furnace inventory interaction after recruit spawn-egg use");
                return;
            }
            helper.succeed();
        });
    }

    private static void kingdomHallAuthority(GameTestHelper helper) {
        BlockPos hallPos = isolatedCapital(helper, 9);
        CommandCenterBlockEntity hall = placeCommandCenter(helper, hallPos);
        ServerPlayer owner = makeConnectedMockPlayer(helper, GameType.CREATIVE);
        ServerPlayer intruder = makeConnectedMockPlayer(helper, GameType.CREATIVE);
        if (!hall.claim(owner) || hall.claim(intruder) || !hall.isOwner(owner)) {
            helper.fail("Command Center ownership guard rejected the owner or accepted an intruder");
        }
        long claimGameTime = helper.getLevel().getGameTime();
        if (!hall.chargeDailyUpkeep(Math.addExact(claimGameTime, 23999L), 1)) {
            helper.fail("New Command Center was charged upkeep before its first full day elapsed");
        }
        hall.setFaction("galacticwars:mandalorian");
        if (hall.getUpdatePacket() == null
                || !hall.getUpdateTag(helper.getLevel().registryAccess())
                        .getStringOr("CommandCenterFaction", "")
                        .equals("galacticwars:mandalorian")) {
            helper.fail("Command Center custom state was not exposed through its client update packet");
        }
        hall.setItem(0, new ItemStack(galacticwars.clonewars.registry.ModItems.CREDIT_CHIP.get(), 32));
        if (!hall.reserveCredits(10) || hall.treasuryCredits() != 22 || hall.refundCredits(5) != 5
                || hall.treasuryCredits() != 27) {
            helper.fail("Command Center treasury did not conserve reserved and refunded credits");
        }
        if (!hall.reserveCredits(27) || hall.getItem(0) != ItemStack.EMPTY) {
            helper.fail("Command Center treasury did not normalize a depleted slot to ItemStack.EMPTY");
        }
        KingdomSavedData data = KingdomSavedData.get(helper.getLevel());
        KingdomRecord kingdom = data.foundKingdom(
                owner.getUUID(),
                hall.factionId(),
                helper.getLevel().dimension().identifier().toString(),
                hallPos);
        if (!kingdom.ownerId().equals(owner.getUUID())
                || data.kingdomForOwner(owner.getUUID()).isEmpty()) {
            helper.fail("Kingdom state was not stored authoritatively in overworld SavedData");
        }
        BlockPos absoluteHall = hallPos;
        UUID overlappingOwner = UUID.randomUUID();
        if (data.activateHall(
                        overlappingOwner,
                        "galacticwars:separatist",
                        helper.getLevel().dimension().identifier().toString(),
                        absoluteHall.offset(32, 0, 0)).isPresent()
                || data.kingdomForOwner(overlappingOwner).isPresent()) {
            helper.fail("Overlapping Command Center capital claims were accepted");
            return;
        }
        RecruitmentCampaign campaign = new RecruitmentCampaign(
                UUID.randomUUID(),
                "galacticwars:mandalorian_rider",
                "",
                12,
                claimGameTime + 24000L,
                RecruitmentCampaignState.RESERVED,
                "reserved");
        if (!data.beginCampaign(owner.getUUID(), RecruitmentCampaignDecision.accepted(campaign))
                || !data.replaceCampaign(owner.getUUID(), campaign.cancel("commander_lost"))
                || hall.settlePendingCampaignRefunds(helper.getLevel()) != 12
                || hall.treasuryCredits() != 12
                || data.kingdomForOwner(owner.getUUID()).orElseThrow().settlement().recruitmentCampaigns().stream()
                        .filter(stored -> stored.id().equals(campaign.id()))
                        .findFirst()
                        .orElseThrow()
                        .reservedCost() != 0) {
            helper.fail("Cancelled commander campaign refund was not conserved through SavedData and Hall storage");
        }
        BlockPos relocatedHall = absoluteHall.offset(64, 0, 0);
        if (data.activateHall(owner.getUUID(), hall.factionId(),
                        helper.getLevel().dimension().identifier().toString(), relocatedHall).isPresent()) {
            helper.fail("A second active Command Center was accepted");
        }
        BlockPos localDropHallPos = new BlockPos(1, 1, 1);
        helper.setBlock(localDropHallPos, ModBlocks.COMMAND_CENTER.get());
        CommandCenterBlockEntity localDropHall = helper.getBlockEntity(
                localDropHallPos, CommandCenterBlockEntity.class);
        localDropHall.setItem(0, new ItemStack(
                galacticwars.clonewars.registry.ModItems.CREDIT_CHIP.get(), 12));
        BlockPos absoluteDropHall = helper.absolutePos(localDropHallPos);
        ModBlocks.COMMAND_CENTER.get().playerWillDestroy(
                helper.getLevel(), absoluteDropHall,
                helper.getLevel().getBlockState(absoluteDropHall), intruder);
        int localRemovalRefund = helper.getLevel().getEntitiesOfClass(
                        ItemEntity.class,
                        new net.minecraft.world.phys.AABB(absoluteDropHall).inflate(3.0))
                .stream()
                .filter(item -> item.getItem().is(galacticwars.clonewars.registry.ModItems.CREDIT_CHIP.get()))
                .mapToInt(item -> item.getItem().getCount())
                .sum();
        if (localRemovalRefund != 12 || !localDropHall.isEmpty()) {
            helper.fail("Command Center inventory removal did not conserve its physical drops");
        }
        ModBlocks.COMMAND_CENTER.get().playerWillDestroy(
                helper.getLevel(), absoluteHall, helper.getLevel().getBlockState(absoluteHall), intruder);
        if (!data.isHallActive(owner.getUUID()) || hall.treasuryCredits() != 12) {
            helper.fail("Intruder Hall removal bypassed owner authority");
        }
        owner.setPos(absoluteHall.getX() + 0.5, absoluteHall.getY(), absoluteHall.getZ() + 0.5);
        helper.runAfterDelay(2, () -> {
            CommandCenterBlockEntity removalHall = (CommandCenterBlockEntity) helper.getLevel()
                    .getBlockEntity(absoluteHall);
            int treasuryBeforeRemoval = removalHall == null ? -1 : removalHall.treasuryCredits();
            ModBlocks.COMMAND_CENTER.get().playerWillDestroy(
                    helper.getLevel(), absoluteHall, helper.getLevel().getBlockState(absoluteHall), owner);
            if (data.isHallActive(owner.getUUID()) || removalHall == null || !removalHall.isEmpty()) {
                helper.fail("Owner Hall removal did not conserve inventory and deactivate authority: active="
                        + data.isHallActive(owner.getUUID()) + ", treasuryBefore=" + treasuryBeforeRemoval
                        + ", hallPresent=" + (removalHall != null)
                        + ", empty=" + (removalHall != null && removalHall.isEmpty()));
            }
            KingdomRecord relocated = data.activateHall(owner.getUUID(), hall.factionId(),
                            helper.getLevel().dimension().identifier().toString(), relocatedHall)
                    .orElseThrow();
            if (!relocated.id().equals(kingdom.id())
                    || relocated.settlement().hallX() != relocatedHall.getX()
                    || relocated.settlement().hallY() != relocatedHall.getY()
                    || relocated.settlement().hallZ() != relocatedHall.getZ()
                    || data.claimAt(
                            helper.getLevel().dimension().identifier().toString(),
                            new net.minecraft.world.level.ChunkPos(
                                    absoluteHall.getX() >> 4, absoluteHall.getZ() >> 4))
                            .isPresent()
                    || data.claimAt(
                            helper.getLevel().dimension().identifier().toString(),
                            new net.minecraft.world.level.ChunkPos(
                                    relocatedHall.getX() >> 4, relocatedHall.getZ() >> 4))
                            .filter(claim -> claim.kingdomId().equals(kingdom.id())).isEmpty()) {
                helper.fail("Command Center relocation did not preserve kingdom identity and update its position");
            }

            int housingBeforeReward = relocated.settlement().housingCapacity();
            KingdomBaseBlueprint farmBlueprint = GameplayDataManager.snapshot()
                    .blueprint("galacticwars:moisture_farm").orElseThrow();
            BuildProject farmProject = fullyProgressProject(
                    data, owner.getUUID(), farmBlueprint,
                    helper.getLevel().dimension().identifier().toString(),
                    relocatedHall.offset(4, 0, 0));
            if (!data.completeBuildProject(owner.getUUID(), farmProject, farmBlueprint)
                    || data.completeBuildProject(owner.getUUID(), farmProject, farmBlueprint)) {
                helper.fail("Build rewards were not applied exactly once");
            }
            KingdomRecord rewarded = data.kingdomForOwner(owner.getUUID()).orElseThrow();
            if (rewarded.settlement().housingCapacity() != housingBeforeReward + farmBlueprint.housingReward()
                    || rewarded.settlement().buildProjects().stream()
                            .filter(project -> project.blueprintId().equals(farmBlueprint.id()))
                            .count() != 1
                    || rewarded.settlement().worksites().stream()
                            .filter(worksite -> worksite.type().equals("farmer"))
                            .count() != 1) {
                helper.fail("Build completion rewards were duplicated or omitted");
            }
            Tag encoded = KingdomSavedData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
            KingdomSavedData restored = KingdomSavedData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
            KingdomRecord restoredReward = restored.kingdomForOwner(owner.getUUID()).orElseThrow();
            if (!restoredReward.settlement().containsCompletedProject(farmProject)
                    || restoredReward.settlement().housingCapacity()
                            != housingBeforeReward + farmBlueprint.housingReward()
                    || restoredReward.settlement().worksites().stream()
                            .filter(worksite -> worksite.type().equals("farmer"))
                            .count() != 1) {
                helper.fail("Build completion rewards did not survive a SavedData codec round trip");
            }
            helper.succeed();
        });
    }

    private static void commandCenterPlayerInteraction(GameTestHelper helper) {
        BlockPos hallPos = isolatedCapital(helper, 28);
        CommandCenterBlockEntity hall = placeCommandCenter(helper, hallPos);
        ServerPlayer player = makeConnectedMockPlayer(helper, GameType.CREATIVE);
        player.setPos(hallPos.getX() + 0.5D, hallPos.getY(), hallPos.getZ() + 2.5D);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        InteractionResult result;
        try {
            result = player.gameMode.useItemOn(
                    player,
                    helper.getLevel(),
                    ItemStack.EMPTY,
                    InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(hallPos), Direction.SOUTH, hallPos, false));
        } catch (RuntimeException exception) {
            String message = exception.getMessage() == null ? "" : exception.getMessage();
            if (!hall.isOwner(player)
                    || !message.contains("advanced_open_screen may not be sent to the client")) {
                helper.fail("Command Center interaction failed before its headless-client menu boundary: "
                        + exception);
                return;
            }
            helper.succeed();
            return;
        }
        if (!result.consumesAction()
                || !hall.isOwner(player)
                || !(player.containerMenu instanceof FactionSelectionMenu)) {
            helper.fail("Empty-hand Command Center use did not claim the Hall and open faction selection: result="
                    + result + ", owner=" + hall.isOwner(player)
                    + ", menu=" + player.containerMenu.getClass().getSimpleName());
            return;
        }
        helper.succeed();
    }

    private static void commandCenterMultiplayerInvite(GameTestHelper helper) {
        BlockPos hallPos = isolatedCapital(helper, 18);
        CommandCenterBlockEntity hall = placeCommandCenter(helper, hallPos);
        ServerPlayer owner = makeConnectedMockPlayer(helper, GameType.CREATIVE);
        ServerPlayer candidate = makeConnectedMockPlayer(helper, GameType.CREATIVE);
        ServerPlayer distant = makeConnectedMockPlayer(helper, GameType.CREATIVE);
        owner.setPos(hallPos.getX() + 0.5D, hallPos.getY(), hallPos.getZ() + 0.5D);
        candidate.setPos(hallPos.getX() + 4.5D, hallPos.getY(), hallPos.getZ() + 0.5D);
        distant.setPos(hallPos.getX() + 40.5D, hallPos.getY(), hallPos.getZ() + 0.5D);
        hall.claim(owner);
        KingdomSavedData data = KingdomSavedData.get(helper.getLevel());
        KingdomRecord kingdom = data.foundKingdom(
                owner.getUUID(), "galacticwars:republic",
                helper.getLevel().dimension().identifier().toString(), hallPos);

        CommandCenterOperationsMenu staleMenu = (CommandCenterOperationsMenu)
                new CommandCenterOperationsMenuProvider(hallPos)
                        .createMenu(1, owner.getInventory(), owner);
        List<CommandCenterDashboardState.NearbyPlayerSummary> inviteTargets =
                staleMenu.dashboardState().nearbyPlayers();
        if (inviteTargets.size() != 1
                || !inviteTargets.getFirst().playerId().equals(candidate.getUUID())
                || inviteTargets.stream().anyMatch(target -> target.playerId().equals(distant.getUUID()))) {
            helper.fail("Command Center did not expose only bounded explicit invite targets: "
                    + inviteTargets);
            return;
        }

        candidate.setPos(hallPos.getX() + 32.5D, hallPos.getY(), hallPos.getZ() + 0.5D);
        if (staleMenu.handleReplayAction(
                owner, UUID.randomUUID(), CommandCenterOperationsMenu.INVITE_NEAREST,
                Optional.of(candidate.getUUID()), Optional.empty())) {
            helper.fail("Command Center accepted a stale invite target outside embodied range");
            return;
        }
        candidate.setPos(hallPos.getX() + 4.5D, hallPos.getY(), hallPos.getZ() + 0.5D);
        CommandCenterOperationsMenu ownerMenu = (CommandCenterOperationsMenu)
                new CommandCenterOperationsMenuProvider(hallPos)
                        .createMenu(2, owner.getInventory(), owner);
        UUID replayId = UUID.randomUUID();
        boolean invited = ownerMenu.handleReplayAction(
                owner, replayId, CommandCenterOperationsMenu.INVITE_NEAREST,
                Optional.of(candidate.getUUID()), Optional.empty());
        boolean replayRejected = !ownerMenu.handleReplayAction(
                owner, replayId, CommandCenterOperationsMenu.INVITE_NEAREST,
                Optional.of(candidate.getUUID()), Optional.empty());
        var invite = data.pendingInvites().stream()
                .filter(value -> value.kingdomId().equals(kingdom.id()))
                .filter(value -> value.targetPlayerId().equals(candidate.getUUID()))
                .findFirst().orElse(null);
        if (!invited || !replayRejected || invite == null) {
            helper.fail("Explicit multiplayer invitation was not replay-safe: invited="
                    + invited + ", replay=" + replayRejected + ", invite=" + invite);
            return;
        }

        CommandCenterOperationsMenu candidateMenu = (CommandCenterOperationsMenu)
                new CommandCenterOperationsMenuProvider(hallPos)
                        .createMenu(3, candidate.getInventory(), candidate);
        if (!candidateMenu.dashboardState().actorRole().equals("visitor")
                || !candidateMenu.handleReplayAction(
                        candidate, UUID.randomUUID(), CommandCenterOperationsMenu.ACCEPT_INVITE,
                        Optional.of(invite.id()), Optional.empty())
                || data.kingdomForPlayer(candidate.getUUID())
                        .filter(joined -> joined.id().equals(kingdom.id()))
                        .flatMap(joined -> joined.member(candidate.getUUID()))
                        .filter(member -> member.role() == KingdomMemberRole.MEMBER)
                        .isEmpty()
                || !hall.canOpen(candidate)) {
            helper.fail("Invited player could not accept into the authoritative kingdom");
            return;
        }
        helper.succeed();
    }

    private static void forceEmbodiedRuntime(GameTestHelper helper) {
        ProgressionSavedData progression = ProgressionSavedData.get(helper.getLevel());
        ForceSavedData force = ForceSavedData.get(helper.getLevel());
        ServerPlayer light = makeConnectedMockPlayer(helper, GameType.CREATIVE);
        BlockPos lightOrigin = helper.absolutePos(new BlockPos(2, 1, 2));
        light.setPos(lightOrigin.getX() + 0.5D, lightOrigin.getY(), lightOrigin.getZ() + 0.5D);
        light.setYRot(0.0F);
        light.setYHeadRot(0.0F);
        light.setXRot(0.0F);
        completeForceCampaign(progression, light, "galacticwars:republic",
                "clone_trooper", "kamino");

        GalacticRecruitEntity ally = helper.spawn(
                ModEntityTypes.CLONE_TROOPER.get(), new BlockPos(2, 1, 5));
        ally.setNoAi(true);
        ally.initializeFromSpawnEgg();
        ally.tame(light);
        var pushed = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(2, 1, 8));
        pushed.setNoAi(true);
        ally.setDeltaMovement(Vec3.ZERO);
        pushed.setDeltaMovement(Vec3.ZERO);
        UUID lightPushId = UUID.randomUUID();
        boolean lightPush = ForceWorldEffectService.activate(light, lightPushId, 0);
        int lightAfterPush = force.state(light.getUUID()).energy();
        boolean lightReplay = ForceWorldEffectService.activate(light, lightPushId, 0);
        boolean lightCooldown = !ForceWorldEffectService.activate(
                light, UUID.randomUUID(), 0);
        if (!lightPush || !lightReplay || !lightCooldown || lightAfterPush != 80
                || force.state(light.getUUID()).energy() != 80
                || ally.getDeltaMovement().lengthSqr() > 0.0001D
                || pushed.getDeltaMovement().z <= 0.1D) {
            helper.fail("Light Push did not protect allied NPCs or enforce replay/cooldown state: "
                    + "accepted=" + lightPush + ", replay=" + lightReplay
                    + ", cooldown=" + lightCooldown + ", energy="
                    + force.state(light.getUUID()).energy() + ", ally="
                    + ally.getDeltaMovement() + ", target=" + pushed.getDeltaMovement());
            return;
        }
        ally.discard();
        pushed.discard();

        var pulled = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(2, 1, 7));
        pulled.setNoAi(true);
        pulled.setDeltaMovement(Vec3.ZERO);
        UUID lightPullId = UUID.randomUUID();
        if (!ForceWorldEffectService.activate(light, lightPullId, 1)
                || pulled.getDeltaMovement().z >= -0.1D
                || force.state(light.getUUID()).energy() != 55) {
            helper.fail("Light Pull did not move its physical target toward the player: "
                    + pulled.getDeltaMovement() + "/" + force.state(light.getUUID()));
            return;
        }
        pulled.discard();
        light.setDeltaMovement(Vec3.ZERO);
        UUID lightLeapId = UUID.randomUUID();
        if (!ForceWorldEffectService.activate(light, lightLeapId, 2)
                || light.getDeltaMovement().y <= 0.5D
                || force.state(light.getUUID()).energy() != 40) {
            helper.fail("Light Leap did not launch the player and conserve Force energy: "
                    + light.getDeltaMovement() + "/" + force.state(light.getUUID()));
            return;
        }
        light.setPos(lightOrigin.getX() + 100.5D, lightOrigin.getY(), lightOrigin.getZ() + 0.5D);

        ServerPlayer dark = makeConnectedMockPlayer(helper, GameType.CREATIVE);
        BlockPos darkOrigin = helper.absolutePos(new BlockPos(2, 1, 2));
        dark.setPos(darkOrigin.getX() + 0.5D, darkOrigin.getY(), darkOrigin.getZ() + 0.5D);
        dark.setYRot(0.0F);
        dark.setYHeadRot(0.0F);
        dark.setXRot(0.0F);
        completeForceCampaign(progression, dark, "galacticwars:nightsister",
                "nightsister_acolyte", "coruscant");

        var darkPushed = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(2, 1, 8));
        darkPushed.setNoAi(true);
        darkPushed.setDeltaMovement(Vec3.ZERO);
        UUID darkPushId = UUID.randomUUID();
        if (!ForceWorldEffectService.activate(dark, darkPushId, 0)
                || darkPushed.getDeltaMovement().z <= 0.1D
                || force.state(dark.getUUID()).energy() != 80) {
            helper.fail("Dark Push did not move its physical target: "
                    + darkPushed.getDeltaMovement() + "/" + force.state(dark.getUUID()));
            return;
        }
        darkPushed.discard();
        dark.setDeltaMovement(Vec3.ZERO);
        UUID darkDashId = UUID.randomUUID();
        if (!ForceWorldEffectService.activate(dark, darkDashId, 1)
                || dark.getDeltaMovement().z <= 1.0D
                || force.state(dark.getUUID()).energy() != 65) {
            helper.fail("Dark Dash did not propel the player and conserve Force energy: "
                    + dark.getDeltaMovement() + "/" + force.state(dark.getUUID()));
            return;
        }
        dark.setDeltaMovement(Vec3.ZERO);
        var choked = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(2, 1, 6));
        choked.setNoAi(true);
        UUID darkChokeId = UUID.randomUUID();
        if (!ForceWorldEffectService.activate(dark, darkChokeId, 2)
                || !choked.hasEffect(MobEffects.LEVITATION)
                || !choked.hasEffect(MobEffects.WEAKNESS)
                || force.state(dark.getUUID()).energy() != 30) {
            helper.fail("Dark Choke did not apply its physical effects and cost: effects="
                    + choked.getActiveEffects() + ", state=" + force.state(dark.getUUID()));
            return;
        }

        Tag encoded = ForceSavedData.CODEC.encodeStart(NbtOps.INSTANCE, force).getOrThrow();
        ForceSavedData restored = ForceSavedData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        if (!restored.state(light.getUUID()).path().equals("light")
                || restored.state(light.getUUID()).energy() != 40
                || !restored.state(light.getUUID()).processedActivationIds().contains(lightLeapId)
                || !restored.state(dark.getUUID()).path().equals("dark")
                || restored.state(dark.getUUID()).energy() != 30
                || !restored.state(dark.getUUID()).processedActivationIds().contains(darkChokeId)) {
            helper.fail("Force paths, energy, cooldowns, or replay IDs did not survive persistence");
            return;
        }
        helper.succeed();
    }

    private static void allFactionCampaignPaths(GameTestHelper helper) {
        record CampaignPath(
                String faction,
                String firstRecruit,
                String planet,
                String chapterTwoTrade,
                String chapterThreeRecruit,
                boolean chapterThreeTrade
        ) {
        }
        List<CampaignPath> paths = List.of(
                new CampaignPath("galacticwars:republic", "clone_trooper", "kamino", "", "", true),
                new CampaignPath("galacticwars:separatist", "b1_battle_droid", "geonosis", "", "", true),
                new CampaignPath("galacticwars:mandalorian", "mandalorian_warrior", "tatooine",
                        "mandalorian_armorer", "bounty_hunter", false),
                new CampaignPath("galacticwars:hutt_cartel", "hutt_enforcer", "tatooine",
                        "hutt_broker", "smuggler", false),
                new CampaignPath("galacticwars:nightsister", "nightsister_acolyte", "coruscant",
                        "", "", true));
        ProgressionSavedData progression = ProgressionSavedData.get(helper.getLevel());
        for (CampaignPath path : paths) {
            ServerPlayer player = makeConnectedMockPlayer(helper, GameType.SURVIVAL);
            applyCampaignSetupEvent(
                    progression, player, ProgressionEventType.FACTION_PLEDGED, path.faction());
            applyCampaignSetupEvent(
                    progression, player, ProgressionEventType.BUILDING_COMPLETED, "command_center");
            applyCampaignSetupEvent(
                    progression, player, ProgressionEventType.RECRUIT_HIRED, path.firstRecruit());
            for (int delivery = 1; delivery <= 3; delivery++) {
                applyCampaignSetupEvent(progression, player, ProgressionEventType.DELIVERY_COMPLETED,
                        "campaign_matrix_delivery_" + delivery);
            }
            applyCampaignSetupEvent(
                    progression, player, ProgressionEventType.BUILDING_COMPLETED, "forward_base");
            applyCampaignSetupEvent(
                    progression, player, ProgressionEventType.BUILDING_COMPLETED, "supply_depot");
            if (!path.chapterTwoTrade().isEmpty()) {
                applyCampaignSetupEvent(progression, player, ProgressionEventType.TRADE_COMPLETED,
                        path.chapterTwoTrade());
            }
            applyCampaignSetupEvent(
                    progression, player, ProgressionEventType.PLANET_VISITED, path.planet());
            applyCampaignSetupEvent(
                    progression, player, ProgressionEventType.VEHICLE_ACQUIRED, "campaign_matrix_vehicle");
            applyCampaignSetupEvent(
                    progression, player, ProgressionEventType.REGION_CAPTURED, "campaign_matrix_region");
            if (path.chapterThreeTrade()) {
                applyCampaignSetupEvent(progression, player, ProgressionEventType.TRADE_COMPLETED,
                        "campaign_matrix_trade");
            }
            if (!path.chapterThreeRecruit().isEmpty()) {
                applyCampaignSetupEvent(progression, player, ProgressionEventType.RECRUIT_HIRED,
                        path.chapterThreeRecruit());
            }
            String factionPath = path.faction().substring(path.faction().indexOf(':') + 1);
            ProgressionState state = progression.state(player.getUUID());
            if (!state.hasSubject(
                    ProgressionEventType.QUEST_ADVANCED, factionPath + "_chapter_3")
                    || !state.hasSubject(
                    ProgressionEventType.CAMPAIGN_COMPLETED, factionPath + "_campaign")
                    || !state.unlocks().contains("campaign_victory")
                    || !state.unlocks().contains("veteran_operations")) {
                helper.fail("Faction campaign path did not reach persistent victory for "
                        + path.faction() + ": " + state);
                return;
            }
        }
        helper.succeed();
    }

    private static void completeForceCampaign(
            ProgressionSavedData progression,
            ServerPlayer player,
            String faction,
            String firstRecruit,
            String chapterTwoPlanet
    ) {
        applyCampaignSetupEvent(progression, player, ProgressionEventType.FACTION_PLEDGED, faction);
        applyCampaignSetupEvent(
                progression, player, ProgressionEventType.BUILDING_COMPLETED, "command_center");
        applyCampaignSetupEvent(
                progression, player, ProgressionEventType.RECRUIT_HIRED, firstRecruit);
        applyCampaignSetupEvent(
                progression, player, ProgressionEventType.DELIVERY_COMPLETED, "force_test_delivery");
        applyCampaignSetupEvent(
                progression, player, ProgressionEventType.BUILDING_COMPLETED, "forward_base");
        applyCampaignSetupEvent(
                progression, player, ProgressionEventType.PLANET_VISITED, chapterTwoPlanet);
        applyCampaignSetupEvent(
                progression, player, ProgressionEventType.VEHICLE_ACQUIRED, "force_test_vehicle");
        applyCampaignSetupEvent(
                progression, player, ProgressionEventType.TRADE_COMPLETED, "force_test_trade");
        applyCampaignSetupEvent(
                progression, player, ProgressionEventType.REGION_CAPTURED, "force_test_region");
        ProgressionState state = progression.state(player.getUUID());
        String factionPath = faction.substring(faction.indexOf(':') + 1);
        if (!state.hasSubject(ProgressionEventType.QUEST_ADVANCED, factionPath + "_chapter_3")) {
            throw new IllegalStateException("Force test campaign did not unlock Chapter 3: " + state);
        }
    }

    private static void commandCenterSquadOrders(GameTestHelper helper) {
        BlockPos hallPos = helper.absolutePos(new BlockPos(1, 1, 1));
        CommandCenterBlockEntity hall = placeCommandCenter(helper, hallPos);
        ServerPlayer owner = makeConnectedMockPlayer(helper, GameType.CREATIVE);
        owner.setPos(hallPos.getX() + 0.5D, hallPos.getY(), hallPos.getZ() + 0.5D);
        hall.claim(owner);
        KingdomSavedData data = KingdomSavedData.get(helper.getLevel());
        KingdomRecord kingdom = data.foundKingdom(
                owner.getUUID(), "galacticwars:republic",
                helper.getLevel().dimension().identifier().toString(), isolatedCapital(helper, 14));
        GalacticRecruitEntity commander = helper.spawn(
                ModEntityTypes.CLONE_TROOPER.get(), new BlockPos(2, 1, 2));
        GalacticRecruitEntity hostile = helper.spawn(
                ModEntityTypes.B1_BATTLE_DROID.get(), new BlockPos(4, 1, 2));
        commander.tame(owner);
        commander.setNoAi(true);
        hostile.setNoAi(true);
        hostile.initializeNaturalFactionNpc(UUID.randomUUID(), NpcServiceBranch.MILITARY);
        KingdomBaseBlueprint forwardBase = GameplayDataManager.snapshot()
                .blueprint("galacticwars:forward_base").orElseThrow();
        BuildProject forwardBaseProject = fullyProgressProject(
                data, owner.getUUID(), forwardBase,
                helper.getLevel().dimension().identifier().toString(),
                isolatedCapital(helper, 14).offset(32, 0, 0));
        boolean commanderSlotEarned = data.completeBuildProject(
                owner.getUUID(), forwardBaseProject, forwardBase);
        boolean commanderRegistered = data.registerRecruit(owner.getUUID(), commander.getUUID());
        boolean commanderPromoted = commanderRegistered
                && data.promoteCommander(owner.getUUID(), commander.getUUID());
        if (!commanderSlotEarned || !commanderRegistered || !commanderPromoted) {
            helper.fail("Command Center squad-order setup could not earn/register/promote its commander: slot="
                    + commanderSlotEarned + ", registered=" + commanderRegistered
                    + ", promoted=" + commanderPromoted);
            return;
        }
        ArmyLocation anchor = new ArmyLocation(
                helper.getLevel().dimension().identifier().toString(),
                commander.getX(), commander.getY(), commander.getZ());
        var squad = data.createOrReclaimArmyGroup(
                owner.getUUID(), commander.getUUID(), ArmyFormation.LINE,
                anchor, helper.getLevel().getGameTime()).orElseThrow();
        boolean[] scenarioRun = {false};
        helper.onEachTick(() -> {
            if (scenarioRun[0]) {
                return;
            }
            if (helper.getLevel().getEntity(commander.getUUID()) != commander
                    || helper.getLevel().getEntity(hostile.getUUID()) != hostile) {
                if (helper.getTick() >= 30L) {
                    scenarioRun[0] = true;
                    helper.fail("Command Center squad-order entities never entered the server index");
                }
                return;
            }
            scenarioRun[0] = true;
            if (!commander.isHostileFactionRecruit(hostile)) {
                helper.fail("Command Center target fixture is not faction-hostile");
                return;
            }
            CommandCenterOperationsMenu menu = (CommandCenterOperationsMenu)
                    new CommandCenterOperationsMenuProvider(hallPos)
                            .createMenu(7, owner.getInventory(), owner);
            if (menu.dashboardState().combatTargets().stream()
                    .noneMatch(target -> target.entityId().equals(hostile.getUUID()))) {
                helper.fail("Command Center dashboard omitted a nearby explicit hostile target");
                return;
            }
            Optional<UUID> selectedSquad = Optional.of(squad.id());
            boolean moveAccepted = menu.handleReplayAction(owner, UUID.randomUUID(),
                    CommandCenterOperationsMenu.MOVE_SQUAD, selectedSquad, Optional.empty());
            var moveOrder = data.armyGroup(squad.id()).orElseThrow().order();
            boolean holdAccepted = menu.handleReplayAction(owner, UUID.randomUUID(),
                    CommandCenterOperationsMenu.HOLD_SQUAD, selectedSquad, Optional.empty());
            var holdOrder = data.armyGroup(squad.id()).orElseThrow().order();
            boolean protectAccepted = menu.handleReplayAction(owner, UUID.randomUUID(),
                    CommandCenterOperationsMenu.PROTECT_SQUAD, selectedSquad, Optional.empty());
            var protectOrder = data.armyGroup(squad.id()).orElseThrow().order();
            boolean attackAccepted = menu.handleReplayAction(owner, UUID.randomUUID(),
                    CommandCenterOperationsMenu.ATTACK_SQUAD_TARGET, selectedSquad,
                    Optional.of(hostile.getUUID()));
            var attackOrder = data.armyGroup(squad.id()).orElseThrow().order();
            boolean clearAccepted = menu.handleReplayAction(owner, UUID.randomUUID(),
                    CommandCenterOperationsMenu.CLEAR_SQUAD_TARGET, selectedSquad, Optional.empty());
            var clearOrder = data.armyGroup(squad.id()).orElseThrow().order();
            boolean rallyAccepted = menu.handleReplayAction(owner, UUID.randomUUID(),
                    CommandCenterOperationsMenu.SET_SQUAD_RALLY, selectedSquad, Optional.empty());
            var rally = data.armyGroup(squad.id()).orElseThrow().rallyPoint();
            boolean friendlyTargetRejected = !menu.handleReplayAction(owner, UUID.randomUUID(),
                    CommandCenterOperationsMenu.ATTACK_SQUAD_TARGET, selectedSquad,
                    Optional.of(commander.getUUID()));
            if (!moveAccepted
                    || moveOrder.type() != galacticwars.clonewars.army.ArmyCommandType.MOVE_TO_POSITION
                    || !holdAccepted
                    || holdOrder.type() != galacticwars.clonewars.army.ArmyCommandType.HOLD_POSITION
                    || !protectAccepted
                    || protectOrder.type() != galacticwars.clonewars.army.ArmyCommandType.PROTECT_OWNER
                    || !attackAccepted
                    || attackOrder.targetEntityId().filter(hostile.getUUID()::equals).isEmpty()
                    || !clearAccepted
                    || clearOrder.type() != galacticwars.clonewars.army.ArmyCommandType.CLEAR_TARGET
                    || !rallyAccepted
                    || rally.filter(location ->
                    new Vec3(location.x(), location.y(), location.z())
                            .distanceToSqr(owner.position()) <= 4.0D).isEmpty()
                    || !friendlyTargetRejected) {
                helper.fail("Command Center selected-squad orders failed: move=" + moveAccepted + "/" + moveOrder
                        + ", hold=" + holdAccepted + "/" + holdOrder
                        + ", protect=" + protectAccepted + "/" + protectOrder
                        + ", attack=" + attackAccepted + "/" + attackOrder
                        + ", clear=" + clearAccepted + "/" + clearOrder
                        + ", rally=" + rallyAccepted + "/" + rally
                        + ", friendlyRejected=" + friendlyTargetRejected);
                return;
            }
            UUID replayId = UUID.randomUUID();
            if (!menu.handleReplayAction(owner, replayId,
                    CommandCenterOperationsMenu.FOLLOW_SQUAD, selectedSquad, Optional.empty())
                    || menu.handleReplayAction(owner, replayId,
                    CommandCenterOperationsMenu.HOLD_SQUAD, selectedSquad, Optional.empty())
                    || data.armyGroup(squad.id()).orElseThrow().order().type()
                    != galacticwars.clonewars.army.ArmyCommandType.FOLLOW_OWNER) {
                helper.fail("Command Center squad command replay protection was not idempotent");
                return;
            }
            helper.succeed();
        });
    }

    private static void commandCenterWorkforceControl(GameTestHelper helper) {
        BlockPos hallPos = isolatedCapital(helper, 15);
        CommandCenterBlockEntity hall = placeCommandCenter(helper, hallPos);
        ServerPlayer owner = makeConnectedMockPlayer(helper, GameType.CREATIVE);
        owner.setPos(hallPos.getX() + 0.5D, hallPos.getY(), hallPos.getZ() + 0.5D);
        hall.claim(owner);
        hall.setFaction("galacticwars:republic");
        KingdomSavedData data = KingdomSavedData.get(helper.getLevel());
        KingdomRecord kingdom = data.activateHall(
                owner.getUUID(), hall.factionId(),
                helper.getLevel().dimension().identifier().toString(), hallPos).orElse(null);
        if (kingdom == null) {
            helper.fail("Command Center workforce setup could not activate its isolated Hall");
            return;
        }
        FactionAlignmentSavedData.get(helper.getLevel()).setScore(
                owner.getUUID(), FactionId.of("republic"), 100);
        GalacticRecruitEntity worker = helper.spawn(
                ModEntityTypes.CLONE_TROOPER.get(), new BlockPos(3, 1, 1));
        worker.setNoAi(true);
        boolean[] scenarioRun = {false};
        helper.onEachTick(() -> {
            if (scenarioRun[0]) {
                return;
            }
            if (helper.getLevel().getEntity(worker.getUUID()) != worker) {
                if (helper.getTick() >= 30L) {
                    scenarioRun[0] = true;
                    helper.fail("Command Center workforce recruit never entered the server index");
                }
                return;
            }
            scenarioRun[0] = true;
            owner.setPos(worker.getX(), worker.getY(), worker.getZ());
            boolean hired = worker.handleMenuButton(owner, RecruitCommandMenu.BUTTON_HIRE);
            boolean assigned = hired && worker.handleMenuButton(
                    owner, RecruitCommandMenu.BUTTON_ASSIGN_FARMER);
            owner.setPos(hallPos.getX() + 0.5D, hallPos.getY(), hallPos.getZ() + 0.5D);
            CommandCenterOperationsMenu menu = (CommandCenterOperationsMenu)
                    new CommandCenterOperationsMenuProvider(hallPos)
                            .createMenu(8, owner.getInventory(), owner);
            boolean listed = menu.dashboardState().workers().stream()
                    .anyMatch(summary -> summary.entityId().equals(worker.getUUID())
                            && summary.profession().equals("farmer")
                            && summary.worksite().isPresent());
            Optional<UUID> selectedWorker = Optional.of(worker.getUUID());
            boolean pauseAccepted = menu.handleReplayAction(
                    owner, UUID.randomUUID(), CommandCenterOperationsMenu.PAUSE_WORKER,
                    selectedWorker, Optional.empty());
            boolean paused = worker.getRecruitCommand() == RecruitmentAction.HOLD_POSITION
                    && worker.getWorkerStatus().phase() == WorkerPhase.PAUSED
                    && worker.getWorkerStatus().reasonCode().equals("paused_by_command_center");
            boolean recallAccepted = menu.handleReplayAction(
                    owner, UUID.randomUUID(), CommandCenterOperationsMenu.RECALL_WORKER,
                    selectedWorker, Optional.empty());
            boolean recalled = worker.getRecruitCommand() == RecruitmentAction.MOVE_TO_POSITION
                    && hallPos.equals(worker.getMoveTarget())
                    && worker.getWorkerStatus().phase() == WorkerPhase.PAUSED
                    && worker.getWorkerStatus().reasonCode().equals("recalled_to_command_center");
            boolean resumeAccepted = menu.handleReplayAction(
                    owner, UUID.randomUUID(), CommandCenterOperationsMenu.RESUME_WORKER,
                    selectedWorker, Optional.empty());
            boolean resumed = worker.getRecruitCommand() == RecruitmentAction.WORK_AT_SITE
                    && worker.getWorkerStatus().phase() == WorkerPhase.ACQUIRE_ORDER
                    && worker.getWorkerStatus().reasonCode().equals("return_to_worksite");
            boolean unknownRejected = !menu.handleReplayAction(
                    owner, UUID.randomUUID(), CommandCenterOperationsMenu.PAUSE_WORKER,
                    Optional.of(UUID.randomUUID()), Optional.empty());
            if (!hired || !assigned || !listed
                    || !pauseAccepted || !paused
                    || !recallAccepted || !recalled
                    || !resumeAccepted || !resumed
                    || !unknownRejected) {
                helper.fail("Command Center workforce control failed: hired=" + hired
                        + ", assigned=" + assigned + ", listed=" + listed
                        + ", pause=" + pauseAccepted + "/" + paused
                        + ", recall=" + recallAccepted + "/" + recalled
                        + ", resume=" + resumeAccepted + "/" + resumed
                        + ", unknownRejected=" + unknownRejected
                        + ", command=" + worker.getRecruitCommand()
                        + ", status=" + worker.getWorkerStatus());
                return;
            }
            helper.succeed();
        });
    }

    private static void blueprintProjectorRuntime(GameTestHelper helper) {
        BlockPos hallPos = isolatedCapital(helper, 13);
        CommandCenterBlockEntity hall = placeCommandCenter(helper, hallPos);
        ServerPlayer owner = makeConnectedMockPlayer(helper, GameType.CREATIVE);
        GalacticRecruitEntity recruit = spawnRecruitAt(
                helper, ModEntityTypes.CLONE_TROOPER.get(),
                helper.absolutePos(new BlockPos(3, 1, 1)));
        owner.setPos(recruit.getX(), recruit.getY(), recruit.getZ());
        hall.claim(owner);
        KingdomSavedData data = KingdomSavedData.get(helper.getLevel());
        KingdomRecord kingdom = data.activateHall(
                owner.getUUID(), hall.factionId(),
                helper.getLevel().dimension().identifier().toString(), hallPos).orElseThrow();
        FactionAlignmentSavedData.get(helper.getLevel()).setScore(
                owner.getUUID(), FactionId.of("republic"), 100);
        if (!recruit.handleMenuButton(owner, RecruitCommandMenu.BUTTON_HIRE)) {
            helper.fail("Blueprint projector builder could not be hired");
            return;
        }
        KingdomBaseBlueprint blueprint = GameplayDataManager.snapshot()
                .blueprint("galacticwars:barracks").orElseThrow();
        BlockPos origin = hallPos.offset(4, 8, 4);
        for (int placementIndex = 0; placementIndex < blueprint.placements().size(); placementIndex++) {
            var placement = blueprint.rotatedPlacement(placementIndex, 0);
            helper.getLevel().getChunkAt(origin.offset(
                    placement.x(), placement.y(), placement.z()));
        }
        helper.getLevel().setBlock(origin.below(), Blocks.STONE.defaultBlockState(), 3);
        BlockPos loadedBuilderPosition = helper.absolutePos(new BlockPos(3, 1, 1));
        int[] scenarioPhase = {0};
        long[] phaseStartedTick = {helper.getTick()};
        BuildProject[] startedProject = {null};
        UUID[] startedWorkOrder = {null};
        helper.onEachTick(() -> {
            if (scenarioPhase[0] >= 2) {
                return;
            }
            if (helper.getLevel().getEntity(recruit.getUUID()) != recruit) {
                if (helper.getTick() - phaseStartedTick[0] >= 30L) {
                    int stalledPhase = scenarioPhase[0];
                    scenarioPhase[0] = 2;
                    helper.fail("Blueprint projector builder did not enter the server entity index in phase "
                            + stalledPhase);
                }
                return;
            }
            if (scenarioPhase[0] == 1) {
                scenarioPhase[0] = 2;
                verifyBlueprintProjectCancellation(
                        helper, owner, recruit, data, startedProject[0], startedWorkOrder[0]);
                return;
            }
            owner.setPos(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D);
            ItemStack projector = new ItemStack(ModItems.BLUEPRINT_PROJECTOR.get());
            projector.set(ModDataComponents.CONSTRUCTION_PLAN.get(), new ConstructionPlan(
                    blueprint.id(), 0, recruit.getUUID(), kingdom.id()));
            owner.setItemInHand(InteractionHand.MAIN_HAND, projector);
            ItemStack heldProjector = owner.getItemInHand(InteractionHand.MAIN_HAND);
            InteractionResult interaction = heldProjector.getItem().useOn(new UseOnContext(
                    owner,
                    InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(origin.below()), Direction.UP, origin.below(), false)));
            if (interaction != InteractionResult.SUCCESS
                    || heldProjector.get(ModDataComponents.CONSTRUCTION_PLAN.get()) != null) {
                var diagnostic = ConstructionProjectService.start(
                        helper.getLevel(), owner,
                        new ConstructionPlan(blueprint.id(), 0, recruit.getUUID(), kingdom.id()),
                        origin);
                helper.fail("Successful in-world projection did not consume its armed plan: result="
                        + interaction + ", heldPlan="
                        + heldProjector.get(ModDataComponents.CONSTRUCTION_PLAN.get())
                        + ", localPlan=" + projector.get(ModDataComponents.CONSTRUCTION_PLAN.get())
                        + ", duty=" + recruit.getRecruitDuty()
                        + ", profession=" + recruit.getWorkerProfession()
                        + ", base=" + recruit.getBaseTarget()
                        + ", diagnostic=" + diagnostic.reason()
                        + ", indexed=" + (helper.getLevel().getEntity(recruit.getUUID()) == recruit)
                        + ", kingdom=" + recruit.getKingdomId());
                return;
            }
            BuildProject project = data.kingdomForOwner(owner.getUUID()).orElseThrow().settlement()
                    .buildProjects().stream()
                    .filter(candidate -> candidate.blueprintId().equals(blueprint.id()))
                    .filter(candidate -> candidate.originX() == origin.getX()
                            && candidate.originY() == origin.getY()
                            && candidate.originZ() == origin.getZ())
                    .findFirst().orElse(null);
            if (project == null
                    || recruit.getWorkerProfession().filter(WorkerProfession.BUILDER::equals).isEmpty()
                    || recruit.getRecruitDuty() != RecruitDuty.WORKER
                    || !origin.equals(recruit.getBaseTarget())
                    || !origin.equals(recruit.getWorkTarget())
                    || data.assignedWorksite(owner.getUUID(), recruit.getUUID())
                            .flatMap(WorksiteRecord::sourceProjectId)
                            .filter(project.id()::equals).isEmpty()) {
                helper.fail("Projection did not create a persisted project-specific builder assignment");
                return;
            }
            putContainerItem(hall, new ItemStack(Items.OAK_PLANKS, 64));
            putContainerItem(hall, new ItemStack(Items.OAK_LOG, 64));
            setWorkerPhase(recruit, WorkerPhase.ACQUIRE_ORDER, "ready", null);
            recruit.tickWorkerController();
            UUID workOrderId = (UUID) getRecruitField(recruit, "workOrderId");
            if (workOrderId == null || !recruit.getWorkerStatus().reasonCode()
                    .equals("withdraw_build_material")) {
                helper.fail("Projected build did not enter the persisted material-hauling loop");
                return;
            }
            interactWorkerAt(recruit, hallPos, "withdraw_build_material");
            setWorkerPhase(recruit, WorkerPhase.ACQUIRE_ORDER, "ready", null);
            recruit.tickWorkerController();
            var firstPlacement = blueprint.rotatedPlacement(0, 0);
            BlockPos firstPosition = origin.offset(
                    firstPlacement.x(), firstPlacement.y(), firstPlacement.z());
            interactWorkerAt(recruit, firstPosition, "build_place");
            BuildProject progressed = data.kingdomForOwner(owner.getUUID()).orElseThrow().settlement()
                    .buildProjects().stream().filter(candidate -> candidate.id().equals(project.id()))
                    .findFirst().orElseThrow();
            if (!helper.getLevel().getBlockState(firstPosition)
                    .is(BuiltInRegistries.BLOCK.getValue(Identifier.parse(firstPlacement.blockId())))
                    || !progressed.completedPlacements().contains(0)
                    || data.workOrder(owner.getUUID(), workOrderId).isEmpty()) {
                helper.fail("Projected builder did not place and persist its first blueprint block");
                return;
            }
            startedProject[0] = project;
            startedWorkOrder[0] = workOrderId;
            scenarioPhase[0] = 1;
            phaseStartedTick[0] = helper.getTick();
            recruit.teleportTo(
                    loadedBuilderPosition.getX() + 0.5D,
                    loadedBuilderPosition.getY(),
                    loadedBuilderPosition.getZ() + 0.5D);
            recruit.getNavigation().stop();
        });
    }

    private static void commandMarkerRuntime(GameTestHelper helper) {
        BlockPos hallPos = isolatedCapital(helper, 16);
        for (int x = -1; x <= 7; x++) {
            for (int z = -1; z <= 5; z++) {
                BlockPos floor = hallPos.offset(x, -1, z);
                ChunkPos chunk = new ChunkPos(floor.getX() >> 4, floor.getZ() >> 4);
                helper.getLevel().getChunkSource().updateChunkForced(chunk, true);
                helper.getLevel().getChunk(chunk.x(), chunk.z());
                helper.getLevel().setBlockAndUpdate(
                        floor, Blocks.STONE.defaultBlockState());
            }
        }
        CommandCenterBlockEntity hall = placeCommandCenter(helper, hallPos);
        ServerPlayer owner = makeConnectedMockPlayer(helper, GameType.CREATIVE);
        owner.setPos(hallPos.getX() + 0.5D, hallPos.getY(), hallPos.getZ() + 0.5D);
        hall.claim(owner);
        hall.setFaction("galacticwars:republic");
        KingdomSavedData data = KingdomSavedData.get(helper.getLevel());
        KingdomRecord kingdom = data.activateHall(
                owner.getUUID(), hall.factionId(),
                helper.getLevel().dimension().identifier().toString(), hallPos).orElse(null);
        if (kingdom == null) {
            helper.fail("Command marker scenario could not activate its isolated kingdom");
            return;
        }
        ProgressionSavedData progression = ProgressionSavedData.get(helper.getLevel());
        progression.apply(new ProgressionEvent(
                UUID.randomUUID(), owner.getUUID(), ProgressionEventType.FACTION_PLEDGED,
                "galacticwars:republic", 1));
        FactionAlignmentSavedData.get(helper.getLevel()).setScore(
                owner.getUUID(), FactionId.of("republic"), 100);

        BlockPos recruitSpawn = helper.absolutePos(new BlockPos(2, 1, 1));
        BlockPos hostileSpawn = helper.absolutePos(new BlockPos(5, 1, 1));
        helper.getLevel().setBlockAndUpdate(recruitSpawn.below(), Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(hostileSpawn.below(), Blocks.STONE.defaultBlockState());
        GalacticRecruitEntity recruit = spawnRecruitAt(
                helper, ModEntityTypes.CLONE_TROOPER.get(), recruitSpawn);
        GalacticRecruitEntity hostile = spawnRecruitAt(
                helper, ModEntityTypes.B1_BATTLE_DROID.get(), hostileSpawn);
        hostile.setNoAi(true);
        hostile.initializeNaturalFactionNpc(UUID.randomUUID(), NpcServiceBranch.MILITARY);
        owner.setPos(recruit.getX(), recruit.getY(), recruit.getZ());
        if (!recruit.handleMenuButton(owner, RecruitCommandMenu.BUTTON_HIRE)) {
            helper.fail("Command marker recruit could not be hired");
            return;
        }

        boolean[] scenarioRun = {false};
        long indexWaitStartedTick = helper.getTick();
        helper.onEachTick(() -> {
            if (scenarioRun[0]) {
                return;
            }
            boolean recruitIndexed = helper.getLevel().getEntity(recruit.getUUID()) == recruit;
            boolean hostileIndexed = helper.getLevel().getEntity(hostile.getUUID()) == hostile;
            if (!recruitIndexed || !hostileIndexed) {
                if (helper.getTick() - indexWaitStartedTick >= 120L) {
                    scenarioRun[0] = true;
                    helper.fail("Command marker entities never entered the server entity index: recruit="
                            + recruitIndexed + ", hostile=" + hostileIndexed);
                }
                return;
            }
            scenarioRun[0] = true;
            verifyCommandMarkerActions(
                    helper, hallPos, hall, owner, kingdom, recruit, hostile, progression);
        });
    }

    private static void verifyCommandMarkerActions(
            GameTestHelper helper,
            BlockPos hallPos,
            CommandCenterBlockEntity hall,
            ServerPlayer owner,
            KingdomRecord kingdom,
            GalacticRecruitEntity recruit,
            GalacticRecruitEntity hostile,
            ProgressionSavedData progression
    ) {
        Vec3 managedRecruitPosition = recruit.position();
        ItemStack marker = new ItemStack(ModItems.COMMAND_MARKER.get());
        owner.setItemInHand(InteractionHand.MAIN_HAND, marker);
        ItemStack heldMarker = owner.getItemInHand(InteractionHand.MAIN_HAND);
        owner.setShiftKeyDown(true);
        InteractionResult entitySelection = heldMarker.interactLivingEntity(
                owner, hostile, InteractionHand.MAIN_HAND);
        owner.setShiftKeyDown(false);
        CommandTargetSelection selectedEntity = heldMarker.get(ModDataComponents.COMMAND_TARGET.get());
        Optional<net.minecraft.world.entity.LivingEntity> resolvedEntity =
                CommandTargetSelection.entityFromInventory(owner);
        boolean attackAccepted = recruit.handleMenuButton(owner, RecruitCommandMenu.BUTTON_ATTACK);
        boolean attackTargetAssigned = recruit.getTarget() == hostile;

        BlockPos destination = hallPos.offset(4, 0, 3);
        helper.getLevel().setBlock(destination, Blocks.CHEST.defaultBlockState(), 3);
        InteractionResult blockSelection = heldMarker.getItem().useOn(new UseOnContext(
                owner,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(destination), Direction.UP, destination, false)));
        CommandTargetSelection selectedBlock = heldMarker.get(ModDataComponents.COMMAND_TARGET.get());
        boolean courierAssigned = recruit.handleMenuButton(
                owner, RecruitCommandMenu.BUTTON_ASSIGN_COURIER);
        boolean worksiteAccepted = courierAssigned && recruit.handleMenuButton(
                owner, RecruitCommandMenu.BUTTON_SET_WORKSITE);

        heldMarker.getItem().useOn(new UseOnContext(
                owner,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(hallPos), Direction.UP, hallPos, false)));
        boolean storageAccepted = recruit.handleMenuButton(
                owner, RecruitCommandMenu.BUTTON_SET_STORAGE);
        invokeWorkerAuthorityReconciliation(recruit, helper.getLevel());
        boolean courierRouteReconciled = hallPos.equals(recruit.getStorageTarget())
                && destination.equals(recruit.getWorkTarget());
        putContainerItem(hall, new ItemStack(ModItems.DURACRETE.get(), 3));
        UUID courierOrder = acquirePersistedOrder(recruit);
        interactWorkerAt(recruit, hallPos, "courier_withdraw");
        setWorkerPhase(recruit, WorkerPhase.ACQUIRE_ORDER, "ready", null);
        recruit.tickWorkerController();
        depositWorkerAt(recruit, destination);
        boolean courierCompleted = KingdomSavedData.get(helper.getLevel())
                .workOrder(owner.getUUID(), courierOrder)
                .filter(order -> order.state() == WorkOrderState.COMPLETED)
                .isPresent();
        boolean deliveryRecorded = progression.state(owner.getUUID())
                .total(ProgressionEventType.DELIVERY_COMPLETED) == 1;
        int deliveredDuracrete = helper.getLevel().getBlockEntity(destination) instanceof Container destinationStorage
                ? countContainerItem(destinationStorage, ModItems.DURACRETE.get())
                : 0;
        recruit.setPos(managedRecruitPosition);
        recruit.setDeltaMovement(Vec3.ZERO);
        recruit.getNavigation().stop();
        owner.setPos(recruit.getX(), recruit.getY(), recruit.getZ());
        boolean projectorPrepared = recruit.handleMenuButton(
                owner, RecruitCommandMenu.BUTTON_BUILD_STARTER_KEEP);
        ConstructionPlan projectorPlan = null;
        for (int slot = 0; slot < owner.getInventory().getContainerSize(); slot++) {
            ItemStack stack = owner.getInventory().getItem(slot);
            if (stack.is(ModItems.BLUEPRINT_PROJECTOR.get())) {
                projectorPlan = stack.get(ModDataComponents.CONSTRUCTION_PLAN.get());
                break;
            }
        }

        if (entitySelection != InteractionResult.SUCCESS
                || selectedEntity == null
                || selectedEntity.entityId().filter(hostile.getUUID()::equals).isEmpty()
                || !attackAccepted
                || !attackTargetAssigned
                || blockSelection != InteractionResult.SUCCESS
                || selectedBlock == null
                || selectedBlock.entityId().isPresent()
                || !selectedBlock.blockPos().equals(destination)
                || !courierAssigned
                || !worksiteAccepted
                || !destination.equals(recruit.getWorkTarget())
                || !storageAccepted
                || !hallPos.equals(recruit.getStorageTarget())
                || !courierRouteReconciled
                || !courierCompleted
                || !deliveryRecorded
                || deliveredDuracrete != 3
                || !projectorPrepared
                || projectorPlan == null
                || !projectorPlan.builderId().equals(recruit.getUUID())
                || !projectorPlan.kingdomId().equals(kingdom.id())) {
            helper.fail("Command marker did not bridge physical selection into recruit controls: entity="
                    + entitySelection + "/" + selectedEntity
                    + ", attack=" + attackAccepted + "/" + attackTargetAssigned
                    + ", resolved=" + resolvedEntity
                    + ", factions=" + recruit.getRecruitFactionId() + "/" + hostile.getRecruitFactionId()
                    + ", relation=" + recruit.factionRelationTo(hostile)
                    + ", hostile=" + recruit.isHostileFactionRecruit(hostile)
                    + ", duty=" + hostile.getRecruitDuty()
                    + ", block=" + blockSelection + "/" + selectedBlock
                    + ", courier=" + courierAssigned
                    + ", worksite=" + worksiteAccepted + "/" + recruit.getWorkTarget()
                    + ", storage=" + storageAccepted + "/" + recruit.getStorageTarget()
                    + ", reconciled=" + courierRouteReconciled
                    + ", delivery=" + courierCompleted + "/" + deliveryRecorded
                    + "/" + deliveredDuracrete
                    + ", courierStatus=" + recruit.getWorkerStatus()
                    + ", carried=" + workerInventoryCount(recruit)
                    + ", shouldRun=" + recruit.shouldRunWorkerCycle()
                    + ", courierOrder=" + KingdomSavedData.get(helper.getLevel())
                    .workOrder(owner.getUUID(), courierOrder)
                    + ", assigned=" + KingdomSavedData.get(helper.getLevel())
                    .assignedWorksite(owner.getUUID(), recruit.getUUID())
                    + ", projector=" + projectorPrepared + "/" + projectorPlan);
            return;
        }
        if (!completeForwardBaseFromPreparedProjector(
                helper, hallPos, hall, owner, recruit)) {
            return;
        }
        ProgressionState frontierState = progression.state(owner.getUUID());
        boolean chapterOneComplete = frontierState.hasSubject(
                ProgressionEventType.QUEST_ADVANCED, "republic_chapter_1");
        boolean forwardBaseRecorded = frontierState.hasSubjectPath(
                ProgressionEventType.BUILDING_COMPLETED, "forward_base");
        boolean planetTravelUnlocked = frontierState.unlocks().contains("planet_travel");
        boolean chapterTwoWaitingForTravel = !frontierState.hasSubject(
                ProgressionEventType.QUEST_ADVANCED, "republic_chapter_2");
        if (!chapterOneComplete
                || !forwardBaseRecorded
                || !planetTravelUnlocked
                || !chapterTwoWaitingForTravel) {
            helper.fail("Chapter-two frontier journey did not unlock travel at the correct boundary: chapterOne="
                    + chapterOneComplete + ", delivery=" + deliveryRecorded
                    + ", forwardBase=" + forwardBaseRecorded
                    + ", travel=" + planetTravelUnlocked
                    + ", waitingForPlanet=" + chapterTwoWaitingForTravel
                    + ", unlocks=" + frontierState.unlocks());
            return;
        }
        helper.succeed();
    }

    private static void vehicleEmbodiedRuntime(GameTestHelper helper) {
        SmartBrainTestArea area = prepareSmartBrainTestArea(
                helper, GameType.SURVIVAL, -4, 36, -4, 50);
        ServerPlayer owner = area.player();
        ServerPlayer intruder = makeConnectedMockPlayer(helper, GameType.SURVIVAL);
        intruder.setPos(
                area.at(1, 1, 1).getX() + 0.5D,
                area.at(1, 1, 1).getY(),
                area.at(1, 1, 1).getZ() + 0.5D);
        List<GalacticVehicleEntity> vehicles = new java.util.ArrayList<>();
        int lane = 0;
        for (var holder : ModEntityTypes.vehicles()) {
            GalacticVehicleEntity vehicle = spawnVehicleAt(
                    helper, holder.get(), area.at(2 + lane * 7, 1, 4));
            vehicle.deploy(owner.getUUID(), "galacticwars:republic");
            vehicles.add(vehicle);
            lane++;
        }
        VehicleEmbodiedTestScenario scenario = new VehicleEmbodiedTestScenario(
                helper, owner, intruder, vehicles);
        helper.onEachTick(scenario::tick);
    }

    private static void chapterThreeCampaignRuntime(GameTestHelper helper) {
        SmartBrainTestArea area = prepareSmartBrainTestAreaAt(
                helper, GameType.SURVIVAL, -4, 24, -4, 24,
                isolatedCapital(helper, 17));
        ServerPlayer owner = area.player();
        BlockPos hallPos = area.at(1, 1, 1);
        CommandCenterBlockEntity hall = placeCommandCenter(helper, hallPos);
        hall.claim(owner);
        hall.setFaction("galacticwars:republic");
        KingdomSavedData kingdoms = KingdomSavedData.get(helper.getLevel());
        KingdomRecord kingdom = kingdoms.foundKingdom(
                owner.getUUID(), hall.factionId(),
                helper.getLevel().dimension().identifier().toString(), hallPos);
        ProgressionSavedData progression = ProgressionSavedData.get(helper.getLevel());
        applyCampaignSetupEvent(
                progression, owner, ProgressionEventType.BUILDING_COMPLETED, "command_center");
        applyCampaignSetupEvent(
                progression, owner, ProgressionEventType.FACTION_PLEDGED, "galacticwars:republic");
        FactionAlignmentSavedData.get(helper.getLevel()).setScore(
                owner.getUUID(), FactionId.of("republic"), 100);
        applyCampaignSetupEvent(
                progression, owner, ProgressionEventType.RECRUIT_HIRED, "clone_trooper");
        applyCampaignSetupEvent(
                progression, owner, ProgressionEventType.DELIVERY_COMPLETED, "chapter_three_supply_run");
        applyCampaignSetupEvent(
                progression, owner, ProgressionEventType.BUILDING_COMPLETED, "forward_base");
        applyCampaignSetupEvent(
                progression, owner, ProgressionEventType.PLANET_VISITED, "kamino");
        applyCampaignSetupEvent(
                progression, owner, ProgressionEventType.BUILDING_COMPLETED, "supply_depot");
        ProgressionState chapterTwo = progression.state(owner.getUUID());
        if (!chapterTwo.hasSubject(ProgressionEventType.QUEST_ADVANCED, "republic_chapter_2")
                || !chapterTwo.unlocks().contains("vehicle_crafting")) {
            helper.fail("Chapter-three setup did not cross the real Chapter 2 and Supply Depot gates");
            return;
        }

        putContainerItem(hall, new ItemStack(ModItems.CREDIT_CHIP.get(), 32));
        putContainerItem(hall, new ItemStack(ModItems.REPUBLIC_PLASTOID_INGOT.get(), 8));
        putContainerItem(hall, new ItemStack(ModItems.ENERGY_CELL.get(), 4));
        putContainerItem(hall, new ItemStack(ModItems.DURACRETE.get(), 16));
        owner.setPos(hallPos.getX() + 0.5D, hallPos.getY(), hallPos.getZ() + 0.5D);
        CommandCenterOperationsMenu fabricationMenu = (CommandCenterOperationsMenu)
                new CommandCenterOperationsMenuProvider(hallPos)
                        .createMenu(20, owner.getInventory(), owner);
        UUID fabricationId = UUID.randomUUID();
        boolean fabricated = fabricationMenu.handleReplayAction(
                owner, fabricationId, CommandCenterOperationsMenu.FABRICATE_FIRST);
        boolean fabricationReplayRejected = !fabricationMenu.handleReplayAction(
                owner, fabricationId, CommandCenterOperationsMenu.FABRICATE_FIRST);
        int kitSlot = findPlayerItemSlot(owner, ModItems.BARC_SPEEDER_DEPLOYMENT_KIT.get());
        if (!fabricated || !fabricationReplayRejected || kitSlot < 0
                || CreditTransactionService.containerBalance(hall) != 0
                || countContainerItem(hall, ModItems.REPUBLIC_PLASTOID_INGOT.get()) != 0
                || countContainerItem(hall, ModItems.ENERGY_CELL.get()) != 0
                || countContainerItem(hall, ModItems.DURACRETE.get()) != 0) {
            helper.fail("Command Center fabrication did not atomically consume the BARC recipe: accepted="
                    + fabricated + ", replay=" + fabricationReplayRejected + ", kitSlot=" + kitSlot
                    + ", credits=" + CreditTransactionService.containerBalance(hall));
            return;
        }
        owner.getInventory().setSelectedSlot(kitSlot);
        BlockPos deploymentFloor = area.at(5, 0, 4);
        InteractionResult deployed = owner.getMainHandItem().getItem().useOn(new UseOnContext(
                owner, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(deploymentFloor), Direction.UP, deploymentFloor, false)));
        if (deployed != InteractionResult.SUCCESS || !owner.getMainHandItem().isEmpty()
                || !progression.state(owner.getUUID()).hasSubject(
                ProgressionEventType.VEHICLE_ACQUIRED, "barc_speeder")) {
            helper.fail("Fabricated BARC kit did not deploy into owned in-world gameplay: result="
                    + deployed + ", held=" + owner.getMainHandItem());
            return;
        }

        GalacticRecruitEntity merchant = spawnRecruitAt(
                helper, ModEntityTypes.REPUBLIC_CIVILIAN.get(), area.at(3, 1, 8));
        merchant.setNoAi(true);
        merchant.initializeFromSpawnEgg();
        merchant.tame(owner);
        merchant.setWorkerProfession(WorkerProfession.MERCHANT);
        BlockPos beacon = area.at(10, 1, 10);
        helper.getLevel().setBlockAndUpdate(beacon, ModBlocks.CONTROL_BEACON.get().defaultBlockState());
        GalacticRecruitEntity escort = spawnRecruitAt(
                helper, ModEntityTypes.CLONE_TROOPER.get(), beacon.offset(1, 0, 0));
        escort.setNoAi(true);
        escort.initializeFromSpawnEgg();
        escort.tame(owner);
        var region = LaunchContentCatalog.data().conquestRegions().get("tatooine_spaceport");
        ConquestSavedData.get(helper.getLevel()).put(new ConquestControlState(
                region.id(), helper.getLevel().dimension().identifier().toString(),
                beacon.getX(), beacon.getY(), beacon.getZ(), "galacticwars:hutt_cartel",
                "", "", 0, 0L));
        helper.setTime(101L);
        ChapterThreeCampaignTestScenario scenario = new ChapterThreeCampaignTestScenario(
                helper, area, owner, hallPos, hall, kingdom, merchant, escort,
                deploymentFloor, beacon, region);
        helper.onEachTick(scenario::tick);
    }

    private static void applyCampaignSetupEvent(
            ProgressionSavedData progression,
            ServerPlayer player,
            ProgressionEventType type,
            String subject
    ) {
        ProgressionDecision decision = progression.apply(new ProgressionEvent(
                UUID.randomUUID(), player.getUUID(), type, subject, 1));
        if (!decision.accepted()) {
            throw new IllegalStateException(
                    "Campaign setup rejected " + type + "/" + subject + ": " + decision.reason());
        }
    }

    private static int findPlayerItemSlot(ServerPlayer player, net.minecraft.world.item.Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(item)) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean completeForwardBaseFromPreparedProjector(
            GameTestHelper helper,
            BlockPos hallPos,
            CommandCenterBlockEntity hall,
            ServerPlayer owner,
            GalacticRecruitEntity recruit
    ) {
        KingdomBaseBlueprint blueprint = GameplayDataManager.snapshot()
                .blueprint("galacticwars:forward_base").orElseThrow();
        BlockPos origin = hallPos.offset(0, 8, 0);
        for (int placementIndex = 0; placementIndex < blueprint.placements().size(); placementIndex++) {
            var placement = blueprint.rotatedPlacement(placementIndex, 0);
            BlockPos placementPos = origin.offset(
                    placement.x(), placement.y(), placement.z());
            helper.getLevel().getChunkAt(placementPos);
            helper.getLevel().setBlock(placementPos, Blocks.AIR.defaultBlockState(), 3);
        }
        helper.getLevel().setBlock(origin.below(), Blocks.STONE.defaultBlockState(), 3);
        ItemStack projector = ItemStack.EMPTY;
        for (int slot = 0; slot < owner.getInventory().getContainerSize(); slot++) {
            ItemStack candidate = owner.getInventory().getItem(slot);
            ConstructionPlan plan = candidate.get(ModDataComponents.CONSTRUCTION_PLAN.get());
            if (candidate.is(ModItems.BLUEPRINT_PROJECTOR.get())
                    && plan != null
                    && plan.blueprintId().equals(blueprint.id())) {
                projector = candidate;
                break;
            }
        }
        if (projector.isEmpty()) {
            helper.fail("Prepared Forward Base projector was not present in the player's inventory");
            return false;
        }
        owner.setPos(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D);
        owner.setItemInHand(InteractionHand.MAIN_HAND, projector);
        ItemStack heldProjector = owner.getItemInHand(InteractionHand.MAIN_HAND);
        InteractionResult projection = heldProjector.getItem().useOn(new UseOnContext(
                owner,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(origin.below()), Direction.UP, origin.below(), false)));
        KingdomSavedData data = KingdomSavedData.get(helper.getLevel());
        BuildProject project = data.kingdomForOwner(owner.getUUID()).orElseThrow().settlement()
                .buildProjects().stream()
                .filter(candidate -> candidate.blueprintId().equals(blueprint.id()))
                .filter(candidate -> candidate.originX() == origin.getX()
                        && candidate.originY() == origin.getY()
                        && candidate.originZ() == origin.getZ())
                .findFirst().orElse(null);
        if (projection != InteractionResult.SUCCESS
                || project == null
                || recruit.getWorkerProfession().filter(WorkerProfession.BUILDER::equals).isEmpty()
                || !origin.equals(recruit.getBaseTarget())) {
            var diagnostic = ConstructionProjectService.start(
                    helper.getLevel(), owner,
                    new ConstructionPlan(blueprint.id(), 0, recruit.getUUID(),
                            data.kingdomForOwner(owner.getUUID()).orElseThrow().id()),
                    origin);
            helper.fail("Forward Base projector did not create and assign an embodied build: projection="
                    + projection + ", project=" + project
                    + ", profession=" + recruit.getWorkerProfession()
                    + ", base=" + recruit.getBaseTarget()
                    + ", work=" + recruit.getWorkTarget()
                    + ", diagnostic=" + diagnostic.reason()
                    + ", indexed=" + (helper.getLevel().getEntity(recruit.getUUID()) == recruit)
                    + ", kingdom=" + recruit.getKingdomId());
            return false;
        }

        putContainerItem(hall, new ItemStack(ModItems.DURACRETE.get(), 64));
        putContainerItem(hall, new ItemStack(ModItems.NIGHTSISTER_WEAVE_LOG.get(), 64));
        putContainerItem(hall, new ItemStack(Items.OAK_PLANKS, 64));
        UUID workOrderId = null;
        for (int placementIndex = 0; placementIndex < blueprint.placements().size(); placementIndex++) {
            setWorkerPhase(recruit, WorkerPhase.ACQUIRE_ORDER, "ready", null);
            recruit.tickWorkerController();
            if (workOrderId == null) {
                workOrderId = (UUID) getRecruitField(recruit, "workOrderId");
            }
            if (!recruit.getWorkerStatus().reasonCode().equals("withdraw_build_material")) {
                helper.fail("Forward Base builder did not request material for placement "
                        + placementIndex + ": status=" + recruit.getWorkerStatus()
                        + ", carried=" + workerInventoryCount(recruit)
                        + ", storage=" + recruit.getStorageTarget());
                return false;
            }
            interactWorkerAt(recruit, hallPos, "withdraw_build_material");
            setWorkerPhase(recruit, WorkerPhase.ACQUIRE_ORDER, "ready", null);
            recruit.tickWorkerController();
            BuildProject current = data.kingdomForOwner(owner.getUUID()).orElseThrow().settlement()
                    .buildProjects().stream()
                    .filter(candidate -> candidate.id().equals(project.id()))
                    .findFirst().orElseThrow();
            var placement = blueprint.rotatedPlacement(placementIndex, current.rotationSteps());
            BlockPos placementPos = origin.offset(
                    placement.x(), placement.y(), placement.z());
            interactWorkerAt(recruit, placementPos, "build_place");
        }
        setWorkerPhase(recruit, WorkerPhase.ACQUIRE_ORDER, "ready", null);
        recruit.tickWorkerController();
        boolean orderComplete = workOrderId != null && data.workOrder(owner.getUUID(), workOrderId)
                .filter(order -> order.state() == WorkOrderState.COMPLETED).isPresent();
        boolean projectComplete = data.kingdomForOwner(owner.getUUID()).orElseThrow().settlement()
                .buildProjects().stream()
                .filter(candidate -> candidate.id().equals(project.id()))
                .anyMatch(candidate -> candidate.state()
                        == galacticwars.clonewars.kingdom.BuildProjectState.COMPLETED);
        if (!orderComplete || !projectComplete || recruit.getBaseTarget() != null) {
            helper.fail("Forward Base builder did not complete its physical project: order="
                    + orderComplete + ", project=" + projectComplete
                    + ", status=" + recruit.getWorkerStatus()
                    + ", base=" + recruit.getBaseTarget());
            return false;
        }
        return true;
    }

    private static void verifyBlueprintProjectCancellation(
            GameTestHelper helper,
            ServerPlayer owner,
            GalacticRecruitEntity recruit,
            KingdomSavedData data,
            BuildProject project,
            UUID workOrderId
    ) {
        var cancelled = ConstructionProjectService.cancel(
                helper.getLevel(), owner, project.id());
        BuildProject cancelledProject = data.kingdomForOwner(owner.getUUID()).orElseThrow()
                .settlement().buildProjects().stream()
                .filter(candidate -> candidate.id().equals(project.id())).findFirst().orElseThrow();
        if (!cancelled.accepted()
                || cancelledProject.state()
                != galacticwars.clonewars.kingdom.BuildProjectState.CANCELLED
                || data.workOrder(owner.getUUID(), workOrderId)
                        .filter(order -> order.state() == WorkOrderState.CANCELLED).isEmpty()
                || data.assignedWorksite(owner.getUUID(), recruit.getUUID()).isPresent()
                || recruit.getBaseTarget() != null
                || recruit.getWorkTarget() != null) {
            helper.fail("Construction cancellation left stale state: result=" + cancelled
                    + ", project=" + cancelledProject.state()
                    + ", order=" + data.workOrder(owner.getUUID(), workOrderId)
                    + ", worksite=" + data.assignedWorksite(owner.getUUID(), recruit.getUUID())
                    + ", base=" + recruit.getBaseTarget()
                    + ", work=" + recruit.getWorkTarget());
            return;
        }
        helper.succeed();
    }

    private static void kingdomGovernancePersistence(GameTestHelper helper) {
        ServerPlayer owner = makeConnectedMockPlayer(helper, GameType.CREATIVE);
        ServerPlayer builder = makeConnectedMockPlayer(helper, GameType.CREATIVE);
        ServerPlayer enemyOwner = makeConnectedMockPlayer(helper, GameType.CREATIVE);
        KingdomSavedData data = KingdomSavedData.get(helper.getLevel());
        // SavedData is shared by the whole GameTest world. Keep governance-only
        // claims away from the physical test structures and their Command Centers.
        BlockPos capitalPos = helper.absolutePos(new BlockPos(1, 1, 1)).offset(0, 0, 10_000);
        KingdomRecord kingdom = data.foundKingdom(
                owner.getUUID(), "galacticwars:republic",
                helper.getLevel().dimension().identifier().toString(), capitalPos);
        if (data.establishTreaty(owner.getUUID(), kingdom.id(),
                helper.getLevel().getGameTime(), 200L, 0L)) {
            helper.fail("Kingdom established a treaty with itself");
        }
        if (!data.addMember(owner.getUUID(), builder.getUUID(), KingdomMemberRole.BUILDER,
                "galacticwars:republic")
                || !data.allows(builder.getUUID(), KingdomPermission.BUILD)
                || data.allows(builder.getUUID(), KingdomPermission.MANAGE_CLAIMS)) {
            helper.fail("Kingdom membership roles did not enforce their permissions");
        }

        KingdomClaim capitalClaim = data.kingdom(kingdom.id()).orElseThrow().claims().getFirst();
        net.minecraft.world.level.ChunkPos expansion = new net.minecraft.world.level.ChunkPos(
                capitalClaim.center().x() + 2, capitalClaim.center().z());
        if (data.expandClaim(builder.getUUID(), capitalClaim.id(), capitalClaim.dimensionId(), expansion)
                || !data.expandClaim(owner.getUUID(), capitalClaim.id(), capitalClaim.dimensionId(), expansion)) {
            helper.fail("Claim expansion ignored role or contiguity authority");
        }

        BlockPos enemyCapital = capitalPos.offset(512, 0, 0);
        KingdomRecord enemy = data.foundKingdom(
                enemyOwner.getUUID(), "galacticwars:separatist",
                helper.getLevel().dimension().identifier().toString(), enemyCapital);
        long diplomacyTime = helper.getLevel().getGameTime();
        FactionId republicFaction = FactionId.of("galacticwars:republic");
        FactionId separatistFaction = FactionId.of("galacticwars:separatist");
        if (KingdomFactionRelations.resolve(
                        GameplayDataManager.snapshot().factions(), data,
                        kingdom.id(), republicFaction, enemy.id(), separatistFaction, diplomacyTime)
                        != FactionRelation.ENEMY
                || !data.setRelation(owner.getUUID(), enemy.id(), KingdomRelation.NEUTRAL, diplomacyTime, 0L)
                || !data.establishTreaty(owner.getUUID(), enemy.id(), diplomacyTime, 100L, 0L)
                || KingdomFactionRelations.resolve(
                        GameplayDataManager.snapshot().factions(), data,
                        kingdom.id(), republicFaction, enemy.id(), separatistFaction, diplomacyTime + 99L)
                        != FactionRelation.ALLY
                || KingdomFactionRelations.resolve(
                        GameplayDataManager.snapshot().factions(), data,
                        kingdom.id(), republicFaction, enemy.id(), separatistFaction, diplomacyTime + 100L)
                        != FactionRelation.NEUTRAL
                || !data.setRelation(
                        owner.getUUID(), enemy.id(), KingdomRelation.ENEMY, diplomacyTime + 100L, 200L)
                || !data.setEmbargo(owner.getUUID(), enemy.id(), true)
                || !data.relation(kingdom.id(), enemy.id()).embargo()) {
            helper.fail("Kingdom diplomacy did not override faction defaults or expire treaties safely");
            return;
        }
        SettlementRecord enemyOutpost = SettlementRecord.create(
                helper.getLevel().dimension().identifier().toString(),
                enemyCapital.getX() + 256, enemyCapital.getY(), enemyCapital.getZ());
        KingdomClaim outpostClaim = data.addOutpost(enemyOwner.getUUID(), enemyOutpost).orElseThrow();
        KingdomSiege siege = data.startSiege(
                owner.getUUID(), outpostClaim.id(), true, true, 10,
                helper.getLevel().getGameTime()).orElseThrow();
        KingdomSiege captured = data.progressSiege(
                siege.id(), 12, 2, helper.getLevel().getGameTime() + 20,
                List.of(owner.getUUID()), List.of(enemyOwner.getUUID())).orElseThrow();
        if (captured.state() != SiegeState.CAPTURED
                || data.kingdom(kingdom.id()).orElseThrow().claims().stream()
                        .noneMatch(claim -> claim.id().equals(outpostClaim.id()))) {
            helper.fail("Outpost siege did not transfer claim authority atomically");
        }

        Tag encoded = KingdomSavedData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        KingdomSavedData restored = KingdomSavedData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        if (restored.schemaVersion() != KingdomSavedData.CURRENT_SCHEMA_VERSION
                || restored.kingdomForPlayer(builder.getUUID()).isEmpty()
                || !restored.allows(builder.getUUID(), KingdomPermission.BUILD)
                || restored.sieges().stream().noneMatch(stored -> stored.id().equals(siege.id())
                        && stored.state() == SiegeState.CAPTURED)) {
            helper.fail("Kingdom governance state did not survive its SavedData codec round trip");
        }
        helper.succeed();
    }

    private static void kingdomMultiplayerRuntime(GameTestHelper helper) {
        ServerPlayer owner = makeConnectedMockPlayer(helper, GameType.CREATIVE);
        ServerPlayer officer = makeConnectedMockPlayer(helper, GameType.CREATIVE);
        ServerPlayer builder = makeConnectedMockPlayer(helper, GameType.CREATIVE);
        ServerPlayer member = makeConnectedMockPlayer(helper, GameType.CREATIVE);
        ServerPlayer intruder = makeConnectedMockPlayer(helper, GameType.CREATIVE);
        KingdomSavedData data = KingdomSavedData.get(helper.getLevel());
        BlockPos capitalPos = helper.absolutePos(new BlockPos(1, 1, 1)).offset(0, 0, 20_000);
        KingdomRecord kingdom = data.foundKingdom(
                owner.getUUID(), "galacticwars:republic",
                helper.getLevel().dimension().identifier().toString(), capitalPos);
        if (!data.addMember(owner.getUUID(), officer.getUUID(), KingdomMemberRole.OFFICER,
                "galacticwars:republic")
                || !data.addMember(owner.getUUID(), builder.getUUID(), KingdomMemberRole.BUILDER, "")
                || !data.addMember(owner.getUUID(), member.getUUID(), KingdomMemberRole.MEMBER, "")) {
            helper.fail("Multiplayer kingdom setup rejected compatible member roles");
            return;
        }

        BlockPos deniedBlock = capitalPos.offset(1, 0, 1);
        BlockPos allowedBlock = capitalPos.offset(2, 0, 1);
        helper.getLevel().setBlock(deniedBlock, Blocks.STONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(allowedBlock, Blocks.STONE.defaultBlockState(), 3);
        intruder.setPos(deniedBlock.getX() + 0.5, deniedBlock.getY(), deniedBlock.getZ() + 0.5);
        member.setPos(deniedBlock.getX() + 0.5, deniedBlock.getY(), deniedBlock.getZ() + 0.5);
        builder.setPos(allowedBlock.getX() + 0.5, allowedBlock.getY(), allowedBlock.getZ() + 0.5);
        intruder.gameMode.destroyBlock(deniedBlock);
        member.gameMode.destroyBlock(deniedBlock);
        builder.gameMode.destroyBlock(allowedBlock);
        if (!helper.getLevel().getBlockState(deniedBlock).is(Blocks.STONE)
                || !helper.getLevel().getBlockState(allowedBlock).isAir()) {
            helper.fail("Runtime claim protection did not deny outsiders/members and allow builders");
            return;
        }

        KingdomBaseBlueprint forwardBase = GameplayDataManager.snapshot()
                .blueprint("galacticwars:forward_base").orElseThrow();
        BuildProject firstBase = fullyProgressProject(
                data, owner.getUUID(), forwardBase,
                helper.getLevel().dimension().identifier().toString(), capitalPos.offset(32, 0, 0));
        BuildProject secondBase = fullyProgressProject(
                data, owner.getUUID(), forwardBase,
                helper.getLevel().dimension().identifier().toString(), capitalPos.offset(64, 0, 0));
        if (!data.completeBuildProject(owner.getUUID(), firstBase, forwardBase)
                || !data.completeBuildProject(owner.getUUID(), secondBase, forwardBase)) {
            helper.fail("Multiple squad setup could not earn two commander slots");
            return;
        }
        GalacticRecruitEntity firstCommanderEntity = helper.spawn(
                ModEntityTypes.CLONE_TROOPER.get(), new BlockPos(2, 1, 2));
        GalacticRecruitEntity secondCommanderEntity = helper.spawn(
                ModEntityTypes.ARC_TROOPER.get(), new BlockPos(3, 1, 2));
        GalacticRecruitEntity soldierEntity = helper.spawn(
                ModEntityTypes.CLONE_TROOPER.get(), new BlockPos(4, 1, 2));
        firstCommanderEntity.tame(owner);
        secondCommanderEntity.tame(owner);
        soldierEntity.tame(owner);
        firstCommanderEntity.setNoAi(true);
        secondCommanderEntity.setNoAi(true);
        soldierEntity.setNoAi(true);
        UUID firstCommander = firstCommanderEntity.getUUID();
        UUID secondCommander = secondCommanderEntity.getUUID();
        UUID soldier = soldierEntity.getUUID();
        if (!data.registerRecruit(owner.getUUID(), firstCommander)
                || !data.registerRecruit(owner.getUUID(), secondCommander)
                || !data.registerRecruit(owner.getUUID(), soldier)
                || !data.promoteCommander(officer.getUUID(), firstCommander)) {
            helper.fail("Officer could not register and promote the first military squad");
            return;
        }
        ArmyLocation anchor = new ArmyLocation(
                helper.getLevel().dimension().identifier().toString(),
                capitalPos.getX(), capitalPos.getY(), capitalPos.getZ());
        var firstSquad = data.createOrReclaimArmyGroup(
                officer.getUUID(), firstCommander, ArmyFormation.LINE, anchor,
                helper.getLevel().getGameTime()).orElseThrow();
        var secondSquad = data.splitArmyGroup(
                officer.getUUID(), firstSquad.id(), secondCommander, List.of(soldier),
                "Outer Rim Patrol", anchor, helper.getLevel().getGameTime()).orElse(null);
        if (secondSquad == null || data.armyGroupsForKingdom(kingdom.id()).size() != 2
                || !data.configureArmyGroup(
                        officer.getUUID(), secondSquad.id(), "Outer Rim Patrol", anchor,
                        List.of(anchor, new ArmyLocation(anchor.dimensionId(),
                                anchor.x() + 32, anchor.y(), anchor.z())),
                        Optional.of(kingdom.claims().getFirst().id()))
                || !data.changeArmySupply(officer.getUUID(), secondSquad.id(), 32)) {
            helper.fail("Named multiple squads, patrol, claim defense, or supply authority failed");
            return;
        }
        for (int tick = 0; tick < 20; tick++) {
            secondCommanderEntity.tick();
        }
        officer.setPos(secondCommanderEntity.getX(), secondCommanderEntity.getY(), secondCommanderEntity.getZ());
        builder.setPos(secondCommanderEntity.getX(), secondCommanderEntity.getY(), secondCommanderEntity.getZ());
        if (!secondCommanderEntity.handleMenuButton(officer, RecruitCommandMenu.BUTTON_HOLD)
                || secondCommanderEntity.handleMenuButton(builder, RecruitCommandMenu.BUTTON_HOLD)
                || data.armyGroup(secondSquad.id()).orElseThrow().order().type()
                        != galacticwars.clonewars.army.ArmyCommandType.HOLD_POSITION) {
            helper.fail("Officer army command authority was not honored by the recruit runtime menu");
            return;
        }
        if (!secondCommanderEntity.handleMenuButton(officer, RecruitCommandMenu.BUTTON_PATROL)
                || secondCommanderEntity.handleMenuButton(builder, RecruitCommandMenu.BUTTON_PATROL)
                || data.armyGroup(secondSquad.id()).orElseThrow().order().type()
                        != galacticwars.clonewars.army.ArmyCommandType.PATROL_ROUTE
                || data.armyGroup(secondSquad.id()).orElseThrow().patrolRoute().size() != 2) {
            helper.fail("Playable officer patrol command did not persist an executable route");
            return;
        }
        if (data.setNpcServiceBranch(owner.getUUID(), soldier, NpcServiceBranch.CIVILIAN)
                || !data.releaseArmyMember(owner.getUUID(), soldier, false, anchor)
                || !data.reserveWorksite(owner.getUUID(), soldier, WorkerProfession.FARMER)
                || data.addRecruitToArmy(owner.getUUID(), secondSquad.id(), soldier)
                || !data.kingdom(kingdom.id()).orElseThrow().npc(soldier)
                        .map(entry -> entry.serviceBranch() == NpcServiceBranch.CIVILIAN).orElse(false)) {
            helper.fail("Military and civilian roster separation was not enforced atomically");
            return;
        }
        helper.succeed();
    }

    private static void overworldFactionOutpostRuntime(GameTestHelper helper) {
        OverworldFactionSpawnProfile profile = GameplayDataManager.snapshot().overworldSpawnProfiles()
                .get("galacticwars:hutt_cartel");
        if (profile == null
                || profile.branchFor("galacticwars:hutt_enforcer") != NpcServiceBranch.MILITARY
                || profile.branchFor("galacticwars:hutt_civilian") != NpcServiceBranch.CIVILIAN) {
            helper.fail("Datapack Overworld faction spawn profiles were not loaded atomically");
            return;
        }
        BlockPos center = helper.absolutePos(new BlockPos(1, 1, 1)).offset(0, 0, 30_000);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                helper.getLevel().setBlock(center.offset(dx, -1, dz), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                helper.getLevel().setBlock(center.offset(dx, 0, dz), Blocks.AIR.defaultBlockState(), 3);
                helper.getLevel().setBlock(center.offset(dx, 1, dz), Blocks.AIR.defaultBlockState(), 3);
            }
        }
        if (!FactionNaturalSpawnRules.check(
                ModEntityTypes.HUTT_ENFORCER.get(), helper.getLevel(),
                net.minecraft.world.entity.EntitySpawnReason.NATURAL, center,
                helper.getLevel().getRandom())) {
            helper.fail("Valid Overworld faction ground was rejected by natural spawn placement");
            return;
        }
        GalacticRecruitEntity guard = helper.spawn(ModEntityTypes.HUTT_ENFORCER.get(), new BlockPos(1, 1, 1));
        GalacticRecruitEntity civilian = helper.spawn(ModEntityTypes.HUTT_CIVILIAN.get(), new BlockPos(2, 1, 1));
        guard.setPos(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
        civilian.setPos(center.getX() + 1.5, center.getY(), center.getZ() + 0.5);
        FinalizeSpawnEvent guardSpawn = new FinalizeSpawnEvent(
                guard, helper.getLevel(), guard.getX(), guard.getY(), guard.getZ(),
                helper.getLevel().getCurrentDifficultyAt(center),
                net.minecraft.world.entity.EntitySpawnReason.NATURAL, null, null);
        FinalizeSpawnEvent civilianSpawn = new FinalizeSpawnEvent(
                civilian, helper.getLevel(), civilian.getX(), civilian.getY(), civilian.getZ(),
                helper.getLevel().getCurrentDifficultyAt(center),
                net.minecraft.world.entity.EntitySpawnReason.NATURAL, null, null);
        FactionNaturalSpawnEvents.onFinalizeSpawn(guardSpawn);
        FactionNaturalSpawnEvents.onFinalizeSpawn(civilianSpawn);
        FactionOutpostSavedData data = FactionOutpostSavedData.get(helper.getLevel());
        FactionOutpostRecord outpost = data.outpostForNpc(guard.getUUID()).orElse(null);
        FactionOutpostRecord shared = data.outpostForNpc(civilian.getUUID()).orElse(null);
        if (guardSpawn.isSpawnCancelled() || civilianSpawn.isSpawnCancelled()
                || outpost == null || shared == null || !outpost.id().equals(shared.id())
                || !data.siteGenerated(shared.id())) {
            helper.fail("Natural faction NPCs did not share a persisted visible Overworld outpost");
            return;
        }
        civilian.setWorkerProfession(WorkerProfession.COOK);
        boolean producedSupplies = civilian.tryProduceNaturalSettlementSupplies();
        civilian.tick();
        int storedBread = helper.getLevel().getBlockEntity(center.offset(1, 0, 0))
                instanceof net.minecraft.world.Container container ? container.countItem(Items.BREAD) : 0;
        if (!helper.getLevel().getBlockState(center.offset(1, 0, 0)).is(Blocks.BARREL)
                || !helper.getLevel().getBlockState(center.offset(0, 2, 0)).is(Blocks.SANDSTONE)
                || !civilian.hasHome()
                || civilian.getServiceBranch() != NpcServiceBranch.CIVILIAN
                || !civilian.getRecruitFactionId().equals("galacticwars:hutt_cartel")
                || Math.round(civilian.getMaxHealth()) != 18
                || !producedSupplies || storedBread != 1) {
            helper.fail("Generated Hutt shelter, civilian archetype, or physical production was incomplete");
            return;
        }

        GalacticRecruitEntity republic = helper.spawn(ModEntityTypes.CLONE_TROOPER.get(), new BlockPos(4, 1, 1));
        GalacticRecruitEntity separatist = helper.spawn(ModEntityTypes.B1_BATTLE_DROID.get(), new BlockPos(5, 1, 1));
        republic.setPos(center.getX() + 4.5, center.getY(), center.getZ() + 0.5);
        separatist.setPos(center.getX() + 5.5, center.getY(), center.getZ() + 0.5);
        republic.initializeNaturalFactionNpc(UUID.randomUUID(), NpcServiceBranch.MILITARY);
        separatist.initializeNaturalFactionNpc(UUID.randomUUID(), NpcServiceBranch.MILITARY);
        ServerPlayer enemyPlayer = makeConnectedMockPlayer(helper, GameType.SURVIVAL);
        ProgressionSavedData.get(helper.getLevel()).apply(new ProgressionEvent(
                UUID.randomUUID(), enemyPlayer.getUUID(), ProgressionEventType.FACTION_PLEDGED,
                "galacticwars:separatist", 1));
        if (!republic.canNaturallyEngage(separatist)
                || !republic.canNaturallyEngagePlayer(enemyPlayer)
                || civilian.canNaturallyEngage(republic)
                || civilian.canNaturallyEngagePlayer(enemyPlayer)
                || civilian.getServiceBranch() != NpcServiceBranch.CIVILIAN) {
            helper.fail("Natural military hostility or civilian noncombatant behavior was incorrect");
            return;
        }
        Tag encoded = FactionOutpostSavedData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        FactionOutpostSavedData restored = FactionOutpostSavedData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        if (restored.outpost(shared.id()).isEmpty() || !restored.siteGenerated(shared.id())
                || restored.outpostForNpc(civilian.getUUID()).isEmpty()) {
            helper.fail("Overworld faction outpost population did not survive SavedData round trip");
            return;
        }
        helper.succeed();
    }

    private static void physicalTradeTransaction(GameTestHelper helper) {
        ServerPlayer player = makeConnectedMockPlayer(helper, GameType.SURVIVAL);
        ProgressionSavedData progression = ProgressionSavedData.get(helper.getLevel());
        ProgressionDecision pledge = progression.apply(new ProgressionEvent(
                UUID.randomUUID(), player.getUUID(), ProgressionEventType.FACTION_PLEDGED,
                "galacticwars:republic", 1));
        if (!pledge.accepted() || !pledge.state().unlocks().contains("faction_intro")) {
            helper.fail("Faction pledge did not unlock introductory physical trade");
        }
        player.getInventory().add(new ItemStack(ModItems.CREDIT_CHIP.get(), 12));
        UUID eventId = UUID.randomUUID();
        PhysicalTradeService.TradeResult purchase = PhysicalTradeService.purchase(
                player, eventId, "republic_quartermaster");
        PhysicalTradeService.TradeResult missingTrade = PhysicalTradeService.purchase(
                player, UUID.randomUUID(), null);
        int energyCells = player.getInventory().getNonEquipmentItems().stream()
                .filter(stack -> stack.is(ModItems.ENERGY_CELL.get()))
                .mapToInt(ItemStack::getCount).sum();
        if (!purchase.accepted() || !purchase.changed() || purchase.creditsCharged() != 12
                || missingTrade.accepted() || !missingTrade.reason().equals("unknown_trade")
                || CreditTransactionService.playerBalance(player) != 0 || energyCells != 8) {
            helper.fail("Physical trade did not atomically exchange Credit Chips for goods");
        }
        PhysicalTradeService.TradeResult replay = PhysicalTradeService.purchase(
                player, eventId, "republic_quartermaster");
        int replayedEnergyCells = player.getInventory().getNonEquipmentItems().stream()
                .filter(stack -> stack.is(ModItems.ENERGY_CELL.get()))
                .mapToInt(ItemStack::getCount).sum();
        if (!replay.accepted() || replay.changed() || replayedEnergyCells != energyCells) {
            helper.fail("Duplicate physical trade charged or granted twice");
        }
        helper.succeed();
    }

    private static void recruitEntityContract(GameTestHelper helper) {
        GalacticRecruitEntity recruit = helper.spawn(ModEntityTypes.CLONE_TROOPER.get(), new BlockPos(1, 1, 1));
        recruit.setWorkerProfession(WorkerProfession.MINER);
        if (!recruit.isAlive()
                || recruit.getRecruitDuty() != RecruitDuty.WORKER
                || !recruit.getMainHandItem().is(Items.IRON_PICKAXE)) {
            helper.fail("Recruit entity did not apply its persisted worker duty and held equipment contract");
        }
        helper.succeed();
    }

    private static void workerTagsAndLoot(GameTestHelper helper) {
        BlockPos stonePos = new BlockPos(1, 1, 1);
        BlockPos logPos = new BlockPos(2, 1, 1);
        helper.setBlock(stonePos, ModBlocks.DURACRETE.get());
        helper.setBlock(logPos, Blocks.OAK_LOG);
        if (!helper.getBlockState(stonePos).is(ModBlockTags.WORKER_MINEABLE)
                || !helper.getBlockState(logPos).is(ModBlockTags.WORKER_LOGS)) {
            helper.fail("Worker allowlist tags were not loaded from the datapack");
        }
        List<ItemStack> drops = Block.getDrops(
                helper.getBlockState(stonePos),
                helper.getLevel(),
                helper.absolutePos(stonePos),
                null);
        if (drops.stream().mapToInt(ItemStack::getCount).sum() != 1
                || drops.stream().noneMatch(stack -> stack.is(ModBlocks.DURACRETE.asItem()))) {
            helper.fail("duracrete loot table did not conserve one mined block");
        }
        helper.succeed();
    }

    private static void workforceSavedDataAuthority(GameTestHelper helper) {
        ServerPlayer owner = makeConnectedMockPlayer(helper, GameType.CREATIVE);
        BlockPos hallPos = isolatedCapital(helper, 3);
        KingdomSavedData data = KingdomSavedData.get(helper.getLevel());
        KingdomRecord kingdom = data.activateHall(
                owner.getUUID(), "galacticwars:republic",
                helper.getLevel().dimension().identifier().toString(), hallPos).orElseThrow();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        if (!data.registerRecruit(owner.getUUID(), first)
                || !data.registerRecruit(owner.getUUID(), second)
                || !data.registerRecruit(owner.getUUID(), third)
                || !data.reserveWorksite(owner.getUUID(), first, WorkerProfession.FARMER)
                || !data.reserveWorksite(owner.getUUID(), second, WorkerProfession.FARMER)
                || data.reserveWorksite(owner.getUUID(), third, WorkerProfession.FARMER)) {
            helper.fail("Frontier worksite did not enforce its persisted two-worker capacity");
        }
        WorksiteRecord worksite = data.assignedWorksite(owner.getUUID(), first).orElseThrow();
        WorkOrder queued = new WorkOrder(
                UUID.randomUUID(), WorkOrderType.FARM, java.util.Optional.empty(), WorkOrderState.QUEUED,
                java.util.Optional.of(worksite.id()), java.util.Optional.empty(), worksite.dimensionId(),
                worksite.x(), worksite.y(), worksite.z(), "minecraft:wheat", 1, 0, "", 0);
        WorkOrder claimed = data.queueAndClaimWorkOrder(owner.getUUID(), first, queued).orElseThrow();
        WorkOrder blocked = data.blockWorkOrder(
                owner.getUUID(), claimed.id(), claimed.revision(), "target_unloaded").orElseThrow();
        WorkOrder resumed = data.resumeWorkOrder(
                owner.getUUID(), blocked.id(), first, blocked.revision()).orElseThrow();
        WorkOrder completed = data.progressWorkOrder(
                owner.getUUID(), resumed.id(), resumed.revision(), 1).orElseThrow();
        if (completed.state() != WorkOrderState.COMPLETED
                || !data.releaseWorkerAssignments(owner.getUUID(), first)
                || !data.reserveWorksite(owner.getUUID(), third, WorkerProfession.FARMER)) {
            helper.fail("Persisted work order transitions or capacity release failed");
        }
        Tag encoded = KingdomSavedData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        KingdomSavedData restored = KingdomSavedData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        if (restored.assignedWorksite(owner.getUUID(), third).isEmpty()
                || restored.workOrder(owner.getUUID(), completed.id())
                        .filter(order -> order.state() == WorkOrderState.COMPLETED).isEmpty()
                || !restored.kingdomForOwner(owner.getUUID()).orElseThrow().id().equals(kingdom.id())) {
            helper.fail("Worksite and work order authority did not survive a SavedData round trip");
        }
        helper.succeed();
    }

    private static void recruitContractLifecycle(GameTestHelper helper) {
        BlockPos hallPos = isolatedCapital(helper, 10);
        CommandCenterBlockEntity hall = placeCommandCenter(helper, hallPos);
        ServerPlayer owner = makeConnectedMockPlayer(helper, GameType.SURVIVAL);
        ServerPlayer intruder = makeConnectedMockPlayer(helper, GameType.SURVIVAL);
        GalacticRecruitEntity recruit = helper.spawn(
                ModEntityTypes.CLONE_TROOPER.get(), new BlockPos(3, 1, 1));
        owner.setPos(recruit.getX(), recruit.getY(), recruit.getZ());
        intruder.setPos(recruit.getX(), recruit.getY(), recruit.getZ());
        hall.claim(owner);
        KingdomSavedData data = KingdomSavedData.get(helper.getLevel());
        data.activateHall(
                owner.getUUID(),
                hall.factionId(),
                helper.getLevel().dimension().identifier().toString(),
                hallPos).orElseThrow();
        FactionAlignmentSavedData.get(helper.getLevel()).setScore(
                owner.getUUID(), FactionId.of("republic"), 100);
        FactionAlignmentSavedData.get(helper.getLevel()).setScore(
                owner.getUUID(), FactionId.of("mandalorian"), 100);
        owner.getInventory().add(new ItemStack(galacticwars.clonewars.registry.ModItems.CREDIT_CHIP.get(), 53));

        boolean hired = recruit.handleMenuButton(owner, RecruitCommandMenu.BUTTON_HIRE);
        boolean owned = recruit.isOwnedBy(owner);
        int remainingCredits = RecruitmentPaymentService.creditCount(owner);
        boolean registered = data.kingdomForOwner(owner.getUUID()).orElseThrow()
                .settlement().containsRecruit(recruit.getUUID());
        if (!hired || !owned || remainingCredits != 28 || !registered) {
            helper.fail("Exact-cost direct hiring failed: hired=" + hired
                    + ", owned=" + owned
                    + ", credits=" + remainingCredits
                    + ", registered=" + registered);
        }
        BlockPos intruderHallPos = hallPos.offset(64, 0, 0);
        CommandCenterBlockEntity intruderHall = placeCommandCenter(helper, intruderHallPos);
        intruderHall.claim(intruder);
        data.activateHall(
                intruder.getUUID(),
                intruderHall.factionId(),
                helper.getLevel().dimension().identifier().toString(),
                intruderHallPos).orElseThrow();
        FactionAlignmentSavedData.get(helper.getLevel()).setScore(
                intruder.getUUID(), FactionId.of("republic"), 100);
        GalacticRecruitEntity poorRecruit = helper.spawn(
                ModEntityTypes.CLONE_TROOPER.get(), new BlockPos(3, 1, 3));
        intruder.setPos(poorRecruit.getX(), poorRecruit.getY(), poorRecruit.getZ());
        intruder.getInventory().add(new ItemStack(galacticwars.clonewars.registry.ModItems.CREDIT_CHIP.get(), 24));
        if (poorRecruit.handleMenuButton(intruder, RecruitCommandMenu.BUTTON_HIRE)
                || RecruitmentPaymentService.creditCount(intruder) != 24
                || data.kingdomForOwner(intruder.getUUID()).orElseThrow()
                        .settlement().containsRecruit(poorRecruit.getUUID())) {
            helper.fail("Insufficient direct-hire payment mutated funds or settlement state");
        }
        if (intruderHall.chargeDailyUpkeep(helper.getLevel().getGameTime() + 24000L, 1)) {
            helper.fail("Empty Hall treasury incorrectly paid upkeep");
        }
        intruder.getInventory().add(new ItemStack(galacticwars.clonewars.registry.ModItems.CREDIT_CHIP.get()));
        if (poorRecruit.handleMenuButton(intruder, RecruitCommandMenu.BUTTON_HIRE)
                || RecruitmentPaymentService.creditCount(intruder) != 25) {
            helper.fail("Unpaid upkeep did not reject hiring without charging the player");
        }
        intruder.setPos(recruit.getX(), recruit.getY(), recruit.getZ());
        if (recruit.handleMenuButton(intruder, RecruitCommandMenu.BUTTON_FOLLOW)) {
            helper.fail("A non-owner command was accepted");
        }
        if (recruit.handleMenuButton(owner, Integer.MAX_VALUE)) {
            helper.fail("An unrecognized command button was accepted");
        }
        owner.setPos(recruit.getX() + 16.0, recruit.getY(), recruit.getZ());
        if (recruit.handleMenuButton(owner, RecruitCommandMenu.BUTTON_FOLLOW)) {
            helper.fail("An out-of-range command was accepted");
        }
        owner.setPos(recruit.getX(), recruit.getY(), recruit.getZ());
        if (!recruit.handleMenuButton(owner, RecruitCommandMenu.BUTTON_ASSIGN_MINER)
                || recruit.getWorkerProfession().filter(value -> value == WorkerProfession.MINER).isEmpty()) {
            helper.fail("Worker profession assignment failed");
        }
        setWorkerInventory(recruit, new ItemStack(Items.COBBLESTONE));
        if (recruit.handleMenuButton(owner, RecruitCommandMenu.BUTTON_RETURN_TO_SOLDIER)) {
            helper.fail("Worker with carried resources returned to soldier duty");
        }
        setWorkerInventory(recruit, ItemStack.EMPTY);
        BlockPos cancelledBuild = helper.absolutePos(new BlockPos(4, 1, 1));
        setRecruitField(recruit, "baseTarget", cancelledBuild);
        setRecruitField(recruit, "workTarget", cancelledBuild);
        if (recruit.handleMenuButton(owner, RecruitCommandMenu.BUTTON_RETURN_TO_SOLDIER)
                || !recruit.handleMenuButton(owner, RecruitCommandMenu.BUTTON_CANCEL_BUILD)
                || recruit.getBaseTarget() != null
                || recruit.getWorkTarget() != null
                || !recruit.handleMenuButton(owner, RecruitCommandMenu.BUTTON_RETURN_TO_SOLDIER)
                || recruit.getWorkerProfession().isPresent()
                || recruit.getRecruitDuty() != RecruitDuty.SOLDIER
                || !recruit.getMainHandItem().is(Items.IRON_SWORD)) {
            helper.fail("Worker build cancellation or contract exit did not preserve its guards");
        }

        GalacticRecruitEntity commander = helper.spawn(
                ModEntityTypes.MANDALORIAN_WARRIOR.get(), new BlockPos(3, 1, 2));
        owner.setPos(commander.getX(), commander.getY(), commander.getZ());
        owner.getInventory().add(new ItemStack(galacticwars.clonewars.registry.ModItems.CREDIT_CHIP.get(), 25));
        if (!commander.handleMenuButton(owner, RecruitCommandMenu.BUTTON_HIRE)) {
            helper.fail("Commander candidate could not be hired");
        }
        KingdomBaseBlueprint keepBlueprint = GameplayDataManager.snapshot()
                .blueprint(KingdomBaseBlueprint.STARTER_KEEP_ID).orElseThrow();
        BuildProject keepProject = fullyProgressProject(
                data, owner.getUUID(), keepBlueprint,
                helper.getLevel().dimension().identifier().toString(),
                hallPos.offset(8, 0, 0));
        if (!data.completeBuildProject(owner.getUUID(), keepProject, keepBlueprint)
                || !commander.handleMenuButton(owner, RecruitCommandMenu.BUTTON_PROMOTE_COMMANDER)) {
            helper.fail("Commander promotion prerequisites were not applied");
        }
        RecruitmentCampaign commanderCampaign = new RecruitmentCampaign(
                UUID.randomUUID(),
                "galacticwars:mandalorian_warrior",
                "",
                7,
                helper.getLevel().getGameTime() + 24000L,
                RecruitmentCampaignState.RESERVED,
                "reserved");
        hall.setItem(0, new ItemStack(galacticwars.clonewars.registry.ModItems.CREDIT_CHIP.get(), 7));
        if (!hall.reserveCredits(7)
                || !data.beginCampaign(owner.getUUID(), RecruitmentCampaignDecision.accepted(commanderCampaign))) {
            helper.fail("Commander campaign setup did not reserve its payment");
        }
        commander.die(helper.getLevel().damageSources().generic());
        KingdomRecord afterCommanderDeath = data.kingdomForOwner(owner.getUUID()).orElseThrow();
        RecruitmentCampaign cancelledCampaign = afterCommanderDeath.settlement().recruitmentCampaigns().stream()
                .filter(campaign -> campaign.id().equals(commanderCampaign.id()))
                .findFirst()
                .orElseThrow();
        if (afterCommanderDeath.settlement().containsRecruit(commander.getUUID())
                || afterCommanderDeath.settlement().commanderId().isPresent()
                || cancelledCampaign.active()
                || cancelledCampaign.reservedCost() != 0
                || hall.treasuryCredits() != 7) {
            helper.fail("Commander death did not release state and conserve its campaign reservation");
        }

        int housingToFill = data.kingdomForOwner(owner.getUUID()).orElseThrow().settlement().housingCapacity()
                - data.kingdomForOwner(owner.getUUID()).orElseThrow().settlement().recruitIds().size();
        for (int index = 0; index < housingToFill; index++) {
            if (!data.registerRecruit(owner.getUUID(), UUID.randomUUID())) {
                helper.fail("Could not fill settlement housing for the rejection check");
            }
        }
        GalacticRecruitEntity crowdedRecruit = helper.spawn(
                ModEntityTypes.CLONE_TROOPER.get(), new BlockPos(4, 1, 2));
        owner.setPos(crowdedRecruit.getX(), crowdedRecruit.getY(), crowdedRecruit.getZ());
        owner.getInventory().add(new ItemStack(galacticwars.clonewars.registry.ModItems.CREDIT_CHIP.get(), 25));
        int creditsBeforeCrowdedHire = RecruitmentPaymentService.creditCount(owner);
        if (crowdedRecruit.handleMenuButton(owner, RecruitCommandMenu.BUTTON_HIRE)
                || RecruitmentPaymentService.creditCount(owner) != creditsBeforeCrowdedHire
                || crowdedRecruit.isOwnedBy(owner)) {
            helper.fail("Full housing did not reject hiring without charging the player: before="
                    + creditsBeforeCrowdedHire + ", after="
                    + RecruitmentPaymentService.creditCount(owner));
        }
        owner.getInventory().clearContent();
        owner.setPos(recruit.getX(), recruit.getY(), recruit.getZ());

        RecruitLifecycleService.dropCarriedItems(
                helper.getLevel(), recruit, List.of(new ItemStack(Items.COBBLESTONE, 3)));
        int dropped = helper.getLevel().getEntitiesOfClass(
                        ItemEntity.class, recruit.getBoundingBox().inflate(3.0))
                .stream()
                .filter(item -> item.getItem().is(Items.COBBLESTONE))
                .mapToInt(item -> item.getItem().getCount())
                .sum();
        if (dropped != 3) {
            helper.fail("Recruit lifecycle item drops did not conserve carried resources");
        }
        recruit.die(helper.getLevel().damageSources().generic());
        if (data.kingdomForOwner(owner.getUUID()).orElseThrow()
                .settlement().containsRecruit(recruit.getUUID())) {
            helper.fail("Dead recruit continued consuming settlement housing");
        }
        helper.succeed();
    }

    private static void localRecruitProtectOwner(GameTestHelper helper) {
        SmartBrainTestArea area = prepareSmartBrainTestArea(
                helper, GameType.SURVIVAL, 0, 4, 0, 3);
        ServerPlayer owner = area.player();
        GalacticRecruitEntity recruit = spawnRecruitAt(
                helper, ModEntityTypes.CLONE_TROOPER.get(), area.at(1, 1, 1));
        owner.setPos(recruit.getX(), recruit.getY(), recruit.getZ());
        recruit.setOwner(owner);
        recruit.setTame(true, true);
        if (recruit.hasAuthoritativeArmyGroup()
                || !recruit.handleMenuButton(owner, RecruitCommandMenu.BUTTON_PROTECT)) {
            helper.fail("Local recruit protect-order setup failed");
            return;
        }

        GalacticRecruitEntity attacker = spawnRecruitAt(
                helper, ModEntityTypes.B1_BATTLE_DROID.get(), area.at(2, 1, 1));
        ServerPlayer enemyOwner = makeConnectedMockPlayer(helper, GameType.SURVIVAL);
        attacker.setOwner(enemyOwner);
        attacker.setTame(true, true);
        attacker.setNoAi(true);
        attacker.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024.0D);
        attacker.setHealth(attacker.getMaxHealth());
        helper.runAfterDelay(2, () -> owner.setLastHurtByMob(attacker));
        helper.succeedWhen(() -> {
            if (recruit.getTarget() != attacker) {
                helper.fail("Local Protect Me order did not target the owner's attacker: ownerThreat="
                        + (owner.getLastHurtByMob() == attacker)
                        + ", timestamp=" + owner.getLastHurtByMobTimestamp()
                        + ", attackerAlive=" + attacker.isAlive()
                        + ", wantsToAttack=" + recruit.wantsToAttack(attacker, owner)
                        + ", command=" + recruit.getRecruitCommand());
            }
        });
    }

    private static void workerResourceConservation(GameTestHelper helper) {
        BlockPos hallPos = isolatedCapital(helper, 11);
        BlockPos cropPos = hallPos.offset(3, 0, 0);
        BlockPos chestPos = hallPos.offset(4, 0, 0);
        helper.getLevel().setBlock(cropPos.below(), Blocks.FARMLAND.defaultBlockState(), 3);
        helper.getLevel().setBlock(
                cropPos, Blocks.WHEAT.defaultBlockState().setValue(BlockStateProperties.AGE_7, 7), 3);
        helper.getLevel().setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        CommandCenterBlockEntity hall = placeCommandCenter(helper, hallPos);
        Container chest = (Container) helper.getLevel().getBlockEntity(chestPos);
        ServerPlayer owner = makeConnectedMockPlayer(helper, GameType.SURVIVAL);
        GalacticRecruitEntity recruit = helper.spawn(
                ModEntityTypes.CLONE_TROOPER.get(), new BlockPos(3, 1, 1));
        recruit.setPos(hallPos.getX() + 2.5, hallPos.getY(), hallPos.getZ() + 0.5);
        owner.setPos(recruit.getX(), recruit.getY(), recruit.getZ());
        hall.claim(owner);
        KingdomSavedData data = KingdomSavedData.get(helper.getLevel());
        data.activateHall(
                owner.getUUID(),
                hall.factionId(),
                helper.getLevel().dimension().identifier().toString(),
                hallPos).orElseThrow();
        FactionAlignmentSavedData.get(helper.getLevel()).setScore(
                owner.getUUID(), FactionId.of("republic"), 100);
        owner.getInventory().add(new ItemStack(galacticwars.clonewars.registry.ModItems.CREDIT_CHIP.get(), 45));
        boolean hired = recruit.handleMenuButton(owner, RecruitCommandMenu.BUTTON_HIRE);
        boolean assignedFarmer = recruit.handleMenuButton(owner, RecruitCommandMenu.BUTTON_ASSIGN_FARMER);
        if (!hired || !assignedFarmer) {
            helper.fail("Farmer setup failed: hired=" + hired
                    + ", assigned=" + assignedFarmer
                    + ", credits=" + RecruitmentPaymentService.creditCount(owner)
                    + ", owned=" + recruit.isOwnedBy(owner));
        }

        setRecruitField(recruit, "workTarget", cropPos);
        setRecruitField(recruit, "storageTarget", chestPos);
        setWorkerInventory(recruit, new ItemStack(Items.WHEAT_SEEDS));
        invokeRecruitCommand(recruit, RecruitmentAction.WORK_AT_SITE);
        recruit.setPos(cropPos.getX() + 0.5, cropPos.getY(), cropPos.getZ() + 0.5);
        setRecruitField(recruit, "workerPhase", WorkerPhase.INTERACT);
        setRecruitField(recruit, "workerReason", "navigate_work_target");
        setRecruitField(recruit, "activeWorkTarget", cropPos);
        int toolDamageBefore = recruit.getMainHandItem().getDamageValue();
        recruit.tickWorkerController();
        if (helper.getLevel().getBlockState(cropPos).getValue(BlockStateProperties.AGE_7) != 0
                || recruit.getMainHandItem().getDamageValue() != toolDamageBefore + 1
                || workerInventoryCount(recruit) <= 0) {
            helper.fail("Farmer harvesting did not replant, wear its tool, and conserve drops");
        }

        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            chest.setItem(slot, new ItemStack(Items.STONE, 64));
        }
        int carriedBeforeFailedDeposit = workerInventoryCount(recruit);
        recruit.setPos(chestPos.getX() + 0.5, chestPos.getY(), chestPos.getZ() + 0.5);
        setRecruitField(recruit, "workerPhase", WorkerPhase.DEPOSIT);
        setRecruitField(recruit, "workerReason", "deposit_inventory");
        setRecruitField(recruit, "activeWorkTarget", chestPos);
        recruit.tickWorkerController();
        if (!recruit.getWorkerStatus().reasonCode().equals("storage_full_or_missing")
                || workerInventoryCount(recruit) != carriedBeforeFailedDeposit) {
            helper.fail("Failed deposit consumed worker resources");
        }

        AABB deathArea = recruit.getBoundingBox().inflate(4.0);
        recruit.die(helper.getLevel().damageSources().generic());
        helper.succeedWhen(() -> {
            int droppedWorkerItems = helper.getLevel().getEntitiesOfClass(
                            ItemEntity.class, deathArea)
                    .stream()
                    .filter(item -> item.getItem().is(Items.WHEAT) || item.getItem().is(Items.WHEAT_SEEDS))
                    .mapToInt(item -> item.getItem().getCount())
                    .sum();
            if (droppedWorkerItems != carriedBeforeFailedDeposit
                    || data.kingdomForOwner(owner.getUUID()).orElseThrow()
                            .settlement().containsRecruit(recruit.getUUID())) {
                helper.fail("Worker death did not conserve carried resources and release housing");
            }
        });
    }

    private static void enabledWorkerLoops(GameTestHelper helper) {
        BlockPos hallPos = isolatedCapital(helper, 12);
        CommandCenterBlockEntity hall = placeCommandCenter(helper, hallPos);
        ServerPlayer owner = makeConnectedMockPlayer(helper, GameType.CREATIVE);
        GalacticRecruitEntity recruit = helper.spawn(
                ModEntityTypes.CLONE_TROOPER.get(), new BlockPos(3, 1, 1));
        owner.setPos(recruit.getX(), recruit.getY(), recruit.getZ());
        hall.claim(owner);
        KingdomSavedData data = KingdomSavedData.get(helper.getLevel());
        data.activateHall(owner.getUUID(), hall.factionId(),
                helper.getLevel().dimension().identifier().toString(), hallPos).orElseThrow();
        FactionAlignmentSavedData.get(helper.getLevel()).setScore(
                owner.getUUID(), FactionId.of("republic"), 100);
        if (!recruit.handleMenuButton(owner, RecruitCommandMenu.BUTTON_HIRE)) {
            helper.fail("Enabled worker loop recruit could not be hired");
        }

        if (!recruit.handleMenuButton(owner, RecruitCommandMenu.BUTTON_ASSIGN_FARMER)) {
            helper.fail("Farmer contract was rejected");
        }
        BlockPos cropPos = hallPos.offset(3, 0, 0);
        helper.getLevel().setBlock(cropPos.below(), Blocks.FARMLAND.defaultBlockState(), 3);
        helper.getLevel().setBlock(cropPos,
                Blocks.WHEAT.defaultBlockState().setValue(BlockStateProperties.AGE_7, 7), 3);
        setWorkerInventory(recruit, new ItemStack(Items.WHEAT_SEEDS));
        UUID farmerOrder = acquirePersistedOrder(recruit);
        interactWorkerAt(recruit, cropPos, "navigate_work_target");
        depositWorkerAt(recruit, hallPos);
        assertCompletedOrder(helper, data, owner.getUUID(), farmerOrder, "farmer");
        if (helper.getLevel().getBlockState(cropPos).getValue(BlockStateProperties.AGE_7) != 0) {
            helper.fail("Farmer loop did not replant its crop");
        }

        owner.setPos(recruit.getX(), recruit.getY(), recruit.getZ());
        if (!recruit.handleMenuButton(owner, RecruitCommandMenu.BUTTON_ASSIGN_LUMBERJACK)) {
            helper.fail("Lumberjack contract was rejected");
        }
        BlockPos logPos = hallPos.offset(4, 0, 2);
        helper.getLevel().setBlock(logPos, Blocks.OAK_LOG.defaultBlockState(), 3);
        setWorkerInventory(recruit, new ItemStack(Items.OAK_SAPLING));
        UUID lumberOrder = acquirePersistedOrder(recruit);
        interactWorkerAt(recruit, logPos, "navigate_work_target");
        depositWorkerAt(recruit, hallPos);
        assertCompletedOrder(helper, data, owner.getUUID(), lumberOrder, "lumberjack");
        if (!helper.getLevel().getBlockState(logPos).is(Blocks.OAK_SAPLING)) {
            helper.fail("Lumberjack loop did not replant its sapling");
        }

        owner.setPos(recruit.getX(), recruit.getY(), recruit.getZ());
        if (!recruit.handleMenuButton(owner, RecruitCommandMenu.BUTTON_ASSIGN_MINER)) {
            helper.fail("Miner contract was rejected");
        }
        BlockPos orePos = hallPos.offset(5, 0, 2);
        helper.getLevel().setBlock(orePos, ModBlocks.DURACRETE.get().defaultBlockState(), 3);
        setWorkerInventory(recruit, ItemStack.EMPTY);
        UUID minerOrder = acquirePersistedOrder(recruit);
        interactWorkerAt(recruit, orePos, "navigate_work_target");
        depositWorkerAt(recruit, hallPos);
        assertCompletedOrder(helper, data, owner.getUUID(), minerOrder, "miner");
        if (!helper.getLevel().getBlockState(orePos).isAir()) {
            helper.fail("Miner loop did not remove its authorized block");
        }

        KingdomBaseBlueprint barracks = GameplayDataManager.snapshot()
                .blueprint("galacticwars:barracks").orElseThrow();
        // Keep multi-block projects above this test's horizontal cell. GameTests sharing the
        // gameplay environment run in parallel, so building beyond the tiny empty template in X/Z
        // can collide with a neighboring test and produce a nondeterministic blocked placement.
        BlockPos barracksOrigin = hallPos.offset(0, 16, 0);
        BuildProject barracksProject = data.startBuildProject(
                owner.getUUID(), barracks, helper.getLevel().dimension().identifier().toString(),
                barracksOrigin, 0).orElseThrow();
        setRecruitField(recruit, "activeBuildProjectId", barracksProject.id());
        setRecruitField(recruit, "baseTarget", barracksOrigin);
        setRecruitField(recruit, "workTarget", barracksOrigin);
        owner.setPos(recruit.getX(), recruit.getY(), recruit.getZ());
        if (!recruit.handleMenuButton(owner, RecruitCommandMenu.BUTTON_ASSIGN_BUILDER)) {
            helper.fail("Builder contract was rejected");
        }
        putContainerItem(hall, new ItemStack(Items.OAK_PLANKS, 64));
        putContainerItem(hall, new ItemStack(Items.OAK_LOG, 64));
        UUID builderOrder = null;
        for (int placementIndex = 0; placementIndex < barracks.placements().size(); placementIndex++) {
            setWorkerPhase(recruit, WorkerPhase.ACQUIRE_ORDER, "ready", null);
            recruit.tickWorkerController();
            if (builderOrder == null) {
                builderOrder = (UUID) getRecruitField(recruit, "workOrderId");
            }
            if (!recruit.getWorkerStatus().reasonCode().equals("withdraw_build_material")) {
                BuildProject current = data.kingdomForOwner(owner.getUUID()).orElseThrow().settlement()
                        .buildProjects().stream().filter(project -> project.id().equals(barracksProject.id()))
                        .findFirst().orElseThrow();
                helper.fail("Builder did not request its next material at placement " + placementIndex
                        + "; reason=" + recruit.getWorkerStatus().reasonCode()
                        + ", placed=" + current.completedPlacements().size()
                        + ", inventory=" + workerInventoryCount(recruit));
            }
            interactWorkerAt(recruit, hallPos, "withdraw_build_material");
            setWorkerPhase(recruit, WorkerPhase.ACQUIRE_ORDER, "ready", null);
            recruit.tickWorkerController();
            BuildProject current = data.kingdomForOwner(owner.getUUID()).orElseThrow().settlement()
                    .buildProjects().stream().filter(project -> project.id().equals(barracksProject.id()))
                    .findFirst().orElseThrow();
            var placement = barracks.rotatedPlacement(placementIndex, current.rotationSteps());
            BlockPos placementPos = barracksOrigin.offset(placement.x(), placement.y(), placement.z());
            interactWorkerAt(recruit, placementPos, "build_place");
        }
        setWorkerPhase(recruit, WorkerPhase.ACQUIRE_ORDER, "ready", null);
        recruit.tickWorkerController();
        assertCompletedOrder(helper, data, owner.getUUID(), builderOrder, "builder");
        if (data.kingdomForOwner(owner.getUUID()).orElseThrow().settlement().buildProjects().stream()
                .filter(project -> project.id().equals(barracksProject.id()))
                .noneMatch(project -> project.state() == galacticwars.clonewars.kingdom.BuildProjectState.COMPLETED)) {
            helper.fail("Builder loop did not complete its persisted project");
        }

        KingdomBaseBlueprint supply_depot = GameplayDataManager.snapshot()
                .blueprint("galacticwars:supply_depot").orElseThrow();
        BuildProject supply_depotProject = fullyProgressProject(
                data, owner.getUUID(), supply_depot,
                helper.getLevel().dimension().identifier().toString(), hallPos.offset(0, 32, 0));
        if (!data.completeBuildProject(owner.getUUID(), supply_depotProject, supply_depot)) {
            helper.fail("Courier supply_depot reward could not be applied");
        }
        List<StorageEndpoint> externalStorage = supply_depot.storageEndpoints(supply_depotProject);
        for (StorageEndpoint endpoint : externalStorage) {
            helper.getLevel().setBlock(
                    new BlockPos(endpoint.x(), endpoint.y(), endpoint.z()), Blocks.CHEST.defaultBlockState(), 3);
        }
        owner.setPos(recruit.getX(), recruit.getY(), recruit.getZ());
        if (!recruit.handleMenuButton(owner, RecruitCommandMenu.BUTTON_ASSIGN_COURIER)) {
            helper.fail("Courier contract was rejected");
        }
        BlockPos sourcePos = new BlockPos(
                externalStorage.getFirst().x(), externalStorage.getFirst().y(), externalStorage.getFirst().z());
        Container source = (Container) helper.getLevel().getBlockEntity(sourcePos);
        source.setItem(0, new ItemStack(Items.IRON_INGOT, 3));
        setRecruitField(recruit, "storageTarget", sourcePos);
        setRecruitField(recruit, "workTarget", hallPos);
        invokeRecruitCommand(recruit, RecruitmentAction.WORK_AT_SITE);
        UUID courierOrder = acquirePersistedOrder(recruit);
        interactWorkerAt(recruit, sourcePos, "courier_withdraw");
        setWorkerPhase(recruit, WorkerPhase.ACQUIRE_ORDER, "ready", null);
        recruit.tickWorkerController();
        depositWorkerAt(recruit, hallPos);
        assertCompletedOrder(helper, data, owner.getUUID(), courierOrder, "courier");
        if (!source.getItem(0).isEmpty() || countContainerItem(hall, Items.IRON_INGOT) < 3) {
            helper.fail("Courier loop did not conserve its transferred stack");
        }
        helper.succeed();
    }

    private static void lightsaberBoltDeflection(GameTestHelper helper) {
        GalacticRecruitEntity shooter = helper.spawn(
                ModEntityTypes.B1_BATTLE_DROID.get(), new BlockPos(2, 1, 2));
        GalacticRecruitEntity guardian = helper.spawn(
                ModEntityTypes.JEDI_KNIGHT.get(), new BlockPos(5, 1, 2));
        shooter.initializeFromSpawnEgg();
        guardian.initializeFromSpawnEgg();
        guardian.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BLUE_LIGHTSABER.get()));
        int resourceBeforeRejectedDeflection = guardian.classProgressState().resource();
        BlasterBoltEntity selfOwnedBolt = new BlasterBoltEntity(
                helper.getLevel(), guardian, new ItemStack(ModItems.BLUE_LIGHTSABER.get()), 5.0D);
        if (LightsaberDeflectionService.tryDeflect(
                selfOwnedBolt, guardian, helper.getLevel().getGameTime())
                || guardian.classProgressState().resource() != resourceBeforeRejectedDeflection) {
            helper.fail("Rejected self-owned bolt deflection consumed saber guard resources");
            return;
        }
        BlasterBoltEntity bolt = new BlasterBoltEntity(
                helper.getLevel(), shooter, new ItemStack(ModItems.E5_BLASTER.get()), 5.0D);
        bolt.setPos(guardian.getX() - 0.5D, guardian.getEyeY(), guardian.getZ());
        bolt.setDeltaMovement(1.0D, 0.0D, 0.0D);
        ProjectileImpactEvent impact = new ProjectileImpactEvent(bolt, new EntityHitResult(guardian));
        BlasterCombatEvents.onProjectileImpact(impact);
        if (!impact.isCanceled()
                || bolt.getOwner() != guardian
                || bolt.getDeltaMovement().x >= 0.0D
                || guardian.classProgressState().resource() >= 100) {
            helper.fail("Jedi saber guard did not authoritatively deflect and charge the class ability");
            return;
        }
        helper.succeed();
    }

    private static void specialistWorkerLoops(GameTestHelper helper) {
        BlockPos hallPos = helper.absolutePos(new BlockPos(1, 1, 1));
        CommandCenterBlockEntity hall = placeCommandCenter(helper, hallPos);
        ServerPlayer owner = makeConnectedMockPlayer(helper, GameType.CREATIVE);
        GalacticRecruitEntity recruit = ModEntityTypes.CLONE_TROOPER.get().create(
                helper.getLevel(), EntitySpawnReason.TRIGGERED);
        if (recruit == null) {
            helper.fail("Could not create specialist worker recruit");
            return;
        }
        recruit.setPos(hallPos.getX() + 2.5, hallPos.getY(), hallPos.getZ() + 0.5);
        helper.getLevel().addFreshEntity(recruit);
        owner.setPos(recruit.getX(), recruit.getY(), recruit.getZ());
        hall.claim(owner);
        KingdomSavedData data = KingdomSavedData.get(helper.getLevel());
        data.activateHall(owner.getUUID(), hall.factionId(),
                helper.getLevel().dimension().identifier().toString(), hallPos).orElseThrow();
        putContainerItem(hall, new ItemStack(ModItems.CREDIT_CHIP.get(), 32));
        FactionAlignmentSavedData.get(helper.getLevel()).setScore(
                owner.getUUID(), FactionId.of("republic"), 100);
        if (!recruit.handleMenuButton(owner, RecruitCommandMenu.BUTTON_HIRE)) {
            helper.fail("Specialist worker loop recruit could not be hired");
            return;
        }

        owner.setPos(recruit.getX(), recruit.getY(), recruit.getZ());
        if (!recruit.handleMenuButton(owner, RecruitCommandMenu.BUTTON_ASSIGN_FISHERMAN)) {
            helper.fail("Fisher contract was rejected");
            return;
        }
        BlockPos waterPos = hallPos.offset(3, 0, 1);
        helper.getLevel().setBlock(waterPos, Blocks.WATER.defaultBlockState(), 3);
        UUID fisherOrder = acquirePersistedOrder(recruit);
        interactWorkerAt(recruit, waterPos, "navigate_work_target");
        depositWorkerAt(recruit, hallPos);
        assertCompletedOrder(helper, data, owner.getUUID(), fisherOrder, "fisher");
        if (countContainerItem(hall, Items.COD) + countContainerItem(hall, Items.SALMON) != 1) {
            helper.fail("Fisher did not deposit one physical catch");
            return;
        }

        owner.setPos(recruit.getX(), recruit.getY(), recruit.getZ());
        if (!recruit.handleMenuButton(owner, RecruitCommandMenu.BUTTON_ASSIGN_ANIMAL_FARMER)) {
            helper.fail("Animal farmer contract was rejected");
            return;
        }
        Animal first = EntityTypes.COW.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        Animal second = EntityTypes.COW.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        if (first == null || second == null) {
            helper.fail("Could not create livestock for animal farmer test");
            return;
        }
        first.setPos(hallPos.getX() + 3.5, hallPos.getY(), hallPos.getZ() + 2.5);
        second.setPos(hallPos.getX() + 4.5, hallPos.getY(), hallPos.getZ() + 2.5);
        helper.getLevel().addFreshEntity(first);
        helper.getLevel().addFreshEntity(second);
        helper.runAfterDelay(2, () -> finishSpecialistWorkerLoops(
                helper, hallPos, hall, owner, recruit, data, first, second));
    }

    private static void animalFarmerSpeciesPairing(GameTestHelper helper) {
        GalacticRecruitEntity recruit = helper.spawn(
                ModEntityTypes.CLONE_TROOPER.get(), new BlockPos(2, 1, 2));
        Animal cow = EntityTypes.COW.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        Animal sheep = EntityTypes.SHEEP.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        Animal secondCow = EntityTypes.COW.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        if (cow == null || sheep == null || secondCow == null) {
            helper.fail("Could not create cross-species livestock pair");
            return;
        }
        cow.setPos(recruit.getX() + 1.0D, recruit.getY(), recruit.getZ());
        sheep.setPos(recruit.getX() - 1.0D, recruit.getY(), recruit.getZ());
        secondCow.setPos(recruit.getX() + 2.0D, recruit.getY(), recruit.getZ());
        secondCow.setAge(-24_000);
        helper.getLevel().addFreshEntity(cow);
        helper.getLevel().addFreshEntity(sheep);
        helper.getLevel().addFreshEntity(secondCow);
        helper.runAfterDelay(2, () -> {
            setWorkerInventory(recruit, new ItemStack(Items.WHEAT, 2));
            invokeFeedAnimalPair(recruit, helper.getLevel());
            if (!recruit.getWorkerStatus().reasonCode().equals("breeding_pair_required")
                    || cow.isInLove()
                    || sheep.isInLove()
                    || workerInventoryCount(recruit) != 2) {
                helper.fail("Animal farmer accepted an incompatible cow/sheep breeding pair");
                return;
            }

            secondCow.setAge(0);
            helper.runAfterDelay(2, () -> {
                invokeFeedAnimalPair(recruit, helper.getLevel());
                if (!cow.isInLove()
                        || !secondCow.isInLove()
                        || sheep.isInLove()
                        || workerInventoryCount(recruit) != 0) {
                    helper.fail("Animal farmer did not select and feed the compatible cow pair; reason="
                            + recruit.getWorkerStatus().reasonCode()
                            + ", carried=" + workerInventoryCount(recruit)
                            + ", firstCow=" + cow.isAlive() + "/" + cow.canFallInLove() + "/" + cow.isInLove()
                            + ", secondCow=" + secondCow.isAlive() + "/" + secondCow.canFallInLove() + "/" + secondCow.isInLove()
                            + ", sheep=" + sheep.isAlive() + "/" + sheep.canFallInLove() + "/" + sheep.isInLove());
                    return;
                }
                helper.succeed();
            });
        });
    }

    private static void finishSpecialistWorkerLoops(
            GameTestHelper helper,
            BlockPos hallPos,
            CommandCenterBlockEntity hall,
            ServerPlayer owner,
            GalacticRecruitEntity recruit,
            KingdomSavedData data,
            Animal first,
            Animal second
    ) {
        setWorkerInventory(recruit, new ItemStack(Items.WHEAT, 2));
        UUID animalOrder = acquirePersistedOrder(recruit);
        interactWorkerAt(recruit, first.blockPosition(), "feed_animals");
        recruit.tickWorkerController();
        WorkOrder animalWorkOrder = data.workOrder(owner.getUUID(), animalOrder).orElseThrow();
        if (animalWorkOrder.state() != WorkOrderState.COMPLETED) {
            helper.fail("Animal farmer order remained " + animalWorkOrder.state()
                    + "; phase=" + recruit.getWorkerStatus().phase()
                    + ", reason=" + recruit.getWorkerStatus().reasonCode()
                    + ", carried=" + workerInventoryCount(recruit)
                    + ", first=" + first.blockPosition() + "/" + first.canFallInLove()
                    + "/" + first.isFood(new ItemStack(Items.WHEAT))
                    + ", second=" + second.blockPosition() + "/" + second.canFallInLove()
                    + "/" + second.isFood(new ItemStack(Items.WHEAT)));
            return;
        }
        if (!first.isInLove() || !second.isInLove()) {
            helper.fail("Animal farmer did not consume feed and breed a bounded pair");
            return;
        }

        owner.setPos(recruit.getX(), recruit.getY(), recruit.getZ());
        if (!recruit.handleMenuButton(owner, RecruitCommandMenu.BUTTON_ASSIGN_COOK)) {
            helper.fail("Cook contract was rejected");
            return;
        }
        putContainerItem(hall, new ItemStack(Items.BEEF));
        putContainerItem(hall, new ItemStack(Items.COAL));
        setWorkerInventory(recruit, ItemStack.EMPTY);
        UUID cookOrder = acquirePersistedOrder(recruit);
        interactWorkerAt(recruit, hallPos, "withdraw_cooking_ingredient");
        setWorkerPhase(recruit, WorkerPhase.ACQUIRE_ORDER, "ready", null);
        recruit.tickWorkerController();
        interactWorkerAt(recruit, hallPos, "withdraw_cooking_ingredient");
        setWorkerPhase(recruit, WorkerPhase.ACQUIRE_ORDER, "ready", null);
        recruit.tickWorkerController();
        interactWorkerAt(recruit, hallPos, "cook_food");
        depositWorkerAt(recruit, hallPos);
        assertCompletedOrder(helper, data, owner.getUUID(), cookOrder, "cook");
        if (countContainerItem(hall, Items.COOKED_BEEF) != 1) {
            helper.fail("Cook did not consume physical food and fuel into stored food");
            return;
        }

        owner.setPos(recruit.getX(), recruit.getY(), recruit.getZ());
        if (!recruit.handleMenuButton(owner, RecruitCommandMenu.BUTTON_ASSIGN_MERCHANT)) {
            helper.fail("Merchant contract was rejected");
            return;
        }
        UUID merchantOrder = acquirePersistedOrder(recruit);
        WorkOrder order = data.workOrder(owner.getUUID(), merchantOrder).orElseThrow();
        if (!recruit.getWorkerStatus().reasonCode().equals("market_open")
                || order.state().terminal()) {
            helper.fail("Merchant did not maintain a persisted open-market assignment");
            return;
        }
        helper.succeed();
    }

    private static void setWorkerInventory(GalacticRecruitEntity recruit, ItemStack stack) {
        NonNullList<ItemStack> inventory = NonNullList.withSize(9, ItemStack.EMPTY);
        if (!stack.isEmpty()) {
            inventory.set(0, stack.copy());
        }
        setRecruitField(recruit, "workerInventory", inventory);
    }

    private static UUID acquirePersistedOrder(GalacticRecruitEntity recruit) {
        invokeRecruitCommand(recruit, RecruitmentAction.WORK_AT_SITE);
        setWorkerPhase(recruit, WorkerPhase.ACQUIRE_ORDER, "ready", null);
        recruit.tickWorkerController();
        Object orderId = getRecruitField(recruit, "workOrderId");
        if (!(orderId instanceof UUID uuid)) {
            throw new IllegalStateException("Worker did not claim a persisted work order; profession="
                    + recruit.getWorkerProfession().orElse(null)
                    + ", phase=" + recruit.getWorkerStatus().phase()
                    + ", reason=" + recruit.getWorkerStatus().reasonCode()
                    + ", target=" + recruit.getTarget()
                    + ", tame=" + recruit.isTame()
                    + ", removed=" + recruit.isRemoved()
                    + ", duty=" + recruit.getRecruitDuty()
                    + ", command=" + recruit.getRecruitCommand()
                    + ", workTarget=" + recruit.getWorkTarget()
                    + ", shouldRun=" + recruit.shouldRunWorkerCycle());
        }
        return uuid;
    }

    private static void interactWorkerAt(GalacticRecruitEntity recruit, BlockPos target, String reason) {
        recruit.setPos(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
        setWorkerPhase(recruit, WorkerPhase.INTERACT, reason, target);
        recruit.tickWorkerController();
    }

    private static void depositWorkerAt(GalacticRecruitEntity recruit, BlockPos storage) {
        recruit.setPos(storage.getX() + 0.5, storage.getY(), storage.getZ() + 0.5);
        setWorkerPhase(recruit, WorkerPhase.DEPOSIT, "deposit_inventory", storage);
        recruit.tickWorkerController();
    }

    private static void setWorkerPhase(
            GalacticRecruitEntity recruit,
            WorkerPhase phase,
            String reason,
            BlockPos target
    ) {
        setRecruitField(recruit, "workerPhase", phase);
        setRecruitField(recruit, "workerReason", reason);
        setRecruitField(recruit, "activeWorkTarget", target);
    }

    private static void assertCompletedOrder(
            GameTestHelper helper,
            KingdomSavedData data,
            UUID ownerId,
            UUID orderId,
            String profession
    ) {
        if (orderId == null || data.workOrder(ownerId, orderId)
                .filter(order -> order.state() == WorkOrderState.COMPLETED).isEmpty()) {
            helper.fail(profession + " loop did not complete its persisted work order");
        }
    }

    private static void putContainerItem(Container container, ItemStack stack) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (container.getItem(slot).isEmpty()) {
                container.setItem(slot, stack.copy());
                container.setChanged();
                return;
            }
        }
        throw new IllegalStateException("Test container has no empty slot");
    }

    private static int countContainerItem(Container container, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int countContainerItems(Container container) {
        int count = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            count += container.getItem(slot).getCount();
        }
        return count;
    }

    private static SmartBrainTestArea prepareSmartBrainTestArea(
            GameTestHelper helper,
            GameType playerGameType,
            int minX,
            int maxX,
            int minZ,
            int maxZ
    ) {
        return prepareSmartBrainTestAreaAt(
                helper, playerGameType, minX, maxX, minZ, maxZ,
                helper.absolutePos(BlockPos.ZERO));
    }

    private static SmartBrainTestArea prepareSmartBrainTestAreaAt(
            GameTestHelper helper,
            GameType playerGameType,
            int minX,
            int maxX,
            int minZ,
            int maxZ,
            BlockPos base
    ) {
        int minChunkX = Math.min(base.getX() + minX, base.getX() + maxX) >> 4;
        int maxChunkX = Math.max(base.getX() + minX, base.getX() + maxX) >> 4;
        int minChunkZ = Math.min(base.getZ() + minZ, base.getZ() + maxZ) >> 4;
        int maxChunkZ = Math.max(base.getZ() + minZ, base.getZ() + maxZ) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
                helper.getLevel().getChunkSource().updateChunkForced(chunk, true);
                helper.getLevel().getChunk(chunkX, chunkZ);
            }
        }
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                helper.getLevel().setBlockAndUpdate(base.offset(x, 0, z), Blocks.STONE.defaultBlockState());
            }
        }
        ServerPlayer player = makeConnectedMockPlayer(helper, playerGameType);
        if (!player.teleportTo(
                helper.getLevel(),
                base.getX() + 0.5D,
                base.getY() + 1.0D,
                base.getZ() + 0.5D,
                Set.of(),
                0.0F,
                0.0F,
                true)) {
            throw new IllegalStateException("Could not load SmartBrain GameTest area");
        }
        return new SmartBrainTestArea(base, player);
    }

    private static GalacticRecruitEntity spawnRecruitAt(
            GameTestHelper helper,
            EntityType<GalacticRecruitEntity> type,
            BlockPos position
    ) {
        GalacticRecruitEntity recruit = type.create(
                helper.getLevel(), EntitySpawnReason.TRIGGERED);
        if (recruit == null) {
            throw new IllegalStateException("Could not create SmartBrain GameTest recruit");
        }
        recruit.setPos(
                position.getX() + 0.5D,
                position.getY(),
                position.getZ() + 0.5D);
        if (!helper.getLevel().addFreshEntity(recruit)) {
            throw new IllegalStateException("Could not register SmartBrain GameTest recruit");
        }
        recruit.setDeltaMovement(Vec3.ZERO);
        recruit.getNavigation().stop();
        return recruit;
    }

    private static String pathState(GalacticRecruitEntity recruit, BlockPos target) {
        net.minecraft.world.level.pathfinder.Path path = recruit.getNavigation().createPath(target, 0);
        if (path == null) {
            return "none";
        }
        return "nodes=" + path.getNodeCount()
                + ", canReach=" + path.canReach()
                + ", target=" + path.getTarget();
    }

    @SuppressWarnings("unchecked")
    private static int workerInventoryCount(GalacticRecruitEntity recruit) {
        return ((NonNullList<ItemStack>) getRecruitField(recruit, "workerInventory")).stream()
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    private static Object getRecruitField(GalacticRecruitEntity recruit, String fieldName) {
        try {
            Field field = GalacticRecruitEntity.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(recruit);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not read recruit field " + fieldName, exception);
        }
    }

    private static void setRecruitField(GalacticRecruitEntity recruit, String fieldName, Object value) {
        try {
            Field field = GalacticRecruitEntity.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(recruit, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not set recruit field " + fieldName, exception);
        }
    }

    private static GalacticVehicleEntity spawnVehicleAt(
            GameTestHelper helper,
            EntityType<GalacticVehicleEntity> type,
            BlockPos position
    ) {
        GalacticVehicleEntity vehicle = type.create(
                helper.getLevel(), EntitySpawnReason.TRIGGERED);
        if (vehicle == null) {
            throw new IllegalStateException("Could not create vehicle GameTest entity");
        }
        vehicle.setPos(
                position.getX() + 0.5D,
                position.getY(),
                position.getZ() + 0.5D);
        if (!helper.getLevel().addFreshEntity(vehicle)) {
            throw new IllegalStateException("Could not register vehicle GameTest entity");
        }
        vehicle.setDeltaMovement(Vec3.ZERO);
        return vehicle;
    }

    private static void invokeRecruitCommand(GalacticRecruitEntity recruit, RecruitmentAction command) {
        try {
            Method method = GalacticRecruitEntity.class.getDeclaredMethod(
                    "setRecruitCommand", RecruitmentAction.class);
            method.setAccessible(true);
            method.invoke(recruit, command);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not set recruit command", exception);
        }
    }

    private static void invokeFeedAnimalPair(GalacticRecruitEntity recruit, ServerLevel level) {
        try {
            Method method = GalacticRecruitEntity.class.getDeclaredMethod("feedAnimalPair", ServerLevel.class);
            method.setAccessible(true);
            method.invoke(recruit, level);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not run animal feeding interaction", exception);
        }
    }

    private static BuildProject fullyProgressProject(
            KingdomSavedData data,
            UUID ownerId,
            KingdomBaseBlueprint blueprint,
            String dimensionId,
            BlockPos origin
    ) {
        BuildProject project = data.startBuildProject(ownerId, blueprint, dimensionId, origin, 0)
                .orElseThrow();
        for (int placement = 0; placement < blueprint.placements().size(); placement++) {
            BuildProject progressed = project.markCompleted(placement);
            if (!data.replaceBuildProject(ownerId, progressed)) {
                throw new IllegalStateException("Could not persist placement " + placement
                        + " for " + blueprint.id());
            }
            project = progressed;
        }
        return project;
    }

    private static final class VehicleEmbodiedTestScenario {
        private final GameTestHelper helper;
        private final ServerPlayer owner;
        private final ServerPlayer intruder;
        private final List<GalacticVehicleEntity> vehicles;
        private final long indexWaitStartedTick;
        private int phase;
        private int driveStartedVehicleTick;
        private long destructionStartedTick;
        private int startingFuel;
        private Vec3 startingPosition = Vec3.ZERO;
        private boolean shotObserved;
        private boolean shotRequested;

        private VehicleEmbodiedTestScenario(
                GameTestHelper helper,
                ServerPlayer owner,
                ServerPlayer intruder,
                List<GalacticVehicleEntity> vehicles
        ) {
            this.helper = helper;
            this.owner = owner;
            this.intruder = intruder;
            this.vehicles = vehicles;
            this.indexWaitStartedTick = helper.getTick();
        }

        private void tick() {
            if (this.phase == 0) {
                this.waitForIndexAndPrepare();
            } else if (this.phase == 1) {
                this.driveAndFire();
            } else if (this.phase == 2) {
                this.verifyDestruction();
            }
        }

        private void waitForIndexAndPrepare() {
            boolean allIndexed = this.vehicles.stream().allMatch(
                    vehicle -> this.helper.getLevel().getEntity(vehicle.getUUID()) == vehicle);
            if (!allIndexed) {
                if (this.helper.getTick() - this.indexWaitStartedTick >= 120L) {
                    this.phase = 3;
                    this.helper.fail("Vehicle chassis never entered the server entity index");
                }
                return;
            }
            for (GalacticVehicleEntity vehicle : this.vehicles) {
                int maximum = vehicle.maxHealthForHud();
                vehicle.damageVehicle(20);
                int damaged = vehicle.health();
                this.intruder.setItemInHand(
                        InteractionHand.MAIN_HAND, new ItemStack(ModItems.DURACRETE.get()));
                InteractionResult denied = vehicle.interact(
                        this.intruder, InteractionHand.MAIN_HAND, this.intruder.position());
                if (denied != InteractionResult.FAIL
                        || vehicle.health() != damaged
                        || this.intruder.getMainHandItem().getCount() != 1) {
                    this.phase = 3;
                    this.helper.fail("Unauthorized player repaired chassis " + vehicle.vehicleId());
                    return;
                }
                this.owner.setItemInHand(
                        InteractionHand.MAIN_HAND, new ItemStack(ModItems.DURACRETE.get()));
                InteractionResult repaired = vehicle.interact(
                        this.owner, InteractionHand.MAIN_HAND, this.owner.position());
                int expectedHealth = Math.min(maximum, damaged + 12);
                if (repaired != InteractionResult.SUCCESS
                        || vehicle.health() != expectedHealth
                        || !this.owner.getMainHandItem().isEmpty()) {
                    this.phase = 3;
                    this.helper.fail("Authorized Duracrete repair was not atomic for "
                            + vehicle.vehicleId() + ": health=" + damaged + "->" + vehicle.health()
                            + "/" + maximum + ", item=" + this.owner.getMainHandItem());
                    return;
                }
            }

            GalacticVehicleEntity driverVehicle = this.vehicles.getFirst();
            this.owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            this.owner.setYRot(0.0F);
            this.owner.setXRot(0.0F);
            InteractionResult boarded = driverVehicle.interact(
                    this.owner, InteractionHand.MAIN_HAND, this.owner.position());
            if (boarded != InteractionResult.SUCCESS || this.owner.getVehicle() != driverVehicle) {
                this.phase = 3;
                this.helper.fail("Authorized owner could not physically board the BARC speeder");
                return;
            }
            this.startingPosition = driverVehicle.position();
            this.startingFuel = driverVehicle.fuel();
            this.driveStartedVehicleTick = driverVehicle.tickCount;
            this.phase = 1;
        }

        private void driveAndFire() {
            GalacticVehicleEntity driverVehicle = this.vehicles.getFirst();
            boolean fire = !this.shotRequested;
            if (!driverVehicle.applyInput(
                    this.owner, UUID.randomUUID(), 1.0F, 0.0F, false, false, fire)) {
                this.phase = 3;
                this.helper.fail("Server rejected fresh authorized BARC driving input");
                return;
            }
            this.shotRequested = true;
            this.shotObserved |= !this.helper.getLevel().getEntitiesOfClass(
                    BlasterBoltEntity.class,
                    driverVehicle.getBoundingBox().inflate(120.0D),
                    bolt -> bolt.getOwner() == this.owner).isEmpty();
            int drivenTicks = driverVehicle.tickCount - this.driveStartedVehicleTick;
            if (drivenTicks < 25 || !this.shotObserved) {
                if (drivenTicks >= 80) {
                    this.phase = 3;
                    this.helper.fail("BARC embodied runtime did not fire within 80 vehicle ticks: fuel="
                            + driverVehicle.fuel() + ", position=" + driverVehicle.position()
                            + ", passenger=" + this.owner.getVehicle());
                }
                return;
            }
            if (driverVehicle.position().distanceToSqr(this.startingPosition) <= 1.0D
                    || driverVehicle.fuel() >= this.startingFuel) {
                this.phase = 3;
                this.helper.fail("BARC input did not produce movement and fuel use: position="
                        + this.startingPosition + "->" + driverVehicle.position()
                        + ", fuel=" + this.startingFuel + "->" + driverVehicle.fuel());
                return;
            }

            int fuelBeforeIntrusion = driverVehicle.fuel();
            this.intruder.setItemInHand(
                    InteractionHand.MAIN_HAND, new ItemStack(ModItems.ENERGY_CELL.get()));
            InteractionResult denied = driverVehicle.interact(
                    this.intruder, InteractionHand.MAIN_HAND, this.intruder.position());
            if (denied != InteractionResult.FAIL
                    || driverVehicle.fuel() != fuelBeforeIntrusion
                    || this.intruder.getMainHandItem().getCount() != 1) {
                this.phase = 3;
                this.helper.fail("Unauthorized player refuelled the occupied BARC speeder");
                return;
            }

            this.owner.setItemInHand(
                    InteractionHand.MAIN_HAND, new ItemStack(ModItems.ENERGY_CELL.get()));
            InteractionResult refuelled = driverVehicle.interact(
                    this.owner, InteractionHand.MAIN_HAND, this.owner.position());
            if (refuelled != InteractionResult.SUCCESS
                    || driverVehicle.fuel() <= fuelBeforeIntrusion
                    || !this.owner.getMainHandItem().isEmpty()) {
                this.phase = 3;
                this.helper.fail("Authorized Energy Cell refuel was not atomic: fuel="
                        + fuelBeforeIntrusion + "->" + driverVehicle.fuel()
                        + ", item=" + this.owner.getMainHandItem());
                return;
            }

            for (GalacticVehicleEntity vehicle : this.vehicles) {
                vehicle.damageVehicle(vehicle.health());
            }
            this.destructionStartedTick = this.helper.getTick();
            this.phase = 2;
        }

        private void verifyDestruction() {
            boolean allRemoved = this.vehicles.stream().allMatch(GalacticVehicleEntity::isRemoved);
            if (allRemoved && this.owner.getVehicle() == null) {
                this.phase = 3;
                this.helper.succeed();
                return;
            }
            if (this.helper.getTick() - this.destructionStartedTick >= 20L) {
                this.phase = 3;
                this.helper.fail("Destroyed vehicle did not discard and release its passenger: removed="
                        + this.vehicles.stream().map(GalacticVehicleEntity::isRemoved).toList()
                        + ", passengerVehicle=" + this.owner.getVehicle());
            }
        }
    }

    private static final class ChapterThreeCampaignTestScenario {
        private static final int EXPECTED_CAMPAIGN_REWARDS = 320;
        private final GameTestHelper helper;
        private final SmartBrainTestArea area;
        private final ServerPlayer owner;
        private final BlockPos hallPos;
        private final CommandCenterBlockEntity hall;
        private final KingdomRecord kingdom;
        private final GalacticRecruitEntity merchant;
        private final GalacticRecruitEntity escort;
        private final BlockPos deploymentFloor;
        private final BlockPos beacon;
        private final galacticwars.clonewars.data.LaunchContentDefinitions.ConquestRegionDefinition region;
        private final long indexWaitStartedTick;
        private int phase;
        private int captureTicks;
        private GalacticVehicleEntity vehicle;

        private ChapterThreeCampaignTestScenario(
                GameTestHelper helper,
                SmartBrainTestArea area,
                ServerPlayer owner,
                BlockPos hallPos,
                CommandCenterBlockEntity hall,
                KingdomRecord kingdom,
                GalacticRecruitEntity merchant,
                GalacticRecruitEntity escort,
                BlockPos deploymentFloor,
                BlockPos beacon,
                galacticwars.clonewars.data.LaunchContentDefinitions.ConquestRegionDefinition region
        ) {
            this.helper = helper;
            this.area = area;
            this.owner = owner;
            this.hallPos = hallPos;
            this.hall = hall;
            this.kingdom = kingdom;
            this.merchant = merchant;
            this.escort = escort;
            this.deploymentFloor = deploymentFloor;
            this.beacon = beacon;
            this.region = region;
            this.indexWaitStartedTick = helper.getTick();
        }

        private void tick() {
            if (this.phase == 0) {
                this.waitForEntitiesAndTrade();
            } else if (this.phase == 1) {
                this.captureRegion();
            }
        }

        private void waitForEntitiesAndTrade() {
            if (this.vehicle == null) {
                this.vehicle = this.helper.getLevel().getEntitiesOfClass(
                        GalacticVehicleEntity.class, new AABB(this.deploymentFloor).inflate(4.0D),
                        candidate -> candidate.ownerId().filter(this.owner.getUUID()::equals).isPresent())
                        .stream().findFirst().orElse(null);
            }
            boolean indexed = this.helper.getLevel().getEntity(this.merchant.getUUID()) == this.merchant
                    && this.helper.getLevel().getEntity(this.escort.getUUID()) == this.escort
                    && this.vehicle != null
                    && this.helper.getLevel().getEntity(this.vehicle.getUUID()) == this.vehicle;
            if (!indexed) {
                if (this.helper.getTick() - this.indexWaitStartedTick >= 120L) {
                    this.phase = 2;
                    this.helper.fail("Chapter-three merchant, escort, or vehicle never entered the entity index");
                }
                return;
            }
            this.owner.setPos(
                    this.merchant.getX(), this.merchant.getY(), this.merchant.getZ() + 1.0D);
            this.owner.getInventory().add(new ItemStack(ModItems.CREDIT_CHIP.get(), 12));
            MerchantTradeMenu tradeMenu = (MerchantTradeMenu) new MerchantTradeMenuProvider(this.merchant)
                    .createMenu(21, this.owner.getInventory(), this.owner);
            int offer = tradeMenu.tradeIds().indexOf("republic_quartermaster");
            UUID tradeId = UUID.randomUUID();
            boolean traded = offer >= 0 && tradeMenu.handleReplayAction(this.owner, tradeId, offer);
            boolean replayRejected = offer >= 0
                    && !tradeMenu.handleReplayAction(this.owner, tradeId, offer);
            ProgressionState tradedState = ProgressionSavedData.get(this.helper.getLevel())
                    .state(this.owner.getUUID());
            if (!traded || !replayRejected || countPlayerItem(this.owner, ModItems.ENERGY_CELL.get()) != 8
                    || CreditTransactionService.playerBalance(this.owner) != 0
                    || tradedState.total(ProgressionEventType.TRADE_COMPLETED) != 1) {
                this.phase = 2;
                this.helper.fail("Physical merchant trade did not commit exactly once in Chapter 3: offer="
                        + offer + ", traded=" + traded + ", replay=" + replayRejected
                        + ", energy=" + countPlayerItem(this.owner, ModItems.ENERGY_CELL.get())
                        + ", credits=" + CreditTransactionService.playerBalance(this.owner)
                        + ", total=" + tradedState.total(ProgressionEventType.TRADE_COMPLETED)
                        + ", merchant=" + this.merchant.isMerchant() + "/"
                        + this.merchant.factionIdForGameplay() + "/"
                        + this.merchant.factionRelationTo(this.owner)
                        + ", valid=" + tradeMenu.stillValid(this.owner)
                        + ", unlocks=" + tradedState.unlocks());
                return;
            }
            this.owner.setPos(
                    this.beacon.getX() + 0.5D, this.beacon.getY(), this.beacon.getZ() + 0.5D);
            this.escort.setPos(
                    this.beacon.getX() + 1.5D, this.beacon.getY(), this.beacon.getZ() + 0.5D);
            this.phase = 1;
        }

        private void captureRegion() {
            this.captureTicks++;
            ConquestCaptureService.CaptureResult result = ConquestCaptureService.tick(
                    this.helper.getLevel(), this.region, this.beacon);
            if (!result.accepted()) {
                this.phase = 2;
                this.helper.fail("Embodied conquest transaction was rejected: " + result);
                return;
            }
            ConquestControlState control = ConquestSavedData.get(this.helper.getLevel())
                    .state(this.region.id()).orElseThrow();
            if (!control.controllingFaction().equals("galacticwars:republic")) {
                if (this.captureTicks >= 80) {
                    this.phase = 2;
                    this.helper.fail("Owned military escort did not capture the physical beacon: result="
                            + result + ", control=" + control + ", escortOwner="
                            + this.escort.getOwnerReference() + ", escortDuty="
                            + this.escort.getServiceBranch());
                }
                return;
            }
            this.verifyVictory(control);
        }

        private void verifyVictory(ConquestControlState control) {
            ProgressionSavedData progression = ProgressionSavedData.get(this.helper.getLevel());
            ProgressionState victory = progression.state(this.owner.getUUID());
            boolean chapterComplete = victory.hasSubject(
                    ProgressionEventType.QUEST_ADVANCED, "republic_chapter_3");
            boolean campaignComplete = victory.hasSubject(
                    ProgressionEventType.CAMPAIGN_COMPLETED, "republic_campaign");
            boolean victoryUnlocks = victory.unlocks().contains("campaign_victory")
                    && victory.unlocks().contains("veteran_operations");
            ConquestCaptureService.CaptureResult replay = ConquestCaptureService.tick(
                    this.helper.getLevel(), this.region, this.beacon);
            if (!chapterComplete || !campaignComplete || !victoryUnlocks
                    || control.revision() != 1L
                    || !control.controllingKingdom().equals(this.kingdom.id().toString())
                    || victory.total(ProgressionEventType.VEHICLE_ACQUIRED) != 1
                    || victory.total(ProgressionEventType.TRADE_COMPLETED) != 1
                    || victory.total(ProgressionEventType.REGION_CAPTURED) != 1
                    || victory.pendingCreditRewards() != EXPECTED_CAMPAIGN_REWARDS
                    || !replay.accepted() || replay.captured()
                    || progression.state(this.owner.getUUID())
                    .total(ProgressionEventType.REGION_CAPTURED) != 1) {
                this.phase = 2;
                this.helper.fail("Chapter 3 did not atomically become a persistent campaign victory: chapter="
                        + chapterComplete + ", campaign=" + campaignComplete
                        + ", unlocks=" + victory.unlocks() + ", control=" + control
                        + ", totals=" + victory.eventTotals() + ", pending="
                        + victory.pendingCreditRewards() + ", replay=" + replay);
                return;
            }

            this.owner.setPos(
                    this.merchant.getX(), this.merchant.getY(), this.merchant.getZ() + 1.0D);
            this.owner.getInventory().add(new ItemStack(ModItems.CREDIT_CHIP.get(), 12));
            MerchantTradeMenu veteranTrade = (MerchantTradeMenu)
                    new MerchantTradeMenuProvider(this.merchant)
                            .createMenu(22, this.owner.getInventory(), this.owner);
            int offer = veteranTrade.tradeIds().indexOf("republic_quartermaster");
            boolean repeatedTrade = offer >= 0 && veteranTrade.handleReplayAction(
                    this.owner, UUID.randomUUID(), offer);
            ProgressionState repeated = progression.state(this.owner.getUUID());
            if (!repeatedTrade || repeated.total(ProgressionEventType.TRADE_COMPLETED) != 2
                    || repeated.pendingCreditRewards() != EXPECTED_CAMPAIGN_REWARDS
                    || countPlayerItem(this.owner, ModItems.ENERGY_CELL.get()) != 16) {
                this.phase = 2;
                this.helper.fail("Veteran trade operation was not repeatable without duplicating victory rewards");
                return;
            }

            this.owner.setPos(
                    this.hallPos.getX() + 0.5D, this.hallPos.getY(), this.hallPos.getZ() + 0.5D);
            CommandCenterOperationsMenu operations = (CommandCenterOperationsMenu)
                    new CommandCenterOperationsMenuProvider(this.hallPos)
                            .createMenu(23, this.owner.getInventory(), this.owner);
            CommandCenterDashboardState dashboard = operations.dashboardState();
            boolean claimed = operations.handleReplayAction(
                    this.owner, UUID.randomUUID(), CommandCenterOperationsMenu.CLAIM_REWARDS);
            int paidBalance = CreditTransactionService.playerBalance(this.owner);
            boolean duplicateClaimRejected = !operations.handleReplayAction(
                    this.owner, UUID.randomUUID(), CommandCenterOperationsMenu.CLAIM_REWARDS);
            Tag encoded = ProgressionSavedData.CODEC.encodeStart(
                    NbtOps.INSTANCE, progression).getOrThrow();
            ProgressionSavedData restored = ProgressionSavedData.CODEC.parse(
                    NbtOps.INSTANCE, encoded).getOrThrow();
            ProgressionState restoredVictory = restored.state(this.owner.getUUID());
            if (!dashboard.campaignVictory()
                    || dashboard.activeQuest().isPresent()
                    || dashboard.veteranVehicleDeployments() != 1
                    || dashboard.veteranTrades() != 2
                    || dashboard.veteranRegionCaptures() != 1
                    || !claimed || !duplicateClaimRejected
                    || paidBalance != EXPECTED_CAMPAIGN_REWARDS
                    || progression.pendingCreditRewards(this.owner.getUUID()) != 0
                    || !restoredVictory.hasSubject(
                    ProgressionEventType.CAMPAIGN_COMPLETED, "republic_campaign")
                    || !restoredVictory.unlocks().contains("veteran_operations")
                    || restoredVictory.total(ProgressionEventType.TRADE_COMPLETED) != 2) {
                this.phase = 2;
                this.helper.fail("Victory dashboard, reward claim, or persistence contract failed: dashboard="
                        + dashboard.campaignVictory() + "/" + dashboard.activeQuest()
                        + "/" + dashboard.veteranVehicleDeployments() + "/"
                        + dashboard.veteranTrades() + "/" + dashboard.veteranRegionCaptures()
                        + ", claim=" + claimed + "/" + duplicateClaimRejected
                        + ", paid=" + paidBalance + ", pending="
                        + progression.pendingCreditRewards(this.owner.getUUID())
                        + ", restored=" + restoredVictory);
                return;
            }
            this.phase = 2;
            this.helper.succeed();
        }
    }

    private static int countPlayerItem(ServerPlayer player, net.minecraft.world.item.Item item) {
        return player.getInventory().getNonEquipmentItems().stream()
                .filter(stack -> stack.is(item))
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    private static final class NaturalCivilianTestScenario {
        private final GameTestHelper helper;
        private final SmartBrainTestArea area;
        private boolean started;
        private boolean shelterVerified;
        private boolean workerPrepared;
        private boolean complete;
        private long startTick;
        private long workerSpawnTick;
        private long workStartTick;
        private BlockPos shelterHome;
        private BlockPos storage;
        private GalacticRecruitEntity sheltering;
        private GalacticRecruitEntity threat;
        private GalacticRecruitEntity worker;
        private double initialShelterDistance;

        private NaturalCivilianTestScenario(GameTestHelper helper, SmartBrainTestArea area) {
            this.helper = helper;
            this.area = area;
        }

        private void tick() {
            if (this.complete) {
                return;
            }
            if (!this.started) {
                BlockPos spawnPosition = this.area.at(1, 1, 1);
                if (!this.helper.getLevel().areEntitiesActuallyLoadedAndTicking(
                        new ChunkPos(spawnPosition.getX() >> 4, spawnPosition.getZ() >> 4))) {
                    if (this.helper.getTick() >= 100L) {
                        this.complete = true;
                        this.helper.fail("Natural civilian GameTest chunk never reached entity-ticking status");
                    }
                    return;
                }
                this.start();
            }
            if (!this.shelterVerified) {
                if (this.sheltering.distanceToSqr(Vec3.atCenterOf(this.shelterHome))
                        < this.initialShelterDistance - 4.0D) {
                    this.startWorkerScenario();
                } else if (this.helper.getTick() - this.startTick >= 120L) {
                    this.complete = true;
                    this.helper.fail("Natural civilian shelter behaviour did not retreat from a hostile soldier: position="
                            + this.sheltering.position() + ", home=" + this.shelterHome
                            + ", orderedSit=" + this.sheltering.isOrderedToSit()
                            + ", tickCount=" + this.sheltering.tickCount
                            + ", branch=" + this.sheltering.getServiceBranch()
                            + ", outpost=" + this.sheltering.getFactionOutpostId()
                            + ", group=" + this.sheltering.getArmyGroupId()
                            + ", threatAlive=" + this.threat.isAlive()
                            + ", threatDistance=" + this.sheltering.distanceToSqr(this.threat)
                            + ", hostile=" + this.sheltering.isHostileFactionRecruit(this.threat)
                            + ", walkMemory=" + BrainUtil.hasMemory(this.sheltering,
                            net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET)
                            + ", navigationDone=" + this.sheltering.getNavigation().isDone()
                            + ", pathMemory=" + BrainUtil.hasMemory(this.sheltering,
                            net.minecraft.world.entity.ai.memory.MemoryModuleType.PATH)
                            + ", running=" + this.sheltering.getBrain().getRunningBehaviors()
                            + ", path=" + pathState(this.sheltering, this.shelterHome));
                }
                return;
            }
            if (!this.workerPrepared && this.helper.getTick() - this.workerSpawnTick >= 2L) {
                this.prepareWorker();
            }
            if (this.workerPrepared && this.helper.getTick() - this.workStartTick >= 30L) {
                this.complete = true;
                verifyNaturalCivilianWork(
                        this.helper, this.sheltering, this.worker, this.storage);
            }
        }

        private void start() {
            this.started = true;
            this.startTick = this.helper.getTick();
            this.shelterHome = this.area.at(4, 1, 4);
            this.sheltering = spawnRecruitAt(
                    this.helper, ModEntityTypes.REPUBLIC_CIVILIAN.get(), this.area.at(1, 1, 1));
            this.sheltering.setInvulnerable(true);
            this.sheltering.initializeNaturalFactionNpc(
                    UUID.randomUUID(), NpcServiceBranch.CIVILIAN, this.shelterHome, 32);
            this.initialShelterDistance = this.sheltering.distanceToSqr(
                    Vec3.atCenterOf(this.shelterHome));
            this.threat = spawnRecruitAt(
                    this.helper, ModEntityTypes.B1_BATTLE_DROID.get(), this.area.at(1, 1, 2));
            this.threat.setInvulnerable(true);
            this.threat.setNoAi(true);
            this.threat.initializeNaturalFactionNpc(
                    UUID.randomUUID(), NpcServiceBranch.MILITARY);
            if (!this.sheltering.isHostileFactionRecruit(this.threat)) {
                this.complete = true;
                this.helper.fail("Natural civilian test threat is not hostile: relation="
                        + this.sheltering.factionRelationTo(this.threat)
                        + ", civilianFaction=" + this.sheltering.getRecruitFactionId()
                        + ", threatFaction=" + this.threat.getRecruitFactionId()
                        + ", threatDuty=" + this.threat.getRecruitDuty());
                return;
            }
            BlockPos workHome = this.area.at(3, 1, 0);
            this.storage = workHome.offset(1, 0, 0);
            this.helper.getLevel().setBlockAndUpdate(this.storage, Blocks.CHEST.defaultBlockState());
        }

        private void startWorkerScenario() {
            this.shelterVerified = true;
            this.threat.discard();
            this.helper.setTime(6000L);
            BlockPos workHome = this.area.at(3, 1, 0);
            this.worker = spawnRecruitAt(
                    this.helper, ModEntityTypes.HUTT_CIVILIAN.get(), this.area.at(1, 1, 1));
            this.worker.setInvulnerable(true);
            this.worker.initializeNaturalFactionNpc(
                    UUID.randomUUID(), NpcServiceBranch.CIVILIAN, workHome, 32);
            this.workerSpawnTick = this.helper.getTick();
        }

        private void prepareWorker() {
            this.workerPrepared = true;
            this.worker.setWorkerProfession(WorkerProfession.FARMER);
            setRecruitField(this.worker, "nextNaturalProductionGameTime", 0L);
            BlockPos workstation = this.worker.naturalWorkstationPosition();
            this.worker.setPos(
                    workstation.getX() + 0.5D,
                    workstation.getY(),
                    workstation.getZ() + 0.5D);
            this.worker.getNavigation().stop();
            this.workStartTick = this.helper.getTick();
        }
    }

    private static void invokeWorkerAuthorityReconciliation(
            GalacticRecruitEntity recruit,
            net.minecraft.server.level.ServerLevel level
    ) {
        try {
            Method method = GalacticRecruitEntity.class.getDeclaredMethod(
                    "reconcileWorkerAuthority", net.minecraft.server.level.ServerLevel.class);
            method.setAccessible(true);
            method.invoke(recruit, level);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not reconcile recruit worker authority", exception);
        }
    }

    private record SpawnEggCase(
            RecruitSpawnEggItem item,
            EntityType<GalacticRecruitEntity> type,
            boolean civilian
    ) {
        private SpawnEggCase(
                RecruitSpawnEggItem item,
                EntityType<GalacticRecruitEntity> type
        ) {
            this(item, type, false);
        }
    }

    private record SmartBrainTestArea(BlockPos base, ServerPlayer player) {
        private BlockPos at(int x, int y, int z) {
            return base.offset(x, y, z);
        }
    }

    private static BlockPos isolatedCapital(GameTestHelper helper, int lane) {
        return helper.absolutePos(new BlockPos(1, 1, 1)).offset(0, 0, lane * 10_000);
    }

    private static CommandCenterBlockEntity placeCommandCenter(GameTestHelper helper, BlockPos position) {
        helper.getLevel().getChunkAt(position);
        if (!helper.getLevel().setBlock(position, ModBlocks.COMMAND_CENTER.get().defaultBlockState(), 3)
                || !(helper.getLevel().getBlockEntity(position) instanceof CommandCenterBlockEntity hall)) {
            throw new IllegalStateException("Could not place isolated GameTest Command Center at " + position);
        }
        return hall;
    }

    @SuppressWarnings("removal")
    private static ServerPlayer makeConnectedMockPlayer(GameTestHelper helper, GameType gameType) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        gameType.updatePlayerAbilities(player.getAbilities());
        return player;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(GalacticWars.MODID, path);
    }
}
