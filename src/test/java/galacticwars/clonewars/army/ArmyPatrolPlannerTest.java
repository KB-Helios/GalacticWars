package galacticwars.clonewars.army;

import java.util.Arrays;
import java.util.List;

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
