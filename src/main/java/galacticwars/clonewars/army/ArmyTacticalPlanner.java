package galacticwars.clonewars.army;

import java.util.Objects;

public final class ArmyTacticalPlanner {
    private ArmyTacticalPlanner() {
    }

    public static ArmyTacticalDecision plan(
            ArmyBehaviorDecision behaviorDecision,
            RecruitVitals vitals,
            ArmyPosition fallbackPosition
    ) {
        Objects.requireNonNull(behaviorDecision, "behaviorDecision");
        Objects.requireNonNull(vitals, "vitals");
        Objects.requireNonNull(fallbackPosition, "fallbackPosition");

        if (vitals.isCriticalHealth()) {
            return ArmyTacticalDecision.retreat(behaviorDecision, fallbackPosition, "health_critical");
        }
        if (vitals.isBrokenMorale()) {
            return ArmyTacticalDecision.retreat(behaviorDecision, fallbackPosition, "morale_broken");
        }
        if (vitals.isLowMorale() && behaviorDecision.intent() == ArmyBehaviorIntent.ATTACK_TARGET) {
            return ArmyTacticalDecision.regroup(behaviorDecision, fallbackPosition, "morale_low");
        }
        if (vitals.isExhausted()) {
            ArmyPosition holdTarget = behaviorDecision.moveTarget() == null
                    ? fallbackPosition
                    : behaviorDecision.moveTarget();
            return ArmyTacticalDecision.hold(behaviorDecision, holdTarget, "hunger_exhausted");
        }
        if (vitals.isUpkeepOverdue()) {
            return ArmyTacticalDecision.regroup(behaviorDecision, fallbackPosition, "upkeep_overdue");
        }

        return ArmyTacticalDecision.execute(behaviorDecision);
    }
}
