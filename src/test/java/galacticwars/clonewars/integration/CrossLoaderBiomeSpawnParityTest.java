package galacticwars.clonewars.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/** Guards the intentional cross-loader removal of free Overworld NPC spawning. */
public final class CrossLoaderBiomeSpawnParityTest {
    private static final Path NEOFORGE_MODIFIERS = Path.of(
            "src/main/resources/data/galacticwars/neoforge/biome_modifier");
    private static final Path FABRIC_SPAWNS = Path.of(
            "fabric/src/main/kotlin/galacticwars/clonewars/fabric/FabricBiomeSpawns.kt");

    private CrossLoaderBiomeSpawnParityTest() {
    }

    public static void main(String[] args) throws IOException {
        if (Files.exists(FABRIC_SPAWNS)) {
            throw new AssertionError("Fabric free Overworld NPC spawn registration still exists");
        }
        try (Stream<Path> files = Files.walk(NEOFORGE_MODIFIERS)) {
            for (Path path : files.filter(Files::isRegularFile).toList()) {
                String content = Files.readString(path);
                if (path.getFileName().toString().endsWith("_spawns.json") || content.contains("add_spawns")) {
                    throw new AssertionError("NeoForge free Overworld NPC spawn registration still exists: " + path);
                }
            }
        }
        String fabricEntrypoint = Files.readString(Path.of(
                "fabric/src/main/kotlin/galacticwars/clonewars/fabric/GalacticWarsFabric.kt"));
        assertContains(fabricEntrypoint, "FabricWorldgenFeatures.register()", "Fabric resource bootstrap");
        String fabricFeatures = Files.readString(Path.of(
                "fabric/src/main/kotlin/galacticwars/clonewars/fabric/FabricWorldgenFeatures.kt"));
        assertContains(fabricFeatures, "galacticwars:beskar_ore", "Fabric Beskar feature");
        assertContains(fabricFeatures, "galacticwars:nightsister_weave_grove", "Fabric weave feature");
        assertContains(Files.readString(NEOFORGE_MODIFIERS.resolve("beskar_ore.json")),
                "galacticwars:beskar_ore", "NeoForge Beskar feature");
        assertContains(Files.readString(NEOFORGE_MODIFIERS.resolve("nightsister_weave_grove.json")),
                "galacticwars:nightsister_weave_grove", "NeoForge weave feature");
        System.out.println("CrossLoaderBiomeSpawnParityTest passed");
    }

    private static void assertContains(String value, String expected, String label) {
        if (!value.contains(expected)) throw new AssertionError(label + " missing <" + expected + ">");
    }
}
