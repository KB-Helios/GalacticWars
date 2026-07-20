package galacticwars.clonewars.army;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Pure persisted patrol configuration and progress. Existing saves can derive
 * this plan from their legacy {@code patrolRoute} with {@link #fromLegacyRoute}.
 */
public record ArmyPatrolPlan(
        List<ArmyPatrolWaypoint> waypoints,
        ArmyPatrolMode mode,
        ArmyPatrolState state,
        int arrivalDistance,
        double movementSpeed,
        ArmyPatrolEnemyPolicy enemyPolicy,
        String name
) {
    public static final int DEFAULT_ARRIVAL_DISTANCE = 2;
    public static final double DEFAULT_MOVEMENT_SPEED = 1.0D;
    public static final String DEFAULT_NAME = "Patrol Route";
    public static final int MAX_NAME_LENGTH = 32;
    /** Ten minutes at the normal 20 ticks per second field-command limit. */
    public static final int MAX_FIELD_COMMAND_WAIT_TICKS = 12_000;

    public ArmyPatrolPlan {
        Objects.requireNonNull(waypoints, "waypoints");
        if (waypoints.size() < 2 || waypoints.size() > 32) {
            throw new IllegalArgumentException("waypoints must contain 2-32 entries");
        }
        waypoints = List.copyOf(waypoints);
        String dimension = waypoints.getFirst().location().dimensionId();
        if (waypoints.stream().anyMatch(waypoint -> !waypoint.location().dimensionId().equals(dimension))) {
            throw new IllegalArgumentException("waypoints must share one dimension");
        }
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(state, "state");
        if (state.waypointIndex() >= waypoints.size()) {
            throw new IllegalArgumentException("state waypointIndex is outside the route");
        }
        if (arrivalDistance < 0) {
            throw new IllegalArgumentException("arrivalDistance cannot be negative");
        }
        if (!Double.isFinite(movementSpeed) || movementSpeed <= 0.0D) {
            throw new IllegalArgumentException("movementSpeed must be finite and positive");
        }
        Objects.requireNonNull(enemyPolicy, "enemyPolicy");
        name = normalizeName(name);
    }

    /** Compatibility constructor for pre-named patrol plans and existing save data. */
    public ArmyPatrolPlan(
            List<ArmyPatrolWaypoint> waypoints,
            ArmyPatrolMode mode,
            ArmyPatrolState state,
            int arrivalDistance,
            double movementSpeed,
            ArmyPatrolEnemyPolicy enemyPolicy
    ) {
        this(waypoints, mode, state, arrivalDistance, movementSpeed, enemyPolicy, DEFAULT_NAME);
    }

    public static Optional<ArmyPatrolPlan> fromLegacyRoute(List<ArmyLocation> legacyRoute) {
        Objects.requireNonNull(legacyRoute, "legacyRoute");
        if (legacyRoute.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ArmyPatrolPlan(
                legacyRoute.stream().map(ArmyPatrolWaypoint::immediate).toList(),
                ArmyPatrolMode.LOOP,
                ArmyPatrolState.start(),
                DEFAULT_ARRIVAL_DISTANCE,
                DEFAULT_MOVEMENT_SPEED,
                ArmyPatrolEnemyPolicy.ENGAGE_HOSTILES,
                DEFAULT_NAME));
    }

    public List<ArmyLocation> locations() {
        return waypoints.stream().map(ArmyPatrolWaypoint::location).toList();
    }

    public ArmyPatrolRoute toRuntimeRoute() {
        return new ArmyPatrolRoute(
                waypoints.stream().map(waypoint -> waypoint.location().blockPosition()).toList(),
                mode,
                arrivalDistance,
                0,
                waypoints.stream().map(ArmyPatrolWaypoint::waitTicks).toList(),
                movementSpeed,
                enemyPolicy);
    }

    /**
     * Converts a persisted patrol multiplier to a safe SmartBrain walk speed.
     * The field command cycles within this range, while a hand-edited save is
     * still prevented from producing an unusably slow or runaway loaded path.
     */
    public float loadedMovementSpeed() {
        return (float)Math.max(0.25D, Math.min(2.0D, movementSpeed));
    }

    public ArmyPatrolPlan withState(ArmyPatrolState state) {
        return new ArmyPatrolPlan(waypoints, mode, state, arrivalDistance, movementSpeed, enemyPolicy, name);
    }

    public ArmyPatrolPlan pause() {
        return withState(state.pause());
    }

    public ArmyPatrolPlan resume() {
        return withState(state.resume());
    }

    public ArmyPatrolPlan stop() {
        return withState(state.stop());
    }

    public ArmyPatrolPlan withName(String name) {
        return new ArmyPatrolPlan(waypoints, mode, state, arrivalDistance, movementSpeed, enemyPolicy, name);
    }

    /** Replaces one dwell interval while retaining route progress and all other patrol controls. */
    public ArmyPatrolPlan withWaypointWaitTicks(int waypointIndex, int waitTicks) {
        if (waypointIndex < 0 || waypointIndex >= waypoints.size()) {
            throw new IllegalArgumentException("waypointIndex is outside the route");
        }
        List<ArmyPatrolWaypoint> updated = new java.util.ArrayList<>(waypoints);
        updated.set(waypointIndex, new ArmyPatrolWaypoint(waypoints.get(waypointIndex).location(), waitTicks));
        return new ArmyPatrolPlan(updated, mode, state, arrivalDistance, movementSpeed, enemyPolicy, name);
    }

    private static String normalizeName(String value) {
        String normalized = Objects.requireNonNull(value, "name").trim();
        if (normalized.isEmpty() || normalized.length() > MAX_NAME_LENGTH
                || normalized.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("name must contain 1-" + MAX_NAME_LENGTH + " visible characters");
        }
        return normalized;
    }
}
