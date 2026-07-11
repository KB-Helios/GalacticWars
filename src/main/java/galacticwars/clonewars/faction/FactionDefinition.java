package galacticwars.clonewars.faction;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record FactionDefinition(
        FactionId id,
        String displayName,
        int hireCost,
        int minimumHiringAlignment,
        int maxOwnedRecruits,
        Set<FactionId> allies,
        Set<FactionId> enemies,
        int selectionOrder,
        String pledgeTokenItemId,
        int pledgeDirectDelta,
        int pledgeAllyDelta,
        int pledgeEnemyDelta,
        FactionStrategyDefinition strategy
) {
    public FactionDefinition(
            FactionId id,
            String displayName,
            int hireCost,
            int minimumHiringAlignment,
            int maxOwnedRecruits,
            Set<FactionId> allies,
            Set<FactionId> enemies
    ) {
        this(id, displayName, hireCost, minimumHiringAlignment, maxOwnedRecruits, allies, enemies,
                0, "", 10, 2, -5, FactionStrategyDefinition.shared());
    }

    public FactionDefinition(
            FactionId id,
            String displayName,
            int hireCost,
            int minimumHiringAlignment,
            int maxOwnedRecruits,
            Set<FactionId> allies,
            Set<FactionId> enemies,
            int selectionOrder,
            String pledgeTokenItemId,
            int pledgeDirectDelta,
            int pledgeAllyDelta,
            int pledgeEnemyDelta
    ) {
        this(id, displayName, hireCost, minimumHiringAlignment, maxOwnedRecruits, allies, enemies,
                selectionOrder, pledgeTokenItemId, pledgeDirectDelta, pledgeAllyDelta, pledgeEnemyDelta,
                FactionStrategyDefinition.shared());
    }

    public FactionDefinition {
        Objects.requireNonNull(id, "id");
        displayName = requireNonBlank(displayName, "displayName");
        if (hireCost < 0) {
            throw new IllegalArgumentException("hireCost cannot be negative");
        }
        if (maxOwnedRecruits < 0) {
            throw new IllegalArgumentException("maxOwnedRecruits cannot be negative");
        }
        allies = immutableCopy(allies, "allies");
        enemies = immutableCopy(enemies, "enemies");
        validateRelationSets(id.toString(), id, allies, enemies);
        pledgeTokenItemId = pledgeTokenItemId == null ? "" : pledgeTokenItemId.trim().toLowerCase();
        Objects.requireNonNull(strategy, "strategy");
    }

    public static void validateRelationSets(
            String resourceId,
            FactionId factionId,
            Set<FactionId> allies,
            Set<FactionId> enemies
    ) {
        Objects.requireNonNull(resourceId, "resourceId");
        Objects.requireNonNull(factionId, "factionId");
        Objects.requireNonNull(allies, "allies");
        Objects.requireNonNull(enemies, "enemies");
        for (FactionId related : allies) {
            if (enemies.contains(related)) {
                throw new IllegalArgumentException("Faction " + factionId + " in " + resourceId
                        + " declares " + related + " as both ally and enemy");
            }
        }
    }

    private static Set<FactionId> immutableCopy(Set<FactionId> values, String label) {
        Objects.requireNonNull(values, label);
        return Collections.unmodifiableSet(new LinkedHashSet<>(values));
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
