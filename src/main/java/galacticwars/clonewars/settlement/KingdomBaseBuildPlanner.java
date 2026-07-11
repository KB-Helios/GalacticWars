package galacticwars.clonewars.settlement;

import java.util.Objects;
import java.util.Set;

import galacticwars.clonewars.workforce.ResourceInventory;

public final class KingdomBaseBuildPlanner {
    private KingdomBaseBuildPlanner() {
    }

    public static KingdomBaseBuildDecision planNext(
            KingdomBaseBlueprint blueprint,
            ResourceInventory storage,
            Set<BaseBlockPlacement> completedPlacements
    ) {
        Objects.requireNonNull(blueprint, "blueprint");
        Objects.requireNonNull(storage, "storage");
        Objects.requireNonNull(completedPlacements, "completedPlacements");

        for (BaseBlockPlacement placement : blueprint.placements()) {
            if (!completedPlacements.contains(placement)) {
                if (!storage.hasAtLeast(placement.itemId(), 1)) {
                    int missing = Math.max(1, blueprint.requiredResources().count(placement.itemId()) - storage.count(placement.itemId()));
                    return KingdomBaseBuildDecision.gather(placement.itemId(), missing);
                }
                return KingdomBaseBuildDecision.place(placement);
            }
        }
        return KingdomBaseBuildDecision.complete();
    }

    public static KingdomBaseBuildDecision planNext(
            KingdomBaseBlueprint blueprint,
            ResourceInventory storage,
            int completedPlacementCount
    ) {
        Objects.requireNonNull(blueprint, "blueprint");
        if (completedPlacementCount < 0) {
            throw new IllegalArgumentException("completedPlacementCount cannot be negative");
        }
        return planNext(
                blueprint,
                storage,
                Set.copyOf(blueprint.placements().subList(0, Math.min(completedPlacementCount, blueprint.placements().size()))));
    }
}
