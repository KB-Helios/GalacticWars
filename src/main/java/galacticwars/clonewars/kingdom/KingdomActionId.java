package galacticwars.clonewars.kingdom;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Stable identity for a retryable kingdom gameplay action. */
public record KingdomActionId(String value) {
    public KingdomActionId {
        Objects.requireNonNull(value, "value");
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty() || !value.matches("[a-z0-9_:.\\-/]+")) {
            throw new IllegalArgumentException("Invalid kingdom action id: " + value);
        }
    }

    public static KingdomActionId of(String kind, Object... identityParts) {
        Objects.requireNonNull(identityParts, "identityParts");
        StringBuilder value = new StringBuilder(normalizePart(kind));
        for (Object identityPart : identityParts) {
            value.append('/').append(normalizePart(Objects.toString(identityPart, "")));
        }
        return new KingdomActionId(value.toString());
    }

    public UUID progressionEventId() {
        return UUID.nameUUIDFromBytes(("galacticwars:kingdom_action/" + value)
                .getBytes(StandardCharsets.UTF_8));
    }

    private static String normalizePart(String value) {
        String normalized = Objects.requireNonNull(value, "identity part")
                .trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        if (normalized.isEmpty() || !normalized.matches("[a-z0-9_:.\\-]+")) {
            throw new IllegalArgumentException("Invalid kingdom action identity part: " + value);
        }
        return normalized;
    }
}
