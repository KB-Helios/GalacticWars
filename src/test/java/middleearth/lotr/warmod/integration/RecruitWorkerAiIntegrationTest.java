package middleearth.lotr.warmod.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RecruitWorkerAiIntegrationTest {
    private RecruitWorkerAiIntegrationTest() {
    }

    public static void main(String[] args) throws IOException {
        recruitRegistersWorkerGoal();
        recruitPersistsWorkerResourceState();
        recruitCanAdvanceWorkerCycle();
        recruitHarvestsRealWorksiteBlocks();
        recruitUsesCourierLogisticsPlanner();
        workerGoalDelegatesToRecruitCycle();

        System.out.println("RecruitWorkerAiIntegrationTest passed");
    }

    private static void recruitRegistersWorkerGoal() throws IOException {
        String entity = read("src/main/java/middleearth/lotr/warmod/entity/MiddleEarthRecruitEntity.java");

        assertContains(entity, "RecruitWorkerGoal", "worker goal import/use");
        assertContains(entity, "this.goalSelector.addGoal(4, new RecruitWorkerGoal(this));", "worker goal registration");
    }

    private static void recruitPersistsWorkerResourceState() throws IOException {
        String entity = read("src/main/java/middleearth/lotr/warmod/entity/MiddleEarthRecruitEntity.java");

        assertContains(entity, "carriedResources", "carried resources field");
        assertContains(entity, "storageResources", "storage resources field");
        assertContains(entity, "starterBaseCompletedBlocks", "base completed block count field");
        assertContains(entity, "\"WorkerCarriedResources\"", "carried resources save key");
        assertContains(entity, "\"WorkerStorageResources\"", "storage resources save key");
        assertContains(entity, "\"StarterBaseCompletedBlocks\"", "base progress save key");
    }

    private static void recruitCanAdvanceWorkerCycle() throws IOException {
        String entity = read("src/main/java/middleearth/lotr/warmod/entity/MiddleEarthRecruitEntity.java");

        assertContains(entity, "performWorkerCycle", "worker cycle method");
        assertContains(entity, "WorkerResourceAction.GATHER_RESOURCE", "gather action handling");
        assertContains(entity, "WorkerResourceAction.DEPOSIT_TO_STORAGE", "deposit action handling");
        assertContains(entity, "KingdomBaseBuildAction.PLACE_BLOCK", "base place action handling");
        assertContains(entity, "completeNextStarterBaseBlock", "base placement method");
        assertContains(entity, "this.level().isClientSide()", "server-only worker cycle guard");
    }

    private static void recruitHarvestsRealWorksiteBlocks() throws IOException {
        String entity = read("src/main/java/middleearth/lotr/warmod/entity/MiddleEarthRecruitEntity.java");

        assertContains(entity, "tryHarvestWorksiteResource", "world resource harvest method");
        assertContains(entity, "findHarvestTarget", "worksite scan method");
        assertContains(entity, "this.level().destroyBlock", "world block harvest call");
        assertContains(entity, "CropBlock", "farmer crop handling");
        assertContains(entity, "Blocks.OAK_LOG", "lumberjack target block");
        assertContains(entity, "Blocks.STONE", "miner target block");
        assertContains(entity, "Blocks.WHEAT", "farmer target block");
        assertContains(entity, "state.canBeReplaced()", "builder can place through replaceable blocks");
    }

    private static void recruitUsesCourierLogisticsPlanner() throws IOException {
        String entity = read("src/main/java/middleearth/lotr/warmod/entity/MiddleEarthRecruitEntity.java");

        assertContains(entity, "planCourierLogistics", "courier logistics method");
        assertContains(entity, "WorkerLogisticsPlanner.planAnyAvailableSupply", "courier logistics planner hook");
        assertContains(entity, "WorkerResourceAction.WITHDRAW_FROM_STORAGE", "courier withdraw handling");
        assertContains(entity, "WorkerResourceAction.DELIVER_TO_WORKSITE", "courier delivery handling");
        assertContains(entity, "createCourierRoute", "courier route method");
    }

    private static void workerGoalDelegatesToRecruitCycle() throws IOException {
        String goal = read("src/main/java/middleearth/lotr/warmod/entity/ai/RecruitWorkerGoal.java");

        assertContains(goal, "class RecruitWorkerGoal extends Goal", "worker goal class");
        assertContains(goal, "shouldRunWorkerCycle", "worker cycle guard");
        assertContains(goal, "performWorkerCycle", "worker cycle call");
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    private static void assertContains(String haystack, String needle, String label) {
        if (!haystack.contains(needle)) {
            throw new AssertionError(label + " missing <" + needle + ">");
        }
    }
}
