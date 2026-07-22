package galacticwars.clonewars.classes

import dev.architectury.event.events.common.PlayerEvent
import dev.architectury.event.events.common.TickEvent
import galacticwars.clonewars.Config
import galacticwars.clonewars.ability.AbilityActivation
import galacticwars.clonewars.ability.AbilityDefinition
import galacticwars.clonewars.data.GameplayDataManager
import galacticwars.clonewars.menu.CommandCenterOperationsMenu
import galacticwars.clonewars.network.ClassHudPayload
import galacticwars.clonewars.network.GalacticNetwork
import galacticwars.clonewars.progression.ForceSavedData
import galacticwars.clonewars.progression.ProgressionEventType
import galacticwars.clonewars.progression.ProgressionSavedData
import galacticwars.clonewars.progression.ProgressionState
import java.util.LinkedHashSet
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3

/**
 * Loader-neutral, server-authoritative runtime for player class selection and abilities.
 *
 * Class state is persisted in [ClassProgressSavedData]. Clients receive only a bounded HUD
 * projection and can request actions; faction, progression, range, PvP, cooldown and Command
 * Center authority are all revalidated on the logical server.
 */
object PlayerClassRuntime {
    private const val MAX_REPLAY_IDS_PER_PLAYER = 128
    private const val ACTIVE_ABILITY_SLOTS = 2
    private const val SYNCHRONIZATION_INTERVAL_TICKS = 20

    private val registered = AtomicBoolean()
    private val processedActionIds = ConcurrentHashMap<UUID, LinkedHashSet<UUID>>()

    @JvmStatic
    fun register() {
        if (!registered.compareAndSet(false, true)) {
            return
        }
        PlayerEvent.PLAYER_JOIN.register(::synchronize)
        PlayerEvent.PLAYER_QUIT.register { player -> processedActionIds.remove(player.uuid) }
        PlayerEvent.PLAYER_RESPAWN.register { player, _, _ -> synchronize(player) }
        TickEvent.PLAYER_POST.register { player ->
            if (player is ServerPlayer && player.tickCount % SYNCHRONIZATION_INTERVAL_TICKS == 0) {
                tick(player)
            }
        }
    }

    @JvmStatic
    fun select(player: ServerPlayer, requestId: UUID, classId: String): Boolean {
        if (!acceptReplay(player.uuid, requestId)) {
            synchronize(player)
            return true
        }
        val operations = player.containerMenu as? CommandCenterOperationsMenu
            ?: return reject(player, "command_center_required")
        if (!operations.authorizesClassSelection(player)) {
            return reject(player, "command_center_required")
        }

        val definition = GameplayDataManager.snapshot().unitClass(classId).orElse(null)
            ?: return reject(player, "unknown_class")
        if (!definition.playerAssignable()) {
            return reject(player, "not_player_assignable")
        }

        val progression = ProgressionSavedData.get(player.level()).state(player.uuid)
        if (progression.factionId != definition.factionId().toString()) {
            return reject(player, "wrong_faction")
        }
        if (!definition.requirements().all { requirement -> requirementMet(progression, requirement) }) {
            return reject(player, "requirements_locked")
        }
        if (definition.forceTraditionSlot().isNotEmpty() &&
            ForceSavedData.get(player.level()).state(player.uuid).traditionId != definition.forceTraditionSlot()
        ) {
            return reject(player, "force_path_required")
        }

        ClassProgressSavedData.get(player.level()).update(player.uuid) { state ->
            state.switchClass(definition.id())
        }
        player.sendSystemMessage(Component.translatable(
            "message.galacticwars.class.selected",
            definition.displayName(),
        ))
        synchronize(player)
        return true
    }

    @JvmStatic
    fun activate(player: ServerPlayer, activationId: UUID, slot: Int): Boolean {
        if (slot !in 0 until ACTIVE_ABILITY_SLOTS) {
            return reject(player, "invalid_slot")
        }
        if (!acceptReplay(player.uuid, activationId)) {
            synchronize(player)
            return true
        }
        val level = player.level()
        val savedData = ClassProgressSavedData.get(level)
        val state = savedData.state(player.uuid)
        val definition = GameplayDataManager.snapshot().unitClass(state.classId()).orElse(null)
            ?: return reject(player, "unassigned")
        val abilities = activeAbilities(definition)
        if (slot >= abilities.size) {
            return reject(player, "empty_slot")
        }
        val ability = abilities[slot]
        val target = if (ability.activation == AbilityActivation.TARGET) {
            targetedLivingEntity(player, maxOf(1.0, ability.range))
        } else {
            null
        }
        val targetPresent = target?.isAlive == true
        val decision = ClassAbilityRuntimeService.activate(
            definition,
            ability,
            state,
            level.gameTime,
            targetPresent,
            target?.let { selected -> player.distanceTo(selected).toDouble() } ?: 0.0,
            target is Player,
            Config.ALLOW_CLASS_PVP.getAsBoolean(),
        )
        if (!decision.accepted()) {
            return reject(player, decision.reason())
        }
        if (targetPresent && !ClassAbilityEffectRegistry.isValidTarget(
                level,
                player,
                ability,
                target,
            )
        ) {
            return reject(player, "invalid_target")
        }
        if (!ClassAbilityEffectRegistry.execute(level, player, ability, target)) {
            return reject(player, if (ability.activation == AbilityActivation.AREA) {
                "no_valid_targets"
            } else {
                "effect_failed"
            })
        }

        val experience = maxOf(1L, ability.resourceCost.toLong())
        savedData.update(player.uuid) { decision.state().gainExperience(experience) }
        player.sendSystemMessage(Component.translatable(
            "message.galacticwars.class.activated",
            ability.displayName,
        ))
        synchronize(player)
        return true
    }

