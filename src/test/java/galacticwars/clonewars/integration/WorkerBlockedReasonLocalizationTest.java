package galacticwars.clonewars.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Every {@code blockWorker(...)}/build project block reason surfaced to players through the
 * Command Center must resolve to a curated {@code reason.galacticwars.operations.*} translation
 * key instead of leaking a raw snake_case identifier. This scans the actual reason-code call
 * sites so a future reason added without a matching lang entry fails the build instead of
 * silently degrading to the humanized fallback.
 */
public final class WorkerBlockedReasonLocalizationTest {
    private static final Pattern BLOCK_WORKER_CALL = Pattern.compile("blockWorker\\(([^;]*?)\\);", Pattern.DOTALL);
    private static final Pattern BUILD_PROJECT_BLOCK_CALL =
            Pattern.compile("\\.block\\(([^;]*?)\\);", Pattern.DOTALL);
    private static final Pattern QUOTED_REASON = Pattern.compile("\"([a-z_]+)\"");

    private WorkerBlockedReasonLocalizationTest() {
    }

    public static void main(String[] args) throws IOException {
        String entity = read("src/main/java/galacticwars/clonewars/entity/GalacticRecruitEntity.java");
        String screen = read("src/main/java/galacticwars/clonewars/client/gui/CommandCenterOperationsScreen.java");
        String language = read("src/main/resources/assets/galacticwars/lang/en_us.json");

        Set<String> reasons = new LinkedHashSet<>();
        collectQuotedReasons(entity, BLOCK_WORKER_CALL, reasons);
        collectQuotedReasons(entity, BUILD_PROJECT_BLOCK_CALL, reasons);

        if (reasons.isEmpty()) {
            throw new AssertionError("expected at least one worker blocked-reason call site");
        }

        for (String reason : reasons) {
            String key = "\"reason.galacticwars.operations." + reason + "\"";
            if (!language.contains(key)) {
                throw new AssertionError(
                        "missing localized text for blocked reason \"" + reason + "\" (expected " + key + ")");
            }
        }

        assertContains(screen, "reasonText(worker.reasonCode())", "worker label uses localized reason text");
        assertContains(screen, "reasonText(build.blockedReason())", "build label uses localized reason text");
        assertContains(screen, "Language.getInstance().has(key)", "reasonText falls back gracefully for unknown reasons");

        System.out.println("WorkerBlockedReasonLocalizationTest passed (" + reasons.size() + " reasons checked)");
    }

    private static void collectQuotedReasons(String source, Pattern callPattern, Set<String> reasons) {
        Matcher callMatcher = callPattern.matcher(source);
        while (callMatcher.find()) {
            Matcher reasonMatcher = QUOTED_REASON.matcher(callMatcher.group(1));
            while (reasonMatcher.find()) {
                reasons.add(reasonMatcher.group(1));
            }
        }
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    private static void assertContains(String haystack, String needle, String label) {
        if (!haystack.contains(needle)) {
            throw new AssertionError(label + " missing <" + needle + ">");
        }
    }
}
