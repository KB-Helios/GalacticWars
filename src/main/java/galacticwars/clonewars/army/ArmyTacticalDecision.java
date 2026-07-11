package galacticwars.clonewars.army;

import java.util.Objects;

public record ArmyTacticalDecision(
        ArmyTacticalIntent intent,
        ArmyBehaviorDecision behaviorDecision,
        ArmyPosition tacticalTarget,
        String reasonCode
) {
    public ArmyTacticalDecision {
        Objects.requireNonNull(intent, "intent");
        Objects.requireNonNull(behaviorDecision, "behaviorDecision");
        reasonCode = requireNonBlank(reasonCode);
        if (intent != ArmyTacticalIntent.EXECUTE_ORDER) {
            Objects.requireNonNull(tacticalTarget, "tacticalTarget");
        }
    }

    public static ArmyTacticalDecision execute(ArmyBehaviorDecision behaviorDecision) {
        return new ArmyTacticalDecision(ArmyTacticalIntent.EXECUTE_ORDER, behaviorDecision, null, "ready");
    }

    public static ArmyTacticalDecision retreat(
            ArmyBehaviorDecision behaviorDecision,
            ArmyPosition tacticalTarget,
            String reasonCode
    ) {
        return new ArmyTacticalDecision(ArmyTacticalIntent.RETREAT, behaviorDecision, tacticalTarget, reasonCode);
    }

    public static ArmyTacticalDecision regroup(
            ArmyBehaviorDecision behaviorDecision,
            ArmyPosition tacticalTarget,
            String reasonCode
    ) {
        return new ArmyTacticalDecision(ArmyTacticalIntent.REGROUP, behaviorDecision, tacticalTarget, reasonCode);
    }

    public static ArmyTacticalDecision hold(
            ArmyBehaviorDecision behaviorDecision,
            ArmyPosition tacticalTarget,
            String reasonCode
    ) {
        return new ArmyTacticalDecision(ArmyTacticalIntent.HOLD_POSITION, behaviorDecision, tacticalTarget, reasonCode);
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
