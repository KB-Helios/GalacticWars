package galacticwars.clonewars.army;

import java.util.Locale;
import java.util.Objects;

public record ArmyUnitId(String namespace, String path) {
    public static final String DEFAULT_NAMESPACE = "galacticwars";

    public ArmyUnitId {
        namespace = normalizePart(namespace, "namespace");
        path = normalizePart(path, "path");
    }

    public static ArmyUnitId of(String value) {
        Objects.requireNonNull(value, "value");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Army unit id cannot be empty");
        }

        int separator = trimmed.indexOf(':');
        if (separator >= 0) {
            return new ArmyUnitId(trimmed.substring(0, separator), trimmed.substring(separator + 1));
        }

        return new ArmyUnitId(DEFAULT_NAMESPACE, trimmed);
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }

    private static String normalizePart(String value, String label) {
        Objects.requireNonNull(value, label);
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9_.-]+")) {
            throw new IllegalArgumentException("Invalid army unit id " + label + ": " + value);
        }
        return normalized;
    }
}
