package galacticwars.clonewars.faction;

import java.util.Locale;
import java.util.Objects;

public record FactionStrategyDefinition(
        String archetype,
        int recruitmentCapacityBonus,
        int upkeepPercent,
        int productionPercent,
        int moraleBonus,
        String strategicStrength,
        String strategicWeakness
) {
    public FactionStrategyDefinition {
        archetype = normalize(archetype, "archetype");
        if (recruitmentCapacityBonus < 0 || upkeepPercent < 25 || upkeepPercent > 400
                || productionPercent < 25 || productionPercent > 400
                || moraleBonus < -100 || moraleBonus > 100) {
            throw new IllegalArgumentException("invalid faction strategy balance values");
        }
        strategicStrength = normalize(strategicStrength, "strategicStrength");
        strategicWeakness = normalize(strategicWeakness, "strategicWeakness");
    }

    public static FactionStrategyDefinition shared() {
        return new FactionStrategyDefinition("shared", 0, 100, 100, 0,
                "combined_arms", "none");
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
