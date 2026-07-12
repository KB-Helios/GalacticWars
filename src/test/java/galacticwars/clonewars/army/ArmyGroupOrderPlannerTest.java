package galacticwars.clonewars.army;

import java.util.List;
import java.util.UUID;

public final class ArmyGroupOrderPlannerTest {
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final UUID GROUP_ID = UUID.fromString("00000000-0000-0000-0000-000000000302");
    private static final UUID FIRST_RECRUIT_ID = UUID.fromString("00000000-0000-0000-0000-000000000303");
    private static final UUID SECOND_RECRUIT_ID = UUID.fromString("00000000-0000-0000-0000-000000000304");
    private static final UUID THIRD_RECRUIT_ID = UUID.fromString("00000000-0000-0000-0000-000000000305");
    private static final UUID TARGET_ID = UUID.fromString("00000000-0000-0000-0000-000000000306");

    private ArmyGroupOrderPlannerTest() {
    }

    public static void main(String[] args) {
        assignsMoveOrdersToLineFormationPositions();
        assignsHoldOrdersToColumnFormationPositions();
        assignsPatrolOrdersToFormationPositions();
        assignsFollowAndProtectOrdersToOwnerRelativeFormationPositions();
        propagatesDirectGroupOrders();
        returnsImmutableEmptyAssignmentsForEmptyGroups();

        System.out.println("ArmyGroupOrderPlannerTest passed");
    }

    private static void assignsMoveOrdersToLineFormationPositions() {
        ArmyPosition anchor = new ArmyPosition(10, 64, 20);
        ArmyGroupState group = populatedGroup()
                .applyCommand(ArmyCommand.moveToPosition(OWNER_ID, GROUP_ID, anchor));

        List<ArmyGroupOrderAssignment> assignments = ArmyGroupOrderPlanner.plan(group, ArmyFormation.LINE, 2);

        assertEquals(3, assignments.size(), "move assignment count");
        assertAssignment(assignments.get(0), FIRST_RECRUIT_ID, ArmyCommandType.MOVE_TO_POSITION,
                new ArmyPosition(8, 64, 20), new FormationSlot(0, -2, 0), "move first");
        assertAssignment(assignments.get(1), SECOND_RECRUIT_ID, ArmyCommandType.MOVE_TO_POSITION,
                new ArmyPosition(10, 64, 20), new FormationSlot(1, 0, 0), "move second");
        assertAssignment(assignments.get(2), THIRD_RECRUIT_ID, ArmyCommandType.MOVE_TO_POSITION,
                new ArmyPosition(12, 64, 20), new FormationSlot(2, 2, 0), "move third");
        assertEquals("move_group_order", assignments.get(0).reasonCode(), "move reason");
    }

    private static void assignsHoldOrdersToColumnFormationPositions() {
        ArmyPosition anchor = new ArmyPosition(-4, 70, 2);
        ArmyGroupState group = populatedGroup()
                .applyCommand(ArmyCommand.holdPosition(OWNER_ID, GROUP_ID, anchor));

        List<ArmyGroupOrderAssignment> assignments = ArmyGroupOrderPlanner.plan(group, ArmyFormation.COLUMN, 3);

        assertAssignment(assignments.get(0), FIRST_RECRUIT_ID, ArmyCommandType.HOLD_POSITION,
                new ArmyPosition(-4, 70, 2), new FormationSlot(0, 0, 0), "hold first");
        assertAssignment(assignments.get(1), SECOND_RECRUIT_ID, ArmyCommandType.HOLD_POSITION,
                new ArmyPosition(-4, 70, 5), new FormationSlot(1, 0, 3), "hold second");
        assertAssignment(assignments.get(2), THIRD_RECRUIT_ID, ArmyCommandType.HOLD_POSITION,
                new ArmyPosition(-4, 70, 8), new FormationSlot(2, 0, 6), "hold third");
        assertEquals("hold_group_order", assignments.get(0).reasonCode(), "hold reason");
    }

    private static void assignsPatrolOrdersToFormationPositions() {
        ArmyPosition waypoint = new ArmyPosition(20, 64, 20);
        ArmyGroupState group = populatedGroup()
                .applyCommand(ArmyCommand.patrolRoute(OWNER_ID, GROUP_ID, waypoint));

        List<ArmyGroupOrderAssignment> assignments = ArmyGroupOrderPlanner.plan(
                group, ArmyFormation.LINE, 2);

        assertEquals(3, assignments.size(), "patrol assignment count");
        assertAssignment(assignments.get(0), FIRST_RECRUIT_ID, ArmyCommandType.MOVE_TO_POSITION,
                new ArmyPosition(18, 64, 20), new FormationSlot(0, -2, 0), "patrol first");
        assertAssignment(assignments.get(1), SECOND_RECRUIT_ID, ArmyCommandType.MOVE_TO_POSITION,
                new ArmyPosition(20, 64, 20), new FormationSlot(1, 0, 0), "patrol second");
        assertAssignment(assignments.get(2), THIRD_RECRUIT_ID, ArmyCommandType.MOVE_TO_POSITION,
                new ArmyPosition(22, 64, 20), new FormationSlot(2, 2, 0), "patrol third");
        assertEquals("move_group_order", assignments.get(0).reasonCode(), "patrol movement reason");
    }

