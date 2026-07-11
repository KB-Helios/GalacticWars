package galacticwars.clonewars.faction;

import java.util.Locale;
import java.util.Objects;

public record FactionId(String namespace, String path) {
    public static final String DEFAULT_NAMESPACE = "galacticwars";

    public FactionId {
        namespace = normalizePart(namespace, "namespace");
        path = normalizePart(path, "path");
    }

    public static FactionId of(String value) {
        Objects.requireNonNull(value, "value");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Faction id cannot be empty");
        }

        int separator = trimmed.indexOf(':');
        if (separator >= 0) {
            return new FactionId(trimmed.substring(0, separator), trimmed.substring(separator + 1));
        }

        return new FactionId(DEFAULT_NAMESPACE, trimmed);
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }

    private static String normalizePart(String value, String label) {
        Objects.requireNonNull(value, label);
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9_.-]+")) {
            throw new IllegalArgumentException("Invalid faction id " + label + ": " + value);
        }
        return normalized;
    }
}
