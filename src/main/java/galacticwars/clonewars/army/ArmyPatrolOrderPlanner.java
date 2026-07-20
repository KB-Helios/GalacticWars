package galacticwars.clonewars.army;

import java.util.Objects;
import java.util.Optional;

/** Computes the next persisted patrol order for loaded and virtual squads. */
public final class ArmyPatrolOrderPlanner {
    private ArmyPatrolOrderPlanner() {
    }

    /**
     * Calculates both values that must be persisted together. A caller that
     * owns saved data should apply {@code nextPlan} and {@code nextOrder}
     * atomically. Legacy groups are derived without materializing a plan.
     */
    public static Optional<ArmyPatrolOrderDecision> advance(
            ArmyGroupRecord group,
            ArmyPosition currentPosition
    ) {
        return advance(group, currentPosition, 1);
    }

    /**
     * Advances persisted patrol progress after {@code elapsedTicks}. The
     * standard loaded path uses one tick, while virtual groups supply the
     * elapsed server ticks since their prior simulation.
     */
    public static Optional<ArmyPatrolOrderDecision> advance(
            ArmyGroupRecord group,
            ArmyPosition currentPosition,
            int elapsedTicks
    ) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(currentPosition, "currentPosition");
        if (elapsedTicks < 0) {
            throw new IllegalArgumentException("elapsedTicks cannot be negative");
        }
        if (group.order().type() != ArmyCommandType.PATROL_ROUTE) {
            return Optional.empty();
        }

        Optional<ArmyPatrolPlan> effectivePlan = group.patrolPlan().isPresent()
                ? group.patrolPlan()
                : legacyPlanForCurrentOrder(group);
        if (effectivePlan.isEmpty()) {
            return Optional.empty();
        }

        ArmyPatrolPlan plan = effectivePlan.orElseThrow();
        ArmyPatrolDecision patrolDecision = ArmyPatrolPlanner.advance(
                plan.toRuntimeRoute(), plan.state(), currentPosition, elapsedTicks);
        ArmyPatrolPlan nextPlan = plan.withState(patrolDecision.nextState());
        ArmyGroupOrder nextOrder = patrolDecision.shouldMove()
                ? orderFor(group, nextPlan.waypoints().get(nextPlan.state().waypointIndex()).location())
                : group.order();
        return Optional.of(new ArmyPatrolOrderDecision(nextOrder, nextPlan, patrolDecision));
    }

    /** Legacy convenience API. It deliberately does not materialize new plan state. */
    public static ArmyGroupOrder nextOrder(ArmyGroupRecord group, ArmyPosition currentPosition) {
        return advance(group, currentPosition)
                .map(ArmyPatrolOrderDecision::nextOrder)
                .orElseGet(group::order);
    }

    private static Optional<ArmyPatrolPlan> legacyPlanForCurrentOrder(ArmyGroupRecord group) {
        Optional<ArmyPatrolPlan> derived = group.effectivePatrolPlan();
        if (derived.isEmpty()) {
            return Optional.empty();
        }
        ArmyPatrolPlan plan = derived.orElseThrow();
        ArmyLocation activeWaypoint = group.order().targetPosition().orElse(null);
        int activeIndex = plan.locations().indexOf(activeWaypoint);
        if (activeIndex < 0) {
            activeIndex = 0;
        }
        return Optional.of(plan.withState(new ArmyPatrolState(activeIndex, 1, 0)));
    }

    private static ArmyGroupOrder orderFor(ArmyGroupRecord group, ArmyLocation waypoint) {
        return new ArmyGroupOrder(
                ArmyCommandType.PATROL_ROUTE,
                Optional.of(waypoint),
                Optional.empty(),
                group.order().formation(),
                group.order().spacing());
    }
}
