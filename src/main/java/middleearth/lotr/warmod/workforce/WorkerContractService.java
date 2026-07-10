package middleearth.lotr.warmod.workforce;

import middleearth.lotr.warmod.recruitment.RecruitDuty;

public final class WorkerContractService {
    private WorkerContractService() {
    }

    public static ExitDecision evaluateExit(
            RecruitDuty duty,
            boolean hasWorkerProfession,
            boolean inventoryEmpty,
            boolean buildActive
    ) {
        if (duty == RecruitDuty.COMMANDER || !hasWorkerProfession) {
            return ExitDecision.NOT_WORKER;
        }
        if (!inventoryEmpty) {
            return ExitDecision.CARRIED_ITEMS;
        }
        if (buildActive) {
            return ExitDecision.ACTIVE_BUILD;
        }
        return ExitDecision.ACCEPTED;
    }

    public enum ExitDecision {
        ACCEPTED,
        NOT_WORKER,
        CARRIED_ITEMS,
        ACTIVE_BUILD
    }
}
