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

import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.data.GameplayDataManager;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = GalacticWars.MODID)
public final class ArmyRuntimeEvents {
    private static final int PLAYER_RADIUS = 128;
    private static final int HIBERNATE_DELAY_TICKS = 100;
    private static final Map<UUID, Integer> idleTicks = new LinkedHashMap<>();

    private ArmyRuntimeEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
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
                group = refreshAttackTarget(data, level, group);
                maybeHibernate(data, level, group);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof GalacticRecruitEntity recruit)
                || !event.loadedFromDisk()) {
            return;
        }
        Optional<ArmyGroupRecord> group = KingdomSavedData.get(level).armyGroupForRecruit(recruit.getUUID());
        if (group.isEmpty()) {
            return;
        }
        ArmyGroupRecord record = group.orElseThrow();
        if (record.simulation().lifecycleState() == ArmyGroupLifecycleState.VIRTUAL
                || recruit.getArmySnapshotGeneration() < record.simulation().snapshotGeneration()) {
            event.setCanceled(true);
        }
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
                "");
        ArmyGroupRecord virtual = group.withSimulation(simulation, orderedSnapshots(group, snapshots));
        if (!data.replaceArmyGroup(virtual, group.simulation().revision())) {
            return;
        }
        for (GalacticRecruitEntity recruit : liveMembers) {
            recruit.discard();
        }
        idleTicks.remove(group.id());
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
        List<ArmyPosition> positions = FormationPlanner.planPositions(
                group.simulation().anchor().blockPosition(),
                group.order().formation(),
                snapshots.size(),
                group.order().spacing());
        boolean complete = true;
        for (int index = 0; index < snapshots.size(); index++) {
            ArmyMemberSnapshot snapshot = snapshots.get(index);
            Entity existing = level.getEntity(snapshot.recruitId());
            if (existing instanceof GalacticRecruitEntity recruit
                    && recruit.getArmySnapshotGeneration() >= snapshot.generation()) {
                continue;
            }
            if (existing != null) {
                existing.discard();
            }
            ArmyPosition planned = positions.get(index);
            GalacticRecruitEntity recruit = createRecruit(level, snapshot, group.id(), planned);
            if (recruit == null || !level.addFreshEntity(recruit)) {
                complete = false;
                continue;
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
            slowest = Math.min(slowest, unit.orElseThrow().movementSpeed());
            found = true;
        }
        return found ? OptionalDouble.of(slowest) : OptionalDouble.empty();
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
                generation, snapshot.equipment(), snapshot.customName());
    }
}
