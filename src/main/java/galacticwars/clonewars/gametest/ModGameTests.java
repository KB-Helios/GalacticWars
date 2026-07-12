package galacticwars.clonewars.gametest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.army.ArmyFormation;
import galacticwars.clonewars.army.ArmyGroupLifecycleState;
import galacticwars.clonewars.army.ArmyLocation;
import galacticwars.clonewars.army.ArmyTravelService;
import galacticwars.clonewars.combat.BlasterCombatEvents;
import galacticwars.clonewars.combat.FactionRangedWeaponService;
import galacticwars.clonewars.combat.BlasterHeatPolicy;
import galacticwars.clonewars.combat.BlasterItem;
import galacticwars.clonewars.economy.CreditTransactionService;
import galacticwars.clonewars.economy.PhysicalTradeService;
import galacticwars.clonewars.data.GameplayDataManager;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.entity.RecruitSpawnEggItem;
import galacticwars.clonewars.entity.RecruitLifecycleService;
import galacticwars.clonewars.entity.ai.RecruitRangedCombatGoal;
import galacticwars.clonewars.faction.FactionAlignmentSavedData;
import galacticwars.clonewars.faction.FactionId;
import galacticwars.clonewars.kingdom.BuildProject;
import galacticwars.clonewars.kingdom.KingdomRecord;
import galacticwars.clonewars.kingdom.KingdomClaim;
import galacticwars.clonewars.kingdom.KingdomActionId;
import galacticwars.clonewars.kingdom.KingdomGameplayAction;
import galacticwars.clonewars.kingdom.KingdomGameplayResult;
import galacticwars.clonewars.kingdom.KingdomGameplayRuntimeService;
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
import galacticwars.clonewars.menu.RecruitCommandMenu;
import galacticwars.clonewars.menu.CommandCenterNavigationMenu;
import galacticwars.clonewars.menu.FactionSelectionMenu;
import galacticwars.clonewars.progression.ProgressionEvent;
import galacticwars.clonewars.progression.ProgressionDecision;
import galacticwars.clonewars.progression.ProgressionEventType;
import galacticwars.clonewars.progression.ProgressionSavedData;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
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

