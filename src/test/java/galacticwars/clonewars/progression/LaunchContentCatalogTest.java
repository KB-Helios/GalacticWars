package galacticwars.clonewars.progression;

public final class LaunchContentCatalogTest {
    public static void main(String[] args) {
        assertEquals(5, LaunchContentCatalog.UNITS.size(), "factions");
        assertEquals(15, LaunchContentCatalog.UNITS.values().stream().mapToInt(java.util.List::size).sum(), "units");
        assertEquals(4, LaunchContentCatalog.PLANETS.size(), "planets");
        assertEquals(5, LaunchContentCatalog.VEHICLES.size(), "vehicles");
        assertEquals(6, LaunchContentCatalog.FORCE_ABILITIES.size(), "Force abilities");
        assertEquals(15, LaunchContentCatalog.QUESTS.size(), "quests");
        assertEquals(15, LaunchContentCatalog.QUEST_UNLOCKS.size(), "quest unlock definitions");
        assertEquals(java.util.Set.of("barc_speeder", "force_path"),
                LaunchContentCatalog.questUnlocks("republic_chapter_2"), "Republic chapter 2 unlocks");
        assertEquals(java.util.Set.of("vehicle_crafting"),
                LaunchContentCatalog.questUnlocks("mandalorian_chapter_2"), "Mandalorian chapter 2 unlocks");
        assertEquals(java.util.Set.of("vehicle_crafting"),
                LaunchContentCatalog.questUnlocks("hutt_cartel_chapter_2"), "Hutt chapter 2 unlocks");
        System.out.println("LaunchContentCatalogTest passed");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) throw new AssertionError(label + " expected " + expected + " but was " + actual);
    }
}
