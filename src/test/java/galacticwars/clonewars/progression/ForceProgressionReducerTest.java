package galacticwars.clonewars.progression;

import galacticwars.clonewars.data.LaunchContentDefinitions;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ForceProgressionReducerTest {
    private static final List<Integer> THRESHOLDS = List.of(0, 10, 25, 45, 70, 100, 140, 190, 250, 320);

    public static void main(String[] args) {
        LaunchContentDefinitions content = content();
        ProgressionState republic = new ProgressionState(
                ProgressionState.CURRENT_SCHEMA_VERSION, UUID.randomUUID(),
                "galacticwars:republic", 0, Set.of(), Map.of(),
                Map.of(ProgressionEventType.QUEST_ADVANCED, Set.of("republic_chapter_1")), Set.of());
        ForceRuntimeState uninitiated = ForceRuntimeState.full();
        var initiated = ForceProgressionService.initiate(republic, uninitiated, "jedi", content);
        assertTrue(initiated.accepted(), "chapter-one Republic player can initiate Jedi");
        ForceRuntimeState state = initiated.state();
        assertEquals("jedi", state.traditionId(), "tradition");
        assertEquals(1, state.rank(), "initial rank");
        assertEquals(Set.of("sense", "push", "leap"), state.learnedNodeIds(), "core nodes");
        assertEquals(List.of("push", "leap"), state.equippedAbilityIds(), "core loadout");
        assertTrue(!ForceProgressionService.initiate(republic, state, "jedi", content).accepted(),
                "tradition cannot be switched");

        var mastery = ForceProgressionService.gainMastery(state, 320, content);
        assertTrue(mastery.accepted(), "mastery can advance");
        state = mastery.state();
        assertEquals(10, state.rank(), "rank ten threshold");
        assertEquals(9, state.unspentPoints(), "one point per post-initiation rank");
        assertTrue(!ForceProgressionService.learnNode(state, "guardian_5", content).accepted(),
                "capstone prerequisites are enforced");
        for (int tier = 1; tier <= 5; tier++) {
            var learned = ForceProgressionService.learnNode(state, "guardian_" + tier, content);
            assertTrue(learned.accepted(), "Guardian tier " + tier + " learns in sequence");
            state = learned.state();
        }
        for (int tier = 1; tier <= 4; tier++) {
            var learned = ForceProgressionService.learnNode(state, "consular_" + tier, content);
            assertTrue(learned.accepted(), "Consular tier " + tier + " learns in sequence");
            state = learned.state();
        }
        assertEquals(0, state.unspentPoints(), "nine branch points are exhausted");
        assertTrue(!ForceProgressionService.learnNode(state, "consular_5", content).accepted(),
                "both branches cannot be mastered");

        var equipped = ForceProgressionService.equip(state, 2, "push", content);
        assertTrue(equipped.accepted(), "learned active can be equipped");
        assertEquals("push", equipped.state().equippedAbilityIds().get(2), "exact equip slot");
        assertTrue(!ForceProgressionService.equip(state, 0, "foreign", content).accepted(),
                "unknown or foreign abilities fail closed");

        var respec = ForceProgressionService.respec(state, content);
        assertTrue(respec.accepted(), "branch respec is available");
        assertEquals(Set.of("sense", "push", "leap"), respec.state().learnedNodeIds(),
                "respec preserves only fundamentals");
        assertEquals(9, respec.state().unspentPoints(), "respec refunds all branch points");

        ForceRuntimeState farming = initiated.state();
        var first = ForceProgressionService.awardCombatMastery(farming, "target:push", 1_000L, content);
        assertTrue(first.accepted(), "real first effect grants combat mastery");
        var replay = ForceProgressionService.awardCombatMastery(first.state(), "target:push", 1_199L, content);
        assertTrue(!replay.accepted(), "same target/executor is throttled for 200 ticks");
        farming = first.state();
        for (int index = 1; index < 20; index++) {
            farming = ForceProgressionService.awardCombatMastery(
                    farming, "target-" + index + ":push", 1_000L, content).state();
        }
        assertEquals(20, farming.dailyCombatExperience(), "daily combat mastery cap reached");
        assertTrue(!ForceProgressionService.awardCombatMastery(
                farming, "target-overflow:push", 2_000L, content).accepted(),
                "twenty-first daily combat award is rejected");

        ForceRuntimeState legacyLight = new ForceRuntimeState("light", 80, Map.of("push", 20L), Set.of());
        ForceRuntimeState legacyDark = new ForceRuntimeState("dark", 75, Map.of(), Set.of());
        assertEquals("jedi", legacyLight.traditionId(), "v1 light migration");
        assertEquals("nightsister", legacyDark.traditionId(), "v1 dark migration");
        assertEquals(Map.of(), legacyLight.cooldownEnds(), "obsolete Jedi cooldown key migration");
        assertEquals(Map.of(), legacyDark.cooldownEnds(), "obsolete Nightsister cooldown migration");
        assertEquals(List.of("light_push", "light_pull", "light_leap"),
                legacyLight.equippedAbilityIds(), "legacy Jedi loadout migration");
        System.out.println("ForceProgressionReducerTest passed");
    }

    private static LaunchContentDefinitions content() {
        Map<String, LaunchContentDefinitions.ForceNodeDefinition> nodes = new LinkedHashMap<>();
        nodes.put("sense", node("sense", "core", 0, Set.of(), "", true));
        nodes.put("push", node("push", "core", 0, Set.of(), "push", false));
        nodes.put("leap", node("leap", "core", 0, Set.of(), "leap", false));
        for (String branch : List.of("guardian", "consular")) {
            for (int tier = 1; tier <= 5; tier++) {
                String id = branch + "_" + tier;
                nodes.put(id, node(id, branch, tier,
                        tier == 1 ? Set.of() : Set.of(branch + "_" + (tier - 1)), id, false));
            }
        }
        Map<String, LaunchContentDefinitions.ForceAbilityDefinition> abilities = new LinkedHashMap<>();
        for (var node : nodes.values()) {
            if (node.abilityId().isBlank()) continue;
            abilities.put(node.abilityId(), new LaunchContentDefinitions.ForceAbilityDefinition(
                    node.abilityId(), "jedi", 10, 20, "republic_chapter_1", true,
                    "push", 8.0D, 10, "force_path", node.id(), "instant", "ray",
                    "push", 0, 0, 0, 1));
        }
        var tradition = new LaunchContentDefinitions.ForceTraditionDefinition(
                "jedi", "republic", "republic_chapter_1", "Jedi",
                List.of("sense", "push", "leap"), List.of("guardian", "consular"), THRESHOLDS);
        return new LaunchContentDefinitions(Map.of(), Map.of(), abilities,
                Map.of("jedi", tradition), nodes, Map.of(), Map.of(), Map.of());
    }

    private static LaunchContentDefinitions.ForceNodeDefinition node(
            String id, String branch, int tier, Set<String> prerequisites,
            String ability, boolean passive
    ) {
        return new LaunchContentDefinitions.ForceNodeDefinition(
                id, "jedi", branch, tier, branch.equals("core") ? 0 : 1,
                prerequisites, ability, passive);
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected " + expected + " but was " + actual);
        }
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }
}
