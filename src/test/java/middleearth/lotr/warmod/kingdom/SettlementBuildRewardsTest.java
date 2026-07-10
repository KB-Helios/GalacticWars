package middleearth.lotr.warmod.kingdom;

import java.util.List;
import java.util.UUID;

public final class SettlementBuildRewardsTest {
    private SettlementBuildRewardsTest() {
    }

    public static void main(String[] args) {
        completedKeepUnlocksCommanderAndHousing();
        completedWorksiteAddsBoundedCapacityOnce();
        System.out.println("SettlementBuildRewardsTest passed");
    }

    private static void completedKeepUnlocksCommanderAndHousing() {
        SettlementRecord initial = SettlementRecord.create("minecraft:overworld", 0, 64, 0);
        BuildProject keep = project("starter_keep", 8, 64, 8);
        SettlementRecord completed = initial.withCompletedProject(keep, 2, "", 0);

        assertTrue(completed.hasCommanderSlot(), "starter keep commander slot");
        assertEquals(6, completed.housingCapacity(), "starter keep housing reward");
        assertEquals(1, completed.buildProjects().size(), "recorded build project");
    }

    private static void completedWorksiteAddsBoundedCapacityOnce() {
        SettlementRecord initial = SettlementRecord.create("minecraft:overworld", 0, 64, 0);
        BuildProject farm = project("farm_plot", 12, 64, 12);
        SettlementRecord completed = initial.withCompletedProject(farm, 0, "farmer", 2);
        SettlementRecord repeated = completed.withCompletedProject(farm, 0, "farmer", 2);

        assertEquals(1, completed.worksites().size(), "farm worksite reward");
        assertEquals(2, completed.worksites().getFirst().capacity(), "farm worker capacity");
        assertTrue(repeated == completed, "duplicate completion ignored");
    }

    private static BuildProject project(String blueprintId, int x, int y, int z) {
        return new BuildProject(UUID.randomUUID(), blueprintId, "minecraft:overworld",
                x, y, z, 0, List.of(0), "");
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
