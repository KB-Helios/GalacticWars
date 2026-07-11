package galacticwars.clonewars.army;

import java.util.Objects;

public record ArmyCommandValidation(boolean accepted, String reasonCode) {
    public ArmyCommandValidation {
        reasonCode = requireNonBlank(reasonCode);
    }

    public static ArmyCommandValidation acceptedResult() {
        return new ArmyCommandValidation(true, "accepted");
    }

    public static ArmyCommandValidation rejected(String reasonCode) {
        return new ArmyCommandValidation(false, reasonCode);
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
