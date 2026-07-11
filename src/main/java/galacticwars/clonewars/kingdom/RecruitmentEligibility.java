package galacticwars.clonewars.kingdom;

import java.util.Objects;

public record RecruitmentEligibility(boolean accepted, String reasonCode) {
    public RecruitmentEligibility {
        reasonCode = Objects.requireNonNull(reasonCode, "reasonCode").trim();
        if (reasonCode.isEmpty()) {
            throw new IllegalArgumentException("reasonCode cannot be blank");
        }
    }

    public static RecruitmentEligibility acceptedResult() {
        return new RecruitmentEligibility(true, "accepted");
    }

    public static RecruitmentEligibility rejected(String reasonCode) {
        return new RecruitmentEligibility(false, reasonCode);
    }
}
