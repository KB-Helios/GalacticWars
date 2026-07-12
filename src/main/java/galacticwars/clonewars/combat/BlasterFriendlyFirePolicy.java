package galacticwars.clonewars.combat;

import galacticwars.clonewars.faction.FactionRelation;
import java.util.Objects;

public final class BlasterFriendlyFirePolicy {
    private BlasterFriendlyFirePolicy() {
    }

    public static boolean blocksHit(
            boolean targetIsPlayer,
            boolean sameOwner,
            FactionRelation relation,
            boolean allowFriendlyFire,
            boolean allowPvp
    ) {
        Objects.requireNonNull(relation, "relation");
        if (targetIsPlayer) {
            return !allowPvp;
        }
        return !allowFriendlyFire
                && (sameOwner || relation == FactionRelation.SAME || relation == FactionRelation.ALLY);
    }

    public static boolean blocksRecruitHitOnPlayer(
            boolean sameOwner,
            FactionRelation relation,
            boolean allowPvp
    ) {
        Objects.requireNonNull(relation, "relation");
        return !allowPvp
                || sameOwner
                || relation != FactionRelation.ENEMY;
    }
}
