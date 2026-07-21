package galacticwars.clonewars.integration;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

public final class LightsaberAssetIntegrationTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/galacticwars");
    private static final List<String> COLORS =
            List.of("blue", "green", "red", "purple", "yellow", "white");

    private LightsaberAssetIntegrationTest() {
    }

    public static void main(String[] args) throws Exception {
        String base = json(ASSETS.resolve("models/item/lightsaber_base.json"));
        require(base.contains("\"parent\": \"builtin/entity\""),
                "lightsaber display model must delegate rendering to GeckoLib");
        require(base.contains("\"display\""), "lightsaber base model must define held-item transforms");
        require(transformContains(base, "thirdperson_righthand", 0, -90, 30),
                "third-person saber transform must keep the blade ready without over-rotating the hilt");
        require(transformContains(base, "firstperson_righthand", 0, -90, 15),
                "first-person saber transform must keep the blade clear of the camera");
        require(transformTranslationContains(base, "thirdperson_righthand", 0, 0.5, -0.75),
                "third-person saber transform must center the hand on the hilt pivot");
        require(transformTranslationContains(base, "firstperson_righthand", 0.45, 0.8, -0.65),
                "first-person saber transform must center the hand on the hilt pivot");

        Set<Integer> geometryHashes = new HashSet<>();
        for (String color : COLORS) {
            String definition = json(ASSETS.resolve("items/" + color + "_lightsaber.json"));
            require(
                    definition.contains("\"type\": \"minecraft:special\"")
                            && definition.contains("galacticwars:item/" + color + "_lightsaber")
                            && definition.contains("\"type\": \"geckolib:geckolib\""),
                    color + " item definition must resolve through GeckoLib's special renderer");

            String model = json(ASSETS.resolve("models/item/" + color + "_lightsaber.json"));
            require(
                    model.contains("\"parent\": \"galacticwars:item/lightsaber_base\""),
                    color + " display model must inherit the shared transforms");

            image("textures/item/" + color + "_lightsaber.png", 16, 16, true);
            image("textures/item/lightsaber/" + color + ".png", 256, 1024, true);
            image("textures/item/lightsaber/" + color + "_glowmask.png", 256, 1024, true);

            String geometry = json(ASSETS.resolve(
                    "geckolib/models/item/lightsaber/" + color + ".geo.json"));
            require(geometry.contains("geometry.galacticwars.item.lightsaber." + color),
                    color + " lightsaber must own a stable geometry identifier");
            require(geometry.contains("\"texture_width\": 256")
                            && geometry.contains("\"texture_height\": 256")
                            && geometry.contains("\"name\": \"hilt\"")
                            && geometry.contains("\"name\": \"blade\""),
                    color + " lightsaber geometry and atlas contract");
            require(occurrences(geometry, "\"origin\"") >= 26
                            && geometry.contains("36.0") && geometry.contains("36.1"),
                    color + " must retain a detailed original hilt and long blade");
            require(hiltSpansHandPivot(geometry), color + " hilt must straddle the hand origin");
            require(Pattern.compile(
                            "\\\"name\\\"\\s*:\\s*\\\"blade\\\"(?s:.*?)"
                                    + "\\\"pivot\\\"\\s*:\\s*\\[\\s*0(?:\\.0)?\\s*,\\s*5(?:\\.0)?\\s*,\\s*0(?:\\.0)?\\s*]")
                            .matcher(geometry).find(),
                    color + " blade pivot contract");
            geometryHashes.add(java.util.Arrays.hashCode(geometry.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            require(json(ASSETS.resolve(
                    "geckolib/animations/item/lightsaber/" + color + ".animation.json"))
                            .contains("animation.lightsaber.idle"),
                    color + " GeckoLib idle animation");

            String animation = json(
                    ASSETS.resolve("textures/item/lightsaber/" + color + ".png.mcmeta"));
            require(animation.contains("\"frametime\": 2"), color + " blade animation speed");
            require(animation.contains("\"interpolate\": true"), color + " blade interpolation");
            require(Files.isRegularFile(ASSETS.resolve(
                            "textures/item/lightsaber/" + color + "_glowmask.png.mcmeta")),
                    color + " glowmask animation metadata");
        }
        require(geometryHashes.size() == COLORS.size(), "every lightsaber color must own distinct hilt geometry");

        String itemClass = Files.readString(Path.of(
                "src/main/java/galacticwars/clonewars/item/LightsaberItem.java"));
        require(itemClass.contains("implements GeoItem"), "lightsabers must be GeckoLib items");
        require(itemClass.contains("createGeoRenderer"), "lightsabers must register a GeckoLib renderer");
        require(itemClass.contains("animation.lightsaber.idle"),
                "lightsabers must drive their GeckoLib idle animation");

        String renderer = Files.readString(Path.of(
                "src/main/java/galacticwars/clonewars/client/render/GalacticLightsaberRenderer.java"));
        require(renderer.contains("extends GeoItemRenderer"), "lightsabers must use GeoItemRenderer");
        require(renderer.contains("AutoGlowingGeoLayer"), "lightsaber blades must use an emissive glowmask");

        String clientExtensions = Files.readString(Path.of(
                "src/main/java/galacticwars/clonewars/client/render/LightsaberClientExtensions.java"));
        require(
                clientExtensions.contains("HumanoidModel.ArmPose.ITEM"),
                "third-person lightsaber held-item stance");
        require(
                clientExtensions.contains("applyForgeHandTransform"),
                "first-person lightsaber wield animation");
        require(clientExtensions.contains("swingProgress"), "lightsaber slash must follow swing progress");
        String handTransform = methodBody(clientExtensions, "boolean applyForgeHandTransform");
        require(
                handTransform.contains("return false;") && !handTransform.contains("return true;"),
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

    private static boolean transformContains(
            String model,
            String transform,
            int x,
            int y,
            int z
    ) {
        Pattern pattern = Pattern.compile(
                "\\\"" + Pattern.quote(transform) + "\\\"[^}]*"
                        + "\\\"rotation\\\"\\s*:\\s*\\[\\s*" + x
                        + "(?:\\.0)?\\s*,\\s*" + y + "(?:\\.0)?\\s*,\\s*" + z
                        + "(?:\\.0)?\\s*]");
        return pattern.matcher(model).find();
    }

    private static boolean transformTranslationContains(
            String model,
            String transform,
            double x,
            double y,
            double z
    ) {
        Pattern pattern = Pattern.compile(
                "\\\"" + Pattern.quote(transform) + "\\\"[^}]*"
                        + "\\\"translation\\\"\\s*:\\s*\\[\\s*" + number(x)
                        + "\\s*,\\s*" + number(y) + "\\s*,\\s*" + number(z) + "\\s*]");
        return pattern.matcher(model).find();
    }

    private static String number(double value) {
        String literal = Double.toString(value).replace(".", "\\.");
        return value == Math.rint(value) ? literal.replace("\\.0", "(?:\\.0)?") : literal;
    }

    private static boolean hiltSpansHandPivot(String geometry) {
        int hiltStart = geometry.indexOf("\"name\": \"hilt\"");
        int bladeStart = geometry.indexOf("\"name\": \"blade\"");
        require(hiltStart >= 0 && bladeStart > hiltStart, "lightsaber hilt and blade ordering");
        String hilt = geometry.substring(hiltStart, bladeStart);
        Matcher origins = Pattern.compile(
                "\\\"origin\\\"\\s*:\\s*\\[\\s*-?[0-9.]+\\s*,\\s*(-?[0-9.]+)")
                .matcher(hilt);
        double minimumY = Double.POSITIVE_INFINITY;
        double maximumY = Double.NEGATIVE_INFINITY;
        while (origins.find()) {
            double y = Double.parseDouble(origins.group(1));
            minimumY = Math.min(minimumY, y);
            maximumY = Math.max(maximumY, y);
        }
        return minimumY <= -5.0 && maximumY >= 3.0;
    }

    private static String methodBody(String source, String signature) {
        int signatureStart = source.indexOf(signature);
        require(signatureStart >= 0, "missing method " + signature);
        int bodyStart = source.indexOf('{', signatureStart);
        require(bodyStart >= 0, "missing method body " + signature);
        int depth = 0;
        for (int index = bodyStart; index < source.length(); index++) {
            char character = source.charAt(index);
            if (character == '{') {
                depth++;
            } else if (character == '}' && --depth == 0) {
                return source.substring(bodyStart + 1, index);
            }
        }
        throw new AssertionError("unterminated method body " + signature);
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
