package galacticwars.clonewars.workforce;

import java.util.Collections;
import java.util.List;

public final class CourierRoutePlannerTest {
    private static final String DIMENSION = "minecraft:overworld";

    private CourierRoutePlannerTest() {
    }

    public static void main(String[] args) {
        preservesLegacyActionIdsWhileAddingSourceParityActions();
        loopProcessesOrderedActionsAndWraps();
        pingPongReversesAtBothEdges();
        dwellProgressSurvivesAcrossTicks();
        routeRevisionReconcilesPersistedProgress();
        emptyWaypointsAdvanceExplicitly();
        transfersAdvanceOnlyWhenTheirContractIsSatisfied();
        rejectsInvalidDomainState();

        System.out.println("CourierRoutePlannerTest passed");
    }

    private static void preservesLegacyActionIdsWhileAddingSourceParityActions() {
        assertEquals(CourierTransferType.FILL, CourierTransferType.byId("fill"), "legacy fill id");
        assertEquals(CourierTransferType.EMPTY, CourierTransferType.byId("EMPTY"), "legacy empty id");
        assertEquals(CourierTransferType.PUT_FILL, CourierTransferType.FILL.canonical(), "legacy fill semantics");
        assertEquals(CourierTransferType.PUT_ALL, CourierTransferType.EMPTY.canonical(), "legacy empty semantics");

        CourierTransferAction legacy = new CourierTransferAction(
                CourierTransferType.FILL, "GalacticWars:Energy_Cell", 0);
        assertEquals("galacticwars:energy_cell", legacy.itemId(), "legacy item normalization");
        assertEquals(CourierTransferType.PUT_FILL, legacy.effectiveType(), "legacy effective type");

        assertEquals(CourierTransferType.TAKE, CourierTransferAction.take("minecraft:bread", 2).type(),
                "bounded take");
        assertEquals(CourierTransferType.PUT, CourierTransferAction.put("minecraft:bread", 2).type(),
                "bounded put");
        assertEquals(CourierTransferType.TAKE_ANY, CourierTransferAction.takeAny("minecraft:bread").type(),
                "take any");
        assertEquals(CourierTransferType.PUT_ANY, CourierTransferAction.putAny("minecraft:bread").type(),
                "put any");
        assertEquals(CourierTransferType.TAKE_ALL, CourierTransferAction.takeAll().type(), "take all");
        assertEquals(CourierTransferType.PUT_ALL, CourierTransferAction.putAll().type(), "put all");
        assertEquals(CourierTransferType.PUT_FILL,
                CourierTransferAction.putFill("minecraft:bread", 16).type(), "put fill");
        assertEquals(CourierTransferType.TAKE_FILL,
                CourierTransferAction.takeFill("minecraft:bread", 16).type(), "take fill");
        assertEquals(40, CourierTransferAction.waitTicks(40).dwellTicks(), "wait ticks");

        assertTrue(CourierTransferType.TAKE_ALL.takesFromWaypoint(), "take-all direction");
        assertTrue(CourierTransferType.PUT_ALL.putsIntoWaypoint(), "put-all direction");
        assertTrue(CourierTransferType.TAKE_FILL.hasItemFilter(), "take-fill item filter");
        assertTrue(CourierTransferType.WAIT.waitsAtWaypoint(), "wait action classification");
    }

    private static void loopProcessesOrderedActionsAndWraps() {
        CourierRoutePlan route = new CourierRoutePlan(List.of(
                waypoint(0, CourierTransferAction.take("minecraft:bread", 2), CourierTransferAction.putAll()),
                waypoint(10, CourierTransferAction.takeAny("minecraft:iron_ingot")),
                waypoint(20, CourierTransferAction.putFill("minecraft:iron_ingot", 16))),
                CourierRouteMode.LOOP,
                7L);
        CourierRouteExecutionState state = CourierRouteExecutionState.start(route.revision());

        assertEquals(CourierTransferType.TAKE,
                CourierRoutePlanner.currentAction(route, state).orElseThrow().type(), "first action");
        state = CourierRoutePlanner.completeCurrentAction(route, state);
        assertEquals(new CourierRouteExecutionState(0, 1, 1, 0, 7L), state, "second action cursor");

        state = CourierRoutePlanner.completeCurrentAction(route, state);
        assertEquals(new CourierRouteExecutionState(1, 0, 1, 0, 7L), state, "second waypoint");
        state = CourierRoutePlanner.completeCurrentAction(route, state);
        assertEquals(new CourierRouteExecutionState(2, 0, 1, 0, 7L), state, "third waypoint");
        state = CourierRoutePlanner.completeCurrentAction(route, state);
        assertEquals(CourierRouteExecutionState.start(7L), state, "loop wrap");
    }

