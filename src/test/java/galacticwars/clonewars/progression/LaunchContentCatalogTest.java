package galacticwars.clonewars.progression;

public final class LaunchContentCatalogTest {
    public static void main(String[] args) {
        assertEquals(5, LaunchContentCatalog.UNITS.size(), "factions");
        assertEquals(15, LaunchContentCatalog.UNITS.values().stream().mapToInt(java.util.List::size).sum(), "units");
        assertEquals(4, LaunchContentCatalog.PLANETS.size(), "planets");
        assertEquals(5, LaunchContentCatalog.VEHICLES.size(), "vehicles");
        assertEquals(6, LaunchContentCatalog.FORCE_ABILITIES.size(), "Force abilities");
        assertEquals(15, LaunchContentCatalog.QUESTS.size(), "quests");
        System.out.println("LaunchContentCatalogTest passed");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) throw new AssertionError(label + " expected " + expected + " but was " + actual);
    }
}
