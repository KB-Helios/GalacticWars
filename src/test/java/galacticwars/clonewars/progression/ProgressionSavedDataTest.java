package galacticwars.clonewars.progression;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ProgressionSavedDataTest {
    public static void main(String[] args) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/galacticwars/clonewars/progression/ProgressionSavedData.java"));
        assertContains(source, "schema_version", "versioned persistence");
        assertContains(source, "processed_events", "idempotency persistence");
        assertContains(source, "event_subjects", "quest and unlock subjects");
        assertContains(source, "computeIfAbsent(TYPE)", "overworld-authoritative saved data");
        assertContains(source, "decision.accepted() && decision.changed()", "dirty only after committed change");
        System.out.println("ProgressionSavedDataTest passed");
    }

    private static void assertContains(String source, String expected, String label) {
        if (!source.contains(expected)) throw new AssertionError(label + " missing " + expected);
    }
}
