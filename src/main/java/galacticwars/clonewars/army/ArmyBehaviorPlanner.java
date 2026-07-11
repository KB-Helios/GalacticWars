package galacticwars.clonewars.army;

import java.util.Objects;
import java.util.UUID;

public final class ArmyBehaviorPlanner {
    private ArmyBehaviorPlanner() {
    }

    public static ArmyBehaviorDecision plan(RecruitState recruit, ArmyBehaviorContext context) {
        Objects.requireNonNull(recruit, "recruit");
        Objects.requireNonNull(context, "context");
        ArmyCommand command = recruit.currentCommand();

        return switch (command.type()) {
            case FOLLOW_OWNER -> planFollowOwner(context, "owner_out_of_range");
            case MOVE_TO_POSITION -> ArmyBehaviorDecision.move(command.targetPosition(), "move_command");
            case HOLD_POSITION -> ArmyBehaviorDecision.hold(command.targetPosition(), "hold_command");
            case PROTECT_OWNER -> planProtectOwner(context);
            case ATTACK_TARGET -> planAttack(command.targetEntityId(), context);
            case CLEAR_TARGET -> ArmyBehaviorDecision.idle("target_cleared");
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
        if (targetEntityId != null && context.commandTargetAlive()) {
            return ArmyBehaviorDecision.attack(targetEntityId, "attack_command");
        }
        return ArmyBehaviorDecision.idle("target_unavailable");
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
