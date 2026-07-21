package galacticwars.clonewars.integration;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

public final class BlasterAssetIntegrationTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/galacticwars");
    private static final List<String> WEAPONS =
            List.of("dc15_blaster", "e5_blaster", "westar_blaster", "scatter_blaster");

    private BlasterAssetIntegrationTest() {
    }

    public static void main(String[] args) throws Exception {
        Set<String> geometryDigests = new HashSet<>();
        Set<String> textureDigests = new HashSet<>();
        for (String weapon : WEAPONS) {
            String definition = json(ASSETS.resolve("items/" + weapon + ".json"));
            require(definition.contains("\"type\": \"minecraft:special\"")
                            && definition.contains("\"type\": \"geckolib:geckolib\"")
                            && definition.contains("galacticwars:item/" + weapon),
                    weapon + " must resolve through GeckoLib's special item renderer");

            String display = json(ASSETS.resolve("models/item/" + weapon + ".json"));
            require(display.contains("\"parent\": \"builtin/entity\"")
                            && display.contains("\"thirdperson_righthand\"")
                            && display.contains("\"firstperson_righthand\"")
                            && display.contains("\"gui\"")
                            && display.contains("\"fixed\""),
                    weapon + " must define consistent GeckoLib transforms for all item contexts");
            require(!display.contains("minecraft:item/generated")
                            && !display.contains("minecraft:item/handheld"),
                    weapon + " must not fall back to a paper-thin vanilla model");
            require(!Pattern.compile("-?\\d+\\.\\d{10,}").matcher(display).find(),
                    weapon + " display transforms must use clean decimal values");

            Path geometryPath = ASSETS.resolve("geckolib/models/item/blaster/" + weapon + ".geo.json");
            String geometry = json(geometryPath);
            require(geometry.contains("geometry.galacticwars.item.blaster." + weapon),
                    weapon + " stable visual geometry identifier");
            require(geometry.contains("\"name\": \"receiver\"")
                            && geometry.contains("\"name\": \"grip\"")
                            && geometry.contains("\"name\": \"barrel\"")
                            && geometry.contains("\"name\": \"power_cell\"")
                            && geometry.contains("\"name\": \"muzzle\""),
                    weapon + " must expose firearm landmark bones");
            require(occurrences(geometry, "\"origin\"") >= 12,
                    weapon + " must be a detailed volumetric model");
            geometryDigests.add(digest(geometryPath));

            Path texture = ASSETS.resolve("textures/item/blaster/" + weapon + ".png");
            Path glowmask = ASSETS.resolve("textures/item/blaster/" + weapon + "_glowmask.png");
            assertAtlas(texture, true);
            assertAtlas(glowmask, true);
            textureDigests.add(digest(texture));
            require(json(ASSETS.resolve("geckolib/animations/item/blaster/" + weapon + ".animation.json"))
                            .contains("animation.blaster.idle"),
                    weapon + " GeckoLib animation contract");
        }
        require(geometryDigests.size() == WEAPONS.size(), "each blaster must own distinct geometry");
        require(textureDigests.size() == WEAPONS.size(), "each blaster must own a distinct UV-safe atlas");

        String itemClass = Files.readString(Path.of(
                "src/main/java/galacticwars/clonewars/combat/BlasterItem.java"));
        require(itemClass.contains("implements GeoItem")
                        && itemClass.contains("visualId")
                        && itemClass.contains("createGeoRenderer")
                        && itemClass.contains("animation.blaster.idle"),
                "BlasterItem must expose the shared GeckoLib visual contract");
        String renderer = Files.readString(Path.of(
                "src/main/java/galacticwars/clonewars/client/render/GalacticBlasterRenderer.java"));
        require(renderer.contains("extends GeoItemRenderer")
                        && renderer.contains("AutoGlowingGeoLayer")
                        && renderer.contains("item.visualId()"),
                "blasters must use weapon-specific geometry and emissive materials");
        assertHeldWeaponPose();
        System.out.println("BlasterAssetIntegrationTest passed");
    }

    private static void assertHeldWeaponPose() throws Exception {
        String extension = Files.readString(Path.of(
                "src/main/java/galacticwars/clonewars/client/render/BlasterClientExtensions.java"));
        require(extension.contains("HumanoidModel.ArmPose.CROSSBOW_HOLD")
                        && extension.contains("applyForgeHandTransform")
                        && extension.contains("recoil"),
                "NeoForge blasters must retain their shouldered/recoil pose");
        String fabricClient = Files.readString(Path.of(
                "fabric/src/main/kotlin/galacticwars/clonewars/fabric/GalacticWarsFabricClient.kt"));
        require(fabricClient.contains("GalacticWarsClient.init()"),
                "Fabric must initialize the common GeckoLib item-rendering path");
    }

    private static void assertAtlas(Path path, boolean requireTransparency) throws Exception {
        require(Files.isRegularFile(path), "missing texture " + path);
        BufferedImage image = ImageIO.read(path.toFile());
        require(image != null && image.getWidth() == 256 && image.getHeight() == 256,
                path + " must be a 256x256 geometry-bound atlas");
        boolean visible = false;
        boolean transparent = false;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                visible |= alpha != 0;
                transparent |= alpha == 0;
            }
        }
        require(visible, path + " must contain visible mapped pixels");
        require(!requireTransparency || transparent, path + " must retain unused transparent space");
    }

    private static String digest(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(path)));
    }

    private static String json(Path path) throws Exception {
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

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
