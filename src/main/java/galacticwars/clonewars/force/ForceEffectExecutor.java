package galacticwars.clonewars.force;

/** Shared executor contract used by player and NPC Force users. */
@FunctionalInterface
public interface ForceEffectExecutor {
    ForceEffectReport execute(ForceEffectContext context);
}
