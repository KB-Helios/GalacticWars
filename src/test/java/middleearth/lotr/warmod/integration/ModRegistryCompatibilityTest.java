package middleearth.lotr.warmod.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ModRegistryCompatibilityTest {
    private ModRegistryCompatibilityTest() {
    }

    public static void main(String[] args) throws IOException {
        spawnEggUsesDeferredItemIdInjection();
        spawnEggModelUsesCurrentItemModelFormat();
        registeredItemsHaveItemModelDefinitions();
        recruitRendererUsesGeckoLibRenderer();

        System.out.println("ModRegistryCompatibilityTest passed");
    }

    private static void spawnEggUsesDeferredItemIdInjection() throws IOException {
        String modItems = Files.readString(Path.of("src/main/java/middleearth/lotr/warmod/registry/ModItems.java"));

        assertContains(modItems,
                "ITEMS.registerItem(\"gondor_recruit_spawn_egg\"",
                "spawn egg registration must use DeferredRegister.Items#registerItem");
        assertNotContains(modItems,
                "new SpawnEggItem(new Item.Properties()",
                "spawn egg registration must not bypass Item.Properties#setId");
    }

    private static void registeredItemsHaveItemModelDefinitions() {
        assertRegularFile("src/main/resources/assets/kingdomwarsmiddleearth/items/middle_earth_stone.json");
        assertRegularFile("src/main/resources/assets/kingdomwarsmiddleearth/items/mithril_ore.json");
        assertRegularFile("src/main/resources/assets/kingdomwarsmiddleearth/items/mallorn_log.json");
        assertRegularFile("src/main/resources/assets/kingdomwarsmiddleearth/items/mithril_ingot.json");
        assertRegularFile("src/main/resources/assets/kingdomwarsmiddleearth/items/gondor_recruit_spawn_egg.json");
    }

    private static void spawnEggModelUsesCurrentItemModelFormat() throws IOException {
        String model = Files.readString(Path.of(
                "src/main/resources/assets/kingdomwarsmiddleearth/models/item/gondor_recruit_spawn_egg.json"));

        assertContains(model,
                "\"parent\": \"minecraft:item/generated\"",
                "spawn egg model should use an existing generated item parent in 26.2");
        assertContains(model,
                "\"layer0\": \"kingdomwarsmiddleearth:item/gondor_recruit_spawn_egg\"",
                "spawn egg model should point at the modded egg texture");
        assertNotContains(model,
                "template_spawn_egg",
                "spawn egg model must not point at removed template_spawn_egg parent");
        assertRegularFile("src/main/resources/assets/kingdomwarsmiddleearth/textures/item/gondor_recruit_spawn_egg.png");
    }

    private static void recruitRendererUsesGeckoLibRenderer() throws IOException {
        String renderer = Files.readString(Path.of(
                "src/main/java/middleearth/lotr/warmod/client/render/MiddleEarthRecruitRenderer.java"));

        assertContains(renderer,
                "extends GeoEntityRenderer",
                "recruit renderer should use GeckoLib's entity renderer");
        assertContains(renderer,
                "EntityType<MiddleEarthRecruitEntity>",
                "recruit renderer should bind each recruit variant to its registered entity type");
        assertNotContains(renderer,
                "context.bakeLayer(ModelLayers.ZOMBIE)",
                "recruit renderer must not use the zombie layer for soldiers");
        assertNotContains(renderer,
                "HumanoidMobRenderer",
                "recruit renderer must not use vanilla mob humanoid rendering for GeckoLib recruits");
    }

    private static void assertContains(String haystack, String needle, String label) {
        if (!haystack.contains(needle)) {
            throw new AssertionError(label + " missing <" + needle + ">");
        }
    }

    private static void assertNotContains(String haystack, String needle, String label) {
        if (haystack.contains(needle)) {
            throw new AssertionError(label + " contains forbidden <" + needle + ">");
        }
    }

    private static void assertRegularFile(String relativePath) {
        if (!Files.isRegularFile(Path.of(relativePath))) {
            throw new AssertionError("missing item model definition <" + relativePath + ">");
        }
    }
}
