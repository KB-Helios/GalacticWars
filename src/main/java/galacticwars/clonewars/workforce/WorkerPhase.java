package galacticwars.clonewars.workforce;

import java.util.Locale;

public enum WorkerPhase {
    ACQUIRE_ORDER,
    FIND_TARGET,
    NAVIGATE_SOURCE,
    INTERACT,
    COLLECT,
    NAVIGATE_STORAGE,
    DEPOSIT,
    COOLDOWN,
    PAUSED,
    BLOCKED;

    public String id() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public static WorkerPhase byId(String value) {
        if (value == null || value.isBlank()) {
            return ACQUIRE_ORDER;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return BLOCKED;
        }
    }
}
