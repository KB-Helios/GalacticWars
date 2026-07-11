package galacticwars.clonewars.workforce;

import galacticwars.clonewars.recruitment.RecruitDuty;

public final class WorkerContractServiceTest {
    private WorkerContractServiceTest() {
    }

    public static void main(String[] args) {
        assertEquals(WorkerContractService.ExitDecision.ACCEPTED,
                WorkerContractService.evaluateExit(RecruitDuty.WORKER, true, true, false),
                "clean worker exit");
        assertEquals(WorkerContractService.ExitDecision.CARRIED_ITEMS,
                WorkerContractService.evaluateExit(RecruitDuty.WORKER, true, false, false),
                "carried item guard");
        assertEquals(WorkerContractService.ExitDecision.ACTIVE_BUILD,
                WorkerContractService.evaluateExit(RecruitDuty.WORKER, true, true, true),
                "active build guard");
        assertEquals(WorkerContractService.ExitDecision.NOT_WORKER,
                WorkerContractService.evaluateExit(RecruitDuty.COMMANDER, false, true, false),
                "commander guard");
        assertEquals(WorkerContractService.ExitDecision.NOT_WORKER,
                WorkerContractService.evaluateExit(RecruitDuty.WORKER, false, true, false),
                "worker without profession guard");
        System.out.println("WorkerContractServiceTest passed");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
