package galacticwars.clonewars.force;

import java.util.Set;

public final class ForceEffectExecutorCoverageTest {
    public static void main(String[] args) {
        if (ForceEffectExecutorCatalog.supports(null)) {
            throw new AssertionError("Null Force effect ids must fail closed");
        }
        if (!ForceEffectExecutorCatalog.registeredIds().equals(
                Set.of(
                        "push", "pull", "leap", "dash", "guard", "repulse",
                        "guardian_stand", "stasis", "mind_trick", "healing_meditation",
                        "balance_wave", "saber_frenzy", "choke", "throw", "assault",
                        "lightning", "chain_lightning", "crush", "maelstrom", "blade_dance",
                        "spirit_ward", "shadow_hunt", "hex", "spirit_snare", "ichor_bolt",
                        "life_weave", "ritual_storm"))) {
            throw new AssertionError("Every launch Force effect must have one server executor");
        }
        System.out.println("ForceEffectExecutorCoverageTest passed");
    }
}
