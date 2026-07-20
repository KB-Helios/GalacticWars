package galacticwars.clonewars.army;

import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.item.CommandTargetSelection;
import galacticwars.clonewars.kingdom.KingdomPermission;
import galacticwars.clonewars.kingdom.KingdomRecord;
import galacticwars.clonewars.kingdom.KingdomSavedData;
import galacticwars.clonewars.network.FieldCommandRequestPayload;
import galacticwars.clonewars.network.FieldCommandStatePayload;
import galacticwars.clonewars.recruitment.NpcServiceBranch;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;

/**
 * Server-authoritative entry point for field-command requests.
 *
 * <p>The client submits only a bounded squad selection and a replay token.
 * Actor identity comes from the packet context; marker targets, facing, and
 * temporary advance/retreat locations are all resolved on the server. This
 * service deliberately permits kingdom officers with {@code COMMAND_ARMY}, as
 * the existing kingdom command model does, while deriving the durable squad
 * owner from SavedData for structural command validation.</p>
 */
public final class ArmyFieldCommandService {
    private static final double MAX_TARGET_DISTANCE_SQUARED = 32.0D * 32.0D;
    private static final double MAX_FIELD_SQUAD_DISTANCE_SQUARED = 96.0D * 96.0D;
    private static final int FIELD_ADVANCE_DISTANCE = 12;
    private static final List<Double> PATROL_SPEEDS = List.of(0.75D, 1.0D, 1.25D, 1.5D);
    private static final ArmyFieldCommandReplayGuard REPLAY_GUARD =
            new ArmyFieldCommandReplayGuard(256, 128);

    private ArmyFieldCommandService() {
    }

    /** Releases the bounded replay window when a player leaves the server. */
    public static void clearReplayHistory(UUID playerId) {
        REPLAY_GUARD.clear(Objects.requireNonNull(playerId, "playerId"));
    }

    public static FieldCommandStatePayload execute(ServerPlayer player, FieldCommandRequestPayload request) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(request, "request");
        if (!REPLAY_GUARD.claim(player.getUUID(), request.replayId())) {
            return captureState(player, request.replayId(), FieldCommandResult.REPLAY_REJECTED);
        }

