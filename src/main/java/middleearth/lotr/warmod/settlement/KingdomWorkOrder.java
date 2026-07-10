package middleearth.lotr.warmod.settlement;

import java.util.Locale;
import java.util.Objects;

import middleearth.lotr.warmod.workforce.WorkerProfession;

public record KingdomWorkOrder(
        KingdomWorkOrderType type,
        WorkerProfession profession,
        String itemId,
        int quantity,
        String reasonCode
) {
    public KingdomWorkOrder {
        Objects.requireNonNull(type, "type");
        itemId = normalizeItemId(itemId);
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity cannot be negative");
        }
        Objects.requireNonNull(reasonCode, "reasonCode");
        reasonCode = reasonCode.trim();
        if (reasonCode.isEmpty()) {
            throw new IllegalArgumentException("reasonCode cannot be blank");
        }
    }

    public static KingdomWorkOrder idle(String reasonCode) {
        return new KingdomWorkOrder(KingdomWorkOrderType.IDLE, null, "", 0, reasonCode);
    }

    public static KingdomWorkOrder recruit(WorkerProfession profession, String reasonCode) {
        return new KingdomWorkOrder(KingdomWorkOrderType.RECRUIT_WORKER, profession, "", 1, reasonCode);
    }

    private static String normalizeItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return "";
        }
        return itemId.trim().toLowerCase(Locale.ROOT);
    }
}
