package galacticwars.clonewars.integration;

import java.nio.file.Files;
import java.nio.file.Path;

public final class VehicleClientInputIntegrationTest {
    private VehicleClientInputIntegrationTest() {
    }

    public static void main(String[] args) throws Exception {
        String input = Files.readString(Path.of(
                "src/main/java/galacticwars/clonewars/client/ForceKeyMappings.java"))
                .replace("\r\n", "\n");
        String language = Files.readString(Path.of(
                "src/main/resources/assets/galacticwars/lang/en_us.json"));

        assertContains(input, "key.galacticwars.vehicle_descend",
                "dedicated vehicle descend mapping");
        assertContains(input, "GLFW.GLFW_KEY_LEFT_CONTROL",
                "non-dismount default key");
        assertContains(input, "MAPPINGS.forEach(KeyMappingRegistry::register)",
                "complete client mapping registration");
        assertContains(input, "VEHICLE_DESCEND.isDown()",
                "sustained descend packet input");
        assertContains(input, "minecraft.gui.screen() == null",
                "menu-safe vehicle and Force input");
        assertContains(input, "minecraft.gui.overlay() == null",
                "overlay-safe vehicle and Force input");
        assertContains(input, "if (acceptsGameplayInput)",
                "Force activation input guard");
        assertContains(input, "if (acceptsGameplayInput\n",
                "vehicle input guard");
        assertNotContains(input, "minecraft.options.keyShift.isDown()",
                "vanilla sneak dismount binding");
        assertContains(language, "\"key.galacticwars.vehicle_descend\"",
                "vehicle descend translation");

        System.out.println("VehicleClientInputIntegrationTest passed");
    }

    private static void assertContains(String value, String expected, String label) {
        if (!value.contains(expected)) {
            throw new AssertionError(label + " missing <" + expected + ">");
        }
    }

    private static void assertNotContains(String value, String unexpected, String label) {
        if (value.contains(unexpected)) {
            throw new AssertionError(label + " unexpectedly contained <" + unexpected + ">");
        }
    }
}
