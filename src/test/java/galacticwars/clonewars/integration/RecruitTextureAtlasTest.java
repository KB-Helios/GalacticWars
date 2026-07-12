package galacticwars.clonewars.integration;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

public final class RecruitTextureAtlasTest {
    private static final int ATLAS_SIZE = 128;
    private static final int ARMOR_ATLAS_SIZE = 1024;
    private static final int ARMOR_TEXEL_DENSITY = 6;
    private static final Pattern TEXTURE_WIDTH = Pattern.compile(
            "\\\"texture_width\\\"\\s*:\\s*([0-9]+)");
    private static final Pattern TEXTURE_HEIGHT = Pattern.compile(
            "\\\"texture_height\\\"\\s*:\\s*([0-9]+)");
    private static final Pattern CUBE = Pattern.compile(
            "\\\"size\\\"\\s*:\\s*\\[\\s*([0-9.]+)\\s*,\\s*([0-9.]+)\\s*,\\s*([0-9.]+)\\s*]"
                    + "\\s*,\\s*\\\"uv\\\"\\s*:\\s*",
            Pattern.DOTALL);
    private static final Pattern BOX_UV = Pattern.compile(
            "\\[\\s*([0-9]+)\\s*,\\s*([0-9]+)\\s*]");
    private static final Path ASSET_ROOT = Path.of("src/main/resources/assets/galacticwars");
    private static final List<String> RECRUITS = List.of(
            "clone_trooper", "arc_trooper", "jedi_knight",
            "b1_battle_droid", "b2_super_battle_droid", "commando_droid",
            "mandalorian_warrior", "mandalorian_marksman", "mandalorian_heavy",
            "hutt_enforcer", "bounty_hunter", "smuggler",
            "nightsister_acolyte", "nightsister_archer", "nightbrother_brute",
            "republic_civilian", "separatist_technician", "mandalorian_clansperson",
            "hutt_civilian", "nightsister_civilian");
    private static final List<String> ARMOR_FAMILIES = List.of(
            "mandalorian_alloy", "nightsister_weave", "republic_plastoid",
            "separatist_alloy", "beskar");
    private static final List<String> HUMANOID_BONES = List.of(
            "head", "body", "right_arm", "left_arm", "right_leg", "left_leg",
            "RightHandItem", "LeftHandItem");
    private static final List<String> ARMOR_BONES = List.of(
            "armorHead", "armorBody", "armorRightArm", "armorLeftArm",
            "armorRightLeg", "armorLeftLeg", "armorRightBoot", "armorLeftBoot");

    private RecruitTextureAtlasTest() {
    }

    public static void main(String[] args) throws IOException {
        for (String recruit : RECRUITS) {
            validatesRecruitAssetSet(recruit);
        }
        for (String family : ARMOR_FAMILIES) {
            validatesArmorAssetSet(family);
        }
        validatesDistinctAssets(RECRUITS, "textures/entity/", ".png", "recruit textures");
        validatesDistinctAssets(RECRUITS, "textures/item/", "_spawn_egg.png", "spawn eggs");
        validatesDistinctAssets(ARMOR_FAMILIES, "textures/armor/", ".png", "armor textures");
        validatesRendererAndProvenance();
        System.out.println("RecruitTextureAtlasTest passed");
    }

    private static void validatesRecruitAssetSet(String recruit) throws IOException {
        Path texturePath = ASSET_ROOT.resolve("textures/entity/" + recruit + ".png");
        Path modelPath = ASSET_ROOT.resolve("geckolib/models/entity/" + recruit + ".geo.json");
        BufferedImage image = readAtlas(texturePath, recruit, ATLAS_SIZE);
        String geometry = geometry(modelPath, ATLAS_SIZE);
        validatesGeometry(recruit, geometry, image, HUMANOID_BONES, 12, 1);

        String animation = Files.readString(
                ASSET_ROOT.resolve("geckolib/animations/entity/" + recruit + ".animation.json"));
        for (String bone : HUMANOID_BONES.subList(0, 6)) {
            assertContains(animation, '"' + bone + '"', recruit + " animation bone " + bone);
        }
        assertContains(animation, "\"misc.idle\"", recruit + " idle animation");
        assertContains(animation, "\"move.walk\"", recruit + " walk animation");
        assertContains(animation, "\"attack.swing\"", recruit + " attack animation");

        BufferedImage egg = ImageIO.read(ASSET_ROOT.resolve(
                "textures/item/" + recruit + "_spawn_egg.png").toFile());
        assertNotNull(egg, recruit + " spawn egg decodes");
        assertEquals(16, egg.getWidth(), recruit + " spawn egg width");
        assertEquals(16, egg.getHeight(), recruit + " spawn egg height");
        assertRegularFile(ASSET_ROOT.resolve("items/" + recruit + "_spawn_egg.json"));
        assertRegularFile(ASSET_ROOT.resolve("models/item/" + recruit + "_spawn_egg.json"));
    }

