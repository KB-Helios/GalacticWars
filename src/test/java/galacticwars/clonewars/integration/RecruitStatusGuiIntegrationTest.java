package galacticwars.clonewars.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RecruitStatusGuiIntegrationTest {
    private RecruitStatusGuiIntegrationTest() {
    }

    public static void main(String[] args) throws IOException {
        screenRendersRecruitStatusPanel();
        entityExposesWorkerStatusLines();
        languageContainsStatusLabels();

        System.out.println("RecruitStatusGuiIntegrationTest passed");
    }

    private static void screenRendersRecruitStatusPanel() throws IOException {
        String screen = read("src/main/java/galacticwars/clonewars/client/gui/RecruitCommandScreen.java");

        assertContains(screen, "GuiGraphicsExtractor", "gui graphics extraction parameter");
        assertContains(screen, "extractRenderState", "status extraction method");
        assertContains(screen, "drawRecruitStatusPanel", "status panel method");
        assertContains(screen, "recruit.recruitStatusLines()", "entity status lines");
        assertContains(screen, "screen.galacticwars.recruit.status.title", "status title label");
        assertContains(screen, "drawCompactStatusTooltip", "narrow-screen status layout");
        assertContains(screen, "setComponentTooltipForNextFrame", "narrow-screen status tooltip");
    }

    private static void entityExposesWorkerStatusLines() throws IOException {
        String entity = read("src/main/java/galacticwars/clonewars/entity/GalacticRecruitEntity.java");

        assertContains(entity, "recruitStatusLines", "status lines method");
        assertContains(entity, "screen.galacticwars.recruit.status.command", "command status key");
        assertContains(entity, "screen.galacticwars.recruit.status.profession", "profession status key");
        assertContains(entity, "screen.galacticwars.recruit.status.resource_action", "resource action status key");
        assertContains(entity, "screen.galacticwars.recruit.status.base_progress", "base progress status key");
        assertContains(entity, "planKingdomWorkOrder", "kingdom work order status method");
        assertContains(entity, "screen.galacticwars.recruit.status.kingdom_order", "kingdom order status key");
        assertContains(entity, "KingdomSettlementPlanner.planNextWorkOrder", "kingdom work order planner hook");
    }

    private static void languageContainsStatusLabels() throws IOException {
        String language = read("src/main/resources/assets/galacticwars/lang/en_us.json");

        assertContains(language, "\"screen.galacticwars.recruit.status.title\"", "status title translation");
        assertContains(language, "\"screen.galacticwars.recruit.status.command\"", "command status translation");
        assertContains(language, "\"screen.galacticwars.recruit.status.profession\"", "profession status translation");
        assertContains(language, "\"screen.galacticwars.recruit.status.resource_action\"", "resource status translation");
        assertContains(language, "\"screen.galacticwars.recruit.status.base_progress\"", "base progress translation");
        assertContains(language, "\"screen.galacticwars.recruit.status.kingdom_order\"", "kingdom order translation");
        assertContains(language, "\"screen.galacticwars.recruit.kingdom_order.build_block\"", "kingdom order type translation");
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    private static void assertContains(String haystack, String needle, String label) {
        if (!haystack.contains(needle)) {
            throw new AssertionError(label + " missing <" + needle + ">");
        }
    }
}
