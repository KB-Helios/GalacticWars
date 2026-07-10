package middleearth.lotr.warmod.integration;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

public final class RecruitTextureAtlasTest {
    private static final Pattern BOX_UV = Pattern.compile(
            "\\\"size\\\": \\[([0-9.]+), ([0-9.]+), ([0-9.]+)], \\\"uv\\\": \\[([0-9]+), ([0-9]+)]");
    private static final Path ASSET_ROOT = Path.of("src/main/resources/assets/kingdomwarsmiddleearth");
    private static final List<String> RECRUITS = List.of(
            "gondor_recruit",
            "rohan_recruit",
            "mordor_orc_recruit",
            "dwarf_recruit",
            "elf_recruit");
    private static final Map<String, String> FACTION_GEOMETRY_MARKERS = Map.of(
            "gondor_recruit", "\"size\": [5.5, 4, 5.5]",
            "rohan_recruit", "\"size\": [2, 3, 6]",
            "mordor_orc_recruit", "\"rotation\": [7, 0, 0]",
            "dwarf_recruit", "\"visible_bounds_height\": 2.4",
            "elf_recruit", "\"visible_bounds_height\": 3.2");

    private static final List<BoxUv> REQUIRED_BASE_CUBES = List.of(
            new BoxUv(0, 0, 8, 8, 8, "head"),
            new BoxUv(16, 16, 8, 12, 4, "body"),
            new BoxUv(40, 16, 4, 12, 4, "right arm"),
            new BoxUv(32, 48, 4, 12, 4, "left arm"),
            new BoxUv(0, 16, 4, 12, 4, "right leg"),
            new BoxUv(16, 48, 4, 12, 4, "left leg"));

    private RecruitTextureAtlasTest() {
    }

    public static void main(String[] args) throws IOException {
        for (String recruit : RECRUITS) {
            validatesSkinAtlas(recruit);
            validatesGeometryContract(recruit);
            validatesAnimationContract(recruit);
            validatesSpawnEgg(recruit);
        }
        validatesDistinctRecruitAssets();
        validatesDistinctSpawnEggs();
        validatesHeldItemLayerAndProvenance();
        System.out.println("RecruitTextureAtlasTest passed");
    }

    private static void validatesSkinAtlas(String recruit) throws IOException {
        BufferedImage image = ImageIO.read(texture("entity/" + recruit + ".png").toFile());
        assertNotNull(image, recruit + " texture decodes");
        assertEquals(64, image.getWidth(), recruit + " width");
        assertEquals(64, image.getHeight(), recruit + " height");
        for (BoxUv cube : REQUIRED_BASE_CUBES) {
            assertOpaqueBoxFaces(image, cube, recruit + " " + cube.label());
        }
    }

    private static void validatesGeometryContract(String recruit) throws IOException {
        String model = Files.readString(ASSET_ROOT.resolve("geckolib/models/entity/" + recruit + ".geo.json"));
        BufferedImage image = ImageIO.read(texture("entity/" + recruit + ".png").toFile());
        assertContains(model, "\"texture_width\": 64", recruit + " texture width declaration");
        assertContains(model, "\"texture_height\": 64", recruit + " texture height declaration");
        assertContains(model, "\"name\": \"RightHandItem\"", recruit + " right hand bone");
        assertContains(model, "\"name\": \"LeftHandItem\"", recruit + " left hand bone");
        assertContains(model, FACTION_GEOMETRY_MARKERS.get(recruit), recruit + " faction silhouette");
        assertContains(model, "\"mirror\": true", recruit + " mirrored left limbs");
        assertContains(model, "\"uv\": [16, 48], \"mirror\": true", recruit + " mirrored left leg");
        assertNotContains(model, "\"size\": [9, 9, 9]", recruit + " oversized head overlay");
        assertNotContains(model, "\"size\": [9, 13, 5]", recruit + " oversized body overlay");
        assertNotContains(model, "\"size\": [4.5, 12.5, 4.5]", recruit + " fractional limb overlay");
        Matcher cubeUvs = BOX_UV.matcher(model);
        int uvCount = 0;
        while (cubeUvs.find()) {
            int width = (int) Math.ceil(Double.parseDouble(cubeUvs.group(1)));
            int height = (int) Math.ceil(Double.parseDouble(cubeUvs.group(2)));
            int depth = (int) Math.ceil(Double.parseDouble(cubeUvs.group(3)));
            int u = Integer.parseInt(cubeUvs.group(4));
            int v = Integer.parseInt(cubeUvs.group(5));
            BoxUv cube = new BoxUv(u, v, width, height, depth, "cube " + uvCount);
            if (u < 0 || v < 0 || u + 2 * depth + 2 * width > 64 || v + depth + height > 64) {
                throw new AssertionError(recruit + " " + cube.label()
                        + " UV faces extend outside the 64x64 atlas");
            }
            assertOpaqueBoxFaces(image, cube, recruit + " " + cube.label());
            uvCount++;
        }
        if (uvCount < 12) {
            throw new AssertionError(recruit + " must retain all base and overlay UV cubes");
        }
    }

    private static void validatesSpawnEgg(String recruit) throws IOException {
        BufferedImage image = ImageIO.read(texture("item/" + recruit + "_spawn_egg.png").toFile());
        assertNotNull(image, recruit + " spawn egg decodes");
        assertEquals(16, image.getWidth(), recruit + " spawn egg width");
        assertEquals(16, image.getHeight(), recruit + " spawn egg height");
    }