    @JvmStatic
    fun synchronize(player: ServerPlayer) {
        val payload = hudPayload(player)
        if (GalacticNetwork.canPlayerReceive(player, ClassHudPayload.TYPE)) {
            GalacticNetwork.CHANNEL.sendToPlayer({ player }, payload)
        }
    }

    @JvmStatic
    fun hudPayload(player: ServerPlayer): ClassHudPayload {
        val level = player.level()
        val state = ClassProgressSavedData.get(level).state(player.uuid)
        val definition = GameplayDataManager.snapshot().unitClass(state.classId()).orElse(null)
            ?: return ClassHudPayload.unassigned()
        if (definition.forceTraditionSlot().isNotEmpty()) {
            return ClassHudPayload.unassigned()
        }
        val abilities = activeAbilities(definition)
        val first = abilities.getOrNull(0)
        val second = abilities.getOrNull(1)
        return ClassHudPayload(
            state.classId(),
            state.rank(),
            state.resource(),
            first?.id?.toString().orEmpty(),
            cooldownRemaining(state, first, level.gameTime),
            second?.id?.toString().orEmpty(),
            cooldownRemaining(state, second, level.gameTime),
        )
    }

    private fun tick(player: ServerPlayer) {
        val level = player.level()
        val savedData = ClassProgressSavedData.get(level)
        val regenerated = savedData.update(player.uuid) { state -> state.regenerate(2) }
        val definition = GameplayDataManager.snapshot().unitClass(regenerated.classId()).orElse(null)
        if (definition != null) {
            definition.abilityIds().asSequence()
                .mapNotNull { abilityId ->
                    GameplayDataManager.snapshot().ability(abilityId.toString()).orElse(null)
                }
                .filter { ability -> ability.enabled && ability.activation == AbilityActivation.PASSIVE }
                .filter { ability -> ClassAbilityEffectRegistry.registered(ability.id.toString()) }
                .forEach { ability ->
                    ClassAbilityEffectRegistry.execute(level, player, ability, null)
                }
        }
        synchronize(player)
    }

    private fun activeAbilities(definition: UnitClassDefinition): List<AbilityDefinition> =
        definition.abilityIds().asSequence()
            .mapNotNull { abilityId ->
                GameplayDataManager.snapshot().ability(abilityId.toString()).orElse(null)
            }
            .filter { ability -> ability.enabled && ability.activation != AbilityActivation.PASSIVE }
            .take(ACTIVE_ABILITY_SLOTS)
            .toList()

    private fun requirementMet(
        progression: ProgressionState,
        requirement: ProgressionRequirement,
    ): Boolean = when (requirement.type()) {
        "quest" -> requirement.amount() == 1 && progression.hasSubjectPath(
            ProgressionEventType.QUEST_ADVANCED,
            requirement.subjectId(),
        )
        else -> false
    }

    private fun cooldownRemaining(
        state: ClassProgressState,
        ability: AbilityDefinition?,
        gameTime: Long,
    ): Int {
        if (ability == null) {
            return 0
        }
        val end = state.cooldownEnds()[ability.id.toString()] ?: 0L
        return (end - gameTime).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    }

    private fun targetedLivingEntity(player: ServerPlayer, range: Double): LivingEntity? {
        val start = player.eyePosition
        val direction: Vec3 = player.getViewVector(1.0F).scale(range)
        val end = start.add(direction)
        val blockHit = player.pick(range, 1.0F, false)
        val maximumDistanceSquared = if (blockHit.type == HitResult.Type.MISS) {
            range * range
        } else {
            start.distanceToSqr(blockHit.location)
        }
        val hit: EntityHitResult? = ProjectileUtil.getEntityHitResult(
            player,
            start,
            end,
            player.boundingBox.expandTowards(direction).inflate(1.0),
            { entity -> entity is LivingEntity && entity.isAlive && entity.isPickable && entity !== player },
            maximumDistanceSquared,
        )
        return hit?.entity as? LivingEntity
    }

    private fun acceptReplay(playerId: UUID, actionId: UUID): Boolean {
        val ids = processedActionIds.computeIfAbsent(playerId) { LinkedHashSet() }
        synchronized(ids) {
            if (!ids.add(actionId)) {
                return false
            }
            while (ids.size > MAX_REPLAY_IDS_PER_PLAYER) {
                val iterator = ids.iterator()
                iterator.next()
                iterator.remove()
            }
        }
        return true
    }

    private fun reject(player: ServerPlayer, reason: String): Boolean {
        player.sendSystemMessage(Component.translatable(
            "message.galacticwars.class.$reason",
        ))
        synchronize(player)
        return false
    }
}
