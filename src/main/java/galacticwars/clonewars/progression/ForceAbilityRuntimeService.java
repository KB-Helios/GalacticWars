package galacticwars.clonewars.progression;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ForceAbilityRuntimeService {
    private static final Map<String, Ability> ABILITIES = Map.of(
            "light_push", new Ability(20, 60),
            "light_pull", new Ability(25, 80),
            "light_leap", new Ability(15, 40),
            "dark_push", new Ability(20, 60),
            "dark_choke", new Ability(35, 120),
            "dark_dash", new Ability(15, 40));

    private ForceAbilityRuntimeService() {
    }

    public static ActivationDecision activate(
            ProgressionState progression,
            ForceRuntimeState runtime,
            String abilityId,
            long gameTime,
            boolean targetsPlayer,
            boolean allowForcePvp
    ) {
        Objects.requireNonNull(progression, "progression");
        Objects.requireNonNull(runtime, "runtime");
        Ability ability = ABILITIES.get(abilityId);
        if (ability == null) {
            return ActivationDecision.rejected("unknown_force_ability", runtime);
        }
        if (!progression.hasSubject(ProgressionEventType.FORCE_ABILITY_UNLOCKED, abilityId)) {
            return ActivationDecision.rejected("force_ability_locked", runtime);
        }
        if (targetsPlayer && !allowForcePvp) {
            return ActivationDecision.rejected("force_pvp_disabled", runtime);
        }
        long cooldownEnd = runtime.cooldownEnds().getOrDefault(abilityId, 0L);
        if (gameTime < cooldownEnd) {
            return ActivationDecision.rejected("force_ability_cooldown", runtime);
        }
        if (runtime.energy() < ability.energyCost()) {
            return ActivationDecision.rejected("insufficient_force_energy", runtime);
        }
        HashMap<String, Long> cooldowns = new HashMap<>(runtime.cooldownEnds());
        cooldowns.put(abilityId, Math.addExact(gameTime, ability.cooldownTicks()));
        ForceRuntimeState updated = new ForceRuntimeState(
                runtime.energy() - ability.energyCost(), cooldowns);
        return new ActivationDecision(true, "accepted", updated,
                ability.energyCost(), ability.cooldownTicks());
    }

    public record ActivationDecision(
            boolean accepted,
            String reason,
            ForceRuntimeState state,
            int energySpent,
            int cooldownTicks
    ) {
        public ActivationDecision {
            Objects.requireNonNull(reason, "reason");
            Objects.requireNonNull(state, "state");
        }

        static ActivationDecision rejected(String reason, ForceRuntimeState state) {
            return new ActivationDecision(false, reason, state, 0, 0);
        }
    }

    private record Ability(int energyCost, int cooldownTicks) {
    }
}
