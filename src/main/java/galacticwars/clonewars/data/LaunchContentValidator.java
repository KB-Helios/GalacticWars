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
        assertIds(manager, "trades", "trades", LaunchContentCatalog.TRADES.keySet());
        assertQuestContracts(manager);
        assertTradeContracts(manager);
        assertCount(manager, "conquest_regions", "regions", 4);
        assertRegionRewards(manager);
    }

    private static void assertQuestContracts(ResourceManager manager) throws IOException {
        FileToIdConverter converter = FileToIdConverter.json("galacticwars/quests");
        Map<String, Set<String>> actualUnlocks = new HashMap<>();
        Map<String, java.util.List<String>> actualObjectives = new HashMap<>();
        Map<String, Integer> actualRewards = new HashMap<>();
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
                    JsonArray objectivesJson = quest.getAsJsonArray("objectives");
                    if (unlocksJson == null || objectivesJson == null || !quest.has("reward_credits")) {
                        throw new IllegalArgumentException("Quest " + id + " has an incomplete progression contract");
                    }
                    HashSet<String> unlocks = new HashSet<>();
                    unlocksJson.forEach(unlock -> unlocks.add(unlock.getAsString()));
                    java.util.ArrayList<String> objectives = new java.util.ArrayList<>();
                    objectivesJson.forEach(objective -> objectives.add(objective.getAsString()));
                    if (actualUnlocks.putIfAbsent(id, Set.copyOf(unlocks)) != null) {
                        throw new IllegalArgumentException("Duplicate quest id " + id);
                    }
                    actualObjectives.put(id, java.util.List.copyOf(objectives));
                    actualRewards.put(id, quest.get("reward_credits").getAsInt());
                }
            }
        }
        if (!actualUnlocks.equals(LaunchContentCatalog.QUEST_UNLOCKS)
                || !actualObjectives.equals(LaunchContentCatalog.QUEST_OBJECTIVES)
                || !actualRewards.equals(LaunchContentCatalog.QUEST_REWARD_CREDITS)) {
            throw new IllegalArgumentException("Quest unlocks do not match launch data contract; expected "
                    + LaunchContentCatalog.QUEST_UNLOCKS + " but found " + actualUnlocks);
        }
    }

    private static void assertRegionRewards(ResourceManager manager) throws IOException {
        FileToIdConverter converter = FileToIdConverter.json("galacticwars/conquest_regions");
        Map<String, Integer> actual = new HashMap<>();
        for (Map.Entry<Identifier, Resource> entry
                : converter.listMatchingResourcesFromNamespace(manager, GalacticWars.MODID).entrySet()) {
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                JsonArray regions = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("regions");
                for (JsonElement element : regions) {
                    JsonObject region = element.getAsJsonObject();
                    String id = region.get("id").getAsString();
                    if (!region.has("reward_credits")
                            || actual.putIfAbsent(id, region.get("reward_credits").getAsInt()) != null) {
                        throw new IllegalArgumentException("Invalid conquest reward contract for " + id);
                    }
                }
            }
        }
        if (!actual.equals(LaunchContentCatalog.REGION_REWARD_CREDITS)) {
            throw new IllegalArgumentException("Conquest rewards do not match launch data contract");
        }
    }

    private static void assertTradeContracts(ResourceManager manager) throws IOException {
        FileToIdConverter converter = FileToIdConverter.json("galacticwars/trades");
        Map<String, LaunchContentCatalog.TradeDefinition> actual = new HashMap<>();
        for (Map.Entry<Identifier, Resource> entry
                : converter.listMatchingResourcesFromNamespace(manager, GalacticWars.MODID).entrySet()) {
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                JsonArray trades = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("trades");
                if (trades == null) {
                    throw new IllegalArgumentException("Missing trades in " + entry.getKey());
                }
                for (JsonElement element : trades) {
                    JsonObject trade = element.getAsJsonObject();
                    String id = trade.get("id").getAsString();
                    LaunchContentCatalog.TradeDefinition definition = new LaunchContentCatalog.TradeDefinition(
                            trade.get("faction").getAsString(),
                            trade.get("cost").getAsInt(),
                            trade.get("item").getAsString(),
                            trade.get("count").getAsInt(),
                            trade.get("unlock").getAsString());
                    if (actual.putIfAbsent(id, definition) != null) {
                        throw new IllegalArgumentException("Duplicate trade id " + id);
                    }
                }
            }
        }
        if (!actual.equals(LaunchContentCatalog.TRADES)) {
            throw new IllegalArgumentException("Trade definitions do not match launch data contract");
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
