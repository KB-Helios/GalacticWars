package middleearth.lotr.warmod.kingdom;

import java.util.Objects;
import java.util.Optional;

public record RecruitmentCampaignDecision(
        boolean accepted,
        String reasonCode,
        Optional<RecruitmentCampaign> campaign
) {
    public RecruitmentCampaignDecision {
        Objects.requireNonNull(reasonCode, "reasonCode");
        campaign = campaign == null ? Optional.empty() : campaign;
        if (accepted != campaign.isPresent()) {
            throw new IllegalArgumentException("accepted decisions must contain exactly one campaign");
        }
    }

    public static RecruitmentCampaignDecision rejected(String reasonCode) {
        return new RecruitmentCampaignDecision(false, reasonCode, Optional.empty());
    }

    public static RecruitmentCampaignDecision accepted(RecruitmentCampaign campaign) {
        return new RecruitmentCampaignDecision(true, "accepted", Optional.of(campaign));
    }
}
