package galacticwars.clonewars.faction;

import java.util.Objects;

public record FactionAlignmentRule(int directDelta, int allyDelta, int enemyDelta, String reasonCode) {
    public FactionAlignmentRule {
        reasonCode = requireNonBlank(reasonCode);
        if (directDelta == 0 && allyDelta == 0 && enemyDelta == 0) {
            throw new IllegalArgumentException("alignment rule must change at least one relation group");
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
