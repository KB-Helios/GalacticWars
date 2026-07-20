package galacticwars.clonewars.army;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ArmyGroupRecordLegacyFieldsTest {
    private static final UUID GROUP_ID = UUID.fromString("00000000-0000-0000-0000-000000000021");
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000022");
    private static final UUID KINGDOM_ID = UUID.fromString("00000000-0000-0000-0000-000000000023");
    private static final UUID COMMANDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000024");
    private static final UUID FIRST_MEMBER = UUID.fromString("00000000-0000-0000-0000-000000000025");
    private static final UUID SECOND_MEMBER = UUID.fromString("00000000-0000-0000-0000-000000000026");
    private static final UUID THIRD_MEMBER = UUID.fromString("00000000-0000-0000-0000-000000000027");
    private static final UUID NEW_MEMBER = UUID.fromString("00000000-0000-0000-0000-000000000028");
    private static final ArmyLocation RALLY = new ArmyLocation("minecraft:overworld", 4, 64, 8);
    private static final ArmyLocation PATROL_STOP = new ArmyLocation("minecraft:overworld", 24, 64, 8);

    private ArmyGroupRecordLegacyFieldsTest() {
    }

    public static void main(String[] args) {
        legacyFieldsRemainAbsentWhileEffectiveViewsPreserveCurrentBehavior();
        membershipChangesMaterializeOnlyReconciledFormationSlots();

        System.out.println("ArmyGroupRecordLegacyFieldsTest passed");
    }

    private static void legacyFieldsRemainAbsentWhileEffectiveViewsPreserveCurrentBehavior() {
        ArmyGroupRecord legacy = legacyGroup();

        assertTrue(legacy.formationSlotAssignments().isEmpty(), "legacy slots remain absent");
        assertTrue(legacy.patrolPlan().isEmpty(), "legacy patrol plan remains absent");
        assertTrue(legacy.tactics().isEmpty(), "legacy tactics remain absent");
        assertEquals(List.of(
                new ArmyFormationSlotAssignment(FIRST_MEMBER, 0),
                new ArmyFormationSlotAssignment(SECOND_MEMBER, 1),
                new ArmyFormationSlotAssignment(THIRD_MEMBER, 2)),
                legacy.effectiveFormationSlotAssignments(), "legacy deterministic slots");

        ArmyPatrolPlan derivedPatrol = legacy.effectivePatrolPlan().orElseThrow();
        assertEquals(List.of(RALLY, PATROL_STOP), derivedPatrol.locations(), "legacy patrol locations");
        assertEquals(ArmyPatrolMode.LOOP, derivedPatrol.mode(), "legacy patrol mode");
        assertEquals(ArmyPatrolState.start(), derivedPatrol.state(), "legacy patrol state");
        assertEquals(ArmyGroupTactics.DEFAULT, legacy.effectiveTactics(), "legacy tactics default");

        assertTrue(legacy.formationSlotAssignments().isEmpty(), "effective slots do not migrate save data");
        assertTrue(legacy.patrolPlan().isEmpty(), "effective patrol does not migrate save data");
        assertTrue(legacy.tactics().isEmpty(), "effective tactics do not migrate save data");
    }

    private static void membershipChangesMaterializeOnlyReconciledFormationSlots() {
        ArmyGroupRecord updated = legacyGroup().withMembers(List.of(THIRD_MEMBER, NEW_MEMBER, SECOND_MEMBER));

        assertEquals(List.of(
                new ArmyFormationSlotAssignment(NEW_MEMBER, 0),
                new ArmyFormationSlotAssignment(SECOND_MEMBER, 1),
                new ArmyFormationSlotAssignment(THIRD_MEMBER, 2)),
                updated.formationSlotAssignments().orElseThrow(), "reconciled persisted slots");
        assertTrue(updated.patrolPlan().isEmpty(), "membership change does not force patrol migration");
        assertTrue(updated.tactics().isEmpty(), "membership change does not force tactics migration");
        assertEquals(1L, updated.simulation().revision(), "membership revision");
    }

    private static ArmyGroupRecord legacyGroup() {
        return new ArmyGroupRecord(
                GROUP_ID,
                OWNER_ID,
                KINGDOM_ID,
                Optional.of(COMMANDER_ID),
                List.of(THIRD_MEMBER, FIRST_MEMBER, SECOND_MEMBER),
                ArmyGroupOrder.follow(ArmyFormation.LINE),
                new ArmyGroupSimulation(ArmyGroupLifecycleState.LIVE, RALLY, 100L, 0L, 0L, ""),
                List.of(),
                "Legacy 501st",
                Optional.of(RALLY),
                List.of(RALLY, PATROL_STOP),
                Optional.empty(),
                12);
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
