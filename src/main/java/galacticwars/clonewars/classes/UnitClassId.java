package galacticwars.clonewars.classes;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record UnitClassId(String namespace, String path) {
    public static final String DEFAULT_NAMESPACE = "galacticwars";
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9/._-]+");

    public UnitClassId {
        namespace = normalize(namespace, "namespace");
        path = normalize(path, "path");
        if (!NAMESPACE.matcher(namespace).matches() || !PATH.matcher(path).matches()) {
            throw new IllegalArgumentException("Invalid unit class id " + namespace + ":" + path);
        }
    }

    public static UnitClassId of(String value) {
        Objects.requireNonNull(value, "value");
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        int separator = normalized.indexOf(':');
        return separator < 0
                ? new UnitClassId(DEFAULT_NAMESPACE, normalized)
                : new UnitClassId(normalized.substring(0, separator), normalized.substring(separator + 1));
    }

    private static String normalize(String value, String label) {
        Objects.requireNonNull(value, label);
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return normalized;
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}
