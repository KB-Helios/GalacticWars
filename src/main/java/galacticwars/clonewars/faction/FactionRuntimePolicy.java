package galacticwars.clonewars.faction;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record FactionRuntimePolicy(
        FactionId factionId,
        Set<String> traits,
        Map<String, Integer> modifiers
) {
    public FactionRuntimePolicy {
        Objects.requireNonNull(factionId, "factionId");
        LinkedHashSet<String> normalizedTraits = new LinkedHashSet<>();
        for (String trait : Objects.requireNonNull(traits, "traits")) {
            normalizedTraits.add(normalize(trait, "trait"));
        }
        traits = Set.copyOf(normalizedTraits);
        LinkedHashMap<String, Integer> normalizedModifiers = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : Objects.requireNonNull(modifiers, "modifiers").entrySet()) {
            String key = normalize(entry.getKey(), "modifier");
            Integer value = Objects.requireNonNull(entry.getValue(), "modifier value");
            if (value < 0 || value > 500) {
                throw new IllegalArgumentException("modifier " + key + " must be between 0 and 500");
            }
            if (normalizedModifiers.containsKey(key)) {
                throw new IllegalArgumentException("duplicate modifier key after normalization: " + key);
            }
            normalizedModifiers.put(key, value);
        }
        modifiers = Map.copyOf(normalizedModifiers);
    }

    public int modifier(String id, int fallback) {
        return modifiers.getOrDefault(normalize(id, "modifier"), fallback);
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
