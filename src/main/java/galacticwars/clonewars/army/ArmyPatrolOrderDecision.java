package galacticwars.clonewars.army;

import java.util.Objects;

/**
 * The next persisted order and patrol progress computed from one pure patrol
 * tick. Callers persist both values atomically when they own saved-data writes.
 */
public record ArmyPatrolOrderDecision(
        ArmyGroupOrder nextOrder,
        ArmyPatrolPlan nextPlan,
        ArmyPatrolDecision patrolDecision
) {
    public ArmyPatrolOrderDecision {
        Objects.requireNonNull(nextOrder, "nextOrder");
        Objects.requireNonNull(nextPlan, "nextPlan");
        Objects.requireNonNull(patrolDecision, "patrolDecision");
    }
}
