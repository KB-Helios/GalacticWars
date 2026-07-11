package galacticwars.clonewars.army;

import java.util.Objects;
import java.util.UUID;

public record ArmyTargetSelection(
        UUID targetId,
        ArmyPosition targetPosition,
        String reasonCode,
        int score
) {
    public ArmyTargetSelection {
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(targetPosition, "targetPosition");
        reasonCode = requireNonBlank(reasonCode);
        if (score < 0) {
            throw new IllegalArgumentException("score cannot be negative");
        }
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
