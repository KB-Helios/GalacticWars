package galacticwars.clonewars.army;

import java.util.List;
import java.util.Optional;

import galacticwars.clonewars.faction.FactionId;

public final class ArmyUnitCatalogTest {
    private ArmyUnitCatalogTest() {
    }

    public static void main(String[] args) {
        normalizesUnitIds();
        storesUnitDefinitionValues();
        looksUpUnitsByIdFactionAndRole();
        rejectsDuplicateUnitIds();

        System.out.println("ArmyUnitCatalogTest passed");
    }

    private static void normalizesUnitIds() {
        assertEquals("galacticwars:clone_trooper", ArmyUnitId.of("Clone_Trooper").toString(),
                "default namespace unit id");
        assertEquals("galacticwars:mandalorian_rider", ArmyUnitId.of("galacticwars:Mandalorian_Rider").toString(),
                "explicit namespace unit id");
    }

    private static void storesUnitDefinitionValues() {
        ArmyUnitDefinition republicSoldier = republicSoldier();

        assertEquals(ArmyUnitId.of("clone_trooper"), republicSoldier.id(), "unit id");
        assertEquals("Clone Trooper", republicSoldier.displayName(), "unit display name");
        assertEquals(FactionId.of("republic"), republicSoldier.factionId(), "unit faction");
        assertEquals(ArmyUnitRole.INFANTRY, republicSoldier.role(), "unit role");
        assertEquals(25, republicSoldier.hireCost(), "unit hire cost");
        assertEquals(24, republicSoldier.maxHealth(), "unit max health");
        assertEquals(5, republicSoldier.attackDamage(), "unit attack damage");
        assertEquals(ArmyFormation.LINE, republicSoldier.defaultFormation(), "unit default formation");
    }

    private static void looksUpUnitsByIdFactionAndRole() {
        ArmyUnitCatalog catalog = testCatalog();

        Optional<ArmyUnitDefinition> republicSoldier = catalog.definition(ArmyUnitId.of("clone_trooper"));
        assertTrue(republicSoldier.isPresent(), "republic soldier lookup");
        assertEquals("Clone Trooper", republicSoldier.orElseThrow().displayName(), "republic soldier lookup name");

        List<ArmyUnitDefinition> republicUnits = catalog.unitsForFaction(FactionId.of("republic"));
        assertEquals(1, republicUnits.size(), "republic unit count");
        assertEquals(ArmyUnitId.of("clone_trooper"), republicUnits.get(0).id(), "republic unit id");

        List<ArmyUnitDefinition> cavalryUnits = catalog.unitsForRole(ArmyUnitRole.CAVALRY);
        assertEquals(1, cavalryUnits.size(), "cavalry unit count");
        assertEquals(ArmyUnitId.of("mandalorian_rider"), cavalryUnits.get(0).id(), "cavalry unit id");
    }

    private static void rejectsDuplicateUnitIds() {
        assertThrows(IllegalArgumentException.class, () -> new ArmyUnitCatalog(List.of(republicSoldier(), republicSoldier())),
                "duplicate unit ids");
    }

    private static ArmyUnitCatalog testCatalog() {
        return new ArmyUnitCatalog(List.of(
                republicSoldier(),
                new ArmyUnitDefinition(
                        ArmyUnitId.of("mandalorian_rider"),
                        "Mandalorian Rider",
                        FactionId.of("mandalorian"),
                        ArmyUnitRole.CAVALRY,
                        35,
                        26,
                        6,
                        ArmyFormation.WEDGE),
                new ArmyUnitDefinition(
                        ArmyUnitId.of("b1_battle_droid"),
                        "B1 Battle Droid",
                        FactionId.of("separatist"),
                        ArmyUnitRole.BRUTE,
                        20,
                        22,
                        5,
                        ArmyFormation.COLUMN)));
    }

    private static ArmyUnitDefinition republicSoldier() {
        return new ArmyUnitDefinition(
                ArmyUnitId.of("clone_trooper"),
                "Clone Trooper",
                FactionId.of("republic"),
                ArmyUnitRole.INFANTRY,
                25,
                24,
                5,
                ArmyFormation.LINE);
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
