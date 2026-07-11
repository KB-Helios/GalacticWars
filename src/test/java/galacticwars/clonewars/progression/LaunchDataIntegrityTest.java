package galacticwars.clonewars.progression;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

public final class LaunchDataIntegrityTest {
    private static final Path ROOT = Path.of("src/main/resources/data/galacticwars");
    private static final Path GAMEPLAY = ROOT.resolve("galacticwars");

    public static void main(String[] args) throws Exception {
        assertJsonCount(GAMEPLAY.resolve("factions"), 5, "factions");
        assertJsonCount(GAMEPLAY.resolve("units"), 15, "units");
        for (String category : Set.of("planets", "vehicles", "force_abilities", "quests", "trades", "conquest_regions")) {
            assertTrue(Files.isRegularFile(GAMEPLAY.resolve(category).resolve("launch.json")), category + " launch data");
        }
        for (String planet : LaunchContentCatalog.PLANETS) {
            assertTrue(Files.isRegularFile(ROOT.resolve("dimension").resolve(planet + ".json")), planet + " dimension");
        }
        assertTrue(Files.isRegularFile(ROOT.resolve("dimension_type/planet.json")), "planet dimension type");
        String quests = Files.readString(GAMEPLAY.resolve("quests/launch.json"));
        for (String quest : LaunchContentCatalog.QUESTS) {
            assertTrue(quests.contains("\"id\":\"" + quest + "\""), quest);
        }
        System.out.println("LaunchDataIntegrityTest passed");
    }

    private static void assertJsonCount(Path directory, long expected, String label) throws Exception {
        try (Stream<Path> files = Files.list(directory)) {
            assertTrue(files.filter(path -> path.toString().endsWith(".json")).count() == expected, label);
        }
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }
}
