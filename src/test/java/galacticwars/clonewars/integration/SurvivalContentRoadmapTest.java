package galacticwars.clonewars.integration;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;

public final class SurvivalContentRoadmapTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/galacticwars");
    private static final Path DATA = Path.of("src/main/resources/data/galacticwars");
    private static final Path MOD_BLOCKS = Path.of(
            "src/main/java/galacticwars/clonewars/registry/ModBlocks.java");
    private static final List<String> FAMILIES = List.of(
            "mandalorian_alloy", "nightsister_weave", "republic_plastoid", "separatist_alloy", "beskar");
    private static final List<String> TYPES = List.of(
            "sword", "axe", "pickaxe", "shovel", "hoe", "helmet", "chestplate", "leggings", "boots");

    private SurvivalContentRoadmapTest() {
    }

    public static void main(String[] args) throws Exception {
        hallAndNightsisterWeaveAssetsAreComplete();
        everyEquipmentFamilyHasRecipesModelsIconsAndArmorLayers();
        processingAndAcquisitionRecipesExist();
        beskarOreRequiresDeepslateMiningProperties();
        System.out.println("SurvivalContentRoadmapTest passed");
    }

    private static void hallAndNightsisterWeaveAssetsAreComplete() throws Exception {
        for (String texture : List.of("command_center_side", "command_center_top", "command_center_bottom")) {
            assertPng(ASSETS.resolve("textures/block/" + texture + ".png"), 16, 16);
        }
        String hallModel = Files.readString(ASSETS.resolve("models/block/command_center.json"));
        assertTrue(hallModel.contains("\"from\":[2,12,2]"), "Hall inset top");
        assertTrue(count(hallModel, "\"from\":[") >= 6, "Hall corner details");
        assertContains(ASSETS.resolve("items/command_center.json"),
                "\"model\":\"galacticwars:item/command_center\"",
                "Hall item definition uses its item model");
        assertContains(ASSETS.resolve("models/item/command_center.json"),
                "\"parent\":\"galacticwars:block/command_center\"",
                "Hall item model uses the dedicated block model");
        for (String block : List.of("nightsister_weave_sapling", "nightsister_weave_leaves", "nightsister_weave_planks")) {
            assertTrue(Files.exists(ASSETS.resolve("blockstates/" + block + ".json")), block + " blockstate");
            assertTrue(Files.exists(DATA.resolve("loot_table/blocks/" + block + ".json")), block + " loot");
        }
        assertTrue(Files.exists(DATA.resolve("worldgen/configured_feature/nightsister_weave_tree.json")), "NightsisterWeave tree feature");
    }

    private static void everyEquipmentFamilyHasRecipesModelsIconsAndArmorLayers() throws Exception {
        int itemCount = 0;
        Set<Integer> iconHashes = new HashSet<>();
        Set<Integer> armorLayerHashes = new HashSet<>();
        for (String family : FAMILIES) {
            assertTrue(Files.exists(ASSETS.resolve("equipment/" + family + ".json")), family + " equipment asset");
            Path humanoid = ASSETS.resolve("textures/entity/equipment/humanoid/" + family + ".png");
            Path baby = ASSETS.resolve("textures/entity/equipment/humanoid_baby/" + family + ".png");
            Path leggings = ASSETS.resolve("textures/entity/equipment/humanoid_leggings/" + family + ".png");
            assertArmorPng(humanoid, 64, 32, 648);
            assertArmorPng(baby, 64, 64, 615);
            assertArmorPng(leggings, 64, 32, 280);
            armorLayerHashes.add(pixelHash(humanoid));
            armorLayerHashes.add(pixelHash(baby));
            armorLayerHashes.add(pixelHash(leggings));
            for (String type : TYPES) {
                String id = family + "_" + type;
                assertTrue(Files.exists(ASSETS.resolve("items/" + id + ".json")), id + " item definition");
                assertTrue(Files.exists(ASSETS.resolve("models/item/" + id + ".json")), id + " model");
                assertTrue(Files.exists(DATA.resolve("recipe/" + id + ".json")), id + " recipe");
                Path icon = ASSETS.resolve("textures/item/" + id + ".png");
                assertPng(icon, 16, 16);
                iconHashes.add(pixelHash(icon));
                String expectedParent = switch (type) {
                    case "sword", "axe", "pickaxe", "shovel", "hoe" -> "minecraft:item/handheld";
                    default -> "minecraft:item/generated";
                };
                assertContains(ASSETS.resolve("models/item/" + id + ".json"),
                        "\"parent\":\"" + expectedParent + "\"", id + " display parent");
                itemCount++;
            }
        }
        assertEquals(45, itemCount, "equipment item count");
        assertEquals(45, iconHashes.size(), "every equipment item has distinct art");
        assertEquals(15, armorLayerHashes.size(), "every equipment layer has distinct art");
    }

    private static void processingAndAcquisitionRecipesExist() {
        for (String recipe : List.of(
                "command_center", "duracrete_stonecutting", "nightsister_weave_sapling", "nightsister_weave",
                "beskar_ore", "beskar_ingot_from_smelting", "beskar_ingot_from_blasting",
                "republic_plastoid_ingot", "mandalorian_alloy_ingot", "separatist_alloy_shard", "separatist_alloy_ingot")) {
            assertTrue(Files.exists(DATA.resolve("recipe/" + recipe + ".json")), recipe + " recipe");
        }
    }

    private static void beskarOreRequiresDeepslateMiningProperties() throws Exception {
        String source = Files.readString(MOD_BLOCKS);
        int start = source.indexOf("BESKAR_ORE =");
        int end = source.indexOf("NIGHTSISTER_WEAVE_LOG =", start);
        assertTrue(start >= 0 && end > start, "Beskar ore registration");
        String registration = source.substring(start, end);
        assertTrue(registration.contains("mapColor(MapColor.DEEPSLATE)"), "Beskar ore deepslate map color");
        assertTrue(registration.contains("strength(4.5F, 3.0F)"), "Beskar ore deepslate strength");
        assertTrue(registration.contains("sound(SoundType.DEEPSLATE)"), "Beskar ore deepslate sound");
        assertTrue(registration.contains("requiresCorrectToolForDrops()"), "Beskar ore tool requirement");
    }

    private static void assertContains(Path path, String expected, String label) throws Exception {
        assertTrue(Files.readString(path).contains(expected), label);
    }

    private static void assertPng(Path path, int width, int height) throws Exception {
        BufferedImage image = ImageIO.read(path.toFile());
        assertTrue(image != null && image.getWidth() == width && image.getHeight() == height, path.toString());
        int visiblePixels = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) visiblePixels++;
            }
        }
        assertTrue(visiblePixels > 0, path + " is fully transparent");
    }

    private static int pixelHash(Path path) throws Exception {
        BufferedImage image = ImageIO.read(path.toFile());
        int hash = 1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                hash = 31 * hash + image.getRGB(x, y);
            }
        }
        return hash;
    }

    private static void assertArmorPng(Path path, int width, int height, int expectedOpaquePixels) throws Exception {
        BufferedImage image = ImageIO.read(path.toFile());
        assertTrue(image != null && image.getWidth() == width && image.getHeight() == height, path.toString());
        int opaquePixels = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) opaquePixels++;
            }
        }
        assertEquals(expectedOpaquePixels, opaquePixels, path + " vanilla-valid opaque silhouette");
        assertTrue(opaquePixels < width * height * 3 / 4, path + " paints unused UV space");
        assertTrue((image.getRGB(width - 1, 0) >>> 24) == 0, path + " must keep unused corners transparent");
    }

    private static int count(String value, String needle) {
        int count = 0;
        for (int index = 0; (index = value.indexOf(needle, index)) >= 0; index += needle.length()) count++;
        return count;
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) throw new AssertionError(label + " expected " + expected + " but was " + actual);
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }
}
