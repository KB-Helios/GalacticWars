package galacticwars.clonewars.army;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import galacticwars.clonewars.faction.FactionId;

public record ArmyUnitCatalog(Map<ArmyUnitId, ArmyUnitDefinition> definitions) {
    public ArmyUnitCatalog(List<ArmyUnitDefinition> definitions) {
        this(indexDefinitions(definitions));
    }

    public ArmyUnitCatalog {
        Objects.requireNonNull(definitions, "definitions");
        definitions = Collections.unmodifiableMap(new LinkedHashMap<>(definitions));
    }

    public Optional<ArmyUnitDefinition> definition(ArmyUnitId unitId) {
        return Optional.ofNullable(definitions.get(Objects.requireNonNull(unitId, "unitId")));
    }

    public List<ArmyUnitDefinition> unitsForFaction(FactionId factionId) {
        Objects.requireNonNull(factionId, "factionId");
        ArrayList<ArmyUnitDefinition> matches = new ArrayList<>();
        for (ArmyUnitDefinition definition : definitions.values()) {
            if (definition.factionId().equals(factionId)) {
                matches.add(definition);
            }
        }
        return List.copyOf(matches);
    }

    public List<ArmyUnitDefinition> unitsForRole(ArmyUnitRole role) {
        Objects.requireNonNull(role, "role");
        ArrayList<ArmyUnitDefinition> matches = new ArrayList<>();
        for (ArmyUnitDefinition definition : definitions.values()) {
            if (definition.role() == role) {
                matches.add(definition);
            }
        }
        return List.copyOf(matches);
    }

    private static Map<ArmyUnitId, ArmyUnitDefinition> indexDefinitions(List<ArmyUnitDefinition> definitions) {
        Objects.requireNonNull(definitions, "definitions");
        LinkedHashMap<ArmyUnitId, ArmyUnitDefinition> indexed = new LinkedHashMap<>();
        for (ArmyUnitDefinition definition : definitions) {
            Objects.requireNonNull(definition, "definition");
            ArmyUnitDefinition previous = indexed.putIfAbsent(definition.id(), definition);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate army unit id: " + definition.id());
            }
        }
        return indexed;
    }
}
