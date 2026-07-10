package middleearth.lotr.warmod.recruitment;

import java.util.Objects;

import middleearth.lotr.warmod.army.ArmyUnitDefinition;
import middleearth.lotr.warmod.faction.FactionAlignment;
import middleearth.lotr.warmod.faction.FactionDefinition;
import middleearth.lotr.warmod.workforce.WorkerProfession;

public final class RecruitmentContractPolicy {
    private RecruitmentContractPolicy() {
    }

    public static RecruitmentContractOffer evaluate(
            ArmyUnitDefinition unit,
            FactionDefinition faction,
            FactionAlignment alignment,
            RecruitmentCapacity capacity,
            int availableEmeralds,
            WorkerProfession workerProfession
    ) {
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(faction, "faction");
        Objects.requireNonNull(capacity, "capacity");

        if (alignment == null) {
            return RecruitmentContractOffer.rejected("unknown_player", unit, faction, workerProfession);
        }
        if (alignment.score(faction.id()) < faction.minimumHiringAlignment()) {
            return RecruitmentContractOffer.rejected("alignment_too_low", unit, faction, workerProfession);
        }
        if (availableEmeralds < unit.hireCost()) {
            return RecruitmentContractOffer.rejected("coins_too_low", unit, faction, workerProfession);
        }
        if (!capacity.hasHousingSpace()) {
            return RecruitmentContractOffer.rejected("housing_full", unit, faction, workerProfession);
        }
        if (workerProfession != null && !capacity.hasWorksiteSpace()) {
            return RecruitmentContractOffer.rejected("worksite_required", unit, faction, workerProfession);
        }
        return RecruitmentContractOffer.accepted(
                unit,
                faction,
                workerProfession,
                unit.hireCost(),
                dailyUpkeep(unit));
    }

    private static int dailyUpkeep(ArmyUnitDefinition unit) {
        return Math.max(1, (unit.hireCost() + 9) / 10);
    }
}
