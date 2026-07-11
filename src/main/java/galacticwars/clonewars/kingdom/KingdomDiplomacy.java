package galacticwars.clonewars.kingdom;

import java.util.Objects;
import java.util.UUID;

public record KingdomDiplomacy(
        UUID firstKingdomId,
        UUID secondKingdomId,
        KingdomRelation relation,
        long treatyExpiresGameTime,
        long cooldownUntilGameTime,
        boolean embargo
) {
    public KingdomDiplomacy {
        Objects.requireNonNull(firstKingdomId, "firstKingdomId");
        Objects.requireNonNull(secondKingdomId, "secondKingdomId");
        Objects.requireNonNull(relation, "relation");
        if (firstKingdomId.equals(secondKingdomId)) {
            throw new IllegalArgumentException("kingdom cannot have diplomacy with itself");
        }
        if (compare(firstKingdomId, secondKingdomId) > 0) {
            UUID swap = firstKingdomId;
            firstKingdomId = secondKingdomId;
            secondKingdomId = swap;
        }
        treatyExpiresGameTime = Math.max(0L, treatyExpiresGameTime);
        cooldownUntilGameTime = Math.max(0L, cooldownUntilGameTime);
    }

    public static KingdomDiplomacy neutral(UUID first, UUID second) {
        return new KingdomDiplomacy(first, second, KingdomRelation.NEUTRAL, 0L, 0L, false);
    }

    public boolean matches(UUID first, UUID second) {
        return (firstKingdomId.equals(first) && secondKingdomId.equals(second))
                || (firstKingdomId.equals(second) && secondKingdomId.equals(first));
    }

    public boolean treatyActive(long gameTime) {
        return treatyExpiresGameTime > gameTime;
    }

    public KingdomDiplomacy withRelation(KingdomRelation relation, long cooldownUntil) {
        return new KingdomDiplomacy(firstKingdomId, secondKingdomId, relation,
                relation == KingdomRelation.ALLY ? treatyExpiresGameTime : 0L,
                cooldownUntil, embargo);
    }

    public KingdomDiplomacy withTreaty(long expiresAt, long cooldownUntil) {
        return new KingdomDiplomacy(firstKingdomId, secondKingdomId, KingdomRelation.ALLY,
                expiresAt, cooldownUntil, embargo);
    }

    public KingdomDiplomacy withEmbargo(boolean embargo) {
        return new KingdomDiplomacy(firstKingdomId, secondKingdomId, relation,
                treatyExpiresGameTime, cooldownUntilGameTime, embargo);
    }

    private static int compare(UUID first, UUID second) {
        int high = Long.compareUnsigned(first.getMostSignificantBits(), second.getMostSignificantBits());
        return high != 0 ? high
                : Long.compareUnsigned(first.getLeastSignificantBits(), second.getLeastSignificantBits());
    }
}
