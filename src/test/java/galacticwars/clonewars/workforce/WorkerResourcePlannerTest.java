package galacticwars.clonewars.workforce;

public final class WorkerResourcePlannerTest {
    private WorkerResourcePlannerTest() {
    }

    public static void main(String[] args) {
        workerGathersProfessionResourceWhenInventoryHasRoom();
        workerDepositsResourcesWhenInventoryIsFull();
        workerWithoutValidWorksiteStaysIdle();

        System.out.println("WorkerResourcePlannerTest passed");
    }

    private static void workerGathersProfessionResourceWhenInventoryHasRoom() {
        WorkerResourceDecision farmer = WorkerResourcePlanner.plan(
                WorkerProfession.FARMER,
                new WorkerWorksite(WorkAreaType.CROP_FARM, 10, 64, 10, 8),
                ResourceInventory.empty(),
                ResourceInventory.empty(),
                128);
        WorkerResourceDecision lumberjack = WorkerResourcePlanner.plan(
                WorkerProfession.LUMBERJACK,
                new WorkerWorksite(WorkAreaType.LUMBER_AREA, 20, 64, 20, 8),
                ResourceInventory.empty(),
                ResourceInventory.empty(),
                128);
        WorkerResourceDecision miner = WorkerResourcePlanner.plan(
                WorkerProfession.MINER,
                new WorkerWorksite(WorkAreaType.MINING_AREA, 30, 48, 30, 8),
                ResourceInventory.empty(),
                ResourceInventory.empty(),
                128);

        assertDecision(WorkerResourceAction.GATHER_RESOURCE, "minecraft:wheat", 1, "ready_to_gather", farmer);
        assertDecision(WorkerResourceAction.GATHER_RESOURCE, "minecraft:oak_log", 1, "ready_to_gather", lumberjack);
        assertDecision(WorkerResourceAction.GATHER_RESOURCE, "minecraft:cobblestone", 1, "ready_to_gather", miner);
    }

    private static void workerDepositsResourcesWhenInventoryIsFull() {
        WorkerResourceDecision decision = WorkerResourcePlanner.plan(
                WorkerProfession.LUMBERJACK,
                new WorkerWorksite(WorkAreaType.LUMBER_AREA, 20, 64, 20, 8),
                ResourceInventory.of("minecraft:oak_log", 128),
                ResourceInventory.empty(),
                128);

        assertDecision(WorkerResourceAction.DEPOSIT_TO_STORAGE, "minecraft:oak_log", 128, "inventory_full", decision);
    }

    private static void workerWithoutValidWorksiteStaysIdle() {
        WorkerResourceDecision decision = WorkerResourcePlanner.plan(
                WorkerProfession.BUILDER,
                new WorkerWorksite(WorkAreaType.CROP_FARM, 10, 64, 10, 8),
                ResourceInventory.empty(),
                ResourceInventory.empty(),
                128);

        assertDecision(WorkerResourceAction.IDLE, "", 0, "worksite_type_mismatch", decision);
    }

    private static void assertDecision(
            WorkerResourceAction action,
            String itemId,
            int quantity,
            String reasonCode,
            WorkerResourceDecision decision
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
