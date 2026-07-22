package galacticwars.clonewars.entity;

import java.util.Locale;

/** Server-selected gameplay intent consumed by the GeckoLib controller. */
public enum RecruitVisualAction {
    IDLE,
    ACKNOWLEDGE_ORDER,
    FORM_UP,
    MARCH,
    HALT,
    BUILD,
    RANGED_COMBAT,
    MELEE_COMBAT,
    RETREAT,
    INJURED,
    LOW_MORALE;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static RecruitVisualAction byId(String id) {
        if (id == null || id.isBlank()) {
            return IDLE;
        }
        try {
            return valueOf(id.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return IDLE;
        }
    }
}
