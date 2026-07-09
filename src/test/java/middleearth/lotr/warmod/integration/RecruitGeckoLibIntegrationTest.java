package middleearth.lotr.warmod.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class RecruitGeckoLibIntegrationTest {
    private static final List<String> RECRUITS = List.of(
            "gondor_recruit",
            "rohan_recruit",
            "mordor_orc_recruit",
            "dwarf_recruit",
            "elf_recruit");

    private RecruitGeckoLibIntegrationTest() {
    }

    public static void main(String[] args) throws IOException {
        recruitEntityOwnsGeckoLibControllers();
        clientRegistersGeckoRenderersForEveryRecruit();
        geckoAssetsExistForEveryRecruit();

        System.out.println("RecruitGeckoLibIntegrationTest passed");
    }

    private static void recruitEntityOwnsGeckoLibControllers() throws IOException {
        String entity = read("src/main/java/middleearth/lotr/warmod/entity/MiddleEarthRecruitEntity.java");

        assertContains(entity, "implements GeoEntity", "GeoEntity implementation");
        assertContains(entity, "GeckoLibUtil.createInstanceCache(this)", "GeckoLib instance cache");
        assertContains(entity, "registerControllers", "controller registration method");
        assertContains(entity, "DefaultAnimations.genericWalkRunIdleController()", "walk/run/idle controller");
        assertContains(entity, "DefaultAnimations.genericAttackAnimation(DefaultAnimations.ATTACK_SWING)",
                "attack animation controller");
    }

    private static void clientRegistersGeckoRenderersForEveryRecruit() throws IOException {
        String client = read("src/main/java/middleearth/lotr/warmod/KingdomWarsMiddleEarthClient.java");
        String entities = read("src/main/java/middleearth/lotr/warmod/registry/ModEntityTypes.java");

        for (String recruit : RECRUITS) {
            String constant = toConstant(recruit);
            assertContains(entities, '"' + recruit + '"', "registered entity " + recruit);
            assertContains(client, "ModEntityTypes." + constant + ".get()", "renderer registration " + recruit);
        }
    }

    private static void geckoAssetsExistForEveryRecruit() throws IOException {
        for (String recruit : RECRUITS) {
            assertRegularFile("src/main/resources/assets/kingdomwarsmiddleearth/geckolib/models/entity/"
                    + recruit + ".geo.json");
            assertRegularFile("src/main/resources/assets/kingdomwarsmiddleearth/geckolib/animations/entity/"
                    + recruit + ".animation.json");
            assertRegularFile("src/main/resources/assets/kingdomwarsmiddleearth/textures/entity/"
                    + recruit + ".png");
            String animation = read("src/main/resources/assets/kingdomwarsmiddleearth/geckolib/animations/entity/"
                    + recruit + ".animation.json");
            assertContains(animation, "\"misc.idle\"", "idle animation " + recruit);
            assertContains(animation, "\"move.walk\"", "walk animation " + recruit);
            assertContains(animation, "\"attack.swing\"", "attack animation " + recruit);
        }
    }

    private static String toConstant(String recruit) {
        return recruit.toUpperCase();
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }

    private static void assertContains(String haystack, String needle, String label) {
        if (!haystack.contains(needle)) {
            throw new AssertionError(label + " missing <" + needle + ">");
        }
    }

    private static void assertRegularFile(String relativePath) {
        if (!Files.isRegularFile(Path.of(relativePath))) {
            throw new AssertionError("missing GeckoLib asset <" + relativePath + ">");
        }
    }
}
