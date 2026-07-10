package middleearth.lotr.warmod.faction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import middleearth.lotr.warmod.KingdomWarsMiddleEarth;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class FactionAlignmentSavedData extends SavedData {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    private static final int MIN_ALIGNMENT = -100;
    private static final int MAX_ALIGNMENT = 100;

    private static final Codec<PlayerScores> PLAYER_SCORES_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("player_id").forGetter(PlayerScores::playerId),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("scores", Map.of())
                    .forGetter(PlayerScores::scores)
    ).apply(instance, PlayerScores::new));

    public static final Codec<FactionAlignmentSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema_version", CURRENT_SCHEMA_VERSION)
                    .forGetter(data -> data.schemaVersion),
            PLAYER_SCORES_CODEC.listOf().optionalFieldOf("players", List.of())
                    .forGetter(FactionAlignmentSavedData::serializedPlayers)
    ).apply(instance, FactionAlignmentSavedData::new));

    public static final SavedDataType<FactionAlignmentSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(KingdomWarsMiddleEarth.MODID, "faction_alignments"),
            FactionAlignmentSavedData::new,
            CODEC);

    private final int schemaVersion;
    private final Map<UUID, FactionAlignment> alignments = new LinkedHashMap<>();

    public FactionAlignmentSavedData() {
        this(CURRENT_SCHEMA_VERSION, List.of());
    }

    private FactionAlignmentSavedData(int schemaVersion, List<PlayerScores> players) {
        this.schemaVersion = Math.max(CURRENT_SCHEMA_VERSION, schemaVersion);
        for (PlayerScores player : players) {
            LinkedHashMap<FactionId, Integer> scores = new LinkedHashMap<>();
            player.scores().forEach((id, score) -> scores.put(FactionId.of(id), clamp(score)));
            alignments.put(player.playerId(), new FactionAlignment(player.playerId(), scores));
        }
    }

    public static FactionAlignmentSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public FactionAlignment alignment(UUID playerId) {
        return alignments.getOrDefault(playerId, FactionAlignment.empty(playerId));
    }

    public FactionAlignmentUpdateResult applyPledge(
            UUID playerId,
            FactionDefinition faction,
            FactionCatalog catalog
    ) {
        FactionAlignmentUpdateResult raw = FactionAlignmentUpdater.apply(
                alignment(playerId),
                catalog,
                faction.id(),
                new FactionAlignmentRule(
                        faction.pledgeDirectDelta(),
                        faction.pledgeAllyDelta(),
                        faction.pledgeEnemyDelta(),
                        "faction_pledge"));
        LinkedHashMap<FactionId, Integer> clampedScores = new LinkedHashMap<>();
        raw.alignment().scores().forEach((id, score) -> clampedScores.put(id, clamp(score)));
        FactionAlignment updated = new FactionAlignment(playerId, clampedScores);
        alignments.put(playerId, updated);
        this.setDirty();

        ArrayList<FactionAlignmentChange> changes = new ArrayList<>();
        for (FactionAlignmentChange change : raw.changes()) {
            int before = alignmentFromChanges(raw, change.factionId(), change.beforeScore());
            int after = updated.score(change.factionId());
            if (after != before) {
                changes.add(new FactionAlignmentChange(
                        change.factionId(), before, after - before, after, change.reasonCode()));
            }
        }
        return new FactionAlignmentUpdateResult(updated, List.copyOf(changes));
    }

    public void setScore(UUID playerId, FactionId factionId, int score) {
        FactionAlignment current = alignment(playerId);
        LinkedHashMap<FactionId, Integer> scores = new LinkedHashMap<>(current.scores());
        scores.put(factionId, clamp(score));
        alignments.put(playerId, new FactionAlignment(playerId, scores));
        this.setDirty();
    }

    private List<PlayerScores> serializedPlayers() {
        ArrayList<PlayerScores> players = new ArrayList<>();
        for (FactionAlignment alignment : alignments.values()) {
            LinkedHashMap<String, Integer> scores = new LinkedHashMap<>();
            alignment.scores().forEach((id, score) -> scores.put(id.toString(), clamp(score)));
            players.add(new PlayerScores(alignment.playerId(), scores));
        }
        return List.copyOf(players);
    }

    private static int alignmentFromChanges(
            FactionAlignmentUpdateResult raw,
            FactionId factionId,
            int fallback
    ) {
        return raw.changes().stream()
                .filter(change -> change.factionId().equals(factionId))
                .map(FactionAlignmentChange::beforeScore)
                .findFirst()
                .orElse(fallback);
    }

    private static int clamp(int score) {
        return Math.max(MIN_ALIGNMENT, Math.min(MAX_ALIGNMENT, score));
    }

    private record PlayerScores(UUID playerId, Map<String, Integer> scores) {
        private PlayerScores {
            scores = Map.copyOf(scores);
        }
    }
}
