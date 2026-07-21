package galacticwars.clonewars.workforce;

/**
 * Persistable route progress. Route definitions and execution progress remain
 * separate so editing a route can invalidate an old cursor deterministically.
 */
public record CourierRouteExecutionState(
        int waypointCursor,
        int actionCursor,
        int direction,
        int dwellTicksRemaining,
        long routeRevision
) {
    public CourierRouteExecutionState {
        if (waypointCursor < 0) {
            throw new IllegalArgumentException("waypointCursor cannot be negative");
        }
        if (actionCursor < 0) {
            throw new IllegalArgumentException("actionCursor cannot be negative");
        }
        if (direction != 1 && direction != -1) {
            throw new IllegalArgumentException("direction must be 1 or -1");
        }
        if (dwellTicksRemaining < 0) {
            throw new IllegalArgumentException("dwellTicksRemaining cannot be negative");
        }
        if (routeRevision < 0L) {
            throw new IllegalArgumentException("routeRevision cannot be negative");
        }
    }

    public static CourierRouteExecutionState start(long routeRevision) {
        return new CourierRouteExecutionState(0, 0, 1, 0, routeRevision);
    }
}
