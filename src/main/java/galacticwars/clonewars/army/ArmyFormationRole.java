package galacticwars.clonewars.army;

/** Tactical position family used without changing a unit's gameplay class. */
public enum ArmyFormationRole {
    LEADER(0.0D),
    HEAVY(0.15D),
    FRONTLINE(0.30D),
    SUPPORT(0.65D),
    RANGED(1.0D);

    private final double preferredDepth;

    ArmyFormationRole(double preferredDepth) {
        this.preferredDepth = preferredDepth;
    }

    public double preferredDepth() {
        return preferredDepth;
    }

    public static ArmyFormationRole fromUnitRole(ArmyUnitRole role) {
        return switch (role) {
            case ARCHER -> RANGED;
            case BRUTE -> HEAVY;
            case CAVALRY -> SUPPORT;
            case INFANTRY -> FRONTLINE;
        };
    }
}
