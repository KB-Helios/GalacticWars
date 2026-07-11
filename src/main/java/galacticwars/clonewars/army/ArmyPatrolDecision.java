package galacticwars.clonewars.army;

import java.util.Objects;

public record ArmyPatrolDecision(
        ArmyPosition moveTarget,
        ArmyPatrolState nextState,
        boolean waiting,
        String reasonCode
) {
    public ArmyPatrolDecision {
        Objects.requireNonNull(moveTarget, "moveTarget");
        Objects.requireNonNull(nextState, "nextState");
        reasonCode = requireNonBlank(reasonCode);
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
