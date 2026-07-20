package galacticwars.clonewars.army;

import java.util.Objects;

public final class ArmyPatrolPlanner {
    private ArmyPatrolPlanner() {
    }

    public static ArmyPatrolDecision advance(
            ArmyPatrolRoute route,
            ArmyPatrolState state,
            ArmyPosition currentPosition
    ) {
        return advance(route, state, currentPosition, 1, false);
    }

    /**
     * Advances route progress after the supplied number of elapsed game ticks.
     * Loaded SmartBrain behaviour continues to use the one-tick overload;
     * virtual squads use their actual simulation interval so waypoint dwell
     * times retain their persisted tick meaning while unloaded.
     */
    public static ArmyPatrolDecision advance(
            ArmyPatrolRoute route,
            ArmyPatrolState state,
            ArmyPosition currentPosition,
            int elapsedTicks
    ) {
        return advance(route, state, currentPosition, elapsedTicks, true);
    }

    private static ArmyPatrolDecision advance(
            ArmyPatrolRoute route,
            ArmyPatrolState state,
            ArmyPosition currentPosition,
            int elapsedTicks,
            boolean publishNextWaypointWhenWaitExpires
    ) {
        Objects.requireNonNull(route, "route");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(currentPosition, "currentPosition");
        if (elapsedTicks < 0) {
            throw new IllegalArgumentException("elapsedTicks cannot be negative");
        }
        if (state.waypointIndex() >= route.waypoints().size()) {
            throw new IllegalArgumentException("state waypointIndex is outside the route");
        }

        if (state.status() == ArmyPatrolStatus.PAUSED) {
            return decision(currentPosition, state, true, "patrol_paused", route);
        }
        if (state.status() == ArmyPatrolStatus.STOPPED) {
            return decision(currentPosition, state, true, "patrol_stopped", route);
        }

        if (state.waitTicksRemaining() > 0) {
            int remainingWaitTicks = Math.max(0, state.waitTicksRemaining() - elapsedTicks);
            if (remainingWaitTicks > 0 || !publishNextWaypointWhenWaitExpires) {
                ArmyPatrolState nextState = new ArmyPatrolState(
                        state.waypointIndex(),
                        state.direction(),
                        remainingWaitTicks,
                        state.status());
                return decision(currentPosition, nextState, true, "waiting_at_waypoint", route);
            }
            // The dwell interval has just elapsed. Continue below so a coarse
            // virtual simulation can publish the next waypoint immediately
            // instead of waiting for another full simulation interval.
            state = new ArmyPatrolState(state.waypointIndex(), state.direction(), 0, state.status());
        }

        ArmyPosition activeWaypoint = route.waypoints().get(state.waypointIndex());
        if (!isWithinArrivalDistance(currentPosition, activeWaypoint, route.arrivalDistance())) {
            return decision(activeWaypoint, state, false, "moving_to_waypoint", route);
        }

        ArmyPatrolState advancedState = advanceState(route, state);
        int waitTicks = route.waitTicksAtWaypoint(state.waypointIndex());
        if (waitTicks > 0) {
            ArmyPatrolState waitingState = new ArmyPatrolState(
                    advancedState.waypointIndex(),
                    advancedState.direction(),
                    waitTicks,
                    advancedState.status());
            return decision(currentPosition, waitingState, true, "arrived_waiting", route);
        }

        return decision(
                route.waypoints().get(advancedState.waypointIndex()),
                advancedState,
                false,
                "advanced_waypoint",
                route);
    }

    private static ArmyPatrolState advanceState(ArmyPatrolRoute route, ArmyPatrolState state) {
        return switch (route.mode()) {
            case LOOP -> advanceLoop(route, state);
            case PING_PONG -> advancePingPong(route, state);
        };
    }

    private static ArmyPatrolState advanceLoop(ArmyPatrolRoute route, ArmyPatrolState state) {
        int size = route.waypoints().size();
        int nextIndex = state.waypointIndex() + state.direction();
        if (nextIndex >= size) {
            nextIndex = 0;
        } else if (nextIndex < 0) {
            nextIndex = size - 1;
        }
        return new ArmyPatrolState(nextIndex, state.direction(), 0, state.status());
    }

    private static ArmyPatrolState advancePingPong(ArmyPatrolRoute route, ArmyPatrolState state) {
        int size = route.waypoints().size();
        int nextIndex = state.waypointIndex() + state.direction();
        int nextDirection = state.direction();
        if (nextIndex >= size) {
            nextDirection = -1;
            nextIndex = state.waypointIndex() - 1;
        } else if (nextIndex < 0) {
            nextDirection = 1;
            nextIndex = state.waypointIndex() + 1;
        }
        return new ArmyPatrolState(nextIndex, nextDirection, 0, state.status());
    }

    private static ArmyPatrolDecision decision(
            ArmyPosition target,
            ArmyPatrolState state,
            boolean waiting,
            String reasonCode,
            ArmyPatrolRoute route
    ) {
        return new ArmyPatrolDecision(target, state, waiting, reasonCode,
                route.movementSpeed(), route.enemyPolicy());
    }

    private static boolean isWithinArrivalDistance(ArmyPosition currentPosition, ArmyPosition target, int arrivalDistance) {
        return distanceSquared(currentPosition, target) <= (long) arrivalDistance * arrivalDistance;
    }

    private static long distanceSquared(ArmyPosition first, ArmyPosition second) {
        long dx = (long) first.x() - second.x();
        long dy = (long) first.y() - second.y();
        long dz = (long) first.z() - second.z();
        return dx * dx + dy * dy + dz * dz;
    }
}
