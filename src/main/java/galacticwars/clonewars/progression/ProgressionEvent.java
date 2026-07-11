package galacticwars.clonewars.progression;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record ProgressionEvent(
        UUID id,
        UUID playerId,
        ProgressionEventType type,
        String subjectId,
        int amount
) {
    public ProgressionEvent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(type, "type");
        subjectId = normalize(subjectId);
        if (type != ProgressionEventType.CREDIT_TRANSACTION && amount < 0) {
            throw new IllegalArgumentException("Only credit transactions may have a negative amount");
        }
    }

    private static String normalize(String value) {
        Objects.requireNonNull(value, "subjectId");
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || !normalized.matches("[a-z0-9_:.\\-/]+")) {
            throw new IllegalArgumentException("Invalid progression subject: " + value);
        }
        return normalized;
    }
}
