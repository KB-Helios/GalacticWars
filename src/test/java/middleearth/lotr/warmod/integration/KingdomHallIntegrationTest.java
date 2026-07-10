package middleearth.lotr.warmod.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class KingdomHallIntegrationTest {
    private KingdomHallIntegrationTest() {
    }

    public static void main(String[] args) throws IOException {
        hallIsRegisteredAsPhysicalOwnedStorage();
        hallFoundsAuthoritativeKingdomState();
        hallAssetsAreComplete();
        System.out.println("KingdomHallIntegrationTest passed");
    }

    private static void hallIsRegisteredAsPhysicalOwnedStorage() throws IOException {
        String blocks = read("src/main/java/middleearth/lotr/warmod/registry/ModBlocks.java");
        String items = read("src/main/java/middleearth/lotr/warmod/registry/ModItems.java");
        String blockEntities = read("src/main/java/middleearth/lotr/warmod/registry/ModBlockEntityTypes.java");
        String hall = read("src/main/java/middleearth/lotr/warmod/settlement/KingdomHallBlockEntity.java");
        assertContains(blocks, "KINGDOM_HALL", "hall block registration");
        assertContains(items, "KINGDOM_HALL", "hall item registration");
        assertContains(blockEntities, "KINGDOM_HALL", "hall block entity registration");
        assertContains(hall, "CONTAINER_SIZE = 54", "physical shared inventory");
        assertContains(hall, "ownerId", "persisted owner");
        assertContains(hall, "reserveEmeralds", "atomic treasury reservation");
        assertContains(hall, "refundEmeralds", "campaign refund");
        assertContains(hall, "chargeDailyUpkeep", "daily upkeep");
        assertContains(hall, "UpkeepClockInitialized", "new Hall upkeep grace clock");
        assertContains(hall, "settlePendingCampaignRefunds", "persisted commander campaign refunds");
        assertContains(hall, "items.set(slot, ItemStack.EMPTY)", "depleted-slot normalization");
        assertContains(hall, "getUpdatePacket", "block entity client update packet");
        assertContains(hall, "getUpdateTag", "block entity client update tag");
        assertContains(hall, "sendBlockUpdated", "custom state synchronization");
    }

    private static void hallFoundsAuthoritativeKingdomState() throws IOException {
        String block = read("src/main/java/middleearth/lotr/warmod/settlement/KingdomHallBlock.java");
        assertContains(block, "KingdomSavedData.get(serverLevel)", "overworld saved data access");
        assertContains(block, "foundKingdom", "kingdom founding");
        assertContains(block, "changeFaction", "guarded faction selection");
        assertContains(block, "player.openMenu(hall)", "hall treasury menu");
    }

    private static void hallAssetsAreComplete() {
        assertFile("src/main/resources/assets/kingdomwarsmiddleearth/blockstates/kingdom_hall.json");
        assertFile("src/main/resources/assets/kingdomwarsmiddleearth/items/kingdom_hall.json");
        assertFile("src/main/resources/assets/kingdomwarsmiddleearth/models/block/kingdom_hall.json");
        assertFile("src/main/resources/assets/kingdomwarsmiddleearth/models/item/kingdom_hall.json");
        assertFile("src/main/resources/data/kingdomwarsmiddleearth/loot_table/blocks/kingdom_hall.json");
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
