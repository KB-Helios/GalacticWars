package galacticwars.clonewars.integration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GameplayDataRoadmapTest {
    private static final Path FACTIONS = Path.of(
            "src/main/resources/data/galacticwars/galacticwars/factions");
    private static final Path UNITS = Path.of(
            "src/main/resources/data/galacticwars/galacticwars/units");

    private GameplayDataRoadmapTest() {
    }

    public static void main(String[] args) throws Exception {
        factionsAreReciprocalAndPledgesAreDataDriven();
        unitsOwnTheirRuntimeAttributesCostsAndEquipment();
        reloadIsAtomicAndLegacyMandalorianIdIsAliased();
        System.out.println("GameplayDataRoadmapTest passed");
    }

    private static void factionsAreReciprocalAndPledgesAreDataDriven() throws Exception {
        Map<String, String> factions = new HashMap<>();
        try (var paths = Files.list(FACTIONS)) {
            for (Path path : paths.filter(value -> value.toString().endsWith(".json")).toList()) {
                String faction = Files.readString(path);
                factions.put(stringValue(faction, "id"), faction);
                assertTrue(intValue(faction, "direct_delta")
                                >= intValue(faction, "minimum_hiring_alignment"),
                        "pledge must unlock direct hiring for " + path.getFileName());
                assertEquals(2, intValue(faction, "ally_delta"), "ally pledge delta");
                assertEquals(-5, intValue(faction, "enemy_delta"), "enemy pledge delta");
                assertTrue(faction.contains("\"selection_order\""), "selection order");
            }
        }
        assertEquals(5, factions.size(), "faction count");
        for (Map.Entry<String, String> entry : factions.entrySet()) {
            reciprocal(entry.getKey(), arrayValues(entry.getValue(), "allies"), "allies", factions);
            reciprocal(entry.getKey(), arrayValues(entry.getValue(), "enemies"), "enemies", factions);
            Set<String> allies = arrayValues(entry.getValue(), "allies");
            Set<String> enemies = arrayValues(entry.getValue(), "enemies");
            assertTrue(allies.stream().noneMatch(enemies::contains), "contradictory relation");
        }
        Set<String> separatistEnemies = arrayValues(factions.get("galacticwars:separatist"), "enemies");
        assertTrue(separatistEnemies.contains("galacticwars:hutt_cartel"), "Separatist hutt_cartel hostility");
        assertTrue(separatistEnemies.contains("galacticwars:nightsister"), "Separatist nightsister hostility");
    }

    private static void unitsOwnTheirRuntimeAttributesCostsAndEquipment() throws Exception {
        int unitCount = 0;
        try (var paths = Files.list(UNITS)) {
            for (Path path : paths.filter(value -> value.toString().endsWith(".json")).toList()) {
                unitCount++;
                String unit = Files.readString(path);
                assertTrue(unit.contains("\"entity_type\""), path + " entity type");
                assertTrue(unit.contains("\"hire_cost\""), path + " cost precedence");
                for (String key : Set.of("max_health", "attack_damage", "movement_speed", "follow_range", "armor")) {
                    assertTrue(unit.contains("\"" + key + "\""), path + " attribute " + key);
                }
                for (String key : Set.of("main_hand", "head", "chest", "legs", "feet")) {
                    assertTrue(unit.contains("\"" + key + "\""), path + " equipment " + key);
                }
            }
        }
        assertEquals(21, unitCount, "launch unit count");
        assertTrue(Files.exists(UNITS.resolve("mandalorian_warrior.json")), "Mandalorian warrior definition");
        assertTrue(Files.notExists(UNITS.resolve("mandalorian_rider.json")), "legacy rider definition removed");
    }

    private static void reloadIsAtomicAndLegacyMandalorianIdIsAliased() throws Exception {
        String manager = Files.readString(Path.of(
                "src/main/java/galacticwars/clonewars/data/GameplayDataManager.java"));
        String runtime = Files.readString(Path.of(
                "src/main/kotlin/galacticwars/clonewars/data/LaunchContentRuntime.kt"));
        assertContains(manager, "LaunchContentRuntime.installAccepted(",
                "reload delegates to the shared atomic content publication");
        assertContains(runtime, "@Volatile", "runtime publication is visible across threads");
        assertContains(runtime, "Publishes every production view of an accepted datapack reload",
                "full runtime snapshot is swapped atomically");
        assertContains(manager, "retaining the previous valid snapshot", "last-good reload fallback logging");
        assertContains(manager, "mandalorian_rider", "legacy Mandalorian alias");
        assertContains(manager, "mandalorian_warrior", "Mandalorian alias target");
        assertContains(manager, "integer(resource.json(), \"schema_version\", -1)",
                "missing schema versions are rejected");
        assertContains(manager, "pledge grants", "pledge-to-hire deadlocks are rejected");
    }

    private static void reciprocal(String own, Set<String> relations, String field, Map<String, String> factions) {
        for (String related : relations) {
            String target = factions.get(related);
            assertTrue(target != null && arrayValues(target, field).contains(own),
                    field + " reciprocal " + own + " -> " + related);
        }
    }

    private static Set<String> arrayValues(String json, String key) {
        HashSet<String> values = new HashSet<>();
        Matcher array = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL)
                .matcher(json);
        if (!array.find()) return Set.of();
        Matcher strings = Pattern.compile("\\\"([^\\\"]+)\\\"").matcher(array.group(1));
        while (strings.find()) values.add(strings.group(1));
        return Set.copyOf(values);
    }

    private static String stringValue(String json, String key) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
                .matcher(json);
        if (!matcher.find()) throw new AssertionError("missing string " + key);
        return matcher.group(1);
    }

    private static int intValue(String json, String key) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(-?\\d+)").matcher(json);
        if (!matcher.find()) throw new AssertionError("missing integer " + key);
        return Integer.parseInt(matcher.group(1));
    }

    private static void assertContains(String value, String expected, String label) {
        assertTrue(value.contains(expected), label);
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) throw new AssertionError(label + " expected " + expected + " but was " + actual);
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }
}
