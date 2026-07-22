package galacticwars.clonewars.army;

import java.util.Objects;
import java.util.Optional;

public final class VirtualArmyMovementPlanner {
    public static final String HOLDING_POSITION = "holding_position";
    public static final String NO_MOVEMENT_ORDER = "no_movement_order";
    public static final String OWNER_UNAVAILABLE = "owner_unavailable";
    public static final String TARGET_UNAVAILABLE = "target_unavailable";
    public static final String DIMENSION_MISMATCH = "dimension_mismatch";
    public static final String DESTINATION_REACHED = "destination_reached";
    public static final String UNIT_DEFINITION_UNAVAILABLE = "unit_definition_unavailable";

    private VirtualArmyMovementPlanner() {
    }

    public static double blocksPerSecond(double slowestMovementSpeed) {
        if (!Double.isFinite(slowestMovementSpeed) || slowestMovementSpeed < 0.0D) {
            throw new IllegalArgumentException("slowestMovementSpeed must be finite and non-negative");
        }
        return Math.max(1.0D, Math.min(4.0D, slowestMovementSpeed * 10.0D));
    }

    public static VirtualArmyMovementDecision decide(
            ArmyGroupOrder order,
            ArmyLocation anchor,
            Optional<ArmyLocation> onlineOwnerLocation,
            double slowestMovementSpeed
    ) {
        return decide(order, anchor, onlineOwnerLocation, Optional.empty(), slowestMovementSpeed);
    }

    /**
     * Durable rally orders require the group's persisted rally point. The
     * legacy overload intentionally has no rally point and remains unchanged.
     */
    public static VirtualArmyMovementDecision decide(
            ArmyGroupOrder order,
            ArmyLocation anchor,
            Optional<ArmyLocation> onlineOwnerLocation,
            Optional<ArmyLocation> rallyPoint,
            double slowestMovementSpeed
    ) {
        Objects.requireNonNull(order, "order");
        Objects.requireNonNull(anchor, "anchor");
        onlineOwnerLocation = onlineOwnerLocation == null ? Optional.empty() : onlineOwnerLocation;
        rallyPoint = rallyPoint == null ? Optional.empty() : rallyPoint;

        if (order.type() == ArmyCommandType.HOLD_POSITION) {
            return new VirtualArmyMovementDecision(anchor, HOLDING_POSITION);
        }
        if (order.type() == ArmyCommandType.CLEAR_TARGET) {
            return new VirtualArmyMovementDecision(anchor, NO_MOVEMENT_ORDER);
        }

        Optional<ArmyLocation> destination = switch (order.type()) {
            case MOVE_TO_POSITION, ATTACK_TARGET, PATROL_ROUTE -> order.targetPosition();
            case FOLLOW_OWNER, PROTECT_OWNER -> onlineOwnerLocation;
            case RETURN_TO_RALLY -> rallyPoint;
            case PROTECT_ENTITY, HOLD_POSITION, CLEAR_TARGET -> Optional.empty();
        };
        if (destination.isEmpty()) {
            String reason = order.type() == ArmyCommandType.FOLLOW_OWNER
                    || order.type() == ArmyCommandType.PROTECT_OWNER
                    ? OWNER_UNAVAILABLE
                    : TARGET_UNAVAILABLE;
            return new VirtualArmyMovementDecision(anchor, reason);
        }

        ArmyLocation target = destination.orElseThrow();
        if (!target.dimensionId().equals(anchor.dimensionId())) {
            return new VirtualArmyMovementDecision(anchor, DIMENSION_MISMATCH);
        }

        double dx = target.x() - anchor.x();
        double dy = target.y() - anchor.y();
        double dz = target.z() - anchor.z();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance <= 0.5D) {
            return new VirtualArmyMovementDecision(target, DESTINATION_REACHED);
        }

