package galacticwars.clonewars.army;

import java.util.Objects;
import java.util.UUID;

public record ArmyBehaviorContext(
        ArmyPosition snightsisterPosition,
        ArmyPosition ownerPosition,
        UUID visibleThreatToOwner,
        boolean commandTargetAlive,
        int followRange
) {
    public ArmyBehaviorContext {
        Objects.requireNonNull(snightsisterPosition, "snightsisterPosition");
        Objects.requireNonNull(ownerPosition, "ownerPosition");
        if (followRange < 1) {
            throw new IllegalArgumentException("followRange must be at least 1");
        }
    }

    public static ArmyBehaviorContext of(
            ArmyPosition snightsisterPosition,
            ArmyPosition ownerPosition,
            UUID visibleThreatToOwner,
            boolean commandTargetAlive,
            int followRange
    ) {
        return new ArmyBehaviorContext(snightsisterPosition, ownerPosition, visibleThreatToOwner, commandTargetAlive, followRange);
    }
}
