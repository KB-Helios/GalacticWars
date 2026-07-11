package galacticwars.clonewars.faction;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record FactionCatalog(Map<FactionId, FactionDefinition> definitions) {
    public FactionCatalog {
        Objects.requireNonNull(definitions, "definitions");
        definitions = Collections.unmodifiableMap(new LinkedHashMap<>(definitions));
    }

    public Optional<FactionDefinition> definition(FactionId factionId) {
        return Optional.ofNullable(definitions.get(Objects.requireNonNull(factionId, "factionId")));
    }

    public boolean contains(FactionId factionId) {
        return definitions.containsKey(Objects.requireNonNull(factionId, "factionId"));
    }

    public FactionRelation relation(FactionId first, FactionId second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        if (first.equals(second)) {
            return FactionRelation.SAME;
        }

        FactionDefinition definition = definitions.get(first);
        if (definition == null) {
            return FactionRelation.NEUTRAL;
        }
        if (definition.allies().contains(second)) {
            return FactionRelation.ALLY;
        }
        if (definition.enemies().contains(second)) {
            return FactionRelation.ENEMY;
        }
        return FactionRelation.NEUTRAL;
    }
}
