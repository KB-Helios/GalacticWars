package galacticwars.clonewars.ability;

public final class AbilityDefinitionTest {
    private AbilityDefinitionTest() {
    }

    public static void main(String[] args) {
        activeAbilityContract();
        passiveAbilityContract();
        forceAbilitiesStayDisabled();
        System.out.println("AbilityDefinitionTest passed");
    }

    private static void activeAbilityContract() {
        AbilityDefinition ability = new AbilityDefinition(
                AbilityId.of("suppressive_fire"),
                "Suppressive Fire",
                AbilityKind.MARTIAL,
                AbilityActivation.TARGET,
                100,
                15,
                24.0D,
                20,
                true);
        assertEquals("galacticwars:suppressive_fire", ability.id().toString(), "default namespace");
        assertTrue(ability.active(), "target ability is active");
        assertThrows(() -> AbilityId.of("minecraft:suppressive_fire"), "foreign namespace rejected");
        assertThrows(() -> AbilityId.of("galacticwars:suppressive-fire"), "non-snake-case path rejected");
        assertThrows(() -> AbilityId.of("galacticwars:combat/suppressive_fire"), "nested path rejected");
    }

    private static void passiveAbilityContract() {
        AbilityDefinition ability = new AbilityDefinition(
                AbilityId.of("galacticwars:formation_discipline"),
                "Formation Discipline",
                AbilityKind.SUPPORT,
                AbilityActivation.PASSIVE,
                0,
                0,
                8.0D,
                40,
                true);
        assertTrue(!ability.active(), "passive ability is not active");
        assertThrows(() -> new AbilityDefinition(
                ability.id(), ability.displayName(), ability.kind(), ability.activation(),
                20, 0, ability.range(), ability.aiEvaluationIntervalTicks(), true),
                "passive cooldown rejected");
    }

    private static void forceAbilitiesStayDisabled() {
        assertThrows(() -> new AbilityDefinition(
                AbilityId.of("future_force_push"),
                "Future Force Push",
                AbilityKind.FORCE,
                AbilityActivation.TARGET,
                60,
                20,
                12.0D,
                20,
                true), "enabled Force ability rejected");
    }

    private static void assertThrows(Runnable action, String label) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError(label);
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected " + expected + " but was " + actual);
        }
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) {
            throw new AssertionError(label);
        }
    }
}
