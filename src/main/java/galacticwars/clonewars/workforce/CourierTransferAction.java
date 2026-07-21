package galacticwars.clonewars.workforce;

import java.util.Locale;
import java.util.Objects;

public record CourierTransferAction(CourierTransferType type, String itemId, int quantity) {
    public CourierTransferAction {
        Objects.requireNonNull(type, "type");
        itemId = itemId == null ? "" : itemId.trim().toLowerCase(Locale.ROOT);
        if (type.hasItemFilter() && itemId.isBlank()) {
            throw new IllegalArgumentException("filtered courier action requires an item id");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("courier quantity cannot be negative");
        }
        if (type.waitsAtWaypoint() && quantity == 0) {
            throw new IllegalArgumentException("courier wait duration must be positive");
        }
    }

    public static CourierTransferAction take(String itemId, int quantity) {
        return new CourierTransferAction(CourierTransferType.TAKE, itemId, positiveQuantity(quantity));
    }

    public static CourierTransferAction put(String itemId, int quantity) {
        return new CourierTransferAction(CourierTransferType.PUT, itemId, positiveQuantity(quantity));
    }

    public static CourierTransferAction takeAny(String itemId) {
        return new CourierTransferAction(CourierTransferType.TAKE_ANY, itemId, 0);
    }

    public static CourierTransferAction putAny(String itemId) {
        return new CourierTransferAction(CourierTransferType.PUT_ANY, itemId, 0);
    }

    public static CourierTransferAction takeAll() {
        return new CourierTransferAction(CourierTransferType.TAKE_ALL, "", 0);
    }

    public static CourierTransferAction putAll() {
        return new CourierTransferAction(CourierTransferType.PUT_ALL, "", 0);
    }

    public static CourierTransferAction putFill(String itemId, int quantity) {
        return new CourierTransferAction(CourierTransferType.PUT_FILL, itemId, positiveQuantity(quantity));
    }

    public static CourierTransferAction takeFill(String itemId, int quantity) {
        return new CourierTransferAction(CourierTransferType.TAKE_FILL, itemId, positiveQuantity(quantity));
    }

    public static CourierTransferAction waitTicks(int ticks) {
        return new CourierTransferAction(CourierTransferType.WAIT, "", positiveQuantity(ticks));
    }

    public CourierTransferType effectiveType() {
        return type.canonical();
    }

    public int dwellTicks() {
        return type.waitsAtWaypoint() ? quantity : 0;
    }

    private static int positiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("courier quantity must be positive");
        }
        return quantity;
    }
}
