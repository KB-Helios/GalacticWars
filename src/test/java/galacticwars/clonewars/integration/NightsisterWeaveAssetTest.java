package galacticwars.clonewars.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class NightsisterWeaveAssetTest {
    private NightsisterWeaveAssetTest() {
    }

    public static void main(String[] args) throws IOException {
        nightsister_weaveLogUsesAxisAwareBlock();
        nightsister_weaveLogUsesModdedTextures();
        duracreteAndBeskarUseModdedTextures();
        nightsister_weaveTexturesExist();

        System.out.println("NightsisterWeaveAssetTest passed");
    }

    private static void nightsister_weaveLogUsesAxisAwareBlock() throws IOException {
        String blocks = Files.readString(Path.of("src/main/java/galacticwars/clonewars/registry/ModBlocks.java"));
        String blockstate = Files.readString(Path.of(
                "src/main/resources/assets/galacticwars/blockstates/nightsister_weave_log.json"));

        assertContains(blocks, "RotatedPillarBlock", "nightsister_weave log should be a rotated pillar block");
        assertContains(blockstate, "\"axis=y\"", "nightsister_weave log should have vertical axis state");
        assertContains(blockstate, "\"axis=x\"", "nightsister_weave log should have x axis state");
        assertContains(blockstate, "\"axis=z\"", "nightsister_weave log should have z axis state");
    }

    private static void nightsister_weaveLogUsesModdedTextures() throws IOException {
        String model = Files.readString(Path.of(
                "src/main/resources/assets/galacticwars/models/block/nightsister_weave_log.json"));

        assertContains(model,
                "galacticwars:block/nightsister_weave_log_top",
                "nightsister_weave log top texture");
        assertContains(model,
                "galacticwars:block/nightsister_weave_log",
                "nightsister_weave log side texture");
        assertNotContains(model, "minecraft:block/oak_log", "nightsister_weave log should not use oak side texture");
        assertNotContains(model, "minecraft:block/oak_log_top", "nightsister_weave log should not use oak top texture");
    }

    private static void nightsister_weaveTexturesExist() {
        assertRegularFile("src/main/resources/assets/galacticwars/textures/block/nightsister_weave_log.png");
        assertRegularFile("src/main/resources/assets/galacticwars/textures/block/nightsister_weave_log_top.png");
        assertRegularFile("src/main/resources/assets/galacticwars/textures/block/duracrete.png");
        assertRegularFile("src/main/resources/assets/galacticwars/textures/block/beskar_ore.png");
    }

    private static void duracreteAndBeskarUseModdedTextures() throws IOException {
        String stoneModel = Files.readString(Path.of(
                "src/main/resources/assets/galacticwars/models/block/duracrete.json"));
        String beskarModel = Files.readString(Path.of(
                "src/main/resources/assets/galacticwars/models/block/beskar_ore.json"));

        assertContains(stoneModel,
                "galacticwars:block/duracrete",
                "duracrete texture");
        assertContains(beskarModel,
                "galacticwars:block/beskar_ore",
                "beskar ore texture");
        assertNotContains(stoneModel, "minecraft:block/stone", "duracrete should not use vanilla stone");
        assertNotContains(beskarModel, "minecraft:block/deepslate_diamond_ore", "beskar ore should not use vanilla ore");
    }

    private static void assertContains(String haystack, String needle, String label) {
        if (!haystack.contains(needle)) {
            throw new AssertionError(label + " missing <" + needle + ">");
        }
    }

    private static void assertNotContains(String haystack, String needle, String label) {
        if (haystack.contains(needle)) {
            throw new AssertionError(label + " contains forbidden <" + needle + ">");
        }
    }

    private static void assertRegularFile(String relativePath) {
        if (!Files.isRegularFile(Path.of(relativePath))) {
            throw new AssertionError("missing texture <" + relativePath + ">");
        }
    }
}
