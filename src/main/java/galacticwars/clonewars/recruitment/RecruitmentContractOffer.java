package galacticwars.clonewars.recruitment;

import java.util.Objects;

import galacticwars.clonewars.army.ArmyUnitDefinition;
import galacticwars.clonewars.faction.FactionDefinition;
import galacticwars.clonewars.workforce.WorkerProfession;

public record RecruitmentContractOffer(
        boolean accepted,
        String reasonCode,
        ArmyUnitDefinition unit,
        FactionDefinition faction,
        WorkerProfession workerProfession,
        int hireCost,
        int dailyUpkeep
) {
    public RecruitmentContractOffer {
        reasonCode = requireNonBlank(reasonCode, "reasonCode");
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(faction, "faction");
        if (hireCost < 0) {
            throw new IllegalArgumentException("hireCost cannot be negative");
        }
        if (dailyUpkeep < 0) {
            throw new IllegalArgumentException("dailyUpkeep cannot be negative");
        }
    }

    public static RecruitmentContractOffer accepted(
            ArmyUnitDefinition unit,
            FactionDefinition faction,
            WorkerProfession workerProfession,
            int hireCost,
            int dailyUpkeep
    ) {
        return new RecruitmentContractOffer(true, "accepted", unit, faction, workerProfession, hireCost, dailyUpkeep);
    }

    public static RecruitmentContractOffer rejected(
            String reasonCode,
            ArmyUnitDefinition unit,
            FactionDefinition faction,
            WorkerProfession workerProfession
    ) {
        return new RecruitmentContractOffer(false, reasonCode, unit, faction, workerProfession, 0, 0);
    }

    public String workerProfessionId() {
        return workerProfession == null ? "" : workerProfession.id();
    }

    public String workerProfessionName() {
        return workerProfession == null ? "" : displayWorkerProfession(workerProfession);
    }

    public String summaryTitle() {
        if (workerProfession == null) {
            return "Hire " + unit.displayName();
        }
        return "Hire " + unit.displayName() + " as " + displayWorkerProfession(workerProfession);
    }

    static String displayWorkerProfession(WorkerProfession profession) {
        String[] parts = profession.id().split("_");
        StringBuilder display = new StringBuilder();
        for (String part : parts) {
            if (!display.isEmpty()) {
                display.append(' ');
            }
            display.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return display.toString();
    }

    private static String requireNonBlank(String value, String label) {
        Objects.requireNonNull(value, label);
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return trimmed;
    }
}
