package galacticwars.clonewars.kingdom;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class KingdomStoragePolicyTest {
    private KingdomStoragePolicyTest() {
    }

    public static void main(String[] args) {
        rewardsBoundRegisteredStorageAndDestroyedContainersBecomeUnavailable();
        System.out.println("KingdomStoragePolicyTest passed");
    }

    private static void rewardsBoundRegisteredStorageAndDestroyedContainersBecomeUnavailable() {
        StorageEndpoint firstChest = new StorageEndpoint("minecraft:overworld", 10, 64, 10, 27);
        StorageEndpoint secondChest = new StorageEndpoint("minecraft:overworld", 11, 64, 10, 27);
        StorageEndpoint unrewardedChest = new StorageEndpoint("minecraft:overworld", 12, 64, 10, 27);
        WorksiteRecord supply_depot = new WorksiteRecord(
                UUID.randomUUID(), "supply_depot", "minecraft:overworld", 10, 64, 10, 8, 1,
                List.of(), Optional.empty(), List.of(),
                List.of(firstChest, secondChest, unrewardedChest));
        SettlementRecord settlement = new SettlementRecord(
                UUID.randomUUID(), "minecraft:overworld", 0, 64, 0, 48, 4,
                List.of(), Optional.empty(), CommanderPolicy.defaults(), List.of(supply_depot),
                List.of(), List.of(), List.of(), new SettlementRewards(54, 0), 0);

        List<StorageEndpoint> registered = KingdomStoragePolicy.registeredEndpoints(settlement);
        assertEquals(3, registered.size(), "Hall plus exactly two rewarded chests");
        assertEquals(108, registered.stream().mapToInt(StorageEndpoint::slots).sum(), "total storage slots");
        assertTrue(!KingdomStoragePolicy.isRegistered(settlement, "minecraft:overworld", 12, 64, 10),
                "unrewarded endpoint rejected");
        int available = KingdomStoragePolicy.accessibleSlots(
                settlement,
                endpoint -> endpoint.x() == 0 || endpoint.x() == firstChest.x());
        assertEquals(81, available, "destroyed second chest becomes unavailable");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected " + expected + " but was " + actual);
        }
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }
}
