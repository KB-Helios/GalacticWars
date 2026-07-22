package galacticwars.clonewars.settlement;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record BlueprintWorldgenProfile(
        List<String> biomes,
        String factionId,
        int siteRadius,
        List<BlueprintRosterEntry> roster,
        List<String> lootMarkers,
        int placementWeight
) {
    public BlueprintWorldgenProfile {
        biomes = normalizeList(biomes, "biome");
        factionId = normalize(factionId, "factionId");
        roster = List.copyOf(Objects.requireNonNull(roster, "roster"));
        lootMarkers = normalizeList(lootMarkers, "loot marker");
        if (biomes.isEmpty() || roster.isEmpty() || siteRadius < 8 || siteRadius > 128 || placementWeight <= 0) {
            throw new IllegalArgumentException("invalid worldgen profile for " + factionId);
        }
        int maximumResidents = roster.stream().mapToInt(BlueprintRosterEntry::maximum).sum();
        if (maximumResidents > 32) {
            throw new IllegalArgumentException("worldgen roster exceeds 32 residents for " + factionId);
        }
    }

    private static List<String> normalizeList(List<String> values, String label) {
        return Objects.requireNonNull(values, label).stream().map(value -> normalize(value, label)).distinct().toList();
    }

    private static String normalize(String value, String label) {
        Objects.requireNonNull(value, label);
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return normalized;
    }
}
