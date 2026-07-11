package galacticwars.clonewars.kingdom;

import java.util.Locale;

public enum KingdomMemberRole {
    OWNER,
    OFFICER,
    BUILDER,
    QUARTERMASTER,
    MEMBER;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static KingdomMemberRole byId(String id) {
        return valueOf(id.trim().toUpperCase(Locale.ROOT));
    }
}
