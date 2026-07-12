package galacticwars.clonewars.army;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import galacticwars.clonewars.recruitment.RecruitDuty;

public final class VirtualArmyMovementPlannerTest {
    private VirtualArmyMovementPlannerTest() {
    }

    public static void main(String[] args) {
        speedIsBoundedAndDeterministic();
        movementOrdersAdvanceOnlyTowardSameDimensionTargets();
        unavailableAndNonMovementOrdersPauseExplicitly();
        advancingDoesNotMutateSnapshotsOrdersOrVitals();
        virtualPatrolAdvancesToTheNextWaypoint();
        System.out.println("VirtualArmyMovementPlannerTest passed");
    }

    private static void speedIsBoundedAndDeterministic() {
        assertDouble(1.0D, VirtualArmyMovementPlanner.blocksPerSecond(0.0D), "zero speed floor");
        assertDouble(1.0D, VirtualArmyMovementPlanner.blocksPerSecond(0.05D), "slow speed floor");
        assertDouble(2.8D, VirtualArmyMovementPlanner.blocksPerSecond(0.28D), "normal speed");
        assertDouble(4.0D, VirtualArmyMovementPlanner.blocksPerSecond(1.0D), "fast speed ceiling");
        assertThrows(() -> VirtualArmyMovementPlanner.blocksPerSecond(Double.NaN), "NaN speed");
        assertThrows(() -> VirtualArmyMovementPlanner.blocksPerSecond(-0.1D), "negative speed");
    }

    private static void movementOrdersAdvanceOnlyTowardSameDimensionTargets() {
        ArmyLocation anchor = location("minecraft:overworld", 0.0D, 64.0D, 0.0D);
        ArmyLocation destination = location("minecraft:overworld", 10.0D, 64.0D, 0.0D);
        ArmyGroupOrder move = new ArmyGroupOrder(
                ArmyCommandType.MOVE_TO_POSITION, Optional.of(destination), Optional.empty(), ArmyFormation.LINE, 2);
        VirtualArmyMovementDecision decision = VirtualArmyMovementPlanner.decide(
                move, anchor, Optional.empty(), 0.3D);
        assertDouble(3.0D, decision.anchor().x(), "three blocks per simulated second");
        assertEquals("", decision.pauseReason(), "active move reason");

        ArmyGroupOrder crossDimension = new ArmyGroupOrder(
                ArmyCommandType.MOVE_TO_POSITION,
                Optional.of(location("minecraft:the_nether", 10.0D, 64.0D, 0.0D)),
                Optional.empty(), ArmyFormation.LINE, 2);
        VirtualArmyMovementDecision paused = VirtualArmyMovementPlanner.decide(
                crossDimension, anchor, Optional.empty(), 0.3D);
        assertEquals(anchor, paused.anchor(), "cross-dimension anchor");
        assertEquals(VirtualArmyMovementPlanner.DIMENSION_MISMATCH, paused.pauseReason(),
                "cross-dimension pause reason");
    }

    private static void unavailableAndNonMovementOrdersPauseExplicitly() {
        ArmyLocation anchor = location("minecraft:overworld", 1.0D, 64.0D, 1.0D);
        ArmyGroupOrder follow = ArmyGroupOrder.follow(ArmyFormation.COLUMN);
        assertEquals(VirtualArmyMovementPlanner.OWNER_UNAVAILABLE,
                VirtualArmyMovementPlanner.decide(follow, anchor, Optional.empty(), 0.28D).pauseReason(),
                "offline owner pause");

        ArmyGroupOrder hold = new ArmyGroupOrder(
                ArmyCommandType.HOLD_POSITION, Optional.of(anchor), Optional.empty(), ArmyFormation.SQUARE, 2);
        assertEquals(VirtualArmyMovementPlanner.HOLDING_POSITION,
                VirtualArmyMovementPlanner.decide(hold, anchor, Optional.empty(), 0.28D).pauseReason(),
                "hold pause");

        ArmyGroupOrder attackWithoutPosition = new ArmyGroupOrder(
                ArmyCommandType.ATTACK_TARGET, Optional.empty(), Optional.of(UUID.randomUUID()),
                ArmyFormation.WEDGE, 2);
        assertEquals(VirtualArmyMovementPlanner.TARGET_UNAVAILABLE,
                VirtualArmyMovementPlanner.decide(
                        attackWithoutPosition, anchor, Optional.empty(), 0.28D).pauseReason(),
                "attack last-known position missing");
    }

