package galacticwars.clonewars.recruitment;

import java.util.Locale;

public enum NpcServiceBranch {
    MILITARY,
    CIVILIAN;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static NpcServiceBranch byId(String id) {
        if (id == null || id.isBlank()) {
            return CIVILIAN;
        }
        try {
            return valueOf(id.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return CIVILIAN;
        }
    }

    public static NpcServiceBranch migrate(RecruitDuty duty) {
        return duty == RecruitDuty.WORKER ? CIVILIAN : MILITARY;
    }
}
