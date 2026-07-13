package galacticwars.clonewars.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PlanetTravelIntegrationTest {
    private PlanetTravelIntegrationTest() {
    }

    public static void main(String[] args) throws IOException {
        String block = read("src/main/java/galacticwars/clonewars/settlement/CommandCenterBlock.java");
        String menu = read("src/main/java/galacticwars/clonewars/menu/CommandCenterNavigationMenu.java");
        String menuProvider = read("src/main/java/galacticwars/clonewars/menu/CommandCenterNavigationMenuProvider.java");
        String service = read("src/main/java/galacticwars/clonewars/world/PlanetTravelService.java");
        String arrival = read("src/main/java/galacticwars/clonewars/world/PlanetArrivalService.java");
        String armyTravel = read("src/main/java/galacticwars/clonewars/army/ArmyTravelService.java");
        String navigator = read("src/main/java/galacticwars/clonewars/world/HyperspaceNavigatorItem.java");
        String items = read("src/main/java/galacticwars/clonewars/registry/ModItems.java");

        assertContains(block, "new CommandCenterNavigationMenuProvider()", "post-pledge navigation entrypoint");
        assertContains(menu, "this.stillValid(player)", "server menu distance and ownership validation");
        assertContains(menu, "size > MAX_PLANET_IDS", "navigation payload read cap");
        assertContains(menuProvider, "planets.size() > CommandCenterNavigationMenu.MAX_PLANET_IDS",
                "navigation payload write cap");
        assertContains(menuProvider, "LaunchContentDefinitions.MAX_SERIALIZED_PLANET_ID_BYTES",
                "navigation planet id wire limit");
        assertContains(service, "state.unlocks().contains(\"planet_travel\")", "Forward Base unlock validation");
        assertContains(service, "hall != null && hall.upkeepPaid()", "upkeep validation");
        assertContains(service, "player.teleportTo", "server dimension transfer");
        assertContains(service, "squadTravel.reserve()", "pre-teleport squad reservation");
        assertContains(service, "squadTravel.rollback", "failed-teleport squad rollback");
        assertContains(armyTravel, "candidate.ownerId().equals(owner.getUUID())",
                "traveler-owned squad selection");
        assertContains(service, "setRespawnPosition", "planet arrival respawn configuration");
        assertContains(service, "resolveCommandCenter", "cross-dimension Command Center authority");
        assertContains(service, "hallLevel.getChunkAt(hallPos)", "remote Command Center chunk resolution");
        assertBefore(service, "player.teleportTo", "ProgressionEventType.PLANET_VISITED",
                "visit event emitted only after successful transfer");
        assertContains(arrival, "state.canBeReplaced()", "non-destructive landing pad preflight");
        assertContains(arrival, "findExistingPlatform", "stable arrival reuse");
        assertContains(arrival, "state.getFluidState().isEmpty()", "fluid-free arrival volume");
        assertContains(armyTravel, "ArmyGroupLifecycleState.VIRTUAL", "cross-dimension squad virtualization");
        assertContains(armyTravel, "createArmySnapshot", "live squad snapshot preflight");
        assertContains(armyTravel, "liveMembers.forEach", "source squad removal after commit");
        assertContains(navigator, "PlanetTravelService.hasActiveCommandCenter", "remote navigator authority");
        assertContains(items, "HYPERSPACE_NAVIGATOR", "navigator item registration");
        assertFile("src/main/resources/data/galacticwars/recipe/hyperspace_navigator.json");
        assertFile("src/main/resources/assets/galacticwars/items/hyperspace_navigator.json");
        assertFile("src/main/resources/assets/galacticwars/models/item/hyperspace_navigator.json");
        System.out.println("PlanetTravelIntegrationTest passed");
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    private static void assertContains(String value, String expected, String label) {
        if (!value.contains(expected)) {
            throw new AssertionError(label + " missing <" + expected + ">");
        }
    }

    private static void assertFile(String path) {
        if (!Files.isRegularFile(Path.of(path))) {
            throw new AssertionError("missing file " + path);
        }
    }

    private static void assertBefore(String value, String first, String second, String label) {
        int firstIndex = value.indexOf(first);
        int secondIndex = value.indexOf(second, firstIndex + first.length());
        if (firstIndex < 0 || secondIndex <= firstIndex) {
            throw new AssertionError(label);
        }
    }
}
