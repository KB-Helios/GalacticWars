package galacticwars.clonewars.army;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import galacticwars.clonewars.faction.FactionCatalog;
import galacticwars.clonewars.faction.FactionId;

public final class ArmyEngagementPlanner {
    private ArmyEngagementPlanner() {
    }

    public static ArmyEngagementDecision plan(
            ArmyEngagementStance stance,
            FactionId ownFaction,
            ArmyPosition origin,
            List<ArmyTargetCandidate> candidates,
            FactionCatalog factions,
            int maxRange
    ) {
        Objects.requireNonNull(stance, "stance");
        Objects.requireNonNull(ownFaction, "ownFaction");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(candidates, "candidates");
        Objects.requireNonNull(factions, "factions");
        if (maxRange < 0) {
            throw new IllegalArgumentException("maxRange cannot be negative");
        }

        for (ArmyTargetCandidate candidate : candidates) {
            Objects.requireNonNull(candidate, "candidate");
        }

        return switch (stance) {
            case PASSIVE -> ArmyEngagementDecision.idle("passive_stance");
            case DEFENSIVE -> planDefensive(ownFaction, origin, candidates, factions, maxRange);
            case AGGRESSIVE -> planAggressive(ownFaction, origin, candidates, factions, maxRange);
        };
    }

    private static ArmyEngagementDecision planDefensive(
            FactionId ownFaction,
            ArmyPosition origin,
            List<ArmyTargetCandidate> candidates,
            FactionCatalog factions,
            int maxRange
    ) {
        List<ArmyTargetCandidate> defensiveCandidates = new ArrayList<>();
        for (ArmyTargetCandidate candidate : candidates) {
            if (candidate.attackingOwner() || candidate.attackingRecruit()) {
                defensiveCandidates.add(candidate);
            }
        }

        Optional<ArmyTargetSelection> selection = ArmyTargetSelector.selectTarget(
                ownFaction,
                origin,
                defensiveCandidates,
                factions,
                maxRange);
        return selection.map(ArmyEngagementDecision::engage)
                .orElseGet(() -> ArmyEngagementDecision.idle("no_defensive_threat"));
    }

    private static ArmyEngagementDecision planAggressive(
            FactionId ownFaction,
            ArmyPosition origin,
            List<ArmyTargetCandidate> candidates,
            FactionCatalog factions,
            int maxRange
    ) {
        Optional<ArmyTargetSelection> selection = ArmyTargetSelector.selectTarget(
                ownFaction,
                origin,
                candidates,
                factions,
                maxRange);
        return selection.map(ArmyEngagementDecision::engage)
                .orElseGet(() -> ArmyEngagementDecision.idle("no_target"));
    }
}
