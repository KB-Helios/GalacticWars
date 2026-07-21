package galacticwars.clonewars.menu;

import galacticwars.clonewars.workforce.WorkerProfession;
import galacticwars.clonewars.workforce.WorkerProfessionCatalog;

public final class RecruitCommandActionTest {
    private RecruitCommandActionTest() {
    }

    public static void main(String[] args) {
        mapsStableMenuIds();
        mapsEnabledProfessions();
        rejectsUnknownIds();
        System.out.println("RecruitCommandActionTest passed");
    }

    private static void mapsStableMenuIds() {
        assertEquals(RecruitCommandAction.HIRE,
                RecruitCommandAction.fromButtonId(RecruitCommandMenu.BUTTON_HIRE).orElseThrow(), "hire");
        assertEquals(RecruitCommandAction.MOVE,
                RecruitCommandAction.fromButtonId(RecruitCommandMenu.BUTTON_MOVE).orElseThrow(), "move");
        assertEquals(RecruitCommandAction.CYCLE_FORMATION,
                RecruitCommandAction.fromButtonId(RecruitCommandMenu.BUTTON_CYCLE_FORMATION).orElseThrow(),
                "formation");
        assertEquals(RecruitCommandAction.OPEN_LOADOUT,
                RecruitCommandAction.fromButtonId(RecruitCommandMenu.BUTTON_OPEN_LOADOUT).orElseThrow(),
                "loadout");
    }

    private static void mapsEnabledProfessions() {
        int farmerButton = WorkerProfessionCatalog.definition(WorkerProfession.FARMER).orElseThrow().commandButtonId();
        assertEquals(RecruitCommandAction.ASSIGN_WORKER_PROFESSION,
                RecruitCommandAction.fromButtonId(farmerButton).orElseThrow(), "profession action");
        assertEquals(WorkerProfession.FARMER,
                RecruitCommandAction.workerProfession(farmerButton).orElseThrow(), "profession value");
    }

    private static void rejectsUnknownIds() {
        if (RecruitCommandAction.fromButtonId(Integer.MAX_VALUE).isPresent()) {
            throw new AssertionError("unknown button id accepted");
        }
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
    }
}
