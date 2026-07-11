package galacticwars.clonewars.world;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import galacticwars.clonewars.faction.FactionId;

public final class GalacticRegionCatalogTest {
    private GalacticRegionCatalogTest() {
    }

    public static void main(String[] args) {
        normalizesRegionIds();
        storesRegionDefinitionValues();
        looksUpRegionsByIdFactionAndClimate();
        rejectsDuplicateRegionIds();
        rejectsInvalidCatalogMapEntries();
        rejectsInvalidRegionValues();

        System.out.println("GalacticRegionCatalogTest passed");
    }

    private static void normalizesRegionIds() {
        assertEquals("galacticwars:republic", GalacticRegionId.of("Republic").toString(),
                "default namespace region id");
        assertEquals("galacticwars:separatist", GalacticRegionId.of("galacticwars:Separatist").toString(),
                "explicit namespace region id");
    }

    private static void storesRegionDefinitionValues() {
        GalacticRegionDefinition republic = republic();

        assertEquals(GalacticRegionId.of("republic"), republic.id(), "region id");
        assertEquals("Republic", republic.displayName(), "region display name");
        assertEquals(FactionId.of("republic"), republic.controllingFaction(), "region controlling faction");
        assertEquals(GalacticRegionClimate.TEMPERATE, republic.climate(), "region climate");
        assertEquals(0.8F, republic.baseTemperature(), "region base temperature");
        assertEquals(0.4F, republic.downfall(), "region downfall");
        assertEquals(10, republic.spawnWeight(), "region spawn weight");
        assertTrue(republic.features().contains("white_city_outskirts"), "region feature");
    }

    private static void looksUpRegionsByIdFactionAndClimate() {
        GalacticRegionCatalog catalog = testCatalog();

        Optional<GalacticRegionDefinition> republic = catalog.definition(GalacticRegionId.of("republic"));
        assertTrue(republic.isPresent(), "republic lookup");
        assertEquals("Republic", republic.orElseThrow().displayName(), "republic lookup name");

        List<GalacticRegionDefinition> mandalorianRegions = catalog.regionsForFaction(FactionId.of("mandalorian"));
        assertEquals(1, mandalorianRegions.size(), "mandalorian region count");
        assertEquals(GalacticRegionId.of("mandalorian"), mandalorianRegions.get(0).id(), "mandalorian region id");

        List<GalacticRegionDefinition> shadowRegions =
                catalog.regionsForClimate(GalacticRegionClimate.SHADOW);
        assertEquals(1, shadowRegions.size(), "shadow region count");
        assertEquals(GalacticRegionId.of("separatist"), shadowRegions.get(0).id(), "shadow region id");
    }

    private static void rejectsDuplicateRegionIds() {
        assertThrows(IllegalArgumentException.class, () -> new GalacticRegionCatalog(List.of(republic(), republic())),
                "duplicate region ids");
    }

    private static void rejectsInvalidCatalogMapEntries() {
        assertThrows(IllegalArgumentException.class, () -> new GalacticRegionCatalog(Map.of(
                GalacticRegionId.of("mandalorian"), republic())), "mismatched region catalog map entry");

        LinkedHashMap<GalacticRegionId, GalacticRegionDefinition> nullKey = new LinkedHashMap<>();
        nullKey.put(null, republic());
        assertThrows(NullPointerException.class, () -> new GalacticRegionCatalog(nullKey),
                "null region catalog key");

        LinkedHashMap<GalacticRegionId, GalacticRegionDefinition> nullValue = new LinkedHashMap<>();
        nullValue.put(GalacticRegionId.of("republic"), null);
        assertThrows(NullPointerException.class, () -> new GalacticRegionCatalog(nullValue),
                "null region catalog value");
    }

    private static void rejectsInvalidRegionValues() {
        assertThrows(IllegalArgumentException.class, () -> new GalacticRegionDefinition(
                GalacticRegionId.of("ithilien"),
                "Ithilien",
                FactionId.of("republic"),
                GalacticRegionClimate.WOODLAND,
                -0.1F,
                0.6F,
                4,
                Set.of("crossroads")), "negative temperature");
        assertThrows(IllegalArgumentException.class, () -> new GalacticRegionDefinition(
                GalacticRegionId.of("dead_marshes"),
                "Dead Marshes",
                FactionId.of("separatist"),
                GalacticRegionClimate.SHADOW,
                0.5F,
                0.9F,
                -1,
                Set.of("marsh_lights")), "negative spawn weight");
        assertThrows(IllegalArgumentException.class, () -> new GalacticRegionDefinition(
                GalacticRegionId.of("fangorn"),
                "Fangorn",
                FactionId.of("mandalorian"),
                GalacticRegionClimate.WOODLAND,
                0.7F,
                0.8F,
                3,
                Set.of(" ")), "blank feature name");
    }

    private static GalacticRegionCatalog testCatalog() {
        return new GalacticRegionCatalog(List.of(
                republic(),
                new GalacticRegionDefinition(
                        GalacticRegionId.of("mandalorian"),
                        "Mandalorian",
                        FactionId.of("mandalorian"),
                        GalacticRegionClimate.PLAINS,
                        0.7F,
                        0.3F,
                        8,
                        Set.of("horse_lords_plains", "thatched_villages")),
                new GalacticRegionDefinition(
                        GalacticRegionId.of("separatist"),
                        "Separatist",
                        FactionId.of("separatist"),
                        GalacticRegionClimate.SHADOW,
                        1.0F,
                        0.0F,
                        6,
                        Set.of("ash_wastes", "black_gate"))));
    }

    private static GalacticRegionDefinition republic() {
        return new GalacticRegionDefinition(
                GalacticRegionId.of("republic"),
                "Republic",
                FactionId.of("republic"),
                GalacticRegionClimate.TEMPERATE,
                0.8F,
                0.4F,
                10,
                Set.of("white_city_outskirts", "beacon_hills"));
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertEquals(float expected, float actual, String label) {
        if (Float.compare(expected, actual) != 0) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertTrue(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label + " expected to be true");
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
