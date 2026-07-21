package galacticwars.clonewars.workforce;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Pure route and action-cursor transitions shared by loaded and simulated couriers. */
public final class CourierRoutePlanner {
    private CourierRoutePlanner() {
    }

    /**
     * Reconciles persisted progress with the active definition. A route edit or
     * invalid cursor restarts safely instead of skipping transfers.
     */
    public static CourierRouteExecutionState reconcile(
            CourierRoutePlan route,
            CourierRouteExecutionState state
    ) {
        Objects.requireNonNull(route, "route");
        Objects.requireNonNull(state, "state");
        if (state.routeRevision() != route.revision()
                || state.waypointCursor() >= route.waypoints().size()) {
            return CourierRouteExecutionState.start(route.revision());
        }

        List<CourierTransferAction> actions = route.waypoints().get(state.waypointCursor()).actions();
        if ((!actions.isEmpty() && state.actionCursor() >= actions.size())
                || (actions.isEmpty() && state.actionCursor() != 0)) {
            return new CourierRouteExecutionState(
                    state.waypointCursor(), 0, normalizedDirection(route, state.direction()), 0, route.revision());
        }

        int direction = normalizedDirection(route, state.direction());
        int dwell = state.dwellTicksRemaining();
        if (dwell > 0) {
            CourierTransferAction action = actions.isEmpty() ? null : actions.get(state.actionCursor());
            if (action == null || !action.type().waitsAtWaypoint()) {
                dwell = 0;
            } else {
                dwell = Math.min(dwell, action.dwellTicks());
            }
        }
        if (direction == state.direction() && dwell == state.dwellTicksRemaining()) {
            return state;
        }
        return new CourierRouteExecutionState(
                state.waypointCursor(), state.actionCursor(), direction, dwell, route.revision());
    }

    public static CourierWaypoint currentWaypoint(
            CourierRoutePlan route,
            CourierRouteExecutionState state
    ) {
        CourierRouteExecutionState reconciled = reconcile(route, state);
        return route.waypoints().get(reconciled.waypointCursor());
    }

    public static Optional<CourierTransferAction> currentAction(
            CourierRoutePlan route,
            CourierRouteExecutionState state
    ) {
        CourierRouteExecutionState reconciled = reconcile(route, state);
        List<CourierTransferAction> actions = route.waypoints().get(reconciled.waypointCursor()).actions();
        return actions.isEmpty() ? Optional.empty() : Optional.of(actions.get(reconciled.actionCursor()));
    }

    /**
     * Decides whether a physical transfer fulfilled the current action. Bounded
     * TAKE/PUT actions advance only after one exact transfer, fill actions use
     * the resulting target inventory count, and open-ended actions require at
     * least one physical item to move.
     */
    public static boolean transferSatisfied(
            CourierTransferAction action,
            int transferredQuantity,
            int resultingTargetQuantity
    ) {
        Objects.requireNonNull(action, "action");
        if (transferredQuantity < 0 || resultingTargetQuantity < 0) {
            throw new IllegalArgumentException("courier transfer quantities cannot be negative");
        }
        return switch (action.effectiveType()) {
            case TAKE, PUT -> transferredQuantity == action.quantity();
            case TAKE_FILL, PUT_FILL -> resultingTargetQuantity >= action.quantity();
            case TAKE_ANY, PUT_ANY, TAKE_ALL, PUT_ALL -> transferredQuantity > 0;
            default -> false;
        };
    }

    /** Starts the current WAIT action without consuming a game tick. */
    public static CourierRouteExecutionState startDwell(
            CourierRoutePlan route,
            CourierRouteExecutionState state
    ) {
        CourierRouteExecutionState reconciled = reconcile(route, state);
        CourierTransferAction action = currentAction(route, reconciled)
                .orElseThrow(() -> new IllegalStateException("current waypoint has no courier action"));
        if (!action.type().waitsAtWaypoint()) {
            throw new IllegalStateException("current courier action is not WAIT");
        }
        if (reconciled.dwellTicksRemaining() > 0) {
            return reconciled;
        }
        return new CourierRouteExecutionState(
                reconciled.waypointCursor(),
                reconciled.actionCursor(),
                reconciled.direction(),
                action.dwellTicks(),
                route.revision());
    }

