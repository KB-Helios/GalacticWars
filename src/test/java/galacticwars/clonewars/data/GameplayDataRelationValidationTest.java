package galacticwars.clonewars.data;

import java.util.Set;
import galacticwars.clonewars.faction.FactionDefinition;
import galacticwars.clonewars.faction.FactionId;

public final class GameplayDataRelationValidationTest {
    private GameplayDataRelationValidationTest() {
    }

    public static void main(String[] args) {
        contradictoryRelationsAreRejectedWithResourceContext();
        disjointRelationsAreAccepted();
        System.out.println("GameplayDataRelationValidationTest passed");
    }

    private static void contradictoryRelationsAreRejectedWithResourceContext() {
        String resourceId = "galacticwars:galacticwars/factions/republic";
        FactionId republic = FactionId.of("galacticwars:republic");
        FactionId mandalorian = FactionId.of("galacticwars:mandalorian");
        try {
            FactionDefinition.validateRelationSets(resourceId, republic, Set.of(mandalorian), Set.of(mandalorian));
            throw new AssertionError("contradictory faction relation was accepted");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains(resourceId), "resource id in validation error");
            assertTrue(expected.getMessage().contains(mandalorian.toString()), "related faction in validation error");
        }
    }

    private static void disjointRelationsAreAccepted() {
        FactionDefinition.validateRelationSets(
                "galacticwars:galacticwars/factions/republic",
                FactionId.of("galacticwars:republic"),
                Set.of(FactionId.of("galacticwars:mandalorian")),
                Set.of(FactionId.of("galacticwars:separatist")));
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) {
            throw new AssertionError(label);
        }
    }
}
