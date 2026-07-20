package galacticwars.clonewars.army;

import java.util.Objects;
import java.util.UUID;

public final class ArmyBehaviorPlanner {
    private ArmyBehaviorPlanner() {
    }

    public static ArmyBehaviorDecision plan(RecruitState recruit, ArmyBehaviorContext context) {
        return plan(recruit, context, null, null, null);
    }

    /**
     * Extended pure context for durable protect/rally orders. Existing callers
     * retain the two-argument overload and therefore legacy behavior.
     */
    public static ArmyBehaviorDecision plan(
            RecruitState recruit,
            ArmyBehaviorContext context,
            ArmyPosition protectedEntityPosition,
            UUID visibleThreatToProtectedEntity,
            ArmyPosition rallyPosition
    ) {
        Objects.requireNonNull(recruit, "recruit");
        Objects.requireNonNull(context, "context");
        ArmyCommand command = recruit.currentCommand();

        return switch (command.type()) {
            case FOLLOW_OWNER -> planFollowOwner(context, "owner_out_of_range");
            case MOVE_TO_POSITION -> ArmyBehaviorDecision.move(command.targetPosition(), "move_command");
            case HOLD_POSITION -> ArmyBehaviorDecision.hold(command.targetPosition(), "hold_command");
            case PROTECT_OWNER -> planProtectOwner(context);
            case PROTECT_ENTITY -> planProtectEntity(
                    protectedEntityPosition, visibleThreatToProtectedEntity, context.followRange());
            case ATTACK_TARGET -> planAttack(command.targetEntityId(), context);
            case CLEAR_TARGET -> ArmyBehaviorDecision.idle("target_cleared");
            case RETURN_TO_RALLY -> rallyPosition == null
                    ? ArmyBehaviorDecision.idle("rally_target_required")
                    : ArmyBehaviorDecision.move(rallyPosition, "return_to_rally");
            case PATROL_ROUTE -> ArmyBehaviorDecision.move(command.targetPosition(), "patrol_route");
        };
    }

    private static ArmyBehaviorDecision planFollowOwner(ArmyBehaviorContext context, String followReason) {
        if (horizontalDistanceSquared(context.selfPosition(), context.ownerPosition()) > squared(context.followRange())) {
            return ArmyBehaviorDecision.follow(context.ownerPosition(), followReason);
        }
        return ArmyBehaviorDecision.idle("within_follow_range");
    }

    private static ArmyBehaviorDecision planProtectOwner(ArmyBehaviorContext context) {
        UUID visibleThreat = context.visibleThreatToOwner();
        if (visibleThreat != null) {
            return ArmyBehaviorDecision.attack(visibleThreat, "owner_threat_visible");
        }
        if (horizontalDistanceSquared(context.selfPosition(), context.ownerPosition()) > squared(context.followRange())) {
            return ArmyBehaviorDecision.follow(context.ownerPosition(), "protect_follow_owner");
        }
        return ArmyBehaviorDecision.protect(context.ownerPosition(), "protect_owner");
    }

    private static ArmyBehaviorDecision planAttack(UUID targetEntityId, ArmyBehaviorContext context) {
        return planAttack(targetEntityId, context, "target_unavailable");
    }

    private static ArmyBehaviorDecision planAttack(
            UUID targetEntityId,
            ArmyBehaviorContext context,
            String unavailableReason
    ) {
        if (targetEntityId != null && context.commandTargetAlive()) {
            return ArmyBehaviorDecision.attack(targetEntityId, "attack_command");
        }
        return ArmyBehaviorDecision.idle(unavailableReason);
    }

    private static ArmyBehaviorDecision planProtectEntity(
            ArmyPosition protectedEntityPosition,
            UUID visibleThreat,
            int followRange
    ) {
        if (visibleThreat != null) {
            return ArmyBehaviorDecision.attack(visibleThreat, "protected_entity_threat_visible");
        }
        if (protectedEntityPosition == null) {
            return ArmyBehaviorDecision.idle("protected_entity_unavailable");
        }
        return ArmyBehaviorDecision.protect(protectedEntityPosition,
                followRange > 0 ? "protect_entity" : "protect_entity_stationary");
    }

    private static int horizontalDistanceSquared(ArmyPosition first, ArmyPosition second) {
        int x = first.x() - second.x();
        int z = first.z() - second.z();
        return (x * x) + (z * z);
    }

    private static int squared(int value) {
        return value * value;
    }
}
