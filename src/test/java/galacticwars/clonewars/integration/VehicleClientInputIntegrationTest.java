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
        String forceHud = Files.readString(Path.of(
                "src/main/java/galacticwars/clonewars/client/gui/ForceHud.java"))
                .replace("\r\n", "\n");
        String forceHudPayload = Files.readString(Path.of(
                "src/main/java/galacticwars/clonewars/network/ForceHudPayload.java"))
                .replace("\r\n", "\n");

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
        assertContains(input, "boolean down = acceptsGameplayInput && KEYS[slot].isDown()",
                "press-hold-release Force input guard");
        assertContains(input, "ForceActivationPhase.CANCEL",
                "automatic invalid-state Force cancellation");
        assertContains(input, "if (acceptsGameplayInput\n",
                "vehicle input guard");
        assertNotContains(input, "minecraft.options.keyShift.isDown()",
                "vanilla sneak dismount binding");
        assertContains(language, "\"key.galacticwars.vehicle_descend\"",
                "vehicle descend translation");
        assertContains(forceHudPayload, "int targetValidityMask",
                "bounded authoritative target-validity HUD state");
        assertContains(forceHudPayload, "String failureReason",
                "bounded authoritative failure feedback");
        assertContains(forceHud, "state.targetValid(slot)",
                "graphical target-validity indicator");
        assertContains(forceHud, "friendlyFailure(state.failureReason())",
                "visible concise server failure feedback");
        assertContains(forceHud, "drawAbilityBadge",
                "graphical three-slot Force ability badges");

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
