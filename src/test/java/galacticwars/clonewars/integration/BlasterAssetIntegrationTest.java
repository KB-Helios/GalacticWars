package galacticwars.clonewars.integration;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;

public final class BlasterAssetIntegrationTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/galacticwars");
    private static final List<WeaponAsset> WEAPONS = List.of(
            new WeaponAsset("dc15_blaster", "minecraft:item/generated"),
            new WeaponAsset("e5_blaster", "galacticwars:item/dc15_blaster"),
            new WeaponAsset("westar_blaster", "galacticwars:item/dc15_blaster"),
            new WeaponAsset("scatter_blaster", "galacticwars:item/dc15_blaster"));

    private BlasterAssetIntegrationTest() {
    }

    public static void main(String[] args) throws Exception {
        assertModelsAndTextures();
        assertHeldWeaponPose();
        System.out.println("BlasterAssetIntegrationTest passed");
    }

    private static void assertModelsAndTextures() throws Exception {
        Set<String> textureDigests = new HashSet<>();
        for (WeaponAsset weapon : WEAPONS) {
            String model = json(ASSETS.resolve("models/item/" + weapon.id() + ".json"));
            require(model.contains("\"parent\": \"" + weapon.parentModel() + "\""),
                    weapon.id() + " must use its firearm-specific held transform");
            require(model.contains("galacticwars:item/" + weapon.id() + "_hq"),
                    weapon.id() + " must render its high-resolution texture");
            require(!model.contains("minecraft:item/handheld"),
                    weapon.id() + " must not use the sword/tool transform preset");

            Path texture = ASSETS.resolve("textures/item/" + weapon.id() + "_hq.png");
            assertTexture(texture);
            textureDigests.add(HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(texture))));
        }
        require(textureDigests.size() == WEAPONS.size(),
                "each blaster must have a distinct authored texture");

        String rifle = json(ASSETS.resolve("models/item/dc15_blaster.json"));
        String pistol = json(ASSETS.resolve("models/item/westar_blaster.json"));
        String heavy = json(ASSETS.resolve("models/item/scatter_blaster.json"));
        require(rifle.contains("\"thirdperson_righthand\"")
                        && rifle.contains("\"firstperson_righthand\""),
                "rifle model must define both held perspectives");
        require(pistol.contains("[-90, 0, -62]") && pistol.contains("[-90, 0, -58]"),
                "pistol model must align its grip independently from rifles");
        require(heavy.contains("[0.58, 0.58, 0.58]"),
                "heavy blaster must remain clear of the first-person camera");
    }

    private static void assertHeldWeaponPose() throws Exception {
        String extension = Files.readString(Path.of(
                "src/main/java/galacticwars/clonewars/client/render/BlasterClientExtensions.java"));
        require(extension.contains("HumanoidModel.ArmPose.CROSSBOW_HOLD"),
                "blasters must use a two-handed shouldered stance");
        require(extension.contains("applyForgeHandTransform") && extension.contains("recoil"),
                "blasters must keep a dedicated first-person hold transform");

        String neoClient = Files.readString(Path.of(
                "neoforge/src/main/kotlin/galacticwars/clonewars/neoforge/GalacticWarsNeoForgeClient.kt"));
        require(neoClient.contains("BlasterClientExtensions.INSTANCE"),
                "NeoForge must register blaster client extensions");
        for (WeaponAsset weapon : WEAPONS) {
            require(neoClient.contains(registryName(weapon.id())),
                    weapon.id() + " must receive the held-weapon extension");
        }
    }

    private static String registryName(String id) {
        return "ModItems." + id.toUpperCase() + ".get()";
    }

    private static String json(Path path) throws Exception {
        require(Files.isRegularFile(path), "missing JSON asset " + path);
        String content = Files.readString(path).trim();
        require(content.startsWith("{") && content.endsWith("}"), "invalid JSON envelope " + path);
        return content;
    }

    private static void assertTexture(Path path) throws Exception {
        require(Files.isRegularFile(path), "missing texture " + path);
        BufferedImage image = ImageIO.read(path.toFile());
        require(image != null, "unreadable texture " + path);
        require(image.getWidth() == 512 && image.getHeight() == 512,
                path + " must be a 512x512 high-resolution item texture");

        int visible = 0;
        int transparent = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                visible += alpha > 0 ? 1 : 0;
                transparent += alpha == 0 ? 1 : 0;
            }
        }
        require(visible >= 5_000, path + " must retain a detailed readable silhouette");
        require(transparent >= image.getWidth() * image.getHeight() / 2,
                path + " must retain a clean transparent inventory background");
        require((image.getRGB(0, 0) >>> 24) == 0, path + " must have transparent corners");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private record WeaponAsset(String id, String parentModel) {
    }
}
