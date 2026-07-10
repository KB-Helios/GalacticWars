package middleearth.lotr.warmod.kingdom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.UUIDUtil;

final class KingdomCodecs {
    static final Codec<CommanderPolicy> COMMANDER_POLICY = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("automatic_recruitment", false).forGetter(CommanderPolicy::automaticRecruitment),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("target_recruit_count", 4).forGetter(CommanderPolicy::targetRecruitCount),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("maximum_campaign_spend", 64).forGetter(CommanderPolicy::maximumCampaignSpend),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("minimum_treasury_reserve", 16).forGetter(CommanderPolicy::minimumTreasuryReserve),
            Codec.intRange(20, Integer.MAX_VALUE).optionalFieldOf("campaign_delay_ticks", 24000).forGetter(CommanderPolicy::campaignDelayTicks)
    ).apply(instance, CommanderPolicy::new));

    static final Codec<RecruitmentCampaign> RECRUITMENT_CAMPAIGN = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(RecruitmentCampaign::id),
            Codec.STRING.fieldOf("unit_id").forGetter(RecruitmentCampaign::unitId),
            Codec.STRING.optionalFieldOf("profession_id", "").forGetter(RecruitmentCampaign::professionId),
            Codec.INT.fieldOf("reserved_cost").forGetter(RecruitmentCampaign::reservedCost),
            Codec.LONG.fieldOf("ready_game_time").forGetter(RecruitmentCampaign::readyGameTime),
            Codec.STRING.xmap(RecruitmentCampaignState::byId, RecruitmentCampaignState::id)
                    .fieldOf("state").forGetter(RecruitmentCampaign::state),
            Codec.STRING.optionalFieldOf("reason_code", "reserved").forGetter(RecruitmentCampaign::reasonCode),
            Codec.BOOL.optionalFieldOf("refund_pending", false).forGetter(RecruitmentCampaign::refundPending)
    ).apply(instance, RecruitmentCampaign::new));

    static final Codec<WorksiteRecord> WORKSITE = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(WorksiteRecord::id),
            Codec.STRING.fieldOf("type").forGetter(WorksiteRecord::type),
            Codec.STRING.fieldOf("dimension").forGetter(WorksiteRecord::dimensionId),
            Codec.INT.fieldOf("x").forGetter(WorksiteRecord::x),
            Codec.INT.fieldOf("y").forGetter(WorksiteRecord::y),
            Codec.INT.fieldOf("z").forGetter(WorksiteRecord::z),
            Codec.intRange(1, 32).fieldOf("radius").forGetter(WorksiteRecord::radius),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("capacity", 1).forGetter(WorksiteRecord::capacity)
    ).apply(instance, WorksiteRecord::new));

    static final Codec<BuildProject> BUILD_PROJECT = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(BuildProject::id),
            Codec.STRING.fieldOf("blueprint_id").forGetter(BuildProject::blueprintId),
            Codec.STRING.fieldOf("dimension").forGetter(BuildProject::dimensionId),
            Codec.INT.fieldOf("origin_x").forGetter(BuildProject::originX),
            Codec.INT.fieldOf("origin_y").forGetter(BuildProject::originY),
            Codec.INT.fieldOf("origin_z").forGetter(BuildProject::originZ),
            Codec.INT.optionalFieldOf("rotation_steps", 0).forGetter(BuildProject::rotationSteps),
            Codec.INT.listOf().optionalFieldOf("completed_placements", List.of()).forGetter(BuildProject::completedPlacements),
            Codec.STRING.optionalFieldOf("blocked_reason", "").forGetter(BuildProject::blockedReason)
    ).apply(instance, BuildProject::new));

    static final Codec<WorkOrder> WORK_ORDER = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(WorkOrder::id),
            Codec.STRING.fieldOf("type").forGetter(WorkOrder::type),
            UUIDUtil.CODEC.optionalFieldOf("assigned_recruit_id").forGetter(WorkOrder::assignedRecruitId),
            Codec.STRING.optionalFieldOf("state", "queued").forGetter(WorkOrder::state),
            Codec.STRING.fieldOf("dimension").forGetter(WorkOrder::dimensionId),
            Codec.INT.fieldOf("target_x").forGetter(WorkOrder::targetX),
            Codec.INT.fieldOf("target_y").forGetter(WorkOrder::targetY),
            Codec.INT.fieldOf("target_z").forGetter(WorkOrder::targetZ),
            Codec.STRING.optionalFieldOf("resource_id", "").forGetter(WorkOrder::resourceId),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("quantity", 0).forGetter(WorkOrder::quantity)
    ).apply(instance, WorkOrder::new));

    static final Codec<SettlementRecord> SETTLEMENT = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(SettlementRecord::id),
            Codec.STRING.fieldOf("dimension").forGetter(SettlementRecord::dimensionId),
            Codec.INT.fieldOf("hall_x").forGetter(SettlementRecord::hallX),
            Codec.INT.fieldOf("hall_y").forGetter(SettlementRecord::hallY),
            Codec.INT.fieldOf("hall_z").forGetter(SettlementRecord::hallZ),
            Codec.intRange(8, 256).optionalFieldOf("claim_radius", 48).forGetter(SettlementRecord::claimRadius),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("housing_capacity", 4).forGetter(SettlementRecord::housingCapacity),
            UUIDUtil.CODEC.listOf().optionalFieldOf("recruit_ids", List.of()).forGetter(SettlementRecord::recruitIds),
            UUIDUtil.CODEC.optionalFieldOf("commander_id").forGetter(SettlementRecord::commanderId),
            COMMANDER_POLICY.optionalFieldOf("commander_policy", CommanderPolicy.defaults()).forGetter(SettlementRecord::commanderPolicy),
            WORKSITE.listOf().optionalFieldOf("worksites", List.of()).forGetter(SettlementRecord::worksites),
            BUILD_PROJECT.listOf().optionalFieldOf("build_projects", List.of()).forGetter(SettlementRecord::buildProjects),
            WORK_ORDER.listOf().optionalFieldOf("work_orders", List.of()).forGetter(SettlementRecord::workOrders),
            RECRUITMENT_CAMPAIGN.listOf().optionalFieldOf("recruitment_campaigns", List.of()).forGetter(SettlementRecord::recruitmentCampaigns),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("revision", 0).forGetter(SettlementRecord::revision)
    ).apply(instance, SettlementRecord::new));

    static final Codec<KingdomRecord> KINGDOM_RECORD = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(KingdomRecord::id),
            UUIDUtil.CODEC.fieldOf("owner_id").forGetter(KingdomRecord::ownerId),
            Codec.STRING.fieldOf("faction_id").forGetter(KingdomRecord::factionId),
            SETTLEMENT.fieldOf("settlement").forGetter(KingdomRecord::settlement)
    ).apply(instance, KingdomRecord::new));

    private KingdomCodecs() {
    }
}
