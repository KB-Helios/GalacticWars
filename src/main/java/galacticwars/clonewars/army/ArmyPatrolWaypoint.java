package galacticwars.clonewars.army;

import java.util.Objects;

/** A persisted patrol location together with the dwell time after arrival. */
public record ArmyPatrolWaypoint(ArmyLocation location, int waitTicks) {
    public ArmyPatrolWaypoint {
        Objects.requireNonNull(location, "location");
        if (waitTicks < 0) {
            throw new IllegalArgumentException("waitTicks cannot be negative");
        }
    }

    public static ArmyPatrolWaypoint immediate(ArmyLocation location) {
        return new ArmyPatrolWaypoint(location, 0);
    }
}
