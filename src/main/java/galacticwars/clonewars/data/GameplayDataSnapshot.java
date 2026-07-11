package galacticwars.clonewars.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import galacticwars.clonewars.army.ArmyUnitCatalog;
import galacticwars.clonewars.army.ArmyUnitDefinition;
import galacticwars.clonewars.army.ArmyUnitId;
import galacticwars.clonewars.faction.FactionCatalog;
import galacticwars.clonewars.faction.FactionDefinition;
import galacticwars.clonewars.faction.FactionId;
import galacticwars.clonewars.settlement.KingdomBaseBlueprint;

public record GameplayDataSnapshot(
        FactionCatalog factions,
        ArmyUnitCatalog units,
        Map<String, ArmyUnitId> unitIdsByEntityType,
        Map<String, ArmyUnitId> unitAliases,
        Map<String, KingdomBaseBlueprint> blueprints
) {
    public GameplayDataSnapshot {
        Objects.requireNonNull(factions, "factions");
        Objects.requireNonNull(units, "units");
        unitIdsByEntityType = immutableMap(unitIdsByEntityType, "unitIdsByEntityType");
        unitAliases = immutableMap(unitAliases, "unitAliases");
        Objects.requireNonNull(blueprints, "blueprints");
        LinkedHashMap<String, KingdomBaseBlueprint> normalizedBlueprints = new LinkedHashMap<>();
        for (Map.Entry<String, KingdomBaseBlueprint> entry : blueprints.entrySet()) {
            KingdomBaseBlueprint blueprint = Objects.requireNonNull(entry.getValue(), "blueprint");
            String key = normalizeBlueprintId(entry.getKey());
            if (!key.equals(blueprint.id())) {
                throw new IllegalArgumentException("Blueprint map key " + entry.getKey()
                        + " does not match definition id " + blueprint.id());
            }
            if (normalizedBlueprints.putIfAbsent(key, blueprint) != null) {
                throw new IllegalArgumentException("Duplicate blueprint id " + key);
            }
        }
        blueprints = Collections.unmodifiableMap(normalizedBlueprints);
    }

    public Optional<FactionDefinition> faction(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return factions.definition(FactionId.of(id));
    }

    public Optional<ArmyUnitDefinition> unit(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        ArmyUnitId requested = ArmyUnitId.of(id);
        ArmyUnitId resolved = unitAliases.getOrDefault(requested.toString(), requested);
        return units.definition(resolved);
    }

    public Optional<ArmyUnitDefinition> unitForEntityType(String entityTypeId) {
        ArmyUnitId id = unitIdsByEntityType.get(entityTypeId);
        return id == null ? Optional.empty() : units.definition(id);
    }

    public Optional<KingdomBaseBlueprint> blueprint(String id) {
        return Optional.ofNullable(blueprints.get(normalizeBlueprintId(id)));
    }

    public List<FactionDefinition> selectableFactions() {
        ArrayList<FactionDefinition> ordered = new ArrayList<>(factions.definitions().values());
        ordered.sort(Comparator.comparingInt(FactionDefinition::selectionOrder)
                .thenComparing(definition -> definition.id().toString()));
        return List.copyOf(ordered);
    }

    public static String normalizeBlueprintId(String id) {
        if (id == null || id.isBlank()) {
            return "";
        }
        return KingdomBaseBlueprint.canonicalId(id);
    }

    private static <K, V> Map<K, V> immutableMap(Map<K, V> source, String label) {
        Objects.requireNonNull(source, label);
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
