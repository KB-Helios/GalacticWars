package galacticwars.clonewars.kingdom;

import galacticwars.clonewars.army.ArmyGroupRecord;
import galacticwars.clonewars.progression.GalacticProgressionCoordinator;
import galacticwars.clonewars.progression.LaunchContentCatalog;
import galacticwars.clonewars.progression.ProgressionEventType;
import galacticwars.clonewars.progression.ProgressionState;
import galacticwars.clonewars.settlement.KingdomBaseBlueprint;

import java.util.Comparator;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Immutable, client-safe view of the server-owned Command Center state.
 *
 * <p>The dashboard deliberately contains identifiers and display-safe primitive values rather
 * than live game objects. This keeps the client informative without making it authoritative.</p>
 */
public record CommandCenterDashboardState(
        long generatedGameTime,
        boolean kingdomAvailable,
        UUID actorId,
        UUID kingdomId,
        String factionId,
        String actorRole,
        int treasuryCredits,
        int pendingRewardCredits,
        boolean upkeepPaid,
        int recruitCount,
        int housingCapacity,
        int settlementCount,
        int claimCount,
        boolean commanderAssigned,
        boolean campaignVictory,
        int veteranVehicleDeployments,
        int veteranTrades,
        int veteranRegionCaptures,
        List<UUID> commandCandidateIds,
        List<ClaimSummary> claims,
        List<SquadSummary> squads,
        List<CombatTargetSummary> combatTargets,
        List<BlueprintSummary> blueprints,
        List<UUID> constructionBuilderIds,
        List<BuildSummary> builds,
        List<WorkOrderSummary> workOrders,
        List<WorkerSummary> workers,
        List<QuestSummary> campaign,
        List<NearbyPlayerSummary> nearbyPlayers,
        List<MemberSummary> members,
        List<ForeignKingdomSummary> foreignKingdoms,
        List<InviteSummary> invites,
        List<DiplomacyProposalSummary> diplomacyProposals
) {
    private static final UUID NO_KINGDOM = new UUID(0L, 0L);

    public CommandCenterDashboardState {
        if (generatedGameTime < 0L) {
            throw new IllegalArgumentException("generatedGameTime cannot be negative");
        }
        actorId = Objects.requireNonNull(actorId, "actorId");
        kingdomId = Objects.requireNonNullElse(kingdomId, NO_KINGDOM);
        factionId = normalize(factionId);
        actorRole = normalize(actorRole);
        if (treasuryCredits < 0 || pendingRewardCredits < 0 || recruitCount < 0
                || housingCapacity < 0 || settlementCount < 0 || claimCount < 0
                || veteranVehicleDeployments < 0 || veteranTrades < 0
                || veteranRegionCaptures < 0) {
            throw new IllegalArgumentException("dashboard counters cannot be negative");
        }
        commandCandidateIds = List.copyOf(Objects.requireNonNull(
                commandCandidateIds, "commandCandidateIds"));
        claims = List.copyOf(Objects.requireNonNull(claims, "claims"));
        squads = List.copyOf(Objects.requireNonNull(squads, "squads"));
        combatTargets = List.copyOf(Objects.requireNonNull(combatTargets, "combatTargets"));
        blueprints = List.copyOf(Objects.requireNonNull(blueprints, "blueprints"));
        constructionBuilderIds = List.copyOf(Objects.requireNonNull(
                constructionBuilderIds, "constructionBuilderIds"));
        builds = List.copyOf(Objects.requireNonNull(builds, "builds"));
        workOrders = List.copyOf(Objects.requireNonNull(workOrders, "workOrders"));
        workers = List.copyOf(Objects.requireNonNull(workers, "workers"));
        campaign = List.copyOf(Objects.requireNonNull(campaign, "campaign"));
        nearbyPlayers = List.copyOf(Objects.requireNonNull(nearbyPlayers, "nearbyPlayers"));
        members = List.copyOf(Objects.requireNonNull(members, "members"));
        foreignKingdoms = List.copyOf(Objects.requireNonNull(foreignKingdoms, "foreignKingdoms"));
        invites = List.copyOf(Objects.requireNonNull(invites, "invites"));
        diplomacyProposals = List.copyOf(Objects.requireNonNull(
                diplomacyProposals, "diplomacyProposals"));
    }

    public static CommandCenterDashboardState empty(UUID actorId, long gameTime) {
        Objects.requireNonNull(actorId, "actorId");
        return new CommandCenterDashboardState(
                Math.max(0L, gameTime), false, actorId, NO_KINGDOM, "", "visitor",
                0, 0, true, 0, 0, 0, 0, false, false, 0, 0, 0,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of());
    }

    public static CommandCenterDashboardState capture(
            UUID actorId,
            KingdomRecord kingdom,
            List<ArmyGroupRecord> armyGroups,
            List<UUID> commandCandidateIds,
            List<CombatTargetSummary> combatTargets,
            List<WorkerSummary> workers,
            List<UUID> constructionBuilderIds,
            List<KingdomBaseBlueprint> availableBlueprints,
            List<NearbyPlayerSummary> nearbyPlayers,
            ProgressionState progression,
            int treasuryCredits,
            boolean upkeepPaid,
            List<KingdomRecord> foreignKingdoms,
            List<KingdomInvite> pendingInvites,
            List<DiplomacyProposal> pendingDiplomacy,
            long gameTime
    ) {
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(kingdom, "kingdom");
        Objects.requireNonNull(progression, "progression");
        SettlementRecord settlement = kingdom.settlement();
        String role = kingdom.member(actorId).map(member -> member.role().name())
                .orElse("visitor").toLowerCase(Locale.ROOT);

        List<SquadSummary> squads = armyGroups.stream()
                .filter(group -> group.kingdomId().equals(kingdom.id()))
                .sorted(Comparator.comparing(ArmyGroupRecord::name).thenComparing(ArmyGroupRecord::id))
                .map(SquadSummary::from)
                .toList();
        List<CombatTargetSummary> targets = Objects.requireNonNull(combatTargets, "combatTargets")
                .stream()
                .sorted(Comparator.comparingInt(CombatTargetSummary::distanceBlocks)
                        .thenComparing(CombatTargetSummary::displayName)
                        .thenComparing(CombatTargetSummary::entityId))
                .distinct()
                .limit(32)
                .toList();
        java.util.Set<UUID> kingdomRoster = kingdom.npcRoster().stream()
                .map(KingdomNpcRecord::recruitId)
                .collect(java.util.stream.Collectors.toSet());
        java.util.Set<UUID> militaryRoster = kingdom.npcRoster().stream()
                .filter(npc -> npc.serviceBranch()
                        == galacticwars.clonewars.recruitment.NpcServiceBranch.MILITARY)
                .map(KingdomNpcRecord::recruitId)
                .collect(java.util.stream.Collectors.toSet());
        java.util.Set<UUID> groupedRecruitIds = armyGroups.stream()
                .flatMap(group -> java.util.stream.Stream.concat(
                        group.commanderId().stream(), group.memberIds().stream()))
                .collect(java.util.stream.Collectors.toSet());
        List<UUID> availableCommanders = commandCandidateIds.stream()
                .filter(militaryRoster::contains)
                .filter(candidate -> !groupedRecruitIds.contains(candidate))
                .distinct()
                .sorted()
                .toList();
        List<ClaimSummary> claims = kingdom.claims().stream()
                .sorted(Comparator.comparing((KingdomClaim claim) -> claim.capital()).reversed()
                        .thenComparing(KingdomClaim::id))
                .map(ClaimSummary::from)
                .toList();
        List<BlueprintSummary> blueprints = availableBlueprints.stream()
                .sorted(Comparator.comparing(KingdomBaseBlueprint::displayName)
                        .thenComparing(KingdomBaseBlueprint::id))
                .map(BlueprintSummary::from)
                .toList();
        List<UUID> availableBuilders = constructionBuilderIds.stream()
                .filter(kingdomRoster::contains)
                .distinct()
                .sorted()
                .toList();
        List<BuildSummary> builds = settlement.buildProjects().stream()
                .sorted(Comparator.comparing(BuildProject::state).thenComparing(BuildProject::id))
                .map(BuildSummary::from)
                .toList();
        List<WorkOrderSummary> workOrders = settlement.workOrders().stream()
                .sorted(Comparator.comparing(WorkOrder::state).thenComparing(WorkOrder::id))
                .map(WorkOrderSummary::from)
                .toList();
        List<WorkerSummary> liveWorkers = Objects.requireNonNull(workers, "workers").stream()
                .sorted(Comparator.comparing(WorkerSummary::profession)
                        .thenComparing(WorkerSummary::displayName)
                        .thenComparing(WorkerSummary::entityId))
                .distinct()
                .limit(64)
                .toList();
        List<QuestSummary> campaign = campaign(progression, kingdom.factionId());
        boolean campaignVictory = progression.hasSubject(
                ProgressionEventType.CAMPAIGN_COMPLETED,
                path(kingdom.factionId()) + "_campaign");
        List<MemberSummary> members = kingdom.members().stream()
                .sorted(Comparator.comparing(member -> member.role().ordinal()))
                .map(member -> new MemberSummary(
                        member.playerId(), member.role().name().toLowerCase(Locale.ROOT)))
                .toList();
        List<NearbyPlayerSummary> inviteTargets = Objects.requireNonNull(
                        nearbyPlayers, "nearbyPlayers").stream()
                .filter(candidate -> !candidate.playerId().equals(actorId))
                .filter(candidate -> kingdom.member(candidate.playerId()).isEmpty())
                .sorted(Comparator.comparingInt(NearbyPlayerSummary::distanceBlocks)
                        .thenComparing(NearbyPlayerSummary::displayName)
                        .thenComparing(NearbyPlayerSummary::playerId))
                .distinct()
                .limit(16)
                .toList();
        List<ForeignKingdomSummary> others = foreignKingdoms.stream()
                .filter(other -> !other.id().equals(kingdom.id()))
                .sorted(Comparator.comparing(KingdomRecord::factionId).thenComparing(KingdomRecord::id))
                .map(other -> new ForeignKingdomSummary(other.id(), other.ownerId(), other.factionId()))
                .toList();
        List<InviteSummary> invites = pendingInvites.stream()
                .filter(invite -> invite.kingdomId().equals(kingdom.id())
                        || invite.targetPlayerId().equals(actorId))
                .filter(invite -> invite.expiresGameTime() >= gameTime)
                .sorted(Comparator.comparingLong(KingdomInvite::expiresGameTime))
                .map(invite -> new InviteSummary(
                        invite.id(), invite.kingdomId(), invite.inviterId(), invite.targetPlayerId(),
                        invite.offeredRole().name().toLowerCase(Locale.ROOT), invite.expiresGameTime()))
                .toList();
        List<DiplomacyProposalSummary> proposals = pendingDiplomacy.stream()
                .filter(proposal -> proposal.proposerKingdomId().equals(kingdom.id())
                        || proposal.targetKingdomId().equals(kingdom.id()))
                .filter(proposal -> proposal.expiresGameTime() >= gameTime)
                .sorted(Comparator.comparingLong(DiplomacyProposal::expiresGameTime))
                .map(proposal -> new DiplomacyProposalSummary(
                        proposal.id(), proposal.proposerKingdomId(), proposal.targetKingdomId(),
                        proposal.relation().name().toLowerCase(Locale.ROOT), proposal.expiresGameTime()))
                .toList();

        return new CommandCenterDashboardState(
                gameTime, true, actorId, kingdom.id(), kingdom.factionId(), role,
                Math.max(0, treasuryCredits), progression.pendingCreditRewards(), upkeepPaid,
                settlement.recruitIds().size(), settlement.housingCapacity(), kingdom.settlements().size(),
                kingdom.claims().size(), !settlement.commanderIds().isEmpty(), campaignVictory,
                progression.total(ProgressionEventType.VEHICLE_ACQUIRED),
                progression.total(ProgressionEventType.TRADE_COMPLETED),
                progression.total(ProgressionEventType.REGION_CAPTURED), availableCommanders,
                claims, squads, targets, blueprints, availableBuilders, builds, workOrders,
                liveWorkers, campaign, inviteTargets, members, others, invites, proposals);
    }

    public Optional<QuestSummary> activeQuest() {
        return campaign.stream().filter(quest -> !quest.complete()).findFirst();
    }

    public Optional<ObjectiveSummary> nextObjective() {
        return activeQuest().flatMap(quest -> quest.objectives().stream()
                .filter(objective -> !objective.complete()).findFirst());
    }

    private static List<QuestSummary> campaign(ProgressionState progression, String factionId) {
        String factionPath = path(factionId);
        if (factionPath.isBlank()) {
            return List.of();
        }
        return LaunchContentCatalog.quests().stream()
                .filter(questId -> path(questId).startsWith(factionPath + "_chapter_"))
                .sorted()
                .map(questId -> new QuestSummary(
                        questId,
                        progression.hasSubject(ProgressionEventType.QUEST_ADVANCED, questId),
                        LaunchContentCatalog.questRewardCredits(questId),
                        LaunchContentCatalog.questUnlocks(questId).stream().sorted().toList(),
                        LaunchContentCatalog.questObjectives(questId).stream()
                                .map(objective -> new ObjectiveSummary(
                                        objective,
                                        GalacticProgressionCoordinator.objectiveComplete(
                                                progression, objective)))
                                .toList()))
                .toList();
    }

    private static String path(String id) {
        String normalized = normalize(id);
        int separator = normalized.indexOf(':');
        return separator < 0 ? normalized : normalized.substring(separator + 1);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record SquadSummary(
            UUID id,
            String name,
            Optional<UUID> commanderId,
            List<UUID> memberIds,
            int unitCount,
            String order,
            String formation,
            String lifecycle,
            int supplyUnits
    ) {
        public SquadSummary {
            Objects.requireNonNull(id, "id");
            name = Objects.requireNonNull(name, "name");
            commanderId = commanderId == null ? Optional.empty() : commanderId;
            memberIds = List.copyOf(Objects.requireNonNull(memberIds, "memberIds"));
            order = normalize(order);
            formation = normalize(formation);
            lifecycle = normalize(lifecycle);
            if (unitCount < 0 || supplyUnits < 0) {
                throw new IllegalArgumentException("squad counters cannot be negative");
            }
        }

        private static SquadSummary from(ArmyGroupRecord group) {
            return new SquadSummary(
                    group.id(), group.name(), group.commanderId(), group.memberIds(),
                    group.memberIds().size() + (group.commanderId().isPresent() ? 1 : 0),
                    group.order().type().name(), group.order().formation().name(),
                    group.simulation().lifecycleState().name(), group.supplyUnits());
        }
    }

    public record ClaimSummary(
            UUID id,
            String dimensionId,
            int centerChunkX,
            int centerChunkZ,
            int chunkCount,
            boolean capital
    ) {
        public ClaimSummary {
            Objects.requireNonNull(id, "id");
            dimensionId = normalize(dimensionId);
            if (chunkCount < 1) {
                throw new IllegalArgumentException("claim must contain at least one chunk");
            }
        }

        private static ClaimSummary from(KingdomClaim claim) {
            return new ClaimSummary(
                    claim.id(), claim.dimensionId(), claim.center().x(), claim.center().z(),
                    claim.chunks().size(), claim.capital());
        }
    }

    public record CombatTargetSummary(
            UUID entityId,
            String displayName,
            String factionId,
            int distanceBlocks
    ) {
        public CombatTargetSummary {
            Objects.requireNonNull(entityId, "entityId");
            displayName = Objects.requireNonNull(displayName, "displayName").trim();
            if (displayName.isEmpty()) {
                displayName = entityId.toString().substring(0, 8);
            } else if (displayName.length() > 64) {
                displayName = displayName.substring(0, 64);
            }
            factionId = normalize(factionId);
            if (distanceBlocks < 0) {
                throw new IllegalArgumentException("distanceBlocks cannot be negative");
            }
        }
    }

    public record BlueprintSummary(
            UUID targetId,
            String blueprintId,
            String displayName,
            List<Integer> allowedRotations,
            int placementCount,
            List<MaterialSummary> materials,
            int housingReward,
            int storageSlotReward,
            int commanderSlotReward
    ) {
        public BlueprintSummary {
            Objects.requireNonNull(targetId, "targetId");
            blueprintId = normalize(blueprintId);
            displayName = Objects.requireNonNull(displayName, "displayName").trim();
            allowedRotations = List.copyOf(Objects.requireNonNull(
                    allowedRotations, "allowedRotations"));
            materials = List.copyOf(Objects.requireNonNull(materials, "materials"));
            if (displayName.isEmpty() || allowedRotations.isEmpty() || placementCount < 1
                    || housingReward < 0 || storageSlotReward < 0 || commanderSlotReward < 0) {
                throw new IllegalArgumentException("invalid blueprint dashboard summary");
            }
        }

        public static UUID targetId(String blueprintId) {
            return UUID.nameUUIDFromBytes(("galacticwars:blueprint:" + normalize(blueprintId))
                    .getBytes(StandardCharsets.UTF_8));
        }

        private static BlueprintSummary from(KingdomBaseBlueprint blueprint) {
            LinkedHashMap<String, Integer> materialCounts = new LinkedHashMap<>();
            blueprint.placements().forEach(placement -> materialCounts.merge(
                    normalize(placement.itemId()), 1, Math::addExact));
            List<MaterialSummary> materials = materialCounts.entrySet().stream()
                    .sorted(java.util.Map.Entry.comparingByKey())
                    .map(entry -> new MaterialSummary(entry.getKey(), entry.getValue()))
                    .toList();
            return new BlueprintSummary(
                    targetId(blueprint.id()), blueprint.id(), blueprint.displayName(),
                    blueprint.allowedRotations(), blueprint.placements().size(), materials,
                    blueprint.housingReward(), blueprint.storageSlotReward(),
                    blueprint.commanderSlotReward());
        }
    }

    public record MaterialSummary(String itemId, int count) {
        public MaterialSummary {
            itemId = normalize(itemId);
            if (itemId.isEmpty() || count < 1) {
                throw new IllegalArgumentException("invalid blueprint material summary");
            }
        }
    }

    public record BuildSummary(
            UUID id,
            String blueprintId,
            String state,
            int completedPlacements,
            int totalPlacements,
            String blockedReason
    ) {
        public BuildSummary {
            Objects.requireNonNull(id, "id");
            blueprintId = normalize(blueprintId);
            state = normalize(state);
            blockedReason = blockedReason == null ? "" : blockedReason.trim();
            if (completedPlacements < 0 || totalPlacements < completedPlacements) {
                throw new IllegalArgumentException("invalid build progress");
            }
        }

        private static BuildSummary from(BuildProject project) {
            int total = KingdomBaseBlueprint.byId(project.blueprintId())
                    .map(blueprint -> blueprint.placements().size())
                    .orElse(project.completedPlacements().size());
            return new BuildSummary(
                    project.id(), project.blueprintId(), project.state().name(),
                    project.completedPlacements().size(), Math.max(total, project.completedPlacements().size()),
                    project.blockedReason());
        }
    }

    public record WorkOrderSummary(
            UUID id,
            String type,
            String state,
            Optional<UUID> assignedRecruitId,
            int completedQuantity,
            int quantity,
            String blockedReason
    ) {
        public WorkOrderSummary {
            Objects.requireNonNull(id, "id");
            type = normalize(type);
            state = normalize(state);
            assignedRecruitId = assignedRecruitId == null ? Optional.empty() : assignedRecruitId;
            blockedReason = blockedReason == null ? "" : blockedReason.trim();
            if (completedQuantity < 0 || quantity < completedQuantity) {
                throw new IllegalArgumentException("invalid work-order progress");
            }
        }

        private static WorkOrderSummary from(WorkOrder order) {
            return new WorkOrderSummary(
                    order.id(), order.type().name(), order.state().name(), order.assignedRecruitId(),
                    order.completedQuantity(), order.quantity(), order.blockedReason());
        }
    }

    public record PositionSummary(String dimensionId, int x, int y, int z) {
        public PositionSummary {
            dimensionId = normalize(dimensionId);
            if (dimensionId.isEmpty()) {
                throw new IllegalArgumentException("position dimension cannot be blank");
            }
        }
    }

    public record WorkerSummary(
            UUID entityId,
            String displayName,
            String profession,
            String command,
            String phase,
            String reasonCode,
            Optional<PositionSummary> worksite,
            int workRadius,
            Optional<PositionSummary> storage,
            Optional<PositionSummary> activeTarget,
            Optional<UUID> workOrderId,
            int carriedItemCount,
            int storageItemCount,
            int distanceBlocks
    ) {
        public WorkerSummary {
            Objects.requireNonNull(entityId, "entityId");
            displayName = Objects.requireNonNull(displayName, "displayName").trim();
            if (displayName.isEmpty()) {
                displayName = entityId.toString().substring(0, 8);
            } else if (displayName.length() > 64) {
                displayName = displayName.substring(0, 64);
            }
            profession = normalize(profession);
            command = normalize(command);
            phase = normalize(phase);
            reasonCode = reasonCode == null || reasonCode.isBlank()
                    ? "ready" : normalize(reasonCode);
            worksite = worksite == null ? Optional.empty() : worksite;
            storage = storage == null ? Optional.empty() : storage;
            activeTarget = activeTarget == null ? Optional.empty() : activeTarget;
            workOrderId = workOrderId == null ? Optional.empty() : workOrderId;
            if (profession.isEmpty() || command.isEmpty() || phase.isEmpty()
                    || workRadius < 0 || workRadius > 32
                    || carriedItemCount < 0 || storageItemCount < 0 || distanceBlocks < 0) {
                throw new IllegalArgumentException("invalid worker dashboard summary");
            }
        }
    }

    public record QuestSummary(
            String questId,
            boolean complete,
            int rewardCredits,
            List<String> unlocks,
            List<ObjectiveSummary> objectives
    ) {
        public QuestSummary {
            questId = normalize(questId);
            if (rewardCredits < 0) {
                throw new IllegalArgumentException("rewardCredits cannot be negative");
            }
            unlocks = List.copyOf(Objects.requireNonNull(unlocks, "unlocks"));
            objectives = List.copyOf(Objects.requireNonNull(objectives, "objectives"));
        }
    }

    public record ObjectiveSummary(String objectiveId, boolean complete) {
        public ObjectiveSummary {
            objectiveId = normalize(objectiveId);
        }
    }

    public record MemberSummary(UUID playerId, String role) {
        public MemberSummary {
            Objects.requireNonNull(playerId, "playerId");
            role = normalize(role);
        }
    }

    public record NearbyPlayerSummary(UUID playerId, String displayName, int distanceBlocks) {
        public NearbyPlayerSummary {
            Objects.requireNonNull(playerId, "playerId");
            displayName = Objects.requireNonNullElse(displayName, "").trim();
            if (displayName.isEmpty() || displayName.length() > 64
                    || distanceBlocks < 0) {
        }
    }

    public record ForeignKingdomSummary(UUID kingdomId, UUID ownerId, String factionId) {
        public ForeignKingdomSummary {
            Objects.requireNonNull(kingdomId, "kingdomId");
            Objects.requireNonNull(ownerId, "ownerId");
            factionId = normalize(factionId);
        }
    }

    public record InviteSummary(
            UUID inviteId,
            UUID kingdomId,
            UUID inviterId,
            UUID targetPlayerId,
            String offeredRole,
            long expiresGameTime
    ) {
        public InviteSummary {
            Objects.requireNonNull(inviteId, "inviteId");
            Objects.requireNonNull(kingdomId, "kingdomId");
            Objects.requireNonNull(inviterId, "inviterId");
            Objects.requireNonNull(targetPlayerId, "targetPlayerId");
            offeredRole = normalize(offeredRole);
            if (expiresGameTime < 0L) {
                throw new IllegalArgumentException("expiresGameTime cannot be negative");
            }
        }
    }

    public record DiplomacyProposalSummary(
            UUID proposalId,
            UUID proposerKingdomId,
            UUID targetKingdomId,
            String relation,
            long expiresGameTime
    ) {
        public DiplomacyProposalSummary {
            Objects.requireNonNull(proposalId, "proposalId");
            Objects.requireNonNull(proposerKingdomId, "proposerKingdomId");
            Objects.requireNonNull(targetKingdomId, "targetKingdomId");
            relation = normalize(relation);
            if (expiresGameTime < 0L) {
                throw new IllegalArgumentException("expiresGameTime cannot be negative");
            }
        }
    }
}
