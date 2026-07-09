package middleearth.lotr.warmod.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WorkerWorksiteIntegrationTest {
    private WorkerWorksiteIntegrationTest() {
    }

    public static void main(String[] args) throws IOException {
        menuExposesWorksiteButtons();
        recruitPersistsWorksiteAndUsesWorkCommand();
        screenRendersWorksiteButtons();
        languageContainsWorksiteMessages();

        System.out.println("WorkerWorksiteIntegrationTest passed");
    }

    private static void menuExposesWorksiteButtons() throws IOException {
        String menu = read("src/main/java/middleearth/lotr/warmod/menu/RecruitCommandMenu.java");

        assertContains(menu, "BUTTON_SET_WORKSITE", "set worksite button");
        assertContains(menu, "BUTTON_RETURN_WORKSITE", "return worksite button");
        assertContains(menu, "BUTTON_CLEAR_WORKSITE", "clear worksite button");
        assertContains(menu, "BUTTON_WORK_RADIUS_DECREASE", "decrease work radius button");
        assertContains(menu, "BUTTON_WORK_RADIUS_INCREASE", "increase work radius button");
    }

    private static void recruitPersistsWorksiteAndUsesWorkCommand() throws IOException {
        String entity = read("src/main/java/middleearth/lotr/warmod/entity/MiddleEarthRecruitEntity.java");
        String actions = read("src/main/java/middleearth/lotr/warmod/recruitment/RecruitmentAction.java");

        assertContains(actions, "WORK_AT_SITE", "work command action");
        assertContains(entity, "workTarget", "work target field");
        assertContains(entity, "\"WorkTargetX\"", "work target save x");
        assertContains(entity, "\"WorkRadius\"", "work radius save key");
        assertContains(entity, "DATA_WORK_RADIUS", "synched work radius");
        assertContains(entity, "WorkerWorksite(\n                        area,\n                        this.workTarget.getX(),\n                        this.workTarget.getY(),\n                        this.workTarget.getZ(),\n                        this.workRadius)", "worksite uses recruit radius");
        assertContains(entity, "worksiteScanRadius()", "scan uses configured work radius");
        assertContains(entity, "BUTTON_SET_WORKSITE", "set worksite handling");
        assertContains(entity, "BUTTON_RETURN_WORKSITE", "return worksite handling");
        assertContains(entity, "BUTTON_CLEAR_WORKSITE", "clear worksite handling");
        assertContains(entity, "BUTTON_WORK_RADIUS_DECREASE", "decrease radius handling");
        assertContains(entity, "BUTTON_WORK_RADIUS_INCREASE", "increase radius handling");
        assertContains(entity, "WorkerTaskPlanner.plan", "task planner hook");
    }

    private static void screenRendersWorksiteButtons() throws IOException {
        String screen = read("src/main/java/middleearth/lotr/warmod/client/gui/RecruitCommandScreen.java");

        assertContains(screen, "screen.kingdomwarsmiddleearth.recruit.worksite.set", "set worksite screen label");
        assertContains(screen, "screen.kingdomwarsmiddleearth.recruit.worksite.return", "return worksite screen label");
        assertContains(screen, "screen.kingdomwarsmiddleearth.recruit.worksite.clear", "clear worksite screen label");
        assertContains(screen, "screen.kingdomwarsmiddleearth.recruit.worksite.radius.decrease", "decrease radius screen label");
        assertContains(screen, "screen.kingdomwarsmiddleearth.recruit.worksite.radius.increase", "increase radius screen label");
    }

    private static void languageContainsWorksiteMessages() throws IOException {
        String language = read("src/main/resources/assets/kingdomwarsmiddleearth/lang/en_us.json");

        assertContains(language, "\"screen.kingdomwarsmiddleearth.recruit.worksite.set\"", "set label translation");
        assertContains(language, "\"screen.kingdomwarsmiddleearth.recruit.worksite.return\"", "return label translation");
        assertContains(language, "\"screen.kingdomwarsmiddleearth.recruit.worksite.clear\"", "clear label translation");
        assertContains(language, "\"screen.kingdomwarsmiddleearth.recruit.worksite.radius.decrease\"", "decrease radius label translation");
        assertContains(language, "\"screen.kingdomwarsmiddleearth.recruit.worksite.radius.increase\"", "increase radius label translation");
        assertContains(language, "\"screen.kingdomwarsmiddleearth.recruit.status.work_radius\"", "work radius status translation");
        assertContains(language, "\"message.kingdomwarsmiddleearth.recruit.worksite.set\"", "set message translation");
        assertContains(language, "\"message.kingdomwarsmiddleearth.recruit.worksite.return\"", "return message translation");
        assertContains(language, "\"message.kingdomwarsmiddleearth.recruit.worksite.missing\"", "missing message translation");
        assertContains(language, "\"message.kingdomwarsmiddleearth.recruit.worksite.clear\"", "clear message translation");
        assertContains(language, "\"message.kingdomwarsmiddleearth.recruit.worksite.radius\"", "radius message translation");
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