        return captureState(player, request.replayId(), executeFresh(player, request));
    }

    private static FieldCommandResult executeFresh(ServerPlayer player, FieldCommandRequestPayload request) {
        KingdomSavedData data = KingdomSavedData.get(player.level());
        KingdomRecord kingdom = commandKingdom(data, player);
        if (kingdom == null) {
            return data.kingdomForPlayer(player.getUUID()).isPresent()
                    ? FieldCommandResult.PERMISSION_DENIED
                    : FieldCommandResult.KINGDOM_UNAVAILABLE;
        }
        if (request.action() == FieldCommandAction.REFRESH) {
            return FieldCommandResult.ACCEPTED;
        }

        List<ArmyGroupRecord> groups = selectedFieldGroups(data, player, kingdom, request.groupIds());
        if (groups == null) {
            return FieldCommandResult.SQUAD_UNAVAILABLE;
        }

        return switch (request.action()) {
            case FOLLOW -> applyOrders(data, player, groups, group -> order(
                    ArmyCommandType.FOLLOW_OWNER, group, Optional.empty(), Optional.empty()));
            case HOLD -> applyOrders(data, player, groups, group -> liveCommanderLocation(player, group)
                    .map(anchor -> order(ArmyCommandType.HOLD_POSITION, group, Optional.of(anchor), Optional.empty()))
                    .orElse(null));
            case MOVE_TO_MARKER -> markedBlockLocation(player)
                    .map(target -> applyOrders(data, player, groups, group -> order(
                            ArmyCommandType.MOVE_TO_POSITION, group, Optional.of(target), Optional.empty())))
                    .orElse(FieldCommandResult.TARGET_REQUIRED);
            case FACE_FORWARD -> applyTactics(data, player, groups,
                    tactics -> tactics.withFormationYaw(player.getYRot()));
            case ADVANCE -> forwardLocation(player, FIELD_ADVANCE_DISTANCE)
                    .map(target -> applyOrders(data, player, groups, group -> order(
                            ArmyCommandType.MOVE_TO_POSITION, group, Optional.of(target), Optional.empty())))
                    .orElse(FieldCommandResult.TARGET_UNAVAILABLE);
            case RETREAT -> forwardLocation(player, -FIELD_ADVANCE_DISTANCE)
                    .map(target -> applyOrders(data, player, groups, group -> order(
                            ArmyCommandType.MOVE_TO_POSITION, group, Optional.of(target), Optional.empty())))
                    .orElse(FieldCommandResult.TARGET_UNAVAILABLE);
            case RETURN_TO_RALLY -> applyOrders(data, player, groups, group -> {
                if (group.rallyPoint().isEmpty()) {
                    return null;
                }
                return order(ArmyCommandType.RETURN_TO_RALLY, group, Optional.empty(), Optional.empty());
            });
            case PROTECT_OWNER -> applyOrders(data, player, groups, group -> order(
                    ArmyCommandType.PROTECT_OWNER, group, Optional.empty(), Optional.empty()));
            case PROTECT_MARKED_ENTITY -> applyMarkedProtection(data, player, groups);
            case ATTACK_MARKED_TARGET -> applyMarkedAttack(data, player, groups);
            case CLEAR_TARGET -> applyOrders(data, player, groups, group -> order(
                    ArmyCommandType.CLEAR_TARGET, group, Optional.empty(), Optional.empty()));
            case CYCLE_FORMATION -> applyOrders(data, player, groups,
                    group -> group.order().withFormation(next(group.order().formation(), ArmyFormation.values())));
            case TOGGLE_HOLD_FORMATION -> applyTactics(data, player, groups,
                    tactics -> tactics.withFormationControls(!tactics.holdFormation(), tactics.tightFormation()));
            case TOGGLE_TIGHT_FORMATION -> applyTactics(data, player, groups,
                    tactics -> tactics.withFormationControls(tactics.holdFormation(), !tactics.tightFormation()));
            case CYCLE_ENGAGEMENT -> applyTactics(data, player, groups,
                    tactics -> tactics.withEngagement(next(tactics.engagementStance(), ArmyEngagementStance.values())));
            case CYCLE_TARGET_PRIORITY -> applyTactics(data, player, groups,
                    tactics -> tactics.withTargetPriority(next(tactics.targetPriority(), ArmyTargetPriority.values())));
            case CYCLE_RANGED_FIRE -> applyTactics(data, player, groups,
                    tactics -> tactics.withRangedFirePolicy(next(tactics.rangedFirePolicy(), ArmyRangedFirePolicy.values())));
            case PATROL_MARKER -> markedBlockLocation(player)
                    .map(center -> applyPatrols(data, player, groups, center,
                            patrolRouteNameOrDefault(request), request.patrolWaypointWaitTicks()))
                    .orElse(FieldCommandResult.TARGET_REQUIRED);
            case RENAME_PATROL_ROUTE -> requestedPatrolRouteName(request)
                    .map(name -> applyPatrolChange(data, player, groups, plan -> plan.withName(name)))
                    .orElse(FieldCommandResult.INVALID_ACTION);
            case SET_PATROL_WAYPOINT_WAIT -> applyPatrolChange(data, player, groups,
                    plan -> plan.withWaypointWaitTicks(
                            request.patrolWaypointIndex(), request.patrolWaypointWaitTicks()));
            case PAUSE_PATROL -> applyPatrolChange(data, player, groups, ArmyPatrolPlan::pause);
            case RESUME_PATROL -> applyPatrolChange(data, player, groups, ArmyPatrolPlan::resume);
            case STOP_PATROL -> applyPatrolChange(data, player, groups, ArmyPatrolPlan::stop);
            case CYCLE_PATROL_MODE -> applyPatrolChange(data, player, groups, plan -> new ArmyPatrolPlan(
                    plan.waypoints(), next(plan.mode(), ArmyPatrolMode.values()), plan.state(),
                    plan.arrivalDistance(), plan.movementSpeed(), plan.enemyPolicy(), plan.name()));
            case CYCLE_PATROL_SPEED -> applyPatrolChange(data, player, groups, plan -> new ArmyPatrolPlan(
                    plan.waypoints(), plan.mode(), plan.state(), plan.arrivalDistance(),
                    nextPatrolSpeed(plan.movementSpeed()), plan.enemyPolicy(), plan.name()));
            case CYCLE_PATROL_ENEMY_POLICY -> applyPatrolChange(data, player, groups, plan -> new ArmyPatrolPlan(
                    plan.waypoints(), plan.mode(), plan.state(), plan.arrivalDistance(),
                    plan.movementSpeed(), next(plan.enemyPolicy(), ArmyPatrolEnemyPolicy.values()), plan.name()));
            case REFRESH -> FieldCommandResult.ACCEPTED;
        };
    }

    private static KingdomRecord commandKingdom(KingdomSavedData data, ServerPlayer player) {
        KingdomRecord kingdom = data.kingdomForPlayer(player.getUUID()).orElse(null);
        return kingdom != null && kingdom.allows(player.getUUID(), KingdomPermission.COMMAND_ARMY)
                ? kingdom
                : null;
    }

    private static List<ArmyGroupRecord> selectedFieldGroups(
            KingdomSavedData data,
            ServerPlayer player,
            KingdomRecord kingdom,
            List<UUID> requestedIds
    ) {
        if (requestedIds.isEmpty()) {
            return null;
        }
        Map<UUID, ArmyGroupRecord> groupsById = fieldCommandableGroups(data, player, kingdom).stream()
                .collect(java.util.stream.Collectors.toMap(
                        ArmyGroupRecord::id, Function.identity(), (first, second) -> first, LinkedHashMap::new));
        ArrayList<ArmyGroupRecord> selected = new ArrayList<>(requestedIds.size());
        for (UUID id : requestedIds) {
            ArmyGroupRecord group = groupsById.get(id);
            if (group == null) {
                return null;
            }
            selected.add(group);
        }
        return List.copyOf(selected);
    }

    private static List<ArmyGroupRecord> fieldCommandableGroups(
            KingdomSavedData data,
            ServerPlayer player,
            KingdomRecord kingdom
    ) {
        return data.armyGroupsForKingdom(kingdom.id()).stream()
                .filter(group -> isFieldCommandable(player, group))
                .sorted(Comparator.comparing(ArmyGroupRecord::name).thenComparing(ArmyGroupRecord::id))
                .limit(FieldCommandStatePayload.MAX_SQUADS)
                .toList();
    }

    private static boolean isFieldCommandable(ServerPlayer player, ArmyGroupRecord group) {
        if (group.simulation().lifecycleState() != ArmyGroupLifecycleState.LIVE || group.commanderId().isEmpty()) {
            return false;
        }
        Entity entity = player.level().getEntity(group.commanderId().orElseThrow());
        if (!(entity instanceof GalacticRecruitEntity commander)
                || !commander.isAlive()
                || commander.getServiceBranch() != NpcServiceBranch.MILITARY
                || !group.contains(commander.getUUID())) {
            return false;
        }
        return player.distanceToSqr(commander) <= MAX_FIELD_SQUAD_DISTANCE_SQUARED;
    }

    /**
     * A field Hold is anchored where the loaded commander is standing now,
     * not at the persistent simulation anchor. The latter is intentionally
     * updated for virtual travel/hibernate transitions and can be stale while
     * a live squad has followed the player across the battlefield.
     */
    private static Optional<ArmyLocation> liveCommanderLocation(ServerPlayer player, ArmyGroupRecord group) {
        return group.commanderId()
                .map(player.level()::getEntity)
                .filter(GalacticRecruitEntity.class::isInstance)
                .map(GalacticRecruitEntity.class::cast)
                .filter(Entity::isAlive)
                .map(commander -> new ArmyLocation(
                        player.level().dimension().identifier().toString(),
                        commander.getX(), commander.getY(), commander.getZ()));
    }

    private static FieldCommandResult applyMarkedProtection(
            KingdomSavedData data,
            ServerPlayer player,
            List<ArmyGroupRecord> groups
    ) {
        LivingEntity target = CommandTargetSelection.entityFromInventory(player).orElse(null);
        if (target == null) {
            return FieldCommandResult.TARGET_REQUIRED;
        }
        if (!isAllowedProtectionTarget(player, groups, target)) {
            return FieldCommandResult.TARGET_UNAVAILABLE;
        }
        return applyOrders(data, player, groups, group -> order(
                ArmyCommandType.PROTECT_ENTITY, group, Optional.empty(), Optional.of(target.getUUID())));
    }

    private static FieldCommandResult applyMarkedAttack(
            KingdomSavedData data,
            ServerPlayer player,
            List<ArmyGroupRecord> groups
    ) {
        LivingEntity target = CommandTargetSelection.entityFromInventory(player).orElse(null);
        if (target == null) {
            return FieldCommandResult.TARGET_REQUIRED;
        }
        if (!isAllowedAttackTarget(player, groups, target)) {
            return FieldCommandResult.TARGET_UNAVAILABLE;
        }
        ArmyLocation location = new ArmyLocation(
                player.level().dimension().identifier().toString(), target.getX(), target.getY(), target.getZ());
        return applyOrders(data, player, groups, group -> order(
                ArmyCommandType.ATTACK_TARGET, group, Optional.of(location), Optional.of(target.getUUID())));
    }

    private static boolean isAllowedProtectionTarget(
            ServerPlayer player,
            List<ArmyGroupRecord> groups,
            LivingEntity target
    ) {
        return target.isAlive() && !target.isInvulnerable()
                && player.distanceToSqr(target) <= MAX_TARGET_DISTANCE_SQUARED
                && groups.stream().noneMatch(group -> group.contains(target.getUUID()));
    }

    private static boolean isAllowedAttackTarget(
            ServerPlayer player,
            List<ArmyGroupRecord> groups,
            LivingEntity target
    ) {
        if (!isAllowedProtectionTarget(player, groups, target) || target == player) {
            return false;
        }
        if (target instanceof Monster) {
            return true;
        }
        if (!(target instanceof GalacticRecruitEntity recruit)) {
            return false;
        }
        ServerLevel level = player.level();
        return groups.stream()
                .flatMap(group -> java.util.stream.Stream.concat(group.commanderId().stream(), group.memberIds().stream()))
                .map(level::getEntity)
                .filter(GalacticRecruitEntity.class::isInstance)
                .map(GalacticRecruitEntity.class::cast)
                .filter(source -> source.isAlive() && source.getServiceBranch() == NpcServiceBranch.MILITARY)
                .anyMatch(source -> source.isHostileFactionRecruit(recruit));
    }

    private static FieldCommandResult applyOrders(
            KingdomSavedData data,
            ServerPlayer player,
            List<ArmyGroupRecord> groups,
            Function<ArmyGroupRecord, ArmyGroupOrder> orderFactory
    ) {
        return applyUpdates(data, player, groups, group -> {
            ArmyGroupOrder order = orderFactory.apply(group);
            return order == null ? null : group.withOrder(order);
        });
    }

    private static FieldCommandResult applyTactics(
            KingdomSavedData data,
            ServerPlayer player,
            List<ArmyGroupRecord> groups,
            UnaryOperator<ArmyGroupTactics> mutation
    ) {
        return applyUpdates(data, player, groups, group -> group.withOrder(group.order())
                .withTactics(mutation.apply(group.effectiveTactics())));
    }

    private static FieldCommandResult applyPatrols(
            KingdomSavedData data,
            ServerPlayer player,
            List<ArmyGroupRecord> groups,
            ArmyLocation center,
            String name,
            int waitTicks
    ) {
        List<ArmyPatrolWaypoint> waypoints = List.of(
                patrolWaypoint(center, waitTicks),
                patrolWaypoint(new ArmyLocation(
                        center.dimensionId(), center.x() + 16.0D, center.y(), center.z()), waitTicks),
                patrolWaypoint(new ArmyLocation(
                        center.dimensionId(), center.x() + 16.0D, center.y(), center.z() + 16.0D), waitTicks),
                patrolWaypoint(new ArmyLocation(
                        center.dimensionId(), center.x(), center.y(), center.z() + 16.0D), waitTicks));
        ArmyPatrolPlan plan = new ArmyPatrolPlan(waypoints, ArmyPatrolMode.LOOP, ArmyPatrolState.start(),
                ArmyPatrolPlan.DEFAULT_ARRIVAL_DISTANCE, ArmyPatrolPlan.DEFAULT_MOVEMENT_SPEED,
                ArmyPatrolEnemyPolicy.ENGAGE_HOSTILES, name);
        return applyUpdates(data, player, groups, group -> group.withRallyPoint(center)
                .withPatrolPlanAndOrder(plan, order(ArmyCommandType.PATROL_ROUTE, group,
                        Optional.of(center), Optional.empty())));
    }

    private static FieldCommandResult applyPatrolChange(
            KingdomSavedData data,
            ServerPlayer player,
            List<ArmyGroupRecord> groups,
            UnaryOperator<ArmyPatrolPlan> mutation
    ) {
        return applyUpdates(data, player, groups, group -> group.effectivePatrolPlan()
                .filter(ignored -> group.order().type() == ArmyCommandType.PATROL_ROUTE)
                .flatMap(plan -> safelyMutatePatrol(group, plan, mutation))
                .orElse(null));
    }

    /** Converts malformed-but-bounded client waypoint indexes into a clean rejected batch. */
    private static Optional<ArmyGroupRecord> safelyMutatePatrol(
            ArmyGroupRecord group,
            ArmyPatrolPlan plan,
            UnaryOperator<ArmyPatrolPlan> mutation
    ) {
        try {
            return Optional.of(group.withPatrolPlanAndOrder(mutation.apply(plan), group.order()));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    /**
     * Preflights every update, then delegates the single SavedData write to a
     * compare-and-swap batch. Officers remain allowed by kingdom permission;
     * the stored group owner is used only for existing structural policy rules.
     */
    private static FieldCommandResult applyUpdates(
            KingdomSavedData data,
            ServerPlayer player,
            List<ArmyGroupRecord> groups,
            Function<ArmyGroupRecord, ArmyGroupRecord> updateFactory
    ) {
        Optional<ArmyFieldCommandBatch> batch = ArmyFieldCommandBatch.prepare(groups, updateFactory);
        if (batch.isEmpty()) {
            return FieldCommandResult.INVALID_ACTION;
        }
        ArmyFieldCommandBatch prepared = batch.orElseThrow();
        return data.replaceArmyGroupsAtomically(
                player.getUUID(), prepared.replacements(), prepared.expectedRevisions())
                ? FieldCommandResult.ACCEPTED
                : FieldCommandResult.ATOMIC_APPLY_FAILED;
    }

    private static ArmyGroupOrder order(
            ArmyCommandType type,
            ArmyGroupRecord group,
            Optional<ArmyLocation> position,
            Optional<UUID> targetEntity
    ) {
        return new ArmyGroupOrder(type, position, targetEntity, group.order().formation(), group.order().spacing());
    }

    private static Optional<ArmyLocation> markedBlockLocation(ServerPlayer player) {
        return CommandTargetSelection.blockFromInventory(player)
                .filter(position -> player.level().hasChunkAt(position))
                .filter(position -> distanceSquared(player, position.getX() + 0.5D,
                        position.getY() + 0.5D, position.getZ() + 0.5D) <= MAX_TARGET_DISTANCE_SQUARED)
                .map(position -> new ArmyLocation(player.level().dimension().identifier().toString(),
                        position.getX(), position.getY(), position.getZ()));
    }

    private static Optional<ArmyLocation> forwardLocation(ServerPlayer player, int distance) {
        double radians = Math.toRadians(player.getYRot());
        double x = player.getX() - Math.sin(radians) * distance;
        double z = player.getZ() + Math.cos(radians) * distance;
        BlockPos position = BlockPos.containing(x, player.getY(), z);
        if (!player.level().hasChunkAt(position)
                || distanceSquared(player, x, player.getY(), z) > MAX_TARGET_DISTANCE_SQUARED) {
            return Optional.empty();
        }
        return Optional.of(new ArmyLocation(player.level().dimension().identifier().toString(),
                position.getX(), position.getY(), position.getZ()));
    }

    private static double distanceSquared(ServerPlayer player, double x, double y, double z) {
        double deltaX = player.getX() - x;
        double deltaY = player.getY() - y;
        double deltaZ = player.getZ() - z;
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
    }

    private static double nextPatrolSpeed(double current) {
        int index = 0;
        for (int candidate = 0; candidate < PATROL_SPEEDS.size(); candidate++) {
            if (Math.abs(PATROL_SPEEDS.get(candidate) - current) < 0.001D) {
                index = candidate;
                break;
            }
        }
        return PATROL_SPEEDS.get((index + 1) % PATROL_SPEEDS.size());
    }

    private static ArmyPatrolWaypoint patrolWaypoint(ArmyLocation location, int waitTicks) {
        return new ArmyPatrolWaypoint(location, waitTicks);
    }

    private static String patrolRouteNameOrDefault(FieldCommandRequestPayload request) {
        return requestedPatrolRouteName(request).orElse(ArmyPatrolPlan.DEFAULT_NAME);
    }

    private static Optional<String> requestedPatrolRouteName(FieldCommandRequestPayload request) {
        String name = request.patrolRouteName();
        return name.isBlank() ? Optional.empty() : Optional.of(name);
    }

    private static <T> T next(T current, T[] values) {
        int index = java.util.Arrays.asList(values).indexOf(current);
        return values[(Math.max(index, 0) + 1) % values.length];
    }

    private static FieldCommandStatePayload captureState(
            ServerPlayer player,
            UUID replayId,
            FieldCommandResult result
    ) {
        KingdomSavedData data = KingdomSavedData.get(player.level());
        KingdomRecord kingdom = commandKingdom(data, player);
        List<FieldCommandStatePayload.Squad> squads = kingdom == null
                ? List.of()
                : fieldCommandableGroups(data, player, kingdom).stream()
                        .map(FieldCommandStatePayload.Squad::from)
                        .toList();
        return new FieldCommandStatePayload(
                replayId,
                result,
                squads,
                markedBlockLocation(player).isPresent(),
                CommandTargetSelection.entityFromInventory(player).isPresent());
    }

}
