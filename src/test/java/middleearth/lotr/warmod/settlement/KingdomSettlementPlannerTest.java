package middleearth.lotr.warmod.settlement;

import java.util.Map;
import java.util.Optional;

import middleearth.lotr.warmod.workforce.ResourceInventory;
import middleearth.lotr.warmod.workforce.WorkerProfession;

public final class KingdomSettlementPlannerTest {
    private KingdomSettlementPlannerTest() {
    }

    public static void main(String[] args) {
        plannerOrdersMinerWhenStarterKeepNeedsStone();
        plannerOrdersBuilderWhenSuppliesAreReady();
        plannerRecommendsBuilderBeforeRecruitingMoreGatherers();
        plannerRecommendsCourierWhenStorageHasSupplyChainPressure();

        System.out.println("KingdomSettlementPlannerTest passed");
    }

    private static void plannerOrdersMinerWhenStarterKeepNeedsStone() {
        KingdomWorkOrder order = KingdomSettlementPlanner.planNextWorkOrder(
                KingdomSettlementState.empty()
                        .withWorker(WorkerProfession.MINER, 1)
                        .withWorker(WorkerProfession.BUILDER, 1),
                KingdomBaseBlueprint.starterKeep());

        assertEquals(KingdomWorkOrderType.GATHER_RESOURCE, order.type(), "order type");
        assertEquals(WorkerProfession.MINER, order.profession(), "profession");
        assertEquals("kingdomwarsmiddleearth:middle_earth_stone", order.itemId(), "item");
        assertEquals("missing_build_supply", order.reasonCode(), "reason");
    }

    private static void plannerOrdersBuilderWhenSuppliesAreReady() {
        KingdomBaseBlueprint blueprint = KingdomBaseBlueprint.starterKeep();
        KingdomWorkOrder order = KingdomSettlementPlanner.planNextWorkOrder(
                KingdomSettlementState.empty()
                        .withStockpile(blueprint.requiredResources())
                        .withWorker(WorkerProfession.MINER, 1)
                        .withWorker(WorkerProfession.BUILDER, 1),
                blueprint);

        assertEquals(KingdomWorkOrderType.BUILD_BLOCK, order.type(), "order type");
        assertEquals(WorkerProfession.BUILDER, order.profession(), "profession");
        assertEquals("kingdomwarsmiddleearth:middle_earth_stone", order.itemId(), "item");
        assertEquals(1, order.quantity(), "quantity");
        assertEquals("ready_to_place_base_block", order.reasonCode(), "reason");
    }

    private static void plannerRecommendsBuilderBeforeRecruitingMoreGatherers() {
        Optional<WorkerProfession> recommendation = KingdomSettlementPlanner.recommendNextProfession(
                new KingdomSettlementState(
                        ResourceInventory.empty(),
                        Map.of(WorkerProfession.MINER, 1, WorkerProfession.LUMBERJACK, 1),
                        2,
                        6,
                        0,
                        true),
                KingdomBaseBlueprint.starterKeep());

        assertEquals(Optional.of(WorkerProfession.BUILDER), recommendation, "recommendation");
    }

    private static void plannerRecommendsCourierWhenStorageHasSupplyChainPressure() {
        Optional<WorkerProfession> recommendation = KingdomSettlementPlanner.recommendNextProfession(
                new KingdomSettlementState(
                        ResourceInventory.of("kingdomwarsmiddleearth:middle_earth_stone", 64),
                        Map.of(WorkerProfession.MINER, 1, WorkerProfession.BUILDER, 1),
                        2,
                        6,
                        0,
                        true),
                KingdomBaseBlueprint.starterKeep());

        assertEquals(Optional.of(WorkerProfession.COURIER), recommendation, "recommendation");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