    private static void validatesArmorAssetSet(String family) throws IOException {
        Path texturePath = ASSET_ROOT.resolve("textures/armor/" + family + ".png");
        Path modelPath = ASSET_ROOT.resolve("geckolib/models/armor/" + family + ".geo.json");
        BufferedImage image = readAtlas(texturePath, family + " armor", ARMOR_ATLAS_SIZE);
        String geometry = geometry(modelPath, ARMOR_ATLAS_SIZE);
        validatesGeometry(
                family + " armor",
                geometry,
                image,
                ARMOR_BONES,
                32,
                ARMOR_TEXEL_DENSITY
        );
        assertMinimumOpaqueColors(image, 48, family + " high-detail material variation");
        assertArmorBoneParent(geometry, "armorHead", "bipedHead", family);
        assertArmorBoneParent(geometry, "armorBody", "bipedBody", family);
        assertArmorBoneParent(geometry, "armorRightArm", "bipedRightArm", family);
        assertArmorBoneParent(geometry, "armorLeftArm", "bipedLeftArm", family);
        assertArmorBoneParent(geometry, "armorRightLeg", "bipedRightLeg", family);
        assertArmorBoneParent(geometry, "armorRightBoot", "bipedRightLeg", family);
        assertArmorBoneParent(geometry, "armorLeftLeg", "bipedLeftLeg", family);
        assertArmorBoneParent(geometry, "armorLeftBoot", "bipedLeftLeg", family);
        String animation = Files.readString(ASSET_ROOT.resolve(
                "geckolib/animations/armor/" + family + ".animation.json"));
        assertContains(animation, "\"armor.pose\"", family + " valid no-op armor animation");
        if (animation.matches("(?s).*\"animations\"\\s*:\\s*\\{\\s*}.*")) {
            throw new AssertionError(family + " armor animation table must not be empty");
        }
    }

    private static void assertArmorBoneParent(
            String geometry,
            String bone,
            String parent,
            String family
    ) {
        Pattern parentedBone = Pattern.compile(
                "\\\"name\\\"\\s*:\\s*\\\"" + Pattern.quote(bone)
                        + "\\\"(?s:.*?)\\\"parent\\\"\\s*:\\s*\\\""
                        + Pattern.quote(parent) + "\\\"");
        if (!parentedBone.matcher(geometry).find()) {
            throw new AssertionError(family + " " + bone + " must be parented to " + parent);
        }
    }

