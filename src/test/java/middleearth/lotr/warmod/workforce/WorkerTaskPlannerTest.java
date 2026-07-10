package middleearth.lotr.warmod.workforce;

public final class WorkerTaskPlannerTest {
    private WorkerTaskPlannerTest() {
    }

    public static void main(String[] args) {
        eachProfessionPlansExpectedWorkTaskAtMatchingArea();
        mismatchedWorkAreaRequiresReassignment();
        missingWorksiteKeepsWorkerIdle();

        System.out.println("WorkerTaskPlannerTest passed");
    }

    private static void eachProfessionPlansExpectedWorkTaskAtMatchingArea() {
        assertTask(WorkerProfession.FARMER, WorkAreaType.CROP_FARM, WorkerTaskType.HARVEST_AND_REPLANT);
        assertTask(WorkerProfession.LUMBERJACK, WorkAreaType.LUMBER_AREA, WorkerTaskType.CHOP_AND_REPLANT);
        assertTask(WorkerProfession.FISHERMAN, WorkAreaType.FISHING_AREA, WorkerTaskType.FISH);
        assertTask(WorkerProfession.ANIMAL_FARMER, WorkAreaType.ANIMAL_FARM, WorkerTaskType.FEED_AND_BREED_ANIMALS);
        assertTask(WorkerProfession.MINER, WorkAreaType.MINING_AREA, WorkerTaskType.MINE_AND_STORE);
        assertTask(WorkerProfession.BUILDER, WorkAreaType.BUILDING_AREA, WorkerTaskType.BUILD_FROM_SUPPLIES);
        assertTask(WorkerProfession.COOK, WorkAreaType.KITCHEN, WorkerTaskType.COOK_FOOD);
        assertTask(WorkerProfession.MERCHANT, WorkAreaType.MARKET, WorkerTaskType.TRADE_GOODS);
        assertTask(WorkerProfession.COURIER, WorkAreaType.COURIER_ROUTE, WorkerTaskType.DELIVER_SUPPLIES);
    }

    private static void mismatchedWorkAreaRequiresReassignment() {
        WorkerTaskDecision decision = WorkerTaskPlanner.plan(
                WorkerProfession.MINER,
                new WorkerWorksite(WorkAreaType.CROP_FARM, 10, 64, 10, 8));

        assertEquals(WorkerTaskType.REASSIGN_WORKSITE, decision.taskType(), "mismatched task");
        assertEquals("worksite_type_mismatch", decision.reasonCode(), "mismatched reason");
        assertEquals(WorkAreaType.MINING_AREA, decision.requiredAreaType(), "required area");
    }

    private static void missingWorksiteKeepsWorkerIdle() {
        WorkerTaskDecision decision = WorkerTaskPlanner.plan(WorkerProfession.BUILDER, null);

        assertEquals(WorkerTaskType.IDLE, decision.taskType(), "missing worksite task");
        assertEquals("missing_worksite", decision.reasonCode(), "missing worksite reason");
        assertEquals(WorkAreaType.BUILDING_AREA, decision.requiredAreaType(), "missing worksite required area");
    }

    private static void assertTask(WorkerProfession profession, WorkAreaType areaType, WorkerTaskType taskType) {
        WorkerTaskDecision decision = WorkerTaskPlanner.plan(
                profession,
                new WorkerWorksite(areaType, 1, 64, 1, 8));

        assertEquals(taskType, decision.taskType(), profession.name() + " task");
        assertEquals("ready", decision.reasonCode(), profession.name() + " reason");
        assertEquals(areaType, decision.requiredAreaType(), profession.name() + " area");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
