package galacticwars.clonewars.kingdom;

import java.util.List;
import java.util.UUID;
import galacticwars.clonewars.settlement.BaseBlockPlacement;
import galacticwars.clonewars.settlement.BlueprintAnchor;
import galacticwars.clonewars.settlement.KingdomBaseBlueprint;

public final class SettlementBuildRewardsTest {
    private SettlementBuildRewardsTest() {
    }

    public static void main(String[] args) {
        completedKeepUnlocksCommanderAndHousing();
        completedWorksiteAddsBoundedCapacityOnce();
        incompleteAndDetachedProjectsCannotMintRewards();
        staleAndMutatedProjectTransitionsAreRejected();
        storageOnlyRewardsRegisterDeterministicEndpoints();
        System.out.println("SettlementBuildRewardsTest passed");
    }

    private static void completedKeepUnlocksCommanderAndHousing() {
        SettlementRecord initial = SettlementRecord.create("minecraft:overworld", 0, 64, 0);
        KingdomBaseBlueprint blueprint = KingdomBaseBlueprint.starterKeep();
        Progress progress = completeProgress(initial, blueprint, 8, 64, 8);
        SettlementRecord completed = progress.settlement().withCompletedProject(progress.project(), blueprint);

        assertTrue(completed.hasCommanderSlot(), "forward base commander slot");
        assertEquals(6, completed.housingCapacity(), "forward base housing reward");
        assertEquals(1, completed.buildProjects().size(), "recorded build project");
    }

    private static void completedWorksiteAddsBoundedCapacityOnce() {
        SettlementRecord initial = SettlementRecord.create("minecraft:overworld", 0, 64, 0);
        KingdomBaseBlueprint blueprint = KingdomBaseBlueprint.farmPlot();
        Progress progress = completeProgress(initial, blueprint, 12, 64, 12);
        SettlementRecord completed = progress.settlement().withCompletedProject(progress.project(), blueprint);
        SettlementRecord repeated = completed.withCompletedProject(progress.project(), blueprint);

        assertEquals(2, completed.worksites().size(), "frontier plus farm worksite reward");
        assertEquals(2, completed.worksites().stream()
                .filter(worksite -> worksite.type().equals("farmer"))
                .findFirst().orElseThrow().capacity(), "farm worker capacity");
        assertTrue(repeated == completed, "duplicate completion ignored");
    }

    private static void incompleteAndDetachedProjectsCannotMintRewards() {
        SettlementRecord initial = SettlementRecord.create("minecraft:overworld", 0, 64, 0);
        KingdomBaseBlueprint blueprint = KingdomBaseBlueprint.barracks();
        BuildProject project = new BuildProject(
                UUID.randomUUID(), blueprint.id(), "minecraft:overworld", 16, 64, 16, 0,
                blueprint.definitionHash(), List.of(), BuildProjectState.ACTIVE, "", 0);
        SettlementRecord started = initial.withNewBuildProject(project);
        assertTrue(started.withCompletedProject(project, blueprint) == started,
                "incomplete stored project rejected");

        Progress completedProgress = completeProgress(initial, blueprint, 20, 64, 20);
        BuildProject detached = new BuildProject(
                UUID.randomUUID(), blueprint.id(), "minecraft:overworld", 20, 64, 20, 0,
                blueprint.definitionHash(), allPlacements(blueprint), BuildProjectState.ACTIVE, "", 0);
        assertTrue(completedProgress.settlement().withCompletedProject(detached, blueprint)
                        == completedProgress.settlement(),
                "detached project rejected");
    }

