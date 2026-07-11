package galacticwars.clonewars.settlement;

import java.util.Set;

import galacticwars.clonewars.workforce.ResourceInventory;

public final class KingdomBasePlannerTest {
    private KingdomBasePlannerTest() {
    }

    public static void main(String[] args) {
        starterBaseRequiresGalacticMaterials();
        plannerRequestsMissingSuppliesBeforeBuilding();
        plannerPlacesNextBlockWhenSuppliesAreAvailable();
        plannerSkipsAlreadyCompletedPlacements();

        System.out.println("KingdomBasePlannerTest passed");
    }

    private static void starterBaseRequiresGalacticMaterials() {
        KingdomBaseBlueprint blueprint = KingdomBaseBlueprint.starterKeep();
        ResourceInventory requirements = blueprint.requiredResources();

        assertTrue(requirements.count("galacticwars:duracrete") >= 9,
                "starter base requires duracrete");
        assertTrue(requirements.count("galacticwars:nightsister_weave_log") >= 4,
                "starter base requires nightsister_weave logs");
    }

    private static void plannerRequestsMissingSuppliesBeforeBuilding() {
        KingdomBaseBuildDecision decision = KingdomBaseBuildPlanner.planNext(
                KingdomBaseBlueprint.starterKeep(),
                ResourceInventory.empty(),
                Set.of());

        assertEquals(KingdomBaseBuildAction.GATHER_SUPPLIES, decision.action(), "action");
        assertEquals("galacticwars:duracrete", decision.itemId(), "item id");
        assertTrue(decision.quantity() > 0, "missing quantity");
        assertEquals("missing_supplies", decision.reasonCode(), "reason");
    }

    private static void plannerPlacesNextBlockWhenSuppliesAreAvailable() {
        KingdomBaseBlueprint blueprint = KingdomBaseBlueprint.starterKeep();
        KingdomBaseBuildDecision decision = KingdomBaseBuildPlanner.planNext(
                blueprint,
                blueprint.requiredResources(),
                Set.of());

        assertEquals(KingdomBaseBuildAction.PLACE_BLOCK, decision.action(), "action");
        assertEquals("galacticwars:duracrete", decision.itemId(), "item id");
        assertEquals(1, decision.quantity(), "quantity");
        assertEquals("ready_to_build", decision.reasonCode(), "reason");
        assertEquals(0, decision.placement().x(), "placement x");
        assertEquals(0, decision.placement().y(), "placement y");
        assertEquals(0, decision.placement().z(), "placement z");
    }

    private static void plannerSkipsAlreadyCompletedPlacements() {
        KingdomBaseBlueprint blueprint = KingdomBaseBlueprint.starterKeep();
        KingdomBaseBuildDecision decision = KingdomBaseBuildPlanner.planNext(
                blueprint,
                blueprint.requiredResources(),
                1);

        assertEquals(KingdomBaseBuildAction.PLACE_BLOCK, decision.action(), "action");
        assertEquals(0, decision.placement().x(), "placement x");
        assertEquals(0, decision.placement().y(), "placement y");
        assertEquals(1, decision.placement().z(), "placement z");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertTrue(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label + " expected to be true");
        }
    }
}
