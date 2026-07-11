package galacticwars.clonewars.kingdom;

import java.util.Locale;

public enum WorkOrderState {
    QUEUED,
    CLAIMED,
    IN_PROGRESS,
    BLOCKED,
    COMPLETED,
    CANCELLED;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static WorkOrderState byId(String id) {
        return valueOf(id.trim().toUpperCase(Locale.ROOT));
    }

    public boolean terminal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
