package galacticwars.clonewars.workforce;

import java.util.Locale;
import java.util.Objects;

public record CourierTransferAction(CourierTransferType type, String itemId, int quantity) {
    public CourierTransferAction {
        Objects.requireNonNull(type, "type");
        itemId = itemId == null ? "" : itemId.trim().toLowerCase(Locale.ROOT);
        if ((type == CourierTransferType.TAKE || type == CourierTransferType.PUT
                || type == CourierTransferType.FILL) && itemId.isBlank()) {
            throw new IllegalArgumentException("filtered courier action requires an item id");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("courier quantity cannot be negative");
        }
    }
}
