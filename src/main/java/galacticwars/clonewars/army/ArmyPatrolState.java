package galacticwars.clonewars.army;

import java.util.Objects;

public record ArmyPatrolState(int waypointIndex, int direction, int waitTicksRemaining, ArmyPatrolStatus status) {
    public ArmyPatrolState {
        if (waypointIndex < 0) {
            throw new IllegalArgumentException("waypointIndex cannot be negative");
        }
        if (direction != 1 && direction != -1) {
            throw new IllegalArgumentException("direction must be 1 or -1");
        }
        if (waitTicksRemaining < 0) {
            throw new IllegalArgumentException("waitTicksRemaining cannot be negative");
        }
        Objects.requireNonNull(status, "status");
    }

    /** Compatibility constructor for active legacy patrol progress. */
    public ArmyPatrolState(int waypointIndex, int direction, int waitTicksRemaining) {
        this(waypointIndex, direction, waitTicksRemaining, ArmyPatrolStatus.ACTIVE);
    }

    public static ArmyPatrolState start() {
        return new ArmyPatrolState(0, 1, 0, ArmyPatrolStatus.ACTIVE);
    }

    public ArmyPatrolState pause() {
        return status == ArmyPatrolStatus.ACTIVE
                ? new ArmyPatrolState(waypointIndex, direction, waitTicksRemaining, ArmyPatrolStatus.PAUSED)
                : this;
    }

    public ArmyPatrolState resume() {
        return status == ArmyPatrolStatus.PAUSED
                ? new ArmyPatrolState(waypointIndex, direction, waitTicksRemaining, ArmyPatrolStatus.ACTIVE)
                : this;
    }

    public ArmyPatrolState stop() {
        return status == ArmyPatrolStatus.STOPPED
                ? this
                : new ArmyPatrolState(waypointIndex, direction, 0, ArmyPatrolStatus.STOPPED);
    }
}
