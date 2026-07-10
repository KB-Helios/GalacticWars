package middleearth.lotr.warmod.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RecruitLifecycleHardeningTest {
    private RecruitLifecycleHardeningTest() {
    }

    public static void main(String[] args) throws IOException {
        String entity = read("src/main/java/middleearth/lotr/warmod/entity/MiddleEarthRecruitEntity.java");
        String lifecycle = read("src/main/java/middleearth/lotr/warmod/entity/RecruitLifecycleService.java");
        String payments = read("src/main/java/middleearth/lotr/warmod/recruitment/RecruitmentPaymentService.java");
        assertContains(entity, "deathResourcesReleased", "idempotent death finalization");
        assertContains(entity, "RecruitLifecycleService.dropCarriedItems", "worker item conservation");
        assertContains(entity, "RecruitLifecycleService.releaseSettlementState", "housing cleanup");
        assertContains(lifecycle, "unregisterRecruit", "settlement recruit cleanup");
        assertContains(lifecycle, "cancelActiveCampaigns", "commander campaign cleanup");
        assertContains(entity, "RecruitCommandMenu.isSupportedButton", "server command allowlist");
        assertContains(entity, "player.distanceToSqr(this) > 64.0", "server distance guard");
        assertContains(payments, "withdrawEmeralds", "atomic payment withdrawal");
        assertContains(payments, "refundEmeralds", "failed payment rollback");
        assertContains(entity, "kingdomData.unregisterRecruit", "failed hire registration rollback");
        System.out.println("RecruitLifecycleHardeningTest passed");
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    private static void assertContains(String value, String expected, String label) {
        if (!value.contains(expected)) {
            throw new AssertionError(label + " missing <" + expected + ">");
        }
    }
}
