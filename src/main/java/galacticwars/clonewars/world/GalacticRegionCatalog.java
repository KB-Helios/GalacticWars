package galacticwars.clonewars.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import galacticwars.clonewars.faction.FactionId;

public record GalacticRegionCatalog(Map<GalacticRegionId, GalacticRegionDefinition> definitions) {
    public GalacticRegionCatalog(List<GalacticRegionDefinition> definitions) {
        this(indexDefinitions(definitions));
    }

    public GalacticRegionCatalog {
        Objects.requireNonNull(definitions, "definitions");
        LinkedHashMap<GalacticRegionId, GalacticRegionDefinition> copied = new LinkedHashMap<>();
        for (Map.Entry<GalacticRegionId, GalacticRegionDefinition> entry : definitions.entrySet()) {
            GalacticRegionId id = Objects.requireNonNull(entry.getKey(), "catalog key cannot be null");
            GalacticRegionDefinition definition =
                    Objects.requireNonNull(entry.getValue(), "catalog value cannot be null");
            if (!id.equals(definition.id())) {
                throw new IllegalArgumentException("Mismatched region catalog key and definition id: "
                        + id + " vs " + definition.id());
            }
            copied.put(id, definition);
        }
        definitions = Collections.unmodifiableMap(copied);
    }

    public Optional<GalacticRegionDefinition> definition(GalacticRegionId regionId) {
        return Optional.ofNullable(definitions.get(Objects.requireNonNull(regionId, "regionId")));
    }

    public List<GalacticRegionDefinition> regionsForFaction(FactionId factionId) {
        Objects.requireNonNull(factionId, "factionId");
        ArrayList<GalacticRegionDefinition> matches = new ArrayList<>();
        for (GalacticRegionDefinition definition : definitions.values()) {
            if (definition.controllingFaction().equals(factionId)) {
                matches.add(definition);
            }
        }
        return List.copyOf(matches);
    }

    public List<GalacticRegionDefinition> regionsForClimate(GalacticRegionClimate climate) {
        Objects.requireNonNull(climate, "climate");
        ArrayList<GalacticRegionDefinition> matches = new ArrayList<>();
        for (GalacticRegionDefinition definition : definitions.values()) {
            if (definition.climate() == climate) {
                matches.add(definition);
            }
        }
        return List.copyOf(matches);
    }

    private static Map<GalacticRegionId, GalacticRegionDefinition> indexDefinitions(
            List<GalacticRegionDefinition> definitions) {
        Objects.requireNonNull(definitions, "definitions");
        LinkedHashMap<GalacticRegionId, GalacticRegionDefinition> indexed = new LinkedHashMap<>();
        for (GalacticRegionDefinition definition : definitions) {
            Objects.requireNonNull(definition, "definition");
            GalacticRegionDefinition previous = indexed.putIfAbsent(definition.id(), definition);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate galactic region id: " + definition.id());
            }
        }
        return indexed;
    }
}
