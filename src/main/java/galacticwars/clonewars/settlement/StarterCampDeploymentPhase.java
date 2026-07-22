package galacticwars.clonewars.settlement;

import java.util.Locale;

public enum StarterCampDeploymentPhase {
    AWAITING_CONFIRMATION,
    BLOCKED,
    BUILDING,
    COMPLETE,
    PACKED_UP;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static StarterCampDeploymentPhase byId(String id) {
        if (id == null || id.isBlank()) {
            return AWAITING_CONFIRMATION;
        }
        try {
            return valueOf(id.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return AWAITING_CONFIRMATION;
        }
    }
}
