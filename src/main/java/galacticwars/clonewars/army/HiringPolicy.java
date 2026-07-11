package galacticwars.clonewars.army;

import java.util.Objects;

import galacticwars.clonewars.faction.FactionAlignment;
import galacticwars.clonewars.faction.FactionDefinition;

public final class HiringPolicy {
    private HiringPolicy() {
    }

    public static HiringDecision canHire(
            FactionAlignment alignment,
            FactionDefinition faction,
            int availableCoins,
            int ownedRecruitCount
    ) {
        Objects.requireNonNull(faction, "faction");
        if (alignment == null) {
            return HiringDecision.rejected("unknown_player");
        }
        if (alignment.score(faction.id()) < faction.minimumHiringAlignment()) {
            return HiringDecision.rejected("alignment_too_low");
        }
        if (availableCoins < faction.hireCost()) {
            return HiringDecision.rejected("coins_too_low");
        }
        int effectiveLimit = Math.addExact(
                faction.maxOwnedRecruits(), faction.strategy().recruitmentCapacityBonus());
        if (ownedRecruitCount >= effectiveLimit) {
            return HiringDecision.rejected("recruit_limit_reached");
        }
        return HiringDecision.accepted(faction.hireCost());
    }
}
