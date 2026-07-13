package galacticwars.clonewars.integration;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;

public final class LightsaberAssetIntegrationTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/galacticwars");
    private static final List<String> COLORS =
            List.of("blue", "green", "red", "purple", "yellow", "white");

    private LightsaberAssetIntegrationTest() {
    }

    public static void main(String[] args) throws Exception {
        String base = json(ASSETS.resolve("models/item/lightsaber_base.json"));
        require(base.contains("\"display\""), "lightsaber base model must define held-item transforms");
        require(occurrences(base, "\"name\"") >= 16,
                "lightsaber base model must contain a detailed segmented hilt and layered blade");
        require(base.contains("\"energy_blade\""), "lightsaber model needs energy blade");
        require(base.contains("\"particle\": \"#hilt\""),
                "lightsaber base model must resolve its particle texture through the hilt");
        require(base.contains("\"light_emission\": 15"), "blade must render at full light");
        require(base.contains("\"shade\": false"), "blade must not receive block-style shading");

        for (String color : COLORS) {
            String definition = json(ASSETS.resolve("items/" + color + "_lightsaber.json"));
            require(
                    definition.contains("galacticwars:item/" + color + "_lightsaber"),
                    color + " item definition must resolve its model");

            String model = json(ASSETS.resolve("models/item/" + color + "_lightsaber.json"));
            require(
                    model.contains("\"parent\":\"galacticwars:item/lightsaber_base\""),
                    color + " model must inherit shared 3D geometry");
            require(
                    model.contains("\"hilt\":\"galacticwars:item/lightsaber/" + color + "_hilt\""),
                    color + " hilt texture reference");
            require(
                    model.contains("\"blade\":\"galacticwars:item/lightsaber/" + color + "_blade\""),
                    color + " blade texture reference");

            image("textures/item/" + color + "_lightsaber.png", 16, 16, true);
            image("textures/item/lightsaber/" + color + "_hilt.png", 32, 32, false);
            image("textures/item/lightsaber/" + color + "_blade.png", 32, 128, true);

            String animation = json(
                    ASSETS.resolve("textures/item/lightsaber/" + color + "_blade.png.mcmeta"));
            require(animation.contains("\"frametime\": 2"), color + " blade animation speed");
            require(animation.contains("\"interpolate\": true"), color + " blade interpolation");
        }

        String clientExtensions = Files.readString(Path.of(
                "src/main/java/galacticwars/clonewars/client/render/LightsaberClientExtensions.java"));
        require(
                clientExtensions.contains("HumanoidModel.ArmPose.ITEM"),
                "third-person lightsaber held-item stance");
        require(
                clientExtensions.contains("applyForgeHandTransform"),
                "first-person lightsaber wield animation");
        require(clientExtensions.contains("swingProgress"), "lightsaber slash must follow swing progress");
        require(
                clientExtensions.contains("return false;"),
                "first-person extension must preserve Minecraft's normal hand transform");

        System.out.println("LightsaberAssetIntegrationTest passed");
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
        require(
                image.getWidth() == expectedWidth && image.getHeight() == expectedHeight,
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
