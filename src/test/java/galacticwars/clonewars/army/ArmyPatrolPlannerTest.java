package galacticwars.clonewars.army;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ArmyPatrolPlannerTest {
    private static final ArmyPosition POINT_A = new ArmyPosition(0, 64, 0);
    private static final ArmyPosition POINT_B = new ArmyPosition(10, 64, 0);
    private static final ArmyPosition POINT_C = new ArmyPosition(20, 64, 0);

    private ArmyPatrolPlannerTest() {
    }

    public static void main(String[] args) {
        movesTowardActiveWaypoint();
        loopsFromLastWaypointToFirst();
        pingPongReversesAtEndWaypoint();
        waitsBeforeMovingToNextWaypoint();
        exposesSafeLoadedMovementSpeed();
        editsNamedRouteAndIndividualWaypointWaitsWithoutLosingProgress();
        derivesBoundedRetreatAnchorsAwayFromThreats();
        preservesFormationSlotsDuringTransientPatrolRetreat();
        persistedPatrolOrdersAdvanceAndLoopAtArrival();
        persistedPatrolOrdersHoldUntilArrivalAndRecoverInvalidTargets();
        rejectsInvalidInputs();

        System.out.println("ArmyPatrolPlannerTest passed");
    }

    private static void movesTowardActiveWaypoint() {
        ArmyPatrolRoute route = route(ArmyPatrolMode.LOOP, 1, 0);
        ArmyPatrolState state = new ArmyPatrolState(1, 1, 0);

        ArmyPatrolDecision decision = ArmyPatrolPlanner.advance(route, state, new ArmyPosition(2, 64, 0));

        assertEquals(POINT_B, decision.moveTarget(), "active waypoint target");
        assertEquals(state, decision.nextState(), "moving state");
        assertFalse(decision.waiting(), "moving should not wait");
        assertEquals("moving_to_waypoint", decision.reasonCode(), "moving reason");
    }

    private static void loopsFromLastWaypointToFirst() {
        ArmyPatrolRoute route = route(ArmyPatrolMode.LOOP, 0, 0);

        ArmyPatrolDecision decision = ArmyPatrolPlanner.advance(route, new ArmyPatrolState(2, 1, 0), POINT_C);

        assertEquals(POINT_A, decision.moveTarget(), "loop next target");
        assertEquals(new ArmyPatrolState(0, 1, 0), decision.nextState(), "loop next state");
        assertFalse(decision.waiting(), "loop should not wait");
        assertEquals("advanced_waypoint", decision.reasonCode(), "loop reason");
    }

    private static void pingPongReversesAtEndWaypoint() {
        ArmyPatrolRoute route = route(ArmyPatrolMode.PING_PONG, 0, 0);

        ArmyPatrolDecision decision = ArmyPatrolPlanner.advance(route, new ArmyPatrolState(2, 1, 0), POINT_C);

        assertEquals(POINT_B, decision.moveTarget(), "ping-pong next target");
        assertEquals(new ArmyPatrolState(1, -1, 0), decision.nextState(), "ping-pong reversed state");
        assertEquals("advanced_waypoint", decision.reasonCode(), "ping-pong reason");
    }

    private static void waitsBeforeMovingToNextWaypoint() {
        ArmyPatrolRoute route = route(ArmyPatrolMode.LOOP, 0, 2);

        ArmyPatrolDecision arrived = ArmyPatrolPlanner.advance(route, ArmyPatrolState.start(), POINT_A);
        ArmyPatrolDecision waitingTwo = ArmyPatrolPlanner.advance(route, arrived.nextState(), POINT_A);
        ArmyPatrolDecision waitingOne = ArmyPatrolPlanner.advance(route, waitingTwo.nextState(), POINT_A);
        ArmyPatrolDecision moving = ArmyPatrolPlanner.advance(route, waitingOne.nextState(), POINT_A);

        assertTrue(arrived.waiting(), "arrived should wait");
        assertEquals(POINT_A, arrived.moveTarget(), "arrived wait target");
        assertEquals(new ArmyPatrolState(1, 1, 2), arrived.nextState(), "arrived wait state");
        assertEquals("arrived_waiting", arrived.reasonCode(), "arrived wait reason");

        assertTrue(waitingTwo.waiting(), "wait tick two should wait");
        assertEquals(new ArmyPatrolState(1, 1, 1), waitingTwo.nextState(), "wait tick two state");
        assertEquals("waiting_at_waypoint", waitingTwo.reasonCode(), "wait tick two reason");

        assertTrue(waitingOne.waiting(), "wait tick one should wait");
        assertEquals(new ArmyPatrolState(1, 1, 0), waitingOne.nextState(), "wait tick one state");

        assertFalse(moving.waiting(), "wait complete should move");
        assertEquals(POINT_B, moving.moveTarget(), "post-wait target");
        assertEquals("moving_to_waypoint", moving.reasonCode(), "post-wait reason");
    }

    private static void exposesSafeLoadedMovementSpeed() {
        ArmyLocation first = location(POINT_A);
        ArmyLocation second = location(POINT_B);
        ArmyPatrolPlan fieldSpeed = new ArmyPatrolPlan(
                List.of(ArmyPatrolWaypoint.immediate(first), ArmyPatrolWaypoint.immediate(second)),
                ArmyPatrolMode.LOOP, ArmyPatrolState.start(), 2, 1.25D,
                ArmyPatrolEnemyPolicy.ENGAGE_HOSTILES);
        ArmyPatrolPlan extremeSpeed = new ArmyPatrolPlan(
                List.of(ArmyPatrolWaypoint.immediate(first), ArmyPatrolWaypoint.immediate(second)),
                ArmyPatrolMode.LOOP, ArmyPatrolState.start(), 2, 20.0D,
                ArmyPatrolEnemyPolicy.ENGAGE_HOSTILES);

        assertEquals(1.25F, fieldSpeed.loadedMovementSpeed(), "field speed reaches loaded pathing");
        assertEquals(2.0F, extremeSpeed.loadedMovementSpeed(), "edited speed is bounded for loaded pathing");
    }

    private static void editsNamedRouteAndIndividualWaypointWaitsWithoutLosingProgress() {
        ArmyLocation first = location(POINT_A);
        ArmyLocation second = location(POINT_B);
        ArmyPatrolState progress = new ArmyPatrolState(1, -1, 40, ArmyPatrolStatus.PAUSED);
        ArmyPatrolPlan plan = new ArmyPatrolPlan(
                List.of(new ArmyPatrolWaypoint(first, 0), new ArmyPatrolWaypoint(second, 20)),
                ArmyPatrolMode.PING_PONG,
                progress,
                3,
                1.25D,
                ArmyPatrolEnemyPolicy.RETREAT_FROM_HOSTILES,
                "Landing Pad Sweep");

        ArmyPatrolPlan updated = plan.withName("Hangar Sweep").withWaypointWaitTicks(1, 180);

        assertEquals("Hangar Sweep", updated.name(), "route name edit");
        assertEquals(0, updated.waypoints().getFirst().waitTicks(), "first waypoint wait retained");
        assertEquals(180, updated.waypoints().get(1).waitTicks(), "selected waypoint wait updated");
        assertEquals(progress, updated.state(), "editing route metadata preserves patrol progress");
        assertEquals(plan.mode(), updated.mode(), "editing route metadata preserves patrol mode");
        assertEquals(plan.movementSpeed(), updated.movementSpeed(), "editing route metadata preserves speed");
        assertEquals(plan.enemyPolicy(), updated.enemyPolicy(), "editing route metadata preserves enemy policy");
        assertThrows(IllegalArgumentException.class,
                () -> plan.withWaypointWaitTicks(2, 0), "out-of-range waypoint edit");
        assertThrows(IllegalArgumentException.class,
                () -> plan.withName(" "), "blank route name");
    }

    private static void derivesBoundedRetreatAnchorsAwayFromThreats() {
        ArmyPosition anchor = new ArmyPosition(10, 64, 10);
        ArmyPosition threat = new ArmyPosition(14, 64, 10);

        ArmyPosition retreat = ArmyPatrolRetreatPlanner.retreatAnchor(anchor, threat, 0.0F);

        assertEquals(new ArmyPosition(-2, 64, 10), retreat, "retreat moves away from threat");
        assertTrue(horizontalDistanceSquared(retreat, threat) > horizontalDistanceSquared(anchor, threat),
                "retreat increases threat separation");
        assertTrue(horizontalDistanceSquared(retreat, anchor)
                        <= ArmyPatrolRetreatPlanner.RETREAT_STEP_BLOCKS
                                * ArmyPatrolRetreatPlanner.RETREAT_STEP_BLOCKS,
                "retreat step is bounded");

        ArmyPosition overlappingThreat = ArmyPatrolRetreatPlanner.retreatAnchor(anchor, anchor, 90.0F);
        assertEquals(new ArmyPosition(22, 64, 10), overlappingThreat,
                "overlapping threat uses formation-facing fallback");
    }

    private static void preservesFormationSlotsDuringTransientPatrolRetreat() {
        UUID ownerId = new UUID(11L, 1L);
        UUID kingdomId = new UUID(11L, 2L);
        UUID commanderId = new UUID(11L, 3L);
        UUID leftMemberId = new UUID(11L, 4L);
        UUID rightMemberId = new UUID(11L, 5L);
        ArmyLocation first = location(POINT_A);
        ArmyLocation second = location(POINT_B);
        ArmyGroupRecord group = new ArmyGroupRecord(
                new UUID(11L, 6L), ownerId, kingdomId, Optional.of(commanderId),
                List.of(leftMemberId, rightMemberId),
                new ArmyGroupOrder(ArmyCommandType.PATROL_ROUTE, Optional.of(first), Optional.empty(),
                        ArmyFormation.LINE, 2),
                new ArmyGroupSimulation(ArmyGroupLifecycleState.LIVE, first, 0L, 0L, 0L, ""),
                List.of(), "Retreat patrol", Optional.of(first), List.of(first, second), Optional.empty(), 0,
                Optional.of(List.of(
                        new ArmyFormationSlotAssignment(leftMemberId, 0),
                        new ArmyFormationSlotAssignment(rightMemberId, 1))),
                Optional.empty(), Optional.of(ArmyGroupTactics.DEFAULT.withFormationYaw(90.0F)));
        ArmyPosition anchor = first.blockPosition();
        ArmyPosition threat = new ArmyPosition(4, 64, 0);

        assertEquals(new ArmyPosition(-12, 64, 0), ArmyPatrolRetreatPlanner.retreatPosition(
                group, commanderId, anchor, threat), "commander takes shared retreat anchor");
        assertEquals(new ArmyPosition(-12, 64, -1), ArmyPatrolRetreatPlanner.retreatPosition(
                group, leftMemberId, anchor, threat), "left member keeps slot around retreat anchor");
        assertEquals(new ArmyPosition(-12, 64, 1), ArmyPatrolRetreatPlanner.retreatPosition(
                group, rightMemberId, anchor, threat), "right member keeps slot around retreat anchor");
        assertEquals(ArmyCommandType.PATROL_ROUTE, group.order().type(),
                "transient retreat does not replace patrol order");
    }

    private static void persistedPatrolOrdersAdvanceAndLoopAtArrival() {
        ArmyLocation first = location(POINT_A);
        ArmyLocation second = location(POINT_B);
        ArmyGroupRecord group = patrolGroup(first, second, first);

        ArmyGroupOrder secondOrder = ArmyPatrolOrderPlanner.nextOrder(group, POINT_A);
        assertEquals(second, secondOrder.targetPosition().orElseThrow(), "persisted next waypoint");

        ArmyGroupOrder loopedOrder = ArmyPatrolOrderPlanner.nextOrder(
                group.withOrder(secondOrder), POINT_B);
        assertEquals(first, loopedOrder.targetPosition().orElseThrow(), "persisted loop waypoint");
    }

    private static void persistedPatrolOrdersHoldUntilArrivalAndRecoverInvalidTargets() {
        ArmyLocation first = location(POINT_A);
        ArmyLocation second = location(POINT_B);
        ArmyGroupRecord group = patrolGroup(first, second, first);

        assertEquals(group.order(), ArmyPatrolOrderPlanner.nextOrder(
                group, new ArmyPosition(5, 64, 0)), "persisted order before arrival");

        ArmyLocation invalid = location(POINT_C);
        ArmyGroupOrder invalidOrder = new ArmyGroupOrder(
                ArmyCommandType.PATROL_ROUTE, Optional.of(invalid), Optional.empty(), ArmyFormation.LINE, 2);
        ArmyGroupOrder recovered = ArmyPatrolOrderPlanner.nextOrder(group.withOrder(invalidOrder), POINT_C);
        assertEquals(first, recovered.targetPosition().orElseThrow(), "invalid target recovery");
    }

    private static ArmyGroupRecord patrolGroup(
            ArmyLocation first,
            ArmyLocation second,
            ArmyLocation activeWaypoint
    ) {
        ArmyGroupOrder order = new ArmyGroupOrder(
                ArmyCommandType.PATROL_ROUTE,
                Optional.of(activeWaypoint),
                Optional.empty(),
                ArmyFormation.LINE,
                2);
        return ArmyGroupRecord.create(
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), List.of(),
                        ArmyFormation.LINE, first, 0L)
                .withPatrolRoute(List.of(first, second))
                .withOrder(order);
    }

    private static ArmyLocation location(ArmyPosition position) {
        return new ArmyLocation("minecraft:overworld", position.x(), position.y(), position.z());
    }

    private static long horizontalDistanceSquared(ArmyPosition first, ArmyPosition second) {
        long x = (long) first.x() - second.x();
        long z = (long) first.z() - second.z();
        return x * x + z * z;
    }

    private static void rejectsInvalidInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> new ArmyPatrolRoute(List.of(POINT_A), ArmyPatrolMode.LOOP, 0, 0),
                "single waypoint");
        assertThrows(NullPointerException.class,
                () -> new ArmyPatrolRoute(Arrays.asList(POINT_A, null), ArmyPatrolMode.LOOP, 0, 0),
                "null waypoint");
        assertThrows(NullPointerException.class,
                () -> new ArmyPatrolRoute(List.of(POINT_A, POINT_B), null, 0, 0),
                "null mode");
        assertThrows(IllegalArgumentException.class,
                () -> new ArmyPatrolRoute(List.of(POINT_A, POINT_B), ArmyPatrolMode.LOOP, -1, 0),
                "negative arrival distance");
        assertThrows(IllegalArgumentException.class,
                () -> new ArmyPatrolState(-1, 1, 0),
                "negative waypoint index");
        assertThrows(IllegalArgumentException.class,
                () -> new ArmyPatrolState(0, 0, 0),
                "invalid direction");
        assertThrows(IllegalArgumentException.class,
                () -> new ArmyPatrolState(0, 1, -1),
                "negative wait");
        assertThrows(IllegalArgumentException.class,
                () -> new ArmyPatrolDecision(POINT_A, ArmyPatrolState.start(), false, " "),
                "blank decision reason");
        assertThrows(IllegalArgumentException.class,
                () -> ArmyPatrolPlanner.advance(route(ArmyPatrolMode.LOOP, 0, 0), new ArmyPatrolState(3, 1, 0), POINT_A),
                "state index outside route");
    }

    private static ArmyPatrolRoute route(ArmyPatrolMode mode, int arrivalDistance, int waitTicksAtWaypoint) {
        return new ArmyPatrolRoute(List.of(POINT_A, POINT_B, POINT_C), mode, arrivalDistance, waitTicksAtWaypoint);
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
