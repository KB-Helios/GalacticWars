package middleearth.lotr.warmod.army;

import java.util.Objects;

import middleearth.lotr.warmod.faction.FactionId;

public record ArmyUnitDefinition(
        ArmyUnitId id,
        String displayName,
        FactionId factionId,
        ArmyUnitRole role,
        int hireCost,
        int maxHealth,
        int attackDamage,
        ArmyFormation defaultFormation,
        String entityTypeId,
        double movementSpeed,
        double followRange,
        double armor,
        ArmyEquipmentLoadout equipment
) {
    public ArmyUnitDefinition(
            ArmyUnitId id,
            String displayName,
            FactionId factionId,
            ArmyUnitRole role,
            int hireCost,
            int maxHealth,
            int attackDamage,
            ArmyFormation defaultFormation
    ) {
        this(id, displayName, factionId, role, hireCost, maxHealth, attackDamage, defaultFormation,
                "", 0.28D, 24.0D, 0.0D, ArmyEquipmentLoadout.empty());
    }

    public ArmyUnitDefinition {
        Objects.requireNonNull(id, "id");
        displayName = requireNonBlank(displayName, "displayName");
        Objects.requireNonNull(factionId, "factionId");
        Objects.requireNonNull(role, "role");
        requireNonNegative(hireCost, "hireCost");
        requireNonNegative(maxHealth, "maxHealth");
        requireNonNegative(attackDamage, "attackDamage");
        Objects.requireNonNull(defaultFormation, "defaultFormation");
        entityTypeId = entityTypeId == null ? "" : entityTypeId.trim().toLowerCase();
        requireNonNegative(movementSpeed, "movementSpeed");
        requireNonNegative(followRange, "followRange");
        requireNonNegative(armor, "armor");
        equipment = equipment == null ? ArmyEquipmentLoadout.empty() : equipment;
    }

    private static void requireNonNegative(int value, String label) {
        if (value < 0) {
            throw new IllegalArgumentException(label + " cannot be negative");
        }
    }

    private static void requireNonNegative(double value, String label) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(label + " cannot be negative or non-finite");
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
