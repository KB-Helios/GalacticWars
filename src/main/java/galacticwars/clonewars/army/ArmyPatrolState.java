package galacticwars.clonewars.army;

public record ArmyPatrolState(int waypointIndex, int direction, int waitTicksRemaining) {
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
    }

    public static ArmyPatrolState start() {
        return new ArmyPatrolState(0, 1, 0);
    }
}
