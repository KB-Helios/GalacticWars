package galacticwars.clonewars.kingdom;

import java.util.Locale;
import java.util.Objects;

final class KingdomNormalizers {
    private KingdomNormalizers() {
    }

    static String normalize(String value, String label) {
        Objects.requireNonNull(value, label);
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return normalized;
    }
}
