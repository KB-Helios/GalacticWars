package galacticwars.clonewars.kingdom;

import java.util.Locale;

public enum BuildProjectState {
    ACTIVE,
    BLOCKED,
    COMPLETED,
    CANCELLED;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static BuildProjectState byId(String id) {
        return valueOf(id.trim().toUpperCase(Locale.ROOT));
    }
}
