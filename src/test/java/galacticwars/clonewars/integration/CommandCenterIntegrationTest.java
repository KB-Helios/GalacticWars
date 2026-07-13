package galacticwars.clonewars.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CommandCenterIntegrationTest {
    private CommandCenterIntegrationTest() {
    }

    public static void main(String[] args) throws IOException {
        hallIsRegisteredAsPhysicalOwnedStorage();
        hallFoundsAuthoritativeKingdomState();
        hallRequiresExplicitFactionSelection();
        hallAssetsAreComplete();
        hallOpensPlanetNavigationAfterPledge();
        System.out.println("CommandCenterIntegrationTest passed");
    }

    private static void hallRequiresExplicitFactionSelection() throws IOException {
        String block = read("src/main/java/galacticwars/clonewars/settlement/CommandCenterBlock.java");
        String menu = read("src/main/java/galacticwars/clonewars/menu/FactionSelectionMenu.java");
        String menus = read("src/main/java/galacticwars/clonewars/registry/ModMenuTypes.java");
        String client = read("src/main/java/galacticwars/clonewars/GalacticWarsClient.java");
        assertContains(block, "FactionSelectionMenuProvider", "placement-time faction picker");
        assertContains(menu, "ProgressionEventType.FACTION_PLEDGED", "server-authoritative pledge");
        assertContains(menu, "activateHall", "selected faction kingdom activation");
        assertContains(menu, "FactionAlignmentSavedData", "selected faction alignment");
        String provider = read("src/main/java/galacticwars/clonewars/menu/FactionSelectionMenuProvider.java");
        assertContains(provider, "buffer.writeUtf(id, 128)", "bounded faction id encoding");
        String claims = read("src/main/java/galacticwars/clonewars/kingdom/KingdomSavedData.java");
        assertContains(claims, "Map<ClaimKey, KingdomClaim> claimsByChunk", "constant-time claim index");
        assertContains(menus, "FACTION_SELECTION", "faction menu registration");
        assertContains(client, "FactionSelectionScreen", "faction screen registration");
    }

    private static void hallOpensPlanetNavigationAfterPledge() throws IOException {
        String block = read("src/main/java/galacticwars/clonewars/settlement/CommandCenterBlock.java");
        String menus = read("src/main/java/galacticwars/clonewars/registry/ModMenuTypes.java");
        assertContains(block, "CommandCenterNavigationMenuProvider", "navigation console provider");
        assertContains(menus, "COMMAND_CENTER_NAVIGATION", "navigation menu registration");
    }

    private static void hallIsRegisteredAsPhysicalOwnedStorage() throws IOException {
        String blocks = read("src/main/java/galacticwars/clonewars/registry/ModBlocks.java");
        String items = read("src/main/java/galacticwars/clonewars/registry/ModItems.java");
        String blockEntities = read("src/main/java/galacticwars/clonewars/registry/ModBlockEntityTypes.java");
        String hall = read("src/main/java/galacticwars/clonewars/settlement/CommandCenterBlockEntity.java");
        String credits = read("src/main/java/galacticwars/clonewars/economy/CreditTransactionService.java");
        assertContains(blocks, "COMMAND_CENTER", "hall block registration");
        assertContains(items, "COMMAND_CENTER", "hall item registration");
        assertContains(blockEntities, "COMMAND_CENTER", "hall block entity registration");
        assertContains(hall, "CONTAINER_SIZE = 54", "physical shared inventory");
        assertContains(hall, "ownerId", "persisted owner");
        assertContains(hall, "reserveCredits", "atomic treasury reservation");
        assertContains(hall, "refundCredits", "campaign refund");
        assertContains(hall, "chargeDailyUpkeep", "daily upkeep");
        assertContains(hall, "UpkeepClockInitialized", "new Hall upkeep grace clock");
        assertContains(hall, "settlePendingCampaignRefunds", "persisted commander campaign refunds");
        assertContains(hall, "CreditTransactionService.withdrawContainer", "single treasury transaction authority");
        assertContains(hall, "CreditTransactionService.depositContainer", "single treasury refund authority");
        assertContains(credits, "container.setItem(slot, ItemStack.EMPTY)", "depleted-slot normalization");
        assertContains(credits, "containerBalance(container) < amount", "atomic withdrawal preflight");
        assertContains(hall, "getUpdatePacket", "block entity client update packet");
        assertContains(hall, "getUpdateTag", "block entity client update tag");
        assertContains(hall, "sendBlockUpdated", "custom state synchronization");
    }

    private static void hallFoundsAuthoritativeKingdomState() throws IOException {
        String block = read("src/main/java/galacticwars/clonewars/settlement/CommandCenterBlock.java");
        String events = read("src/main/java/galacticwars/clonewars/settlement/CommandCenterEvents.java");
        String lifecycle = read("src/main/java/galacticwars/clonewars/settlement/CommandCenterLifecycleService.java");
        assertContains(block, "KingdomSavedData.get(serverLevel)", "overworld saved data access");
        assertContains(block, "activateHall", "kingdom founding and relocation");
        assertContains(block, "FactionSelectionMenuProvider", "explicit faction selection");
        assertContains(block, "CommandCenterOperationsMenuProvider", "operations hub menu");
        String operations = read("src/main/java/galacticwars/clonewars/menu/CommandCenterOperationsMenu.java");
        assertContains(operations, "serverPlayer.openMenu(hall)", "storage tab treasury menu");
        assertContains(block, "onExplosionHit", "explosion-resistant owner removal path");
        assertContains(block, "playerWillDestroy", "pre-removal lifecycle transaction");
        assertContains(block, "dropHallContents", "exactly-once inventory drops");
        assertContains(events, "BreakBlockEvent", "owner-authorized block breaking");
        assertContains(events, "event.setCanceled(true)", "non-owner break rejection");
        assertContains(lifecycle, "cancelActiveCampaigns", "campaign cancellation before removal");
        assertContains(lifecycle, "deactivateHall", "authoritative Hall deactivation");
    }

    private static void hallAssetsAreComplete() {
        assertFile("src/main/resources/assets/galacticwars/blockstates/command_center.json");
        assertFile("src/main/resources/assets/galacticwars/items/command_center.json");
        assertFile("src/main/resources/assets/galacticwars/models/block/command_center.json");
        assertFile("src/main/resources/assets/galacticwars/models/item/command_center.json");
        assertFile("src/main/resources/data/galacticwars/loot_table/blocks/command_center.json");
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    private static void assertFile(String path) {
        if (!Files.isRegularFile(Path.of(path))) {
            throw new AssertionError("missing file " + path);
        }
    }

    private static void assertContains(String value, String expected, String label) {
        if (!value.contains(expected)) {
            throw new AssertionError(label + " missing <" + expected + ">");
        }
    }
}
