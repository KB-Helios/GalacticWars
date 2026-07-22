package galacticwars.clonewars.progression

import java.util.Collections
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.Objects
import java.util.UUID

/**
 * Immutable authoritative Force career state. Legacy light/dark callers are retained through
 * [path], but new gameplay always uses the explicit Jedi, Sith, or Nightsister [traditionId].
 */
class ForceRuntimeState(
    traditionId: String?,
    rank: Int,
    masteryExperience: Int,
    unspentPoints: Int,
    learnedNodeIds: Set<String>,
    equippedAbilityIds: List<String>,
    energy: Int,
    cooldownEnds: Map<String, Long>,
    processedActivationIds: Set<UUID>,
    dailyCombatExperience: Int,
    combatExperienceDay: Long,
    recentMasteryKeys: Map<String, Long>,
) {
    val traditionId: String = normalizeTradition(traditionId)
    val rank: Int = rank
    val masteryExperience: Int = masteryExperience
    val unspentPoints: Int = unspentPoints
    val learnedNodeIds: Set<String> = immutableIdentifiers(learnedNodeIds, MAX_LEARNED_NODES)
    val equippedAbilityIds: List<String> = immutableLoadout(equippedAbilityIds)
    val energy: Int = energy
    val cooldownEnds: Map<String, Long> = immutableCooldowns(cooldownEnds)
    val processedActivationIds: Set<UUID> = boundedActivationIds(processedActivationIds)
    val dailyCombatExperience: Int = dailyCombatExperience
    val combatExperienceDay: Long = combatExperienceDay
    val recentMasteryKeys: Map<String, Long> = immutableRecentMastery(recentMasteryKeys)

    /** Compatibility path for existing attachment and test consumers. */
    val path: String
        get() = when (traditionId) {
            "jedi" -> "light"
            "sith", "nightsister" -> "dark"
            else -> ""
        }

    init {
        require(traditionId.isNullOrEmpty() || normalizeTradition(traditionId) in TRADITIONS) {
            "Unknown Force tradition $traditionId"
        }
        require(rank in 0..MAX_RANK) { "Force rank must be between 0 and $MAX_RANK" }
        require(masteryExperience in 0..MAX_MASTERY_EXPERIENCE) {
            "Force mastery experience is outside the supported range"
        }
        require(unspentPoints in 0..MAX_SKILL_POINTS) { "Invalid Force skill points" }
        require(energy in 0..MAX_ENERGY) { "Force energy must be between 0 and $MAX_ENERGY" }
        require(dailyCombatExperience in 0..MAX_DAILY_COMBAT_EXPERIENCE) {
            "Invalid daily Force combat experience"
        }
        require(combatExperienceDay >= 0L) { "Force combat experience day cannot be negative" }
        require((this.traditionId.isEmpty() && rank == 0) || (this.traditionId.isNotEmpty() && rank >= 1)) {
            "Force rank and tradition must describe the same initiation state"
        }
        require(this.equippedAbilityIds.filter(String::isNotBlank).distinct().size ==
            this.equippedAbilityIds.count(String::isNotBlank)
        ) {
            "Force loadout cannot contain duplicate abilities"
        }
    }

    /** Legacy v1 constructor used by codecs and dependency-light tests. */
    constructor(
        path: String?,
        energy: Int,
        cooldownEnds: Map<String, Long>,
        processedActivationIds: Set<UUID>,
    ) : this(
        normalizeTradition(path),
        if (path.isNullOrEmpty()) 0 else 1,
        0,
        0,
        legacyCoreNodes(path),
        legacyLoadout(path),
        energy,
        legacyCooldowns(path, cooldownEnds),
        processedActivationIds,
        0,
        0L,
        emptyMap(),
    )

    constructor(energy: Int, cooldownEnds: Map<String, Long>) :
        this("", energy, cooldownEnds, emptySet())

    fun traditionId(): String = traditionId
    fun path(): String = path
    fun rank(): Int = rank
    fun masteryExperience(): Int = masteryExperience
    fun unspentPoints(): Int = unspentPoints
    fun learnedNodeIds(): Set<String> = learnedNodeIds
    fun equippedAbilityIds(): List<String> = equippedAbilityIds
    fun energy(): Int = energy
    fun cooldownEnds(): Map<String, Long> = cooldownEnds
    fun processedActivationIds(): Set<UUID> = processedActivationIds
    fun dailyCombatExperience(): Int = dailyCombatExperience
    fun combatExperienceDay(): Long = combatExperienceDay
    fun recentMasteryKeys(): Map<String, Long> = recentMasteryKeys
    fun initiated(): Boolean = traditionId.isNotEmpty()
    fun learned(nodeId: String): Boolean = nodeId in learnedNodeIds

    fun withPath(selectedPath: String?): ForceRuntimeState = withTradition(normalizeTradition(selectedPath))

    fun withTradition(selectedTradition: String?): ForceRuntimeState {
        val normalized = normalizeTradition(selectedTradition)
        return ForceRuntimeState(
            normalized,
            if (normalized.isEmpty()) 0 else maxOf(1, rank),
            masteryExperience,
            unspentPoints,
            learnedNodeIds,
            equippedAbilityIds,
            energy,
            cooldownEnds,
            processedActivationIds,
            dailyCombatExperience,
            combatExperienceDay,
            recentMasteryKeys,
        )
    }

    fun regenerate(amount: Int): ForceRuntimeState {
        require(amount >= 0) { "regeneration amount cannot be negative" }
        val regeneratedEnergy = minOf(MAX_ENERGY.toLong(), energy.toLong() + amount).toInt()
        return copy(energy = regeneratedEnergy)
    }

    fun spendAndCooldown(
        amount: Int,
        abilityId: String,
        cooldownEnd: Long,
        activationId: UUID,
    ): ForceRuntimeState {
        require(amount in 0..energy) { "Force energy spend is outside the available amount" }
        val cooldowns = LinkedHashMap(cooldownEnds)
        cooldowns[abilityId] = cooldownEnd
        val processed = LinkedHashSet(processedActivationIds)
        processed.add(activationId)
        return copy(
            energy = energy - amount,
            cooldownEnds = cooldowns,
            processedActivationIds = processed,
        )
    }

    fun spendSustain(amount: Int): ForceRuntimeState {
        require(amount in 0..energy) { "Force sustain spend is outside the available amount" }
        return copy(energy = energy - amount)
    }

    fun initiate(
        tradition: String,
        coreNodes: Collection<String>,
        coreAbilities: Collection<String>,
    ): ForceRuntimeState {
        require(traditionId.isEmpty()) { "Force tradition is already selected" }
        return ForceRuntimeState(
            tradition,
            1,
            0,
            0,
            LinkedHashSet(coreNodes),
            coreAbilities.take(MAX_EQUIPPED_ABILITIES),
            MAX_ENERGY,
            emptyMap(),
            processedActivationIds,
            0,
            0L,
            emptyMap(),
        )
    }

    fun gainMastery(amount: Int, rankThresholds: List<Int>): ForceRuntimeState {
        require(amount >= 0) { "Force mastery award cannot be negative" }
        require(rankThresholds.size == MAX_RANK) { "Force mastery rank table must contain $MAX_RANK entries" }
        val updatedExperience = minOf(MAX_MASTERY_EXPERIENCE, masteryExperience + amount)
        var updatedRank = 1
        rankThresholds.forEachIndexed { index, threshold ->
            if (updatedExperience >= threshold) updatedRank = index + 1
        }
        val gainedPoints = maxOf(0, updatedRank - rank)
        return copy(
            rank = updatedRank,
            masteryExperience = updatedExperience,
            unspentPoints = minOf(MAX_SKILL_POINTS, unspentPoints + gainedPoints),
        )
    }

    fun awardCombatMastery(
        masteryKey: String,
        gameTime: Long,
        rankThresholds: List<Int>,
    ): ForceRuntimeState {
        require(masteryKey.isNotBlank()) { "Force mastery key cannot be blank" }
        val day = maxOf(0L, gameTime / TICKS_PER_DAY)
        val currentDaily = if (day == combatExperienceDay) dailyCombatExperience else 0
        if (currentDaily >= MAX_DAILY_COMBAT_EXPERIENCE
            || gameTime < recentMasteryKeys.getOrDefault(masteryKey, Long.MIN_VALUE)
        ) {
            return this
        }
        val recent = LinkedHashMap(recentMasteryKeys)
        recent[masteryKey] = Math.addExact(gameTime, MASTERY_KEY_COOLDOWN_TICKS)
        val gained = gainMastery(1, rankThresholds)
        return gained.copy(
            dailyCombatExperience = currentDaily + 1,
            combatExperienceDay = day,
            recentMasteryKeys = recent,
        )
    }

    fun learnNode(nodeId: String, abilityId: String?): ForceRuntimeState {
        require(unspentPoints > 0) { "No Force skill points are available" }
        require(nodeId !in learnedNodeIds) { "Force node is already learned" }
        val learned = LinkedHashSet(learnedNodeIds)
        learned.add(nodeId)
        val loadout = equippedAbilityIds.toMutableList()
        if (!abilityId.isNullOrBlank()) {
            val emptySlot = loadout.indexOfFirst(String::isBlank)
            if (emptySlot >= 0) loadout[emptySlot] = abilityId
            else if (loadout.size < MAX_EQUIPPED_ABILITIES) loadout.add(abilityId)
        }
        return copy(
            unspentPoints = unspentPoints - 1,
            learnedNodeIds = learned,
            equippedAbilityIds = loadout,
        )
    }

    fun equip(slot: Int, abilityId: String): ForceRuntimeState {
        require(slot in 0 until MAX_EQUIPPED_ABILITIES) { "Invalid Force ability slot" }
        require(abilityId.isNotBlank()) { "Force ability id cannot be blank" }
        val updated = equippedAbilityIds.toMutableList()
        while (updated.size <= slot) updated.add("")
        val previousSlot = updated.indexOf(abilityId)
        if (previousSlot >= 0 && previousSlot != slot) {
            updated[previousSlot] = updated[slot]
        }
        updated[slot] = abilityId
        return copy(equippedAbilityIds = updated)
    }

    fun respec(coreNodes: Collection<String>, coreAbilities: Collection<String>): ForceRuntimeState = copy(
        unspentPoints = maxOf(0, rank - 1),
        learnedNodeIds = LinkedHashSet(coreNodes),
        equippedAbilityIds = coreAbilities.take(MAX_EQUIPPED_ABILITIES),
        cooldownEnds = emptyMap(),
    )

    private fun copy(
        traditionId: String = this.traditionId,
        rank: Int = this.rank,
        masteryExperience: Int = this.masteryExperience,
        unspentPoints: Int = this.unspentPoints,
        learnedNodeIds: Set<String> = this.learnedNodeIds,
        equippedAbilityIds: List<String> = this.equippedAbilityIds,
        energy: Int = this.energy,
        cooldownEnds: Map<String, Long> = this.cooldownEnds,
        processedActivationIds: Set<UUID> = this.processedActivationIds,
        dailyCombatExperience: Int = this.dailyCombatExperience,
        combatExperienceDay: Long = this.combatExperienceDay,
        recentMasteryKeys: Map<String, Long> = this.recentMasteryKeys,
    ): ForceRuntimeState = ForceRuntimeState(
        traditionId,
        rank,
        masteryExperience,
        unspentPoints,
        learnedNodeIds,
        equippedAbilityIds,
        energy,
        cooldownEnds,
        processedActivationIds,
        dailyCombatExperience,
        combatExperienceDay,
        recentMasteryKeys,
    )

    override fun equals(other: Any?): Boolean = this === other ||
        other is ForceRuntimeState &&
        traditionId == other.traditionId && rank == other.rank &&
        masteryExperience == other.masteryExperience && unspentPoints == other.unspentPoints &&
        learnedNodeIds == other.learnedNodeIds && equippedAbilityIds == other.equippedAbilityIds &&
        energy == other.energy && cooldownEnds == other.cooldownEnds &&
        processedActivationIds == other.processedActivationIds &&
        dailyCombatExperience == other.dailyCombatExperience &&
        combatExperienceDay == other.combatExperienceDay && recentMasteryKeys == other.recentMasteryKeys

    override fun hashCode(): Int = Objects.hash(
        traditionId, rank, masteryExperience, unspentPoints, learnedNodeIds,
        equippedAbilityIds, energy, cooldownEnds, processedActivationIds,
        dailyCombatExperience, combatExperienceDay, recentMasteryKeys,
    )

    override fun toString(): String = "ForceRuntimeState[tradition=$traditionId, rank=$rank, " +
        "mastery=$masteryExperience, points=$unspentPoints, energy=$energy, " +
        "learned=$learnedNodeIds, equipped=$equippedAbilityIds]"

    companion object {
        const val MAX_ENERGY: Int = 100
        const val MAX_RANK: Int = 10
        const val MAX_MASTERY_EXPERIENCE: Int = 320
        const val MAX_SKILL_POINTS: Int = 9
        const val MAX_LEARNED_NODES: Int = 39
        const val MAX_EQUIPPED_ABILITIES: Int = 3
        const val MAX_PROCESSED_ACTIVATIONS: Int = 128
        const val MAX_DAILY_COMBAT_EXPERIENCE: Int = 20
        const val MAX_RECENT_MASTERY_KEYS: Int = 64
        const val MASTERY_KEY_COOLDOWN_TICKS: Long = 200L
        const val TICKS_PER_DAY: Long = 24_000L

        private val TRADITIONS = setOf("jedi", "sith", "nightsister")

        @JvmStatic
        fun full(): ForceRuntimeState = ForceRuntimeState(
            "", 0, 0, 0, emptySet(), emptyList(), MAX_ENERGY,
            emptyMap(), emptySet(), 0, 0L, emptyMap(),
        )

        @JvmStatic
        fun normalizeTradition(value: String?): String = when (value.orEmpty()) {
            "light" -> "jedi"
            "dark" -> "nightsister"
            else -> value.orEmpty()
        }

        private fun legacyCoreNodes(value: String?): Set<String> = when (normalizeTradition(value)) {
            "jedi" -> linkedSetOf("jedi_force_sense", "light_push", "light_pull", "light_leap")
            "sith" -> linkedSetOf("sith_predator_sense", "dark_push", "dark_dash")
            "nightsister" -> linkedSetOf(
                "nightsister_spirit_sight", "magick_push", "shadow_step", "spirit_snare"
            )
            else -> emptySet()
        }

        private fun legacyLoadout(value: String?): List<String> = when (normalizeTradition(value)) {
            "jedi" -> listOf("light_push", "light_pull", "light_leap")
            "sith" -> listOf("dark_push", "dark_dash")
            "nightsister" -> listOf("magick_push", "shadow_step", "spirit_snare")
            else -> emptyList()
        }

        private fun legacyCooldowns(value: String?, source: Map<String, Long>): Map<String, Long> =
            when (normalizeTradition(value)) {
                "jedi" -> source.filterKeys { it in setOf("light_push", "light_pull", "light_leap") }
                "nightsister", "sith" -> emptyMap()
                else -> source
            }

        private fun immutableIdentifiers(source: Collection<String>, maximum: Int): Set<String> {
            val copy = LinkedHashSet<String>()
            source.forEach { value ->
                require(value.isNotBlank()) { "Force identifier cannot be blank" }
                copy.add(value)
            }
            require(copy.size <= maximum) { "Force identifier collection exceeds $maximum entries" }
            return Collections.unmodifiableSet(copy)
        }

        private fun immutableLoadout(source: Collection<String>): List<String> {
            val copy = source.map { value ->
                require(value.length <= 64) { "Force ability identifier is too long" }
                value
            }
            require(copy.size <= MAX_EQUIPPED_ABILITIES) { "Force loadout exceeds three abilities" }
            return Collections.unmodifiableList(copy.toList())
        }

        private fun immutableCooldowns(source: Map<String, Long>): Map<String, Long> {
            val copy = LinkedHashMap<String, Long>(source.size)
            source.forEach { (abilityId, cooldownEnd) ->
                copy[Objects.requireNonNull(abilityId, "cooldown abilityId")] =
                    Objects.requireNonNull(cooldownEnd, "cooldown end")
            }
            return Collections.unmodifiableMap(copy)
        }

        private fun boundedActivationIds(source: Set<UUID>): Set<UUID> {
            val copy = LinkedHashSet<UUID>(source.size)
            source.forEach { copy.add(Objects.requireNonNull(it, "processed activation id")) }
            while (copy.size > MAX_PROCESSED_ACTIVATIONS) {
                val iterator = copy.iterator()
                iterator.next()
                iterator.remove()
            }
            return Collections.unmodifiableSet(copy)
        }

        private fun immutableRecentMastery(source: Map<String, Long>): Map<String, Long> {
            val copy = LinkedHashMap<String, Long>()
            source.entries.sortedBy(Map.Entry<String, Long>::value).forEach { (key, value) ->
                require(key.isNotBlank()) { "Force mastery key cannot be blank" }
                copy[key] = value
            }
            while (copy.size > MAX_RECENT_MASTERY_KEYS) {
                val iterator = copy.entries.iterator()
                iterator.next()
                iterator.remove()
            }
            return Collections.unmodifiableMap(copy)
        }
    }
}
