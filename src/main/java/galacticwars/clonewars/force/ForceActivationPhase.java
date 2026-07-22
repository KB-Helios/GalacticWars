package galacticwars.clonewars.force;

/** Network activation edge. Charge and channel duration are always measured by the server. */
public enum ForceActivationPhase {
    PRESS,
    RELEASE,
    CANCEL;

    public static ForceActivationPhase byNetworkId(int id) {
        return id >= 0 && id < values().length ? values()[id] : CANCEL;
    }
}
