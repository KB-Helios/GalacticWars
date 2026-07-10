package middleearth.lotr.warmod.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MallornAssetTest {
    private MallornAssetTest() {
    }

    public static void main(String[] args) throws IOException {
        mallornLogUsesAxisAwareBlock();
        mallornLogUsesModdedTextures();
        middleEarthStoneAndMithrilUseModdedTextures();
        mallornTexturesExist();

        System.out.println("MallornAssetTest passed");
    }

    private static void mallornLogUsesAxisAwareBlock() throws IOException {
        String blocks = Files.readString(Path.of("src/main/java/middleearth/lotr/warmod/registry/ModBlocks.java"));
        String blockstate = Files.readString(Path.of(
                "src/main/resources/assets/kingdomwarsmiddleearth/blockstates/mallorn_log.json"));

        assertContains(blocks, "RotatedPillarBlock", "mallorn log should be a rotated pillar block");
        assertContains(blockstate, "\"axis=y\"", "mallorn log should have vertical axis state");
        assertContains(blockstate, "\"axis=x\"", "mallorn log should have x axis state");
        assertContains(blockstate, "\"axis=z\"", "mallorn log should have z axis state");
    }

    private static void mallornLogUsesModdedTextures() throws IOException {
        String model = Files.readString(Path.of(
                "src/main/resources/assets/kingdomwarsmiddleearth/models/block/mallorn_log.json"));

        assertContains(model,
                "kingdomwarsmiddleearth:block/mallorn_log_top",
                "mallorn log top texture");
        assertContains(model,
                "kingdomwarsmiddleearth:block/mallorn_log",
                "mallorn log side texture");
        assertNotContains(model, "minecraft:block/oak_log", "mallorn log should not use oak side texture");
        assertNotContains(model, "minecraft:block/oak_log_top", "mallorn log should not use oak top texture");
    }

    private static void mallornTexturesExist() {
        assertRegularFile("src/main/resources/assets/kingdomwarsmiddleearth/textures/block/mallorn_log.png");
        assertRegularFile("src/main/resources/assets/kingdomwarsmiddleearth/textures/block/mallorn_log_top.png");
        assertRegularFile("src/main/resources/assets/kingdomwarsmiddleearth/textures/block/middle_earth_stone.png");
        assertRegularFile("src/main/resources/assets/kingdomwarsmiddleearth/textures/block/mithril_ore.png");
    }

    private static void middleEarthStoneAndMithrilUseModdedTextures() throws IOException {
        String stoneModel = Files.readString(Path.of(
                "src/main/resources/assets/kingdomwarsmiddleearth/models/block/middle_earth_stone.json"));
        String mithrilModel = Files.readString(Path.of(
                "src/main/resources/assets/kingdomwarsmiddleearth/models/block/mithril_ore.json"));

        assertContains(stoneModel,
                "kingdomwarsmiddleearth:block/middle_earth_stone",
                "middle-earth stone texture");
        assertContains(mithrilModel,
                "kingdomwarsmiddleearth:block/mithril_ore",
                "mithril ore texture");
        assertNotContains(stoneModel, "minecraft:block/stone", "middle-earth stone should not use vanilla stone");
        assertNotContains(mithrilModel, "minecraft:block/deepslate_diamond_ore", "mithril ore should not use vanilla ore");
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
