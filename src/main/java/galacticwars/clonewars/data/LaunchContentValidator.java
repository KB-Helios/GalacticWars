package galacticwars.clonewars.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.progression.LaunchContentCatalog;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class LaunchContentValidator {
    private LaunchContentValidator() {
    }

    static void validate(ResourceManager manager) throws IOException {
        assertIds(manager, "planets", "planets", Set.copyOf(LaunchContentCatalog.PLANETS));
        assertIds(manager, "vehicles", "vehicles", Set.copyOf(LaunchContentCatalog.VEHICLES));
        assertIds(manager, "force_abilities", "abilities", LaunchContentCatalog.FORCE_ABILITIES);
        assertIds(manager, "quests", "quests", Set.copyOf(LaunchContentCatalog.QUESTS));
        assertQuestUnlocks(manager);
        assertCount(manager, "trades", "trades", 5);
        assertCount(manager, "conquest_regions", "regions", 4);
    }

    private static void assertQuestUnlocks(ResourceManager manager) throws IOException {
        FileToIdConverter converter = FileToIdConverter.json("galacticwars/quests");
        Map<String, Set<String>> actual = new HashMap<>();
        for (Map.Entry<Identifier, Resource> entry
                : converter.listMatchingResourcesFromNamespace(manager, GalacticWars.MODID).entrySet()) {
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray quests = json.getAsJsonArray("quests");
                if (quests == null) {
                    throw new IllegalArgumentException("Missing quests in " + entry.getKey());
                }
                for (JsonElement element : quests) {
                    JsonObject quest = element.getAsJsonObject();
                    String id = quest.get("id").getAsString();
                    JsonArray unlocksJson = quest.getAsJsonArray("unlocks");
                    if (unlocksJson == null) {
                        throw new IllegalArgumentException("Quest " + id + " is missing unlocks");
                    }
                    HashSet<String> unlocks = new HashSet<>();
                    unlocksJson.forEach(unlock -> unlocks.add(unlock.getAsString()));
                    if (actual.putIfAbsent(id, Set.copyOf(unlocks)) != null) {
                        throw new IllegalArgumentException("Duplicate quest id " + id);
                    }
                }
            }
        }
        if (!actual.equals(LaunchContentCatalog.QUEST_UNLOCKS)) {
            throw new IllegalArgumentException("Quest unlocks do not match launch data contract; expected "
                    + LaunchContentCatalog.QUEST_UNLOCKS + " but found " + actual);
        }
    }

    private static void assertIds(
            ResourceManager manager,
            String directory,
            String arrayName,
            Set<String> expected
    ) throws IOException {
        Set<String> actual = ids(manager, directory, arrayName);
        if (!actual.equals(expected)) {
            throw new IllegalArgumentException(directory + " ids do not match launch contract; expected "
                    + expected + " but found " + actual);
        }
    }

    private static void assertCount(ResourceManager manager, String directory, String arrayName, int expected)
            throws IOException {
        int actual = ids(manager, directory, arrayName).size();
        if (actual != expected) {
            throw new IllegalArgumentException(directory + " expected " + expected + " entries but found " + actual);
        }
    }

    private static Set<String> ids(ResourceManager manager, String directory, String arrayName) throws IOException {
        FileToIdConverter converter = FileToIdConverter.json("galacticwars/" + directory);
        HashSet<String> ids = new HashSet<>();
        Map<Identifier, Resource> resources = converter.listMatchingResourcesFromNamespace(manager, GalacticWars.MODID);
        if (resources.isEmpty()) {
            throw new IllegalArgumentException("Missing launch content directory " + directory);
        }
        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                int schema = json.has("schema_version") ? json.get("schema_version").getAsInt() : 0;
                if (schema != 1) {
                    throw new IllegalArgumentException("Unsupported " + directory + " schema " + schema
                            + " in " + entry.getKey());
                }
                JsonArray definitions = json.getAsJsonArray(arrayName);
                if (definitions == null) {
                    throw new IllegalArgumentException("Missing " + arrayName + " in " + entry.getKey());
                }
                for (JsonElement element : definitions) {
                    String id = element.getAsJsonObject().get("id").getAsString();
                    if (!ids.add(id)) {
                        throw new IllegalArgumentException("Duplicate " + directory + " id " + id);
                    }
                }
            }
        }
        return Set.copyOf(ids);
    }
}
