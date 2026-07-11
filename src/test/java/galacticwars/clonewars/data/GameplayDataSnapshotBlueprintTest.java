package galacticwars.clonewars.data;

import java.util.List;
import java.util.Map;
import galacticwars.clonewars.army.ArmyUnitCatalog;
import galacticwars.clonewars.faction.FactionCatalog;
import galacticwars.clonewars.settlement.KingdomBaseBlueprint;

public final class GameplayDataSnapshotBlueprintTest {
    private GameplayDataSnapshotBlueprintTest() {
    }

    public static void main(String[] args) {
        legacyKeysCanonicalizeAtTheSnapshotBoundary();
        aliasCollisionsAreRejected();
        optionalLookupsRejectBlankIdsWithoutThrowing();
        System.out.println("GameplayDataSnapshotBlueprintTest passed");
    }

    private static void legacyKeysCanonicalizeAtTheSnapshotBoundary() {
        KingdomBaseBlueprint blueprint = KingdomBaseBlueprint.barracks();
        GameplayDataSnapshot snapshot = snapshot(Map.of("barracks", blueprint));
        assertEquals(blueprint, snapshot.blueprint("barracks").orElseThrow(), "legacy lookup");
        assertEquals(blueprint, snapshot.blueprint(blueprint.id()).orElseThrow(), "canonical lookup");
        assertTrue(snapshot.blueprints().containsKey("galacticwars:barracks"), "canonical map key");
    }

    private static void aliasCollisionsAreRejected() {
        KingdomBaseBlueprint blueprint = KingdomBaseBlueprint.barracks();
        try {
            snapshot(Map.of(
                    "barracks", blueprint,
                    "galacticwars:barracks", blueprint));
            throw new AssertionError("alias collision was accepted");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void optionalLookupsRejectBlankIdsWithoutThrowing() {
        GameplayDataSnapshot snapshot = snapshot(Map.of());
        assertTrue(snapshot.unit("").isEmpty(), "blank unit lookup");
        assertTrue(snapshot.unit(null).isEmpty(), "null unit lookup");
        assertTrue(snapshot.faction(" ").isEmpty(), "blank faction lookup");
    }

    private static GameplayDataSnapshot snapshot(Map<String, KingdomBaseBlueprint> blueprints) {
        return new GameplayDataSnapshot(
                new FactionCatalog(Map.of()),
                new ArmyUnitCatalog(List.of()),
                Map.of(),
                Map.of(),
                blueprints);
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) {
            throw new AssertionError(label);
        }
    }
}
