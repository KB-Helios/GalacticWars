package galacticwars.clonewars.progression;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;
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
        return activate(progression, runtime, UUID.randomUUID(), abilityId, gameTime, targetsPlayer, allowForcePvp,
                LaunchContentCatalog.data());
    }

    public static ActivationDecision activate(
            ProgressionState progression,
            ForceRuntimeState runtime,
            UUID activationId,
            String abilityId,
            long gameTime,
            boolean targetsPlayer,
            boolean allowForcePvp
    ) {
        return activate(progression, runtime, activationId, abilityId, gameTime,
                targetsPlayer, allowForcePvp, LaunchContentCatalog.data());
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
        return activate(progression, runtime, UUID.randomUUID(), abilityId, gameTime,
                targetsPlayer, allowForcePvp, content);
    }

    static ActivationDecision activate(
            ProgressionState progression,
            ForceRuntimeState runtime,
            UUID activationId,
            String abilityId,
            long gameTime,
            boolean targetsPlayer,
            boolean allowForcePvp,
            LaunchContentDefinitions content
    ) {
        Objects.requireNonNull(progression, "progression");
        Objects.requireNonNull(runtime, "runtime");
        Objects.requireNonNull(activationId, "activationId");
        if (runtime.processedActivationIds().contains(activationId)) {
            return new ActivationDecision(true, "duplicate_activation", runtime, 0, 0);
        }
        LaunchContentDefinitions.ForceAbilityDefinition ability =
                content.forceAbilities().get(abilityId);
        if (ability == null) {
            return ActivationDecision.rejected("unknown_force_ability", runtime);
        }
        if (!ability.enabled()) {
            return ActivationDecision.rejected("force_ability_disabled", runtime);
        }
        String allowedPath = factionPath(progression.factionId());
        String selectedPath = runtime.path().isEmpty() ? allowedPath : runtime.path();
        if (selectedPath.isEmpty() || !selectedPath.equals(ability.path())) {
            return ActivationDecision.rejected("force_path_unavailable", runtime);
        }
        if (!progression.hasSubject(ProgressionEventType.QUEST_ADVANCED, ability.requiredQuest())) {
            return ActivationDecision.rejected("force_quest_locked", runtime);
        }
        if (!progression.unlocks().contains(ability.activeUnlock())
                && !progression.hasSubject(ProgressionEventType.QUEST_ADVANCED, ability.activeUnlock())) {
            return ActivationDecision.rejected("force_unlock_missing", runtime);
        }
        if (targetsPlayer && !allowForcePvp) {
            return ActivationDecision.rejected("force_pvp_disabled", runtime);
        }
        long cooldownEnd = runtime.cooldownEnds().getOrDefault(abilityId, 0L);
        if (gameTime < cooldownEnd) {
            return ActivationDecision.rejected("force_cooldown", runtime);
        }
        if (runtime.energy() < ability.energy()) {
            return ActivationDecision.rejected("insufficient_force_energy", runtime);
        }
        HashMap<String, Long> cooldowns = new HashMap<>(runtime.cooldownEnds());
        cooldowns.put(abilityId, Math.addExact(gameTime, ability.cooldownTicks()));
        HashSet<UUID> processed = new HashSet<>(runtime.processedActivationIds());
        processed.add(activationId);
        ForceRuntimeState updated = new ForceRuntimeState(selectedPath,
                runtime.energy() - ability.energy(), cooldowns, processed);
        return new ActivationDecision(true, "accepted", updated,
                ability.energy(), ability.cooldownTicks());
    }

    private static String factionPath(String factionId) {
        String path = factionId.contains(":")
                ? factionId.substring(factionId.indexOf(':') + 1) : factionId;
        return switch (path) {
            case "republic" -> "light";
            case "nightsister" -> "dark";
            default -> "";
        };
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
