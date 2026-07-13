package galacticwars.clonewars.progression;

import java.util.Objects;
import galacticwars.clonewars.data.LaunchContentDefinitions;

public final class ForceAbilityRuntimeService {
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
        return activate(progression, runtime, abilityId, gameTime, targetsPlayer, allowForcePvp,
                LaunchContentCatalog.data());
    }

    static ActivationDecision activate(
            ProgressionState progression,
            ForceRuntimeState runtime,
            String abilityId,
            long gameTime,
            boolean targetsPlayer,
            boolean allowForcePvp,
            LaunchContentDefinitions content
    ) {
        Objects.requireNonNull(progression, "progression");
        Objects.requireNonNull(runtime, "runtime");
        LaunchContentDefinitions.ForceAbilityDefinition ability =
                content.forceAbilities().get(abilityId);
        if (ability == null) {
            return ActivationDecision.rejected("unknown_force_ability", runtime);
        }
        return ActivationDecision.rejected("force_runtime_disabled", runtime);
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
}
