package galacticwars.clonewars.army;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;

import galacticwars.clonewars.data.GameplayDataManager;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.faction.FactionBalanceService;
import galacticwars.clonewars.kingdom.KingdomSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/** Loader-neutral server runtime for live and virtual army groups. */
public final class ArmyRuntimeEvents {
    private static final int PLAYER_RADIUS = 128;
    private static final int HIBERNATE_DELAY_TICKS = 100;
    private static final double MIN_EFFECTIVE_MOVEMENT_SPEED = 0.01D;
    private static final double MAX_EFFECTIVE_MOVEMENT_SPEED = 4.0D;
    private static final Map<UUID, Integer> idleTicks = new LinkedHashMap<>();
    private static final Set<UUID> materializingRecruitIds = new LinkedHashSet<>();

    private ArmyRuntimeEvents() {
    }

    public static void onServerTick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) {
            return;
        }
        KingdomSavedData data = KingdomSavedData.get(server.overworld());
        for (ArmyGroupRecord group : data.armyGroups()) {
            ServerLevel level = level(server, group.simulation().anchor().dimensionId());
            if (level == null) {
                continue;
            }
            if (group.simulation().lifecycleState() == ArmyGroupLifecycleState.VIRTUAL) {
                if (!playerNear(level, group.simulation().anchor())) {
                    discardPartialMaterialization(level, group);
                }
                ArmyGroupRecord advanced = advanceVirtualGroup(server, group, level);
                if (advanced != group
                        && data.replaceArmyGroup(advanced, group.simulation().revision())) {
                    group = advanced;
                }
                if (playerNear(level, group.simulation().anchor())) {
                    rematerialize(data, level, group);
                }
            } else if (group.simulation().lifecycleState() == ArmyGroupLifecycleState.LIVE) {
                group = advanceLiveMarch(data, level, group);
                group = refreshAttackTarget(data, level, group);
                maybeHibernate(data, level, group);
            }
        }
    }

    /**
     * Rejects stale army entities restored from chunk data while allowing the
     * exact recruits created by the bounded rematerialization transaction.
     */
    public static boolean allowEntityAddition(Entity entity, Level entityLevel) {
        if (!(entityLevel instanceof ServerLevel level)
                || !(entity instanceof GalacticRecruitEntity recruit)) {
            return true;
        }
        Optional<ArmyGroupRecord> group = KingdomSavedData.get(level).armyGroupForRecruit(recruit.getUUID());
        if (group.isEmpty()) {
            return true;
        }
        ArmyGroupRecord record = group.orElseThrow();
        boolean expectedRematerialization = materializingRecruitIds.contains(recruit.getUUID())
                && recruit.getArmySnapshotGeneration() == record.simulation().snapshotGeneration();
        return expectedRematerialization
                || record.simulation().lifecycleState() != ArmyGroupLifecycleState.VIRTUAL
                && recruit.getArmySnapshotGeneration() >= record.simulation().snapshotGeneration();
    }

    private static void maybeHibernate(KingdomSavedData data, ServerLevel level, ArmyGroupRecord group) {
        List<GalacticRecruitEntity> liveMembers = liveMembers(level, group);
        boolean active = playerNear(level, group.simulation().anchor())
                || playerNearLiveMember(level, liveMembers)
                || liveMembers.stream().anyMatch(recruit -> recruit.getTarget() != null || recruit.hurtTime > 0);
        if (active) {
            idleTicks.remove(group.id());
            return;
        }
        int idle = idleTicks.merge(group.id(), 20, Integer::sum);
        if (idle < HIBERNATE_DELAY_TICKS) {
            return;
        }

        long generation = group.simulation().snapshotGeneration() + 1L;
        Map<UUID, ArmyMemberSnapshot> snapshots = new LinkedHashMap<>();
        for (ArmyMemberSnapshot snapshot : group.snapshots()) {
            if (group.contains(snapshot.recruitId())) {
                snapshots.put(snapshot.recruitId(), withGeneration(snapshot, generation));
            }
        }
        for (GalacticRecruitEntity recruit : liveMembers) {
            recruit.createArmySnapshot(generation).ifPresent(snapshot -> snapshots.put(snapshot.recruitId(), snapshot));
        }
        if (!snapshots.keySet().containsAll(expectedMembers(group))) {
            return;
        }

        ArmyLocation anchor = group.commanderId()
                .flatMap(id -> liveMembers.stream().filter(recruit -> recruit.getUUID().equals(id)).findFirst())
                .map(recruit -> new ArmyLocation(
                        level.dimension().identifier().toString(), recruit.getX(), recruit.getY(), recruit.getZ()))
                .orElse(group.simulation().anchor());
        ArmyGroupSimulation simulation = new ArmyGroupSimulation(
                ArmyGroupLifecycleState.VIRTUAL,
                anchor,
                level.getGameTime(),
                group.simulation().revision() + 1,
                generation,
                "",
                group.simulation().marchState());
        ArmyGroupRecord virtual = group.withSimulation(simulation, orderedSnapshots(group, snapshots));
        if (!data.replaceArmyGroup(virtual, group.simulation().revision())) {
            return;
        }
        for (GalacticRecruitEntity recruit : liveMembers) {
            recruit.discard();
        }
        idleTicks.remove(group.id());
    }

    private static ArmyGroupRecord advanceLiveMarch(
            KingdomSavedData data,
            ServerLevel level,
            ArmyGroupRecord group
    ) {
        List<GalacticRecruitEntity> participants = liveMembers(level, group);
        GalacticRecruitEntity commander = group.commanderId()
                .flatMap(id -> participants.stream().filter(recruit -> recruit.getUUID().equals(id)).findFirst())
                .orElse(null);
        if (commander == null) {
            return group;
        }
        Map<UUID, ArmyFormationRole> formationRoles = new LinkedHashMap<>();
        for (GalacticRecruitEntity participant : participants) {
            if (group.memberIds().contains(participant.getUUID())) {
                formationRoles.put(participant.getUUID(), participant.armyFormationRole());
            }
        }
        List<ArmyFormationSlotAssignment> roleAwareAssignments =
                ArmyFormationSlotAssignment.reconcileRoleAware(
                        group.memberIds(), formationRoles, group.effectiveFormationSlotAssignments());
        ArmyGroupRecord marchingGroup = group;
        if (!roleAwareAssignments.equals(group.effectiveFormationSlotAssignments())) {
            ArmyGroupRecord replacement = group.withFormationSlotAssignments(roleAwareAssignments);
            if (data.replaceArmyGroup(replacement, group.simulation().revision())) {
                marchingGroup = replacement;
            }
        }
        ArmyLocation liveAnchor = location(commander);
        List<ArmyPosition> memberPositions = participants.stream()
                .filter(recruit -> group.memberIds().contains(recruit.getUUID()))
                .map(recruit -> new ArmyPosition(recruit.getBlockX(), recruit.getBlockY(), recruit.getBlockZ()))
                .toList();
        Optional<ArmyLocation> destination = liveDestination(level, marchingGroup);
        if (destination.isEmpty()) {
            ArmyMarchState previous = marchingGroup.simulation().marchState();
            int cohesion = ArmyMarchCoordinator.cohesionPercent(
                    liveAnchor.blockPosition(), memberPositions,
                    marchingGroup.order().spacing(), marchingGroup.memberIds().size());
            ArmyMarchState halted = previous.transition(
                    ArmyMarchPhase.HALTED, marchingGroup.order().formation(), cohesion,
                    previous.yawDegrees(), level.getGameTime());
            return replaceMarch(data, marchingGroup, liveAnchor, halted, level.getGameTime());
        }
        OptionalDouble movementSpeed = slowestMovementSpeed(marchingGroup);
            return marchingGroup;
        boolean engaged = participants.stream().anyMatch(recruit ->
                recruit.getTarget() != null || recruit.hurtTime > 0 || recruit.isAggressive());
        boolean footprintClear = formationFootprintClear(
                level, liveAnchor.blockPosition(), marchingGroup.order().formation(),
                marchingGroup.memberIds().size(), marchingGroup.order().spacing(),
                marchingGroup.simulation().marchState().yawDegrees());
        ArmyMarchCoordinator.Decision decision = ArmyMarchCoordinator.advance(
                marchingGroup, liveAnchor, destination.orElseThrow(), memberPositions,
                footprintClear, engaged, movementSpeed.orElseThrow(),
                readinessMultiplier(marchingGroup), level.getGameTime());
        return replaceMarch(data, marchingGroup, decision.anchor(), decision.marchState(), level.getGameTime());
    }

    private static ArmyGroupRecord replaceMarch(
            KingdomSavedData data,
            ArmyGroupRecord group,
            ArmyLocation anchor,
            ArmyMarchState march,
            long gameTime
    ) {
        ArmyGroupSimulation nextSimulation = group.simulation().withMarch(anchor, march, gameTime);
        if (nextSimulation == group.simulation()) {
            return group;
        }
        ArmyGroupRecord replacement = group.withSimulation(nextSimulation, group.snapshots());
        return data.replaceArmyGroup(replacement, group.simulation().revision()) ? replacement : group;
    }

    private static Optional<ArmyLocation> liveDestination(ServerLevel level, ArmyGroupRecord group) {
        if (group.order().type() == ArmyCommandType.PATROL_ROUTE
                && group.effectivePatrolPlan().map(ArmyPatrolPlan::state)
                        .map(ArmyPatrolState::status)
                        .filter(status -> status != ArmyPatrolStatus.ACTIVE).isPresent()) {
            return Optional.empty();
        }
        return switch (group.order().type()) {
            case MOVE_TO_POSITION, PATROL_ROUTE, ATTACK_TARGET -> group.order().targetPosition();
            case FOLLOW_OWNER, PROTECT_OWNER -> Optional.ofNullable(level.getEntity(group.ownerId()))
                    .filter(Entity::isAlive).map(ArmyRuntimeEvents::location);
            case PROTECT_ENTITY -> group.order().targetEntityId().map(level::getEntity)
                    .filter(Entity::isAlive).map(ArmyRuntimeEvents::location);
            case RETURN_TO_RALLY -> group.rallyPoint();
            case HOLD_POSITION, CLEAR_TARGET -> Optional.empty();
        };
    }

    private static boolean formationFootprintClear(
            ServerLevel level,
            ArmyPosition anchor,
            ArmyFormation formation,
            int memberCount,
            int spacing,
            float yaw
    ) {
        if (memberCount == 0) {
            return true;
        }
        List<ArmyPosition> positions = FormationPlanner.planPositions(
                anchor, formation, memberCount, spacing, yaw);
        int standable = 0;
        for (ArmyPosition position : positions) {
            BlockPos block = new BlockPos(position.x(), position.y(), position.z());
            if (!level.isLoaded(block)) {
                continue;
            }
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    block.getX(), block.getZ());
            BlockPos surface = new BlockPos(block.getX(), y, block.getZ());
            if (level.getBlockState(surface).isAir()
                    && level.getBlockState(surface.above()).isAir()
                    && level.getBlockState(surface.below()).blocksMotion()) {
                standable++;
            }
        }
        return standable * 4 >= positions.size() * 3;
    }

    private static double readinessMultiplier(ArmyGroupRecord group) {
        double supply = group.supplyUnits() > 0 ? 1.0D : 0.8D;
        double averageMorale = group.snapshots().stream()
                .filter(snapshot -> group.contains(snapshot.recruitId()))
                .mapToInt(ArmyMemberSnapshot::morale)
                .average().orElse(100.0D);
        double morale = 0.7D + Math.max(0.0D, Math.min(100.0D, averageMorale)) / 333.333D;
        return Math.max(0.5D, Math.min(1.15D, supply * morale));
    }

    private static ArmyLocation location(Entity entity) {
        return new ArmyLocation(entity.level().dimension().identifier().toString(),
                entity.getX(), entity.getY(), entity.getZ());
    }

    private static ArmyGroupRecord refreshAttackTarget(
            KingdomSavedData data,
            ServerLevel level,
            ArmyGroupRecord group
    ) {
        if (group.order().type() != ArmyCommandType.ATTACK_TARGET) {
            return group;
        }
        Entity target = group.order().targetEntityId().map(level::getEntity).orElse(null);
        if (target == null || !target.isAlive()) {
            return group;
        }
        ArmyLocation current = new ArmyLocation(
                level.dimension().identifier().toString(), target.getX(), target.getY(), target.getZ());
        ArmyLocation previous = group.order().targetPosition().orElse(null);
        if (previous != null
                && Math.abs(previous.x() - current.x()) < 1.0D
                && Math.abs(previous.y() - current.y()) < 1.0D
                && Math.abs(previous.z() - current.z()) < 1.0D) {
            return group;
        }
        ArmyGroupOrder updatedOrder = new ArmyGroupOrder(
                group.order().type(), Optional.of(current), group.order().targetEntityId(),
                group.order().formation(), group.order().spacing());
        return data.issueArmyOrder(group.ownerId(), group.id(), updatedOrder)
                ? data.armyGroup(group.id()).orElse(group)
                : group;
    }

    private static ArmyGroupRecord advanceVirtualGroup(
            MinecraftServer server,
            ArmyGroupRecord group,
            ServerLevel level
    ) {
        OptionalDouble slowestMovementSpeed = slowestMovementSpeed(group);
        if (slowestMovementSpeed.isEmpty()) {
            return VirtualArmyMovementPlanner.pause(
                    group, VirtualArmyMovementPlanner.UNIT_DEFINITION_UNAVAILABLE, level.getGameTime());
        }
        Optional<ArmyLocation> onlineOwner = Optional.ofNullable(
                        server.getPlayerList().getPlayer(group.ownerId()))
                .map(ArmyRuntimeEvents::location);
        return VirtualArmyMovementPlanner.advance(
                group, onlineOwner, slowestMovementSpeed.orElseThrow(), level.getGameTime());
    }

    private static void rematerialize(KingdomSavedData data, ServerLevel level, ArmyGroupRecord group) {
        BlockPos anchorPos = blockPos(group.simulation().anchor());
        if (!level.isLoaded(anchorPos)) {
            return;
        }
        List<ArmyMemberSnapshot> snapshots = orderedSnapshots(group, group.snapshots().stream()
                .filter(snapshot -> group.contains(snapshot.recruitId()))
                .collect(java.util.stream.Collectors.toMap(
                        ArmyMemberSnapshot::recruitId,
                        snapshot -> snapshot,
                        (first, ignored) -> first,
                        LinkedHashMap::new)));
        if (snapshots.isEmpty()) {
            return;
        }
        ArmyPosition anchor = group.simulation().anchor().blockPosition();
        boolean complete = true;
        for (int index = 0; index < snapshots.size(); index++) {
            ArmyMemberSnapshot snapshot = snapshots.get(index);
            Entity existing = level.getEntity(snapshot.recruitId());
            if (existing instanceof GalacticRecruitEntity recruit
                    && recruit.getArmySnapshotGeneration() >= snapshot.generation()) {
                continue;
            }
            ArmyPosition planned;
            try {
                planned = ArmyGroupOrderPlanner.formationPositionForMember(
                        group, snapshot.recruitId(), anchor);
            } catch (IllegalStateException | IndexOutOfBoundsException ignored) {
                complete = false;
                continue;
            }
            if (existing != null) {
                existing.discard();
            }
            GalacticRecruitEntity recruit = createRecruit(level, snapshot, group.id(), planned);
            if (recruit == null) {
                complete = false;
                continue;
            }
            boolean added;
            materializingRecruitIds.add(recruit.getUUID());
            try {
                added = level.addFreshEntity(recruit);
            } finally {
                materializingRecruitIds.remove(recruit.getUUID());
            }
            if (!added) {
                complete = false;
            }
        }
        if (!complete) {
            return;
        }
        ArmyGroupRecord current = data.armyGroup(group.id()).orElse(group);
        ArmyGroupSimulation liveSimulation = current.simulation().withLifecycle(
                ArmyGroupLifecycleState.LIVE,
                level.getGameTime(),
                current.simulation().snapshotGeneration());
        data.replaceArmyGroup(current.withSimulation(liveSimulation, snapshots), current.simulation().revision());
    }

    private static GalacticRecruitEntity createRecruit(
            ServerLevel level,
            ArmyMemberSnapshot snapshot,
            UUID groupId,
            ArmyPosition planned
    ) {
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(snapshot.entityTypeId()));
        Entity created = entityType.create(level, EntitySpawnReason.EVENT);
        if (!(created instanceof GalacticRecruitEntity recruit)) {
            return null;
        }
        for (BlockPos candidate : safeGroundCandidates(planned)) {
            if (!level.isLoaded(candidate)) {
                continue;
            }
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, candidate.getX(), candidate.getZ());
            recruit.setPos(candidate.getX() + 0.5D, y, candidate.getZ() + 0.5D);
            if (level.noCollision(recruit)) {
                recruit.restoreArmySnapshot(snapshot, groupId);
                return recruit;
            }
        }
        recruit.discard();
        return null;
    }

    private static OptionalDouble slowestMovementSpeed(ArmyGroupRecord group) {
        double slowest = Double.POSITIVE_INFINITY;
        boolean found = false;
        for (ArmyMemberSnapshot snapshot : group.snapshots()) {
            if (!group.contains(snapshot.recruitId())) {
                continue;
            }
            Optional<ArmyUnitDefinition> unit = GameplayDataManager.snapshot().unit(snapshot.unitId());
            if (unit.isEmpty()) {
                return OptionalDouble.empty();
            }
            ArmyUnitDefinition definition = unit.orElseThrow();
            int mobilityPercent = FactionBalanceService.resolve(
                    definition.factionId().toString()).mobilityPercent();
            slowest = Math.min(slowest,
                    effectiveMovementSpeed(definition.movementSpeed(), mobilityPercent));
            found = true;
        }
        return found ? OptionalDouble.of(slowest) : OptionalDouble.empty();
    }

    static double effectiveMovementSpeed(double baseMovementSpeed, int mobilityPercent) {
        if (!Double.isFinite(baseMovementSpeed) || baseMovementSpeed < 0.0D) {
            throw new IllegalArgumentException("baseMovementSpeed must be finite and non-negative");
        }
        int boundedPercent = Math.max(1,
                Math.min(FactionBalanceService.MAX_PERCENT, mobilityPercent));
        double adjusted = baseMovementSpeed * boundedPercent / 100.0D;
        return Math.max(MIN_EFFECTIVE_MOVEMENT_SPEED,
                Math.min(MAX_EFFECTIVE_MOVEMENT_SPEED, adjusted));
    }

    private static List<GalacticRecruitEntity> liveMembers(ServerLevel level, ArmyGroupRecord group) {
        ArrayList<GalacticRecruitEntity> live = new ArrayList<>();
        for (UUID id : expectedMembers(group)) {
            Entity entity = level.getEntity(id);
            if (entity instanceof GalacticRecruitEntity recruit) {
                live.add(recruit);
            }
        }
        return List.copyOf(live);
    }

    private static Set<UUID> expectedMembers(ArmyGroupRecord group) {
        LinkedHashSet<UUID> expected = new LinkedHashSet<>();
        group.commanderId().ifPresent(expected::add);
        expected.addAll(group.memberIds());
        return Set.copyOf(expected);
    }

    private static boolean playerNear(ServerLevel level, ArmyLocation location) {
        double radiusSquared = (double) PLAYER_RADIUS * PLAYER_RADIUS;
        return !level.getPlayers(player -> player.distanceToSqr(location.x(), location.y(), location.z()) <= radiusSquared)
                .isEmpty();
    }

    private static boolean playerNearLiveMember(
            ServerLevel level,
            List<GalacticRecruitEntity> liveMembers
    ) {
        double radiusSquared = (double) PLAYER_RADIUS * PLAYER_RADIUS;
        for (ServerPlayer player : level.players()) {
            for (GalacticRecruitEntity recruit : liveMembers) {
                if (player.distanceToSqr(recruit) <= radiusSquared) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<ArmyMemberSnapshot> orderedSnapshots(
            ArmyGroupRecord group,
            Map<UUID, ArmyMemberSnapshot> snapshots
    ) {
        ArrayList<ArmyMemberSnapshot> ordered = new ArrayList<>();
        group.commanderId().map(snapshots::get).ifPresent(ordered::add);
        for (UUID memberId : group.memberIds()) {
            ArmyMemberSnapshot snapshot = snapshots.get(memberId);
            if (snapshot != null) {
                ordered.add(snapshot);
            }
        }
        return List.copyOf(ordered);
    }

    private static List<BlockPos> safeGroundCandidates(ArmyPosition planned) {
        ArrayList<BlockPos> positions = new ArrayList<>();
        positions.add(new BlockPos(planned.x(), planned.y(), planned.z()));
        for (int radius = 1; radius <= 2; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) == radius) {
                        positions.add(new BlockPos(planned.x() + x, planned.y(), planned.z() + z));
                    }
                }
            }
        }
        return List.copyOf(positions);
    }

    private static void discardPartialMaterialization(ServerLevel level, ArmyGroupRecord group) {
        for (GalacticRecruitEntity recruit : liveMembers(level, group)) {
            if (recruit.getArmySnapshotGeneration() == group.simulation().snapshotGeneration()) {
                recruit.discard();
            }
        }
    }

    private static ServerLevel level(MinecraftServer server, String dimensionId) {
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, Identifier.parse(dimensionId));
        return server.getLevel(key);
    }

    private static ArmyLocation location(ServerPlayer player) {
        return new ArmyLocation(
                player.level().dimension().identifier().toString(),
                player.getX(), player.getY(), player.getZ());
    }

    private static BlockPos blockPos(ArmyLocation location) {
        return BlockPos.containing(location.x(), location.y(), location.z());
    }

    private static ArmyMemberSnapshot withGeneration(ArmyMemberSnapshot snapshot, long generation) {
        return new ArmyMemberSnapshot(
                snapshot.recruitId(), snapshot.entityTypeId(), snapshot.unitId(), snapshot.ownerId(), snapshot.kingdomId(),
                snapshot.duty(), snapshot.health(), snapshot.morale(), snapshot.hunger(), snapshot.unpaidTicks(),
                generation, snapshot.equipment(), snapshot.cargo(), snapshot.customName());
    }
}
