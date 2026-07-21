package galacticwars.clonewars.workforce;

import java.util.List;
import java.util.Objects;

/** Loader-neutral route definition consumed by the courier execution planner. */
public record CourierRoutePlan(
        List<CourierWaypoint> waypoints,
        CourierRouteMode mode,
        long revision
) {
    public static final int MAX_WAYPOINTS = 64;

    public CourierRoutePlan {
        Objects.requireNonNull(waypoints, "waypoints");
        List<CourierWaypoint> normalizedWaypoints = List.copyOf(waypoints);
        if (normalizedWaypoints.size() < 2) {
            throw new IllegalArgumentException("courier route must contain at least 2 waypoints");
        }
        waypoints = normalizedWaypoints.size() <= MAX_WAYPOINTS
                ? normalizedWaypoints
                : List.copyOf(normalizedWaypoints.subList(0, MAX_WAYPOINTS));
        Objects.requireNonNull(mode, "mode");
        if (revision < 0L) {
            throw new IllegalArgumentException("courier route revision cannot be negative");
        }
    }
}
