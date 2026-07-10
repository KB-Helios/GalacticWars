package middleearth.lotr.warmod.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CommanderRuntimeIntegrationTest {
    private CommanderRuntimeIntegrationTest() {
    }

    public static void main(String[] args) throws IOException {
        String entity = Files.readString(Path.of(
                "src/main/java/middleearth/lotr/warmod/entity/MiddleEarthRecruitEntity.java"));
        String savedData = Files.readString(Path.of(
                "src/main/java/middleearth/lotr/warmod/kingdom/KingdomSavedData.java"));
        assertContains(entity, "tryPromoteCommander", "commander promotion");
        assertContains(savedData, "hasCommanderSlot", "completed-keep commander gate");
        assertContains(entity, "linkLoadedSoldiersToCommander", "army group delegation");
        assertContains(entity, "applyCommanderOrderToLoadedGroup", "commander group orders");
        assertContains(entity, "reserveEmeralds", "atomic campaign reservation");
        assertContains(entity, "campaignDelayApplied", "large-gap campaign pause accounting");
        assertContains(entity, "remainingDelay", "single-application unloaded settlement delay");
        assertContains(entity, "applyPendingCampaignRefunds", "commander-loss pending refund");
        assertContains(entity, "findSafeRallyPosition", "safe recruit arrivals");
        assertContains(entity, "cancelActiveCommanderCampaign", "death cancellation");
        assertContains(savedData, "clearCommander", "commander replacement requirement");
        System.out.println("CommanderRuntimeIntegrationTest passed");
    }

    private static void assertContains(String value, String expected, String label) {
        if (!value.contains(expected)) {
            throw new AssertionError(label + " missing <" + expected + ">");
        }
    }
}
