package middleearth.lotr.warmod.kingdom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntUnaryOperator;
import middleearth.lotr.warmod.KingdomWarsMiddleEarth;
import middleearth.lotr.warmod.data.GameplayDataManager;
import middleearth.lotr.warmod.army.ArmyFormation;
import middleearth.lotr.warmod.army.ArmyGroupOrder;
import middleearth.lotr.warmod.army.ArmyGroupRecord;
import middleearth.lotr.warmod.army.ArmyLocation;
import middleearth.lotr.warmod.army.ArmyMemberSnapshot;
import middleearth.lotr.warmod.settlement.KingdomBaseBlueprint;
import middleearth.lotr.warmod.workforce.WorkerProfession;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class KingdomSavedData extends SavedData {
    public static final int CURRENT_SCHEMA_VERSION = 4;
    public static final Codec<KingdomSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema_version", CURRENT_SCHEMA_VERSION).forGetter(KingdomSavedData::schemaVersion),
            KingdomCodecs.KINGDOM_RECORD.listOf().optionalFieldOf("kingdoms", List.of()).forGetter(KingdomSavedData::kingdoms),
            net.minecraft.core.UUIDUtil.CODEC.listOf().optionalFieldOf("inactive_hall_owners", List.of())
                    .forGetter(data -> List.copyOf(data.inactiveHallOwners)),
            KingdomCodecs.ARMY_GROUP.listOf().optionalFieldOf("army_groups", List.of())
                    .forGetter(KingdomSavedData::armyGroups)
    ).apply(instance, KingdomSavedData::new));
    public static final SavedDataType<KingdomSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(KingdomWarsMiddleEarth.MODID, "kingdoms"),
            KingdomSavedData::new,
            CODEC);

    private final int schemaVersion;
    private final Map<UUID, KingdomRecord> kingdomsByOwner = new LinkedHashMap<>();
    private final LinkedHashSet<UUID> inactiveHallOwners = new LinkedHashSet<>();
    private final Map<UUID, ArmyGroupRecord> armyGroupsById = new LinkedHashMap<>();

    public KingdomSavedData() {
        this(CURRENT_SCHEMA_VERSION, List.of(), List.of(), List.of());
    }

    private KingdomSavedData(
            int schemaVersion,
            List<KingdomRecord> kingdoms,
            List<UUID> inactiveHallOwners,
            List<ArmyGroupRecord> armyGroups
    ) {
        this.schemaVersion = Math.max(CURRENT_SCHEMA_VERSION, schemaVersion);
        for (KingdomRecord kingdom : kingdoms) {
            this.kingdomsByOwner.putIfAbsent(kingdom.ownerId(), kingdom);
        }
        this.inactiveHallOwners.addAll(inactiveHallOwners);
        this.inactiveHallOwners.retainAll(this.kingdomsByOwner.keySet());
        for (ArmyGroupRecord armyGroup : armyGroups) {
            boolean knownKingdom = this.kingdomsByOwner.values().stream()
                    .anyMatch(kingdom -> kingdom.id().equals(armyGroup.kingdomId())
                            && kingdom.ownerId().equals(armyGroup.ownerId()));
            if (knownKingdom) {
                this.armyGroupsById.putIfAbsent(armyGroup.id(), armyGroup);
            }
        }
    }

    public static KingdomSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    public List<KingdomRecord> kingdoms() {
        return List.copyOf(kingdomsByOwner.values());
    }

    public List<ArmyGroupRecord> armyGroups() {
        return List.copyOf(armyGroupsById.values());
    }

    public Optional<ArmyGroupRecord> armyGroup(UUID groupId) {
        return Optional.ofNullable(armyGroupsById.get(groupId));
    }

    public Optional<ArmyGroupRecord> armyGroupForOwner(UUID ownerId) {
        return armyGroupsById.values().stream().filter(group -> group.ownerId().equals(ownerId)).findFirst();
    }

    public Optional<ArmyGroupRecord> armyGroupForRecruit(UUID recruitId) {
        return armyGroupsById.values().stream().filter(group -> group.contains(recruitId)).findFirst();
    }

    public Optional<KingdomRecord> kingdomForOwner(UUID ownerId) {
        return Optional.ofNullable(kingdomsByOwner.get(ownerId));
    }

    public boolean isHallActive(UUID ownerId) {
        return kingdomsByOwner.containsKey(ownerId) && !inactiveHallOwners.contains(ownerId);
    }

    public KingdomRecord foundKingdom(UUID ownerId, String factionId, String dimensionId, BlockPos hallPos) {
        KingdomRecord existing = kingdomsByOwner.get(ownerId);
        if (existing != null) {
            return existing;
        }
        KingdomRecord kingdom = new KingdomRecord(
                UUID.randomUUID(),
                ownerId,
                factionId,
                SettlementRecord.create(dimensionId, hallPos.getX(), hallPos.getY(), hallPos.getZ()));
        kingdomsByOwner.put(ownerId, kingdom);
        this.setDirty();
        return kingdom;
    }

    public Optional<KingdomRecord> activateHall(
            UUID ownerId,
            String factionId,
            String dimensionId,
            BlockPos hallPos
    ) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(hallPos, "hallPos");
        KingdomRecord existing = kingdomsByOwner.get(ownerId);
        if (existing == null) {
            return Optional.of(foundKingdom(ownerId, factionId, dimensionId, hallPos));
        }
        SettlementRecord settlement = existing.settlement();
        boolean sameHall = settlement.dimensionId().equals(dimensionId)
                && settlement.hallX() == hallPos.getX()
                && settlement.hallY() == hallPos.getY()
                && settlement.hallZ() == hallPos.getZ();
        if (!inactiveHallOwners.contains(ownerId)) {
            return sameHall ? Optional.of(existing) : Optional.empty();
        }
        KingdomRecord relocated = existing.withSettlement(
                settlement.withHallLocation(dimensionId, hallPos.getX(), hallPos.getY(), hallPos.getZ()));
        kingdomsByOwner.put(ownerId, relocated);
        inactiveHallOwners.remove(ownerId);
        this.setDirty();
        return Optional.of(relocated);
    }

    public boolean deactivateHall(UUID ownerId, String dimensionId, BlockPos hallPos) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        if (kingdom == null || inactiveHallOwners.contains(ownerId)) {
            return false;
        }
        SettlementRecord settlement = kingdom.settlement();
        if (!settlement.dimensionId().equals(dimensionId)
                || settlement.hallX() != hallPos.getX()
                || settlement.hallY() != hallPos.getY()
                || settlement.hallZ() != hallPos.getZ()) {
            return false;
        }
        inactiveHallOwners.add(ownerId);
        this.setDirty();
        return true;
    }

    public boolean registerRecruit(UUID ownerId, UUID recruitId) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        if (kingdom == null || !kingdom.settlement().hasHousingSpace()) {
            return false;
        }
        SettlementRecord updated = kingdom.settlement().withRecruit(recruitId);
        kingdomsByOwner.put(ownerId, kingdom.withSettlement(updated));
        this.setDirty();
        return true;
    }

    public boolean unregisterRecruit(UUID ownerId, UUID recruitId) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        if (kingdom == null) {
            return false;
        }
        SettlementRecord updated = kingdom.settlement().withoutRecruit(recruitId);
        if (updated == kingdom.settlement()) {
            return false;
        }
        kingdomsByOwner.put(ownerId, kingdom.withSettlement(updated));
        this.setDirty();
        return true;
    }

    public int cancelActiveCampaigns(UUID ownerId, String reasonCode) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        if (kingdom == null) {
            return 0;
        }
        SettlementRecord updated = kingdom.settlement();
        int cancelled = 0;
        for (RecruitmentCampaign campaign : kingdom.settlement().recruitmentCampaigns()) {
            if (campaign.active()) {
                updated = updated.replaceCampaign(campaign.cancel(reasonCode));
                cancelled++;
            }
        }
        if (cancelled > 0) {
            kingdomsByOwner.put(ownerId, kingdom.withSettlement(updated));
            this.setDirty();
        }
        return cancelled;
    }

    public boolean changeFaction(UUID ownerId, String factionId) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        if (kingdom == null) {
            return true;
        }
        if (!kingdom.settlement().recruitIds().isEmpty() || kingdom.settlement().hasActiveCampaign()) {
            return false;
        }
        kingdomsByOwner.put(ownerId, kingdom.withFaction(factionId));
        this.setDirty();
        return true;
    }

    public boolean promoteCommander(UUID ownerId, UUID recruitId) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        if (kingdom == null || kingdom.settlement().commanderId().isPresent()
                || !kingdom.settlement().hasCommanderSlot()
                || !kingdom.settlement().containsRecruit(recruitId)) {
            return false;
        }
        kingdomsByOwner.put(ownerId, kingdom.withSettlement(kingdom.settlement().withCommander(recruitId)));
        this.setDirty();
        return true;
    }

    public Optional<ArmyGroupRecord> createOrReclaimArmyGroup(
            UUID ownerId,
            UUID commanderId,
            ArmyFormation formation,
            ArmyLocation anchor,
            long gameTime
    ) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        if (kingdom == null || kingdom.settlement().commanderId().filter(commanderId::equals).isEmpty()) {
            return Optional.empty();
        }
        Optional<ArmyGroupRecord> existing = armyGroupForOwner(ownerId);
        if (existing.isPresent()) {
            ArmyGroupRecord reclaimed = existing.orElseThrow().withCommander(commanderId);
            armyGroupsById.put(reclaimed.id(), reclaimed);
            this.setDirty();
            return Optional.of(reclaimed);
        }
        Set<UUID> workerIds = kingdom.settlement().worksites().stream()
                .flatMap(worksite -> worksite.assignmentIds().stream())
                .collect(java.util.stream.Collectors.toSet());
        List<UUID> members = kingdom.settlement().recruitIds().stream()
                .filter(recruitId -> !recruitId.equals(commanderId))
                .filter(recruitId -> !workerIds.contains(recruitId))
                .toList();
        ArmyGroupRecord group = ArmyGroupRecord.create(
                ownerId, kingdom.id(), commanderId, members, formation, anchor, gameTime);
        armyGroupsById.put(group.id(), group);
        this.setDirty();
        return Optional.of(group);
    }

    public boolean issueArmyOrder(UUID ownerId, UUID groupId, ArmyGroupOrder order) {
        ArmyGroupRecord group = armyGroupsById.get(groupId);
        if (group == null || !group.ownerId().equals(ownerId)
                || group.commanderId().isEmpty()
                || group.simulation().lifecycleState() == middleearth.lotr.warmod.army.ArmyGroupLifecycleState.ORPHANED) {
            return false;
        }
        armyGroupsById.put(groupId, group.withOrder(order));
        this.setDirty();
        return true;
    }

    public boolean replaceArmyGroup(ArmyGroupRecord group, long expectedRevision) {
        ArmyGroupRecord current = armyGroupsById.get(group.id());
        if (current == null || current.simulation().revision() != expectedRevision
                || !current.ownerId().equals(group.ownerId())) {
            return false;
        }
        armyGroupsById.put(group.id(), group);
        this.setDirty();
        return true;
    }

    public boolean upsertArmySnapshot(UUID groupId, ArmyMemberSnapshot snapshot) {
        ArmyGroupRecord current = armyGroupsById.get(groupId);
        if (current == null || !current.contains(snapshot.recruitId())) {
            return false;
        }
        ArmyGroupRecord updated = current.withSnapshot(snapshot);
        if (updated == current) {
            return true;
        }
        armyGroupsById.put(groupId, updated);
        this.setDirty();
        return true;
    }

    public boolean addRecruitToArmy(UUID ownerId, UUID recruitId) {
        Optional<ArmyGroupRecord> groupOptional = armyGroupForOwner(ownerId);
        if (groupOptional.isEmpty()) {
            return false;
        }
        ArmyGroupRecord group = groupOptional.orElseThrow();
        if (group.memberIds().contains(recruitId) || group.commanderId().filter(recruitId::equals).isPresent()) {
            return true;
        }
        ArrayList<UUID> members = new ArrayList<>(group.memberIds());
        members.add(recruitId);
        armyGroupsById.put(group.id(), group.withMembers(members));
        this.setDirty();
        return true;
    }

    public boolean releaseArmyMember(UUID ownerId, UUID recruitId, boolean commander, ArmyLocation lastLocation) {
        Optional<ArmyGroupRecord> groupOptional = armyGroupForRecruit(recruitId);
        if (groupOptional.isEmpty() || !groupOptional.orElseThrow().ownerId().equals(ownerId)) {
            return false;
        }
        ArmyGroupRecord group = groupOptional.orElseThrow();
        ArmyGroupRecord updated;
        if (commander || group.commanderId().filter(recruitId::equals).isPresent()) {
            updated = group.orphan(lastLocation);
        } else {
            updated = group.withMembers(group.memberIds().stream().filter(id -> !id.equals(recruitId)).toList());
        }
        armyGroupsById.put(group.id(), updated);
        this.setDirty();
        return true;
    }

    /**
     * Compatibility signature for old integrations. Caller-provided reward values are deliberately ignored.
     */
    @Deprecated
    public boolean completeBuildProject(
            UUID ownerId,
            BuildProject project,
            int housingReward,
            String worksiteType,
            int worksiteCapacity
    ) {
        return GameplayDataManager.snapshot().blueprint(project.blueprintId())
                .map(blueprint -> completeBuildProject(ownerId, project, blueprint))
                .orElse(false);
    }

    public boolean completeBuildProject(UUID ownerId, BuildProject project, KingdomBaseBlueprint blueprint) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        KingdomBaseBlueprint authoritative = GameplayDataManager.snapshot().blueprint(project.blueprintId())
                .orElse(null);
        if (kingdom == null
                || authoritative == null
                || !blueprint.id().equals(authoritative.id())
                || !blueprint.definitionHash().equals(authoritative.definitionHash())) {
            return false;
        }
        SettlementRecord updated = kingdom.settlement().withCompletedProject(project, authoritative);
        if (updated == kingdom.settlement()) {
            return false;
        }
        kingdomsByOwner.put(ownerId, kingdom.withSettlement(updated));
        this.setDirty();
        return true;
    }

    public Optional<BuildProject> startBuildProject(
            UUID ownerId,
            KingdomBaseBlueprint blueprint,
            String dimensionId,
            BlockPos origin,
            int rotationSteps
    ) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        KingdomBaseBlueprint authoritative = GameplayDataManager.snapshot().blueprint(blueprint.id())
                .orElse(null);
        if (kingdom == null
                || authoritative == null
                || !authoritative.definitionHash().equals(blueprint.definitionHash())
                || !authoritative.supportsRotationSteps(rotationSteps)) {
            return Optional.empty();
        }
        String normalizedDimension = KingdomNormalizers.normalize(dimensionId, "dimensionId");
        Optional<BuildProject> existing = kingdom.settlement().buildProjects().stream()
                .filter(project -> project.state() == BuildProjectState.ACTIVE || project.state() == BuildProjectState.BLOCKED)
                .filter(project -> project.dimensionId().equals(normalizedDimension)
                        && project.originX() == origin.getX()
                        && project.originY() == origin.getY()
                        && project.originZ() == origin.getZ())
                .findFirst();
        if (existing.isPresent()) {
            BuildProject project = existing.orElseThrow();
            if (!project.blueprintId().equals(authoritative.id()) || project.rotationSteps() != rotationSteps) {
                return Optional.empty();
            }
            if (!project.definitionHash().equals(authoritative.definitionHash())) {
                BuildProject blocked = project.block("blueprint_definition_changed");
                replaceBuildProject(ownerId, blocked);
                return Optional.of(blocked);
            }
            return existing;
        }
        BuildProject project = new BuildProject(
                UUID.randomUUID(), authoritative.id(), normalizedDimension,
                origin.getX(), origin.getY(), origin.getZ(), rotationSteps,
                authoritative.definitionHash(), List.of(), BuildProjectState.ACTIVE, "", 0);
        SettlementRecord updated = kingdom.settlement().withNewBuildProject(project);
        if (updated == kingdom.settlement()) {
            return Optional.empty();
        }
        kingdomsByOwner.put(ownerId, kingdom.withSettlement(updated));
        this.setDirty();
        return Optional.of(project);
    }

    public boolean replaceBuildProject(UUID ownerId, BuildProject project) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        if (kingdom == null) {
            return false;
        }
        SettlementRecord updated = kingdom.settlement().replaceBuildProject(project);
        if (updated == kingdom.settlement()) {
            return false;
        }
        kingdomsByOwner.put(ownerId, kingdom.withSettlement(updated));
        this.setDirty();
        return true;
    }

    public boolean reserveWorksite(UUID ownerId, UUID recruitId, WorkerProfession profession) {
        return reserveWorksite(ownerId, recruitId, profession, Optional.empty());
    }

    public boolean reserveWorksite(
            UUID ownerId,
            UUID recruitId,
            WorkerProfession profession,
            Optional<UUID> preferredProjectId
    ) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        if (kingdom == null || inactiveHallOwners.contains(ownerId)
                || !kingdom.settlement().containsRecruit(recruitId)) {
            return false;
        }
        SettlementRecord updated = kingdom.settlement()
                .reserveWorksite(recruitId, profession, preferredProjectId);
        if (updated == kingdom.settlement()) {
            return kingdom.settlement().assignedWorksite(recruitId)
                    .filter(worksite -> worksite.accepts(profession))
                    .filter(worksite -> preferredProjectId.isEmpty()
                            || worksite.sourceProjectId().equals(preferredProjectId))
                    .isPresent();
        }
        kingdomsByOwner.put(ownerId, kingdom.withSettlement(updated));
        this.setDirty();
        return true;
    }

    public Optional<WorksiteRecord> assignedWorksite(UUID ownerId, UUID recruitId) {
        return kingdomForOwner(ownerId)
                .filter(kingdom -> kingdom.settlement().containsRecruit(recruitId))
                .flatMap(kingdom -> kingdom.settlement().assignedWorksite(recruitId));
    }

    public Optional<WorksiteRecord> configureAssignedFrontierWorksite(
            UUID ownerId,
            UUID recruitId,
            String dimensionId,
            BlockPos target,
            int radius
    ) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        if (kingdom == null || inactiveHallOwners.contains(ownerId)
                || !kingdom.settlement().containsRecruit(recruitId)
                || radius < 1 || radius > 32
                || !insideSettlementClaim(kingdom.settlement(), dimensionId, target)) {
            return Optional.empty();
        }
        SettlementRecord updated = kingdom.settlement().configureAssignedFrontierWorksite(
                recruitId, dimensionId, target.getX(), target.getY(), target.getZ(), radius);
        if (updated == kingdom.settlement()) {
            return Optional.empty();
        }
        kingdomsByOwner.put(ownerId, kingdom.withSettlement(updated));
        this.setDirty();
        return updated.assignedWorksite(recruitId);
    }

    public boolean releaseWorksite(UUID ownerId, UUID recruitId) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        if (kingdom == null) {
            return false;
        }
        SettlementRecord updated = kingdom.settlement().releaseWorksite(recruitId);
        if (updated == kingdom.settlement()) {
            return false;
        }
        kingdomsByOwner.put(ownerId, kingdom.withSettlement(updated));
        this.setDirty();
        return true;
    }

    public boolean releaseWorkerAssignments(UUID ownerId, UUID recruitId) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        if (kingdom == null) {
            return false;
        }
        SettlementRecord updated = kingdom.settlement().releaseWorkerAssignments(recruitId);
        if (updated == kingdom.settlement()) {
            return false;
        }
        kingdomsByOwner.put(ownerId, kingdom.withSettlement(updated));
        this.setDirty();
        return true;
    }

    public List<StorageEndpoint> registeredStorageEndpoints(UUID ownerId) {
        return kingdomForOwner(ownerId)
                .map(KingdomRecord::settlement)
                .map(KingdomStoragePolicy::registeredEndpoints)
                .orElse(List.of());
    }

    public boolean isRegisteredStorage(UUID ownerId, String dimensionId, BlockPos target) {
        return registeredStorageEndpoint(ownerId, dimensionId, target).isPresent();
    }

    public Optional<StorageEndpoint> registeredStorageEndpoint(
            UUID ownerId,
            String dimensionId,
            BlockPos target
    ) {
        return registeredStorageEndpoints(ownerId).stream()
                .filter(endpoint -> endpoint.dimensionId().equals(dimensionId)
                        && endpoint.x() == target.getX()
                        && endpoint.y() == target.getY()
                        && endpoint.z() == target.getZ())
                .findFirst();
    }

    public Optional<WorkOrder> workOrder(UUID ownerId, UUID orderId) {
        return kingdomForOwner(ownerId).flatMap(kingdom -> kingdom.settlement().workOrder(orderId));
    }

    public Optional<WorkOrder> assignedWorkOrder(UUID ownerId, UUID recruitId) {
        return kingdomForOwner(ownerId).stream()
                .flatMap(kingdom -> kingdom.settlement().workOrders().stream())
                .filter(order -> !order.state().terminal())
                .filter(order -> order.assignedRecruitId().filter(recruitId::equals).isPresent())
                .findFirst();
    }

    public boolean queueWorkOrder(UUID ownerId, WorkOrder order) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        return kingdom != null
                && !inactiveHallOwners.contains(ownerId)
                && order.state() == WorkOrderState.QUEUED
                && order.assignedRecruitId().isEmpty()
                && order.revision() == 0
                && validWorkOrderReferences(kingdom.settlement(), order, Optional.empty(), true)
                && updateWorkOrder(ownerId, order, true);
    }

    public Optional<WorkOrder> queueAndClaimWorkOrder(UUID ownerId, UUID recruitId, WorkOrder order) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        if (kingdom == null || inactiveHallOwners.contains(ownerId)
                || order.state() != WorkOrderState.QUEUED
                || order.assignedRecruitId().isPresent()
                || order.revision() != 0
                || kingdom.settlement().workOrders().stream().anyMatch(existing -> existing.id().equals(order.id()))
                || kingdom.settlement().workOrders().stream().anyMatch(existing -> !existing.state().terminal()
                        && existing.assignedRecruitId().filter(recruitId::equals).isPresent())
                || !validWorkOrderReferences(kingdom.settlement(), order, Optional.of(recruitId), true)) {
            return Optional.empty();
        }
        WorkOrder claimed = order.claim(recruitId);
        SettlementRecord queued = kingdom.settlement().withWorkOrder(order, true);
        SettlementRecord updated = queued.withWorkOrder(claimed, false);
        if (queued == kingdom.settlement() || updated == queued) {
            return Optional.empty();
        }
        kingdomsByOwner.put(ownerId, kingdom.withSettlement(updated));
        this.setDirty();
        return Optional.of(claimed);
    }

    public Optional<WorkOrder> claimWorkOrder(UUID ownerId, UUID orderId, UUID recruitId, int expectedRevision) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        if (kingdom == null || inactiveHallOwners.contains(ownerId)
                || kingdom.settlement().workOrders().stream().anyMatch(existing -> !existing.id().equals(orderId)
                        && !existing.state().terminal()
                        && existing.assignedRecruitId().filter(recruitId::equals).isPresent())) {
            return Optional.empty();
        }
        WorkOrder current = kingdom.settlement().workOrder(orderId)
                .filter(order -> order.revision() == expectedRevision)
                .orElse(null);
        if (current == null
                || !validWorkOrderReferences(kingdom.settlement(), current, Optional.of(recruitId), true)) {
            return Optional.empty();
        }
        return mutateWorkOrder(ownerId, orderId, expectedRevision, order -> order.claim(recruitId));
    }

    public Optional<WorkOrder> progressWorkOrder(UUID ownerId, UUID orderId, int expectedRevision, int amount) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        WorkOrder current = kingdom == null || inactiveHallOwners.contains(ownerId)
                ? null : kingdom.settlement().workOrder(orderId)
                .filter(order -> order.revision() == expectedRevision).orElse(null);
        if (current == null || current.assignedRecruitId().isEmpty()
                || !validWorkOrderReferences(
                        kingdom.settlement(), current, current.assignedRecruitId(), false)) {
            return Optional.empty();
        }
        if (current.type() == WorkOrderType.BUILD) {
            BuildProject project = current.projectId().flatMap(id -> kingdom.settlement().buildProjects().stream()
                    .filter(candidate -> candidate.id().equals(id)).findFirst()).orElse(null);
            int availableProgress = project == null
                    ? 0 : project.completedPlacements().size() - current.completedQuantity();
            if (amount < 1 || amount > availableProgress) {
                return Optional.empty();
            }
        }
        return mutateWorkOrder(ownerId, orderId, expectedRevision, order -> order.progress(amount));
    }

    public Optional<WorkOrder> blockWorkOrder(UUID ownerId, UUID orderId, int expectedRevision, String reason) {
        return mutateWorkOrder(ownerId, orderId, expectedRevision, order -> order.block(reason));
    }

    public Optional<WorkOrder> resumeWorkOrder(
            UUID ownerId,
            UUID orderId,
            UUID recruitId,
            int expectedRevision
    ) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        WorkOrder current = kingdom == null || inactiveHallOwners.contains(ownerId)
                ? null : kingdom.settlement().workOrder(orderId)
                .filter(order -> order.revision() == expectedRevision).orElse(null);
        if (current == null
                || !validWorkOrderReferences(kingdom.settlement(), current, Optional.of(recruitId), false)) {
            return Optional.empty();
        }
        return mutateWorkOrder(ownerId, orderId, expectedRevision, order -> order.resume(recruitId));
    }

    public Optional<WorkOrder> cancelWorkOrder(UUID ownerId, UUID orderId, int expectedRevision) {
        return mutateWorkOrder(ownerId, orderId, expectedRevision, WorkOrder::cancel);
    }

    public Optional<WorkOrder> releaseWorkOrder(UUID ownerId, UUID orderId, int expectedRevision) {
        return mutateWorkOrder(ownerId, orderId, expectedRevision, WorkOrder::release);
    }

    private Optional<WorkOrder> mutateWorkOrder(
            UUID ownerId,
            UUID orderId,
            int expectedRevision,
            java.util.function.UnaryOperator<WorkOrder> operation
    ) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        if (kingdom == null) {
            return Optional.empty();
        }
        WorkOrder current = kingdom.settlement().workOrders().stream()
                .filter(order -> order.id().equals(orderId) && order.revision() == expectedRevision)
                .findFirst().orElse(null);
        if (current == null) {
            return Optional.empty();
        }
        WorkOrder updated = operation.apply(current);
        return updateWorkOrder(ownerId, updated, false) ? Optional.of(updated) : Optional.empty();
    }

    private boolean updateWorkOrder(UUID ownerId, WorkOrder workOrder, boolean allowInsert) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        if (kingdom == null) {
            return false;
        }
        SettlementRecord updated = kingdom.settlement().withWorkOrder(workOrder, allowInsert);
        if (updated == kingdom.settlement()) {
            return false;
        }
        kingdomsByOwner.put(ownerId, kingdom.withSettlement(updated));
        this.setDirty();
        return true;
    }

    private boolean validWorkOrderReferences(
            SettlementRecord settlement,
            WorkOrder order,
            Optional<UUID> recruitId,
            boolean requireActiveProject
    ) {
        WorksiteRecord worksite = order.worksiteId().stream()
                .flatMap(id -> settlement.worksites().stream().filter(candidate -> candidate.id().equals(id)))
                .findFirst().orElse(null);
        if (worksite == null
                || !worksite.accepts(order.type().profession())
                || !worksite.dimensionId().equals(order.dimensionId())
                || Math.abs(order.targetX() - worksite.x()) > worksite.radius()
                || Math.abs(order.targetZ() - worksite.z()) > worksite.radius()
                || Math.abs(order.targetY() - worksite.y()) > 4) {
            return false;
        }
        if (recruitId.isPresent()) {
            UUID recruit = recruitId.orElseThrow();
            if (!settlement.containsRecruit(recruit)
                    || !worksite.assignmentIds().contains(recruit)) {
                return false;
            }
        }
        if (order.type() == WorkOrderType.BUILD) {
            BuildProject project = order.projectId().stream()
                    .flatMap(id -> settlement.buildProjects().stream()
                            .filter(candidate -> candidate.id().equals(id)))
                    .findFirst().orElse(null);
            KingdomBaseBlueprint blueprint = project == null
                    ? null
                    : GameplayDataManager.snapshot().blueprint(project.blueprintId()).orElse(null);
            if (project == null || blueprint == null
                    || worksite.sourceProjectId().filter(project.id()::equals).isEmpty()
                    || !project.definitionHash().equals(blueprint.definitionHash())
                    || !project.dimensionId().equals(order.dimensionId())
                    || order.targetX() != project.originX()
                    || order.targetY() != project.originY()
                    || order.targetZ() != project.originZ()
                    || order.quantity() != blueprint.placements().size()
                    || order.completedQuantity() > project.completedPlacements().size()
                    || (order.state() == WorkOrderState.QUEUED
                            && order.completedQuantity() != project.completedPlacements().size())
                    || (requireActiveProject && project.state() != BuildProjectState.ACTIVE)
                    || (!requireActiveProject && project.state() != BuildProjectState.ACTIVE
                            && project.state() != BuildProjectState.BLOCKED)) {
                return false;
            }
        } else if (order.projectId().isPresent()
                || order.quantity() != 1
                || order.completedQuantity() != 0) {
            return false;
        }
        return true;
    }

    private static boolean insideSettlementClaim(
            SettlementRecord settlement,
            String dimensionId,
            BlockPos target
    ) {
        return settlement.dimensionId().equals(KingdomNormalizers.normalize(dimensionId, "dimensionId"))
                && Math.abs(target.getX() - settlement.hallX()) <= settlement.claimRadius()
                && Math.abs(target.getZ() - settlement.hallZ()) <= settlement.claimRadius();
    }

    public boolean clearCommander(UUID ownerId, UUID recruitId) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        if (kingdom == null) {
            return false;
        }
        SettlementRecord updated = kingdom.settlement().withoutCommander(recruitId);
        if (updated == kingdom.settlement()) {
            return false;
        }
        kingdomsByOwner.put(ownerId, kingdom.withSettlement(updated));
        this.setDirty();
        return true;
    }

    public boolean updateCommanderPolicy(UUID ownerId, int expectedRevision, CommanderPolicy policy) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        if (kingdom == null || kingdom.settlement().revision() != expectedRevision) {
            return false;
        }
        kingdomsByOwner.put(ownerId,
                kingdom.withSettlement(kingdom.settlement().withCommanderPolicy(policy)));
        this.setDirty();
        return true;
    }

    public boolean beginCampaign(UUID ownerId, RecruitmentCampaignDecision decision) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        if (kingdom == null || !decision.accepted()) {
            return false;
        }
        RecruitmentCampaign campaign = decision.campaign().orElseThrow();
        kingdomsByOwner.put(ownerId,
                kingdom.withSettlement(kingdom.settlement().withCampaign(campaign)));
        this.setDirty();
        return true;
    }

    public boolean replaceCampaign(UUID ownerId, RecruitmentCampaign campaign) {
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        if (kingdom == null) {
            return false;
        }
        SettlementRecord updated = kingdom.settlement().replaceCampaign(campaign);
        if (updated == kingdom.settlement()) {
            return false;
        }
        kingdomsByOwner.put(ownerId, kingdom.withSettlement(updated));
        this.setDirty();
        return true;
    }

    public int applyPendingCampaignRefunds(UUID ownerId, IntUnaryOperator refundSink) {
        Objects.requireNonNull(refundSink, "refundSink");
        KingdomRecord kingdom = kingdomsByOwner.get(ownerId);
        if (kingdom == null) {
            return 0;
        }
        SettlementRecord updated = kingdom.settlement();
        int totalRefunded = 0;
        for (RecruitmentCampaign campaign : kingdom.settlement().recruitmentCampaigns()) {
            if (!campaign.refundPending()) {
                continue;
            }
            int refunded = Math.max(0, Math.min(campaign.reservedCost(), refundSink.applyAsInt(campaign.reservedCost())));
            if (refunded > 0) {
                updated = updated.replaceCampaign(campaign.applyRefund(refunded));
                totalRefunded = Math.addExact(totalRefunded, refunded);
            }
        }
        if (totalRefunded > 0) {
            kingdomsByOwner.put(ownerId, kingdom.withSettlement(updated));
            this.setDirty();
        }
        return totalRefunded;
    }
}
