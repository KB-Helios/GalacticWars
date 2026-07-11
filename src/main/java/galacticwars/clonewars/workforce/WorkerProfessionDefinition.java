package galacticwars.clonewars.workforce;

import java.util.Objects;

public record WorkerProfessionDefinition(
        WorkerProfession profession,
        WorkAreaType workAreaType,
        int hireCostCredits,
        int commandButtonId,
        String defaultHeldItemId
) {
    public WorkerProfessionDefinition {
        Objects.requireNonNull(profession, "profession");
        Objects.requireNonNull(workAreaType, "workAreaType");
        if (hireCostCredits < 0) {
            throw new IllegalArgumentException("hireCostCredits cannot be negative");
        }
        if (commandButtonId < 0) {
            throw new IllegalArgumentException("commandButtonId cannot be negative");
        }
        defaultHeldItemId = requireNonBlank(defaultHeldItemId, "defaultHeldItemId");
    }

    public String id() {
        return profession.id();
    }

    public String translationKey() {
        return profession.translationKey();
    }

    private static String requireNonBlank(String value, String label) {
        Objects.requireNonNull(value, label);
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return trimmed;
    }
}
