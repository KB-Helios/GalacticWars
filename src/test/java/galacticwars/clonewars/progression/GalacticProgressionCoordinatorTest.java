package galacticwars.clonewars.progression;

import galacticwars.clonewars.data.LaunchContentDefinitions;
import galacticwars.clonewars.data.LaunchContentRuntime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class GalacticProgressionCoordinatorTest {
    public static void main(String[] args) {
        installContent();
        UUID player = UUID.randomUUID();
        ProgressionState state = ProgressionState.create(player);
        state = accepted(state, event(player, ProgressionEventType.FACTION_PLEDGED, "galacticwars:republic", 1));
        ProgressionDecision repeatedPledge = GalacticProgressionCoordinator.apply(state,
                event(player, ProgressionEventType.FACTION_PLEDGED, "galacticwars:republic", 1));
        assertTrue(!repeatedPledge.accepted()
                        && repeatedPledge.reason().equals("faction_already_pledged"),
                "a new pledge event cannot re-grant the same faction pledge");
        state = accepted(state, event(player, ProgressionEventType.CREDIT_TRANSACTION, "quest_reward", 50));
        ProgressionEvent purchase = event(player, ProgressionEventType.CREDIT_TRANSACTION, "recruit_purchase", -25);
        state = accepted(state, purchase);
        assertEquals(25, state.credits(), "credit balance");
        ProgressionState duplicate = accepted(state, purchase);
        assertTrue(duplicate == state, "duplicate event is idempotent");
        ProgressionDecision overspend = GalacticProgressionCoordinator.apply(
                state, event(player, ProgressionEventType.CREDIT_TRANSACTION, "invalid_purchase", -26));
        assertTrue(!overspend.accepted() && state.credits() == 25, "overspend is atomic");
        state = accepted(state, event(player, ProgressionEventType.BUILDING_COMPLETED, "command_center", 1));
        state = accepted(state, event(player, ProgressionEventType.BUILDING_COMPLETED, "galacticwars:forward_base", 1));
        state = accepted(state, event(player, ProgressionEventType.BUILDING_COMPLETED, "galacticwars:supply_depot", 1));
        assertTrue(state.unlocks().contains("planet_travel") && state.unlocks().contains("recruitment"),
                "base progression unlocks connected systems");
        assertTrue(state.unlocks().contains("vehicle_crafting"),
                "namespaced supply depot unlocks vehicle crafting");
        state = accepted(state, event(player, ProgressionEventType.PLANET_VISITED, "tatooine", 1));
        ProgressionDecision skippedQuest = GalacticProgressionCoordinator.apply(state,
                event(player, ProgressionEventType.QUEST_ADVANCED, "republic_chapter_2", 1));
        assertTrue(!skippedQuest.accepted() && skippedQuest.reason().equals("quest_prerequisite_missing"),
                "quest chapters cannot be skipped");
        ProgressionDecision hostileQuest = GalacticProgressionCoordinator.apply(state,
                event(player, ProgressionEventType.QUEST_ADVANCED, "nightsister_chapter_1", 1));
        assertTrue(!hostileQuest.accepted() && hostileQuest.reason().equals("wrong_faction_quest"),
                "another faction's quest cannot be advanced");
        state = accepted(state, event(player, ProgressionEventType.RECRUIT_HIRED,
                "galacticwars:clone_trooper", 1));
        state = accepted(state, event(player, ProgressionEventType.QUEST_ADVANCED, "republic_chapter_1", 1));
        assertEquals(65, state.credits(), "chapter 1 configured credit reward");
        state = accepted(state, event(player, ProgressionEventType.DELIVERY_COMPLETED, "starter_delivery", 1));
        state = accepted(state, event(player, ProgressionEventType.PLANET_VISITED, "kamino", 1));
        state = accepted(state, event(player, ProgressionEventType.QUEST_ADVANCED, "republic_chapter_2", 1));
        ProgressionState questReplay = accepted(state,
                event(player, ProgressionEventType.QUEST_ADVANCED, "republic_chapter_2", 1));
        assertTrue(questReplay == state, "semantic quest replay is idempotent across event ids");
        assertTrue(state.unlocks().contains("force_path"), "quest unlocks Force path");

        UUID mandalorianPlayer = UUID.randomUUID();
        ProgressionState mandalorian = ProgressionState.create(mandalorianPlayer);
        mandalorian = accepted(mandalorian, event(mandalorianPlayer,
                ProgressionEventType.FACTION_PLEDGED, "galacticwars:mandalorian", 1));
        mandalorian = accepted(mandalorian, event(mandalorianPlayer,
                ProgressionEventType.BUILDING_COMPLETED, "command_center", 1));
        mandalorian = accepted(mandalorian, event(mandalorianPlayer,
                ProgressionEventType.RECRUIT_HIRED, "galacticwars:mandalorian_warrior", 1));
        mandalorian = accepted(mandalorian, event(mandalorianPlayer,
                ProgressionEventType.QUEST_ADVANCED, "mandalorian_chapter_1", 1));
        mandalorian = accepted(mandalorian, event(mandalorianPlayer,
                ProgressionEventType.DELIVERY_COMPLETED, "beskar_delivery", 1));
        mandalorian = accepted(mandalorian, event(mandalorianPlayer,
                ProgressionEventType.BUILDING_COMPLETED, "forward_base", 1));
        mandalorian = accepted(mandalorian, event(mandalorianPlayer,
                ProgressionEventType.TRADE_COMPLETED, "mandalorian_armorer", 1));
        mandalorian = accepted(mandalorian, event(mandalorianPlayer,
                ProgressionEventType.PLANET_VISITED, "tatooine", 1));
        mandalorian = accepted(mandalorian, event(mandalorianPlayer,
                ProgressionEventType.QUEST_ADVANCED, "mandalorian_chapter_2", 1));
        assertTrue(mandalorian.unlocks().contains("vehicle_crafting"),
                "Mandalorian chapter 2 honors its declared unlock");
        assertTrue(!mandalorian.unlocks().contains("force_path"),
                "Mandalorian chapter 2 does not receive the Republic/Nightsister Force reward");
        System.out.println("GalacticProgressionCoordinatorTest passed");
    }

    private static void installContent() {
        Map<String, LaunchContentDefinitions.PlanetDefinition> planets = Map.of(
                "tatooine", planet("tatooine", "hutt_cartel"),
                "geonosis", planet("geonosis", "separatist"),
                "kamino", planet("kamino", "republic"),
                "coruscant", planet("coruscant", "republic"));
        Map<String, LaunchContentDefinitions.QuestDefinition> quests = Map.of(
                "republic_chapter_1", quest("republic_chapter_1",
                        List.of("faction_pledged", "command_center", "clone_trooper"), 40,
                        Set.of("workforce")),
                "republic_chapter_2", quest("republic_chapter_2",
                        List.of("delivery_completed", "forward_base", "kamino"), 70,
                        Set.of("barc_speeder", "force_path")),
                "nightsister_chapter_1", quest("nightsister_chapter_1",
                        List.of("faction_pledged", "command_center", "nightsister_acolyte"), 40,
                        Set.of("workforce")),
                "mandalorian_chapter_1", quest("mandalorian_chapter_1",
                        List.of("faction_pledged", "command_center", "mandalorian_warrior"), 40,
                        Set.of("workforce")),
                "mandalorian_chapter_2", quest("mandalorian_chapter_2",
                        List.of("delivery_completed", "beskar_ingot", "tatooine"), 75,
                        Set.of("vehicle_crafting")));
        LaunchContentRuntime.install(
                new LaunchContentDefinitions(planets, Map.of(), Map.of(), quests, Map.of(), Map.of()),
                List.of("galacticwars:republic", "galacticwars:mandalorian", "galacticwars:nightsister"),
                Map.of());
    }

    private static LaunchContentDefinitions.PlanetDefinition planet(String id, String faction) {
        return new LaunchContentDefinitions.PlanetDefinition(
                id, "galacticwars:" + id, "arrival", "theme", faction);
    }

    private static LaunchContentDefinitions.QuestDefinition quest(
            String id, List<String> objectives, int credits, Set<String> unlocks
    ) {
        return new LaunchContentDefinitions.QuestDefinition(id, objectives, credits, unlocks);
    }

    private static ProgressionEvent event(UUID player, ProgressionEventType type, String subject, int amount) {
        return new ProgressionEvent(UUID.randomUUID(), player, type, subject, amount);
    }

    private static ProgressionState accepted(ProgressionState state, ProgressionEvent event) {
        ProgressionDecision decision = GalacticProgressionCoordinator.apply(state, event);
        assertTrue(decision.accepted(), decision.reason());
        return decision.state();
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) throw new AssertionError(label + " expected " + expected + " but was " + actual);
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }
}
