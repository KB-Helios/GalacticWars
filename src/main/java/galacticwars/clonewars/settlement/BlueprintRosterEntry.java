package galacticwars.clonewars.settlement;

import java.util.Locale;
import java.util.Objects;

public record BlueprintRosterEntry(String entityTypeId, int minimum, int maximum, int weight, String serviceBranch) {
    public BlueprintRosterEntry {
        entityTypeId = normalize(entityTypeId, "entityTypeId");
        serviceBranch = normalize(serviceBranch, "serviceBranch");
        if (minimum < 0 || maximum < minimum || maximum > 32 || weight <= 0) {
            throw new IllegalArgumentException("invalid blueprint roster entry for " + entityTypeId);
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
