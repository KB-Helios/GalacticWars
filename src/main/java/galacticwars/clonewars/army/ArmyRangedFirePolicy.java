package galacticwars.clonewars.army;

/** Squad-level permission policy for ranged execution layers. */
public enum ArmyRangedFirePolicy {
    HOLD_FIRE,
    RETURN_FIRE,
    FREE_FIRE,
    FOCUS_COMMAND_TARGET;

    /**
     * Resolves the policy without exposing world or entity access to the
     * persistence model. {@code returnFireTarget} means the target has
     * recently attacked the recruit or the subject protected by its order;
     * {@code commandTarget} means it is the explicit attack-order target.
     */
    public boolean allowsRangedFire(boolean returnFireTarget, boolean commandTarget) {
        return switch (this) {
            case HOLD_FIRE -> false;
            case RETURN_FIRE -> returnFireTarget;
            case FREE_FIRE -> true;
            case FOCUS_COMMAND_TARGET -> commandTarget;
        };
    }
}
