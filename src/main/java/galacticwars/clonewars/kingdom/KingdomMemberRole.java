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
        if (id != null) {
            String normalized = id.trim().toLowerCase(Locale.ROOT);
            for (KingdomMemberRole role : values()) {
                if (role.id().equals(normalized)) {
                    return role;
                }
            }
        }
        return MEMBER;
    }
}
