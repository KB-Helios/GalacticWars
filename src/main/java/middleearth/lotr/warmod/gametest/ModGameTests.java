package middleearth.lotr.warmod.gametest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import middleearth.lotr.warmod.KingdomWarsMiddleEarth;
import middleearth.lotr.warmod.entity.MiddleEarthRecruitEntity;
import middleearth.lotr.warmod.entity.RecruitLifecycleService;
import middleearth.lotr.warmod.kingdom.BuildProject;
import middleearth.lotr.warmod.kingdom.KingdomRecord;
import middleearth.lotr.warmod.kingdom.KingdomSavedData;
import middleearth.lotr.warmod.kingdom.RecruitmentCampaign;
import middleearth.lotr.warmod.kingdom.RecruitmentCampaignDecision;
import middleearth.lotr.warmod.kingdom.RecruitmentCampaignState;
import middleearth.lotr.warmod.menu.RecruitCommandMenu;
import middleearth.lotr.warmod.recruitment.RecruitmentAction;
import middleearth.lotr.warmod.recruitment.RecruitDuty;
import middleearth.lotr.warmod.recruitment.RecruitmentPaymentService;
import middleearth.lotr.warmod.registry.ModBlockTags;
import middleearth.lotr.warmod.registry.ModBlocks;
import middleearth.lotr.warmod.registry.ModEntityTypes;
import middleearth.lotr.warmod.settlement.KingdomHallBlockEntity;
import middleearth.lotr.warmod.workforce.WorkerPhase;
import middleearth.lotr.warmod.workforce.WorkerProfession;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
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
        tests.put(id("kingdom_hall_authority"), ModGameTests::kingdomHallAuthority);
        tests.put(id("recruit_entity_contract"), ModGameTests::recruitEntityContract);
        tests.put(id("worker_tags_and_loot"), ModGameTests::workerTagsAndLoot);
        tests.put(id("recruit_contract_lifecycle"), ModGameTests::recruitContractLifecycle);
        tests.put(id("worker_resource_conservation"), ModGameTests::workerResourceConservation);
        return Map.copyOf(tests);
    }

    private static void kingdomHallAuthority(GameTestHelper helper) {
        BlockPos hallPos = new BlockPos(1, 1, 1);
        helper.setBlock(hallPos, ModBlocks.KINGDOM_HALL.get());
        KingdomHallBlockEntity hall = helper.getBlockEntity(hallPos, KingdomHallBlockEntity.class);
        ServerPlayer owner = makeConnectedMockPlayer(helper, GameType.CREATIVE);
        ServerPlayer intruder = makeConnectedMockPlayer(helper, GameType.CREATIVE);
        if (!hall.claim(owner) || hall.claim(intruder) || !hall.isOwner(owner)) {
            helper.fail("Kingdom Hall ownership guard rejected the owner or accepted an intruder");
        }
        long claimGameTime = helper.getLevel().getGameTime();
        if (!hall.chargeDailyUpkeep(Math.addExact(claimGameTime, 23999L), 1)) {
            helper.fail("New Kingdom Hall was charged upkeep before its first full day elapsed");
        }
        hall.setFaction("kingdomwarsmiddleearth:rohan");
        if (hall.getUpdatePacket() == null
                || !hall.getUpdateTag(helper.getLevel().registryAccess())
                        .getStringOr("KingdomFaction", "")
                        .equals("kingdomwarsmiddleearth:rohan")) {
            helper.fail("Kingdom Hall custom state was not exposed through its client update packet");
        }
        hall.setItem(0, new ItemStack(Items.EMERALD, 32));
        if (!hall.reserveEmeralds(10) || hall.treasuryEmeralds() != 22 || hall.refundEmeralds(5) != 5
                || hall.treasuryEmeralds() != 27) {
            helper.fail("Kingdom Hall treasury did not conserve reserved and refunded emeralds");
        }
        if (!hall.reserveEmeralds(27) || hall.getItem(0) != ItemStack.EMPTY) {
            helper.fail("Kingdom Hall treasury did not normalize a depleted slot to ItemStack.EMPTY");
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
                "kingdomwarsmiddleearth:rohan_rider",
                "",
                12,
                claimGameTime + 24000L,
                RecruitmentCampaignState.RESERVED,
                "reserved");
        if (!data.beginCampaign(owner.getUUID(), RecruitmentCampaignDecision.accepted(campaign))
                || !data.replaceCampaign(owner.getUUID(), campaign.cancel("commander_lost"))
                || hall.settlePendingCampaignRefunds(helper.getLevel()) != 12
                || hall.treasuryEmeralds() != 12
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
            helper.fail("A second active Kingdom Hall was accepted");
        }
        ModBlocks.KINGDOM_HALL.get().playerWillDestroy(
                helper.getLevel(), absoluteHall, helper.getLevel().getBlockState(absoluteHall), intruder);
        if (!data.isHallActive(owner.getUUID()) || hall.treasuryEmeralds() != 12) {
            helper.fail("Intruder Hall removal bypassed owner authority");
        }
        ModBlocks.KINGDOM_HALL.get().playerWillDestroy(
                helper.getLevel(), absoluteHall, helper.getLevel().getBlockState(absoluteHall), owner);
        int removalRefund = helper.getLevel().getEntitiesOfClass(
                        ItemEntity.class,
                        new net.minecraft.world.phys.AABB(absoluteHall).inflate(3.0))
                .stream()
                .filter(item -> item.getItem().is(Items.EMERALD))
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
            helper.fail("Kingdom Hall relocation did not preserve kingdom identity and update its position");
        }

        int housingBeforeReward = relocated.settlement().housingCapacity();
        BuildProject farmProject = new BuildProject(
                UUID.randomUUID(),
                "farm_plot",
                helper.getLevel().dimension().identifier().toString(),
                relocatedHall.getX() + 4,
                relocatedHall.getY(),
                relocatedHall.getZ(),
                0,
                List.of(),
                "");
        if (!data.completeBuildProject(owner.getUUID(), farmProject, 2, "farmer", 2)
                || data.completeBuildProject(owner.getUUID(), farmProject, 2, "farmer", 2)) {
            helper.fail("Build rewards were not applied exactly once");
        }
        KingdomRecord rewarded = data.kingdomForOwner(owner.getUUID()).orElseThrow();
        if (rewarded.settlement().housingCapacity() != housingBeforeReward + 2
                || rewarded.settlement().buildProjects().stream()
                        .filter(project -> project.blueprintId().equals("farm_plot"))
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
                || restoredReward.settlement().housingCapacity() != housingBeforeReward + 2
                || restoredReward.settlement().worksites().stream()
                        .filter(worksite -> worksite.type().equals("farmer"))
                        .count() != 1) {
            helper.fail("Build completion rewards did not survive a SavedData codec round trip");
        }
        helper.succeed();
    }

    private static void recruitEntityContract(GameTestHelper helper) {
        MiddleEarthRecruitEntity recruit = helper.spawn(ModEntityTypes.GONDOR_RECRUIT.get(), new BlockPos(1, 1, 1));
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
        helper.setBlock(stonePos, ModBlocks.MIDDLE_EARTH_STONE.get());
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
                || drops.stream().noneMatch(stack -> stack.is(ModBlocks.MIDDLE_EARTH_STONE.asItem()))) {
            helper.fail("Middle-earth stone loot table did not conserve one mined block");
        }
        helper.succeed();
    }

    private static void recruitContractLifecycle(GameTestHelper helper) {
        BlockPos hallPos = new BlockPos(1, 1, 1);
        helper.setBlock(hallPos, ModBlocks.KINGDOM_HALL.get());
        KingdomHallBlockEntity hall = helper.getBlockEntity(hallPos, KingdomHallBlockEntity.class);
        ServerPlayer owner = makeConnectedMockPlayer(helper, GameType.SURVIVAL);
        ServerPlayer intruder = makeConnectedMockPlayer(helper, GameType.SURVIVAL);
        MiddleEarthRecruitEntity recruit = helper.spawn(
                ModEntityTypes.GONDOR_RECRUIT.get(), new BlockPos(3, 1, 1));
        owner.setPos(recruit.getX(), recruit.getY(), recruit.getZ());
        intruder.setPos(recruit.getX(), recruit.getY(), recruit.getZ());
        hall.claim(owner);
        KingdomSavedData data = KingdomSavedData.get(helper.getLevel());
        data.activateHall(
                owner.getUUID(),
                hall.factionId(),
                helper.getLevel().dimension().identifier().toString(),
                helper.absolutePos(hallPos)).orElseThrow();
        owner.getInventory().add(new ItemStack(Items.EMERALD, 53));

        boolean hired = recruit.handleMenuButton(owner, RecruitCommandMenu.BUTTON_HIRE);
        boolean owned = recruit.isOwnedBy(owner);
        int remainingEmeralds = RecruitmentPaymentService.emeraldCount(owner);
        boolean registered = data.kingdomForOwner(owner.getUUID()).orElseThrow()
                .settlement().containsRecruit(recruit.getUUID());
        if (!hired || !owned || remainingEmeralds != 28 || !registered) {
            helper.fail("Exact-cost direct hiring failed: hired=" + hired
                    + ", owned=" + owned
                    + ", emeralds=" + remainingEmeralds
                    + ", registered=" + registered);
        }
        BlockPos intruderHallPos = new BlockPos(1, 1, 3);
        helper.setBlock(intruderHallPos, ModBlocks.KINGDOM_HALL.get());
        KingdomHallBlockEntity intruderHall = helper.getBlockEntity(
                intruderHallPos, KingdomHallBlockEntity.class);
        intruderHall.claim(intruder);
        data.activateHall(
                intruder.getUUID(),
                intruderHall.factionId(),
                helper.getLevel().dimension().identifier().toString(),
                helper.absolutePos(intruderHallPos)).orElseThrow();
        MiddleEarthRecruitEntity poorRecruit = helper.spawn(
                ModEntityTypes.GONDOR_RECRUIT.get(), new BlockPos(3, 1, 3));
        intruder.setPos(poorRecruit.getX(), poorRecruit.getY(), poorRecruit.getZ());
        intruder.getInventory().add(new ItemStack(Items.EMERALD, 24));
        if (poorRecruit.handleMenuButton(intruder, RecruitCommandMenu.BUTTON_HIRE)
                || RecruitmentPaymentService.emeraldCount(intruder) != 24
                || data.kingdomForOwner(intruder.getUUID()).orElseThrow()
                        .settlement().containsRecruit(poorRecruit.getUUID())) {
            helper.fail("Insufficient direct-hire payment mutated funds or settlement state");
        }
        if (intruderHall.chargeDailyUpkeep(helper.getLevel().getGameTime() + 24000L, 1)) {
            helper.fail("Empty Hall treasury incorrectly paid upkeep");
        }
        intruder.getInventory().add(new ItemStack(Items.EMERALD));
        if (poorRecruit.handleMenuButton(intruder, RecruitCommandMenu.BUTTON_HIRE)
                || RecruitmentPaymentService.emeraldCount(intruder) != 25) {
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

        MiddleEarthRecruitEntity commander = helper.spawn(
                ModEntityTypes.ROHAN_RECRUIT.get(), new BlockPos(3, 1, 2));
        owner.setPos(commander.getX(), commander.getY(), commander.getZ());
        owner.getInventory().add(new ItemStack(Items.EMERALD, 25));
        if (!commander.handleMenuButton(owner, RecruitCommandMenu.BUTTON_HIRE)) {
            helper.fail("Commander candidate could not be hired");
        }
        BuildProject keepProject = new BuildProject(
                UUID.randomUUID(),
                "starter_keep",
                helper.getLevel().dimension().identifier().toString(),
                helper.absolutePos(hallPos).getX() + 8,
                helper.absolutePos(hallPos).getY(),
                helper.absolutePos(hallPos).getZ(),
                0,
                List.of(),
                "");
        if (!data.completeBuildProject(owner.getUUID(), keepProject, 0, "", 0)
                || !commander.handleMenuButton(owner, RecruitCommandMenu.BUTTON_PROMOTE_COMMANDER)) {
            helper.fail("Commander promotion prerequisites were not applied");
        }
        RecruitmentCampaign commanderCampaign = new RecruitmentCampaign(
                UUID.randomUUID(),
                "kingdomwarsmiddleearth:rohan_recruit",
                "",
                7,
                helper.getLevel().getGameTime() + 24000L,
                RecruitmentCampaignState.RESERVED,
                "reserved");
        hall.setItem(0, new ItemStack(Items.EMERALD, 7));
        if (!hall.reserveEmeralds(7)
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
                || hall.treasuryEmeralds() != 7) {
            helper.fail("Commander death did not release state and conserve its campaign reservation");
        }

        for (int index = 0; index < 3; index++) {
            if (!data.registerRecruit(owner.getUUID(), UUID.randomUUID())) {
                helper.fail("Could not fill settlement housing for the rejection check");
            }
        }
        MiddleEarthRecruitEntity crowdedRecruit = helper.spawn(
                ModEntityTypes.GONDOR_RECRUIT.get(), new BlockPos(4, 1, 2));
        owner.setPos(crowdedRecruit.getX(), crowdedRecruit.getY(), crowdedRecruit.getZ());
        owner.getInventory().add(new ItemStack(Items.EMERALD, 25));
        if (crowdedRecruit.handleMenuButton(owner, RecruitCommandMenu.BUTTON_HIRE)
                || RecruitmentPaymentService.emeraldCount(owner) != 25
                || crowdedRecruit.isOwnedBy(owner)) {
            helper.fail("Full housing did not reject hiring without charging the player");
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
        helper.setBlock(hallPos, ModBlocks.KINGDOM_HALL.get());
        helper.setBlock(cropPos.below(), Blocks.FARMLAND);
        helper.setBlock(cropPos, Blocks.WHEAT.defaultBlockState().setValue(BlockStateProperties.AGE_7, 7));
        helper.setBlock(chestPos, Blocks.CHEST);
        KingdomHallBlockEntity hall = helper.getBlockEntity(hallPos, KingdomHallBlockEntity.class);
        Container chest = helper.getBlockEntity(chestPos, ChestBlockEntity.class);
        ServerPlayer owner = makeConnectedMockPlayer(helper, GameType.SURVIVAL);
        MiddleEarthRecruitEntity recruit = helper.spawn(
                ModEntityTypes.GONDOR_RECRUIT.get(), new BlockPos(3, 1, 1));
        owner.setPos(recruit.getX(), recruit.getY(), recruit.getZ());
        hall.claim(owner);
        KingdomSavedData data = KingdomSavedData.get(helper.getLevel());
        data.activateHall(
                owner.getUUID(),
                hall.factionId(),
                helper.getLevel().dimension().identifier().toString(),
                helper.absolutePos(hallPos)).orElseThrow();
        owner.getInventory().add(new ItemStack(Items.EMERALD, 45));
        boolean hired = recruit.handleMenuButton(owner, RecruitCommandMenu.BUTTON_HIRE);
        boolean assignedFarmer = recruit.handleMenuButton(owner, RecruitCommandMenu.BUTTON_ASSIGN_FARMER);
        if (!hired || !assignedFarmer) {
            helper.fail("Farmer setup failed: hired=" + hired
                    + ", assigned=" + assignedFarmer
                    + ", emeralds=" + RecruitmentPaymentService.emeraldCount(owner)
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

    private static void setWorkerInventory(MiddleEarthRecruitEntity recruit, ItemStack stack) {
        NonNullList<ItemStack> inventory = NonNullList.withSize(9, ItemStack.EMPTY);
        if (!stack.isEmpty()) {
            inventory.set(0, stack.copy());
        }
        setRecruitField(recruit, "workerInventory", inventory);
    }

    @SuppressWarnings("unchecked")
    private static int workerInventoryCount(MiddleEarthRecruitEntity recruit) {
        return ((NonNullList<ItemStack>) getRecruitField(recruit, "workerInventory")).stream()
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    private static Object getRecruitField(MiddleEarthRecruitEntity recruit, String fieldName) {
        try {
            Field field = MiddleEarthRecruitEntity.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(recruit);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not read recruit field " + fieldName, exception);
        }
    }

    private static void setRecruitField(MiddleEarthRecruitEntity recruit, String fieldName, Object value) {
        try {
            Field field = MiddleEarthRecruitEntity.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(recruit, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not set recruit field " + fieldName, exception);
        }
    }

    private static void invokeRecruitCommand(MiddleEarthRecruitEntity recruit, RecruitmentAction command) {
        try {
            Method method = MiddleEarthRecruitEntity.class.getDeclaredMethod(
                    "setRecruitCommand", RecruitmentAction.class);
            method.setAccessible(true);
            method.invoke(recruit, command);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not set recruit command", exception);
        }
    }

    @SuppressWarnings("removal")
    private static ServerPlayer makeConnectedMockPlayer(GameTestHelper helper, GameType gameType) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        gameType.updatePlayerAbilities(player.getAbilities());
        return player;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(KingdomWarsMiddleEarth.MODID, path);
    }
}
