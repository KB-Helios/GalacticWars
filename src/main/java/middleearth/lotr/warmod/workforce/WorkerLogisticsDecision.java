package middleearth.lotr.warmod.workforce;

import java.util.Locale;
import java.util.Objects;

public record WorkerLogisticsDecision(
        WorkerResourceAction action,
        String itemId,
        int quantity,
        WorkerLogisticsRoute route,
        String reasonCode
) {
    public WorkerLogisticsDecision {
        Objects.requireNonNull(action, "action");
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

    public static WorkerLogisticsDecision idle(String itemId, WorkerLogisticsRoute route, String reasonCode) {
        return new WorkerLogisticsDecision(WorkerResourceAction.IDLE, itemId, 0, route, reasonCode);
    }

    public WorkerResourceDecision asResourceDecision() {
        return new WorkerResourceDecision(action, itemId, quantity, reasonCode);
    }

    private static String normalizeItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return "";
        }
        return itemId.trim().toLowerCase(Locale.ROOT);
    }
}
