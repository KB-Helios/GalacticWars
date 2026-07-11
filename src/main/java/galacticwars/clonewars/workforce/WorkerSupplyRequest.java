package galacticwars.clonewars.workforce;

import java.util.Locale;
import java.util.Objects;

public record WorkerSupplyRequest(String itemId, int quantity) {
    public WorkerSupplyRequest {
        itemId = normalizeItemId(itemId);
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }

    private static String normalizeItemId(String itemId) {
        Objects.requireNonNull(itemId, "itemId");
        String normalized = itemId.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("itemId cannot be blank");
        }
        return normalized;
    }
}
