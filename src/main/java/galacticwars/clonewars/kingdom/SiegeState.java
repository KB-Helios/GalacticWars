package galacticwars.clonewars.kingdom;

import java.util.Locale;

public enum SiegeState {
    ACTIVE,
    ATTACKERS_REPELLED,
    CAPTURED,
    CANCELLED;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static SiegeState byId(String id) {
        return valueOf(id.trim().toUpperCase(Locale.ROOT));
    }

    public boolean terminal() {
        return this != ACTIVE;
    }
}
