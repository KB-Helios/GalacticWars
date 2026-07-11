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
        Objects.requireNonNull(route, "route");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(currentPosition, "currentPosition");
        if (state.waypointIndex() >= route.waypoints().size()) {
            throw new IllegalArgumentException("state waypointIndex is outside the route");
        }

        if (state.waitTicksRemaining() > 0) {
            ArmyPatrolState nextState = new ArmyPatrolState(
                    state.waypointIndex(),
                    state.direction(),
                    state.waitTicksRemaining() - 1);
            return new ArmyPatrolDecision(currentPosition, nextState, true, "waiting_at_waypoint");
        }

        ArmyPosition activeWaypoint = route.waypoints().get(state.waypointIndex());
        if (!isWithinArrivalDistance(currentPosition, activeWaypoint, route.arrivalDistance())) {
            return new ArmyPatrolDecision(activeWaypoint, state, false, "moving_to_waypoint");
        }

        ArmyPatrolState advancedState = advanceState(route, state);
        if (route.waitTicksAtWaypoint() > 0) {
            ArmyPatrolState waitingState = new ArmyPatrolState(
                    advancedState.waypointIndex(),
                    advancedState.direction(),
                    route.waitTicksAtWaypoint());
            return new ArmyPatrolDecision(currentPosition, waitingState, true, "arrived_waiting");
        }

        return new ArmyPatrolDecision(
                route.waypoints().get(advancedState.waypointIndex()),
                advancedState,
                false,
                "advanced_waypoint");
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
        return new ArmyPatrolState(nextIndex, state.direction(), 0);
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
        return new ArmyPatrolState(nextIndex, nextDirection, 0);
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
