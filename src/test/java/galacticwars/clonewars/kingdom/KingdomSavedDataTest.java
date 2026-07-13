package galacticwars.clonewars.kingdom;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class KingdomSavedDataTest {
    private KingdomSavedDataTest() {
    }

    public static void main(String[] args) throws IOException {
        savedDataUsesVersionedOverworldStorage();
        savedDataExposesGuardedMutations();
        recordsKeepRuntimeCodecsOutOfThePureDomainLayer();
        System.out.println("KingdomSavedDataTest passed");
    }

    private static void savedDataUsesVersionedOverworldStorage() throws IOException {
        String source = read("src/main/java/galacticwars/clonewars/kingdom/KingdomSavedData.java");
        assertContains(source, "CURRENT_SCHEMA_VERSION", "schema version");
        assertContains(source, "CURRENT_SCHEMA_VERSION = 7", "kingdom governance schema version");
        assertContains(source, "pending_invites", "pending invitation persistence");
        assertContains(source, "pending_diplomacy", "pending diplomacy persistence");
        assertContains(source, "inactive_hall_owners", "inactive Hall persistence");
        assertContains(source, "SavedDataType<KingdomSavedData>", "saved data type");
        assertContains(source, "level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE)", "overworld storage");
        assertContains(source, "this.setDirty()", "dirty tracking");
    }

    private static void savedDataExposesGuardedMutations() throws IOException {
        String source = read("src/main/java/galacticwars/clonewars/kingdom/KingdomSavedData.java");
        assertContains(source, "foundKingdom", "kingdom founding");
        assertContains(source, "registerRecruit", "recruit registration");
        assertContains(source, "unregisterRecruit", "recruit removal");
        assertContains(source, "activateHall", "Hall activation and relocation");
        assertContains(source, "deactivateHall", "Hall deactivation");
        assertContains(source, "cancelActiveCampaigns", "transactional campaign cancellation");
        assertContains(source, "promoteCommander", "commander promotion");
        assertContains(source, "completeBuildProject", "building completion rewards");
        assertContains(source, "reserveWorksite", "capacity reservation");
        assertContains(source, "claimWorkOrder", "atomic work order claim");
        assertContains(source, "progressWorkOrder", "guarded work order progress");
        assertContains(source, "hasCommanderSlot", "commander unlock guard");
        assertContains(source, "expectedRevision", "stale revision guard");
        assertContains(source, "beginCampaign", "campaign reservation");
        assertContains(source, "applyPendingCampaignRefunds", "persisted campaign refund settlement");
    }

    private static void recordsKeepRuntimeCodecsOutOfThePureDomainLayer() throws IOException {
        String record = read("src/main/java/galacticwars/clonewars/kingdom/SettlementRecord.java");
        String codecs = read("src/main/java/galacticwars/clonewars/kingdom/KingdomCodecs.java");
        assertNotContains(record, "com.mojang.serialization", "pure settlement record");
        assertNotContains(record, "KingdomCodecs", "runtime codec holder in pure settlement record");
        assertContains(codecs, "Codec<SettlementRecord>", "settlement persistence codec");
        assertContains(codecs, "Codec<KingdomRecord>", "kingdom persistence codec");
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    private static void assertContains(String value, String expected, String label) {
        if (!value.contains(expected)) {
            throw new AssertionError(label + " missing <" + expected + ">");
        }
    }

    private static void assertNotContains(String value, String unexpected, String label) {
        if (value.contains(unexpected)) {
            throw new AssertionError(label + " unexpectedly contains <" + unexpected + ">");
        }
    }
}
