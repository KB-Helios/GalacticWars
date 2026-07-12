package galacticwars.clonewars.army;

import java.util.Objects;
import java.util.Optional;

/** Computes the next persisted patrol order for loaded and virtual squads. */
public final class ArmyPatrolOrderPlanner {
    private static final int ARRIVAL_DISTANCE = 2;

    private ArmyPatrolOrderPlanner() {
    }

    public static ArmyGroupOrder nextOrder(ArmyGroupRecord group, ArmyPosition currentPosition) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(currentPosition, "currentPosition");
        if (group.order().type() != ArmyCommandType.PATROL_ROUTE || group.patrolRoute().size() < 2) {
            return group.order();
        }

        ArmyLocation activeWaypoint = group.order().targetPosition().orElse(null);
        int activeIndex = group.patrolRoute().indexOf(activeWaypoint);
        if (activeIndex < 0) {
            return orderFor(group, group.patrolRoute().getFirst());
        }

        ArmyPatrolRoute route = new ArmyPatrolRoute(
                group.patrolRoute().stream().map(ArmyLocation::blockPosition).toList(),
                ArmyPatrolMode.LOOP,
                ARRIVAL_DISTANCE,
                0);
        ArmyPatrolDecision decision = ArmyPatrolPlanner.advance(
                route,
                new ArmyPatrolState(activeIndex, 1, 0),
                currentPosition);
        int nextIndex = decision.nextState().waypointIndex();
        return nextIndex == activeIndex
                ? group.order()
                : orderFor(group, group.patrolRoute().get(nextIndex));
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
