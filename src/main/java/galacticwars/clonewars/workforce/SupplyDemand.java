package galacticwars.clonewars.workforce;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record SupplyDemand(
        UUID id,
        SupplyCategory category,
        String itemId,
        int quantity,
        int fulfilledQuantity,
        int priority,
        String sourceId
) {
    public SupplyDemand {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(category, "category");
        itemId = normalize(itemId, "itemId");
        sourceId = normalize(sourceId, "sourceId");
        if (quantity <= 0 || fulfilledQuantity < 0 || fulfilledQuantity > quantity) {
            throw new IllegalArgumentException("invalid supply demand quantities");
        }
        if (priority < 0 || priority > 100) {
            throw new IllegalArgumentException("priority must be between 0 and 100");
        }
    }

    public int outstandingQuantity() {
        return quantity - fulfilledQuantity;
    }

    public boolean complete() {
        return outstandingQuantity() == 0;
    }

    public SupplyDemand fulfill(int delivered) {
        if (delivered <= 0 || delivered > outstandingQuantity()) {
            throw new IllegalArgumentException("invalid delivered quantity");
        }
        return new SupplyDemand(id, category, itemId, quantity,
                Math.addExact(fulfilledQuantity, delivered), priority, sourceId);
    }

    private static String normalize(String value, String label) {
        String normalized = Objects.requireNonNull(value, label).trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || !normalized.matches("[a-z0-9_:.\\-/]+")) {
            throw new IllegalArgumentException("invalid " + label + ": " + value);
        }
        return normalized;
    }
}
