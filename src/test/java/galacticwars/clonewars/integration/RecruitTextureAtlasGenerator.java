package galacticwars.clonewars.integration;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * Deterministically generates the original grounded vanilla-plus recruit art.
 *
 * <p>Every faction owns a distinct material language and silhouette treatment while retaining
 * a 64x64 wide-arm humanoid atlas. The generated pixels are the distributable source of truth;
 * high-resolution concept references are used only to establish shapes and palettes.</p>
 */
public final class RecruitTextureAtlasGenerator {
    private static final Pattern BOX_UV = Pattern.compile(
            "\\\"size\\\": \\[([0-9.]+), ([0-9.]+), ([0-9.]+)], \\\"uv\\\": \\[([0-9]+), ([0-9]+)]");
    private static final Path ENTITY_TEXTURES = Path.of(
            "src/main/resources/assets/galacticwars/textures/entity");
    private static final Path ITEM_TEXTURES = Path.of(
            "src/main/resources/assets/galacticwars/textures/item");

    private static final List<RecruitDesign> DESIGNS = List.of(
            new RecruitDesign("clone_trooper", 0xC89570, 0x936548, 0x171B22,
                    0xE7E8E3, 0x9299A2, 0x202631, 0x243D72, 0x20242A),
            new RecruitDesign("arc_trooper", 0xC89570, 0x936548, 0x171B22,
                    0xECEBE4, 0x8B929B, 0x1A2636, 0x1864C0, 0x20242A),
            new RecruitDesign("jedi_knight", 0xB87A4A, 0x81502F, 0x4B2C18,
                    0xD8BA78, 0x8E6538, 0x5A3A24, 0x64E64D, 0x35261E),
            new RecruitDesign("b1_battle_droid", 0xC7A46B, 0x82683F, 0x28251F,
                    0xC7A46B, 0x786241, 0x39342A, 0xE09535, 0x24231F),
            new RecruitDesign("b2_super_battle_droid", 0x5C626A, 0x30343A, 0x17191C,
                    0x555B63, 0x292D33, 0x37322C, 0xEF941E, 0x202329),
            new RecruitDesign("commando_droid", 0x30343A, 0x17191D, 0x101214,
                    0x34383D, 0x171A1D, 0x202328, 0xD62F32, 0x151719),
            new RecruitDesign("mandalorian_warrior", 0xC18A5C, 0x86583A, 0x27231F,
                    0x8A918D, 0x454C4A, 0x24464A, 0xE0A332, 0x302A26),
            new RecruitDesign("mandalorian_marksman", 0xC18A5C, 0x86583A, 0x30251E,
                    0xB4A47F, 0x635C4C, 0x34312D, 0xD96718, 0x302A26),
            new RecruitDesign("mandalorian_heavy", 0xC18A5C, 0x86583A, 0x202329,
                    0x526A86, 0x283846, 0x20262E, 0x64D7F0, 0x20242A),
            new RecruitDesign("hutt_enforcer", 0x788044, 0x4B522B, 0x28251F,
                    0x67713A, 0x343A24, 0x2B2925, 0xA46D35, 0x29251F),
            new RecruitDesign("bounty_hunter", 0xB0784E, 0x79482F, 0x382819,
                    0x5C5A52, 0x302E2A, 0x433225, 0xE0921C, 0x2A2520),
            new RecruitDesign("smuggler", 0xC98A58, 0x8E5937, 0x4A2D1A,
                    0xD1BD8E, 0x75654C, 0x9A421F, 0x4B8FC4, 0x49301F),
            new RecruitDesign("nightsister_acolyte", 0xC9C0B8, 0x817A75, 0x18181B,
                    0x80252C, 0x3A191E, 0x202025, 0x71E78A, 0x292326),
            new RecruitDesign("nightsister_archer", 0xC9C0B8, 0x817A75, 0x16171A,
                    0x67222A, 0x32171D, 0x242126, 0x78E7A0, 0x282329),
            new RecruitDesign("nightbrother_brute", 0xB52B27, 0x721B1B, 0x211A18,
                    0x6B3329, 0x34221E, 0x211D1C, 0xE06431, 0x27201D));

    private RecruitTextureAtlasGenerator() {
    }

