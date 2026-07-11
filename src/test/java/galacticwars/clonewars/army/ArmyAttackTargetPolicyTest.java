package galacticwars.clonewars.army;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import galacticwars.clonewars.faction.FactionCatalog;
import galacticwars.clonewars.faction.FactionDefinition;
import galacticwars.clonewars.faction.FactionId;
import galacticwars.clonewars.recruitment.RecruitDuty;

public final class ArmyAttackTargetPolicyTest {
    private static final FactionId REPUBLIC = FactionId.of("republic");
    private static final FactionId MANDALORIAN = FactionId.of("mandalorian");
    private static final FactionId SEPARATIST = FactionId.of("separatist");
    private static final FactionId NEUTRAL = FactionId.of("neutral");

    private ArmyAttackTargetPolicyTest() {
    }

    public static void main(String[] args) {
        enemySoldierIsAccepted();
        sameAllyAndNeutralRecruitsAreRejected();
        workersAndSameOwnerRecruitsAreRejected();
        explicitAndRetaliatoryMonstersAreAccepted();
        System.out.println("ArmyAttackTargetPolicyTest passed");
    }

    private static void enemySoldierIsAccepted() {
        assertTrue(ArmyAttackTargetPolicy.canAttackRecruit(
                factions(), REPUBLIC, SEPARATIST, false, RecruitDuty.SOLDIER), "enemy soldier");
    }

    private static void sameAllyAndNeutralRecruitsAreRejected() {
        assertFalse(ArmyAttackTargetPolicy.canAttackRecruit(
                factions(), REPUBLIC, REPUBLIC, false, RecruitDuty.SOLDIER), "same faction");
        assertFalse(ArmyAttackTargetPolicy.canAttackRecruit(
                factions(), REPUBLIC, MANDALORIAN, false, RecruitDuty.SOLDIER), "allied faction");
        assertFalse(ArmyAttackTargetPolicy.canAttackRecruit(
                factions(), REPUBLIC, NEUTRAL, false, RecruitDuty.SOLDIER), "neutral faction");
    }

    private static void workersAndSameOwnerRecruitsAreRejected() {
        assertFalse(ArmyAttackTargetPolicy.canAttackRecruit(
                factions(), REPUBLIC, SEPARATIST, false, RecruitDuty.WORKER), "enemy worker");
        assertFalse(ArmyAttackTargetPolicy.canAttackRecruit(
                factions(), REPUBLIC, SEPARATIST, true, RecruitDuty.SOLDIER), "same owner");
    }

    private static void explicitAndRetaliatoryMonstersAreAccepted() {
        assertTrue(ArmyAttackTargetPolicy.canAttackMonster(true, false), "explicit monster target");
        assertTrue(ArmyAttackTargetPolicy.canAttackMonster(false, true), "retaliatory monster target");
        assertFalse(ArmyAttackTargetPolicy.canAttackMonster(false, false), "unprovoked monster target");
    }

    private static FactionCatalog factions() {
        Map<FactionId, FactionDefinition> definitions = new LinkedHashMap<>();
        definitions.put(REPUBLIC, faction(REPUBLIC, Set.of(MANDALORIAN), Set.of(SEPARATIST)));
        definitions.put(MANDALORIAN, faction(MANDALORIAN, Set.of(REPUBLIC), Set.of(SEPARATIST)));
        definitions.put(SEPARATIST, faction(SEPARATIST, Set.of(), Set.of(REPUBLIC, MANDALORIAN)));
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
