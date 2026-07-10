package middleearth.lotr.warmod.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WorkerBaseGuiIntegrationTest {
    private WorkerBaseGuiIntegrationTest() {
    }

    public static void main(String[] args) throws IOException {
        menuExposesStorageAndBaseButtons();
        screenRendersStorageAndBaseButtons();
        recruitPersistsStorageAndBaseTargets();
        languageContainsStorageAndBaseMessages();

        System.out.println("WorkerBaseGuiIntegrationTest passed");
    }

    private static void menuExposesStorageAndBaseButtons() throws IOException {
        String menu = read("src/main/java/middleearth/lotr/warmod/menu/RecruitCommandMenu.java");

        assertContains(menu, "BUTTON_SET_STORAGE", "set storage button");
        assertContains(menu, "BUTTON_BUILD_STARTER_KEEP", "build starter keep button");
        assertContains(menu, "BUTTON_NEXT_BLUEPRINT", "next blueprint button");
    }

    private static void screenRendersStorageAndBaseButtons() throws IOException {
        String screen = read("src/main/java/middleearth/lotr/warmod/client/gui/RecruitCommandScreen.java");

        assertContains(screen, "screen.kingdomwarsmiddleearth.recruit.storage.set", "set storage label");
        assertContains(screen, "screen.kingdomwarsmiddleearth.recruit.base.starter_keep", "starter keep label");
        assertContains(screen, "screen.kingdomwarsmiddleearth.recruit.base.next", "next blueprint label");
    }

    private static void recruitPersistsStorageAndBaseTargets() throws IOException {
        String entity = read("src/main/java/middleearth/lotr/warmod/entity/MiddleEarthRecruitEntity.java");

        assertContains(entity, "storageTarget", "storage target field");
        assertContains(entity, "baseTarget", "base target field");
        assertContains(entity, "\"StorageTargetX\"", "storage target save x");
        assertContains(entity, "\"BaseTargetX\"", "base target save x");
        assertContains(entity, "\"SelectedBlueprint\"", "selected blueprint save key");
        assertContains(entity, "DATA_SELECTED_BLUEPRINT", "selected blueprint client sync");
        assertContains(entity, "KingdomBaseBuildPlanner.planNext", "base build planner hook");
        assertContains(entity, "WorkerResourcePlanner.plan", "resource planner hook");
    }

    private static void languageContainsStorageAndBaseMessages() throws IOException {
        String language = read("src/main/resources/assets/kingdomwarsmiddleearth/lang/en_us.json");

        assertContains(language, "\"screen.kingdomwarsmiddleearth.recruit.storage.set\"", "storage screen translation");
        assertContains(language, "\"screen.kingdomwarsmiddleearth.recruit.base.starter_keep\"", "starter keep screen translation");
        assertContains(language, "\"message.kingdomwarsmiddleearth.recruit.storage.set\"", "storage message translation");
        assertContains(language, "\"message.kingdomwarsmiddleearth.recruit.base.starter_keep\"", "starter keep message translation");
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
