package galacticwars.clonewars.world;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record CivilianArchetypeDefinition(
        String id,
        String displayName,
        String factionId,
        String entityTypeId,
        List<String> professions,
        int maxHealth,
        double movementSpeed,
        int baseMorale,
        String homeType
) {
    public CivilianArchetypeDefinition {
        id = required(id, "id");
        displayName = Objects.requireNonNull(displayName, "displayName").trim();
        factionId = required(factionId, "factionId");
        entityTypeId = required(entityTypeId, "entityTypeId");
        professions = List.copyOf(Objects.requireNonNull(professions, "professions"));
        homeType = required(homeType, "homeType");
        if (displayName.isEmpty() || professions.isEmpty() || maxHealth < 1
                || movementSpeed <= 0.0D || baseMorale < 0 || baseMorale > 100) {
            throw new IllegalArgumentException("invalid civilian archetype " + id);
        }
    }

    private static String required(String value, String label) {
        value = Objects.requireNonNull(value, label).trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) throw new IllegalArgumentException(label + " cannot be blank");
        return value;
    }
}
