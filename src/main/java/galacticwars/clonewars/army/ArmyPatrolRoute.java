package galacticwars.clonewars.army;

import java.util.List;
import java.util.Objects;

public record ArmyPatrolRoute(
        List<ArmyPosition> waypoints,
        ArmyPatrolMode mode,
        int arrivalDistance,
        int waitTicksAtWaypoint,
        List<Integer> waypointWaitTicks,
        double movementSpeed,
        ArmyPatrolEnemyPolicy enemyPolicy
) {
    public ArmyPatrolRoute {
        Objects.requireNonNull(waypoints, "waypoints");
        if (waypoints.size() < 2) {
            throw new IllegalArgumentException("waypoints must contain at least two positions");
        }
        waypoints = List.copyOf(waypoints);
        Objects.requireNonNull(mode, "mode");
        if (arrivalDistance < 0) {
            throw new IllegalArgumentException("arrivalDistance cannot be negative");
        }
        if (waitTicksAtWaypoint < 0) {
            throw new IllegalArgumentException("waitTicksAtWaypoint cannot be negative");
        }
        waypointWaitTicks = waypointWaitTicks == null ? List.of() : List.copyOf(waypointWaitTicks);
        if (!waypointWaitTicks.isEmpty() && waypointWaitTicks.size() != waypoints.size()) {
            throw new IllegalArgumentException("waypointWaitTicks must be empty or match waypoints");
        }
        if (waypointWaitTicks.stream().anyMatch(waitTicks -> waitTicks == null || waitTicks < 0)) {
            throw new IllegalArgumentException("waypointWaitTicks cannot contain null or negative values");
        }
        if (!Double.isFinite(movementSpeed) || movementSpeed <= 0.0D) {
            throw new IllegalArgumentException("movementSpeed must be finite and positive");
        }
        Objects.requireNonNull(enemyPolicy, "enemyPolicy");
    }

    /** Compatibility constructor using the previous global dwell time only. */
    public ArmyPatrolRoute(
            List<ArmyPosition> waypoints,
            ArmyPatrolMode mode,
            int arrivalDistance,
            int waitTicksAtWaypoint
    ) {
        this(waypoints, mode, arrivalDistance, waitTicksAtWaypoint, List.of(),
                ArmyPatrolPlan.DEFAULT_MOVEMENT_SPEED, ArmyPatrolEnemyPolicy.ENGAGE_HOSTILES);
    }

    public int waitTicksAtWaypoint(int waypointIndex) {
        if (waypointIndex < 0 || waypointIndex >= waypoints.size()) {
            throw new IllegalArgumentException("waypointIndex is outside the route");
        }
        return waypointWaitTicks.isEmpty() ? waitTicksAtWaypoint : waypointWaitTicks.get(waypointIndex);
    }
}
