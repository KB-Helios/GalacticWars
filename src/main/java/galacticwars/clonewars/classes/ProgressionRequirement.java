package galacticwars.clonewars.classes;

import java.util.Locale;
import java.util.Objects;

public record ProgressionRequirement(String type, String subjectId, int amount) {
    public ProgressionRequirement {
        type = normalize(type, "type");
        subjectId = normalize(subjectId, "subjectId");
        if (amount < 1) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }

    private static String normalize(String value, String label) {
        Objects.requireNonNull(value, label);
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return normalized;
    }
}
