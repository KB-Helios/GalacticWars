package galacticwars.clonewars.army;

import java.util.Objects;
import java.util.UUID;

public record ArmyBehaviorContext(
        ArmyPosition selfPosition,
        ArmyPosition ownerPosition,
        UUID visibleThreatToOwner,
        boolean commandTargetAlive,
        int followRange
) {
    public ArmyBehaviorContext {
        Objects.requireNonNull(selfPosition, "selfPosition");
        Objects.requireNonNull(ownerPosition, "ownerPosition");
        if (followRange < 1) {
            throw new IllegalArgumentException("followRange must be at least 1");
        }
    }

    public static ArmyBehaviorContext of(
            ArmyPosition selfPosition,
            ArmyPosition ownerPosition,
            UUID visibleThreatToOwner,
            boolean commandTargetAlive,
            int followRange
    ) {
        return new ArmyBehaviorContext(selfPosition, ownerPosition, visibleThreatToOwner, commandTargetAlive, followRange);
    }
}
