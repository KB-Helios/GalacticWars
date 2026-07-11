package galacticwars.clonewars.workforce;

import galacticwars.clonewars.kingdom.StorageEndpoint;
import java.util.UUID;

public final class SettlementSupplyLedgerTest {
    public static void main(String[] args) {
        UUID settlement = UUID.randomUUID();
        UUID demandId = UUID.randomUUID();
        UUID firstWorker = UUID.randomUUID();
        UUID secondWorker = UUID.randomUUID();
        StorageEndpoint storage = new StorageEndpoint("minecraft:overworld", 1, 64, 1, 9);
        SupplyDemand demand = new SupplyDemand(demandId, SupplyCategory.CONSTRUCTION,
                "minecraft:stone", 8, 0, 80, "build/project_1");
        SettlementSupplyLedger ledger = SettlementSupplyLedger.create(settlement).request(demand);

        var first = ledger.reserve(demandId, firstWorker, storage, 6, 8, 100, 20);
        assertTrue(first.accepted(), "first lease accepted");
        var second = first.ledger().reserve(demandId, secondWorker, storage, 6, 8, 100, 20);
        assertEquals(2, second.reservation().orElseThrow().quantity(), "physical stock not double reserved");

        SettlementSupplyLedger delivered = second.ledger().complete(
                first.reservation().orElseThrow().id(), firstWorker, 6);
        assertEquals(2, delivered.nextDemand().orElseThrow().outstandingQuantity(), "delivery fulfills demand");
        SettlementSupplyLedger expired = delivered.releaseExpired(121);
        assertTrue(expired.reservation(second.reservation().orElseThrow().id()).orElseThrow().state()
                == SupplyReservation.State.RELEASED, "expired lease released");
        System.out.println("SettlementSupplyLedgerTest passed");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) throw new AssertionError(message + ": expected " + expected + ", got " + actual);
    }
}
