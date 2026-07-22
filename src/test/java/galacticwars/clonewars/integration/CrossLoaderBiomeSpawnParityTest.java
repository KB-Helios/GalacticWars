package galacticwars.clonewars.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class CrossLoaderBiomeSpawnParityTest {
    private static final Path NEOFORGE_MODIFIERS = Path.of(
            "src/main/resources/data/galacticwars/neoforge/biome_modifier");
    private static final Path FABRIC_SPAWNS = Path.of(
            "fabric/src/main/kotlin/galacticwars/clonewars/fabric/FabricBiomeSpawns.kt");
    private static final Pattern STRING = Pattern.compile("\"([^\"]+)\"");
    private static final Pattern FABRIC_ENTRY = Pattern.compile(
            "addSpawn\\(ModEntityTypes\\.([A-Z0-9_]+)\\.get\\(\\),\\s*"
                    + "(\\d+),\\s*(\\d+),\\s*(\\d+),\\s*([^)]*)\\)",
            Pattern.DOTALL);

    private CrossLoaderBiomeSpawnParityTest() {
    }

    public static void main(String[] args) throws IOException {
        Map<String, SpawnEntry> neoForge = readNeoForgeEntries();
        Map<String, SpawnEntry> fabric = readFabricEntries();
        if (neoForge.size() != 20) {
            throw new AssertionError("Expected 20 NeoForge Overworld spawn mappings, found "
                    + neoForge.keySet());
        }
        if (!fabric.equals(neoForge)) {
            throw new AssertionError("Fabric biome spawn mappings differ from NeoForge. Expected <"
                    + neoForge + "> but were <" + fabric + ">");
        }

        String fabricEntrypoint = Files.readString(Path.of(
                "fabric/src/main/kotlin/galacticwars/clonewars/fabric/GalacticWarsFabric.kt"));
        assertContains(fabricEntrypoint, "FabricBiomeSpawns.register()",
                "Fabric spawn bootstrap");
        System.out.println("CrossLoaderBiomeSpawnParityTest passed");
    }

    private static Map<String, SpawnEntry> readNeoForgeEntries() throws IOException {
        LinkedHashMap<String, SpawnEntry> entries = new LinkedHashMap<>();
        try (Stream<Path> files = Files.list(NEOFORGE_MODIFIERS)) {
            for (Path path : files.filter(file -> file.getFileName().toString().endsWith("_spawns.json"))
                    .sorted().toList()) {
                String json = Files.readString(path);
                String entityId = requiredSpawnerType(json, path);
                int weight = requiredInt(json, "weight", path);
                int minimum = requiredInt(json, "minCount", path);
                int maximum = requiredInt(json, "maxCount", path);
                String biomeArray = requiredArray(json, "biomes", path);
                LinkedHashSet<String> biomes = new LinkedHashSet<>();
                Matcher biomeMatcher = STRING.matcher(biomeArray);
                while (biomeMatcher.find()) {
                    biomes.add(biomeMatcher.group(1));
                }
                put(entries, entityId, new SpawnEntry(weight, minimum, maximum, Set.copyOf(biomes)));
            }
        }
        return Map.copyOf(entries);
    }

    private static Map<String, SpawnEntry> readFabricEntries() throws IOException {
        String source = Files.readString(FABRIC_SPAWNS);
        LinkedHashMap<String, SpawnEntry> entries = new LinkedHashMap<>();
        Matcher matcher = FABRIC_ENTRY.matcher(source);
        while (matcher.find()) {
            String entityId = "galacticwars:" + matcher.group(1).toLowerCase(java.util.Locale.ROOT);
            LinkedHashSet<String> biomes = new LinkedHashSet<>();
            Matcher biomeMatcher = STRING.matcher(matcher.group(5));
            while (biomeMatcher.find()) {
                biomes.add(biomeMatcher.group(1));
            }
            put(entries, entityId, new SpawnEntry(
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)),
                    Integer.parseInt(matcher.group(4)),
                    Set.copyOf(biomes)));
        }
        return Map.copyOf(entries);
    }

    private static String requiredString(String json, String field, Path source) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(field)
                + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        if (!matcher.find()) {
            throw new AssertionError("Missing " + field + " in <" + source + ">");
        }
        return matcher.group(1);
    }

    private static String requiredSpawnerType(String json, Path source) {
        Matcher matcher = Pattern.compile("\"spawners\"\\s*:\\s*\\{[^}]*"
                + "\"type\"\\s*:\\s*\"([^\"]+)\"", Pattern.DOTALL).matcher(json);
        if (!matcher.find()) {
            throw new AssertionError("Missing spawners.type in <" + source + ">");
        }
        return matcher.group(1);
    }

    private static int requiredInt(String json, String field, Path source) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(field)
                + "\"\\s*:\\s*(\\d+)").matcher(json);
        if (!matcher.find()) {
            throw new AssertionError("Missing " + field + " in <" + source + ">");
        }
        return Integer.parseInt(matcher.group(1));
    }

    private static String requiredArray(String json, String field, Path source) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(field)
                + "\"\\s*:\\s*\\[([^]]*)]", Pattern.DOTALL).matcher(json);
        if (!matcher.find()) {
            throw new AssertionError("Missing " + field + " in <" + source + ">");
        }
        return matcher.group(1);
    }

    private static void put(Map<String, SpawnEntry> entries, String id, SpawnEntry entry) {
        SpawnEntry previous = entries.putIfAbsent(id, entry);
        if (previous != null) {
            throw new AssertionError("Duplicate biome spawn mapping for " + id);
        }
    }

    private static void assertContains(String value, String expected, String label) {
        if (!value.contains(expected)) {
            throw new AssertionError(label + " missing <" + expected + ">");
        }
    }

    private record SpawnEntry(int weight, int minimum, int maximum, Set<String> biomes) {
        private SpawnEntry {
            if (weight < 1 || minimum < 1 || maximum < minimum || biomes.isEmpty()) {
                throw new IllegalArgumentException("invalid biome spawn entry");
            }
        }
    }
}
