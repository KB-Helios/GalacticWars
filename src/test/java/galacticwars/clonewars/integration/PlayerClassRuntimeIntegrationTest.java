package galacticwars.clonewars.integration;

import java.nio.file.Files;
import java.nio.file.Path;

/** Cross-loader source contract for the client/server player-class boundary. */
public final class PlayerClassRuntimeIntegrationTest {
    private PlayerClassRuntimeIntegrationTest() {
    }

    public static void main(String[] args) throws Exception {
        String runtime = Files.readString(Path.of(
                "src/main/kotlin/galacticwars/clonewars/classes/PlayerClassRuntime.kt"));
        String keys = Files.readString(Path.of(
                "src/main/java/galacticwars/clonewars/client/ClassKeyMappings.java"));
        String classScreen = Files.readString(Path.of(
                "src/main/java/galacticwars/clonewars/client/gui/PlayerClassScreen.java"));
        String operations = Files.readString(Path.of(
                "src/main/java/galacticwars/clonewars/menu/CommandCenterOperationsMenu.java"));

        assertContains(runtime, "operations.authorizesClassSelection(player)",
                "server-side Command Center authority");
        assertContains(runtime, "ability.activation == AbilityActivation.TARGET",
                "target acquisition limited to aimed abilities");
        assertContains(runtime, "maximumDistanceSquared",
                "solid-block target occlusion");
        assertContains(runtime, "PlayerEvent.PLAYER_QUIT",
                "replay-cache disconnect cleanup");
        assertContains(runtime, "definition.forceTraditionSlot().isNotEmpty()",
                "Force career class HUD suppression");
        assertContains(keys, "minecraft.gui.screen() == null",
                "GUI-safe class hotkeys");
        assertContains(keys, "minecraft.gui.overlay() == null",
                "overlay-safe class hotkeys");
        assertContains(classScreen, "player.containerMenu instanceof CommandCenterOperationsMenu",
                "client class-screen menu lifetime guard");
        assertContains(classScreen, "eligibilityHash(attachment) != displayedEligibilityHash",
                "late attachment synchronization rebuilding class choices");
        assertNotContains(classScreen, "closeContainer()",
                "class screen preserving its authoritative server menu");
        assertContains(operations, "authorizesClassSelection(Player player)",
                "operations menu class authority surface");

        System.out.println("PlayerClassRuntimeIntegrationTest passed");
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