    private static BufferedImage readAtlas(Path path, String label, int expectedSize) throws IOException {
        BufferedImage image = ImageIO.read(path.toFile());
        assertNotNull(image, label + " texture decodes");
        assertEquals(expectedSize, image.getWidth(), label + " width");
        assertEquals(expectedSize, image.getHeight(), label + " height");
        boolean transparentPixel = false;
        boolean opaquePixel = false;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                transparentPixel |= alpha == 0;
                opaquePixel |= alpha == 255;
            }
        }
        if (!transparentPixel || !opaquePixel) {
            throw new AssertionError(label + " atlas must contain painted faces and transparent unused space");
        }
        return image;
    }

    private static String geometry(Path path, int expectedSize) throws IOException {
        String model = Files.readString(path);
        assertPatternValue(TEXTURE_WIDTH, model, expectedSize, path + " texture width");
        assertPatternValue(TEXTURE_HEIGHT, model, expectedSize, path + " texture height");
        return model;
    }

    private static void validatesGeometry(
            String label,
            String geometry,
            BufferedImage image,
            List<String> requiredBones,
            int minimumCubeCount,
            int texelDensity
    ) {
        Set<String> missing = new HashSet<>();
        for (String requiredBone : requiredBones) {
            if (!geometry.contains("\"name\": \"" + requiredBone + "\"")) {
                missing.add(requiredBone);
            }
        }
        if (!missing.isEmpty()) {
            throw new AssertionError(label + " missing required bones " + missing);
        }
        Matcher cubes = CUBE.matcher(geometry);
        int cubeCount = 0;
        while (cubes.find()) {
            int width = (int) Math.ceil(Double.parseDouble(cubes.group(1)));
            int height = (int) Math.ceil(Double.parseDouble(cubes.group(2)));
            int depth = (int) Math.ceil(Double.parseDouble(cubes.group(3)));
            int uvStart = skipWhitespace(geometry, cubes.end());
            if (texelDensity == 1) {
                Matcher origin = BOX_UV.matcher(geometry);
                origin.region(uvStart, geometry.length());
                if (!origin.lookingAt()) {
                    throw new AssertionError(label + " cube " + cubeCount + " must use box UVs");
                }
                BoxUv box = new BoxUv(
                        Integer.parseInt(origin.group(1)), Integer.parseInt(origin.group(2)),
                        width, height, depth);
                if (box.u() < 0 || box.v() < 0
                        || box.u() + 2 * box.depth() + 2 * box.width() > image.getWidth()
                        || box.v() + box.depth() + box.height() > image.getHeight()) {
                    throw new AssertionError(label + " cube " + cubeCount + " extends outside its atlas");
                }
                assertOpaqueBoxFaces(image, box, label + " cube " + cubeCount);
            } else {
                if (uvStart >= geometry.length() || geometry.charAt(uvStart) != '{') {
                    throw new AssertionError(label + " cube " + cubeCount
                            + " must use high-density per-face UVs");
                }
                int uvEnd = matchingBrace(geometry, uvStart);
                assertOpaquePerFaceUvs(
                        image,
                        geometry.substring(uvStart, uvEnd + 1),
                        width,
                        height,
                        depth,
                        texelDensity,
                        label + " cube " + cubeCount
                );
            }
            cubeCount++;
        }
        if (cubeCount < minimumCubeCount) {
            throw new AssertionError(label + " has only " + cubeCount
                    + " cubes; expected at least " + minimumCubeCount + " for a detailed silhouette");
        }
    }

    private static void assertOpaquePerFaceUvs(
            BufferedImage image,
            String uv,
            int width,
            int height,
            int depth,
            int density,
            String label
    ) {
        assertOpaqueFace(image, uv, "north", width * density, height * density, label);
        assertOpaqueFace(image, uv, "south", width * density, height * density, label);
        assertOpaqueFace(image, uv, "east", depth * density, height * density, label);
        assertOpaqueFace(image, uv, "west", depth * density, height * density, label);
        assertOpaqueFace(image, uv, "up", width * density, depth * density, label);
        assertOpaqueFace(image, uv, "down", width * density, depth * density, label);
    }

    private static void assertOpaqueFace(
            BufferedImage image,
            String uv,
            String faceName,
            int expectedWidth,
            int expectedHeight,
            String label
    ) {
        Pattern facePattern = Pattern.compile(
                "\\\"" + Pattern.quote(faceName) + "\\\"\\s*:\\s*\\{\\s*"
                        + "\\\"uv\\\"\\s*:\\s*\\[\\s*([0-9]+)\\s*,\\s*([0-9]+)\\s*]\\s*,\\s*"
                        + "\\\"uv_size\\\"\\s*:\\s*\\[\\s*([0-9]+)\\s*,\\s*([0-9]+)\\s*]",
                Pattern.DOTALL);
        Matcher face = facePattern.matcher(uv);
        if (!face.find()) {
            throw new AssertionError(label + " missing " + faceName + " UV mapping");
        }
        int x = Integer.parseInt(face.group(1));
        int y = Integer.parseInt(face.group(2));
        int width = Integer.parseInt(face.group(3));
        int height = Integer.parseInt(face.group(4));
        assertEquals(expectedWidth, width, label + " " + faceName + " UV width");
        assertEquals(expectedHeight, height, label + " " + faceName + " UV height");
        if (x < 0 || y < 0 || x + width > image.getWidth() || y + height > image.getHeight()) {
            throw new AssertionError(label + " " + faceName + " extends outside its atlas");
        }
        assertOpaque(image, x, y, width, height, label + " " + faceName);
    }

    private static int skipWhitespace(String value, int index) {
        while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
            index++;
        }
        return index;
    }

    private static int matchingBrace(String value, int openingBrace) {
        int depth = 0;
        boolean insideString = false;
        boolean escaped = false;
        for (int index = openingBrace; index < value.length(); index++) {
            char character = value.charAt(index);
            if (insideString) {
                if (escaped) {
                    escaped = false;
                } else if (character == '\\') {
                    escaped = true;
                } else if (character == '"') {
                    insideString = false;
                }
                continue;
            }
            if (character == '"') {
                insideString = true;
            } else if (character == '{') {
                depth++;
            } else if (character == '}' && --depth == 0) {
                return index;
            }
        }
        throw new AssertionError("Unclosed per-face UV object at character " + openingBrace);
    }

    private static void assertMinimumOpaqueColors(BufferedImage image, int minimum, String label) {
        Set<Integer> colors = new HashSet<>();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int color = image.getRGB(x, y);
                if ((color >>> 24) == 255) {
                    colors.add(color & 0x00ffffff);
                }
            }
        }
        if (colors.size() < minimum) {
            throw new AssertionError(label + " expected at least " + minimum
                    + " opaque colors but found " + colors.size());
        }
    }

    private static void validatesRendererAndProvenance() throws IOException {
        String recruitRenderer = Files.readString(Path.of(
                "src/main/java/galacticwars/clonewars/client/render/GalacticRecruitRenderer.java"));
        String armorItem = Files.readString(Path.of(
                "src/main/java/galacticwars/clonewars/item/GalacticArmorItem.java"));
        String items = Files.readString(Path.of(
                "src/main/java/galacticwars/clonewars/registry/ModItems.java"));
        String entities = Files.readString(Path.of(
                "src/main/java/galacticwars/clonewars/registry/ModEntityTypes.java"));
        String provenance = Files.readString(Path.of("docs/galacticwars-asset-provenance.md"));
        assertContains(recruitRenderer, "ItemInHandGeoLayer", "held item render layer");
        assertContains(armorItem, "implements GeoItem", "custom equipped armor item");
        assertContains(armorItem, "getGeoArmorRenderer", "custom equipped armor provider");
        assertContains(items, "new GalacticArmorItem", "armor registry uses custom geometry");
        assertNotContains(entities, "renderAlias", "civilian renderer aliases");
        assertContains(provenance, "generate_character_models.py", "character model provenance");
    }

    private static void validatesDistinctAssets(
            List<String> ids,
            String prefix,
            String suffix,
            String label
    ) throws IOException {
        Set<Integer> hashes = new HashSet<>();
        for (String id : ids) {
            hashes.add(java.util.Arrays.hashCode(Files.readAllBytes(ASSET_ROOT.resolve(prefix + id + suffix))));
        }
        assertEquals(ids.size(), hashes.size(), label + " distinct hashes");
    }

    private static void assertOpaqueBoxFaces(BufferedImage image, BoxUv cube, String label) {
        assertOpaque(image, cube.u() + cube.depth(), cube.v(), cube.width(), cube.depth(), label + " top");
        assertOpaque(image, cube.u() + cube.depth() + cube.width(), cube.v(), cube.width(), cube.depth(), label + " bottom");
        assertOpaque(image, cube.u(), cube.v() + cube.depth(), cube.depth(), cube.height(), label + " west");
        assertOpaque(image, cube.u() + cube.depth(), cube.v() + cube.depth(), cube.width(), cube.height(), label + " front");
        assertOpaque(image, cube.u() + cube.depth() + cube.width(), cube.v() + cube.depth(), cube.depth(), cube.height(), label + " east");
        assertOpaque(image, cube.u() + cube.depth() * 2 + cube.width(), cube.v() + cube.depth(), cube.width(), cube.height(), label + " back");
    }

    private static void assertOpaque(BufferedImage image, int x, int y, int width, int height, String label) {
        for (int iy = y; iy < y + height; iy++) {
            for (int ix = x; ix < x + width; ix++) {
                if ((image.getRGB(ix, iy) >>> 24) != 255) {
                    throw new AssertionError(label + " has transparent pixel at " + ix + "," + iy);
                }
            }
        }
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

    private static void assertRegularFile(Path path) {
        if (!Files.isRegularFile(path)) {
            throw new AssertionError("missing asset <" + path + ">");
        }
    }

    private static void assertEquals(int expected, int actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertPatternValue(
            Pattern pattern,
            String value,
            int expected,
            String label
    ) {
        Matcher matcher = pattern.matcher(value);
        if (!matcher.find()) {
            throw new AssertionError(label + " declaration missing");
        }
        assertEquals(expected, Integer.parseInt(matcher.group(1)), label);
    }

    private static void assertNotNull(Object value, String label) {
        if (value == null) {
            throw new AssertionError(label + " was null");
        }
    }

    private record BoxUv(int u, int v, int width, int height, int depth) {
    }
}
