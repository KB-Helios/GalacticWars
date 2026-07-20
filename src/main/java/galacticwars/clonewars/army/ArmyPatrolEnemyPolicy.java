package galacticwars.clonewars.army;

/**
 * The intended response to an opportunistic hostile while travelling a patrol
 * route. Execution layers decide how to realize the policy in a loaded world.
 */
public enum ArmyPatrolEnemyPolicy {
    /** Preserve the current army behaviour: engage valid hostile targets. */
    ENGAGE_HOSTILES,
    /** Keep following the patrol route unless the player explicitly orders combat. */
    IGNORE_HOSTILES,
    /** Prefer regrouping/retreating over opportunistic combat. */
    RETREAT_FROM_HOSTILES
}
