package galacticwars.clonewars.entity.ai;

import galacticwars.clonewars.army.ArmyCommandType;
import galacticwars.clonewars.army.ArmyMarchPhase;
import galacticwars.clonewars.army.ArmyPosition;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Explicit SmartBrain projection of the authoritative moving formation. */
public record ArmyMarchMemory(
        UUID groupId,
        ArmyCommandType orderType,
        int memberSlot,
        ArmyPosition movingAnchor,
        ArmyMarchPhase phase,
        int cohesionPercent,
        @Nullable ArmyPosition targetPosition,
        @Nullable UUID targetEntityId
) {
    public ArmyMarchMemory {
        Objects.requireNonNull(groupId, "groupId");
        Objects.requireNonNull(orderType, "orderType");
        Objects.requireNonNull(movingAnchor, "movingAnchor");
        Objects.requireNonNull(phase, "phase");
        if (memberSlot < -1 || cohesionPercent < 0 || cohesionPercent > 100) {
            throw new IllegalArgumentException("invalid march memory bounds");
        }
    }
}
