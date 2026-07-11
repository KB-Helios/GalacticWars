package galacticwars.clonewars.army;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import galacticwars.clonewars.faction.FactionCatalog;
import galacticwars.clonewars.faction.FactionDefinition;
import galacticwars.clonewars.faction.FactionId;

public final class ArmyTargetSelectorTest {
    private static final FactionId REPUBLIC = FactionId.of("republic");
    private static final FactionId MANDALORIAN = FactionId.of("mandalorian");
    private static final FactionId SEPARATIST = FactionId.of("separatist");
    private static final FactionId UNKNOWN = FactionId.of("unknown");
    private static final ArmyPosition ORIGIN = new ArmyPosition(0, 64, 0);

    private ArmyTargetSelectorTest() {
    }

    public static void main(String[] args) {
        ignoresNonEnemiesAndOutOfRangeEnemies();
        prioritizesOwnerAttackerOverCloserEnemy();
        prioritizesRecruitAttackerOverHigherThreatEnemy();
        usesThreatBeforeDistanceForOrdinaryEnemies();
        usesDistanceAndUuidTieBreakers();
        rejectsInvalidInputs();

        System.out.println("ArmyTargetSelectorTest passed");
    }

    private static void ignoresNonEnemiesAndOutOfRangeEnemies() {
        Optional<ArmyTargetSelection> selection = ArmyTargetSelector.selectTarget(
                REPUBLIC,
                ORIGIN,
                List.of(
                        candidate("00000000-0000-0000-0000-000000000201", REPUBLIC, new ArmyPosition(1, 64, 0), false, false, 100),
                        candidate("00000000-0000-0000-0000-000000000202", MANDALORIAN, new ArmyPosition(2, 64, 0), false, false, 100),
                        candidate("00000000-0000-0000-0000-000000000203", UNKNOWN, new ArmyPosition(3, 64, 0), false, false, 100),
                        candidate("00000000-0000-0000-0000-000000000204", SEPARATIST, new ArmyPosition(100, 64, 0), false, false, 100)),
                catalog(),
                16);

        assertFalse(selection.isPresent(), "non-enemies and out-of-range enemies ignored");
    }

    private static void prioritizesOwnerAttackerOverCloserEnemy() {
        UUID ownerAttacker = UUID.fromString("00000000-0000-0000-0000-000000000212");

        ArmyTargetSelection selection = ArmyTargetSelector.selectTarget(
                REPUBLIC,
                ORIGIN,
                List.of(
                        candidate("00000000-0000-0000-0000-000000000211", SEPARATIST, new ArmyPosition(1, 64, 0), false, false, 100),
                        new ArmyTargetCandidate(ownerAttacker, SEPARATIST, new ArmyPosition(10, 64, 0), true, false, 0)),
                catalog(),
                16).orElseThrow();

        assertEquals(ownerAttacker, selection.targetId(), "owner attacker target");
        assertEquals("protect_owner", selection.reasonCode(), "owner attacker reason");
    }

    private static void prioritizesRecruitAttackerOverHigherThreatEnemy() {
        UUID recruitAttacker = UUID.fromString("00000000-0000-0000-0000-000000000222");

        ArmyTargetSelection selection = ArmyTargetSelector.selectTarget(
                REPUBLIC,
                ORIGIN,
                List.of(
                        candidate("00000000-0000-0000-0000-000000000221", SEPARATIST, new ArmyPosition(1, 64, 0), false, false, 100),
                        new ArmyTargetCandidate(recruitAttacker, SEPARATIST, new ArmyPosition(12, 64, 0), false, true, 0)),
                catalog(),
                16).orElseThrow();

        assertEquals(recruitAttacker, selection.targetId(), "recruit attacker target");
        assertEquals("snightsister_defense", selection.reasonCode(), "recruit attacker reason");
    }

    private static void usesThreatBeforeDistanceForOrdinaryEnemies() {
        UUID highThreat = UUID.fromString("00000000-0000-0000-0000-000000000232");

        ArmyTargetSelection selection = ArmyTargetSelector.selectTarget(
                REPUBLIC,
                ORIGIN,
                List.of(
                        candidate("00000000-0000-0000-0000-000000000231", SEPARATIST, new ArmyPosition(1, 64, 0), false, false, 10),
                        new ArmyTargetCandidate(highThreat, SEPARATIST, new ArmyPosition(12, 64, 0), false, false, 100)),
                catalog(),
                16).orElseThrow();

        assertEquals(highThreat, selection.targetId(), "high threat target");
        assertEquals("hostile_threat", selection.reasonCode(), "ordinary hostile reason");
    }