    public static void main(String[] args) throws IOException {
        Files.createDirectories(ENTITY_TEXTURES);
        Files.createDirectories(ITEM_TEXTURES);
        for (RecruitDesign design : DESIGNS) {
            writeSkin(design);
            writeSpawnEgg(design);
        }
        System.out.println("RecruitTextureAtlasGenerator generated " + DESIGNS.size() + " recruit sets");
    }

    private static void writeSkin(RecruitDesign design) throws IOException {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        drawBaseAtlas(image, design);
        switch (design.id()) {
            case "clone_trooper", "arc_trooper", "jedi_knight" -> paintRepublic(image, design);
            case "mandalorian_warrior", "mandalorian_marksman", "mandalorian_heavy" ->
                    paintMandalorian(image, design);
            case "b1_battle_droid", "b2_super_battle_droid", "commando_droid" ->
                    paintSeparatistOrc(image, design);
            case "hutt_enforcer", "bounty_hunter", "smuggler" -> paintHuttCartel(image, design);
            case "nightsister_acolyte", "nightsister_archer", "nightbrother_brute" ->
                    paintNightsister(image, design);
            default -> throw new IllegalStateException("Unknown recruit design " + design.id());
        }
        clearUnusedUv(image, design.id());
        ImageIO.write(image, "PNG", ENTITY_TEXTURES.resolve(design.id() + ".png").toFile());
    }

