package galacticwars.clonewars.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Guards loader entrypoints and lifecycle-sensitive client registration. */
public final class CrossLoaderLaunchIntegrityTest {
    private CrossLoaderLaunchIntegrityTest() {
    }

    public static void main(String[] args) throws IOException {
        String commonBootstrap = read("src/main/java/galacticwars/clonewars/GalacticWars.java");
        String fabricMetadata = read("fabric/src/main/resources/fabric.mod.json");
        String fabricEntrypoint = read(
                "fabric/src/main/kotlin/galacticwars/clonewars/fabric/GalacticWarsFabric.kt");
        String fabricClientEntrypoint = read(
                "fabric/src/main/kotlin/galacticwars/clonewars/fabric/GalacticWarsFabricClient.kt");
        String neoForgeMetadata = read(
                "neoforge/src/main/resources/META-INF/neoforge.mods.toml");
        String neoForgeEntrypoint = read(
                "neoforge/src/main/kotlin/galacticwars/clonewars/neoforge/GalacticWarsNeoForge.kt");
        String neoForgeClientEntrypoint = read(
                "neoforge/src/main/kotlin/galacticwars/clonewars/neoforge/GalacticWarsNeoForgeClient.kt");
        String fabricBuild = read("fabric/build.gradle.kts");
        String neoForgeBuild = read("neoforge/build.gradle.kts");

        assertContains(fabricMetadata,
                "\"main\": [\"galacticwars.clonewars.fabric.GalacticWarsFabric\"]",
                "Fabric common entrypoint");
        assertContains(fabricMetadata,
                "\"client\": [\"galacticwars.clonewars.fabric.GalacticWarsFabricClient\"]",
                "Fabric client entrypoint");
        assertContains(fabricEntrypoint, "class GalacticWarsFabric : ModInitializer",
                "Fabric server-safe initializer");
        assertContains(fabricClientEntrypoint,
                "class GalacticWarsFabricClient : ClientModInitializer",
                "Fabric client-only initializer");

        assertContains(neoForgeMetadata, "modLoader=\"kotlinforforge\"",
                "NeoForge Kotlin loader");
        assertContains(neoForgeEntrypoint, "@Mod(GalacticWars.MODID)",
                "NeoForge common entrypoint");
        assertNotContains(neoForgeEntrypoint, "net.minecraft.client",
                "NeoForge common entrypoint client leak");
        assertContains(neoForgeClientEntrypoint, "value = [Dist.CLIENT]",
                "NeoForge client-side subscriber guard");
        assertNotContains(neoForgeClientEntrypoint, "@JvmStatic",
                "NeoForge Kotlin subscriber static bridge");

        assertNotContains(commonBootstrap, "net.minecraft.client",
                "common bootstrap client leak");
        assertNotContains(commonBootstrap, "GalacticWarsClient",
                "common bootstrap client invocation");

        // Architectury's NeoForge menu adapter only attaches an event listener. Registering it
        // from FMLClientSetupEvent is too late because NeoForge posts RegisterMenuScreensEvent
        // from ClientHooks.initClientHooks first, so the loader entrypoint must own this event.
        assertContains(neoForgeClientEntrypoint,
                "fun registerMenuScreens(event: RegisterMenuScreensEvent)",
                "NeoForge lifecycle-safe menu registration");
        assertContains(neoForgeClientEntrypoint,
                "fun registerKeyMappings(event: RegisterKeyMappingsEvent)",
                "NeoForge lifecycle-safe key registration");
        assertContains(neoForgeClientEntrypoint, "GalacticWarsClient.initRuntime()",
                "NeoForge runtime-only client setup");
        assertNotContains(neoForgeClientEntrypoint, "GalacticWarsClient.init()",
                "NeoForge lifecycle-late Architectury registration");
        for (String menu : new String[]{
                "RECRUIT_COMMAND",
                "RECRUIT_LOADOUT",
                "COMMAND_CENTER_NAVIGATION",
                "FACTION_SELECTION",
                "MERCHANT_TRADE",
                "COMMAND_CENTER_OPERATIONS"
        }) {
            assertContains(neoForgeClientEntrypoint, "ModMenuTypes." + menu + ".get()",
                    "NeoForge screen registration for " + menu);
        }
        assertContains(neoForgeClientEntrypoint,
                "fun registerEntityRenderers(event: EntityRenderersEvent.RegisterRenderers)",
                "NeoForge lifecycle-safe renderer registration");

        assertContains(fabricBuild,
                "it.name == \"runClient\" || it.name == \"runServer\"",
                "Fabric fresh common development JAR dependency");
        assertContains(neoForgeBuild,
                "it.name == \"runClient\" || it.name == \"runServer\" || it.name == \"runGameTestServer\"",
                "NeoForge fresh common development JAR dependency");
        assertContains(fabricBuild,
                "dependsOn(project(\":common\").tasks.named(\"jar\"))",
                "Fabric run-task common JAR edge");
        assertContains(neoForgeBuild,
                "dependsOn(project(\":common\").tasks.named(\"jar\"))",
                "NeoForge run-task common JAR edge");

        if (Files.exists(Path.of("src/main/resources/galacticwars.mixins.json"))) {
            throw new AssertionError("obsolete unreferenced mixin configuration is still packaged");
        }

        System.out.println("CrossLoaderLaunchIntegrityTest passed");
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }

    private static void assertContains(String value, String expected, String label) {
        if (!value.contains(expected)) {
            throw new AssertionError(label + " missing <" + expected + ">");
        }
    }

    private static void assertNotContains(String value, String forbidden, String label) {
        if (value.contains(forbidden)) {
            throw new AssertionError(label + " contains forbidden <" + forbidden + ">");
        }
    }
}
