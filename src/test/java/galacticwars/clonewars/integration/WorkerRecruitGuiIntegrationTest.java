package galacticwars.clonewars.integration;

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
            "screen.galacticwars.recruit.profession.farmer",
            "screen.galacticwars.recruit.profession.lumberjack",
            "screen.galacticwars.recruit.profession.fisherman",
            "screen.galacticwars.recruit.profession.animal_farmer",
            "screen.galacticwars.recruit.profession.miner",
            "screen.galacticwars.recruit.profession.builder",
            "screen.galacticwars.recruit.profession.cook",
            "screen.galacticwars.recruit.profession.merchant",
            "screen.galacticwars.recruit.profession.courier"
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
        String menu = read("src/main/java/galacticwars/clonewars/menu/RecruitCommandMenu.java");

        for (String constant : PROFESSION_CONSTANTS) {
            assertContains(menu, constant, "menu button " + constant);
        }
        assertContains(menu, "workerProfessionButtonIds", "menu profession button list");
        assertContains(menu, "BUTTON_RETURN_TO_SOLDIER", "return-to-soldier action");
        assertContains(menu, "BUTTON_CANCEL_BUILD", "building cancellation action");
        assertContains(menu, "isSupportedButton", "server button allowlist");
        assertContains(menu, "this.stillValid(player)", "server menu distance validation");
        assertContains(menu, "player.level() == this.level", "server menu dimension validation");
    }

    private static void recruitPersistsAndAppliesWorkerProfession() throws IOException {
        String entity = read("src/main/java/galacticwars/clonewars/entity/GalacticRecruitEntity.java");
        String actions = read("src/main/java/galacticwars/clonewars/menu/RecruitCommandAction.java");

        assertContains(entity, "DATA_WORKER_PROFESSION", "synched worker profession");
        assertContains(actions, "WorkerProfessionCatalog.professionForButton(buttonId)",
                "typed button to profession mapping");
        assertContains(entity, "RecruitCommandAction.workerProfession(buttonId)",
                "entity profession action boundary");
        assertContains(entity, "tryAssignWorkerProfession(player, profession.get())", "paid profession assignment");
        assertContains(entity, "WorkerProfessionDefinition definition", "profession definition cost lookup");
        assertContains(entity, "definition.hireCostCredits()", "profession credit contract cost");
        assertContains(entity, "resumeWorkAfterProfessionAssignment", "profession assignment resumes work");
        assertContains(entity, "RecruitmentAction.WORK_AT_SITE", "profession assignment can activate work mode");
        assertContains(entity, "\"WorkerProfession\"", "profession save data");
        assertContains(entity, "applyWorkerEquipment", "role equipment application");
        assertContains(entity, "tryReturnToSoldier", "safe worker contract exit");
        assertContains(entity, "tryCancelBuilding", "safe building cancellation");
        assertContains(entity, "workerInventoryIsEmpty", "carried-item contract exit guard");
        assertContains(entity, "restorePreviousAssignment", "atomic profession rollback helper");
    }

    private static void screenRendersProfessionButtons() throws IOException {
        String screen = read("src/main/java/galacticwars/clonewars/client/gui/RecruitCommandScreen.java");

        assertContains(screen, "workerProfessionButtonIds", "screen uses menu role buttons");
        assertContains(
                screen,
                "Entity entity = (this.minecraft != null && this.minecraft.level != null)",
                "screen init null-safe entity lookup");
        assertContains(screen, "WorkerProfessionCatalog.definitionForButton", "dynamic profession label lookup");
        assertContains(screen, "RecruitCommandMenu.BUTTON_PROMOTE_COMMANDER", "commander promotion button");
        assertContains(screen, "RecruitCommandMenu.BUTTON_RETURN_TO_SOLDIER", "return-to-soldier button");
        assertContains(screen, "RecruitCommandMenu.BUTTON_CANCEL_BUILD", "cancel-building button");
    }

    private static void languageContainsProfessionLabels() throws IOException {
        String language = read("src/main/resources/assets/galacticwars/lang/en_us.json");

        for (String key : PROFESSION_TRANSLATIONS) {
            assertContains(language, "\"" + key + "\"", "language role key " + key);
        }
        assertContains(language, "\"message.galacticwars.recruit.profession.need_credits\"", "profession cost failure message");
        assertContains(language, "\"message.galacticwars.recruit.profession.contract\"", "profession contract message");
        assertContains(language, "\"message.galacticwars.recruit.soldier.returned\"", "soldier return message");
        assertContains(language, "\"message.galacticwars.recruit.base.cancelled\"", "building cancellation message");
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
