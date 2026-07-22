package galacticwars.clonewars.progression

import galacticwars.clonewars.data.LaunchContentDefinitions
import java.util.UUID
import kotlin.jvm.JvmRecord

/** Dependency-light authoritative reducer for Force ability activations. */
class ForceAbilityRuntimeService private constructor() {
    companion object {
        @JvmStatic
        fun activate(
            progression: ProgressionState,
            runtime: ForceRuntimeState,
            abilityId: String,
            gameTime: Long,
            targetsPlayer: Boolean,
            allowForcePvp: Boolean,
        ): ActivationDecision = activate(
            progression,
            runtime,
            UUID.randomUUID(),
            abilityId,
            gameTime,
            targetsPlayer,
            allowForcePvp,
            LaunchContentCatalog.data(),
        )

        @JvmStatic
        fun activate(
            progression: ProgressionState,
            runtime: ForceRuntimeState,
            activationId: UUID,
            abilityId: String,
            gameTime: Long,
            targetsPlayer: Boolean,
            allowForcePvp: Boolean,
        ): ActivationDecision = activate(
            progression,
            runtime,
            activationId,
            abilityId,
            gameTime,
            targetsPlayer,
            allowForcePvp,
            LaunchContentCatalog.data(),
        )

        @JvmStatic
        fun activate(
            progression: ProgressionState,
            runtime: ForceRuntimeState,
            abilityId: String,
            gameTime: Long,
            targetsPlayer: Boolean,
            allowForcePvp: Boolean,
            content: LaunchContentDefinitions,
        ): ActivationDecision = activate(
            progression,
            runtime,
            UUID.randomUUID(),
            abilityId,
            gameTime,
            targetsPlayer,
            allowForcePvp,
            content,
        )

        @JvmStatic
        fun activate(
            progression: ProgressionState,
            runtime: ForceRuntimeState,
            activationId: UUID,
            abilityId: String,
            gameTime: Long,
            targetsPlayer: Boolean,
            allowForcePvp: Boolean,
            content: LaunchContentDefinitions,
        ): ActivationDecision {
            if (activationId in runtime.processedActivationIds) {
                return ActivationDecision(true, "duplicate_activation", runtime, 0, 0)
            }

            val ability = content.forceAbilities()[abilityId]
                ?: return rejected("unknown_force_ability", runtime)
            if (!ability.enabled()) {
                return rejected("force_ability_disabled", runtime)
            }

            val abilityTradition = normalizeAbilityTradition(ability.path(), progression.factionId)
            val legacyDefinition = content.forceTraditions().isEmpty() || ability.nodeId().isEmpty()
            if (legacyDefinition) {
                val allowedTradition = factionTradition(progression.factionId)
                val selectedTradition = runtime.traditionId.ifEmpty { allowedTradition }
                if (selectedTradition.isEmpty() || selectedTradition != abilityTradition) {
                    return rejected("force_path_unavailable", runtime)
                }
                if (!progression.hasSubject(ProgressionEventType.QUEST_ADVANCED, ability.requiredQuest())) {
                    return rejected("force_quest_locked", runtime)
                }
                if (ability.activeUnlock() !in progression.unlocks &&
                    !progression.hasSubject(ProgressionEventType.QUEST_ADVANCED, ability.activeUnlock())
                ) {
                    return rejected("force_unlock_missing", runtime)
                }
            } else {
                if (!runtime.initiated() || runtime.traditionId != abilityTradition) {
                    return rejected("force_tradition_unavailable", runtime)
                }
                if (!runtime.learned(ability.nodeId())) {
                    return rejected("force_node_locked", runtime)
                }
                if (runtime.rank < ability.requiredRank()) {
                    return rejected("force_rank_locked", runtime)
                }
            }
            if (targetsPlayer && !allowForcePvp) {
                return rejected("force_pvp_disabled", runtime)
            }

            val cooldownEnd = runtime.cooldownEnds[abilityId] ?: 0L
            if (gameTime < cooldownEnd) {
                return rejected("force_cooldown", runtime)
            }
            if (runtime.energy < ability.energy()) {
                return rejected("insufficient_force_energy", runtime)
            }

            val updated = runtime.spendAndCooldown(
                ability.energy(),
                abilityId,
                Math.addExact(gameTime, ability.cooldownTicks().toLong()),
                activationId,
            )
            return ActivationDecision(
                true,
                "accepted",
                updated,
                ability.energy(),
                ability.cooldownTicks(),
            )
        }

        private fun factionTradition(factionId: String): String = when (factionId.substringAfter(':', factionId)) {
            "republic" -> "jedi"
            "separatist" -> "sith"
            "nightsister" -> "nightsister"
            else -> ""
        }

        private fun normalizeAbilityTradition(path: String, factionId: String): String = when (path) {
            "light" -> "jedi"
            "dark" -> if (factionId.substringAfter(':', factionId) == "separatist") "sith" else "nightsister"
            else -> path
        }

        private fun rejected(reason: String, state: ForceRuntimeState): ActivationDecision =
            ActivationDecision(false, reason, state, 0, 0)
    }

    @JvmRecord
    data class ActivationDecision(
        val accepted: Boolean,
        val reason: String,
        val state: ForceRuntimeState,
        val energySpent: Int,
        val cooldownTicks: Int,
    )
}
