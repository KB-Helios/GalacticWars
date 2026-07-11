package galacticwars.clonewars.army;

import java.util.Objects;
import java.util.UUID;

public record ArmyBehaviorDecision(
        ArmyBehaviorIntent intent,
        ArmyPosition moveTarget,
        UUID attackTargetId,
        String reasonCode
) {
    public ArmyBehaviorDecision {
        Objects.requireNonNull(intent, "intent");
        reasonCode = requireNonBlank(reasonCode);
    }

    public static ArmyBehaviorDecision idle(String reasonCode) {
        return new ArmyBehaviorDecision(ArmyBehaviorIntent.IDLE, null, null, reasonCode);
    }

    public static ArmyBehaviorDecision move(ArmyPosition target, String reasonCode) {
        return new ArmyBehaviorDecision(ArmyBehaviorIntent.MOVE_TO_POSITION, Objects.requireNonNull(target, "target"), null, reasonCode);
    }

    public static ArmyBehaviorDecision hold(ArmyPosition target, String reasonCode) {
        return new ArmyBehaviorDecision(ArmyBehaviorIntent.HOLD_POSITION, Objects.requireNonNull(target, "target"), null, reasonCode);
    }

    public static ArmyBehaviorDecision follow(ArmyPosition target, String reasonCode) {
        return new ArmyBehaviorDecision(ArmyBehaviorIntent.FOLLOW_OWNER, Objects.requireNonNull(target, "target"), null, reasonCode);
    }

    public static ArmyBehaviorDecision protect(ArmyPosition target, String reasonCode) {
        return new ArmyBehaviorDecision(ArmyBehaviorIntent.PROTECT_OWNER, Objects.requireNonNull(target, "target"), null, reasonCode);
    }

    public static ArmyBehaviorDecision attack(UUID targetId, String reasonCode) {
        return new ArmyBehaviorDecision(ArmyBehaviorIntent.ATTACK_TARGET, null, Objects.requireNonNull(targetId, "targetId"), reasonCode);
    }

    private static String requireNonBlank(String value) {
        Objects.requireNonNull(value, "reasonCode");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("reasonCode cannot be blank");
        }
        return trimmed;
    }
}