    private static void usesDistanceAndUuidTieBreakers() {
        UUID closer = UUID.fromString("00000000-0000-0000-0000-000000000242");
        UUID lowerUuid = UUID.fromString("00000000-0000-0000-0000-000000000243");

        ArmyTargetSelection distanceSelection = ArmyTargetSelector.selectTarget(
                REPUBLIC,
                ORIGIN,
                List.of(
                        candidate("00000000-0000-0000-0000-000000000241", SEPARATIST, new ArmyPosition(8, 64, 0), false, false, 50),
                        new ArmyTargetCandidate(closer, SEPARATIST, new ArmyPosition(2, 64, 0), false, false, 50)),
                catalog(),
                16).orElseThrow();

        ArmyTargetSelection uuidSelection = ArmyTargetSelector.selectTarget(
                REPUBLIC,
                ORIGIN,
                List.of(
                        candidate("00000000-0000-0000-0000-000000000244", SEPARATIST, new ArmyPosition(4, 64, 0), false, false, 50),
                        new ArmyTargetCandidate(lowerUuid, SEPARATIST, new ArmyPosition(4, 64, 0), false, false, 50)),
                catalog(),
                16).orElseThrow();

        assertEquals(closer, distanceSelection.targetId(), "closer tie target");
        assertEquals(lowerUuid, uuidSelection.targetId(), "uuid tie target");
    }

    private static void rejectsInvalidInputs() {
        assertThrows(NullPointerException.class,
                () -> new ArmyTargetCandidate(null, SEPARATIST, ORIGIN, false, false, 0),
                "null candidate id");
        assertThrows(IllegalArgumentException.class,
                () -> new ArmyTargetCandidate(UUID.randomUUID(), SEPARATIST, ORIGIN, false, false, -1),
                "negative threat");
        assertThrows(IllegalArgumentException.class,
                () -> new ArmyTargetCandidate(UUID.randomUUID(), SEPARATIST, ORIGIN, false, false, 101),
                "threat above range");
        assertThrows(IllegalArgumentException.class,
                () -> new ArmyTargetSelection(UUID.randomUUID(), ORIGIN, " ", 1),
                "blank reason");
        assertThrows(NullPointerException.class,
                () -> ArmyTargetSelector.selectTarget(null, ORIGIN, List.of(), catalog(), 16),
                "null own faction");
        assertThrows(IllegalArgumentException.class,
                () -> ArmyTargetSelector.selectTarget(REPUBLIC, ORIGIN, List.of(), catalog(), -1),
                "negative max range");
    }

    private static ArmyTargetCandidate candidate(
            String entityId,
            FactionId factionId,
            ArmyPosition position,
            boolean attackingOwner,
            boolean attackingRecruit,
            int threat
    ) {
        return new ArmyTargetCandidate(UUID.fromString(entityId), factionId, position, attackingOwner, attackingRecruit, threat);
    }

    private static FactionCatalog catalog() {
        FactionDefinition republic = new FactionDefinition(
                REPUBLIC,
                "Republic",
                25,
                10,
                12,
                Set.of(MANDALORIAN),
                Set.of(SEPARATIST));
        FactionDefinition mandalorian = new FactionDefinition(
                MANDALORIAN,
                "Mandalorian",
                20,
                8,
                10,
                Set.of(REPUBLIC),
                Set.of(SEPARATIST));
        FactionDefinition separatist = new FactionDefinition(
                SEPARATIST,
                "Separatist",
                30,
                15,
                16,
                Set.of(),
                Set.of(REPUBLIC, MANDALORIAN));

        return new FactionCatalog(Map.of(
                REPUBLIC, republic,
                MANDALORIAN, mandalorian,
                SEPARATIST, separatist));
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertFalse(boolean condition, String label) {
        if (condition) {
            throw new AssertionError(label + " expected to be false");
        }
    }

    private static <T extends Throwable> void assertThrows(Class<T> expectedType, ThrowingRunnable runnable, String label) {
        try {
            runnable.run();
        } catch (Throwable throwable) {
            if (expectedType.isInstance(throwable)) {
                return;
            }
            throw new AssertionError(label + " threw " + throwable.getClass().getName() + " instead of "
                    + expectedType.getName(), throwable);
        }

        throw new AssertionError(label + " did not throw " + expectedType.getName());
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run();
    }
}
