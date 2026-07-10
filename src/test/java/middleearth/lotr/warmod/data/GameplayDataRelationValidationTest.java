package middleearth.lotr.warmod.data;

import java.util.Set;
import middleearth.lotr.warmod.faction.FactionDefinition;
import middleearth.lotr.warmod.faction.FactionId;

public final class GameplayDataRelationValidationTest {
    private GameplayDataRelationValidationTest() {
    }

    public static void main(String[] args) {
        contradictoryRelationsAreRejectedWithResourceContext();
        disjointRelationsAreAccepted();
        System.out.println("GameplayDataRelationValidationTest passed");
    }

    private static void contradictoryRelationsAreRejectedWithResourceContext() {
        String resourceId = "kingdomwarsmiddleearth:kingdomwars/factions/gondor";
        FactionId gondor = FactionId.of("kingdomwarsmiddleearth:gondor");
        FactionId rohan = FactionId.of("kingdomwarsmiddleearth:rohan");
        try {
            FactionDefinition.validateRelationSets(resourceId, gondor, Set.of(rohan), Set.of(rohan));
            throw new AssertionError("contradictory faction relation was accepted");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains(resourceId), "resource id in validation error");
            assertTrue(expected.getMessage().contains(rohan.toString()), "related faction in validation error");
        }
    }

    private static void disjointRelationsAreAccepted() {
        FactionDefinition.validateRelationSets(
                "kingdomwarsmiddleearth:kingdomwars/factions/gondor",
                FactionId.of("kingdomwarsmiddleearth:gondor"),
                Set.of(FactionId.of("kingdomwarsmiddleearth:rohan")),
                Set.of(FactionId.of("kingdomwarsmiddleearth:mordor")));
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) {
            throw new AssertionError(label);
        }
    }
}