    private static void advancingDoesNotMutateSnapshotsOrdersOrVitals() {
        UUID owner = UUID.randomUUID();
        UUID kingdom = UUID.randomUUID();
        UUID commander = UUID.randomUUID();
        ArmyLocation anchor = location("minecraft:overworld", 0.0D, 64.0D, 0.0D);
        ArmyMemberSnapshot snapshot = new ArmyMemberSnapshot(
                commander,
                "galacticwars:clone_trooper",
                "galacticwars:clone_trooper",
                owner,
                kingdom,
                RecruitDuty.COMMANDER,
                17.0F,
                73,
                61,
                40,
                9L,
                new ArmySnapshotEquipment("minecraft:iron_sword", "", "", "", ""),
                "Captain");
        ArmyGroupRecord live = ArmyGroupRecord.create(
                owner, kingdom, commander, List.of(), ArmyFormation.LINE, anchor, 100L);
        ArmyGroupOrder move = new ArmyGroupOrder(
                ArmyCommandType.MOVE_TO_POSITION,
                Optional.of(location("minecraft:overworld", 8.0D, 64.0D, 0.0D)),
                Optional.empty(), ArmyFormation.LINE, 2);
        ArmyGroupRecord virtual = live.withOrder(move).withSimulation(
                new ArmyGroupSimulation(
                        ArmyGroupLifecycleState.VIRTUAL, anchor, 100L,
                        live.withOrder(move).simulation().revision() + 1L, 9L, ""),
                List.of(snapshot));
        ArmyGroupRecord advanced = VirtualArmyMovementPlanner.advance(
                virtual, Optional.empty(), 0.28D, 120L);
        assertEquals(virtual.order(), advanced.order(), "persisted order unchanged");
        assertEquals(virtual.snapshots(), advanced.snapshots(), "snapshot and vitals unchanged");
        assertEquals(ArmyGroupLifecycleState.VIRTUAL, advanced.simulation().lifecycleState(), "virtual state retained");
        assertDouble(2.8D, advanced.simulation().anchor().x(), "virtual anchor advance");
    }

    private static void virtualPatrolAdvancesToTheNextWaypoint() {
        UUID owner = UUID.randomUUID();
        UUID kingdom = UUID.randomUUID();
        UUID commander = UUID.randomUUID();
        ArmyLocation first = location("minecraft:overworld", 0.0D, 64.0D, 0.0D);
        ArmyLocation second = location("minecraft:overworld", 12.0D, 64.0D, 0.0D);
        ArmyGroupOrder patrol = new ArmyGroupOrder(
                ArmyCommandType.PATROL_ROUTE,
                Optional.of(first),
                Optional.empty(),
                ArmyFormation.LINE,
                2);
        ArmyGroupRecord group = ArmyGroupRecord.create(
                        owner, kingdom, commander, List.of(), ArmyFormation.LINE, first, 100L)
                .withPatrolRoute(List.of(first, second))
                .withOrder(patrol)
                .withSimulation(
                        new ArmyGroupSimulation(
                                ArmyGroupLifecycleState.VIRTUAL,
                                first,
                                100L,
                                2L,
                                0L,
                                ""),
                        List.of());

        ArmyGroupRecord advanced = VirtualArmyMovementPlanner.advance(
                group, Optional.empty(), 0.28D, 120L);

        assertEquals(ArmyCommandType.PATROL_ROUTE, advanced.order().type(), "virtual patrol order");
        assertEquals(second, advanced.order().targetPosition().orElseThrow(), "next virtual patrol waypoint");
    }

    private static ArmyLocation location(String dimension, double x, double y, double z) {
        return new ArmyLocation(dimension, x, y, z);
    }

    private static void assertThrows(Runnable action, String label) {
        try {
            action.run();
            throw new AssertionError(label + " expected an exception");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void assertDouble(double expected, double actual, String label) {
        if (Math.abs(expected - actual) > 0.000001D) {
            throw new AssertionError(label + " expected " + expected + " but was " + actual);
        }
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
