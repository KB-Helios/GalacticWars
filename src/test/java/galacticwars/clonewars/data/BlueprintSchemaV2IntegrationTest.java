package galacticwars.clonewars.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import galacticwars.clonewars.settlement.KingdomBaseBlueprint;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;

public final class BlueprintSchemaV2IntegrationTest {
    private static final Path BLUEPRINTS = Path.of(
            "src/main/resources/data/galacticwars/galacticwars/blueprints");
    private static final Path STRUCTURES = Path.of(
            "src/main/resources/data/galacticwars/structure");
    private static final Set<String> CONSTRUCTION = Set.of(
            "barracks", "forward_base", "mine", "moisture_farm", "salvage_yard", "starter_camp", "supply_depot");
    private static final Set<String> SITES = Set.of(
            "republic_field_base", "separatist_relay_outpost", "mandalorian_mountain_camp",
            "hutt_salvage_depot", "nightsister_enclave");
    private static final Map<String, Map<String, Integer>> COSTS = Map.of(
            "barracks", Map.of("minecraft:oak_planks", 11, "minecraft:oak_log", 8),
            "forward_base", Map.of("galacticwars:duracrete", 25, "galacticwars:nightsister_weave_log", 4,
                    "minecraft:oak_planks", 4),
            "mine", Map.of("galacticwars:duracrete", 9),
            "moisture_farm", Map.of("minecraft:dirt", 25, "minecraft:oak_log", 10),
            "salvage_yard", Map.of("minecraft:oak_planks", 9, "minecraft:oak_log", 3),
            "starter_camp", Map.of("minecraft:oak_log", 8, "minecraft:oak_planks", 5,
                    "minecraft:campfire", 1, "minecraft:crafting_table", 1, "minecraft:chest", 1),
            "supply_depot", Map.of("galacticwars:duracrete", 12, "minecraft:oak_log", 8,
                    "minecraft:oak_planks", 2, "minecraft:chest", 2));

    private BlueprintSchemaV2IntegrationTest() {
    }

    public static void main(String[] args) throws Exception {
        schemaV1RemainsAccepted();
        constructionTemplatesPreserveExactCosts();
        factionTemplatesContainRequiredMarkers();
        crossLoaderResourcesAreComplete();
        System.out.println("BlueprintSchemaV2IntegrationTest passed");
    }

    private static void schemaV1RemainsAccepted() {
        JsonObject json = JsonParser.parseString("""
                {"schema_version":1,"id":"galacticwars:fixture","display_name":"Fixture",
                 "anchor":{"x":0,"y":0,"z":0},"allowed_rotations":[0],
                 "placements":[{"x":0,"y":0,"z":0,"block":"minecraft:stone","item":"minecraft:stone"}],
                 "rewards":{}}
                """).getAsJsonObject();
        KingdomBaseBlueprint parsed = GameplayDataManager.parseBlueprint(
                Identifier.parse("galacticwars:fixture"), json);
        require(parsed.id().equals("galacticwars:fixture") && parsed.placements().size() == 1,
                "schema v1 compatibility failed");
    }

    private static void constructionTemplatesPreserveExactCosts() throws Exception {
        for (String id : CONSTRUCTION) {
            JsonObject descriptor = json(BLUEPRINTS.resolve(id + ".json"));
            require(descriptor.get("schema_version").getAsInt() == 2, id + " was not migrated to v2");
            require(descriptor.getAsJsonArray("modes").toString().contains("construction"),
                    id + " is not construction-enabled");
            JsonObject costs = descriptor.getAsJsonObject("construction").getAsJsonObject("costs");
            Map<String, Integer> expected = COSTS.get(id);
            require(costs.size() == expected.size(), id + " cost key count changed");
            expected.forEach((item, count) -> require(costs.get(item).getAsInt() == count,
                    id + " cost changed for " + item));
            CompoundTag template = nbt(STRUCTURES.resolve("construction").resolve(id + ".nbt"));
            require(!template.getListOrEmpty("palette").isEmpty(), id + " has no NBT palette");
            require(!template.getListOrEmpty("blocks").isEmpty(), id + " has no NBT blocks");
            require(template.getListOrEmpty("entities").isEmpty(), id + " construction template contains entities");
        }
    }

    private static void factionTemplatesContainRequiredMarkers() throws Exception {
        for (String id : SITES) {
            JsonObject descriptor = json(BLUEPRINTS.resolve(id + ".json"));
            JsonObject worldgen = descriptor.getAsJsonObject("worldgen");
            require(worldgen.getAsJsonArray("biomes").size() == 3, id + " biome routing changed");
            int cap = 0;
            for (var entry : worldgen.getAsJsonArray("roster")) {
                cap += entry.getAsJsonObject().get("maximum").getAsInt();
            }
            require(cap > 0 && cap <= 32, id + " roster cap is unsafe");
            CompoundTag template = nbt(STRUCTURES.resolve("sites").resolve(id + ".nbt"));
            ListTag palette = template.getListOrEmpty("palette");
            ListTag blocks = template.getListOrEmpty("blocks");
            int anchors = 0;
            int loot = 0;
            for (int index = 0; index < blocks.size(); index++) {
                CompoundTag block = blocks.getCompoundOrEmpty(index);
                int stateIndex = block.getIntOr("state", -1);
                if (stateIndex >= 0 && palette.getCompoundOrEmpty(stateIndex)
                        .getStringOr("Name", "").equals("minecraft:structure_block")) {
                    String marker = block.getCompoundOrEmpty("nbt").getStringOr("metadata", "");
                    anchors += marker.equals("site_anchor") ? 1 : 0;
                    loot += marker.startsWith("loot:") ? 1 : 0;
                }
            }
            require(anchors == 1, id + " must contain exactly one site anchor");
            require(loot == worldgen.getAsJsonArray("loot_markers").size(), id + " loot markers do not match");
        }
    }

    private static void crossLoaderResourcesAreComplete() throws Exception {
        require(Files.readString(Path.of("fabric/src/main/kotlin/galacticwars/clonewars/fabric/FabricWorldgenFeatures.kt"))
                .contains("beskar_ore"), "Fabric Beskar feature missing");
        String fabricBootstrap = Files.readString(Path.of(
                "fabric/src/main/kotlin/galacticwars/clonewars/fabric/GalacticWarsFabric.kt"));
        require(!fabricBootstrap.contains("FabricBiomeSpawns"), "Fabric free NPC spawns remain enabled");
        try (var files = Files.walk(Path.of("src/main/resources/data/galacticwars/neoforge/biome_modifier"))) {
            require(files.filter(Files::isRegularFile).noneMatch(path -> {
                try {
                    return Files.readString(path).contains("add_spawns");
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            }), "NeoForge free NPC spawn modifier remains enabled");
        }
        require(Files.isRegularFile(Path.of(
                "src/main/resources/data/galacticwars/worldgen/placed_feature/beskar_ore.json")),
                "Beskar placed feature missing");
        require(Files.isRegularFile(Path.of(
                "src/main/resources/data/galacticwars/worldgen/placed_feature/nightsister_weave_grove.json")),
                "Nightsister grove placed feature missing");
    }

    private static JsonObject json(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    private static CompoundTag nbt(Path path) throws Exception {
        try (InputStream input = Files.newInputStream(path)) {
            return NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
