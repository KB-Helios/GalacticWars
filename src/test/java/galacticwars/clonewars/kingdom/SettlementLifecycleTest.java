package galacticwars.clonewars.kingdom;

import java.util.UUID;

public final class SettlementLifecycleTest {
    private SettlementLifecycleTest() {
    }

    public static void main(String[] args) {
        recruitRemovalReleasesHousingAndCommanderSlot();
        hallRelocationPreservesProgress();
        System.out.println("SettlementLifecycleTest passed");
    }

    private static void recruitRemovalReleasesHousingAndCommanderSlot() {
        UUID recruitId = UUID.randomUUID();
        SettlementRecord settlement = SettlementRecord.create("minecraft:overworld", 1, 2, 3)
                .withRecruit(recruitId)
                .withCommander(recruitId);
        SettlementRecord removed = settlement.withoutRecruit(recruitId);
        assertTrue(!removed.containsRecruit(recruitId), "removed recruit");
        assertTrue(removed.commanderId().isEmpty(), "released commander");
        assertTrue(removed.hasHousingSpace(), "released housing");
        assertTrue(removed.revision() == settlement.revision() + 1, "removal revision");
        assertTrue(removed.withoutRecruit(recruitId) == removed, "idempotent removal");
    }

    private static void hallRelocationPreservesProgress() {
        UUID recruitId = UUID.randomUUID();
        SettlementRecord settlement = SettlementRecord.create("minecraft:overworld", 1, 2, 3)
                .withRecruit(recruitId);
        SettlementRecord relocated = settlement.withHallLocation("minecraft:the_nether", 10, 20, 30);
        assertTrue(relocated.id().equals(settlement.id()), "settlement identity");
        assertTrue(relocated.containsRecruit(recruitId), "recruit preservation");
        assertTrue(relocated.dimensionId().equals("minecraft:the_nether"), "dimension relocation");
        assertTrue(relocated.hallX() == 10 && relocated.hallY() == 20 && relocated.hallZ() == 30,
                "Hall coordinates");
    }

    private static void assertTrue(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label);
        }
    }
}
