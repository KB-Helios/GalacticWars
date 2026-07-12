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
        Objects.requireNonNull(order, "order");
        Objects.requireNonNull(anchor, "anchor");
        onlineOwnerLocation = onlineOwnerLocation == null ? Optional.empty() : onlineOwnerLocation;

        if (order.type() == ArmyCommandType.HOLD_POSITION) {
            return new VirtualArmyMovementDecision(anchor, HOLDING_POSITION);
        }
        if (order.type() == ArmyCommandType.CLEAR_TARGET) {
            return new VirtualArmyMovementDecision(anchor, NO_MOVEMENT_ORDER);
        }

        Optional<ArmyLocation> destination = switch (order.type()) {
            case MOVE_TO_POSITION, ATTACK_TARGET, PATROL_ROUTE -> order.targetPosition();
            case FOLLOW_OWNER, PROTECT_OWNER -> onlineOwnerLocation;
            case HOLD_POSITION, CLEAR_TARGET -> Optional.empty();
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
        VirtualArmyMovementDecision decision = decide(
                group.order(), group.simulation().anchor(), onlineOwnerLocation, slowestMovementSpeed);
        if (decision.anchor().equals(group.simulation().anchor())
                && decision.pauseReason().equals(group.simulation().blockedReason())) {
            return group;
        }
        ArmyGroupSimulation simulation = group.simulation().advance(
                decision.anchor(), gameTime, decision.pauseReason());
        ArmyGroupRecord advanced = group.withSimulation(simulation, group.snapshots());
        if (group.order().type() == ArmyCommandType.PATROL_ROUTE
                && decision.pauseReason().equals(DESTINATION_REACHED)
                && group.patrolRoute().size() >= 2) {
            return advancePatrolWaypoint(advanced, decision.anchor());
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

    private static ArmyGroupRecord advancePatrolWaypoint(ArmyGroupRecord group, ArmyLocation currentLocation) {
        ArmyGroupOrder nextOrder = ArmyPatrolOrderPlanner.nextOrder(
                group, currentLocation.blockPosition());
        return nextOrder.equals(group.order()) ? group : group.withOrder(nextOrder);
    }
}
