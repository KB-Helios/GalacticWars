package galacticwars.clonewars.progression;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class LaunchDataIntegrityTest {
    private static final Path ROOT = Path.of("src/main/resources/data/galacticwars");
    private static final Path GAMEPLAY = ROOT.resolve("galacticwars");
    private static final Set<String> EXPECTED_QUEST_IDS = Set.of(
            "republic_chapter_1", "republic_chapter_2", "republic_chapter_3",
            "republic_force_training_1", "republic_force_training_2", "republic_force_training_3",
            "separatist_chapter_1", "separatist_chapter_2", "separatist_chapter_3",
            "separatist_force_training_1", "separatist_force_training_2", "separatist_force_training_3",
            "mandalorian_chapter_1", "mandalorian_chapter_2", "mandalorian_chapter_3",
            "hutt_cartel_chapter_1", "hutt_cartel_chapter_2", "hutt_cartel_chapter_3",
            "nightsister_chapter_1", "nightsister_chapter_2", "nightsister_chapter_3",
            "nightsister_force_training_1", "nightsister_force_training_2", "nightsister_force_training_3");
    private static final Map<String, Set<String>> EXPECTED_QUEST_UNLOCKS = Map.ofEntries(
            Map.entry("republic_chapter_1", Set.of("workforce")),
            Map.entry("republic_chapter_2", Set.of("barc_speeder")),
            Map.entry("republic_chapter_3", Set.of("conquest", "vehicle_mastery")),
            Map.entry("republic_force_training_1", Set.of()),
            Map.entry("republic_force_training_2", Set.of()),
            Map.entry("republic_force_training_3", Set.of()),
            Map.entry("separatist_chapter_1", Set.of("workforce")),
            Map.entry("separatist_chapter_2", Set.of("stap")),
            Map.entry("separatist_chapter_3", Set.of("conquest", "vehicle_mastery")),
            Map.entry("separatist_force_training_1", Set.of()),
            Map.entry("separatist_force_training_2", Set.of()),
            Map.entry("separatist_force_training_3", Set.of()),
            Map.entry("mandalorian_chapter_1", Set.of("workforce")),
            Map.entry("mandalorian_chapter_2", Set.of("vehicle_crafting")),
            Map.entry("mandalorian_chapter_3", Set.of("conquest", "vehicle_mastery")),
            Map.entry("hutt_cartel_chapter_1", Set.of("trading")),
            Map.entry("hutt_cartel_chapter_2", Set.of("vehicle_crafting")),
            Map.entry("hutt_cartel_chapter_3", Set.of("conquest", "vehicle_mastery")),
            Map.entry("nightsister_chapter_1", Set.of("workforce")),
            Map.entry("nightsister_chapter_2", Set.of()),
            Map.entry("nightsister_chapter_3", Set.of("conquest", "vehicle_mastery")),
            Map.entry("nightsister_force_training_1", Set.of()),
            Map.entry("nightsister_force_training_2", Set.of()),
            Map.entry("nightsister_force_training_3", Set.of()));

    public static void main(String[] args) throws Exception {
        assertJsonCount(GAMEPLAY.resolve("factions"), 5, "factions");
        assertJsonCount(GAMEPLAY.resolve("units"), 21, "units");
        for (String category : Set.of("planets", "vehicles", "force_abilities", "force_progression",
                "quests", "trades", "conquest_regions")) {
            assertTrue(Files.isRegularFile(GAMEPLAY.resolve(category).resolve("launch.json")), category + " launch data");
        }
        String planets = Files.readString(GAMEPLAY.resolve("planets/launch.json"));
        Set<String> planetIds = ids(objects(planets, "planets"));
        assertTrue(planetIds.size() == 4, "planet count");
        for (String planet : planetIds) {
            assertTrue(Files.isRegularFile(ROOT.resolve("dimension").resolve(planet + ".json")), planet + " dimension");
        }
        assertTrue(Files.isRegularFile(ROOT.resolve("dimension_type/planet.json")), "planet dimension type");
        String questJson = Files.readString(GAMEPLAY.resolve("quests/launch.json"));
        List<String> quests = objects(questJson, "quests");
        Set<String> questIds = ids(quests);
        assertTrue(questIds.equals(EXPECTED_QUEST_IDS), "launch quest ids");
        Map<String, Set<String>> declaredUnlocks = new HashMap<>();
        for (String quest : quests) {
            String questId = string(quest, "id");
            List<String> objectives = strings(quest, "objectives");
            assertTrue(!objectives.contains("force_ability_unlocked"),
                    questId + " cannot require a pre-unlock Force activation");
            List<String> unlockList = strings(quest, "unlocks");
            Set<String> unlocks = Set.copyOf(unlockList);
            assertTrue(unlocks.size() == unlockList.size(), questId + " duplicate unlock");
            assertTrue(declaredUnlocks.put(questId, unlocks) == null, questId + " duplicate declaration");
        }
        assertTrue(quests.stream().filter(quest -> string(quest, "id").contains("_chapter_"))
                        .noneMatch(quest -> quest.contains("\"event\":\"force_ability_used\"")),
                "main faction campaigns remain independent of optional Force training");
        List<String> forceTraining = quests.stream()
                .filter(quest -> string(quest, "id").contains("_force_training_")).toList();
        assertTrue(forceTraining.size() == 9
                        && forceTraining.stream().filter(quest -> quest.contains("\"mastery_experience\":25")).count() == 3
                        && forceTraining.stream().filter(quest -> quest.contains("\"mastery_experience\":50")).count() == 3
                        && forceTraining.stream().filter(quest -> quest.contains("\"mastery_experience\":75")).count() == 3,
                "each tradition has optional 25, 50, and 75 XP training quests");
        assertTrue(declaredUnlocks.equals(EXPECTED_QUEST_UNLOCKS), "quest unlock contents");
        List<String> vehicles = objects(Files.readString(GAMEPLAY.resolve("vehicles/launch.json")), "vehicles");
        assertTrue(vehicles.stream().anyMatch(vehicle -> string(vehicle, "unlock").equals("vehicle_crafting")
                        && strings(vehicle, "deployment_requirements").contains("vehicle_crafting")
                        && strings(vehicle, "deployment_requirements").contains("supply_depot")),
                "every faction can fabricate at least one vehicle before chapter 3");
        System.out.println("LaunchDataIntegrityTest passed");
    }

    private static void assertJsonCount(Path directory, long expected, String label) throws Exception {
        try (Stream<Path> files = Files.list(directory)) {
            assertTrue(files.filter(path -> path.toString().endsWith(".json")).count() == expected, label);
        }
    }

    private static Set<String> ids(List<String> values) {
        HashSet<String> ids = new HashSet<>();
        for (String value : values) {
            assertTrue(ids.add(string(value, "id")), "duplicate id");
        }
        return Set.copyOf(ids);
    }

    private static String string(String json, String key) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key)
                + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(json);
        if (!matcher.find()) throw new AssertionError("missing string " + key);
        return matcher.group(1);
    }

    private static List<String> strings(String json, String key) {
        Matcher array = Pattern.compile("\\\"" + Pattern.quote(key)
                + "\\\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL).matcher(json);
        if (!array.find()) throw new AssertionError("missing array " + key);
        ArrayList<String> values = new ArrayList<>();
        Matcher strings = Pattern.compile("\\\"([^\\\"]+)\\\"").matcher(array.group(1));
        while (strings.find()) values.add(strings.group(1));
        return List.copyOf(values);
    }

    private static List<String> objects(String json, String arrayKey) {
        int keyIndex = json.indexOf("\"" + arrayKey + "\"");
        int arrayStart = keyIndex < 0 ? -1 : json.indexOf('[', keyIndex);
        if (arrayStart < 0) throw new AssertionError("missing array " + arrayKey);
        ArrayList<String> objects = new ArrayList<>();
        boolean inString = false;
        boolean escaped = false;
        int depth = 0;
        int objectStart = -1;
        for (int index = arrayStart + 1; index < json.length(); index++) {
            char current = json.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }
            if (current == '"') {
                inString = true;
            } else if (current == '{') {
                if (depth++ == 0) objectStart = index;
            } else if (current == '}') {
                if (--depth == 0 && objectStart >= 0) {
                    objects.add(json.substring(objectStart, index + 1));
                    objectStart = -1;
                }
            } else if (current == ']' && depth == 0) {
                break;
            }
        }
        return List.copyOf(objects);
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }
}
