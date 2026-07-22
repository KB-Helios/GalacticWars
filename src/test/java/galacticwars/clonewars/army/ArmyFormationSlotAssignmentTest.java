package galacticwars.clonewars.army;

import java.util.List;
import java.util.UUID;

public final class ArmyFormationSlotAssignmentTest {
    private static final UUID FIRST = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID SECOND = UUID.fromString("00000000-0000-0000-0000-000000000012");
    private static final UUID THIRD = UUID.fromString("00000000-0000-0000-0000-000000000013");
    private static final UUID FOURTH = UUID.fromString("00000000-0000-0000-0000-000000000014");

    private ArmyFormationSlotAssignmentTest() {
    }

    public static void main(String[] args) {
        assignsSlotsByUuidRegardlessOfIncomingOrder();
        reconciliationKeepsValidMemberSlotsAndFillsVacanciesDeterministically();
        reconciliationDropsStaleAndOutOfRangeBindings();
        roleAwareReinforcementsFillTheirNearestTacticalDepth();

        System.out.println("ArmyFormationSlotAssignmentTest passed");
    }

    private static void roleAwareReinforcementsFillTheirNearestTacticalDepth() {
        List<ArmyFormationSlotAssignment> reconciled = ArmyFormationSlotAssignment.reconcileRoleAware(
                List.of(FIRST, SECOND, THIRD, FOURTH),
                java.util.Map.of(
                        FIRST, ArmyFormationRole.HEAVY,
                        SECOND, ArmyFormationRole.RANGED,
                        THIRD, ArmyFormationRole.SUPPORT,
                        FOURTH, ArmyFormationRole.FRONTLINE),
                List.of(
                        new ArmyFormationSlotAssignment(FIRST, 0),
                        new ArmyFormationSlotAssignment(THIRD, 2)));
        assertEquals(List.of(
                new ArmyFormationSlotAssignment(FIRST, 0),
                new ArmyFormationSlotAssignment(FOURTH, 1),
                new ArmyFormationSlotAssignment(THIRD, 2),
                new ArmyFormationSlotAssignment(SECOND, 3)), reconciled,
                "role-aware reinforcement depths preserve survivors");
    }

    private static void assignsSlotsByUuidRegardlessOfIncomingOrder() {
        List<ArmyFormationSlotAssignment> assignments = ArmyFormationSlotAssignment.assignDeterministically(
                List.of(THIRD, FIRST, SECOND, FIRST));

        assertEquals(List.of(
                new ArmyFormationSlotAssignment(FIRST, 0),
                new ArmyFormationSlotAssignment(SECOND, 1),
                new ArmyFormationSlotAssignment(THIRD, 2)), assignments,
                "deterministic UUID slots");
    }

    private static void reconciliationKeepsValidMemberSlotsAndFillsVacanciesDeterministically() {
        List<ArmyFormationSlotAssignment> reconciled = ArmyFormationSlotAssignment.reconcile(
                List.of(FOURTH, THIRD, SECOND),
                List.of(
                        new ArmyFormationSlotAssignment(FIRST, 1),
                        new ArmyFormationSlotAssignment(SECOND, 0),
                        new ArmyFormationSlotAssignment(THIRD, 2)));

        assertEquals(List.of(
                new ArmyFormationSlotAssignment(SECOND, 0),
                new ArmyFormationSlotAssignment(FOURTH, 1),
                new ArmyFormationSlotAssignment(THIRD, 2)), reconciled,
                "retained slots and deterministic vacancy fill");
    }

    private static void reconciliationDropsStaleAndOutOfRangeBindings() {
        UUID stale = UUID.fromString("00000000-0000-0000-0000-000000000099");
        List<ArmyFormationSlotAssignment> reconciled = ArmyFormationSlotAssignment.reconcile(
                List.of(THIRD, SECOND),
                List.of(
                        new ArmyFormationSlotAssignment(SECOND, 4),
                        new ArmyFormationSlotAssignment(THIRD, 1),
                        new ArmyFormationSlotAssignment(stale, 0)));

        assertEquals(List.of(
                new ArmyFormationSlotAssignment(SECOND, 0),
                new ArmyFormationSlotAssignment(THIRD, 1)), reconciled,
                "stale and out-of-range bindings");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
