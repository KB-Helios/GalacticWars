package galacticwars.clonewars.army;

import java.util.List;
import java.util.Objects;

public record ArmyPatrolRoute(
        List<ArmyPosition> waypoints,
        ArmyPatrolMode mode,
        int arrivalDistance,
        int waitTicksAtWaypoint
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
    }
}
