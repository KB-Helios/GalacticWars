package galacticwars.clonewars.kingdom;

import java.util.Locale;

public enum KingdomRelation {
    NEUTRAL,
    ALLY,
    ENEMY;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static KingdomRelation byId(String id) {
        return valueOf(id.trim().toUpperCase(Locale.ROOT));
    }
}
