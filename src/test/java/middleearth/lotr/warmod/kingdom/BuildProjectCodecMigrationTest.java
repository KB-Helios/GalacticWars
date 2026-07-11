package middleearth.lotr.warmod.kingdom;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class BuildProjectCodecMigrationTest {
    private BuildProjectCodecMigrationTest() {
    }

    public static void main(String[] args) throws IOException {
        legacyActiveProjectRemainsActive();
        legacyBlockedProjectRemainsBlocked();
        codecTreatsAuthorityFieldsAsMigrationAwareOptionals();
        System.out.println("BuildProjectCodecMigrationTest passed");
    }

    private static void legacyActiveProjectRemainsActive() {
        BuildProject project = decodeLegacy("");
        assertEquals("legacy", project.definitionHash(), "legacy definition hash");
        assertEquals(BuildProjectState.ACTIVE, project.state(), "legacy active state");
        assertEquals(1, project.completedPlacements().size(), "legacy placement progress");
    }

    private static void legacyBlockedProjectRemainsBlocked() {
        BuildProject project = decodeLegacy("missing_material");
        assertEquals(BuildProjectState.BLOCKED, project.state(), "legacy blocked state");
        assertEquals("missing_material", project.blockedReason(), "legacy blocked reason");
    }

    private static void codecTreatsAuthorityFieldsAsMigrationAwareOptionals() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/middleearth/lotr/warmod/kingdom/KingdomCodecs.java"));
        assertTrue(source.contains("optionalFieldOf(\"definition_hash\")"), "optional legacy definition hash");
        assertTrue(source.contains("optionalFieldOf(\"state\")"), "optional legacy state");
        assertTrue(source.contains("BuildProject::fromPersistence"), "migration decoder wiring");
    }

    private static BuildProject decodeLegacy(String blockedReason) {
        return BuildProject.fromPersistence(
                UUID.randomUUID(), "starter_keep", "minecraft:overworld",
                10, 64, 20, 0, Optional.empty(), List.of(0), Optional.empty(), blockedReason, 0);
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