public final class ModGameTests {
    private static final Identifier ENVIRONMENT = id("gameplay");
    private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");
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
        for (Identifier testId : TESTS.keySet()) {
            TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                    environment,
                    EMPTY_STRUCTURE,
                    100,
                    0,
                    true);
            event.registerTest(testId, new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, testId),
                    data));
        }
    }

    private static Map<Identifier, Consumer<GameTestHelper>> createTests() {
        LinkedHashMap<Identifier, Consumer<GameTestHelper>> tests = new LinkedHashMap<>();
        tests.put(id("command_center_authority"), ModGameTests::kingdomHallAuthority);
        tests.put(id("kingdom_governance_persistence"), ModGameTests::kingdomGovernancePersistence);
        tests.put(id("kingdom_multiplayer_runtime"), ModGameTests::kingdomMultiplayerRuntime);
        tests.put(id("overworld_faction_outpost_runtime"), ModGameTests::overworldFactionOutpostRuntime);
        tests.put(id("physical_trade_transaction"), ModGameTests::physicalTradeTransaction);
        tests.put(id("faction_selection_transaction"), ModGameTests::factionSelectionTransaction);
        tests.put(id("progression_runtime_adapter"), ModGameTests::progressionRuntimeAdapter);
        tests.put(id("recruit_entity_contract"), ModGameTests::recruitEntityContract);
        tests.put(id("worker_tags_and_loot"), ModGameTests::workerTagsAndLoot);
        tests.put(id("recruit_contract_lifecycle"), ModGameTests::recruitContractLifecycle);
        tests.put(id("worker_resource_conservation"), ModGameTests::workerResourceConservation);
        tests.put(id("enabled_worker_loops"), ModGameTests::enabledWorkerLoops);
        tests.put(id("workforce_saved_data_authority"), ModGameTests::workforceSavedDataAuthority);
        tests.put(id("recruit_spawn_eggs"), ModGameTests::recruitSpawnEggs);
        tests.put(id("blaster_friendly_fire"), ModGameTests::blasterFriendlyFire);
        tests.put(id("recruit_blaster_projectile"), ModGameTests::recruitBlasterProjectile);
        tests.put(id("player_blaster_heat"), ModGameTests::playerBlasterHeat);
        tests.put(id("ungrouped_recruit_ranged_goal"), ModGameTests::ungroupedRecruitRangedGoal);
        tests.put(id("planet_travel_failure_atomicity"), ModGameTests::planetTravelFailureAtomicity);
        tests.put(id("planet_arrival_runtime"), ModGameTests::planetArrivalRuntime);
        tests.put(id("army_planet_transfer_transaction"), ModGameTests::armyPlanetTransferTransaction);
        return Map.copyOf(tests);
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
        BlockPos relativeHall = new BlockPos(1, 1, 1);
        helper.setBlock(relativeHall, ModBlocks.COMMAND_CENTER.get());
        CommandCenterBlockEntity hall = helper.getBlockEntity(relativeHall, CommandCenterBlockEntity.class);
        ServerPlayer owner = makeConnectedMockPlayer(helper, GameType.CREATIVE);
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
        helper.succeed();
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
        BlockPos relativeHall = new BlockPos(1, 1, 1);
        helper.setBlock(relativeHall, ModBlocks.COMMAND_CENTER.get());
        CommandCenterBlockEntity hall = helper.getBlockEntity(relativeHall, CommandCenterBlockEntity.class);
        ServerPlayer owner = makeConnectedMockPlayer(helper, GameType.CREATIVE);
        hall.claim(owner);
        hall.setFaction("galacticwars:republic");
        BlockPos hallPos = hall.getBlockPos();
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

        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.HYPERSPACE_NAVIGATOR.get()));
        InteractionResult navigatorResult = ModItems.HYPERSPACE_NAVIGATOR.get().use(
                helper.getLevel(), owner, InteractionHand.MAIN_HAND);
        if (navigatorResult != InteractionResult.SUCCESS
                || !(owner.containerMenu instanceof CommandCenterNavigationMenu)) {
            helper.fail("Hyperspace Navigator did not open the authoritative navigation menu");
            return;
        }
        owner.closeContainer();

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
                ModEntityTypes.B1_BATTLE_DROID.get(), new BlockPos(6, 1, 2));
        shooter.tame(owner);
        ItemStack weapon = new ItemStack(ModItems.DC15_BLASTER.get());
        shooter.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        ModItems.DC15_BLASTER.get().fireAt(helper.getLevel(), shooter, target, weapon);

        List<Arrow> bolts = helper.getLevel().getEntitiesOfClass(
                Arrow.class, shooter.getBoundingBox().inflate(8.0D), arrow -> arrow.getOwner() == shooter);
        if (bolts.size() != 1 || !bolts.getFirst().getWeaponItem().is(ModItems.DC15_BLASTER.get())) {
            helper.fail("Recruit blaster did not spawn one owned, weapon-tagged bolt");
            return;
        }

        GalacticRecruitEntity archer = helper.spawn(
                ModEntityTypes.NIGHTSISTER_ARCHER.get(), new BlockPos(2, 1, 4));
        GalacticRecruitEntity bowTarget = helper.spawn(
                ModEntityTypes.CLONE_TROOPER.get(), new BlockPos(6, 1, 4));
        archer.tame(owner);
        ItemStack bow = new ItemStack(ModItems.NIGHTSISTER_BOW.get());
        archer.setItemInHand(InteractionHand.MAIN_HAND, bow);
        FactionRangedWeaponService.fireNightsisterBow(helper.getLevel(), archer, bowTarget, bow);
        List<Arrow> arrows = helper.getLevel().getEntitiesOfClass(
                Arrow.class, archer.getBoundingBox().inflate(8.0D), arrow -> arrow.getOwner() == archer);
        if (arrows.size() != 1 || !arrows.getFirst().getWeaponItem().is(ModItems.NIGHTSISTER_BOW.get())) {
            helper.fail("Nightsister Archer did not spawn one owned, bow-tagged ranged projectile");
            return;
        }
        helper.succeed();
    }

    private static void armyPlanetTransferTransaction(GameTestHelper helper) {
        BlockPos relativeHall = new BlockPos(1, 1, 1);
        helper.setBlock(relativeHall, ModBlocks.COMMAND_CENTER.get());
        CommandCenterBlockEntity hall = helper.getBlockEntity(relativeHall, CommandCenterBlockEntity.class);
        ServerPlayer owner = makeConnectedMockPlayer(helper, GameType.CREATIVE);
        hall.claim(owner);
        hall.setFaction("galacticwars:republic");
        BlockPos hallPos = hall.getBlockPos();
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

    private static void ungroupedRecruitRangedGoal(GameTestHelper helper) {
        ServerPlayer owner = makeConnectedMockPlayer(helper, GameType.CREATIVE);
        GalacticRecruitEntity shooter = helper.spawn(
                ModEntityTypes.CLONE_TROOPER.get(), new BlockPos(2, 1, 2));
        GalacticRecruitEntity target = helper.spawn(
                ModEntityTypes.B1_BATTLE_DROID.get(), new BlockPos(6, 1, 2));
        shooter.tame(owner);
        shooter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DC15_BLASTER.get()));
        shooter.setTarget(target);

        RecruitRangedCombatGoal goal = new RecruitRangedCombatGoal(shooter);
        if (!goal.canUse()) {
            helper.fail("Ungrouped ranged recruit goal rejected a valid local attack target");
            return;
        }
        goal.tick();
        List<Arrow> bolts = helper.getLevel().getEntitiesOfClass(
                Arrow.class, shooter.getBoundingBox().inflate(8.0D), arrow -> arrow.getOwner() == shooter);
        if (bolts.size() != 1 || !bolts.getFirst().getWeaponItem().is(ModItems.DC15_BLASTER.get())) {
            helper.fail("Ungrouped ranged recruit goal did not fire one weapon-tagged bolt");
            return;
        }
        helper.succeed();
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
                result = testCase.item().useOn(new UseOnContext(
                        player,
                        InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(clicked), Direction.UP, clicked, false)));
            } catch (Throwable exception) {
                GalacticWars.LOGGER.error(
                        "Spawn egg threw while creating {}", testCase.type(), exception);
                helper.fail("Spawn egg threw while creating " + testCase.type() + ": " + exception);
                return;
            }
            if (result != InteractionResult.SUCCESS) {
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

        BlockPos relativeSpawner = new BlockPos(1, 1, 11);
        helper.setBlock(relativeSpawner, Blocks.SPAWNER);
        BlockPos spawnerPos = helper.absolutePos(relativeSpawner);
        SpawnerBlockEntity spawner = helper.getBlockEntity(
                relativeSpawner, SpawnerBlockEntity.class);
        ItemStack spawnerEgg = new ItemStack(ModItems.CLONE_TROOPER_SPAWN_EGG.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, spawnerEgg);
        InteractionResult spawnerResult = ModItems.CLONE_TROOPER_SPAWN_EGG.get().useOn(
                new UseOnContext(
                        player,
                        InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(spawnerPos), Direction.UP, spawnerPos, false)));
        var spawnerDisplay = spawner.getSpawner().getOrCreateDisplayEntity(
                helper.getLevel(), spawnerPos);
        if (spawnerResult != InteractionResult.SUCCESS
                || spawnerDisplay == null
                || spawnerDisplay.getType() != ModEntityTypes.CLONE_TROOPER.get()) {
            helper.fail("Recruit spawn egg did not preserve vanilla spawner configuration behavior");
        }
        helper.succeed();
    }

    private static void kingdomHallAuthority(GameTestHelper helper) {
        BlockPos hallPos = new BlockPos(1, 1, 1);
        helper.setBlock(hallPos, ModBlocks.COMMAND_CENTER.get());
        CommandCenterBlockEntity hall = helper.getBlockEntity(hallPos, CommandCenterBlockEntity.class);
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
                helper.absolutePos(hallPos));
        if (!kingdom.ownerId().equals(owner.getUUID())
                || data.kingdomForOwner(owner.getUUID()).isEmpty()) {
            helper.fail("Kingdom state was not stored authoritatively in overworld SavedData");
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
        BlockPos absoluteHall = helper.absolutePos(hallPos);
        BlockPos relocatedHall = absoluteHall.offset(16, 0, 0);
        if (data.activateHall(owner.getUUID(), hall.factionId(),
                        helper.getLevel().dimension().identifier().toString(), relocatedHall).isPresent()) {
            helper.fail("A second active Command Center was accepted");
        }
        ModBlocks.COMMAND_CENTER.get().playerWillDestroy(
                helper.getLevel(), absoluteHall, helper.getLevel().getBlockState(absoluteHall), intruder);
        if (!data.isHallActive(owner.getUUID()) || hall.treasuryCredits() != 12) {
            helper.fail("Intruder Hall removal bypassed owner authority");
        }
        ModBlocks.COMMAND_CENTER.get().playerWillDestroy(
                helper.getLevel(), absoluteHall, helper.getLevel().getBlockState(absoluteHall), owner);
        int removalRefund = helper.getLevel().getEntitiesOfClass(
                        ItemEntity.class,
                        new net.minecraft.world.phys.AABB(absoluteHall).inflate(3.0))
                .stream()
                .filter(item -> item.getItem().is(galacticwars.clonewars.registry.ModItems.CREDIT_CHIP.get()))
                .mapToInt(item -> item.getItem().getCount())
                .sum();
        if (data.isHallActive(owner.getUUID()) || removalRefund != 12 || !hall.isEmpty()) {
            helper.fail("Owner Hall removal did not conserve inventory and deactivate authority");
        }
        KingdomRecord relocated = data.activateHall(owner.getUUID(), hall.factionId(),
                        helper.getLevel().dimension().identifier().toString(), relocatedHall)
                .orElseThrow();
        if (!relocated.id().equals(kingdom.id())
                || relocated.settlement().hallX() != relocatedHall.getX()
                || relocated.settlement().hallY() != relocatedHall.getY()
                || relocated.settlement().hallZ() != relocatedHall.getZ()) {
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
        SettlementRecord enemyOutpost = SettlementRecord.create(
                helper.getLevel().dimension().identifier().toString(),
                enemyCapital.getX() + 256, enemyCapital.getY(), enemyCapital.getZ());
        KingdomClaim outpostClaim = data.addOutpost(enemyOwner.getUUID(), enemyOutpost).orElseThrow();
        if (!data.setRelation(owner.getUUID(), enemy.id(), KingdomRelation.ENEMY,
                helper.getLevel().getGameTime(), 0L)) {
            helper.fail("Enemy relation could not be declared");
        }
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
        UUID firstCommander = UUID.randomUUID();
        UUID secondCommander = UUID.randomUUID();
        UUID soldier = UUID.randomUUID();
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
        BlockPos hallPos = helper.absolutePos(new BlockPos(1, 1, 1));
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
        BlockPos hallPos = new BlockPos(1, 1, 1);
        helper.setBlock(hallPos, ModBlocks.COMMAND_CENTER.get());
        CommandCenterBlockEntity hall = helper.getBlockEntity(hallPos, CommandCenterBlockEntity.class);
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
                helper.absolutePos(hallPos)).orElseThrow();
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
        BlockPos intruderHallPos = new BlockPos(1, 1, 3);
        helper.setBlock(intruderHallPos, ModBlocks.COMMAND_CENTER.get());
        CommandCenterBlockEntity intruderHall = helper.getBlockEntity(
                intruderHallPos, CommandCenterBlockEntity.class);
        intruderHall.claim(intruder);
        data.activateHall(
                intruder.getUUID(),
                intruderHall.factionId(),
                helper.getLevel().dimension().identifier().toString(),
                helper.absolutePos(intruderHallPos)).orElseThrow();
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
                helper.absolutePos(hallPos).offset(8, 0, 0));
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

    private static void workerResourceConservation(GameTestHelper helper) {
        BlockPos hallPos = new BlockPos(1, 1, 1);
        BlockPos cropPos = new BlockPos(4, 1, 1);
        BlockPos chestPos = new BlockPos(5, 1, 1);
        helper.setBlock(hallPos, ModBlocks.COMMAND_CENTER.get());
        helper.setBlock(cropPos.below(), Blocks.FARMLAND);
        helper.setBlock(cropPos, Blocks.WHEAT.defaultBlockState().setValue(BlockStateProperties.AGE_7, 7));
        helper.setBlock(chestPos, Blocks.CHEST);
        CommandCenterBlockEntity hall = helper.getBlockEntity(hallPos, CommandCenterBlockEntity.class);
        Container chest = helper.getBlockEntity(chestPos, ChestBlockEntity.class);
        ServerPlayer owner = makeConnectedMockPlayer(helper, GameType.SURVIVAL);
        GalacticRecruitEntity recruit = helper.spawn(
                ModEntityTypes.CLONE_TROOPER.get(), new BlockPos(3, 1, 1));
        owner.setPos(recruit.getX(), recruit.getY(), recruit.getZ());
        hall.claim(owner);
        KingdomSavedData data = KingdomSavedData.get(helper.getLevel());
        data.activateHall(
                owner.getUUID(),
                hall.factionId(),
                helper.getLevel().dimension().identifier().toString(),
                helper.absolutePos(hallPos)).orElseThrow();
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

        BlockPos absoluteCrop = helper.absolutePos(cropPos);
        BlockPos absoluteChest = helper.absolutePos(chestPos);
        setRecruitField(recruit, "workTarget", absoluteCrop);
        setRecruitField(recruit, "storageTarget", absoluteChest);
        setWorkerInventory(recruit, new ItemStack(Items.WHEAT_SEEDS));
        invokeRecruitCommand(recruit, RecruitmentAction.WORK_AT_SITE);
        recruit.setPos(absoluteCrop.getX() + 0.5, absoluteCrop.getY(), absoluteCrop.getZ() + 0.5);
        setRecruitField(recruit, "workerPhase", WorkerPhase.INTERACT);
        setRecruitField(recruit, "workerReason", "navigate_work_target");
        setRecruitField(recruit, "activeWorkTarget", absoluteCrop);
        int toolDamageBefore = recruit.getMainHandItem().getDamageValue();
        recruit.tickWorkerController();
        if (helper.getBlockState(cropPos).getValue(BlockStateProperties.AGE_7) != 0
                || recruit.getMainHandItem().getDamageValue() != toolDamageBefore + 1
                || workerInventoryCount(recruit) <= 0) {
            helper.fail("Farmer harvesting did not replant, wear its tool, and conserve drops");
        }

        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            chest.setItem(slot, new ItemStack(Items.STONE, 64));
        }
        int carriedBeforeFailedDeposit = workerInventoryCount(recruit);
        recruit.setPos(absoluteChest.getX() + 0.5, absoluteChest.getY(), absoluteChest.getZ() + 0.5);
        setRecruitField(recruit, "workerPhase", WorkerPhase.DEPOSIT);
        setRecruitField(recruit, "workerReason", "deposit_inventory");
        setRecruitField(recruit, "activeWorkTarget", absoluteChest);
        recruit.tickWorkerController();
        if (!recruit.getWorkerStatus().reasonCode().equals("storage_full_or_missing")
                || workerInventoryCount(recruit) != carriedBeforeFailedDeposit) {
            helper.fail("Failed deposit consumed worker resources");
        }

        recruit.die(helper.getLevel().damageSources().generic());
        int droppedWorkerItems = helper.getLevel().getEntitiesOfClass(
                        ItemEntity.class, recruit.getBoundingBox().inflate(4.0))
                .stream()
                .filter(item -> item.getItem().is(Items.WHEAT) || item.getItem().is(Items.WHEAT_SEEDS))
                .mapToInt(item -> item.getItem().getCount())
                .sum();
        if (droppedWorkerItems != carriedBeforeFailedDeposit
                || data.kingdomForOwner(owner.getUUID()).orElseThrow()
                        .settlement().containsRecruit(recruit.getUUID())) {
            helper.fail("Worker death did not conserve carried resources and release housing");
        }
        helper.succeed();
    }

    private static void enabledWorkerLoops(GameTestHelper helper) {
        BlockPos relativeHall = new BlockPos(1, 1, 1);
        helper.setBlock(relativeHall, ModBlocks.COMMAND_CENTER.get());
        BlockPos hallPos = helper.absolutePos(relativeHall);
        CommandCenterBlockEntity hall = helper.getBlockEntity(relativeHall, CommandCenterBlockEntity.class);
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
            throw new IllegalStateException("Worker did not claim a persisted work order");
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
