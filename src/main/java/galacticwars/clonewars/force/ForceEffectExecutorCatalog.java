package galacticwars.clonewars.force;

import java.util.Set;

/** Dependency-light registry contract for every server-side Force effect executor. */
public final class ForceEffectExecutorCatalog {
    private static final Set<String> EFFECTS = Set.of("push", "pull", "leap", "dash", "choke");

    private ForceEffectExecutorCatalog() {
    }

    public static boolean supports(String effectId) {
        return EFFECTS.contains(effectId);
    }

    public static Set<String> registeredIds() {
        return EFFECTS;
    }
}
