package middleearth.lotr.warmod.recruitment;

import java.util.Locale;

public enum RecruitDuty {
    SOLDIER,
    WORKER,
    COMMANDER;

    public String id() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public static RecruitDuty byId(String value) {
        if (value == null || value.isBlank()) {
            return SOLDIER;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return SOLDIER;
        }
    }
}
