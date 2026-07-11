package galacticwars.clonewars.army;

import java.util.Objects;
import galacticwars.clonewars.faction.FactionCatalog;
import galacticwars.clonewars.faction.FactionId;
import galacticwars.clonewars.faction.FactionRelation;
import galacticwars.clonewars.recruitment.RecruitDuty;

public final class ArmyAttackTargetPolicy {
    private ArmyAttackTargetPolicy() {
    }

    public static boolean canAttackRecruit(
            FactionCatalog factions,
            FactionId attackerFaction,
            FactionId targetFaction,
            boolean sameOwner,
            RecruitDuty targetDuty
    ) {
        Objects.requireNonNull(factions, "factions");
        Objects.requireNonNull(attackerFaction, "attackerFaction");
        Objects.requireNonNull(targetFaction, "targetFaction");
        Objects.requireNonNull(targetDuty, "targetDuty");
        return !sameOwner
                && targetDuty != RecruitDuty.WORKER
                && factions.relation(attackerFaction, targetFaction) == FactionRelation.ENEMY;
    }

    public static boolean canAttackMonster(boolean explicitOrderTarget, boolean retaliationTarget) {
        return explicitOrderTarget || retaliationTarget;
    }
}
