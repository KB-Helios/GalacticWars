package galacticwars.clonewars.progression;

import galacticwars.clonewars.data.LaunchContentDefinitions;
import java.util.List;
import java.util.Objects;

/** Pure authoritative reducer for Force initiation, mastery nodes, loadouts, and respecs. */
public final class ForceProgressionService {
    public static final int RESPEC_CREDIT_COST = 25;

    private ForceProgressionService() {
    }

    public static ProgressionDecision initiate(
            ProgressionState progression,
            ForceRuntimeState state,
            String traditionId,
            LaunchContentDefinitions content
    ) {
        Objects.requireNonNull(progression, "progression");
        Objects.requireNonNull(state, "state");
        LaunchContentDefinitions.ForceTraditionDefinition tradition =
                content.forceTraditions().get(traditionId);
        if (tradition == null) return ProgressionDecision.rejected("unknown_force_tradition", state);
        if (state.initiated()) return ProgressionDecision.rejected("force_tradition_locked", state);
        if (!subjectPath(progression.factionId()).equals(tradition.factionId())) {
            return ProgressionDecision.rejected("wrong_force_faction", state);
        }
        if (!progression.hasSubjectPath(
                ProgressionEventType.QUEST_ADVANCED, tradition.initiationQuest())) {
            return ProgressionDecision.rejected("force_initiation_locked", state);
        }
        List<String> coreAbilities = tradition.coreNodes().stream()
                .map(content.forceNodes()::get)
                .filter(Objects::nonNull)
                .map(LaunchContentDefinitions.ForceNodeDefinition::abilityId)
                .filter(value -> !value.isBlank()).toList();
        return ProgressionDecision.accepted(state.initiate(
                tradition.id(), tradition.coreNodes(), coreAbilities));
    }

    public static ProgressionDecision learnNode(
            ForceRuntimeState state,
            String nodeId,
            LaunchContentDefinitions content
    ) {
        LaunchContentDefinitions.ForceNodeDefinition node = content.forceNodes().get(nodeId);
        if (node == null) return ProgressionDecision.rejected("unknown_force_node", state);
        if (!node.tradition().equals(state.traditionId())) {
            return ProgressionDecision.rejected("wrong_force_tradition", state);
        }
        if (node.branch().equals("core")) {
            return ProgressionDecision.rejected("force_core_node_fixed", state);
        }
        if (state.learned(node.id())) return ProgressionDecision.rejected("force_node_learned", state);
        if (state.rank() < node.tier() + 1) {
            return ProgressionDecision.rejected("force_rank_locked", state);
        }
        if (!state.learnedNodeIds().containsAll(node.prerequisites())) {
            return ProgressionDecision.rejected("force_prerequisite_missing", state);
        }
        if (state.unspentPoints() < node.pointCost()) {
            return ProgressionDecision.rejected("force_points_required", state);
        }
        return ProgressionDecision.accepted(state.learnNode(node.id(), node.abilityId()));
    }

    public static ProgressionDecision equip(
            ForceRuntimeState state,
            int slot,
            String abilityId,
            LaunchContentDefinitions content
    ) {
        LaunchContentDefinitions.ForceAbilityDefinition ability = content.forceAbilities().get(abilityId);
        if (ability == null) return ProgressionDecision.rejected("unknown_force_ability", state);
        if (!ability.path().equals(state.traditionId())) {
            return ProgressionDecision.rejected("wrong_force_tradition", state);
        }
        if (!state.learned(ability.nodeId())) {
            return ProgressionDecision.rejected("force_node_locked", state);
        }
        if (slot < 0 || slot >= ForceRuntimeState.MAX_EQUIPPED_ABILITIES) {
            return ProgressionDecision.rejected("invalid_force_slot", state);
        }
        return ProgressionDecision.accepted(state.equip(slot, abilityId));
    }

    public static ProgressionDecision respec(
            ForceRuntimeState state,
            LaunchContentDefinitions content
    ) {
        LaunchContentDefinitions.ForceTraditionDefinition tradition =
                content.forceTraditions().get(state.traditionId());
        if (tradition == null) return ProgressionDecision.rejected("force_not_initiated", state);
        List<String> coreAbilities = tradition.coreNodes().stream()
                .map(content.forceNodes()::get)
                .filter(Objects::nonNull)
                .map(LaunchContentDefinitions.ForceNodeDefinition::abilityId)
                .filter(value -> !value.isBlank()).toList();
        ForceRuntimeState updated = state.respec(tradition.coreNodes(), coreAbilities);
        return updated.equals(state)
                ? ProgressionDecision.rejected("force_respec_not_needed", state)
                : ProgressionDecision.accepted(updated);
    }

    public static ProgressionDecision gainMastery(
            ForceRuntimeState state,
            int amount,
            LaunchContentDefinitions content
    ) {
        LaunchContentDefinitions.ForceTraditionDefinition tradition =
                content.forceTraditions().get(state.traditionId());
        if (tradition == null) return ProgressionDecision.rejected("force_not_initiated", state);
        ForceRuntimeState updated = state.gainMastery(amount, tradition.rankThresholds());
        return updated.equals(state)
                ? ProgressionDecision.rejected("force_mastery_capped", state)
                : ProgressionDecision.accepted(updated);
    }

    public static ProgressionDecision awardCombatMastery(
            ForceRuntimeState state,
            String targetAndExecutorKey,
            long gameTime,
            LaunchContentDefinitions content
    ) {
        LaunchContentDefinitions.ForceTraditionDefinition tradition =
                content.forceTraditions().get(state.traditionId());
        if (tradition == null) return ProgressionDecision.rejected("force_not_initiated", state);
        ForceRuntimeState updated = state.awardCombatMastery(
                targetAndExecutorKey, gameTime, tradition.rankThresholds());
        return updated.equals(state)
                ? ProgressionDecision.rejected("force_mastery_throttled", state)
                : ProgressionDecision.accepted(updated);
    }

    public static String defaultTraditionForFaction(String factionId) {
        return switch (subjectPath(factionId)) {
            case "republic" -> "jedi";
            case "separatist" -> "sith";
            case "nightsister" -> "nightsister";
            default -> "";
        };
    }

    private static String subjectPath(String value) {
        int separator = value.indexOf(':');
        return separator < 0 ? value : value.substring(separator + 1);
    }

    public record ProgressionDecision(boolean accepted, String reason, ForceRuntimeState state) {
        public ProgressionDecision {
            Objects.requireNonNull(reason, "reason");
            Objects.requireNonNull(state, "state");
        }

        static ProgressionDecision accepted(ForceRuntimeState state) {
            return new ProgressionDecision(true, "accepted", state);
        }

        static ProgressionDecision rejected(String reason, ForceRuntimeState state) {
            return new ProgressionDecision(false, reason, state);
        }
    }
}
