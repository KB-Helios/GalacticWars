package middleearth.lotr.warmod.kingdom;

public record CommanderPolicy(
        boolean automaticRecruitment,
        int targetRecruitCount,
        int maximumCampaignSpend,
        int minimumTreasuryReserve,
        int campaignDelayTicks
) {
    public CommanderPolicy {
        if (targetRecruitCount < 0) {
            throw new IllegalArgumentException("targetRecruitCount cannot be negative");
        }
        if (maximumCampaignSpend < 0) {
            throw new IllegalArgumentException("maximumCampaignSpend cannot be negative");
        }
        if (minimumTreasuryReserve < 0) {
            throw new IllegalArgumentException("minimumTreasuryReserve cannot be negative");
        }
        if (campaignDelayTicks < 20) {
            throw new IllegalArgumentException("campaignDelayTicks must be at least 20");
        }
    }

    public static CommanderPolicy defaults() {
        return new CommanderPolicy(false, 4, 64, 16, 24000);
    }
}
