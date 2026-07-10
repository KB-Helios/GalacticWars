package middleearth.lotr.warmod.integration;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Deterministically generates the original recruit art used by this project.
 *
 * <p>The output follows the canonical 64x64 wide-arm Java humanoid atlas. Keeping the
 * art recipe beside the validation harness makes the UV contract reproducible and avoids
 * importing pixels from packs whose redistribution terms are unknown.</p>
 */
public final class RecruitTextureAtlasGenerator {
    private static final Path ENTITY_TEXTURES = Path.of(
            "src/main/resources/assets/kingdomwarsmiddleearth/textures/entity");
    private static final Path ITEM_TEXTURES = Path.of(
            "src/main/resources/assets/kingdomwarsmiddleearth/textures/item");

    private static final List<RecruitPalette> PALETTES = List.of(
            new RecruitPalette("gondor_recruit", 0xD5A06F, 0xA86F49, 0x332721,
                    0xAEB7C2, 0x424A55, 0x171B22, 0xF2F2E8, 0x272129),
            new RecruitPalette("rohan_recruit", 0xD9A36D, 0xA96F45, 0xB8873C,
                    0x7D8B67, 0x3E4B34, 0x36532D, 0xD7B760, 0x3B2B21),
            new RecruitPalette("mordor_orc_recruit", 0x71814B, 0x465532, 0x201C1A,
                    0x55545A, 0x26252A, 0x2A2020, 0xA02E28, 0x1B1717),
            new RecruitPalette("dwarf_recruit", 0xC98A62, 0x925A3E, 0x8D4A24,
                    0x78858A, 0x333E43, 0x294448, 0xD59C45, 0x33251F),
            new RecruitPalette("elf_recruit", 0xE2B98D, 0xB78261, 0xD0A64D,
                    0x748B6A, 0x334A39, 0x244D3A, 0xD7C776, 0x2F3527));

    private RecruitTextureAtlasGenerator() {
    }

    public static void main(String[] args) throws IOException {
        Files.createDirectories(ENTITY_TEXTURES);
        Files.createDirectories(ITEM_TEXTURES);
        for (RecruitPalette palette : PALETTES) {
            writeSkin(palette);
            writeSpawnEgg(palette);
        }
        System.out.println("RecruitTextureAtlasGenerator generated " + PALETTES.size() + " recruit sets");
    }

    private static void writeSkin(RecruitPalette palette) throws IOException {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);

        drawBox(image, 0, 0, 8, 8, 8,
                palette.hair(), palette.skinShade(), palette.skinShade(), palette.skin(), palette.hair());
        drawBox(image, 16, 16, 8, 12, 4,
                palette.armor(), palette.armorShade(), palette.armorShade(), palette.cloth(), palette.armorShade());
        drawBox(image, 40, 16, 4, 12, 4,
                palette.armor(), palette.skinShade(), palette.armorShade(), palette.armor(), palette.armorShade());
        drawBox(image, 32, 48, 4, 12, 4,
                palette.armor(), palette.skinShade(), palette.armorShade(), palette.armor(), palette.armorShade());
        drawBox(image, 0, 16, 4, 12, 4,
                palette.cloth(), palette.boot(), palette.boot(), palette.cloth(), palette.boot());
        drawBox(image, 16, 48, 4, 12, 4,
                palette.cloth(), palette.boot(), palette.boot(), palette.cloth(), palette.boot());

        drawHelmet(image, palette);
        drawBox(image, 16, 32, 8, 12, 4,
                palette.armor(), palette.armorShade(), palette.armorShade(), palette.armor(), palette.armorShade());
        drawBox(image, 40, 32, 4, 12, 4,
                palette.armor(), palette.armorShade(), palette.armorShade(), palette.armor(), palette.armorShade());
        drawBox(image, 48, 48, 4, 12, 4,
                palette.armor(), palette.armorShade(), palette.armorShade(), palette.armor(), palette.armorShade());
        drawBox(image, 0, 32, 4, 12, 4,
                palette.cloth(), palette.boot(), palette.boot(), palette.cloth(), palette.boot());
        drawBox(image, 0, 48, 4, 12, 4,
                palette.cloth(), palette.boot(), palette.boot(), palette.cloth(), palette.boot());

        drawFace(image, palette);
        drawArmorDetails(image, palette);
        ImageIO.write(image, "PNG", ENTITY_TEXTURES.resolve(palette.id() + ".png").toFile());
    }

    private static void drawFace(BufferedImage image, RecruitPalette palette) {
        int faceX = 8;
        int faceY = 8;
        fill(image, faceX, faceY, 8, 2, palette.hair());
        set(image, faceX + 2, faceY + 4, 0x24282A);
        set(image, faceX + 5, faceY + 4, 0x24282A);
        set(image, faceX + 3, faceY + 6, palette.skinShade());
        set(image, faceX + 4, faceY + 6, palette.skinShade());

        if (palette.id().equals("dwarf_recruit")) {
            fill(image, faceX + 1, faceY + 5, 6, 3, palette.hair());
            set(image, faceX + 2, faceY + 4, 0x24282A);
            set(image, faceX + 5, faceY + 4, 0x24282A);
        } else if (palette.id().equals("mordor_orc_recruit")) {
            set(image, faceX + 3, faceY + 6, 0xD6C7A2);
            set(image, faceX + 4, faceY + 6, 0xD6C7A2);
        }
    }

    private static void drawHelmet(BufferedImage image, RecruitPalette palette) {
        fill(image, 40, 0, 8, 8, palette.armor());
        fill(image, 32, 8, 4, 3, palette.armorShade());
        fill(image, 40, 8, 8, 3, palette.armor());
        fill(image, 48, 8, 4, 3, palette.armorShade());
        fill(image, 52, 8, 8, 3, palette.armorShade());
        for (int x = 40; x < 48; x += 2) {
            set(image, x, 10, palette.accent());
        }
    }

    private static void drawArmorDetails(BufferedImage image, RecruitPalette palette) {
        int frontX = 20;
        int frontY = 36;
        fill(image, frontX, frontY + 9, 8, 2, palette.armorShade());
        fill(image, frontX + 3, frontY + 2, 2, 7, palette.accent());
        fill(image, frontX + 1, frontY + 4, 6, 2, palette.accent());
        set(image, frontX + 2, frontY + 3, palette.accent());
        set(image, frontX + 5, frontY + 3, palette.accent());

        for (int y = 22; y < 32; y += 3) {
            set(image, 45, y, palette.accent());
            set(image, 37, y + 32, palette.accent());
        }
        fill(image, 4, 29, 4, 3, palette.boot());
        fill(image, 20, 61, 4, 3, palette.boot());
    }

    private static void writeSpawnEgg(RecruitPalette palette) throws IOException {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 1; y < 15; y++) {
            double normalizedY = (y - 8.0) / 7.0;
            int halfWidth = Math.max(1, (int) Math.round(5.0 * Math.sqrt(Math.max(0.0, 1.0 - normalizedY * normalizedY))));
            int centerX = 8;
            for (int x = centerX - halfWidth; x <= centerX + halfWidth; x++) {
                set(image, x, y, y < 6 ? palette.armor() : palette.armorShade());
            }
        }
        set(image, 6, 4, palette.accent());
        set(image, 9, 6, palette.accent());
        set(image, 5, 9, palette.accent());
        set(image, 10, 11, palette.accent());
        set(image, 7, 2, 0xFFFFFF);
        ImageIO.write(image, "PNG", ITEM_TEXTURES.resolve(palette.id() + "_spawn_egg.png").toFile());
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

    private record RecruitPalette(
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