    private static void validatesAnimationContract(String recruit) throws IOException {
        String animation = Files.readString(
                ASSET_ROOT.resolve("geckolib/animations/entity/" + recruit + ".animation.json"));
        for (String bone : List.of("head", "body", "right_arm", "left_arm", "right_leg", "left_leg")) {
            assertContains(animation, "\"" + bone + "\"", recruit + " animation bone " + bone);
        }
        assertContains(animation, "\"misc.idle\"", recruit + " idle animation");
        assertContains(animation, "\"move.walk\"", recruit + " walk animation");
        assertContains(animation, "\"attack.swing\"", recruit + " attack animation");
    }

    private static void validatesHeldItemLayerAndProvenance() throws IOException {
        String renderer = Files.readString(Path.of(
                "src/main/java/middleearth/lotr/warmod/client/render/MiddleEarthRecruitRenderer.java"));
        String notice = Files.readString(Path.of("NOTICE.md"));
        assertContains(renderer, "ItemInHandGeoLayer", "held item render layer");
        assertContains(notice, "Project-Owned Recruit Assets", "project-owned asset notice");
        assertContains(notice, "RecruitTextureAtlasGenerator", "deterministic asset provenance");
    }

    private static void validatesDistinctSpawnEggs() throws IOException {
        for (int first = 0; first < RECRUITS.size(); first++) {
            byte[] firstBytes = Files.readAllBytes(texture("item/" + RECRUITS.get(first) + "_spawn_egg.png"));
            for (int second = first + 1; second < RECRUITS.size(); second++) {
                byte[] secondBytes = Files.readAllBytes(texture("item/" + RECRUITS.get(second) + "_spawn_egg.png"));
                if (java.util.Arrays.equals(firstBytes, secondBytes)) {
                    throw new AssertionError("spawn eggs must be distinct: " + RECRUITS.get(first) + " and " + RECRUITS.get(second));
                }
            }
        }
    }

    private static void validatesDistinctRecruitAssets() throws IOException {
        for (int first = 0; first < RECRUITS.size(); first++) {
            byte[] firstTexture = Files.readAllBytes(texture("entity/" + RECRUITS.get(first) + ".png"));
            byte[] firstModel = Files.readAllBytes(
                    ASSET_ROOT.resolve("geckolib/models/entity/" + RECRUITS.get(first) + ".geo.json"));
            for (int second = first + 1; second < RECRUITS.size(); second++) {
                byte[] secondTexture = Files.readAllBytes(texture("entity/" + RECRUITS.get(second) + ".png"));
                byte[] secondModel = Files.readAllBytes(
                        ASSET_ROOT.resolve("geckolib/models/entity/" + RECRUITS.get(second) + ".geo.json"));
                if (java.util.Arrays.equals(firstTexture, secondTexture)) {
                    throw new AssertionError("recruit textures must be distinct: "
                            + RECRUITS.get(first) + " and " + RECRUITS.get(second));
                }
                if (java.util.Arrays.equals(firstModel, secondModel)) {
                    throw new AssertionError("recruit models must be distinct: "
                            + RECRUITS.get(first) + " and " + RECRUITS.get(second));
                }
            }
        }
    }

    private static void assertOpaqueBoxFaces(BufferedImage image, BoxUv cube, String label) {
        assertOpaque(image, cube.u() + cube.depth(), cube.v(), cube.width(), cube.depth(), label + " top");
        assertOpaque(image, cube.u() + cube.depth() + cube.width(), cube.v(), cube.width(), cube.depth(), label + " bottom");
        assertOpaque(image, cube.u(), cube.v() + cube.depth(), cube.depth(), cube.height(), label + " west");
        assertOpaque(image, cube.u() + cube.depth(), cube.v() + cube.depth(), cube.width(), cube.height(), label + " front");
        assertOpaque(image, cube.u() + cube.depth() + cube.width(), cube.v() + cube.depth(), cube.depth(), cube.height(), label + " east");
        assertOpaque(image, cube.u() + cube.depth() + cube.width() + cube.depth(), cube.v() + cube.depth(), cube.width(), cube.height(), label + " back");
    }

    private static void assertOpaque(BufferedImage image, int x, int y, int width, int height, String label) {
        for (int iy = y; iy < y + height; iy++) {
            for (int ix = x; ix < x + width; ix++) {
                int alpha = (image.getRGB(ix, iy) >>> 24) & 0xFF;
                if (alpha != 255) {
                    throw new AssertionError(label + " has transparent pixel at " + ix + "," + iy);
                }
            }
        }
    }

    private static Path texture(String path) {
        return ASSET_ROOT.resolve("textures").resolve(path);
    }

    private static void assertContains(String value, String expected, String label) {
        if (!value.contains(expected)) {
            throw new AssertionError(label + " missing <" + expected + ">");
        }
    }

    private static void assertNotContains(String value, String unexpected, String label) {
        if (value.contains(unexpected)) {
            throw new AssertionError(label + " still contains <" + unexpected + ">");
        }
    }

    private static void assertEquals(int expected, int actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertNotNull(Object value, String label) {
        if (value == null) {
            throw new AssertionError(label + " was null");
        }
    }

    private record BoxUv(int u, int v, int width, int height, int depth, String label) {
    }
}
