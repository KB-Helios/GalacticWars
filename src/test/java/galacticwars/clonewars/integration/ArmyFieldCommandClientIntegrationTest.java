package galacticwars.clonewars.integration;

import java.nio.file.Files;
import java.nio.file.Path;

/** Ensures the field command panel remains reachable on both supported loaders. */
public final class ArmyFieldCommandClientIntegrationTest {
    private ArmyFieldCommandClientIntegrationTest() {
    }

    public static void main(String[] args) throws Exception {
        String mappings = read("src/main/java/galacticwars/clonewars/client/ArmyFieldCommandKeyMappings.java");
        String screen = read("src/main/java/galacticwars/clonewars/client/gui/ArmyFieldCommandScreen.java");
        String fabric = read("fabric/src/main/kotlin/galacticwars/clonewars/fabric/GalacticWarsFabricClient.kt");
        String neoForge = read("neoforge/src/main/kotlin/galacticwars/clonewars/neoforge/GalacticWarsNeoForgeClient.kt");
        String language = read("src/main/resources/assets/galacticwars/lang/en_us.json");

        assertContains(mappings, "GLFW.GLFW_KEY_G", "default G field command key");
        assertContains(mappings, "ArmyFieldCommandScreen", "field command screen opening");
        assertContains(screen, "FieldCommandRequestPayload.MAX_GROUPS", "bounded multi-squad selection");
        assertContains(screen, "MOVEMENT(\"screen.galacticwars.field_command.category.movement\"",
                "movement category");
        assertContains(screen, "COMBAT(\"screen.galacticwars.field_command.category.combat\"",
                "combat category");
        assertContains(screen, "FORMATION(\"screen.galacticwars.field_command.category.formation\"",
                "formation category");
        assertContains(screen, "PATROL(\"screen.galacticwars.field_command.category.patrol\"",
                "patrol category");
        assertContains(screen, "new EditBox(", "named patrol route editor inputs");
        assertContains(screen, "FieldCommandAction.SET_PATROL_WAYPOINT_WAIT",
                "per-waypoint patrol wait control");
        assertContains(screen, "FieldCommandAction.RENAME_PATROL_ROUTE",
                "named patrol route control");
        assertContains(fabric, "GalacticWarsClient.init()", "Fabric client initialization");
        assertContains(neoForge, "ArmyFieldCommandKeyMappings.mappings()", "NeoForge key registration");
        assertContains(language, "\"key.galacticwars.army_command\"", "field command key translation");

        System.out.println("ArmyFieldCommandClientIntegrationTest passed");
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }

    private static void assertContains(String value, String expected, String label) {
        if (!value.contains(expected)) {
            throw new AssertionError(label + " missing <" + expected + ">");
        }
    }
}
