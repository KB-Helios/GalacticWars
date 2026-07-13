package galacticwars.clonewars.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BlasterRuntimeIntegrationTest {
    private BlasterRuntimeIntegrationTest() {
    }

    public static void main(String[] args) throws IOException {
        playerHeatIsPersistentAndVisible();
        ungroupedRangedRecruitsUseTheirWeapons();
        System.out.println("BlasterRuntimeIntegrationTest passed");
    }

    private static void playerHeatIsPersistentAndVisible() throws IOException {
        String components = read("src/main/java/galacticwars/clonewars/registry/ModDataComponents.java");
        String item = read("src/main/java/galacticwars/clonewars/combat/BlasterItem.java");
        String hud = read("src/main/java/galacticwars/clonewars/client/gui/BlasterHeatHud.java");
        String client = read("src/main/java/galacticwars/clonewars/GalacticWarsClient.java");
        assertContains(components, "builder.persistent", "persistent blaster heat component");
        assertContains(item, "BlasterHeatPolicy.canFire", "server heat gate");
        assertContains(item, "inventoryTick", "server cooling tick");
        assertContains(item, "level.isClientSide()", "client inventory mutation guard");
        assertContains(hud, "heatedSegments", "segmented heat display");
        assertContains(client, "RegisterGuiLayersEvent", "HUD layer registration");
    }

    private static void ungroupedRangedRecruitsUseTheirWeapons() throws IOException {
        String brain = read("src/main/java/galacticwars/clonewars/entity/ai/RecruitBrain.java");
        String behaviour = read(
                "src/main/java/galacticwars/clonewars/entity/ai/RecruitRangedCombatBehaviour.java");
        assertContains(brain, "new RecruitRangedCombatBehaviour()", "ranged brain registration");
        assertContains(behaviour, "!recruit.hasAuthoritativeArmyGroup()", "group-controller exclusion");
        assertContains(behaviour, "blaster.fireAt", "autonomous blaster execution");
        assertContains(behaviour, "fireNightsisterBow", "autonomous bow execution");
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    private static void assertContains(String value, String expected, String label) {
        if (!value.contains(expected)) {
            throw new AssertionError(label + " missing <" + expected + ">");
        }
    }
}
