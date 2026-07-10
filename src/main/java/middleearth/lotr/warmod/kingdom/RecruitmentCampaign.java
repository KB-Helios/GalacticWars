package middleearth.lotr.warmod.kingdom;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record RecruitmentCampaign(
        UUID id,
        String unitId,
        String professionId,
        int reservedCost,
        long readyGameTime,
        RecruitmentCampaignState state,
        String reasonCode,
        boolean refundPending
) {
    public RecruitmentCampaign(
            UUID id,
            String unitId,
            String professionId,
            int reservedCost,
            long readyGameTime,
            RecruitmentCampaignState state,
            String reasonCode
    ) {
        this(id, unitId, professionId, reservedCost, readyGameTime, state, reasonCode, false);
    }

    public RecruitmentCampaign {
        Objects.requireNonNull(id, "id");
        unitId = normalizeId(unitId, "unitId");
        professionId = professionId == null ? "" : professionId.trim().toLowerCase(Locale.ROOT);
        if (reservedCost < 0) {
            throw new IllegalArgumentException("reservedCost cannot be negative");
        }
        if (readyGameTime < 0) {
            throw new IllegalArgumentException("readyGameTime cannot be negative");
        }
        Objects.requireNonNull(state, "state");
        reasonCode = requireText(reasonCode, "reasonCode");
        if (refundPending && (state != RecruitmentCampaignState.CANCELLED || reservedCost == 0)) {
            throw new IllegalArgumentException("refundPending requires a cancelled campaign with reserved funds");
        }
    }

    public boolean active() {
        return state == RecruitmentCampaignState.RESERVED;
    }

    public RecruitmentCampaign complete() {
        return new RecruitmentCampaign(id, unitId, professionId, reservedCost, readyGameTime,
                RecruitmentCampaignState.COMPLETE, "complete", false);
    }

    public RecruitmentCampaign cancel(String reasonCode) {
        return new RecruitmentCampaign(id, unitId, professionId, reservedCost, readyGameTime,
                RecruitmentCampaignState.CANCELLED, reasonCode, reservedCost > 0);
    }

    public RecruitmentCampaign applyRefund(int refundedAmount) {
        if (refundedAmount < 0 || refundedAmount > reservedCost) {
            throw new IllegalArgumentException("refundedAmount must be between zero and the reserved cost");
        }
        if (!refundPending() || refundedAmount == 0) {
            return this;
        }
        return new RecruitmentCampaign(id, unitId, professionId, reservedCost - refundedAmount,
                readyGameTime, state, reasonCode, reservedCost - refundedAmount > 0);
    }

    public RecruitmentCampaign delay(long ticks) {
        if (!active() || ticks <= 0) {
            return this;
        }
        return new RecruitmentCampaign(id, unitId, professionId, reservedCost,
                Math.addExact(readyGameTime, ticks), state, "settlement_unloaded", refundPending);
    }

    private static String normalizeId(String value, String label) {
        return requireText(value, label).toLowerCase(Locale.ROOT);
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        String result = value.trim();
        if (result.isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return result;
    }
}
