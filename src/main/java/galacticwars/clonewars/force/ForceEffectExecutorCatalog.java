package galacticwars.clonewars.force;

import java.util.Set;

/** Dependency-light registry contract for every server-side Force effect executor. */
public final class ForceEffectExecutorCatalog {
    private static final Set<String> EFFECTS = Set.of(
            "push", "pull", "leap", "dash", "guard", "repulse", "guardian_stand",
            "stasis", "mind_trick", "healing_meditation", "balance_wave",
            "saber_frenzy", "choke", "throw", "assault", "lightning",
            "chain_lightning", "crush", "maelstrom", "blade_dance", "spirit_ward",
            "shadow_hunt", "hex", "spirit_snare", "ichor_bolt", "life_weave",
            "ritual_storm");

    private ForceEffectExecutorCatalog() {
    }

    public static boolean supports(String effectId) {
        return effectId != null && EFFECTS.contains(effectId);
    }

    public static Set<String> registeredIds() {
        return EFFECTS;
    }
}
