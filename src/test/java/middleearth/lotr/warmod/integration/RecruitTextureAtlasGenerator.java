package middleearth.lotr.warmod.integration;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Deterministically generates the original grounded vanilla-plus recruit art.
 *
 * <p>Every faction owns a distinct material language and silhouette treatment while retaining
 * a 64x64 wide-arm humanoid atlas. The generated pixels are the distributable source of truth;
 * high-resolution concept references are used only to establish shapes and palettes.</p>
 */
public final class RecruitTextureAtlasGenerator {
    private static final Path ENTITY_TEXTURES = Path.of(
            "src/main/resources/assets/kingdomwarsmiddleearth/textures/entity");
    private static final Path ITEM_TEXTURES = Path.of(
            "src/main/resources/assets/kingdomwarsmiddleearth/textures/item");

    private static final List<RecruitDesign> DESIGNS = List.of(
            new RecruitDesign("gondor_recruit", 0xD5A06F, 0xA86F49, 0x2A211E,
                    0xB9C2CC, 0x626D78, 0x20252D, 0xE8E9E4, 0x352A24),
            new RecruitDesign("rohan_recruit", 0xD9A36D, 0xA96F45, 0x6B4329,
                    0x8E9692, 0x4C5451, 0x263D2A, 0xB4974E, 0x3B2A20),
            new RecruitDesign("mordor_orc_recruit", 0x71814B, 0x465532, 0x201C1A,
                    0x4D4D52, 0x242429, 0x211B1B, 0x8E2D28, 0x181515),
            new RecruitDesign("dwarf_recruit", 0xC98A62, 0x925A3E, 0x8D4A24,
                    0x7E898E, 0x394247, 0x173C40, 0xB17A35, 0x30251F),
            new RecruitDesign("elf_recruit", 0xE2B98D, 0xB78261, 0xC79B3E,
                    0x718468, 0x33483A, 0x203C2C, 0xBFA34D, 0x2B3027));

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
            case "gondor_recruit" -> paintGondor(image, design);
            case "rohan_recruit" -> paintRohan(image, design);
            case "mordor_orc_recruit" -> paintMordorOrc(image, design);
            case "dwarf_recruit" -> paintDwarf(image, design);
            case "elf_recruit" -> paintElf(image, design);
            default -> throw new IllegalStateException("Unknown recruit design " + design.id());
        }
        ImageIO.write(image, "PNG", ENTITY_TEXTURES.resolve(design.id() + ".png").toFile());
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

    private static void paintGondor(BufferedImage image, RecruitDesign design) {
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

    private static void paintRohan(BufferedImage image, RecruitDesign design) {
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

    private static void paintMordorOrc(BufferedImage image, RecruitDesign design) {
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

    private static void paintDwarf(BufferedImage image, RecruitDesign design) {
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

    private static void paintElf(BufferedImage image, RecruitDesign design) {
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