    private static void propagatesDirectGroupOrders() {
        assertDirectOrder(ArmyCommand.attackTarget(OWNER_ID, GROUP_ID, TARGET_ID), ArmyCommandType.ATTACK_TARGET, TARGET_ID,
                "attack_group_order");
        assertDirectOrder(ArmyCommand.clearTarget(OWNER_ID, GROUP_ID), ArmyCommandType.CLEAR_TARGET, null, "clear_group_order");
    }

    private static void assignsFollowAndProtectOrdersToOwnerRelativeFormationPositions() {
        ArmyPosition ownerAnchor = new ArmyPosition(30, 65, -10);
        assertOwnerRelativeOrder(
                ArmyCommand.followOwner(OWNER_ID, GROUP_ID), ArmyCommandType.FOLLOW_OWNER, ownerAnchor);
        assertOwnerRelativeOrder(
                ArmyCommand.protectOwner(OWNER_ID, GROUP_ID), ArmyCommandType.PROTECT_OWNER, ownerAnchor);
    }

    private static void assertOwnerRelativeOrder(
            ArmyCommand groupCommand,
            ArmyCommandType expectedType,
            ArmyPosition ownerAnchor
    ) {
        ArmyGroupState group = populatedGroup().applyCommand(groupCommand);
        List<ArmyGroupOrderAssignment> assignments = ArmyGroupOrderPlanner.plan(
                group, ArmyFormation.LINE, 2, ownerAnchor);

        assertAssignment(assignments.get(0), FIRST_RECRUIT_ID, expectedType,
                new ArmyPosition(28, 65, -10), new FormationSlot(0, -2, 0), expectedType + " first");
        assertAssignment(assignments.get(1), SECOND_RECRUIT_ID, expectedType,
                new ArmyPosition(30, 65, -10), new FormationSlot(1, 0, 0), expectedType + " second");
        assertAssignment(assignments.get(2), THIRD_RECRUIT_ID, expectedType,
                new ArmyPosition(32, 65, -10), new FormationSlot(2, 2, 0), expectedType + " third");
    }

    private static void returnsImmutableEmptyAssignmentsForEmptyGroups() {
        ArmyGroupState emptyGroup = ArmyGroupState.create(GROUP_ID, OWNER_ID);
        List<ArmyGroupOrderAssignment> assignments = ArmyGroupOrderPlanner.plan(emptyGroup, ArmyFormation.LINE, 2);

        assertTrue(assignments.isEmpty(), "empty group assignments");
        assertThrows(UnsupportedOperationException.class, () -> assignments.add(null), "immutable assignments");
    }

    private static void assertDirectOrder(
            ArmyCommand groupCommand,
            ArmyCommandType expectedType,
            UUID expectedTarget,
            String expectedReason
    ) {
        ArmyGroupState group = populatedGroup().applyCommand(groupCommand);
        List<ArmyGroupOrderAssignment> assignments = ArmyGroupOrderPlanner.plan(group, ArmyFormation.WEDGE, 2);

        assertEquals(3, assignments.size(), expectedType + " assignment count");
        for (ArmyGroupOrderAssignment assignment : assignments) {
            assertEquals(expectedType, assignment.command().type(), expectedType + " command type");
            assertEquals(expectedTarget, assignment.command().targetEntityId(), expectedType + " target id");
            assertEquals(null, assignment.assignedPosition(), expectedType + " assigned position");
            assertEquals(null, assignment.formationSlot(), expectedType + " formation slot");
            assertEquals(expectedReason, assignment.reasonCode(), expectedType + " reason");
        }
    }

    private static ArmyGroupState populatedGroup() {
        return ArmyGroupState.create(GROUP_ID, OWNER_ID)
                .withRecruit(FIRST_RECRUIT_ID)
                .withRecruit(SECOND_RECRUIT_ID)
                .withRecruit(THIRD_RECRUIT_ID);
    }

    private static void assertAssignment(
            ArmyGroupOrderAssignment assignment,
            UUID expectedRecruitId,
            ArmyCommandType expectedType,
            ArmyPosition expectedPosition,
            FormationSlot expectedSlot,
            String label
    ) {
        assertEquals(expectedRecruitId, assignment.recruitId(), label + " recruit");
        assertEquals(expectedType, assignment.command().type(), label + " command type");
        if (expectedType == ArmyCommandType.MOVE_TO_POSITION || expectedType == ArmyCommandType.HOLD_POSITION) {
            assertEquals(expectedPosition, assignment.command().targetPosition(), label + " command target");
        }
        assertEquals(expectedPosition, assignment.assignedPosition(), label + " assigned position");
        assertEquals(expectedSlot, assignment.formationSlot(), label + " formation slot");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertTrue(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label + " expected to be true");
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
