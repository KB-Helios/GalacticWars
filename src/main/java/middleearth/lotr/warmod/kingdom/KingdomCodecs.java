package middleearth.lotr.warmod.kingdom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import middleearth.lotr.warmod.army.ArmyFormation;
import middleearth.lotr.warmod.army.ArmyGroupLifecycleState;
import middleearth.lotr.warmod.army.ArmyGroupOrder;
import middleearth.lotr.warmod.army.ArmyGroupRecord;
import middleearth.lotr.warmod.army.ArmyGroupSimulation;
import middleearth.lotr.warmod.army.ArmyLocation;
import middleearth.lotr.warmod.army.ArmyMemberSnapshot;
import middleearth.lotr.warmod.army.ArmySnapshotEquipment;
import middleearth.lotr.warmod.army.ArmyCommandType;
import middleearth.lotr.warmod.recruitment.RecruitDuty;
import middleearth.lotr.warmod.workforce.WorkerProfession;
import net.minecraft.core.UUIDUtil;

final class KingdomCodecs {
    static final Codec<ArmyLocation> ARMY_LOCATION = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("dimension").forGetter(ArmyLocation::dimensionId),
            Codec.DOUBLE.fieldOf("x").forGetter(ArmyLocation::x),
            Codec.DOUBLE.fieldOf("y").forGetter(ArmyLocation::y),
            Codec.DOUBLE.fieldOf("z").forGetter(ArmyLocation::z)
    ).apply(instance, ArmyLocation::new));

    static final Codec<ArmyGroupOrder> ARMY_GROUP_ORDER = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(value -> ArmyCommandType.valueOf(value.toUpperCase()), value -> value.name().toLowerCase())
                    .fieldOf("type").forGetter(ArmyGroupOrder::type),
            ARMY_LOCATION.optionalFieldOf("target_position").forGetter(ArmyGroupOrder::targetPosition),
            UUIDUtil.CODEC.optionalFieldOf("target_entity_id").forGetter(ArmyGroupOrder::targetEntityId),
            Codec.STRING.xmap(value -> ArmyFormation.valueOf(value.toUpperCase()), value -> value.name().toLowerCase())
                    .optionalFieldOf("formation", ArmyFormation.LINE).forGetter(ArmyGroupOrder::formation),
            Codec.intRange(1, 8).optionalFieldOf("spacing", 2).forGetter(ArmyGroupOrder::spacing)
    ).apply(instance, ArmyGroupOrder::new));

    static final Codec<ArmySnapshotEquipment> ARMY_SNAPSHOT_EQUIPMENT = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("main_hand", "").forGetter(ArmySnapshotEquipment::mainHand),
            Codec.STRING.optionalFieldOf("head", "").forGetter(ArmySnapshotEquipment::head),
            Codec.STRING.optionalFieldOf("chest", "").forGetter(ArmySnapshotEquipment::chest),
            Codec.STRING.optionalFieldOf("legs", "").forGetter(ArmySnapshotEquipment::legs),
            Codec.STRING.optionalFieldOf("feet", "").forGetter(ArmySnapshotEquipment::feet)
    ).apply(instance, ArmySnapshotEquipment::new));

    static final Codec<ArmyMemberSnapshot> ARMY_MEMBER_SNAPSHOT = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("recruit_id").forGetter(ArmyMemberSnapshot::recruitId),
            Codec.STRING.fieldOf("entity_type_id").forGetter(ArmyMemberSnapshot::entityTypeId),
            Codec.STRING.fieldOf("unit_id").forGetter(ArmyMemberSnapshot::unitId),
            UUIDUtil.CODEC.fieldOf("owner_id").forGetter(ArmyMemberSnapshot::ownerId),
            UUIDUtil.CODEC.fieldOf("kingdom_id").forGetter(ArmyMemberSnapshot::kingdomId),
            Codec.STRING.xmap(RecruitDuty::byId, RecruitDuty::id).fieldOf("duty").forGetter(ArmyMemberSnapshot::duty),
            Codec.FLOAT.fieldOf("health").forGetter(ArmyMemberSnapshot::health),
            Codec.intRange(0, 100).optionalFieldOf("morale", 100).forGetter(ArmyMemberSnapshot::morale),
            Codec.intRange(0, 100).optionalFieldOf("hunger", 100).forGetter(ArmyMemberSnapshot::hunger),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("unpaid_ticks", 0).forGetter(ArmyMemberSnapshot::unpaidTicks),
            Codec.LONG.optionalFieldOf("generation", 0L).forGetter(ArmyMemberSnapshot::generation),
            ARMY_SNAPSHOT_EQUIPMENT.fieldOf("equipment").forGetter(ArmyMemberSnapshot::equipment),
            Codec.STRING.optionalFieldOf("custom_name", "").forGetter(ArmyMemberSnapshot::customName)
    ).apply(instance, ArmyMemberSnapshot::new));

    static final Codec<ArmyGroupSimulation> ARMY_GROUP_SIMULATION = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(value -> ArmyGroupLifecycleState.valueOf(value.toUpperCase()), value -> value.name().toLowerCase())
                    .optionalFieldOf("state", ArmyGroupLifecycleState.LIVE).forGetter(ArmyGroupSimulation::lifecycleState),
            ARMY_LOCATION.fieldOf("anchor").forGetter(ArmyGroupSimulation::anchor),
            Codec.LONG.optionalFieldOf("last_simulation_game_time", 0L).forGetter(ArmyGroupSimulation::lastSimulationGameTime),
            Codec.LONG.optionalFieldOf("revision", 0L).forGetter(ArmyGroupSimulation::revision),
            Codec.LONG.optionalFieldOf("snapshot_generation", 0L).forGetter(ArmyGroupSimulation::snapshotGeneration),
            Codec.STRING.optionalFieldOf("blocked_reason", "").forGetter(ArmyGroupSimulation::blockedReason)
    ).apply(instance, ArmyGroupSimulation::new));

    static final Codec<ArmyGroupRecord> ARMY_GROUP = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(ArmyGroupRecord::id),
            UUIDUtil.CODEC.fieldOf("owner_id").forGetter(ArmyGroupRecord::ownerId),
            UUIDUtil.CODEC.fieldOf("kingdom_id").forGetter(ArmyGroupRecord::kingdomId),
            UUIDUtil.CODEC.optionalFieldOf("commander_id").forGetter(ArmyGroupRecord::commanderId),
            UUIDUtil.CODEC.listOf().optionalFieldOf("member_ids", List.of()).forGetter(ArmyGroupRecord::memberIds),
            ARMY_GROUP_ORDER.fieldOf("order").forGetter(ArmyGroupRecord::order),
            ARMY_GROUP_SIMULATION.fieldOf("simulation").forGetter(ArmyGroupRecord::simulation),
            ARMY_MEMBER_SNAPSHOT.listOf().optionalFieldOf("snapshots", List.of()).forGetter(ArmyGroupRecord::snapshots)
    ).apply(instance, ArmyGroupRecord::new));

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

    static final Codec<StorageEndpoint> STORAGE_ENDPOINT = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("dimension").forGetter(StorageEndpoint::dimensionId),
            Codec.INT.fieldOf("x").forGetter(StorageEndpoint::x),
            Codec.INT.fieldOf("y").forGetter(StorageEndpoint::y),
            Codec.INT.fieldOf("z").forGetter(StorageEndpoint::z),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("slots").forGetter(StorageEndpoint::slots)
    ).apply(instance, StorageEndpoint::new));

    static final Codec<WorksiteRecord> WORKSITE = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(WorksiteRecord::id),
            Codec.STRING.fieldOf("type").forGetter(WorksiteRecord::type),
            Codec.STRING.fieldOf("dimension").forGetter(WorksiteRecord::dimensionId),
            Codec.INT.fieldOf("x").forGetter(WorksiteRecord::x),
            Codec.INT.fieldOf("y").forGetter(WorksiteRecord::y),
            Codec.INT.fieldOf("z").forGetter(WorksiteRecord::z),
            Codec.intRange(1, 32).fieldOf("radius").forGetter(WorksiteRecord::radius),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("capacity", 1).forGetter(WorksiteRecord::capacity),
            Codec.STRING.xmap(value -> WorkerProfession.byId(value).orElseThrow(), WorkerProfession::id)
                    .listOf().optionalFieldOf("accepted_professions", List.of()).forGetter(WorksiteRecord::acceptedProfessions),
            UUIDUtil.CODEC.optionalFieldOf("source_project_id").forGetter(WorksiteRecord::sourceProjectId),
            UUIDUtil.CODEC.listOf().optionalFieldOf("assignment_ids", List.of()).forGetter(WorksiteRecord::assignmentIds),
            STORAGE_ENDPOINT.listOf().optionalFieldOf("storage_endpoints", List.of()).forGetter(WorksiteRecord::storageEndpoints)
    ).apply(instance, WorksiteRecord::new));

    static final Codec<BuildProject> BUILD_PROJECT = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(BuildProject::id),
            Codec.STRING.fieldOf("blueprint_id").forGetter(BuildProject::blueprintId),
            Codec.STRING.fieldOf("dimension").forGetter(BuildProject::dimensionId),
            Codec.INT.fieldOf("origin_x").forGetter(BuildProject::originX),
            Codec.INT.fieldOf("origin_y").forGetter(BuildProject::originY),
            Codec.INT.fieldOf("origin_z").forGetter(BuildProject::originZ),
            Codec.INT.optionalFieldOf("rotation_steps", 0).forGetter(BuildProject::rotationSteps),
            Codec.STRING.optionalFieldOf("definition_hash")
                    .forGetter(project -> Optional.of(project.definitionHash())),
            Codec.INT.listOf().optionalFieldOf("completed_placements", List.of()).forGetter(BuildProject::completedPlacements),
            Codec.STRING.xmap(BuildProjectState::byId, BuildProjectState::id)
                    .optionalFieldOf("state").forGetter(project -> Optional.of(project.state())),
            Codec.STRING.optionalFieldOf("blocked_reason", "").forGetter(BuildProject::blockedReason),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("revision", 0).forGetter(BuildProject::revision)
    ).apply(instance, BuildProject::fromPersistence));

    static final Codec<WorkOrder> WORK_ORDER = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(WorkOrder::id),
            Codec.STRING.xmap(WorkOrderType::byId, WorkOrderType::id).fieldOf("type").forGetter(WorkOrder::type),
            UUIDUtil.CODEC.optionalFieldOf("assigned_recruit_id").forGetter(WorkOrder::assignedRecruitId),
            Codec.STRING.xmap(WorkOrderState::byId, WorkOrderState::id)
                    .optionalFieldOf("state", WorkOrderState.QUEUED).forGetter(WorkOrder::state),
            UUIDUtil.CODEC.optionalFieldOf("worksite_id").forGetter(WorkOrder::worksiteId),
            UUIDUtil.CODEC.optionalFieldOf("project_id").forGetter(WorkOrder::projectId),
            Codec.STRING.fieldOf("dimension").forGetter(WorkOrder::dimensionId),
            Codec.INT.fieldOf("target_x").forGetter(WorkOrder::targetX),
            Codec.INT.fieldOf("target_y").forGetter(WorkOrder::targetY),
            Codec.INT.fieldOf("target_z").forGetter(WorkOrder::targetZ),
            Codec.STRING.optionalFieldOf("resource_id", "").forGetter(WorkOrder::resourceId),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("quantity", 1).forGetter(WorkOrder::quantity),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("completed_quantity", 0).forGetter(WorkOrder::completedQuantity),
            Codec.STRING.optionalFieldOf("blocked_reason", "").forGetter(WorkOrder::blockedReason),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("revision", 0).forGetter(WorkOrder::revision)
    ).apply(instance, WorkOrder::new));

    static final Codec<SettlementRewards> SETTLEMENT_REWARDS = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("external_storage_slots", 0)
                    .forGetter(SettlementRewards::externalStorageSlots),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("commander_slots", 0)
                    .forGetter(SettlementRewards::commanderSlots)
    ).apply(instance, SettlementRewards::new));

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
            SETTLEMENT_REWARDS.optionalFieldOf("rewards", SettlementRewards.none()).forGetter(SettlementRecord::rewards),
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
