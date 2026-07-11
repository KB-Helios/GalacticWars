package galacticwars.clonewars.world;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import galacticwars.clonewars.faction.FactionId;

public record GalacticRegionDefinition(
        GalacticRegionId id,
        String displayName,
        FactionId controllingFaction,
        GalacticRegionClimate climate,
        float baseTemperature,
        float downfall,
        int spawnWeight,
        Set<String> features
) {
    public GalacticRegionDefinition {
        Objects.requireNonNull(id, "id");
        displayName = requireNonBlank(displayName, "displayName");
        Objects.requireNonNull(controllingFaction, "controllingFaction");
        Objects.requireNonNull(climate, "climate");
        requireNonNegative(baseTemperature, "baseTemperature");
        requireNonNegative(downfall, "downfall");
        requireNonNegative(spawnWeight, "spawnWeight");
        features = copyFeatures(features);
    }

    private static Set<String> copyFeatures(Set<String> values) {
        Objects.requireNonNull(values, "features");
        LinkedHashSet<String> copied = new LinkedHashSet<>();
        for (String value : values) {
            copied.add(requireNonBlank(value, "feature"));
        }
        return Set.copyOf(copied);
    }

    private static void requireNonNegative(float value, String label) {
        if (value < 0.0F) {
            throw new IllegalArgumentException(label + " cannot be negative");
        }
    }

    private static void requireNonNegative(int value, String label) {
        if (value < 0) {
            throw new IllegalArgumentException(label + " cannot be negative");
        }
    }

    private static String requireNonBlank(String value, String label) {
        Objects.requireNonNull(value, label);
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return trimmed;
    }
}