    private static void pingPongReversesAtBothEdges() {
        CourierRoutePlan route = oneActionRoute(CourierRouteMode.PING_PONG, 11L);
        CourierRouteExecutionState state = new CourierRouteExecutionState(2, 0, 1, 0, 11L);

        state = CourierRoutePlanner.completeCurrentAction(route, state);
        assertEquals(new CourierRouteExecutionState(1, 0, -1, 0, 11L), state, "reverse at upper edge");
        state = CourierRoutePlanner.completeCurrentAction(route, state);
        assertEquals(new CourierRouteExecutionState(0, 0, -1, 0, 11L), state, "continue backward");
        state = CourierRoutePlanner.completeCurrentAction(route, state);
        assertEquals(new CourierRouteExecutionState(1, 0, 1, 0, 11L), state, "reverse at lower edge");
    }

    private static void dwellProgressSurvivesAcrossTicks() {
        CourierRoutePlan route = new CourierRoutePlan(List.of(
                waypoint(0, CourierTransferAction.waitTicks(5), CourierTransferAction.takeAll()),
                waypoint(10, CourierTransferAction.putAll())),
                CourierRouteMode.LOOP,
                13L);
        CourierRouteExecutionState state = CourierRoutePlanner.startDwell(
                route, CourierRouteExecutionState.start(route.revision()));

        assertEquals(new CourierRouteExecutionState(0, 0, 1, 5, 13L), state, "started dwell");
        assertEquals(state, CourierRoutePlanner.startDwell(route, state), "starting dwell is idempotent");
        state = CourierRoutePlanner.elapseDwell(route, state, 2);
        assertEquals(new CourierRouteExecutionState(0, 0, 1, 3, 13L), state, "partially elapsed dwell");
        state = CourierRoutePlanner.elapseDwell(route, state, 3);
        assertEquals(new CourierRouteExecutionState(0, 1, 1, 0, 13L), state, "completed dwell action");
        state = CourierRoutePlanner.completeCurrentAction(route, state);
        assertEquals(new CourierRouteExecutionState(1, 0, 1, 0, 13L), state, "post-dwell transfer");
    }

    private static void routeRevisionReconcilesPersistedProgress() {
        CourierRoutePlan route = oneActionRoute(CourierRouteMode.LOOP, 21L);
        CourierRouteExecutionState stale = new CourierRouteExecutionState(2, 0, -1, 8, 20L);
        assertEquals(CourierRouteExecutionState.start(21L), CourierRoutePlanner.reconcile(route, stale),
                "route edit resets stale cursor");

        CourierRouteExecutionState invalidWaypoint = new CourierRouteExecutionState(9, 0, 1, 0, 21L);
        assertEquals(CourierRouteExecutionState.start(21L),
                CourierRoutePlanner.reconcile(route, invalidWaypoint), "invalid waypoint resets cursor");

        CourierRouteExecutionState reverseLoop = new CourierRouteExecutionState(1, 0, -1, 0, 21L);
        assertEquals(new CourierRouteExecutionState(1, 0, 1, 0, 21L),
                CourierRoutePlanner.reconcile(route, reverseLoop), "loop direction normalization");
    }

    private static void emptyWaypointsAdvanceExplicitly() {
        CourierRoutePlan route = new CourierRoutePlan(List.of(
                waypoint(0),
                waypoint(10, CourierTransferAction.putAll())),
                CourierRouteMode.LOOP,
                30L);
        CourierRouteExecutionState advanced = CourierRoutePlanner.completeEmptyWaypoint(
                route, CourierRouteExecutionState.start(route.revision()));

        assertEquals(new CourierRouteExecutionState(1, 0, 1, 0, 30L), advanced, "empty waypoint advance");
    }

