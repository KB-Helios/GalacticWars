package middleearth.lotr.warmod.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WorkerRecruitGuiIntegrationTest {
    private static final String[] PROFESSION_CONSTANTS = {
            "BUTTON_ASSIGN_FARMER",
            "BUTTON_ASSIGN_LUMBERJACK",
            "BUTTON_ASSIGN_FISHERMAN",
            "BUTTON_ASSIGN_ANIMAL_FARMER",
            "BUTTON_ASSIGN_MINER",
            "BUTTON_ASSIGN_BUILDER",
            "BUTTON_ASSIGN_COOK",
            "BUTTON_ASSIGN_MERCHANT",
            "BUTTON_ASSIGN_COURIER"
    };

    private static final String[] PROFESSION_TRANSLATIONS = {
            "screen.kingdomwarsmiddleearth.recruit.profession.farmer",
            "screen.kingdomwarsmiddleearth.recruit.profession.lumberjack",
            "screen.kingdomwarsmiddleearth.recruit.profession.fisherman",
            "screen.kingdomwarsmiddleearth.recruit.profession.animal_farmer",
            "screen.kingdomwarsmiddleearth.recruit.profession.miner",
            "screen.kingdomwarsmiddleearth.recruit.profession.builder",
            "screen.kingdomwarsmiddleearth.recruit.profession.cook",
            "screen.kingdomwarsmiddleearth.recruit.profession.merchant",
            "screen.kingdomwarsmiddleearth.recruit.profession.courier"
    };

    private WorkerRecruitGuiIntegrationTest() {
    }

    public static void main(String[] args) throws IOException {
        menuExposesProfessionButtons();
        recruitPersistsAndAppliesWorkerProfession();
        screenRendersProfessionButtons();
        languageContainsProfessionLabels();

        System.out.println("WorkerRecruitGuiIntegrationTest passed");
    }

    private static void menuExposesProfessionButtons() throws IOException {
        String menu = read("src/main/java/middleearth/lotr/warmod/menu/RecruitCommandMenu.java");

        for (String constant : PROFESSION_CONSTANTS) {
            assertContains(menu, constant, "menu button " + constant);
        }
        assertContains(menu, "workerProfessionButtonIds", "menu profession button list");
    }

    private static void recruitPersistsAndAppliesWorkerProfession() throws IOException {
        String entity = read("src/main/java/middleearth/lotr/warmod/entity/MiddleEarthRecruitEntity.java");

        assertContains(entity, "DATA_WORKER_PROFESSION", "synched worker profession");
        assertContains(entity, "WorkerProfessionCatalog.professionForButton(buttonId)", "button to profession mapping");
        assertContains(entity, "tryAssignWorkerProfession(player, profession.get())", "paid profession assignment");
        assertContains(entity, "WorkerProfessionDefinition definition", "profession definition cost lookup");
        assertContains(entity, "definition.hireCostEmeralds()", "profession emerald contract cost");
        assertContains(entity, "resumeWorkAfterProfessionAssignment", "profession assignment resumes work");
        assertContains(entity, "RecruitmentAction.WORK_AT_SITE", "profession assignment can activate work mode");
        assertContains(entity, "\"WorkerProfession\"", "profession save data");
        assertContains(entity, "applyWorkerEquipment", "role equipment application");
    }

    private static void screenRendersProfessionButtons() throws IOException {
        String screen = read("src/main/java/middleearth/lotr/warmod/client/gui/RecruitCommandScreen.java");

        assertContains(screen, "workerProfessionButtonIds", "screen uses menu role buttons");
        assertContains(
                screen,
                "Entity entity = (this.minecraft != null && this.minecraft.level != null)",
                "screen init null-safe entity lookup");
        assertContains(screen, "WorkerProfessionCatalog.definitionForButton", "dynamic profession label lookup");
        assertContains(screen, "RecruitCommandMenu.BUTTON_PROMOTE_COMMANDER", "commander promotion button");
    }

    private static void languageContainsProfessionLabels() throws IOException {
        String language = read("src/main/resources/assets/kingdomwarsmiddleearth/lang/en_us.json");

        for (String key : PROFESSION_TRANSLATIONS) {
            assertContains(language, "\"" + key + "\"", "language role key " + key);
        }
        assertContains(language, "\"message.kingdomwarsmiddleearth.recruit.profession.need_emeralds\"", "profession cost failure message");
        assertContains(language, "\"message.kingdomwarsmiddleearth.recruit.profession.contract\"", "profession contract message");
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    private static void assertContains(String text, String expected, String label) {
        if (!text.contains(expected)) {
            throw new AssertionError(label + " missing <" + expected + ">");
        }
    }
}
