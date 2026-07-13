package galacticwars.clonewars.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.data.SavedDataSchemaPolicy;
import galacticwars.clonewars.economy.CreditTransactionService;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ProgressionSavedData extends SavedData {
    private static final Codec<PlayerState> PLAYER_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("player_id").forGetter(PlayerState::playerId),
            Codec.STRING.optionalFieldOf("faction_id", "").forGetter(PlayerState::factionId),
            Codec.INT.optionalFieldOf("credits", 0).forGetter(PlayerState::credits),
            UUIDUtil.CODEC.listOf().optionalFieldOf("processed_events", List.of()).forGetter(PlayerState::processedEvents),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("event_totals", Map.of()).forGetter(PlayerState::eventTotals),
            Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()).optionalFieldOf("event_subjects", Map.of()).forGetter(PlayerState::eventSubjects),
            Codec.STRING.listOf().optionalFieldOf("unlocks", List.of()).forGetter(PlayerState::unlocks)
    ).apply(instance, PlayerState::new));

    public static final Codec<ProgressionSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema_version", ProgressionState.CURRENT_SCHEMA_VERSION)
                    .forGetter(data -> ProgressionState.CURRENT_SCHEMA_VERSION),
            PLAYER_CODEC.listOf().optionalFieldOf("players", List.of()).forGetter(ProgressionSavedData::serialized)
    ).apply(instance, ProgressionSavedData::new));

    public static final SavedDataType<ProgressionSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, "progression"),
            ProgressionSavedData::new,
            CODEC);

    private final Map<UUID, ProgressionState> states = new LinkedHashMap<>();

    public ProgressionSavedData() {
    }

    private ProgressionSavedData(int schemaVersion, List<PlayerState> players) {
        SavedDataSchemaPolicy.migrate(
                schemaVersion, ProgressionState.CURRENT_SCHEMA_VERSION, "progression");
        for (PlayerState player : players) {
            HashMap<ProgressionEventType, Integer> totals = new HashMap<>();
            player.eventTotals().forEach((key, value) -> totals.put(type(key), Math.max(0, value)));
            HashMap<ProgressionEventType, Set<String>> subjects = new HashMap<>();
            player.eventSubjects().forEach((key, values) -> subjects.put(type(key), Set.copyOf(values)));
            states.put(player.playerId(), new ProgressionState(
                    ProgressionState.CURRENT_SCHEMA_VERSION,
                    player.playerId(),
                    player.factionId(),
                    Math.max(0, player.credits()),
                    Set.copyOf(player.processedEvents()),
                    totals,
                    subjects,
                    Set.copyOf(player.unlocks())));
        }
    }

    public static ProgressionSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public ProgressionState state(UUID playerId) {
        return states.getOrDefault(playerId, ProgressionState.create(playerId));
    }

    public ProgressionDecision apply(ProgressionEvent event) {
        ProgressionState before = state(event.playerId());
        ProgressionDecision decision = CampaignRuntimeService.record(before, event);
        if (decision.accepted() && decision.changed()) {
            states.put(event.playerId(), decision.state());
            this.setDirty();
        }
        return decision;
    }

    public ProgressionDecision commitEvaluated(
            ProgressionEvent event,
            ProgressionState expectedState,
            ProgressionDecision evaluated
    ) {
        ProgressionState current = state(event.playerId());
        if (!current.equals(expectedState)) {
            return apply(event);
        }
        if (!evaluated.accepted() || !evaluated.changed()
                || !evaluated.state().playerId().equals(event.playerId())
                || !evaluated.state().processed(event.id())) {
            throw new IllegalArgumentException("invalid evaluated progression decision");
        }
        states.put(event.playerId(), evaluated.state());
        ProgressionState completed = CampaignRuntimeService.completeEligibleQuests(evaluated.state());
        states.put(event.playerId(), completed);
        this.setDirty();
        return new ProgressionDecision(true, true, "accepted", completed);
    }

    public int pendingCreditRewards(UUID playerId) {
        return state(playerId).pendingCreditRewards();
    }

    /** Atomically retires pending numeric rewards and materializes them as Credit Chip items. */
    public int claimCreditRewards(ServerPlayer player) {
        ProgressionState current = state(player.getUUID());
        int amount = current.pendingCreditRewards();
        if (amount <= 0) {
            return 0;
        }
        states.put(player.getUUID(), current.clearPendingCreditRewards());
        this.setDirty();
        CreditTransactionService.refundPlayer(player, amount);
        return amount;
    }

    private List<PlayerState> serialized() {
        ArrayList<PlayerState> players = new ArrayList<>();
        for (ProgressionState state : states.values()) {
            LinkedHashMap<String, Integer> totals = new LinkedHashMap<>();
            state.eventTotals().forEach((type, value) -> totals.put(key(type), value));
            LinkedHashMap<String, List<String>> subjects = new LinkedHashMap<>();
            state.eventSubjects().forEach((type, values) -> subjects.put(key(type), values.stream().sorted().toList()));
            players.add(new PlayerState(
                    state.playerId(), state.factionId(), state.credits(),
                    state.processedEventIds().stream().sorted().toList(), totals, subjects,
                    state.unlocks().stream().sorted().toList()));
        }
        return List.copyOf(players);
    }

    private static String key(ProgressionEventType type) {
        return type.name().toLowerCase(Locale.ROOT);
    }

    private static ProgressionEventType type(String value) {
        return ProgressionEventType.valueOf(value.toUpperCase(Locale.ROOT));
    }

    private record PlayerState(
            UUID playerId,
            String factionId,
            int credits,
            List<UUID> processedEvents,
            Map<String, Integer> eventTotals,
            Map<String, List<String>> eventSubjects,
            List<String> unlocks
    ) {
        private PlayerState {
            processedEvents = List.copyOf(processedEvents);
            eventTotals = Map.copyOf(eventTotals);
            eventSubjects = Map.copyOf(eventSubjects);
            unlocks = List.copyOf(unlocks);
        }
    }
}