    /**
     * Consumes persisted dwell time. Finishing the wait advances exactly once
     * to the next action or waypoint.
     */
    public static CourierRouteExecutionState elapseDwell(
            CourierRoutePlan route,
            CourierRouteExecutionState state,
            int elapsedTicks
    ) {
        if (elapsedTicks < 0) {
            throw new IllegalArgumentException("elapsedTicks cannot be negative");
        }
        CourierRouteExecutionState reconciled = reconcile(route, state);
        if (elapsedTicks == 0 || reconciled.dwellTicksRemaining() == 0) {
            return reconciled;
        }
        int remaining = Math.max(0, reconciled.dwellTicksRemaining() - elapsedTicks);
        CourierRouteExecutionState elapsed = new CourierRouteExecutionState(
                reconciled.waypointCursor(),
                reconciled.actionCursor(),
                reconciled.direction(),
                remaining,
                route.revision());
        return remaining == 0 ? advanceAfterCurrentAction(route, elapsed) : elapsed;
    }

    /** Records a successful non-WAIT transfer and advances its action cursor. */
    public static CourierRouteExecutionState completeCurrentAction(
            CourierRoutePlan route,
            CourierRouteExecutionState state
    ) {
        CourierRouteExecutionState reconciled = reconcile(route, state);
        CourierTransferAction action = currentAction(route, reconciled)
                .orElseThrow(() -> new IllegalStateException("current waypoint has no courier action"));
        if (action.type().waitsAtWaypoint()) {
            throw new IllegalStateException("WAIT actions must complete through elapsed dwell time");
        }
        return advanceAfterCurrentAction(route, reconciled);
    }

    /** Advances a waypoint that deliberately has no transfer actions. */
    public static CourierRouteExecutionState completeEmptyWaypoint(
            CourierRoutePlan route,
            CourierRouteExecutionState state
    ) {
        CourierRouteExecutionState reconciled = reconcile(route, state);
        if (currentAction(route, reconciled).isPresent()) {
            throw new IllegalStateException("current waypoint still has courier actions");
        }
        return advanceWaypoint(route, reconciled);
    }

    private static CourierRouteExecutionState advanceAfterCurrentAction(
            CourierRoutePlan route,
            CourierRouteExecutionState state
    ) {
        int actionCount = route.waypoints().get(state.waypointCursor()).actions().size();
        if (state.actionCursor() + 1 < actionCount) {
            return new CourierRouteExecutionState(
                    state.waypointCursor(),
                    state.actionCursor() + 1,
                    state.direction(),
                    0,
                    route.revision());
        }
        return advanceWaypoint(route, state);
    }

    private static CourierRouteExecutionState advanceWaypoint(
            CourierRoutePlan route,
            CourierRouteExecutionState state
    ) {
        int waypointCount = route.waypoints().size();
        int nextWaypoint;
        int nextDirection;
        if (route.mode() == CourierRouteMode.LOOP) {
            nextWaypoint = (state.waypointCursor() + 1) % waypointCount;
            nextDirection = 1;
        } else {
            nextDirection = state.direction();
            nextWaypoint = state.waypointCursor() + nextDirection;
            if (nextWaypoint >= waypointCount) {
                nextDirection = -1;
                nextWaypoint = state.waypointCursor() - 1;
            } else if (nextWaypoint < 0) {
                nextDirection = 1;
                nextWaypoint = state.waypointCursor() + 1;
            }
        }
        return new CourierRouteExecutionState(nextWaypoint, 0, nextDirection, 0, route.revision());
    }

    private static int normalizedDirection(CourierRoutePlan route, int direction) {
        return route.mode() == CourierRouteMode.LOOP ? 1 : direction;
    }
}
