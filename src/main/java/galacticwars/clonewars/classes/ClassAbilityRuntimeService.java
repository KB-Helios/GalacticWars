package galacticwars.clonewars.classes;

import galacticwars.clonewars.ability.AbilityActivation;
import galacticwars.clonewars.ability.AbilityDefinition;
import galacticwars.clonewars.ability.AbilityId;
import galacticwars.clonewars.ability.AbilityKind;

import java.util.Objects;

public final class ClassAbilityRuntimeService {
    private ClassAbilityRuntimeService() {
    }

    public static ActivationDecision activate(
            UnitClassDefinition classDefinition,
            AbilityDefinition ability,
            ClassProgressState state,
            long gameTime,
            boolean targetPresent,
            double targetDistance,
            boolean targetsPlayer,
            boolean allowClassPvp
    ) {
        Objects.requireNonNull(classDefinition, "classDefinition");
        Objects.requireNonNull(ability, "ability");
        Objects.requireNonNull(state, "state");
        if (!state.classId().equals(classDefinition.id().toString())) {
            return ActivationDecision.rejected("class_not_assigned", state);
        }
        if (!classDefinition.abilityIds().contains(ability.id())) {
            return ActivationDecision.rejected("ability_not_in_class", state);
        }
        if (!ability.enabled() || ability.kind() == AbilityKind.FORCE) {
            return ActivationDecision.rejected("ability_disabled", state);
        }
        if (ability.activation() == AbilityActivation.PASSIVE) {
            return ActivationDecision.rejected("passive_ability", state);
        }
        if (ability.activation() == AbilityActivation.TARGET && !targetPresent) {
            return ActivationDecision.rejected("target_required", state);
        }
        if (targetPresent && (!Double.isFinite(targetDistance) || targetDistance < 0.0D
                || targetDistance > ability.range())) {
            return ActivationDecision.rejected("target_out_of_range", state);
        }
        if (targetsPlayer && !allowClassPvp) {
            return ActivationDecision.rejected("class_pvp_disabled", state);
        }
        long cooldownEnd = state.cooldownEnds().getOrDefault(ability.id().toString(), 0L);
        if (gameTime < cooldownEnd) {
            return ActivationDecision.rejected("ability_cooldown", state);
        }
        if (state.resource() < ability.resourceCost()) {
            return ActivationDecision.rejected("insufficient_class_resource", state);
        }
        long nextCooldown = Math.addExact(gameTime, ability.cooldownTicks());
        ClassProgressState updated = state.activate(
                ability.id(), ability.resourceCost(), nextCooldown, gameTime);
        return new ActivationDecision(true, "accepted", updated,
                ability.resourceCost(), ability.cooldownTicks());
    }

    public static boolean shouldEvaluateNpc(AbilityDefinition ability, int stableActorHash, long gameTime) {
        Objects.requireNonNull(ability, "ability");
        int interval = ability.aiEvaluationIntervalTicks();
        long phase = Math.floorMod(stableActorHash, interval);
        return Math.floorMod(gameTime + phase, interval) == 0L;
    }

    public record ActivationDecision(
            boolean accepted,
            String reason,
            ClassProgressState state,
            int resourceSpent,
            int cooldownTicks
    ) {
        public ActivationDecision {
            Objects.requireNonNull(reason, "reason");
            Objects.requireNonNull(state, "state");
        }

        public static ActivationDecision rejected(String reason, ClassProgressState state) {
            return new ActivationDecision(false, reason, state, 0, 0);
        }
    }
}
