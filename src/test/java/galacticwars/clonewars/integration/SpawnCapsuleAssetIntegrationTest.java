package galacticwars.clonewars.integration;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;

public final class SpawnCapsuleAssetIntegrationTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/galacticwars");
    private static final List<String> VISUAL_IDS = List.of(
            "clone_trooper", "arc_trooper", "jedi_knight",
            "b1_battle_droid", "b2_super_battle_droid", "commando_droid",
            "mandalorian_warrior", "mandalorian_marksman", "mandalorian_heavy",
            "hutt_enforcer", "bounty_hunter", "smuggler",
            "nightsister_acolyte", "nightsister_archer", "nightbrother_brute",
            "republic_civilian", "separatist_technician", "mandalorian_clansperson",
            "hutt_civilian", "nightsister_civilian");

    private SpawnCapsuleAssetIntegrationTest() {
    }

    public static void main(String[] args) throws Exception {
        String geometry = json("geckolib/models/item/spawn_capsule.geo.json");
        require(geometry.contains("geometry.galacticwars.item.spawn_capsule"),
                "spawn capsule geometry identifier");
        require(geometry.contains("\"texture_width\": 512")
                        && geometry.contains("\"texture_height\": 512"),
                "spawn capsule high-density atlas dimensions");
        require(geometry.contains("\"name\": \"root\"")
                        && geometry.contains("\"name\": \"shell\"")
                        && geometry.contains("\"name\": \"core\""),
                "spawn capsule bone contract");
        require(occurrences(geometry, "\"origin\"") >= 18,
                "spawn capsule detailed segmented silhouette");
        require(json("geckolib/animations/item/spawn_capsule.animation.json")
                        .contains("animation.spawn_capsule.idle"),
                "spawn capsule idle animation");
        require(json("models/item/spawn_capsule_base.json").contains("\"parent\": \"builtin/entity\""),
                "spawn capsule display model delegates to GeckoLib");

        for (String id : VISUAL_IDS) {
            String definition = json("items/" + id + "_spawn_egg.json");
            require(definition.contains("\"type\": \"minecraft:special\"")
                            && definition.contains("\"type\": \"geckolib:geckolib\""),
                    id + " spawn capsule special item definition");
            require(json("models/item/" + id + "_spawn_egg.json")
                            .contains("galacticwars:item/spawn_capsule_base"),
                    id + " spawn capsule display-model parent");
            image("textures/item/" + id + "_spawn_egg.png", 16, 16, true);
            image("textures/item/spawn_capsule/" + id + ".png", 512, 512, true);
            image("textures/item/spawn_capsule/" + id + "_glowmask.png", 512, 512, true);
        }

        String item = Files.readString(Path.of(
                "src/main/java/galacticwars/clonewars/entity/RecruitSpawnEggItem.java"));
        require(item.contains("extends SpawnEggItem implements GeoItem"),
                "spawn capsule retains vanilla spawning and adds GeckoLib rendering");
        require(item.contains("super(properties.spawnEgg(recruitType))"),
                "spawn capsule retains vanilla recruit type binding");
        require(item.contains("String visualId") && item.contains("createGeoRenderer"),
                "spawn capsule selects its material and creates its renderer");

        String renderer = Files.readString(Path.of(
                "src/main/java/galacticwars/clonewars/client/render/RecruitSpawnEggRenderer.java"));
        require(renderer.contains("extends GeoItemRenderer"), "spawn capsule GeoItemRenderer");
        require(renderer.contains("AutoGlowingGeoLayer"), "spawn capsule emissive status lights");
        require(renderer.contains("spawn_capsule/\" + item.visualId()"),
                "spawn capsule unit-specific material selection");

        System.out.println("SpawnCapsuleAssetIntegrationTest passed");
    }

    private static String json(String relativePath) throws Exception {
        Path path = ASSETS.resolve(relativePath);
        require(Files.isRegularFile(path), "missing JSON asset " + path);
        String content = Files.readString(path).trim();
        require(content.startsWith("{") && content.endsWith("}"), "invalid JSON envelope " + path);
        return content;
    }

    private static int occurrences(String content, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = content.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }

    private static void image(
            String relativePath,
            int expectedWidth,
            int expectedHeight,
            boolean requireTransparency
    ) throws Exception {
        Path path = ASSETS.resolve(relativePath);
        require(Files.isRegularFile(path), "missing image asset " + path);
        BufferedImage image = ImageIO.read(path.toFile());
        require(image != null, "unreadable image asset " + path);
        require(image.getWidth() == expectedWidth && image.getHeight() == expectedHeight,
                path + " expected " + expectedWidth + "x" + expectedHeight);
        boolean visible = false;
        boolean transparent = false;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                visible |= alpha != 0;
                transparent |= alpha == 0;
            }
        }
        require(visible, path + " must contain visible pixels");
        require(!requireTransparency || transparent, path + " must retain transparent background");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
