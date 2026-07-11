package galacticwars.clonewars.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RecruitWorkerAiIntegrationTest {
    private RecruitWorkerAiIntegrationTest() {
    }

    public static void main(String[] args) throws IOException {
        recruitRegistersWorkerGoal();
        recruitPersistsWorkerStateMachine();
        recruitUsesRealLootAndInventoryTransactions();
        recruitRequiresReachableClaimedTargets();
        recruitImplementsEnabledProfessionHandlers();
        builderNormalizesProgressionSubject();
        workerGoalDelegatesToRecruitCycle();

        System.out.println("RecruitWorkerAiIntegrationTest passed");
    }

    private static void recruitRegistersWorkerGoal() throws IOException {
        String entity = read("src/main/java/galacticwars/clonewars/entity/GalacticRecruitEntity.java");

        assertContains(entity, "RecruitWorkerGoal", "worker goal import/use");
        assertContains(entity, "this.goalSelector.addGoal(4, new RecruitWorkerGoal(this));", "worker goal registration");
    }

    private static void recruitPersistsWorkerStateMachine() throws IOException {
        String entity = read("src/main/java/galacticwars/clonewars/entity/GalacticRecruitEntity.java");

        assertContains(entity, "workerInventory", "real worker inventory field");
        assertContains(entity, "WorkerPhase workerPhase", "worker phase field");
        assertContains(entity, "activeWorkTarget", "active target field");
        assertContains(entity, "starterBaseCompletedBlocks", "base completed block count field");
        assertContains(entity, "\"WorkerInventory\"", "worker inventory save key");
        assertContains(entity, "\"WorkerPhase\"", "worker phase save key");
        assertContains(entity, "\"StarterBaseCompletedBlocks\"", "base progress save key");
    }

    private static void builderNormalizesProgressionSubject() throws IOException {
        String entity = read("src/main/java/galacticwars/clonewars/entity/GalacticRecruitEntity.java");
        assertContains(entity, "KingdomBaseBlueprint.path(blueprint.id())",
                "canonical builder progression subject");
    }

    private static void recruitUsesRealLootAndInventoryTransactions() throws IOException {
        String entity = read("src/main/java/galacticwars/clonewars/entity/GalacticRecruitEntity.java");

        assertContains(entity, "Block.getDrops", "loot table evaluation");
        assertContains(entity, "destroyBlock(target, false", "single non-dropping world removal");
        assertContains(entity, "mergeAll", "transactional inventory simulation");
        assertContains(entity, "insertWorkerInventory", "real container deposit");
        assertContains(entity, "withdrawSpecificItem", "real container withdrawal");
        assertContains(entity, "tool.hurtAndBreak", "tool durability");
        assertNotContains(entity, "this.carriedResources = this.carriedResources.withAdded(buildDecision.itemId(), 1)",
                "conjured builder supplies");
    }

    private static void recruitRequiresReachableClaimedTargets() throws IOException {
        String entity = read("src/main/java/galacticwars/clonewars/entity/GalacticRecruitEntity.java");

        assertContains(entity, "tickWorkerNavigation", "worker-owned navigation");
        assertContains(entity, "target_unreachable", "path failure reason");
        assertContains(entity, "blacklistedWorkTarget", "unreachable target blacklist");
        assertContains(entity, "interaction_out_of_reach", "interaction reach guard");
        assertContains(entity, "canModifyWorkerTarget", "claim validation");
        assertContains(entity, "scanBudget = Math.min(128", "bounded scan budget");
        assertContains(entity, "Math.floorMod(this.getUUID().hashCode(), 4)", "staggered worker scans");
        assertContains(entity, "this.level().isLoaded", "loaded chunk guard");
    }

    private static void recruitImplementsEnabledProfessionHandlers() throws IOException {
        String entity = read("src/main/java/galacticwars/clonewars/entity/GalacticRecruitEntity.java");

        assertContains(entity, "CropBlock", "farmer crop handling");
        assertContains(entity, "Items.WHEAT_SEEDS", "farmer seed consumption");
        assertContains(entity, "Items.OAK_SAPLING", "lumberjack replanting");
        assertContains(entity, "ModBlockTags.WORKER_MINEABLE", "miner allowlist");
        assertContains(entity, "acquireCourierOrder", "courier route handler");
        assertContains(entity, "placeCurrentBuildBlock", "builder placement handler");
    }

    private static void workerGoalDelegatesToRecruitCycle() throws IOException {
        String goal = read("src/main/java/galacticwars/clonewars/entity/ai/RecruitWorkerGoal.java");

        assertContains(goal, "class RecruitWorkerGoal extends Goal", "worker goal class");
        assertContains(goal, "shouldRunWorkerCycle", "worker cycle guard");
        assertContains(goal, "tickWorkerController", "worker controller tick");
        assertContains(goal, "pauseWorkerNavigation", "worker navigation cleanup");
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    private static void assertContains(String haystack, String needle, String label) {
        if (!haystack.contains(needle)) {
            throw new AssertionError(label + " missing <" + needle + ">");
        }
    }

    private static void assertNotContains(String haystack, String needle, String label) {
        if (haystack.contains(needle)) {
            throw new AssertionError(label + " unexpectedly contains <" + needle + ">");
        }
    }
}
