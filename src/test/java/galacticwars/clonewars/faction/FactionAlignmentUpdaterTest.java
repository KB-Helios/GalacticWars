package galacticwars.clonewars.faction;

import java.util.LinkedHashMap;
import java.util.Set;
import java.util.UUID;

public final class FactionAlignmentUpdaterTest {
    private FactionAlignmentUpdaterTest() {
    }

    public static void main(String[] args) {
        helpingFactionPropagatesToAlliesAndEnemies();
        harmingFactionPropagatesToAlliesAndEnemies();
        ignoresUnregisteredAlliesAndEnemies();
        clampsAlignmentScores();
        rejectsInvalidRules();

        System.out.println("FactionAlignmentUpdaterTest passed");
    }

    private static void helpingFactionPropagatesToAlliesAndEnemies() {
        FactionAlignment alignment = FactionAlignment.empty(playerId()).withAddedScore(FactionId.of("republic"), 5);
        FactionAlignmentRule rule = new FactionAlignmentRule(10, 4, -6, "aid_faction");

        FactionAlignmentUpdateResult result =
                FactionAlignmentUpdater.apply(alignment, testCatalog(), FactionId.of("republic"), rule);

        assertEquals(15, result.alignment().score(FactionId.of("republic")), "republic direct gain");
        assertEquals(4, result.alignment().score(FactionId.of("mandalorian")), "mandalorian allied gain");
        assertEquals(-6, result.alignment().score(FactionId.of("separatist")), "separatist enemy loss");
        assertEquals(0, result.alignment().score(FactionId.of("shire")), "neutral shire unchanged");

        assertEquals(3, result.changes().size(), "helping change count");
        assertChange(result.changes().get(0), "republic", 5, 10, 15, "aid_faction");
        assertChange(result.changes().get(1), "mandalorian", 0, 4, 4, "aid_faction");
        assertChange(result.changes().get(2), "separatist", 0, -6, -6, "aid_faction");
    }

    private static void harmingFactionPropagatesToAlliesAndEnemies() {
        FactionAlignment alignment = FactionAlignment.empty(playerId())
                .withAddedScore(FactionId.of("republic"), 20)
                .withAddedScore(FactionId.of("mandalorian"), 8)
                .withAddedScore(FactionId.of("separatist"), -3);
        FactionAlignmentRule rule = new FactionAlignmentRule(-12, -5, 7, "harm_faction");

        FactionAlignmentUpdateResult result =
                FactionAlignmentUpdater.apply(alignment, testCatalog(), FactionId.of("republic"), rule);

        assertEquals(8, result.alignment().score(FactionId.of("republic")), "republic direct loss");
        assertEquals(3, result.alignment().score(FactionId.of("mandalorian")), "mandalorian allied loss");
        assertEquals(4, result.alignment().score(FactionId.of("separatist")), "separatist enemy gain");
        assertEquals(0, result.alignment().score(FactionId.of("shire")), "neutral shire unchanged after harm");

        assertEquals(3, result.changes().size(), "harming change count");
        assertChange(result.changes().get(0), "republic", 20, -12, 8, "harm_faction");
        assertChange(result.changes().get(1), "mandalorian", 8, -5, 3, "harm_faction");
        assertChange(result.changes().get(2), "separatist", -3, 7, 4, "harm_faction");
    }

    private static void ignoresUnregisteredAlliesAndEnemies() {
        FactionDefinition republic = new FactionDefinition(
                FactionId.of("republic"),
                "Republic",
                25,
                10,
                12,
                Set.of(FactionId.of("missing_ally")),
                Set.of(FactionId.of("missing_enemy")));
        LinkedHashMap<FactionId, FactionDefinition> definitions = new LinkedHashMap<>();
        definitions.put(republic.id(), republic);
        FactionCatalog catalog = new FactionCatalog(definitions);

        FactionAlignmentUpdateResult result = FactionAlignmentUpdater.apply(
                FactionAlignment.empty(playerId()),
                catalog,
                republic.id(),
                new FactionAlignmentRule(3, 2, -2, "isolated_event"));

        assertEquals(3, result.alignment().score(FactionId.of("republic")), "isolated source changed");
        assertEquals(0, result.alignment().score(FactionId.of("missing_ally")), "missing ally unchanged");
        assertEquals(0, result.alignment().score(FactionId.of("missing_enemy")), "missing enemy unchanged");
        assertEquals(1, result.changes().size(), "isolated change count");
        assertChange(result.changes().get(0), "republic", 0, 3, 3, "isolated_event");
    }

