package galacticwars.clonewars.workforce;

public final class WorkerLogisticsPlannerTest {
    private WorkerLogisticsPlannerTest() {
    }

    public static void main(String[] args) {
        courierWithdrawsRequestedItemFromStorage();
        courierDeliversCarriedRequestedItemBeforeWithdrawingMore();
        courierDepositsUnrequestedLoadWhenInventoryIsFull();
        courierIdlesWithoutSupplyRequest();
        courierIdlesWhenStorageCannotFillRequest();

        System.out.println("WorkerLogisticsPlannerTest passed");
    }

    private static void courierWithdrawsRequestedItemFromStorage() {
        WorkerLogisticsDecision decision = WorkerLogisticsPlanner.plan(
                route(),
                new WorkerSupplyRequest("galacticwars:duracrete", 4),
                ResourceInventory.empty(),
                ResourceInventory.of("galacticwars:duracrete", 12),
                8);

        assertDecision(
                WorkerResourceAction.WITHDRAW_FROM_STORAGE,
                "galacticwars:duracrete",
                4,
                "storage_can_fill_request",
                decision);
        assertEquals(WorkAreaType.STORAGE, decision.route().storageSite().areaType(), "storage route area");
        assertEquals(WorkAreaType.BUILDING_AREA, decision.route().destinationSite().areaType(), "destination route area");
    }

    private static void courierDeliversCarriedRequestedItemBeforeWithdrawingMore() {
        WorkerLogisticsDecision decision = WorkerLogisticsPlanner.plan(
                route(),
                new WorkerSupplyRequest("minecraft:oak_log", 8),
                ResourceInventory.of("minecraft:oak_log", 3),
                ResourceInventory.of("minecraft:oak_log", 20),
                8);

        assertDecision(WorkerResourceAction.DELIVER_TO_WORKSITE, "minecraft:oak_log", 3, "carrying_requested_item", decision);
    }

    private static void courierDepositsUnrequestedLoadWhenInventoryIsFull() {
        WorkerLogisticsDecision decision = WorkerLogisticsPlanner.plan(
                route(),
                new WorkerSupplyRequest("minecraft:bread", 2),
                ResourceInventory.of("minecraft:wheat", 8),
                ResourceInventory.of("minecraft:bread", 12),
                8);

        assertDecision(WorkerResourceAction.DEPOSIT_TO_STORAGE, "minecraft:wheat", 8, "inventory_full_unrequested", decision);
    }

    private static void courierIdlesWithoutSupplyRequest() {
        WorkerLogisticsDecision decision = WorkerLogisticsPlanner.plan(
                route(),
                null,
                ResourceInventory.empty(),
                ResourceInventory.of("minecraft:bread", 12),
                8);

        assertDecision(WorkerResourceAction.IDLE, "", 0, "no_supply_request", decision);
    }

    private static void courierIdlesWhenStorageCannotFillRequest() {
        WorkerLogisticsDecision decision = WorkerLogisticsPlanner.plan(
                route(),
                new WorkerSupplyRequest("minecraft:bread", 2),
                ResourceInventory.empty(),
                ResourceInventory.empty(),
                8);

        assertDecision(WorkerResourceAction.IDLE, "minecraft:bread", 0, "storage_missing_requested_item", decision);
    }

    private static WorkerLogisticsRoute route() {
        return new WorkerLogisticsRoute(
                new WorkerWorksite(WorkAreaType.STORAGE, 0, 64, 0, 4),
                new WorkerWorksite(WorkAreaType.BUILDING_AREA, 12, 64, 12, 8));
    }

    private static void assertDecision(
            WorkerResourceAction action,
            String itemId,
            int quantity,
            String reasonCode,
            WorkerLogisticsDecision decision
    ) {
        assertEquals(action, decision.action(), "action");
        assertEquals(itemId, decision.itemId(), "item id");
        assertEquals(quantity, decision.quantity(), "quantity");
        assertEquals(reasonCode, decision.reasonCode(), "reason");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
