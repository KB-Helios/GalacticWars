package galacticwars.clonewars.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import galacticwars.clonewars.GalacticWars;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

final class LaunchContentValidator {
    private static final Set<String> RUNTIME_UNLOCKS = Set.of(
            "faction_intro", "treasury", "recruitment", "workforce",
            "commander", "planet_travel", "vehicle_crafting", "advanced_trading",
            "vehicle_control", "veteran_trades", "supply_depot");

    private LaunchContentValidator() {
    }

    static LaunchContentDefinitions load(ResourceManager manager, Set<String> factionIds) throws IOException {
        Map<String, LaunchContentDefinitions.PlanetDefinition> planets = loadPlanets(manager, factionIds);
        Map<String, LaunchContentDefinitions.VehicleDefinition> vehicles = loadVehicles(manager);
        Map<String, LaunchContentDefinitions.ForceAbilityDefinition> forceAbilities = loadForceAbilities(manager);
        Map<String, LaunchContentDefinitions.QuestDefinition> quests = loadQuests(manager);
        Map<String, LaunchContentDefinitions.TradeDefinition> trades = loadTrades(manager, factionIds);
        Map<String, LaunchContentDefinitions.ConquestRegionDefinition> regions =
                loadRegions(manager, planets.keySet(), factionIds);
        requireCount("planets", planets, 4);
        requireCount("vehicles", vehicles, 5);
        requireCount("force abilities", forceAbilities, 6);
        requireCount("quests", quests, 15);
        requireCount("trades", trades, 5);
        requireCount("conquest regions", regions, 4);
        validateReferences(vehicles, forceAbilities, quests, trades);
        return new LaunchContentDefinitions(planets, vehicles, forceAbilities, quests, trades, regions);
    }

    static void validateReferences(
            Map<String, LaunchContentDefinitions.VehicleDefinition> vehicles,
            Map<String, LaunchContentDefinitions.ForceAbilityDefinition> forceAbilities,
            Map<String, LaunchContentDefinitions.QuestDefinition> quests,
            Map<String, LaunchContentDefinitions.TradeDefinition> trades
    ) {
        LinkedHashSet<String> knownUnlocks = new LinkedHashSet<>(RUNTIME_UNLOCKS);
        knownUnlocks.addAll(quests.keySet());
        quests.values().forEach(quest -> knownUnlocks.addAll(quest.unlocks()));
        for (LaunchContentDefinitions.ForceAbilityDefinition ability : forceAbilities.values()) {
            if (!quests.containsKey(ability.requiredQuest())) {
                throw new IllegalArgumentException("Force ability " + ability.id() + " references unknown quest");
            }
            if (!knownUnlocks.contains(ability.activeUnlock())) {
                throw new IllegalArgumentException("Force ability " + ability.id() + " references unknown unlock");
            }
        }
        for (LaunchContentDefinitions.VehicleDefinition vehicle : vehicles.values()) {
            if (!knownUnlocks.contains(vehicle.requiredUnlock())) {
                throw new IllegalArgumentException("Vehicle " + vehicle.id() + " references unknown unlock");
            }
            if (vehicle.deploymentRequirements().stream().anyMatch(requirement -> !knownUnlocks.contains(requirement))) {
                throw new IllegalArgumentException("Vehicle " + vehicle.id()
                        + " references unknown deployment requirement");
            }
        }
        for (LaunchContentDefinitions.TradeDefinition trade : trades.values()) {
            if (!knownUnlocks.contains(trade.requiredUnlock())) {
                throw new IllegalArgumentException("Trade " + trade.id() + " references unknown unlock");
            }
        }
    }

    private static Map<String, LaunchContentDefinitions.PlanetDefinition> loadPlanets(
            ResourceManager manager, Set<String> factions) throws IOException {
        LinkedHashMap<String, LaunchContentDefinitions.PlanetDefinition> result = new LinkedHashMap<>();
        for (JsonObject json : objects(manager, "planets", "planets")) {
            var definition = new LaunchContentDefinitions.PlanetDefinition(
                    string(json, "id"), string(json, "dimension"), string(json, "arrival"),
                    string(json, "theme"), string(json, "faction"));
            if (!factions.contains(definition.factionId())) throw new IllegalArgumentException("Unknown planet faction " + definition.factionId());
            put(result, definition.id(), definition, "planet");
        }
        return result;
    }

    private static Map<String, LaunchContentDefinitions.VehicleDefinition> loadVehicles(ResourceManager manager) throws IOException {
        LinkedHashMap<String, LaunchContentDefinitions.VehicleDefinition> result = new LinkedHashMap<>();
        for (JsonObject json : objects(manager, "vehicles", "vehicles")) {
            var definition = new LaunchContentDefinitions.VehicleDefinition(
                    string(json, "id"), string(json, "movement"), integer(json, "seats"),
                    integer(json, "max_health"), integer(json, "fuel_capacity"), string(json, "unlock"),
                    decimal(json, "max_speed"), decimal(json, "strafe_multiplier"),
                    decimal(json, "vertical_speed"), integer(json, "fuel_rate_ticks"),
                    string(json, "weapon_effect"), strings(json, "seat_roles"),
                    integer(json, "fabrication_credits"), integerMap(json, "fabrication_inputs"),
                    Set.copyOf(strings(json, "deployment_requirements")));
            put(result, definition.id(), definition, "vehicle");
        }
        return result;
    }

