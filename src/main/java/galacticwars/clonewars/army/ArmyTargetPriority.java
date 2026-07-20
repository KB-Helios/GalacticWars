package galacticwars.clonewars.army;

/** Ordered preference applied when more than one valid hostile is visible. */
public enum ArmyTargetPriority {
    COMMAND_TARGET,
    OWNER_THREAT,
    NEAREST_HOSTILE,
    LOWEST_HEALTH
}
