package galacticwars.clonewars.settlement;

import java.util.List;

public final class BlueprintAuthorityTest {
    private BlueprintAuthorityTest() {
    }

    public static void main(String[] args) {
        canonicalIdsRetainLegacyAliases();
        anchorAndAllowedRotationsControlOffsets();
        definitionHashIsStableAndComplete();
        invalidDefinitionsAreRejected();
        System.out.println("BlueprintAuthorityTest passed");
    }

    private static void canonicalIdsRetainLegacyAliases() {
        KingdomBaseBlueprint legacy = blueprint("barracks", BlueprintAnchor.ORIGIN, List.of(0));
        assertEquals("galacticwars:barracks", legacy.id(), "canonical legacy id");
        assertEquals(KingdomBaseBlueprint.byId("barracks").orElseThrow().id(),
                KingdomBaseBlueprint.byId("galacticwars:barracks").orElseThrow().id(),
                "legacy catalog alias");
    }

    private static void anchorAndAllowedRotationsControlOffsets() {
        KingdomBaseBlueprint blueprint = blueprint(
                "anchor_test",
                new BlueprintAnchor(1, 4, 1),
                List.of(90, 0));
        BaseBlockPlacement unrotated = blueprint.rotatedPlacement(0, 0);
        BaseBlockPlacement rotated = blueprint.rotatedPlacement(0, 1);
        assertPosition(1, 0, 0, unrotated, "anchor-relative unrotated offset");
        assertPosition(0, 0, 1, rotated, "anchor-relative quarter turn");
        assertThrows(() -> blueprint.rotatedPlacement(0, 2), "unsupported rotation");
    }

    private static void definitionHashIsStableAndComplete() {
        KingdomBaseBlueprint first = blueprint("hash_test", new BlueprintAnchor(1, 4, 1), List.of(90, 0));
        KingdomBaseBlueprint reordered = blueprint("hash_test", new BlueprintAnchor(1, 4, 1), List.of(0, 90));
        KingdomBaseBlueprint movedAnchor = blueprint("hash_test", new BlueprintAnchor(0, 4, 1), List.of(0, 90));
        KingdomBaseBlueprint fewerRotations = blueprint("hash_test", new BlueprintAnchor(1, 4, 1), List.of(0));
        KingdomBaseBlueprint renamed = new KingdomBaseBlueprint(
                first.id(),
                "Cosmetic rename",
                first.anchor(),
                first.allowedRotations(),
                first.placements(),
                first.housingReward(),
                first.storageSlotReward(),
                first.worksiteType(),
                first.worksiteCapacity(),
                first.commanderSlotReward());
        KingdomBaseBlueprint rewarded = new KingdomBaseBlueprint(
                first.id(), "Test", first.anchor(), first.allowedRotations(), first.placements(),
                1, 0, "", 0, 0);
        KingdomBaseBlueprint movedPlacement = new KingdomBaseBlueprint(
                first.id(), "Test", first.anchor(), first.allowedRotations(),
                List.of(new BaseBlockPlacement(3, 4, 1, "minecraft:stone", "minecraft:stone")),
                0, 0, "", 0, 0);
        assertEquals(64, first.definitionHash().length(), "SHA-256 hex length");
        assertEquals(first.definitionHash(), reordered.definitionHash(), "rotation set canonicalization");
        assertEquals(first.definitionHash(), renamed.definitionHash(), "display name is cosmetic");
        assertTrue(!first.definitionHash().equals(movedAnchor.definitionHash()), "anchor affects hash");
        assertTrue(!first.definitionHash().equals(fewerRotations.definitionHash()), "rotations affect hash");
        assertTrue(!first.definitionHash().equals(rewarded.definitionHash()), "rewards affect hash");
        assertTrue(!first.definitionHash().equals(movedPlacement.definitionHash()), "placements affect hash");
    }

    private static void invalidDefinitionsAreRejected() {
        assertThrows(() -> blueprint("bad:id:extra", BlueprintAnchor.ORIGIN, List.of(0)), "invalid id");
        assertThrows(() -> blueprint("rotation", BlueprintAnchor.ORIGIN, List.of(45)), "invalid rotation");
        assertThrows(() -> new KingdomBaseBlueprint(
                "duplicate",
                "Duplicate",
                BlueprintAnchor.ORIGIN,
                List.of(0),
                List.of(
                        new BaseBlockPlacement(0, 0, 0, "minecraft:stone", "minecraft:stone"),
                        new BaseBlockPlacement(0, 0, 0, "minecraft:dirt", "minecraft:dirt")),
                0, 0, "", 0, 0), "duplicate placement");
    }

    private static KingdomBaseBlueprint blueprint(String id, BlueprintAnchor anchor, List<Integer> rotations) {
        return new KingdomBaseBlueprint(
                id,
                "Test",
                anchor,
                rotations,
                List.of(new BaseBlockPlacement(2, 4, 1, "minecraft:stone", "minecraft:stone")),
                0,
                0,
                "",
                0,
                0);
    }

    private static void assertPosition(int x, int y, int z, BaseBlockPlacement placement, String label) {
        assertEquals(x, placement.x(), label + " x");
        assertEquals(y, placement.y(), label + " y");
        assertEquals(z, placement.z(), label + " z");
    }

    private static void assertThrows(Runnable action, String label) {
        try {
            action.run();
            throw new AssertionError(label + " expected an exception");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
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
