package galacticwars.clonewars.settlement;

import java.util.Objects;

public record KingdomBaseBuildDecision(
        KingdomBaseBuildAction action,
        String itemId,
        int quantity,
        String reasonCode,
        BaseBlockPlacement placement
) {
    public KingdomBaseBuildDecision {
        Objects.requireNonNull(action, "action");
        itemId = itemId == null ? "" : itemId.trim().toLowerCase();
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity cannot be negative");
        }
        Objects.requireNonNull(reasonCode, "reasonCode");
        reasonCode = reasonCode.trim();
        if (reasonCode.isEmpty()) {
            throw new IllegalArgumentException("reasonCode cannot be blank");
        }
    }

    public static KingdomBaseBuildDecision gather(String itemId, int quantity) {
        return new KingdomBaseBuildDecision(
                KingdomBaseBuildAction.GATHER_SUPPLIES,
                itemId,
                quantity,
                "missing_supplies",
                null);
    }

    public static KingdomBaseBuildDecision place(BaseBlockPlacement placement) {
        return new KingdomBaseBuildDecision(
                KingdomBaseBuildAction.PLACE_BLOCK,
                placement.itemId(),
                1,
                "ready_to_build",
                placement);
    }

    public static KingdomBaseBuildDecision complete() {
        return new KingdomBaseBuildDecision(KingdomBaseBuildAction.COMPLETE, "", 0, "base_complete", null);
    }
}
