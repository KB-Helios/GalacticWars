package galacticwars.clonewars.army;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import galacticwars.clonewars.faction.FactionCatalog;
import galacticwars.clonewars.faction.FactionId;
import galacticwars.clonewars.faction.FactionRelation;

public final class ArmyTargetSelector {
    private static final int BASE_ENEMY_SCORE = 1000;
    private static final int OWNER_ATTACKER_BONUS = 10000;
    private static final int RECRUIT_ATTACKER_BONUS = 5000;
    private static final int THREAT_MULTIPLIER = 20;

    private ArmyTargetSelector() {
    }

    public static Optional<ArmyTargetSelection> selectTarget(
            FactionId ownFaction,
            ArmyPosition origin,
            List<ArmyTargetCandidate> candidates,
            FactionCatalog factions,
            int maxRange
    ) {
        Objects.requireNonNull(ownFaction, "ownFaction");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(candidates, "candidates");
        Objects.requireNonNull(factions, "factions");
        if (maxRange < 0) {
            throw new IllegalArgumentException("maxRange cannot be negative");
        }

        ArmyTargetCandidate bestCandidate = null;
        int bestScore = Integer.MIN_VALUE;
        int bestDistance = Integer.MAX_VALUE;
        long maxDistanceSquared = (long) maxRange * maxRange;

        for (ArmyTargetCandidate candidate : candidates) {
            Objects.requireNonNull(candidate, "candidate");
            if (factions.relation(ownFaction, candidate.factionId()) != FactionRelation.ENEMY) {
                continue;
            }
            if (distanceSquared(origin, candidate.position()) > maxDistanceSquared) {
                continue;
            }

            int distance = manhattanDistance(origin, candidate.position());
            int score = score(candidate, distance);
            if (isBetter(candidate, score, distance, bestCandidate, bestScore, bestDistance)) {
                bestCandidate = candidate;
                bestScore = score;
                bestDistance = distance;
            }
        }

        if (bestCandidate == null) {
            return Optional.empty();
        }

        return Optional.of(new ArmyTargetSelection(
                bestCandidate.entityId(),
                bestCandidate.position(),
                reasonCode(bestCandidate),
                bestScore));
    }

    private static int score(ArmyTargetCandidate candidate, int distance) {
        int score = BASE_ENEMY_SCORE + candidate.threat() * THREAT_MULTIPLIER - distance;
        if (candidate.attackingOwner()) {
            score += OWNER_ATTACKER_BONUS;
        }
        if (candidate.attackingRecruit()) {
            score += RECRUIT_ATTACKER_BONUS;
        }
        return Math.max(0, score);
    }

    private static boolean isBetter(
            ArmyTargetCandidate candidate,
            int score,
            int distance,
            ArmyTargetCandidate bestCandidate,
            int bestScore,
            int bestDistance
    ) {
        if (bestCandidate == null) {
            return true;
        }
        if (score != bestScore) {
            return score > bestScore;
        }
        if (distance != bestDistance) {
            return distance < bestDistance;
        }
        return candidate.entityId().toString().compareTo(bestCandidate.entityId().toString()) < 0;
    }

    private static String reasonCode(ArmyTargetCandidate candidate) {
        if (candidate.attackingOwner()) {
            return "protect_owner";
        }
        if (candidate.attackingRecruit()) {
            return "self_defense";
        }
        return "hostile_threat";
    }

    private static long distanceSquared(ArmyPosition first, ArmyPosition second) {
        long dx = (long) first.x() - second.x();
        long dy = (long) first.y() - second.y();
        long dz = (long) first.z() - second.z();
        return dx * dx + dy * dy + dz * dz;
    }

    private static int manhattanDistance(ArmyPosition first, ArmyPosition second) {
        return Math.abs(first.x() - second.x())
                + Math.abs(first.y() - second.y())
                + Math.abs(first.z() - second.z());
    }
}