    private static void transfersAdvanceOnlyWhenTheirContractIsSatisfied() {
        CourierTransferAction take = CourierTransferAction.take("minecraft:iron_ingot", 4);
        assertTrue(!CourierRoutePlanner.transferSatisfied(take, 0, 0), "missing exact transfer retries");
        assertTrue(!CourierRoutePlanner.transferSatisfied(take, 3, 0), "partial exact transfer retries");
        assertTrue(CourierRoutePlanner.transferSatisfied(take, 4, 0), "exact transfer advances");

        CourierTransferAction fill = CourierTransferAction.takeFill("minecraft:iron_ingot", 8);
        assertTrue(!CourierRoutePlanner.transferSatisfied(fill, 3, 7), "incomplete fill retries");
        assertTrue(CourierRoutePlanner.transferSatisfied(fill, 0, 8), "already-filled target advances");

        CourierTransferAction takeAll = CourierTransferAction.takeAll();
        assertTrue(!CourierRoutePlanner.transferSatisfied(takeAll, 0, 0), "empty open transfer retries");
        assertTrue(CourierRoutePlanner.transferSatisfied(takeAll, 1, 0), "physical open transfer advances");
    }

    private static void rejectsInvalidDomainState() {
        assertThrows(IllegalArgumentException.class,
                () -> CourierTransferAction.waitTicks(0), "zero wait");
        assertThrows(IllegalArgumentException.class,
                () -> CourierTransferAction.take(" ", 1), "blank filtered item");
        assertThrows(IllegalArgumentException.class,
                () -> CourierTransferAction.put("minecraft:bread", 0), "zero bounded transfer");
        assertThrows(IllegalArgumentException.class,
                () -> new CourierRoutePlan(List.of(waypoint(0)), CourierRouteMode.LOOP, 0L),
                "single waypoint route");
        CourierRoutePlan migratedOversizedRoute = new CourierRoutePlan(
                Collections.nCopies(CourierRoutePlan.MAX_WAYPOINTS + 1, waypoint(0)),
                CourierRouteMode.LOOP,
                0L);
        assertEquals(CourierRoutePlan.MAX_WAYPOINTS, migratedOversizedRoute.waypoints().size(),
                "oversized legacy route truncates");
        assertThrows(IllegalArgumentException.class,
                () -> new CourierRouteExecutionState(0, 0, 0, 0, 0L), "invalid direction");
        assertThrows(IllegalArgumentException.class,
                () -> CourierRoutePlanner.elapseDwell(
                        oneActionRoute(CourierRouteMode.LOOP, 1L),
                        CourierRouteExecutionState.start(1L),
                        -1),
                "negative elapsed ticks");
        assertThrows(IllegalStateException.class,
                () -> CourierRoutePlanner.completeCurrentAction(
                        new CourierRoutePlan(List.of(
                                waypoint(0, CourierTransferAction.waitTicks(1)),
                                waypoint(10, CourierTransferAction.putAll())),
                                CourierRouteMode.LOOP,
                                2L),
                        CourierRouteExecutionState.start(2L)),
                "wait cannot complete as transfer");
    }

    private static CourierRoutePlan oneActionRoute(CourierRouteMode mode, long revision) {
        return new CourierRoutePlan(List.of(
                waypoint(0, CourierTransferAction.takeAll()),
                waypoint(10, CourierTransferAction.putAll()),
                waypoint(20, CourierTransferAction.takeAny("minecraft:bread"))),
                mode,
                revision);
    }

    private static CourierWaypoint waypoint(int x, CourierTransferAction... actions) {
        return new CourierWaypoint(DIMENSION, x, 64, 0, List.of(actions));
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertTrue(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label + " expected true");
        }
    }

    private static <T extends Throwable> void assertThrows(
            Class<T> expectedType,
            ThrowingRunnable runnable,
            String label
    ) {
        try {
            runnable.run();
        } catch (Throwable throwable) {
            if (expectedType.isInstance(throwable)) {
                return;
            }
            throw new AssertionError(label + " threw " + throwable.getClass().getName()
                    + " instead of " + expectedType.getName(), throwable);
        }
        throw new AssertionError(label + " did not throw " + expectedType.getName());
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run();
    }
}
