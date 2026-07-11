package galacticwars.clonewars.army;

import java.util.Objects;
import java.util.UUID;

public record ArmyGroupOrderAssignment(
        UUID recruitId,
        ArmyCommand command,
        ArmyPosition assignedPosition,
        FormationSlot formationSlot,
        String reasonCode
) {
    public ArmyGroupOrderAssignment {
        Objects.requireNonNull(recruitId, "recruitId");
        Objects.requireNonNull(command, "command");
        reasonCode = requireNonBlank(reasonCode);
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
