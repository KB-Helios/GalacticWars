package middleearth.lotr.warmod.faction;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record FactionAlignment(UUID playerId, Map<FactionId, Integer> scores) {
    public FactionAlignment {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(scores, "scores");
        LinkedHashMap<FactionId, Integer> clamped = new LinkedHashMap<>();
        scores.forEach((faction, score) -> clamped.put(
                Objects.requireNonNull(faction, "faction"), clamp(Objects.requireNonNull(score, "score"))));
        scores = Collections.unmodifiableMap(clamped);
    }

    public static FactionAlignment empty(UUID playerId) {
        return new FactionAlignment(playerId, Map.of());
    }

    public int score(FactionId factionId) {
        return scores.getOrDefault(Objects.requireNonNull(factionId, "factionId"), 0);
    }

    public FactionAlignment withAddedScore(FactionId factionId, int delta) {
        Objects.requireNonNull(factionId, "factionId");
        LinkedHashMap<FactionId, Integer> updatedScores = new LinkedHashMap<>(scores);
        updatedScores.put(factionId, clamp((long) score(factionId) + delta));
        return new FactionAlignment(playerId, updatedScores);
    }

    private static int clamp(long score) {
        return (int) Math.max(-100L, Math.min(100L, score));
    }
}