    private static void clearUnusedUv(BufferedImage image, String id) throws IOException {
        boolean[][] used = new boolean[64][64];
        markBoxUv(used, 0, 0, 8, 8, 8);
        markBoxUv(used, 16, 16, 8, 12, 4);
        markBoxUv(used, 40, 16, 4, 12, 4);
        markBoxUv(used, 32, 48, 4, 12, 4);
        markBoxUv(used, 0, 16, 4, 12, 4);
        markBoxUv(used, 16, 48, 4, 12, 4);
        String model = Files.readString(Path.of(
                "src/main/resources/assets/galacticwars/geckolib/models/entity/" + id + ".geo.json"));
        Matcher cubes = BOX_UV.matcher(model);
        while (cubes.find()) {
            int width = (int) Math.ceil(Double.parseDouble(cubes.group(1)));
            int height = (int) Math.ceil(Double.parseDouble(cubes.group(2)));
            int depth = (int) Math.ceil(Double.parseDouble(cubes.group(3)));
            int u = Integer.parseInt(cubes.group(4));
            int v = Integer.parseInt(cubes.group(5));
            markBoxUv(used, u, v, width, height, depth);
        }
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                if (!used[y][x]) {
                    image.setRGB(x, y, 0);
                }
            }
        }
    }

    private static void markBoxUv(boolean[][] used, int u, int v, int width, int height, int depth) {
        for (int y = v; y < Math.min(64, v + depth + height); y++) {
            for (int x = u; x < Math.min(64, u + 2 * depth + 2 * width); x++) {
                used[y][x] = true;
            }
        }
    }

    private static void drawBaseAtlas(BufferedImage image, RecruitDesign design) {
        textureRect(image, 0, 0, 64, 64,
                shade(design.armorShade(), -18), design.armorShade());
        drawBox(image, 0, 0, 8, 8, 8,
                design.hair(), design.skinShade(), design.skinShade(), design.skin(), design.hair());
        drawBox(image, 16, 16, 8, 12, 4,
                design.armor(), design.armorShade(), design.armorShade(), design.cloth(), design.armorShade());
        drawBox(image, 40, 16, 4, 12, 4,
                design.armor(), design.skinShade(), design.armorShade(), design.armor(), design.armorShade());
        drawBox(image, 32, 48, 4, 12, 4,
                design.armor(), design.skinShade(), design.armorShade(), design.armor(), design.armorShade());
        drawBox(image, 0, 16, 4, 12, 4,
                design.cloth(), design.boot(), design.boot(), design.cloth(), design.boot());
        drawBox(image, 16, 48, 4, 12, 4,
                design.cloth(), design.boot(), design.boot(), design.cloth(), design.boot());

        drawBox(image, 32, 0, 8, 8, 8,
                design.armor(), design.armorShade(), design.armorShade(), design.armor(), design.armorShade());
        drawBox(image, 16, 32, 8, 12, 4,
                design.armor(), design.armorShade(), design.armorShade(), design.armor(), design.armorShade());
        drawBox(image, 40, 32, 4, 12, 4,
                design.armor(), design.armorShade(), design.armorShade(), design.armor(), design.armorShade());
        drawBox(image, 48, 48, 4, 12, 4,
                design.armor(), design.armorShade(), design.armorShade(), design.armor(), design.armorShade());
        drawBox(image, 0, 32, 4, 12, 4,
                design.cloth(), design.boot(), design.boot(), design.cloth(), design.boot());
        drawBox(image, 0, 48, 4, 12, 4,
                design.cloth(), design.boot(), design.boot(), design.cloth(), design.boot());

        textureRect(image, 20, 20, 8, 12, design.cloth(), shade(design.cloth(), 18));
        textureRect(image, 20, 36, 8, 12, design.armor(), shade(design.armor(), -20));
        textureRect(image, 44, 20, 4, 12, design.armor(), design.armorShade());
        textureRect(image, 36, 52, 4, 12, design.armor(), design.armorShade());
        textureRect(image, 4, 20, 4, 12, design.cloth(), design.boot());
        textureRect(image, 20, 52, 4, 12, design.cloth(), design.boot());
    }

    private static void paintRepublic(BufferedImage image, RecruitDesign design) {
        paintHumanFace(image, design, 0x252A2D);
        paintHelmet(image, design, true);
        textureRect(image, 20, 36, 8, 12, 0x252B34, 0x161A20);
        fill(image, 20, 45, 8, 2, 0x11151A);
        fill(image, 23, 38, 2, 8, design.accent());
        set(image, 22, 40, design.accent());
        set(image, 25, 40, design.accent());
        set(image, 21, 42, design.accent());
        set(image, 26, 42, design.accent());
        set(image, 22, 44, design.accent());
        set(image, 25, 44, design.accent());
        paintChain(image, 44, 20, 4, 8, design.armor(), design.armorShade());
        paintChain(image, 36, 52, 4, 8, design.armor(), design.armorShade());
        paintBootsAndGloves(image, design, 0x4B3A2D);
        fill(image, 20, 29, 8, 2, design.armorShade());
        fill(image, 20, 61, 4, 3, 0x27221F);
        fill(image, 4, 29, 4, 3, 0x27221F);
    }

    private static void paintMandalorian(BufferedImage image, RecruitDesign design) {
        paintHumanFace(image, design, 0x2A2925);
        paintHelmet(image, design, false);
        fill(image, 42, 8, 4, 2, design.accent());
        fill(image, 44, 4, 2, 6, design.hair());
        textureRect(image, 20, 36, 8, 12, 0x304A31, 0x1F3323);
        fill(image, 20, 36, 8, 2, design.accent());
        fill(image, 20, 45, 8, 2, 0x4A3023);
        paintChain(image, 20, 20, 8, 8, 0x7E8581, 0x464B49);
        fill(image, 20, 28, 8, 4, 0x293D2B);
        fill(image, 21, 39, 6, 1, design.accent());
        fill(image, 21, 43, 6, 1, 0x65452C);
        paintLeatherStraps(image, design);
        paintBootsAndGloves(image, design, 0x493124);
    }

    private static void paintSeparatistOrc(BufferedImage image, RecruitDesign design) {
        paintOrcFace(image, design);
        fill(image, 40, 0, 8, 8, 0x323238);
        fill(image, 32, 8, 4, 4, 0x1E1E22);
        fill(image, 40, 8, 8, 4, 0x3C3B40);
        fill(image, 46, 8, 2, 4, 0x202025);
        set(image, 41, 9, 0x77777B);
        set(image, 44, 10, 0x77777B);
        textureRect(image, 20, 36, 8, 12, 0x2B292C, 0x181719);
        fill(image, 20, 44, 8, 4, design.accent());
        fill(image, 24, 44, 4, 4, shade(design.accent(), -24));
        fill(image, 20, 38, 2, 6, 0x55545A);
        fill(image, 25, 37, 3, 3, 0x55545A);
        paintRivets(image, 20, 36, 8, 8, 0x85858A);
        fill(image, 44, 20, 4, 7, 0x55545A);
        fill(image, 36, 52, 4, 5, 0x242326);
        fill(image, 37, 52, 3, 2, 0x626166);
        paintBootsAndGloves(image, design, 0x211A19);
    }

    private static void paintHuttCartel(BufferedImage image, RecruitDesign design) {
        paintHumanFace(image, design, 0x24282A);
        fill(image, 9, 13, 6, 3, design.hair());
        fill(image, 10, 11, 4, 5, shade(design.hair(), 18));
        set(image, 9, 15, shade(design.hair(), -18));
        set(image, 14, 15, shade(design.hair(), -18));
        paintHelmet(image, design, true);
        fill(image, 41, 8, 6, 2, design.accent());
        textureRect(image, 20, 36, 8, 12, 0x1B4548, 0x123336);
        fill(image, 20, 36, 8, 2, design.accent());
        fill(image, 20, 45, 8, 3, design.hair());
        fill(image, 22, 39, 1, 8, shade(design.hair(), 20));
        fill(image, 25, 39, 1, 8, shade(design.hair(), -18));
        paintChain(image, 44, 20, 4, 8, design.armor(), design.armorShade());
        paintChain(image, 36, 52, 4, 8, design.armor(), design.armorShade());
        fill(image, 44, 20, 4, 2, design.accent());
        fill(image, 36, 52, 4, 2, design.accent());
        paintBootsAndGloves(image, design, 0x3A2B23);
    }

    private static void paintNightsister(BufferedImage image, RecruitDesign design) {
        paintHumanFace(image, design, 0x31533A);
        fill(image, 8, 8, 1, 5, design.hair());
        fill(image, 15, 8, 1, 6, design.hair());
        fill(image, 9, 8, 6, 2, shade(design.hair(), 18));
        fill(image, 40, 0, 8, 8, design.hair());
        fill(image, 32, 8, 4, 7, shade(design.hair(), -18));
        fill(image, 40, 8, 8, 3, design.hair());
        fill(image, 48, 8, 4, 7, shade(design.hair(), 10));
        textureRect(image, 20, 36, 8, 12, 0x294834, 0x1B3325);
        fill(image, 20, 36, 8, 1, design.accent());
        fill(image, 20, 46, 8, 2, 0x1B2C21);
        fill(image, 23, 38, 2, 7, design.accent());
        set(image, 22, 40, design.accent());
        set(image, 25, 40, design.accent());
        set(image, 21, 42, shade(design.accent(), -20));
        set(image, 26, 42, shade(design.accent(), -20));
        fill(image, 44, 20, 4, 2, design.accent());
        fill(image, 36, 52, 4, 2, design.accent());
        paintBootsAndGloves(image, design, 0x342A22);
    }

    private static void paintHumanFace(BufferedImage image, RecruitDesign design, int eyeColor) {
        int faceX = 8;
        int faceY = 8;
        fill(image, faceX, faceY, 8, 2, design.hair());
        set(image, faceX + 2, faceY + 4, eyeColor);
        set(image, faceX + 5, faceY + 4, eyeColor);
        set(image, faceX + 3, faceY + 6, design.skinShade());
        set(image, faceX + 4, faceY + 6, design.skinShade());
        set(image, faceX + 1, faceY + 5, shade(design.skin(), -8));
        set(image, faceX + 6, faceY + 5, shade(design.skin(), -8));
    }

    private static void paintOrcFace(BufferedImage image, RecruitDesign design) {
        int faceX = 8;
        int faceY = 8;
        fill(image, faceX, faceY, 8, 2, 0x25221F);
        set(image, faceX + 2, faceY + 4, 0x852A25);
        set(image, faceX + 5, faceY + 4, 0x852A25);
        fill(image, faceX + 2, faceY + 6, 4, 1, design.skinShade());
        set(image, faceX + 2, faceY + 7, 0xD7C7A2);
        set(image, faceX + 5, faceY + 7, 0xD7C7A2);
        set(image, faceX + 1, faceY + 3, shade(design.skin(), -22));
        set(image, faceX + 6, faceY + 3, shade(design.skin(), -22));
    }

    private static void paintHelmet(BufferedImage image, RecruitDesign design, boolean brightCrown) {
        int crown = brightCrown ? design.armor() : shade(design.armor(), -8);
        fill(image, 40, 0, 8, 8, crown);
        fill(image, 32, 8, 4, 3, design.armorShade());
        fill(image, 40, 8, 8, 3, crown);
        fill(image, 48, 8, 4, 3, design.armorShade());
        fill(image, 52, 8, 8, 3, design.armorShade());
        fill(image, 43, 8, 2, 4, shade(crown, 24));
        for (int x = 40; x < 48; x += 2) {
            set(image, x, 10, design.accent());
        }
    }

    private static void paintLeatherStraps(BufferedImage image, RecruitDesign design) {
        for (int y = 36; y < 46; y++) {
            int x = 20 + Math.floorMod(y - 36, 6);
            set(image, x, y, 0x61422E);
            if (x + 1 < 28) {
                set(image, x + 1, y, shade(0x61422E, 15));
            }
        }
        set(image, 24, 41, design.accent());
    }

    private static void paintChain(
            BufferedImage image,
            int x,
            int y,
            int width,
            int height,
            int light,
            int dark
    ) {
        for (int iy = 0; iy < height; iy++) {
            for (int ix = 0; ix < width; ix++) {
                set(image, x + ix, y + iy, ((ix + iy) & 1) == 0 ? light : dark);
            }
        }
    }

    private static void paintRivets(
            BufferedImage image,
            int x,
            int y,
            int width,
            int height,
            int color
    ) {
        for (int iy = 1; iy < height; iy += 3) {
            for (int ix = 1; ix < width; ix += 3) {
                set(image, x + ix, y + iy, color);
            }
        }
    }

    private static void paintBootsAndGloves(BufferedImage image, RecruitDesign design, int leather) {
        fill(image, 44, 28, 4, 4, leather);
        fill(image, 36, 60, 4, 4, leather);
        fill(image, 4, 28, 4, 4, design.boot());
        fill(image, 20, 60, 4, 4, design.boot());
        fill(image, 4, 28, 4, 1, shade(leather, 18));
        fill(image, 20, 60, 4, 1, shade(design.boot(), 18));
    }

    private static void writeSpawnEgg(RecruitDesign design) throws IOException {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 1; y < 15; y++) {
            double normalizedY = (y - 8.0) / 7.0;
            int halfWidth = Math.max(1,
                    (int) Math.round(5.0 * Math.sqrt(Math.max(0.0, 1.0 - normalizedY * normalizedY))));
            for (int x = 8 - halfWidth; x <= 8 + halfWidth; x++) {
                int base = y < 6 ? design.armor() : design.armorShade();
                set(image, x, y, ((x + y) & 3) == 0 ? shade(base, 16) : base);
            }
        }
        set(image, 6, 4, design.accent());
        set(image, 9, 6, design.accent());
        set(image, 5, 9, design.cloth());
        set(image, 10, 11, design.accent());
        set(image, 7, 2, 0xFFFFFF);
        ImageIO.write(image, "PNG", ITEM_TEXTURES.resolve(design.id() + "_spawn_egg.png").toFile());
    }

    private static void drawBox(
            BufferedImage image,
            int u,
            int v,
            int width,
            int height,
            int depth,
            int top,
            int bottom,
            int side,
            int front,
            int back
    ) {
        fill(image, u + depth, v, width, depth, top);
        fill(image, u + depth + width, v, width, depth, bottom);
        fill(image, u, v + depth, depth, height, side);
        fill(image, u + depth, v + depth, width, height, front);
        fill(image, u + depth + width, v + depth, depth, height, side);
        fill(image, u + depth + width + depth, v + depth, width, height, back);
    }

    private static void textureRect(BufferedImage image, int x, int y, int width, int height, int base, int detail) {
        for (int iy = 0; iy < height; iy++) {
            for (int ix = 0; ix < width; ix++) {
                set(image, x + ix, y + iy, ((ix * 3 + iy * 5) & 3) == 0 ? detail : base);
            }
        }
    }

    private static void fill(BufferedImage image, int x, int y, int width, int height, int color) {
        for (int iy = y; iy < y + height; iy++) {
            for (int ix = x; ix < x + width; ix++) {
                set(image, ix, iy, color);
            }
        }
    }

    private static void set(BufferedImage image, int x, int y, int color) {
        image.setRGB(x, y, 0xFF000000 | color);
    }

    private static int shade(int color, int delta) {
        int red = Math.max(0, Math.min(255, ((color >>> 16) & 0xFF) + delta));
        int green = Math.max(0, Math.min(255, ((color >>> 8) & 0xFF) + delta));
        int blue = Math.max(0, Math.min(255, (color & 0xFF) + delta));
        return red << 16 | green << 8 | blue;
    }

    private record RecruitDesign(
            String id,
            int skin,
            int skinShade,
            int hair,
            int armor,
            int armorShade,
            int cloth,
            int accent,
            int boot
    ) {
    }
}
