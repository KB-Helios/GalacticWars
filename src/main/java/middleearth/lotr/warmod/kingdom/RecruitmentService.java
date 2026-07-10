package middleearth.lotr.warmod.kingdom;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class RecruitmentService {
    private RecruitmentService() {
    }

    public static RecruitmentEligibility evaluateDirectHire(
            KingdomRecord kingdom,
            String unitFactionId,
            int cost,
            int availableFunds,
            boolean upkeepPaid,
            Set<String> alliedFactionIds
    ) {
        Objects.requireNonNull(kingdom, "kingdom");
        Objects.requireNonNull(unitFactionId, "unitFactionId");
        Objects.requireNonNull(alliedFactionIds, "alliedFactionIds");
        if (!upkeepPaid) {
            return RecruitmentEligibility.rejected("upkeep_unpaid");
        }
        RecruitmentEligibility eligibility = evaluateSettlementEligibility(
                kingdom, unitFactionId, alliedFactionIds);
        if (!eligibility.accepted()) {
            return eligibility;
        }
        if (cost < 0 || availableFunds < cost) {
            return RecruitmentEligibility.rejected("insufficient_funds");
        }
        return RecruitmentEligibility.acceptedResult();
    }

    public static RecruitmentCampaignDecision evaluateCommanderCampaign(
            KingdomRecord kingdom,
            UUID actingCommander,
            String unitId,
            String unitFactionId,
            String professionId,
            int cost,
            int treasuryEmeralds,
            long currentGameTime,
            Set<String> alliedFactionIds
    ) {
        Objects.requireNonNull(kingdom, "kingdom");
        Objects.requireNonNull(actingCommander, "actingCommander");
        Objects.requireNonNull(alliedFactionIds, "alliedFactionIds");
        SettlementRecord settlement = kingdom.settlement();
        if (settlement.commanderId().filter(actingCommander::equals).isEmpty()) {
            return RecruitmentCampaignDecision.rejected("not_commander");
        }
        CommanderPolicy policy = settlement.commanderPolicy();
        if (!policy.automaticRecruitment()) {
            return RecruitmentCampaignDecision.rejected("automatic_recruitment_disabled");
        }
        if (settlement.hasActiveCampaign()) {
            return RecruitmentCampaignDecision.rejected("campaign_in_progress");
        }
        RecruitmentEligibility eligibility = evaluateSettlementEligibility(
                kingdom, unitFactionId, alliedFactionIds);
        if (!eligibility.accepted()) {
            return RecruitmentCampaignDecision.rejected(eligibility.reasonCode());
        }
        if (settlement.recruitIds().size() >= policy.targetRecruitCount()) {
            return RecruitmentCampaignDecision.rejected("housing_or_target_full");
        }
        if (cost < 0 || cost > policy.maximumCampaignSpend()) {
            return RecruitmentCampaignDecision.rejected("campaign_budget_exceeded");
        }
        if (treasuryEmeralds - cost < policy.minimumTreasuryReserve()) {
            return RecruitmentCampaignDecision.rejected("treasury_reserve_required");
        }
        RecruitmentCampaign campaign = new RecruitmentCampaign(
                UUID.randomUUID(),
                unitId,
                professionId == null ? "" : professionId,
                cost,
                currentGameTime + policy.campaignDelayTicks(),
                RecruitmentCampaignState.RESERVED,
                "reserved");
        return RecruitmentCampaignDecision.accepted(campaign);
    }

    private static RecruitmentEligibility evaluateSettlementEligibility(
            KingdomRecord kingdom,
            String unitFactionId,
            Set<String> alliedFactionIds
    ) {
        if (!kingdom.settlement().hasHousingSpace()) {
            return RecruitmentEligibility.rejected("housing_full");
        }
        HashSet<String> allowedFactions = new HashSet<>(alliedFactionIds);
        allowedFactions.add(kingdom.factionId());
        if (!allowedFactions.contains(unitFactionId)) {
            return RecruitmentEligibility.rejected("hostile_faction");
        }
        return RecruitmentEligibility.acceptedResult();
    }
}
