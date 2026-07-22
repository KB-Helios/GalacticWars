package galacticwars.clonewars.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.data.SavedDataSchemaPolicy;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Authoritative persisted player Force career, loadout, energy, cooldown, and anti-replay state. */
public final class ForceSavedData extends SavedData {
    public static final int CURRENT_SCHEMA_VERSION = 2;
    private static final Codec<PlayerForceState> PLAYER_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("player_id").forGetter(PlayerForceState::playerId),
            Codec.STRING.optionalFieldOf("path", "").forGetter(PlayerForceState::path),
            Codec.STRING.optionalFieldOf("tradition", "").forGetter(PlayerForceState::tradition),
            Codec.INT.optionalFieldOf("rank", 0).forGetter(PlayerForceState::rank),
            Codec.INT.optionalFieldOf("mastery_experience", 0).forGetter(PlayerForceState::masteryExperience),
            Codec.INT.optionalFieldOf("unspent_points", 0).forGetter(PlayerForceState::unspentPoints),
            Codec.STRING.listOf().optionalFieldOf("learned_nodes", List.of())
                    .forGetter(PlayerForceState::learnedNodes),
            Codec.STRING.listOf().optionalFieldOf("equipped_abilities", List.of())
                    .forGetter(PlayerForceState::equippedAbilities),
            Codec.INT.optionalFieldOf("energy", ForceRuntimeState.MAX_ENERGY)
                    .forGetter(PlayerForceState::energy),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("cooldowns", Map.of())
                    .forGetter(PlayerForceState::cooldowns),
            UUIDUtil.CODEC.listOf().optionalFieldOf("processed_activations", List.of())
                    .forGetter(PlayerForceState::processedActivations),
            Codec.INT.optionalFieldOf("daily_combat_experience", 0)
                    .forGetter(PlayerForceState::dailyCombatExperience),
            Codec.LONG.optionalFieldOf("combat_experience_day", 0L)
                    .forGetter(PlayerForceState::combatExperienceDay),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("recent_mastery", Map.of())
                    .forGetter(PlayerForceState::recentMastery)
    ).apply(instance, PlayerForceState::new));
    public static final Codec<ForceSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema_version", 1).forGetter(data -> CURRENT_SCHEMA_VERSION),
            PLAYER_CODEC.listOf().optionalFieldOf("players", List.of()).forGetter(ForceSavedData::serialized)
    ).apply(instance, ForceSavedData::new));
    public static final SavedDataType<ForceSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, "force_state"),
            ForceSavedData::new, CODEC, null);

    private final Map<UUID, ForceRuntimeState> states = new LinkedHashMap<>();

    public ForceSavedData() {
    }

    private ForceSavedData(int schemaVersion, List<PlayerForceState> players) {
        SavedDataSchemaPolicy.migrate(schemaVersion, CURRENT_SCHEMA_VERSION, "force");
        for (PlayerForceState player : players) {
            ForceRuntimeState state;
            if (schemaVersion < 2 || player.tradition().isBlank()) {
                state = new ForceRuntimeState(
                        player.path(), boundedEnergy(player.energy()), player.cooldowns(),
                        new LinkedHashSet<>(player.processedActivations()));
            } else {
                state = new ForceRuntimeState(
                        player.tradition(), bounded(player.rank(), 1, ForceRuntimeState.MAX_RANK),
                        bounded(player.masteryExperience(), 0, ForceRuntimeState.MAX_MASTERY_EXPERIENCE),
                        bounded(player.unspentPoints(), 0, ForceRuntimeState.MAX_SKILL_POINTS),
                        boundedSet(player.learnedNodes(), ForceRuntimeState.MAX_LEARNED_NODES),
                        player.equippedAbilities().stream()
                                .map(value -> value == null ? "" : value)
                                .limit(ForceRuntimeState.MAX_EQUIPPED_ABILITIES).toList(),
                        boundedEnergy(player.energy()), player.cooldowns(),
                        new LinkedHashSet<>(player.processedActivations()),
                        bounded(player.dailyCombatExperience(), 0,
                                ForceRuntimeState.MAX_DAILY_COMBAT_EXPERIENCE),
                        Math.max(0L, player.combatExperienceDay()), player.recentMastery());
            }
            states.put(player.playerId(), state);
        }
    }

    public static ForceSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public ForceRuntimeState state(UUID playerId) {
        return states.getOrDefault(playerId, ForceRuntimeState.full());
    }

    public boolean hasStoredState(UUID playerId) {
        return states.containsKey(playerId);
    }

    public ForceRuntimeState update(UUID playerId, UnaryOperator<ForceRuntimeState> updater) {
        ForceRuntimeState before = state(playerId);
        ForceRuntimeState after = java.util.Objects.requireNonNull(updater.apply(before), "Force state update");
        if (!after.equals(before)) {
            states.put(playerId, after);
            setDirty();
        }
        return after;
    }

    public boolean compareAndSet(UUID playerId, ForceRuntimeState expected, ForceRuntimeState updated) {
        if (!state(playerId).equals(expected)) return false;
        if (updated.equals(expected)) return true;
        states.put(playerId, updated);
        setDirty();
        return true;
    }

    public ForceAbilityRuntimeService.ActivationDecision evaluate(
            ProgressionState progression, ForceRuntimeState expectedState,
            UUID activationId, String abilityId, long gameTime,
            boolean targetsPlayer, boolean allowForcePvp
    ) {
        if (!state(progression.playerId()).equals(expectedState)) {
            throw new IllegalArgumentException("expected Force state is stale");
        }
        return ForceAbilityRuntimeService.activate(
                progression, expectedState, activationId, abilityId,
                gameTime, targetsPlayer, allowForcePvp);
    }

    public boolean commitEvaluated(
            UUID playerId,
            ForceRuntimeState expectedState,
            ForceAbilityRuntimeService.ActivationDecision evaluated
    ) {
        if (!state(playerId).equals(expectedState)) return false;
        if (!evaluated.accepted() || evaluated.state().equals(expectedState)) {
            throw new IllegalArgumentException("invalid evaluated Force activation");
        }
        return compareAndSet(playerId, expectedState, evaluated.state());
    }

    public boolean restoreAfterFailedTransaction(
            UUID playerId,
            ForceRuntimeState expectedCurrent,
            ForceRuntimeState previous,
            boolean previousWasStored
    ) {
        if (!state(playerId).equals(expectedCurrent)) return false;
        if (previousWasStored) states.put(playerId, previous); else states.remove(playerId);
        setDirty();
        return true;
    }

    public ForceAbilityRuntimeService.ActivationDecision activate(
            ProgressionState progression, UUID activationId, String abilityId,
            long gameTime, boolean targetsPlayer, boolean allowForcePvp
    ) {
        ForceRuntimeState before = state(progression.playerId());
        ForceAbilityRuntimeService.ActivationDecision decision = evaluate(
                progression, before, activationId, abilityId,
                gameTime, targetsPlayer, allowForcePvp);
        if (decision.accepted() && !decision.state().equals(before)) {
            commitEvaluated(progression.playerId(), before, decision);
        }
        return decision;
    }

    public void regenerate(UUID playerId, int amount) {
        update(playerId, state -> state.regenerate(amount));
    }

    private List<PlayerForceState> serialized() {
        return states.entrySet().stream().map(entry -> new PlayerForceState(
                entry.getKey(), entry.getValue().path(), entry.getValue().traditionId(),
                entry.getValue().rank(), entry.getValue().masteryExperience(),
                entry.getValue().unspentPoints(), entry.getValue().learnedNodeIds().stream().toList(),
                entry.getValue().equippedAbilityIds(), entry.getValue().energy(),
                entry.getValue().cooldownEnds(),
                entry.getValue().processedActivationIds().stream().toList(),
                entry.getValue().dailyCombatExperience(), entry.getValue().combatExperienceDay(),
                entry.getValue().recentMasteryKeys())).toList();
    }

    private static int boundedEnergy(int value) {
        return bounded(value, 0, ForceRuntimeState.MAX_ENERGY);
    }

    private static int bounded(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static Set<String> boundedSet(List<String> values, int maximum) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        values.stream().filter(value -> value != null && !value.isBlank())
                .limit(maximum).forEach(result::add);
        return result;
    }

    private record PlayerForceState(
            UUID playerId,
            String path,
            String tradition,
            int rank,
            int masteryExperience,
            int unspentPoints,
            List<String> learnedNodes,
            List<String> equippedAbilities,
            int energy,
            Map<String, Long> cooldowns,
            List<UUID> processedActivations,
            int dailyCombatExperience,
            long combatExperienceDay,
            Map<String, Long> recentMastery
    ) {
        private PlayerForceState {
            learnedNodes = List.copyOf(learnedNodes);
            equippedAbilities = List.copyOf(equippedAbilities);
            cooldowns = Map.copyOf(cooldowns);
            processedActivations = List.copyOf(processedActivations);
            recentMastery = Map.copyOf(recentMastery);
        }
    }
}
