package galacticwars.clonewars.classes;

import galacticwars.clonewars.ability.AbilityId;
import galacticwars.clonewars.army.ArmyUnitId;
import galacticwars.clonewars.faction.FactionId;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record UnitClassDefinition(
        UnitClassId id,
        String displayName,
        FactionId factionId,
        ArmyUnitId unitId,
        boolean playerAssignable,
        List<AbilityId> abilityIds,
        List<ProgressionRequirement> requirements,
        String forcePathSlot
) {
    public UnitClassDefinition {
        Objects.requireNonNull(id, "id");
        displayName = requireNonBlank(displayName, "displayName");
        Objects.requireNonNull(factionId, "factionId");
        Objects.requireNonNull(unitId, "unitId");
        abilityIds = List.copyOf(Objects.requireNonNull(abilityIds, "abilityIds"));
        requirements = List.copyOf(Objects.requireNonNull(requirements, "requirements"));
        if (abilityIds.isEmpty()) {
            throw new IllegalArgumentException("A unit class requires at least one ability");
        }
        if (new LinkedHashSet<>(abilityIds).size() != abilityIds.size()) {
            throw new IllegalArgumentException("A unit class cannot repeat an ability");
        }
        forcePathSlot = forcePathSlot == null ? "" : forcePathSlot.trim().toLowerCase(Locale.ROOT);
        if (!forcePathSlot.isEmpty() && !forcePathSlot.equals("light") && !forcePathSlot.equals("dark")) {
            throw new IllegalArgumentException("forcePathSlot must be empty, light, or dark");
        }
    }

    private static String requireNonBlank(String value, String label) {
        Objects.requireNonNull(value, label);
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return normalized;
    }
}
