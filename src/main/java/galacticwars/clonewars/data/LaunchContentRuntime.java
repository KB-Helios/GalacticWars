package galacticwars.clonewars.data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Dependency-light view of the last atomically accepted gameplay snapshot.
 * Runtime consumers never retain reload-owned mutable collections.
 */
public final class LaunchContentRuntime {
    private static volatile RuntimeSnapshot current = RuntimeSnapshot.empty();

    private LaunchContentRuntime() {
    }

    public static RuntimeSnapshot current() {
        return current;
    }

    public static void install(
            LaunchContentDefinitions definitions,
            List<String> factions,
            Map<String, List<String>> units
    ) {
        current = new RuntimeSnapshot(definitions, factions, units);
    }

    public record RuntimeSnapshot(
            LaunchContentDefinitions definitions,
            List<String> factions,
            Map<String, List<String>> units
    ) {
        public RuntimeSnapshot {
            Objects.requireNonNull(definitions, "definitions");
            factions = List.copyOf(factions);
            LinkedHashMap<String, List<String>> copiedUnits = new LinkedHashMap<>();
            units.forEach((faction, ids) -> copiedUnits.put(faction, List.copyOf(ids)));
            units = java.util.Collections.unmodifiableMap(copiedUnits);
        }

        static RuntimeSnapshot empty() {
            return new RuntimeSnapshot(LaunchContentDefinitions.empty(), List.of(), Map.of());
        }
    }
}