        double ratio = Math.min(1.0D, blocksPerSecond(slowestMovementSpeed) / distance);
        return new VirtualArmyMovementDecision(new ArmyLocation(
                anchor.dimensionId(),
                anchor.x() + dx * ratio,
                anchor.y() + dy * ratio,
                anchor.z() + dz * ratio), "");
    }

    public static ArmyGroupRecord advance(
            ArmyGroupRecord group,
            Optional<ArmyLocation> onlineOwnerLocation,
            double slowestMovementSpeed,
            long gameTime
    ) {
        Objects.requireNonNull(group, "group");
        if (gameTime < 0L) {
            throw new IllegalArgumentException("gameTime cannot be negative");
        }
        if (group.order().type() == ArmyCommandType.PATROL_ROUTE
                && group.effectivePatrolPlan()
                        .map(ArmyPatrolPlan::state)
                        .map(ArmyPatrolState::status)
                        .filter(status -> status != ArmyPatrolStatus.ACTIVE)
                        .isPresent()) {
            ArmyPatrolStatus status = group.effectivePatrolPlan().orElseThrow().state().status();
            return status == ArmyPatrolStatus.PAUSED
                    ? advancePausedPatrolClock(group, gameTime)
                    : pause(group, "patrol_stopped", gameTime);
        }

        double effectiveSpeed = group.order().type() == ArmyCommandType.PATROL_ROUTE
                ? slowestMovementSpeed * group.effectivePatrolPlan()
                        .map(ArmyPatrolPlan::movementSpeed)
                        .orElse(ArmyPatrolPlan.DEFAULT_MOVEMENT_SPEED)
                : slowestMovementSpeed;
        VirtualArmyMovementDecision decision = decide(
                group.order(), group.simulation().anchor(), onlineOwnerLocation, group.rallyPoint(), effectiveSpeed);
        boolean patrolReachedWaypoint = group.order().type() == ArmyCommandType.PATROL_ROUTE
                && decision.pauseReason().equals(DESTINATION_REACHED);
        if (decision.anchor().equals(group.simulation().anchor())
                && decision.pauseReason().equals(group.simulation().blockedReason())
                && !patrolReachedWaypoint) {
            return group;
        }
        ArmyMarchState previousMarch = group.simulation().marchState();
        ArmyMarchPhase phase = decision.pauseReason().equals(DESTINATION_REACHED)
                ? ArmyMarchPhase.HALTED
                : decision.pauseReason().isEmpty() ? ArmyMarchPhase.MARCHING : ArmyMarchPhase.HALTED;
        ArmyMarchState nextMarch = previousMarch.transition(
                phase,
                group.order().formation(),
                100,
                ArmyLocation.yawToward(group.simulation().anchor(), decision.anchor(), previousMarch.yawDegrees()),
                gameTime);
        ArmyGroupSimulation simulation = group.simulation().advance(
                decision.anchor(), gameTime, decision.pauseReason(), nextMarch);
        ArmyGroupRecord advanced = group.withSimulation(simulation, group.snapshots());
        if (patrolReachedWaypoint) {
            return advancePatrolWaypoint(
                    advanced,
                    decision.anchor(),
                    elapsedTicksSinceLastSimulation(group, gameTime));
        }
        return advanced;
    }

    public static ArmyGroupRecord pause(ArmyGroupRecord group, String reason, long gameTime) {
        Objects.requireNonNull(group, "group");
        reason = Objects.requireNonNull(reason, "reason").trim();
        if (reason.isEmpty()) {
            throw new IllegalArgumentException("reason cannot be blank");
        }
        if (gameTime < 0L) {
            throw new IllegalArgumentException("gameTime cannot be negative");
        }
        if (reason.equals(group.simulation().blockedReason())) {
            return group;
        }
        return group.withSimulation(
                group.simulation().advance(group.simulation().anchor(), gameTime, reason),
                group.snapshots());
    }

    private static ArmyGroupRecord advancePatrolWaypoint(
            ArmyGroupRecord group,
            ArmyLocation currentLocation,
            int elapsedTicks
    ) {
        return ArmyPatrolOrderPlanner.advance(group, currentLocation.blockPosition(), elapsedTicks)
                .map(decision -> group.withPatrolProgressAndOrder(decision.nextPlan(), decision.nextOrder()))
                .orElse(group);
    }

    private static int elapsedTicksSinceLastSimulation(ArmyGroupRecord group, long gameTime) {
        long elapsedTicks = Math.max(0L, gameTime - group.simulation().lastSimulationGameTime());
        return (int)Math.min(Integer.MAX_VALUE, elapsedTicks);
    }

    /**
     * A paused virtual patrol must preserve its route state but still record
     * wall-clock progress. Otherwise a later resume would treat the entire
     * pause duration as active dwell time. Stopped patrols deliberately use
     * {@link #pause(ArmyGroupRecord, String, long)} and remain stable instead.
     */
    private static ArmyGroupRecord advancePausedPatrolClock(ArmyGroupRecord group, long gameTime) {
        ArmyGroupSimulation simulation = group.simulation();
        if (gameTime <= simulation.lastSimulationGameTime()
                && simulation.blockedReason().equals("patrol_paused")) {
            return group;
        }
        return group.withSimulation(
                simulation.advance(simulation.anchor(), gameTime, "patrol_paused"),
                group.snapshots());
    }
}
