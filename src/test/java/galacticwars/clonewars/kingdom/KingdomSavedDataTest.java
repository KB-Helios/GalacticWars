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
        schema9StarterCampDeploymentsRoundTrip();
        schema8MigrationPreservesData();
        System.out.println("KingdomSavedDataTest passed");
    }

    private static void savedDataUsesVersionedOverworldStorage() throws IOException {
        String source = read("src/main/java/galacticwars/clonewars/kingdom/KingdomSavedData.java");
        assertContains(source, "CURRENT_SCHEMA_VERSION", "schema version");
        assertContains(source, "CURRENT_SCHEMA_VERSION = 9", "kingdom governance schema version");
        assertContains(source, "starter_camp_deployments", "starter camp exact-once persistence");
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
        assertContains(source, "FactionBalanceService.effectiveRecruitLimit(kingdom.factionId())",
                "authoritative kingdom faction recruit cap");
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

    private static void schema9StarterCampDeploymentsRoundTrip() {
        KingdomSavedData data = new KingdomSavedData();
        java.util.UUID kingdom1 = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001");
        java.util.UUID kingdom2 = java.util.UUID.fromString("00000000-0000-0000-0000-000000000002");
        java.util.UUID builder = java.util.UUID.fromString("00000000-0000-0000-0000-0000000000bb");
        java.util.UUID project = java.util.UUID.fromString("00000000-0000-0000-0000-0000000000cc");

        galacticwars.clonewars.settlement.StarterCampDeployment deployment1 =
            galacticwars.clonewars.settlement.StarterCampDeployment.awaiting(
                kingdom1, "minecraft:overworld", 100, 64, 200, 1)
            .withSuppliesGranted()
            .withBuilder(builder)
            .building(project);
        galacticwars.clonewars.settlement.StarterCampDeployment deployment2 =
            galacticwars.clonewars.settlement.StarterCampDeployment.awaiting(
                kingdom2, "minecraft:the_nether", -50, 80, -100, 2);

        if (!data.setStarterCampDeployment(deployment1) || !data.setStarterCampDeployment(deployment2)) {
            throw new AssertionError("schema 9 deployment insertion");
        }
        if (!data.isDirty()) {
            throw new AssertionError("schema 9 deployment should mark data dirty");
        }

        java.util.Optional<galacticwars.clonewars.settlement.StarterCampDeployment> retrieved1 =
            data.starterCampDeployment(kingdom1);
        java.util.Optional<galacticwars.clonewars.settlement.StarterCampDeployment> retrieved2 =
            data.starterCampDeployment(kingdom2);

        if (retrieved1.isEmpty() || !retrieved1.get().kingdomId().equals(kingdom1)
                || retrieved1.get().originX() != 100 || retrieved1.get().rotationSteps() != 1
                || !retrieved1.get().builderId().equals(java.util.Optional.of(builder))) {
            throw new AssertionError("schema 9 deployment1 round-trip failed: " + retrieved1);
        }
        if (retrieved2.isEmpty() || !retrieved2.get().kingdomId().equals(kingdom2)
                || retrieved2.get().originZ() != -100 || retrieved2.get().rotationSteps() != 2) {
            throw new AssertionError("schema 9 deployment2 round-trip failed: " + retrieved2);
        }

        com.mojang.serialization.DataResult<com.mojang.serialization.JsonElement> encoded =
            KingdomSavedData.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, data);
        if (encoded.error().isPresent()) {
            throw new AssertionError("schema 9 codec encode failed: " + encoded.error().get());
        }

        com.mojang.serialization.DataResult<KingdomSavedData> decoded =
            KingdomSavedData.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, encoded.result().get());
        if (decoded.error().isPresent()) {
            throw new AssertionError("schema 9 codec decode failed: " + decoded.error().get());
        }

        KingdomSavedData roundTripped = decoded.result().get();
        if (roundTripped.starterCampDeployment(kingdom1).isEmpty()
                || roundTripped.starterCampDeployment(kingdom2).isEmpty()) {
            throw new AssertionError("schema 9 codec round-trip lost deployments");
        }
    }

    private static void schema8MigrationPreservesData() {
        String schema8Json = """
            {
                "schema_version": 8,
                "kingdoms": [],
                "army_groups": [],
                "diplomacy": [],
                "sieges": [],
                "pending_invites": [],
                "pending_diplomacy": [],
                "supply_ledgers": []
            }
            """;

        com.google.gson.JsonElement parsed = com.google.gson.JsonParser.parseString(schema8Json);
        com.mojang.serialization.DataResult<KingdomSavedData> result =
            KingdomSavedData.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, parsed);

        if (result.error().isPresent()) {
            throw new AssertionError("schema 8 migration failed: " + result.error().get());
        }

        KingdomSavedData migrated = result.result().get();
        if (migrated.schemaVersion() != 8) {
            throw new AssertionError("schema version should be preserved as 8, got: " + migrated.schemaVersion());
        }
        if (!migrated.starterCampDeployments().isEmpty()) {
            throw new AssertionError("schema 8 migration should have empty deployments");
        }
    }
}