    private static void rejectsInvalidRules() {
        assertThrows(IllegalArgumentException.class, () -> new FactionAlignmentRule(0, 0, 0, "no_effect"),
                "zero-effect rule");
        assertThrows(IllegalArgumentException.class, () -> new FactionAlignmentRule(1, 0, 0, " "),
                "blank rule reason");
        assertThrows(IllegalArgumentException.class, () -> new FactionAlignmentChange(
                FactionId.of("republic"), 1, 0, 1, "no_change"), "zero change delta");
        assertThrows(IllegalArgumentException.class, () -> new FactionAlignmentChange(
                FactionId.of("republic"), 1, 2, 4, "bad_math"), "inconsistent change math");
    }

    private static void clampsAlignmentScores() {
        FactionAlignment alignment = FactionAlignment.empty(playerId())
                .withAddedScore(FactionId.of("republic"), 500)
                .withAddedScore(FactionId.of("separatist"), -500);
        assertEquals(100, alignment.score(FactionId.of("republic")), "positive clamp");
        assertEquals(-100, alignment.score(FactionId.of("separatist")), "negative clamp");
    }

    private static FactionCatalog testCatalog() {
        FactionDefinition republic = new FactionDefinition(
                FactionId.of("republic"),
                "Republic",
                25,
                10,
                12,
                Set.of(FactionId.of("mandalorian")),
                Set.of(FactionId.of("separatist")));
        FactionDefinition mandalorian = new FactionDefinition(
                FactionId.of("mandalorian"),
                "Mandalorian",
                20,
                8,
                10,
                Set.of(FactionId.of("republic")),
                Set.of(FactionId.of("separatist")));
        FactionDefinition separatist = new FactionDefinition(
                FactionId.of("separatist"),
                "Separatist",
                30,
                15,
                16,
                Set.of(),
                Set.of(FactionId.of("republic"), FactionId.of("mandalorian")));
        FactionDefinition shire = new FactionDefinition(
                FactionId.of("shire"),
                "The Shire",
                10,
                4,
                4,
                Set.of(),
                Set.of());

        LinkedHashMap<FactionId, FactionDefinition> definitions = new LinkedHashMap<>();
        definitions.put(republic.id(), republic);
        definitions.put(mandalorian.id(), mandalorian);
        definitions.put(separatist.id(), separatist);
        definitions.put(shire.id(), shire);
        return new FactionCatalog(definitions);
    }

    private static UUID playerId() {
        return UUID.fromString("00000000-0000-0000-0000-000000000201");
    }

    private static void assertChange(
            FactionAlignmentChange change,
            String factionPath,
            int beforeScore,
            int delta,
            int afterScore,
            String reasonCode
    ) {
        assertEquals(FactionId.of(factionPath), change.factionId(), factionPath + " change faction");
        assertEquals(beforeScore, change.beforeScore(), factionPath + " before score");
        assertEquals(delta, change.delta(), factionPath + " delta");
        assertEquals(afterScore, change.afterScore(), factionPath + " after score");
        assertEquals(reasonCode, change.reasonCode(), factionPath + " reason");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertEquals(int expected, int actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static <T extends Throwable> void assertThrows(Class<T> expectedType, ThrowingRunnable runnable, String label) {
        try {
            runnable.run();
        } catch (Throwable throwable) {
            if (expectedType.isInstance(throwable)) {
                return;
            }
            throw new AssertionError(label + " threw " + throwable.getClass().getName() + " instead of "
                    + expectedType.getName(), throwable);
        }

        throw new AssertionError(label + " did not throw " + expectedType.getName());
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run();
    }
}
