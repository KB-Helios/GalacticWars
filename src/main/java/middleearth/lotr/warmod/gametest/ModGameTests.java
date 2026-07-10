package middleearth.lotr.warmod.gametest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import middleearth.lotr.warmod.KingdomWarsMiddleEarth;
import middleearth.lotr.warmod.entity.MiddleEarthRecruitEntity;
import middleearth.lotr.warmod.kingdom.KingdomRecord;
import middleearth.lotr.warmod.kingdom.KingdomSavedData;
import middleearth.lotr.warmod.recruitment.RecruitDuty;
import middleearth.lotr.warmod.registry.ModBlockTags;
import middleearth.lotr.warmod.registry.ModBlocks;
import middleearth.lotr.warmod.registry.ModEntityTypes;
import middleearth.lotr.warmod.settlement.KingdomHallBlockEntity;
import middleearth.lotr.warmod.workforce.WorkerProfession;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
        return Map.copyOf(tests);
    }

    private static void kingdomHallAuthority(GameTestHelper helper) {
        BlockPos hallPos = new BlockPos(1, 1, 1);
        helper.setBlock(hallPos, ModBlocks.KINGDOM_HALL.get());
        KingdomHallBlockEntity hall = helper.getBlockEntity(hallPos, KingdomHallBlockEntity.class);
        var owner = helper.makeMockServerPlayer(GameType.CREATIVE);
        var intruder = helper.makeMockServerPlayer(GameType.CREATIVE);
        if (!hall.claim(owner) || hall.claim(intruder) || !hall.isOwner(owner)) {
            helper.fail("Kingdom Hall ownership guard rejected the owner or accepted an intruder");
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
        KingdomRecord kingdom = KingdomSavedData.get(helper.getLevel()).foundKingdom(
                owner.getUUID(),
                hall.factionId(),
                helper.getLevel().dimension().identifier().toString(),
                helper.absolutePos(hallPos));
        if (!kingdom.ownerId().equals(owner.getUUID())
                || KingdomSavedData.get(helper.getLevel()).kingdomForOwner(owner.getUUID()).isEmpty()) {
            helper.fail("Kingdom state was not stored authoritatively in overworld SavedData");
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

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(KingdomWarsMiddleEarth.MODID, path);
    }
}
