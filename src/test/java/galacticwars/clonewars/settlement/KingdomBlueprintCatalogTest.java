package galacticwars.clonewars.settlement;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class KingdomBlueprintCatalogTest {
    private static final Path BLUEPRINT_ROOT = Path.of(
            "src/main/resources/data/galacticwars/galacticwars/blueprints");

    private KingdomBlueprintCatalogTest() {
    }

    public static void main(String[] args) throws IOException {
        List<KingdomBaseBlueprint> blueprints = KingdomBaseBlueprint.all();
        assertEquals(7, blueprints.size(), "initial blueprint count");
        assertTrue(blueprints == KingdomBaseBlueprint.all(), "static blueprint catalog is cached");
        Set<String> ids = new HashSet<>();
        for (KingdomBaseBlueprint blueprint : blueprints) {
            assertTrue(ids.add(blueprint.id()), "unique blueprint id " + blueprint.id());
            assertTrue(!blueprint.placements().isEmpty(), "placements for " + blueprint.id());
            assertEquals(blueprint.placements().size(), blueprint.requiredResources().totalCount(),
                    "one material per placement for " + blueprint.id());
            validateDataResource(blueprint);
        }
        assertTrue(KingdomBaseBlueprint.byId("barracks").isPresent(), "barracks lookup");
        assertTrue(KingdomBaseBlueprint.byId("galacticwars:barracks").isPresent(),
                "canonical barracks lookup");
        assertEquals("galacticwars:forward_base", KingdomBaseBlueprint.STARTER_KEEP_ID,
                "shared forward base id");
        assertEquals("galacticwars:starter_camp", KingdomBaseBlueprint.STARTER_CAMP_ID,
                "dedicated onboarding camp id");
        assertTrue(KingdomBaseBlueprint.byId("unknown").isEmpty(), "unknown lookup");
        loaderConsumesAuthorityFields();
        System.out.println("KingdomBlueprintCatalogTest passed");
    }

    private static void loaderConsumesAuthorityFields() throws IOException {
        String manager = Files.readString(Path.of(
                "src/main/java/galacticwars/clonewars/data/GameplayDataManager.java"));
        assertContains(manager, "requiredObject(json, \"anchor\"", "loader anchor parsing");
        assertContains(manager, "requiredArray(json, \"allowed_rotations\"", "loader rotation parsing");
        assertContains(manager, "declares mismatched id", "resource id authority");
    }

    private static void validateDataResource(KingdomBaseBlueprint blueprint) throws IOException {
        Path file = BLUEPRINT_ROOT.resolve(KingdomBaseBlueprint.path(blueprint.id()) + ".json");
        assertTrue(Files.isRegularFile(file), "data resource for " + blueprint.id());
        String json = Files.readString(file);
        var descriptor = JsonParser.parseString(json).getAsJsonObject();
        assertEquals(2, descriptor.get("schema_version").getAsInt(), "schema version");
        assertEquals(blueprint.id(), descriptor.get("id").getAsString(), "resource id");
        assertContains(json, "\"anchor\"", "anchor");
        assertContains(json, "\"allowed_rotations\"", "rotation list");
        assertContains(json, "\"template\"", "NBT template reference");
        assertContains(json, "\"construction\"", "construction metadata");
        assertContains(json, "\"rewards\"", "completion rewards");
        String template = descriptor.get("template").getAsString();
        assertTrue(Files.isRegularFile(Path.of("src/main/resources/data/galacticwars/structure")
                        .resolve(template.substring(template.indexOf(':') + 1) + ".nbt")),
                "template file for " + blueprint.id());
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
