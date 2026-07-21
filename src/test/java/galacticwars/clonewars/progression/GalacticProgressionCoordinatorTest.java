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
        assertTrue(!GalacticProgressionCoordinator.objectiveComplete(null, "clone_trooper"),
                "an uninitialized player progression state cannot complete an objective");
        UUID player = UUID.randomUUID();
        ProgressionState state = ProgressionState.create(player);
        state = accepted(state, event(player, ProgressionEventType.FACTION_PLEDGED, "galacticwars:republic", 1));
        ProgressionDecision repeatedPledge = GalacticProgressionCoordinator.apply(state,
                event(player, ProgressionEventType.FACTION_PLEDGED, "galacticwars:republic", 1));
        assertTrue(!repeatedPledge.accepted()
                        && repeatedPledge.reason().equals("faction_already_pledged"),
                "a new pledge event cannot re-grant the same faction pledge");
        ProgressionDecision abstractCredit = GalacticProgressionCoordinator.apply(
                state, event(player, ProgressionEventType.CREDIT_TRANSACTION, "legacy_purchase", -25));
        assertTrue(!abstractCredit.accepted() && abstractCredit.reason().equals("physical_currency_required"),
                "abstract currency transactions are retired");
        assertRejectedUnchanged(state,
                event(player, ProgressionEventType.RECRUIT_HIRED, "galacticwars:invented_trooper", 1),
                "unknown_recruit");
        assertRejectedUnchanged(state,
                event(player, ProgressionEventType.BUILDING_COMPLETED, "galacticwars:death_star", 1),
                "unknown_building");

        ProgressionState deliveryIntegrity = state;
        deliveryIntegrity = accepted(deliveryIntegrity, courierEvent(player));
        deliveryIntegrity = accepted(deliveryIntegrity, courierEvent(player));
        ProgressionDecision inflatedDelivery = GalacticProgressionCoordinator.apply(deliveryIntegrity,
                event(player, ProgressionEventType.DELIVERY_COMPLETED,
                        "courier/" + UUID.randomUUID(), 99));
        assertTrue(!inflatedDelivery.accepted()
                        && inflatedDelivery.reason().equals("invalid_event_amount")
                        && inflatedDelivery.state() == deliveryIntegrity
                        && deliveryIntegrity.total(ProgressionEventType.DELIVERY_COMPLETED) == 2
                        && !deliveryIntegrity.unlocks().contains("advanced_trading"),
                "inflated delivery amount cannot change totals or grant threshold unlocks");
        deliveryIntegrity = accepted(deliveryIntegrity, courierEvent(player));
        assertTrue(deliveryIntegrity.total(ProgressionEventType.DELIVERY_COMPLETED) == 3
                        && deliveryIntegrity.unlocks().contains("advanced_trading"),
                "three distinct authoritative deliveries still grant advanced trading");
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
                "galacticwars:phase_i_clone_trooper", 1));
        assertTrue(GalacticProgressionCoordinator.objectiveComplete(state, "clone_trooper"),
                "a Phase I clone satisfies the generic Clone Trooper campaign objective");
        state = accepted(state, event(player, ProgressionEventType.QUEST_ADVANCED, "republic_chapter_1", 1));
        assertEquals(40, state.pendingCreditRewards(), "chapter 1 physical reward pending");
        state = accepted(state, courierEvent(player));
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
        mandalorian = accepted(mandalorian, courierEvent(mandalorianPlayer));
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
        Map<String, LaunchContentDefinitions.TradeDefinition> trades = Map.of(
                "mandalorian_armorer", new LaunchContentDefinitions.TradeDefinition(
                        "mandalorian_armorer", "mandalorian", 40, "galacticwars:beskar_ingot", 1,
                        "advanced_trading"));
        LaunchContentRuntime.install(
                new LaunchContentDefinitions(planets, Map.of(), Map.of(), quests, trades, Map.of()),
                List.of("galacticwars:republic", "galacticwars:mandalorian", "galacticwars:nightsister"),
                Map.of(
                        "republic", List.of("clone_trooper", "phase_i_clone_trooper"),
                        "mandalorian", List.of("mandalorian_warrior"),
                        "nightsister", List.of("nightsister_acolyte")));
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

    private static ProgressionEvent courierEvent(UUID player) {
        return event(player, ProgressionEventType.DELIVERY_COMPLETED,
                "courier/" + UUID.randomUUID(), 1);
    }

    private static void assertRejectedUnchanged(
            ProgressionState state,
            ProgressionEvent event,
            String reason
    ) {
        ProgressionDecision decision = GalacticProgressionCoordinator.apply(state, event);
        assertTrue(!decision.accepted() && decision.reason().equals(reason) && decision.state() == state,
                reason + " must reject without mutating progression");
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
