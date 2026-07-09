package middleearth.lotr.warmod.integration;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.imageio.ImageIO;

public final class GondorRecruitTextureTest {
    private static final Path TEXTURE_PATH = Path.of(
            "src/main/resources/assets/kingdomwarsmiddleearth/textures/entity/gondor_recruit.png");
    private static final String LORD_OF_THE_PACKS_VINDICATOR_SHA256 =
            "61c9d0d295cd9a431800f759018730227697da135573393087f82f4e1e377ee0";

    private GondorRecruitTextureTest() {
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        assertEquals(LORD_OF_THE_PACKS_VINDICATOR_SHA256, sha256(TEXTURE_PATH), "texture provenance hash");

        BufferedImage texture = ImageIO.read(TEXTURE_PATH.toFile());

        assertEquals(64, texture.getWidth(), "texture width");
        assertEquals(64, texture.getHeight(), "texture height");

        assertAtLeast(1200, countNonTransparent(texture), "visible recruit texture pixels");

        System.out.println("GondorRecruitTextureTest passed");
    }

    private static int countNonTransparent(BufferedImage texture) {
        int count = 0;
        for (int ix = 0; ix < texture.getWidth(); ix++) {
            for (int iy = 0; iy < texture.getHeight(); iy++) {
                int alpha = (texture.getRGB(ix, iy) >>> 24) & 0xFF;
                if (alpha > 0) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void assertAtLeast(int minimum, int actual, String label) {
        if (actual < minimum) {
            throw new AssertionError(label + " expected at least <" + minimum + "> but was <" + actual + ">");
        }
    }

    private static void assertOpaque(BufferedImage texture, int x, int y, int width, int height, String label) {
        for (int ix = x; ix < x + width; ix++) {
            for (int iy = y; iy < y + height; iy++) {
                int alpha = (texture.getRGB(ix, iy) >>> 24) & 0xFF;
                if (alpha == 0) {
                    throw new AssertionError(label + " has transparent pixel at " + ix + "," + iy);
                }
            }
        }
    }

    private static void assertEquals(int expected, int actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertEquals(String expected, String actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static String sha256(Path path) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(Files.readAllBytes(path));
        StringBuilder result = new StringBuilder(hash.length * 2);
        for (byte value : hash) {
            result.append(String.format("%02x", value & 0xFF));
        }
        return result.toString();
    }
}