    private static void staleAndMutatedProjectTransitionsAreRejected() {
        SettlementRecord initial = SettlementRecord.create("minecraft:overworld", 0, 64, 0);
        KingdomBaseBlueprint blueprint = KingdomBaseBlueprint.barracks();
        BuildProject project = new BuildProject(
                UUID.randomUUID(), blueprint.id(), "minecraft:overworld", 24, 64, 24, 0,
                blueprint.definitionHash(), List.of(), BuildProjectState.ACTIVE, "", 0);
        SettlementRecord started = initial.withNewBuildProject(project);

        BuildProject mutated = new BuildProject(
                project.id(), blueprint.id(), project.dimensionId(), project.originX(), project.originY(),
                project.originZ(), project.rotationSteps(), "tampered", List.of(), BuildProjectState.ACTIVE, "", 1);
        assertTrue(started.replaceBuildProject(mutated) == started, "definition mutation rejected");

        BuildProject firstProgress = project.markCompleted(0);
        SettlementRecord advanced = started.replaceBuildProject(firstProgress);
        BuildProject staleAlternative = project.markCompleted(1);
        assertTrue(advanced.replaceBuildProject(staleAlternative) == advanced, "stale revision rejected");

        BuildProject unknown = new BuildProject(
                UUID.randomUUID(), blueprint.id(), project.dimensionId(), project.originX(), project.originY(),
                project.originZ(), project.rotationSteps(), blueprint.definitionHash(), List.of(),
                BuildProjectState.ACTIVE, "", 0);
        assertTrue(started.replaceBuildProject(unknown) == started, "unknown project replacement rejected");
    }

    private static void storageOnlyRewardsRegisterDeterministicEndpoints() {
        KingdomBaseBlueprint blueprint = new KingdomBaseBlueprint(
                "storage_cache",
                "Storage Cache",
                BlueprintAnchor.ORIGIN,
                List.of(0),
                List.of(new BaseBlockPlacement(0, 0, 0, "minecraft:chest", "minecraft:chest")),
                0,
                27,
                "",
                0,
                0);
        SettlementRecord initial = SettlementRecord.create("minecraft:overworld", 0, 64, 0);
        Progress firstProgress = completeProgress(initial, blueprint, 28, 64, 28);
        SettlementRecord completed = firstProgress.settlement()
                .withCompletedProject(firstProgress.project(), blueprint);
        WorksiteRecord storage = completed.worksites().stream()
                .filter(worksite -> worksite.type().equals("storage"))
                .findFirst().orElseThrow();
        assertEquals(27, storage.storageEndpoints().stream().mapToInt(StorageEndpoint::slots).sum(),
                "storage-only endpoint capacity");

        SettlementRecord secondInitial = SettlementRecord.create("minecraft:overworld", 0, 64, 0);
        // Worksite IDs are deterministic within one settlement/project authority path; replay is idempotent.
        SettlementRecord replayed = completed.withCompletedProject(firstProgress.project(), blueprint);
        assertTrue(replayed == completed, "storage endpoint replay idempotency");
        assertTrue(secondInitial.worksites().stream().noneMatch(worksite -> worksite.id().equals(storage.id())),
                "derived endpoint id is settlement scoped");
    }

    private static Progress completeProgress(
            SettlementRecord initial,
            KingdomBaseBlueprint blueprint,
            int x,
            int y,
            int z
    ) {
        BuildProject project = new BuildProject(
                UUID.randomUUID(), blueprint.id(), "minecraft:overworld", x, y, z, 0,
                blueprint.definitionHash(), List.of(), BuildProjectState.ACTIVE, "", 0);
        SettlementRecord settlement = initial.withNewBuildProject(project);
        for (int placement = 0; placement < blueprint.placements().size(); placement++) {
            BuildProject next = project.markCompleted(placement);
            SettlementRecord advanced = settlement.replaceBuildProject(next);
            if (advanced == settlement) {
                throw new AssertionError("project progress transition " + placement + " was rejected");
            }
            settlement = advanced;
            project = next;
        }
        return new Progress(settlement, project);
    }

    private static List<Integer> allPlacements(KingdomBaseBlueprint blueprint) {
        return java.util.stream.IntStream.range(0, blueprint.placements().size()).boxed().toList();
    }

    private record Progress(SettlementRecord settlement, BuildProject project) {
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertTrue(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label + " expected to be true");
        }
    }
}
