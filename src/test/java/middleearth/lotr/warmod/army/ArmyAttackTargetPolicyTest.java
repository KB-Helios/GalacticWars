package middleearth.lotr.warmod.army;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import middleearth.lotr.warmod.faction.FactionCatalog;
import middleearth.lotr.warmod.faction.FactionDefinition;
import middleearth.lotr.warmod.faction.FactionId;
import middleearth.lotr.warmod.recruitment.RecruitDuty;

public final class ArmyAttackTargetPolicyTest {
    private static final FactionId GONDOR = FactionId.of("gondor");
    private static final FactionId ROHAN = FactionId.of("rohan");
    private static final FactionId MORDOR = FactionId.of("mordor");
    private static final FactionId NEUTRAL = FactionId.of("neutral");

    private ArmyAttackTargetPolicyTest() {
    }

    public static void main(String[] args) {
        enemySoldierIsAccepted();
        selfAllyAndNeutralRecruitsAreRejected();
        workersAndSameOwnerRecruitsAreRejected();
        explicitAndRetaliatoryMonstersAreAccepted();
        System.out.println("ArmyAttackTargetPolicyTest passed");
    }

    private static void enemySoldierIsAccepted() {
        assertTrue(ArmyAttackTargetPolicy.canAttackRecruit(
                factions(), GONDOR, MORDOR, false, RecruitDuty.SOLDIER), "enemy soldier");
    }

    private static void selfAllyAndNeutralRecruitsAreRejected() {
        assertFalse(ArmyAttackTargetPolicy.canAttackRecruit(
                factions(), GONDOR, GONDOR, false, RecruitDuty.SOLDIER), "same faction");
        assertFalse(ArmyAttackTargetPolicy.canAttackRecruit(
                factions(), GONDOR, ROHAN, false, RecruitDuty.SOLDIER), "allied faction");
        assertFalse(ArmyAttackTargetPolicy.canAttackRecruit(
                factions(), GONDOR, NEUTRAL, false, RecruitDuty.SOLDIER), "neutral faction");
    }

    private static void workersAndSameOwnerRecruitsAreRejected() {
        assertFalse(ArmyAttackTargetPolicy.canAttackRecruit(
                factions(), GONDOR, MORDOR, false, RecruitDuty.WORKER), "enemy worker");
        assertFalse(ArmyAttackTargetPolicy.canAttackRecruit(
                factions(), GONDOR, MORDOR, true, RecruitDuty.SOLDIER), "same owner");
    }

    private static void explicitAndRetaliatoryMonstersAreAccepted() {
        assertTrue(ArmyAttackTargetPolicy.canAttackMonster(true, false), "explicit monster target");
        assertTrue(ArmyAttackTargetPolicy.canAttackMonster(false, true), "retaliatory monster target");
        assertFalse(ArmyAttackTargetPolicy.canAttackMonster(false, false), "unprovoked monster target");
    }

    private static FactionCatalog factions() {
        Map<FactionId, FactionDefinition> definitions = new LinkedHashMap<>();
        definitions.put(GONDOR, faction(GONDOR, Set.of(ROHAN), Set.of(MORDOR)));
        definitions.put(ROHAN, faction(ROHAN, Set.of(GONDOR), Set.of(MORDOR)));
        definitions.put(MORDOR, faction(MORDOR, Set.of(), Set.of(GONDOR, ROHAN)));
        definitions.put(NEUTRAL, faction(NEUTRAL, Set.of(), Set.of()));
        return new FactionCatalog(definitions);
    }

    private static FactionDefinition faction(FactionId id, Set<FactionId> allies, Set<FactionId> enemies) {
        return new FactionDefinition(id, id.path(), 0, 0, 0, allies, enemies);
    }

    private static void assertTrue(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label + " expected true");
        }
    }

    private static void assertFalse(boolean condition, String label) {
        if (condition) {
            throw new AssertionError(label + " expected false");
        }
    }
}
