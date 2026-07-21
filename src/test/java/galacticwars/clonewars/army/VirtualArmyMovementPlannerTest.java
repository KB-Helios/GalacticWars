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
        virtualPatrolWaitsAdvanceByElapsedVirtualTicks();
        pausedVirtualPatrolDoesNotConsumeDwellUntilResume();
        stoppedVirtualPatrolRemainsStable();
        virtualPatrolRetainsFormationSlotsAndTacticsForRematerialization();
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
                ArmySnapshotEquipment.empty(),
                List.of(),
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

    private static void virtualPatrolWaitsAdvanceByElapsedVirtualTicks() {
        UUID owner = UUID.randomUUID();
        UUID kingdom = UUID.randomUUID();
        UUID commander = UUID.randomUUID();
        ArmyLocation first = location("minecraft:overworld", 0.0D, 64.0D, 0.0D);
        ArmyLocation second = location("minecraft:overworld", 12.0D, 64.0D, 0.0D);
        ArmyPatrolPlan waitingPlan = new ArmyPatrolPlan(
                List.of(new ArmyPatrolWaypoint(first, 40), ArmyPatrolWaypoint.immediate(second)),
                ArmyPatrolMode.LOOP,
                new ArmyPatrolState(1, 1, 40),
                ArmyPatrolPlan.DEFAULT_ARRIVAL_DISTANCE,
                ArmyPatrolPlan.DEFAULT_MOVEMENT_SPEED,
                ArmyPatrolEnemyPolicy.ENGAGE_HOSTILES);
        ArmyGroupOrder patrol = new ArmyGroupOrder(
                ArmyCommandType.PATROL_ROUTE, Optional.of(first), Optional.empty(), ArmyFormation.LINE, 2);
        ArmyGroupRecord group = ArmyGroupRecord.create(
                        owner, kingdom, commander, List.of(), ArmyFormation.LINE, first, 100L)
                .withPatrolPlanAndOrder(waitingPlan, patrol)
                .withSimulation(new ArmyGroupSimulation(
                        ArmyGroupLifecycleState.VIRTUAL, first, 100L, 3L, 0L,
                        VirtualArmyMovementPlanner.DESTINATION_REACHED), List.of());

        ArmyGroupRecord halfway = VirtualArmyMovementPlanner.advance(group, Optional.empty(), 0.28D, 120L);
        assertEquals(new ArmyPatrolState(1, 1, 20), halfway.effectivePatrolPlan().orElseThrow().state(),
                "one virtual interval consumes twenty persisted wait ticks");
        assertEquals(first, halfway.order().targetPosition().orElseThrow(), "wait keeps the arrived waypoint target");

        ArmyGroupRecord released = VirtualArmyMovementPlanner.advance(halfway, Optional.empty(), 0.28D, 140L);
        assertEquals(new ArmyPatrolState(1, 1, 0), released.effectivePatrolPlan().orElseThrow().state(),
                "two virtual intervals consume a forty tick waypoint wait");
        assertEquals(second, released.order().targetPosition().orElseThrow(),
                "expired wait immediately publishes the next waypoint");
    }

    private static void pausedVirtualPatrolDoesNotConsumeDwellUntilResume() {
        UUID owner = UUID.randomUUID();
        UUID kingdom = UUID.randomUUID();
        UUID commander = UUID.randomUUID();
        ArmyLocation first = location("minecraft:overworld", 0.0D, 64.0D, 0.0D);
        ArmyLocation second = location("minecraft:overworld", 12.0D, 64.0D, 0.0D);
        ArmyPatrolPlan pausedPlan = new ArmyPatrolPlan(
                List.of(new ArmyPatrolWaypoint(first, 40), ArmyPatrolWaypoint.immediate(second)),
                ArmyPatrolMode.LOOP,
                new ArmyPatrolState(1, 1, 40, ArmyPatrolStatus.PAUSED),
                ArmyPatrolPlan.DEFAULT_ARRIVAL_DISTANCE,
                ArmyPatrolPlan.DEFAULT_MOVEMENT_SPEED,
                ArmyPatrolEnemyPolicy.ENGAGE_HOSTILES);
        ArmyGroupOrder patrol = new ArmyGroupOrder(
                ArmyCommandType.PATROL_ROUTE, Optional.of(first), Optional.empty(), ArmyFormation.LINE, 2);
        ArmyGroupRecord group = ArmyGroupRecord.create(
                        owner, kingdom, commander, List.of(), ArmyFormation.LINE, first, 100L)
                .withPatrolPlanAndOrder(pausedPlan, patrol)
                .withSimulation(new ArmyGroupSimulation(
                        ArmyGroupLifecycleState.VIRTUAL, first, 100L, 3L, 0L, "patrol_paused"), List.of());

        ArmyGroupRecord pausedAt120 = VirtualArmyMovementPlanner.advance(group, Optional.empty(), 0.28D, 120L);
        ArmyGroupRecord pausedAt140 = VirtualArmyMovementPlanner.advance(pausedAt120, Optional.empty(), 0.28D, 140L);
        ArmyGroupRecord pausedAt160 = VirtualArmyMovementPlanner.advance(pausedAt140, Optional.empty(), 0.28D, 160L);
        assertEquals(pausedPlan.state(), pausedAt160.effectivePatrolPlan().orElseThrow().state(),
                "pause preserves waypoint dwell state");
        assertEquals(160L, pausedAt160.simulation().lastSimulationGameTime(),
                "pause advances the virtual simulation timestamp");

        ArmyPatrolPlan resumedPlan = pausedAt160.effectivePatrolPlan().orElseThrow().resume();
        ArmyGroupRecord resumed = pausedAt160.withPatrolPlanAndOrder(resumedPlan, pausedAt160.order());
        ArmyGroupRecord afterOneActiveInterval = VirtualArmyMovementPlanner.advance(
                resumed, Optional.empty(), 0.28D, 180L);
        assertEquals(new ArmyPatrolState(1, 1, 20),
                afterOneActiveInterval.effectivePatrolPlan().orElseThrow().state(),
                "only post-resume ticks decrement waypoint dwell");
        assertEquals(first, afterOneActiveInterval.order().targetPosition().orElseThrow(),
                "partially elapsed post-resume dwell keeps the arrived waypoint target");

        ArmyGroupRecord released = VirtualArmyMovementPlanner.advance(
                afterOneActiveInterval, Optional.empty(), 0.28D, 200L);
        assertEquals(second, released.order().targetPosition().orElseThrow(),
                "a second active interval releases the remaining dwell");
    }

    private static void stoppedVirtualPatrolRemainsStable() {
        UUID owner = UUID.randomUUID();
        UUID kingdom = UUID.randomUUID();
        UUID commander = UUID.randomUUID();
        ArmyLocation first = location("minecraft:overworld", 0.0D, 64.0D, 0.0D);
        ArmyLocation second = location("minecraft:overworld", 12.0D, 64.0D, 0.0D);
        ArmyPatrolPlan stoppedPlan = new ArmyPatrolPlan(
                List.of(ArmyPatrolWaypoint.immediate(first), ArmyPatrolWaypoint.immediate(second)),
                ArmyPatrolMode.LOOP,
                ArmyPatrolState.start().stop(),
                ArmyPatrolPlan.DEFAULT_ARRIVAL_DISTANCE,
                ArmyPatrolPlan.DEFAULT_MOVEMENT_SPEED,
                ArmyPatrolEnemyPolicy.ENGAGE_HOSTILES);
        ArmyGroupOrder patrol = new ArmyGroupOrder(
                ArmyCommandType.PATROL_ROUTE, Optional.of(first), Optional.empty(), ArmyFormation.LINE, 2);
        ArmyGroupRecord stopped = ArmyGroupRecord.create(
                        owner, kingdom, commander, List.of(), ArmyFormation.LINE, first, 100L)
                .withPatrolPlanAndOrder(stoppedPlan, patrol)
                .withSimulation(new ArmyGroupSimulation(
                        ArmyGroupLifecycleState.VIRTUAL, first, 100L, 3L, 0L, "patrol_stopped"), List.of());

        ArmyGroupRecord at120 = VirtualArmyMovementPlanner.advance(stopped, Optional.empty(), 0.28D, 120L);
        ArmyGroupRecord at140 = VirtualArmyMovementPlanner.advance(at120, Optional.empty(), 0.28D, 140L);
        assertEquals(stopped, at120, "stopped virtual patrol does not churn its persisted state");
        assertEquals(stopped, at140, "stopped virtual patrol stays stable across later intervals");
    }

    private static void virtualPatrolRetainsFormationSlotsAndTacticsForRematerialization() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000701");
        UUID kingdom = UUID.fromString("00000000-0000-0000-0000-000000000702");
        UUID commander = UUID.fromString("00000000-0000-0000-0000-000000000703");
        UUID firstMember = UUID.fromString("00000000-0000-0000-0000-000000000704");
        UUID secondMember = UUID.fromString("00000000-0000-0000-0000-000000000705");
        ArmyLocation first = location("minecraft:overworld", 0.0D, 64.0D, 0.0D);
        ArmyLocation second = location("minecraft:overworld", 12.0D, 64.0D, 0.0D);
        ArmyPatrolPlan plan = new ArmyPatrolPlan(
                List.of(ArmyPatrolWaypoint.immediate(first), ArmyPatrolWaypoint.immediate(second)),
                ArmyPatrolMode.LOOP, ArmyPatrolState.start(), 2, 1.25D,
                ArmyPatrolEnemyPolicy.IGNORE_HOSTILES);
        ArmyGroupOrder patrol = new ArmyGroupOrder(
                ArmyCommandType.PATROL_ROUTE, Optional.of(first), Optional.empty(), ArmyFormation.LINE, 2);
        ArmyGroupTactics tactics = ArmyGroupTactics.DEFAULT.withFormationYaw(90.0F);
        ArmyGroupRecord group = ArmyGroupRecord.create(
                        owner, kingdom, commander, List.of(firstMember, secondMember), ArmyFormation.LINE, first, 100L)
                .withPatrolPlanAndOrder(plan, patrol)
                .withTactics(tactics)
                .withSimulation(new ArmyGroupSimulation(
                        ArmyGroupLifecycleState.VIRTUAL, first, 100L, 3L, 1L, ""), List.of());

        ArmyPosition anchor = first.blockPosition();
        ArmyPosition firstBefore = ArmyGroupOrderPlanner.formationPositionForMember(group, firstMember, anchor);
        ArmyPosition secondBefore = ArmyGroupOrderPlanner.formationPositionForMember(group, secondMember, anchor);
        ArmyGroupRecord advanced = VirtualArmyMovementPlanner.advance(group, Optional.empty(), 0.28D, 120L);

        assertEquals(group.effectiveFormationSlotAssignments(), advanced.effectiveFormationSlotAssignments(),
                "virtual transition retains persisted slots");
        assertEquals(tactics, advanced.effectiveTactics(), "virtual transition retains tactics");
        assertEquals(1.25D, advanced.effectivePatrolPlan().orElseThrow().movementSpeed(),
                "virtual transition retains patrol speed");
        assertEquals(ArmyPatrolEnemyPolicy.IGNORE_HOSTILES,
                advanced.effectivePatrolPlan().orElseThrow().enemyPolicy(),
                "virtual transition retains patrol enemy policy");
        assertEquals(firstBefore, ArmyGroupOrderPlanner.formationPositionForMember(
                advanced, firstMember, anchor), "first member respawn slot is stable");
        assertEquals(secondBefore, ArmyGroupOrderPlanner.formationPositionForMember(
                advanced, secondMember, anchor), "second member respawn slot is stable");
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