    private static Map<String, LaunchContentDefinitions.ForceAbilityDefinition> loadForceAbilities(ResourceManager manager) throws IOException {
        LinkedHashMap<String, LaunchContentDefinitions.ForceAbilityDefinition> result = new LinkedHashMap<>();
        for (JsonObject json : objects(manager, "force_abilities", "abilities")) {
            var definition = new LaunchContentDefinitions.ForceAbilityDefinition(
                    string(json, "id"), string(json, "path"), integer(json, "energy"),
                    integer(json, "cooldown_ticks"), string(json, "quest"), true,
                    string(json, "effect"), decimal(json, "range"),
                    integer(json, "duration_ticks"), string(json, "active_unlock"));
            put(result, definition.id(), definition, "Force ability");
        }
        return result;
    }

    private static Map<String, LaunchContentDefinitions.QuestDefinition> loadQuests(ResourceManager manager) throws IOException {
        LinkedHashMap<String, LaunchContentDefinitions.QuestDefinition> result = new LinkedHashMap<>();
        for (JsonObject json : objects(manager, "quests", "quests")) {
            var definition = new LaunchContentDefinitions.QuestDefinition(
                    string(json, "id"), strings(json, "objectives"), integer(json, "reward_credits"),
                    Set.copyOf(strings(json, "unlocks")));
            put(result, definition.id(), definition, "quest");
        }
        return result;
    }

    private static Map<String, LaunchContentDefinitions.TradeDefinition> loadTrades(
            ResourceManager manager, Set<String> factions) throws IOException {
        LinkedHashMap<String, LaunchContentDefinitions.TradeDefinition> result = new LinkedHashMap<>();
        for (JsonObject json : objects(manager, "trades", "trades")) {
            var definition = new LaunchContentDefinitions.TradeDefinition(
                    string(json, "id"), string(json, "faction"), integer(json, "cost"),
                    string(json, "item"), integer(json, "count"), string(json, "unlock"),
                    integer(json, "stock_tier"), optionalString(json, "regional_prerequisite"));
            if (!factions.contains(definition.factionId())) throw new IllegalArgumentException("Unknown trade faction " + definition.factionId());
            put(result, definition.id(), definition, "trade");
        }
        return result;
    }

    private static Map<String, LaunchContentDefinitions.ConquestRegionDefinition> loadRegions(
            ResourceManager manager, Set<String> planetIds, Set<String> factionIds) throws IOException {
        LinkedHashMap<String, LaunchContentDefinitions.ConquestRegionDefinition> result = new LinkedHashMap<>();
        for (JsonObject json : objects(manager, "conquest_regions", "regions")) {
            var definition = new LaunchContentDefinitions.ConquestRegionDefinition(
                    string(json, "id"), string(json, "planet"), integer(json, "protected_radius"),
                    integer(json, "capture_ticks"), integer(json, "reward_credits"),
                    integer(json, "landmark_x"), integer(json, "landmark_z"),
                    integer(json, "capture_radius"), string(json, "defender_faction"));
            if (!planetIds.contains(definition.planetId())) throw new IllegalArgumentException("Unknown region planet " + definition.planetId());
            if (!factionIds.contains(definition.defenderFaction())) throw new IllegalArgumentException("Unknown region defender faction " + definition.defenderFaction());
            put(result, definition.id(), definition, "conquest region");
        }
        return result;
    }

    private static List<JsonObject> objects(ResourceManager manager, String directory, String array) throws IOException {
        FileToIdConverter converter = FileToIdConverter.json("galacticwars/" + directory);
        Map<Identifier, Resource> resources = converter.listMatchingResourcesFromNamespace(manager, GalacticWars.MODID);
        if (resources.isEmpty()) throw new IllegalArgumentException("Missing launch content directory " + directory);
        java.util.ArrayList<JsonObject> result = new java.util.ArrayList<>();
        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                if (!root.has("schema_version") || root.get("schema_version").getAsInt() != 1) {
                    throw new IllegalArgumentException("Unsupported " + directory + " schema in " + entry.getKey());
                }
                JsonArray values = root.getAsJsonArray(array);
                if (values == null) throw new IllegalArgumentException("Missing " + array + " in " + entry.getKey());
                for (JsonElement value : values) result.add(value.getAsJsonObject());
            }
        }
        return List.copyOf(result);
    }

    private static String string(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).getAsString().isBlank()) throw new IllegalArgumentException("Missing " + key);
        return json.get(key).getAsString();
    }

    private static int integer(JsonObject json, String key) {
        if (!json.has(key)) throw new IllegalArgumentException("Missing " + key);
        return json.get(key).getAsInt();
    }

    private static double decimal(JsonObject json, String key) {
        if (!json.has(key)) throw new IllegalArgumentException("Missing " + key);
        return json.get(key).getAsDouble();
    }

    private static String optionalString(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsString().trim() : "";
    }

    private static List<String> strings(JsonObject json, String key) {
        JsonArray values = json.getAsJsonArray(key);
        if (values == null) throw new IllegalArgumentException("Missing " + key);
        java.util.ArrayList<String> result = new java.util.ArrayList<>();
        values.forEach(value -> result.add(value.getAsString()));
        return List.copyOf(result);
    }

    private static Map<String, Integer> integerMap(JsonObject json, String key) {
        JsonObject values = json.getAsJsonObject(key);
        if (values == null) throw new IllegalArgumentException("Missing " + key);
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        values.entrySet().forEach(entry -> result.put(entry.getKey(), entry.getValue().getAsInt()));
        return Map.copyOf(result);
    }

    private static <V> void put(Map<String, V> target, String id, V value, String label) {
        if (target.putIfAbsent(id, value) != null) throw new IllegalArgumentException("Duplicate " + label + " " + id);
    }

    private static void requireCount(String label, Map<?, ?> values, int expected) {
        if (values.size() != expected) throw new IllegalArgumentException(label + " expected " + expected + " entries but found " + values.size());
    }
}
