package galacticwars.clonewars.army;

import java.util.Objects;

public record ArmyPatrolDecision(
        ArmyPosition moveTarget,
        ArmyPatrolState nextState,
        boolean waiting,
        String reasonCode,
        double movementSpeed,
        ArmyPatrolEnemyPolicy enemyPolicy
) {
    public ArmyPatrolDecision {
        Objects.requireNonNull(moveTarget, "moveTarget");
        Objects.requireNonNull(nextState, "nextState");
        reasonCode = requireNonBlank(reasonCode);
        if (!Double.isFinite(movementSpeed) || movementSpeed <= 0.0D) {
            throw new IllegalArgumentException("movementSpeed must be finite and positive");
        }
        Objects.requireNonNull(enemyPolicy, "enemyPolicy");
    }

    /** Compatibility constructor with the former default patrol policy. */
    public ArmyPatrolDecision(
            ArmyPosition moveTarget,
            ArmyPatrolState nextState,
            boolean waiting,
            String reasonCode
    ) {
        this(moveTarget, nextState, waiting, reasonCode,
                ArmyPatrolPlan.DEFAULT_MOVEMENT_SPEED, ArmyPatrolEnemyPolicy.ENGAGE_HOSTILES);
    }

    public boolean shouldMove() {
        return !waiting && nextState.status() == ArmyPatrolStatus.ACTIVE;
    }

    private static String requireNonBlank(String value) {
        Objects.requireNonNull(value, "reasonCode");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("reasonCode cannot be blank");
        }
        return trimmed;
    }
}
