package galacticwars.clonewars.recruitment;

import java.util.Locale;

public enum NpcServiceBranch {
    MILITARY,
    CIVILIAN;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static NpcServiceBranch byId(String id) {
        return valueOf(id.trim().toUpperCase(Locale.ROOT));
    }

    public static NpcServiceBranch migrate(RecruitDuty duty) {
        return duty == RecruitDuty.WORKER ? CIVILIAN : MILITARY;
    }
}
