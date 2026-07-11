package galacticwars.clonewars.army;

import java.util.Objects;

public record ArmyEngagementDecision(
        boolean engaging,
        ArmyBehaviorDecision behaviorDecision,
        ArmyTargetSelection targetSelection,
        String reasonCode
) {
    public ArmyEngagementDecision {
        Objects.requireNonNull(behaviorDecision, "behaviorDecision");
        reasonCode = requireNonBlank(reasonCode);
        if (engaging) {
            Objects.requireNonNull(targetSelection, "targetSelection");
            if (behaviorDecision.intent() != ArmyBehaviorIntent.ATTACK_TARGET) {
                throw new IllegalArgumentException("engaging decisions must attack a target");
            }
        } else if (targetSelection != null) {
            throw new IllegalArgumentException("idle decisions cannot include a target selection");
        }
    }

    public static ArmyEngagementDecision engage(ArmyTargetSelection selection) {
        Objects.requireNonNull(selection, "selection");
        return new ArmyEngagementDecision(
                true,
                ArmyBehaviorDecision.attack(selection.targetId(), selection.reasonCode()),
                selection,
                "engaging");
    }

    public static ArmyEngagementDecision idle(String reasonCode) {
        return new ArmyEngagementDecision(false, ArmyBehaviorDecision.idle(reasonCode), null, reasonCode);
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
