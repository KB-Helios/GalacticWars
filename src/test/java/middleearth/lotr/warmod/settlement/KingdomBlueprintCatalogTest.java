package middleearth.lotr.warmod.settlement;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class KingdomBlueprintCatalogTest {
    private static final Path BLUEPRINT_ROOT = Path.of(
            "src/main/resources/data/kingdomwarsmiddleearth/kingdomwars/blueprints");

    private KingdomBlueprintCatalogTest() {
    }

    public static void main(String[] args) throws IOException {
        List<KingdomBaseBlueprint> blueprints = KingdomBaseBlueprint.all();
        assertEquals(6, blueprints.size(), "initial blueprint count");
        assertTrue(blueprints == KingdomBaseBlueprint.all(), "static blueprint catalog is cached");
        Set<String> ids = new HashSet<>();
        for (KingdomBaseBlueprint blueprint : blueprints) {
            assertTrue(ids.add(blueprint.id()), "unique blueprint id " + blueprint.id());
            assertTrue(!blueprint.placements().isEmpty(), "placements for " + blueprint.id());
            assertEquals(blueprint.placements().size(), blueprint.requiredResources().totalCount(),
                    "one material per placement for " + blueprint.id());
            validateDataResource(blueprint);
        }
        assertTrue(KingdomBaseBlueprint.byId("house").isPresent(), "house lookup");
        assertTrue(KingdomBaseBlueprint.byId("unknown").isEmpty(), "unknown lookup");
        System.out.println("KingdomBlueprintCatalogTest passed");
    }

    private static void validateDataResource(KingdomBaseBlueprint blueprint) throws IOException {
        Path file = BLUEPRINT_ROOT.resolve(blueprint.id() + ".json");
        assertTrue(Files.isRegularFile(file), "data resource for " + blueprint.id());
        String json = Files.readString(file);
        assertContains(json, "\"schema_version\": 1", "schema version");
        assertContains(json, "\"id\": \"kingdomwarsmiddleearth:" + blueprint.id() + "\"", "resource id");
        assertContains(json, "\"anchor\"", "anchor");
        assertContains(json, "\"allowed_rotations\"", "rotation list");
        assertContains(json, "\"placements\"", "placements");
        assertContains(json, "\"rewards\"", "completion rewards");
        long placementCount = json.lines().filter(line -> line.trim().startsWith("\"block\":" )).count();
        assertEquals(blueprint.placements().size(), (int) placementCount,
                "resource placement count for " + blueprint.id());
    }

    private static void assertContains(String value, String fragment, String label) {
        if (!value.contains(fragment)) {
            throw new AssertionError(label + " missing <" + fragment + ">");
        }
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
