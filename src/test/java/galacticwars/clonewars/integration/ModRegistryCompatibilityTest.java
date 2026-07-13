package galacticwars.clonewars.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

public final class ModRegistryCompatibilityTest {
    private static final Pattern USE_ON_OVERRIDE = Pattern.compile(
            "\\buseOn\\s*\\([^)]*\\bUseOnContext\\b[^)]*\\)", Pattern.DOTALL);

    private ModRegistryCompatibilityTest() {
    }

    public static void main(String[] args) throws IOException {
        spawnEggUsesDeferredItemIdInjection();
        spawnEggModelUsesCurrentItemModelFormat();
        registeredItemsHaveItemModelDefinitions();
        recruitRendererUsesGeckoLibRenderer();
        recruitTypesUseFactionDimensions();

        System.out.println("ModRegistryCompatibilityTest passed");
    }

    private static void spawnEggUsesDeferredItemIdInjection() throws IOException {
        String modItems = Files.readString(Path.of("src/main/java/galacticwars/clonewars/registry/ModItems.java"));
        String spawnEgg = Files.readString(Path.of(
                "src/main/java/galacticwars/clonewars/entity/RecruitSpawnEggItem.java"));

        assertContains(modItems,
                "ITEMS.registerItem(\"clone_trooper_spawn_egg\"",
                "spawn egg registration must use DeferredRegister.Items#registerItem");
        assertContains(modItems,
                "new RecruitSpawnEggItem(ModEntityTypes.CLONE_TROOPER.get(), properties)",
                "spawn egg registration must retain an explicit recruit type fallback");
        assertNotContains(modItems,
                "new SpawnEggItem(new Item.Properties()",
                "spawn egg registration must not bypass Item.Properties#setId");
        assertContains(spawnEgg,
                "super(properties.spawnEgg(recruitType))",
                "spawn egg must bind its recruit type through the vanilla item component");
        assertDoesNotMatch(spawnEgg,
                USE_ON_OVERRIDE,
                "spawn egg must not replace vanilla world interaction and spawning semantics");
    }

    private static void registeredItemsHaveItemModelDefinitions() {
        assertRegularFile("src/main/resources/assets/galacticwars/items/duracrete.json");
        assertRegularFile("src/main/resources/assets/galacticwars/items/beskar_ore.json");
        assertRegularFile("src/main/resources/assets/galacticwars/items/nightsister_weave_log.json");
        assertRegularFile("src/main/resources/assets/galacticwars/items/beskar_ingot.json");
        assertRegularFile("src/main/resources/assets/galacticwars/items/clone_trooper_spawn_egg.json");
    }

    private static void spawnEggModelUsesCurrentItemModelFormat() throws IOException {
        String model = Files.readString(Path.of(
                "src/main/resources/assets/galacticwars/models/item/clone_trooper_spawn_egg.json"));

        assertContains(model,
                "\"parent\": \"minecraft:item/generated\"",
                "spawn egg model should use an existing generated item parent in 26.2");
        assertContains(model,
                "\"layer0\": \"galacticwars:item/clone_trooper_spawn_egg\"",
                "spawn egg model should point at the modded egg texture");
        assertNotContains(model,
                "template_spawn_egg",
                "spawn egg model must not point at removed template_spawn_egg parent");
        assertRegularFile("src/main/resources/assets/galacticwars/textures/item/clone_trooper_spawn_egg.png");
    }

    private static void recruitRendererUsesGeckoLibRenderer() throws IOException {
        String renderer = Files.readString(Path.of(
                "src/main/java/galacticwars/clonewars/client/render/GalacticRecruitRenderer.java"));

        assertContains(renderer,
                "extends GeoEntityRenderer",
                "recruit renderer should use GeckoLib's entity renderer");
        assertContains(renderer,
                "EntityType<GalacticRecruitEntity>",
                "recruit renderer should bind each recruit variant to its registered entity type");
        assertNotContains(renderer,
                "context.bakeLayer(ModelLayers.ZOMBIE)",
                "recruit renderer must not use the zombie layer for soldiers");
        assertNotContains(renderer,
                "HumanoidMobRenderer",
                "recruit renderer must not use vanilla mob humanoid rendering for GeckoLib recruits");
    }

    private static void recruitTypesUseFactionDimensions() throws IOException {
        String entityTypes = Files.readString(Path.of(
                "src/main/java/galacticwars/clonewars/registry/ModEntityTypes.java"));
        assertContains(entityTypes, "registerRecruit(\"clone_trooper\", 0.60F, 1.95F)",
                "Republic dimensions");
        assertContains(entityTypes, "registerRecruit(\"mandalorian_warrior\", 0.60F, 1.95F)",
                "Mandalorian dimensions");
        assertContains(entityTypes, "registerRecruit(\"b1_battle_droid\", 0.70F, 1.85F)",
                "Separatist dimensions");
        assertContains(entityTypes, "registerRecruit(\"hutt_enforcer\", 0.75F, 1.55F)",
                "hutt_cartel dimensions");
        assertContains(entityTypes, "registerRecruit(\"nightsister_acolyte\", 0.60F, 2.05F)",
                "nightsister dimensions");
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

    private static void assertDoesNotMatch(String source, Pattern pattern, String label) {
        if (pattern.matcher(source).find()) {
            throw new AssertionError(label + " matches forbidden <" + pattern + ">");
        }
    }

    private static void assertRegularFile(String relativePath) {
        if (!Files.isRegularFile(Path.of(relativePath))) {
            throw new AssertionError("missing item model definition <" + relativePath + ">");
        }
    }
}
